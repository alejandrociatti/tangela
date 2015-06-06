package controllers

import _root_.util.{Tupler, CSVManager}
import _root_.util.JsArrayer.toJsArray
import models._
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

/**
 * Created by Javier Isoldi.
 * Date: 5/16/14.
 * Project: Tangela.
 */

object Startups extends Controller with Secured {

//  Actions ************************************************************************

  /**
   * Responds with a JSON containing the minimum amount of information
   * required to show given startup in a network graph.
   */
  def getStartupNetInfo(startupId:Long) = Action.async {
    AngelListServices.getStartupById(startupId) map { jsResponse =>
      val followers: Int = (jsResponse \ "follower_count").as[Int]
      val name: String = (jsResponse \ "name").as[String]
      Ok(Json.toJson(Map("id"->startupId.toString, "follower_count"->followers.toString, "name"->name)))
    }
  }

  def getStartupById(startupId: Long) = Action.async {
    AngelListServices.getStartupById(startupId).map { jsResponse =>
      (jsResponse \\ "success").headOption.fold {
        Ok(Json.toJson(jsResponse \\ "fundraising"))
      } { success =>
        Ok("No existe el StartUp")
      }
    }
  }

  def getNumberOfFoundersByStartupId(startupId: Long) = Action.async {
    AngelListServices.getFoundersByStartupId(startupId) map { response =>

      val success = response \\ "success"
      if (success.size == 0) {

        val founders: JsArray = (response \ "startup_roles").as[JsArray]
        val numberOfFounders: Int = founders.value.size

        Ok(numberOfFounders.toString)
      } else {
        Ok("No existe el startup")
      }
    }
  }

  def getStartupsByName(startupName: String) = Action.async {
    val name: String = startupName.replaceAll("\\s", "_")
    AngelListServices.searchStartupByName(name) map { response =>
      //TODO: que me busque todas las paginas y no solo la primera
      val success = response \\ "success"
      if (success.size == 0) {
        val startups: JsArray = response.as[JsArray]
        var seqAux = Seq.empty[Map[String, String]]
        for (startup <- startups.value) {
          val id: Int = (startup \ "id").as[Int]
          val name: String = (startup \ "name").as[String]
          seqAux = seqAux.+:(Map("id" -> id.toString, "name" -> name))
        }
        Ok(Json.toJson(seqAux))
      } else {
        Ok("No existen startups con este nombre")
      }
    }
  }

  def getRolesOfStartup(startupId: Long) = Action.async {
    val futureRoles = Roles.getRolesFromStartupID(startupId)
    // Map the future, save the CSV and send the response of this Action
    futureRoles.map{roles =>
      Future(
        CSVManager.put(
          s"startup-roles-$startupId",
          AngelRole.getCSVHeader,
          roles.map(_.toCSVRow)
        )
      )
      // Reduce the roles for the response, and convert the Seq[JsObject] to JsArray
      Ok(JsArray(roles.map(_.toTinyJson)))
    }
  }

  def getStartupFunding(id:Long) = Action.async {
    AngelListServices.getStartupById(id).flatMap{ jsStartup =>
      jsStartup.validate[Startup] match {
        case role:JsSuccess[Startup] =>
          val startup = jsStartup.validate[Startup].get
          Fundings.getFromStartupID(id).map{ funds =>
            Future(
              CSVManager.put(
                s"startup-funding-$id",
                Funding.getCSVHeader,
                funds.flatMap(_.toCSVRows(startup))
              )
            )
            Ok(Json.toJson(funds))
          }
        case err:JsError =>
          Logger.warn("Failed to read startup json: "+ JsError.toFlatJson(err).toString())
          Future(Ok("{\"success\":false}"))
        case _ =>
          Logger.warn("Failed to read startup json: "+jsStartup.toString())
          Future(Ok("{\"success\":false}"))
      }
    }
  }

//  def getStartupFunding(startup: Startup):Action[AnyContent] = Action.async {
//    val startupID = startup.id
//    Fundings.getFromStartupID(startupID).map{fundings =>
//      Future(
//        CSVManager.put(
//          s"startup-funding-$startupID",
//          Funding.getCSVHeader,
//          fundings.flatMap(_.toCSVRows(startup))
//        )
//      )
//      Ok(Json.toJson(fundings))
//    }
//  }

  def getUsersInfoByCriteria(locationId: Int, marketId: Int, quality: String, creationDate: String) = Action.async {
    //Get all startups with criteria
    startupsByCriteriaNonBlocking(locationId, marketId, Tupler.toQualityTuple(quality), Tupler.toTuple(creationDate)).flatMap { startups =>
      Future.sequence(                //Wrapped in Future.sequence to change Seq[Future] to Future[Seq]
        startups.map { startup =>     //get info of people involved in each startup
          getAllInfoOfPeopleInStartup(startup.id)
        }               //We end up with Future[Seq[Seq[JsValue]]] so we flatten both Seq and then map the resulting Seq
      ).map(_.flatten).map{ users =>
        Future(
          CSVManager.put(s"users-$locationId-$marketId-$quality-$creationDate",
            AngelUser.getCSVHeader,
            users.map(_.toCSVRow)
          )
        )
        Ok( Json.obj("users" -> Json.toJson(users)) )
      }
    }
  }

  def getUserAndRolesByCriteria(locationId: Int, marketId: Int, quality: String, creationDate: String) = Action.async {
    startupsByCriteriaNonBlocking(locationId, marketId, Tupler.toQualityTuple(quality), Tupler.toTuple(creationDate)).flatMap { startups =>
      // get the roles for each startup (Future[Seq[Seq[AngelRole]]])
      Future.sequence( startups.map(startup => Roles.getRolesFromStartupID(startup.id)) ).flatMap( roleArrays =>
        // with the Seq[AngelRole] we produce a Seq[AngelUserRole] which contains the additional user info
        Members.getWithRoles(roleArrays.flatten).map{ rolesAndUsers =>
          Future(
            CSVManager.put(
              s"roles+users-$locationId-$marketId-$quality-$creationDate",
              AngelUserRole.getCSVHeader,
              rolesAndUsers.map(_.toCSVRow))
          )
          Ok(Json.toJson(rolesAndUsers))
        }
      )
    }
  }

  def getUsersInfoByCriteriaToLoad(locationId: Int, marketId: Int, quality: String, creationDate: String): Future[Unit] = {
    startupsByCriteriaNonBlocking(locationId, marketId, Tupler.toQualityTuple(quality), Tupler.toTuple(creationDate)).map { startups =>
      startups.map(startup => getAllInfoOfPeopleInStartup(startup.id) )
    }
  }

  def getAllInfoOfPeopleInStartup(startupId: Long) :Future[Seq[AngelUser]] = {

    def getFutureUserInfoById(userId: Long): Future[Option[AngelUser]] =
      AngelListServices.getUserById(userId) map { userResponse =>
         userResponse.validate[AngelUser] match{
           case user:JsSuccess[AngelUser] => Some(user.get)
           case _ => 
             Logger.warn("Failed to read user json "+userResponse)
             None
         }
      }

    Roles.getRolesFromStartupID(startupId).flatMap{roles =>
      Future.sequence(roles.map(role => getFutureUserInfoById(role.user.id)))
    }.map(_.flatten) //By flattening the Seq[Option] we remove the 'None's
  }

  def startupsFundingByCriteria(locationId: Int, marketId: Int, quality: String, creationDate: String) = Action.async {
    // Find all startups with criteria
    val fundingStartupFuture =
      startupsByCriteriaNonBlocking(locationId, marketId, Tupler.toQualityTuple(quality), Tupler.toTuple(creationDate)) flatMap { startups =>
        // Map those startups to find their fundings, creating tuples of (startup, fund)
        Future.sequence(
          startups.map(startup => Fundings.getFromStartup(startup).map(_.map(fund => (startup, fund))) )
        ).map(_.flatten)
      }
    //Once we have a future of (startup, fund), we map the future to save the CSV and return the results.
    fundingStartupFuture.map{startupFund =>
      Future(
        CSVManager.put(
          s"startups-funding-$locationId-$marketId-$quality-$creationDate",
          Funding.getCSVHeader,
          startupFund.flatMap(tuple => tuple._2.toCSVRows(tuple._1))
        )
      )
      Ok( Json.toJson(startupFund.map(_._2)) ) // this does Json.toJson( Seq[Funding] )
    }
  }

  def startupCriteriaSearch(locationId: Int, marketId: Int, quality: String, creationDate: String) = Action.async {
    startupsByCriteriaNonBlocking(locationId, marketId, Tupler.toQualityTuple(quality), Tupler.toTuple(creationDate)).map { startups =>
      val startupsJson = Json.toJson(startups).as[Seq[JsValue]]
      Future {
        val key: String = s"startups-$locationId-$marketId-$quality-$creationDate"
        CSVManager.put(key, Startup.getCSVHeader, startups.map(_.toCSVRow))
      }
      Ok(JsArray(startupsJson))
    }
  }

  def startupCriteriaSearchAndTags(locationId: Int, marketId: Int, quality: String, creationDate: String) = Action.async {
    startupsByCriteriaNonBlocking(locationId, marketId, Tupler.toQualityTuple(quality), Tupler.toTuple(creationDate)).map { startups =>
      val startupsJson = Json.toJson(startups).as[Seq[JsValue]]
      Future {
        val key: String = s"startups-$locationId-$marketId-$quality-$creationDate"
        CSVManager.put(key, Startup.getCSVHeader, startups.map(_.toCSVRow))
        val tagsKey: String = s"startups-tags-$locationId-$marketId-$quality-$creationDate"
        CSVManager.put(tagsKey, AngelTag.getCSVHeader, startups.map(_.getTagsCSVRows))
      }
      Ok(Json.obj("startups" -> startupsJson, "tags" -> startups.map(_.getTagsJsons)))
    }
  }

  def getStartupIDsFromUserID(id : Long): Future[Seq[Long]] = {
    AngelListServices.getRolesFromUserId(id) map {response =>
      (response \ "startup_roles").asOpt[JsArray].fold{
        Seq[Long]()
      }{ roles =>
        roles.value.filter(role => (role \ "user") != JsNull).map(role =>
          (role \ "startup" \ "id").as[Long]
        )
      }
    }
  }

  def getStartupIDsFromUserIDs(users: Seq[Long]):Future[Seq[Long]] =
    Future.sequence(users map getStartupIDsFromUserID).map(_.flatten.distinct)


  //  Helpers **********************************************************************************

   /**
   * This method returns startups associated to a tag (which are visible)
   * @param tagId id of the Tag
   * @return A Future with a Seq[Startup]
   */
  def searchByTagNonBlocking(tagId: Long): Future[Seq[Startup]] = {

    def responseToStartupSeq(response: JsValue): Seq[Startup] =
      (response \ "startups").as[Seq[JsValue]].filter(isStartupFilter).map(_.validate[Startup].get)

    AngelListServices.getStartupsByTagId(tagId) flatMap { response :JsValue =>
      (response \ "last_page").asOpt[Int].getOrElse(-1) match {
        case -1 =>                                      // If 'last_page' doesn't exist, something is wrong
          Logger.warn(s"unexpected response on search by tag for $tagId")
          Logger.info(response.toString())
          Future(Seq[Startup]())
        case 1 => Future(responseToStartupSeq(response)) //In case this is the only page
        case pages =>
          // Get startups for the rest of the pages (wrapped in Future.sequence to convert Seq of Futures to Future of Seqs)
          Future.sequence(
            (2 to pages).map(AngelListServices.getStartupsByTagIdAndPage(tagId))
          ).map{ results : IndexedSeq[JsValue] =>
            // Convert the 1st page (response) and the rest of them (results) to StartupsSeq and join all of them
            responseToStartupSeq(response) ++ (results flatMap responseToStartupSeq)
          } //The result is a Future[Seq[JsValue]] where each JsValue represents a Startup
      }
    }
  }

  /**
   *  This method fetchs Startups using locationId or marketId
   *  and then filters the results with the given parameters
   */
  def startupsByCriteriaNonBlocking(locationId: Int, marketId: Int, qualityRange: (Int,Int),
                                     creationRange: (String,String)): Future[Seq[Startup]] = {
    val seed = (locationId, marketId) match {
      case (location, -1) => searchByTagNonBlocking(location)
      case (-1, market) => searchByTagNonBlocking(market)
      case (location, market) => searchByTagNonBlocking(location).map(_.filter(marketFilter(market)))
    }
    val qualityFiltered = qualityRange match {
      case (-1, -1) => seed
      case (start, -1) => seed.map(_.filter(qualityBottomFilter(start)))
      case (-1, end) => seed.map(_.filter(qualityTopFilter(end)))
      case (start, end) => seed.map(
        _.filter(qualityBottomFilter(start)).filter(qualityTopFilter(end))
      )
    }
    val creationFiltered = creationRange match {
      case ("", "") => qualityFiltered
      case (start, "") => qualityFiltered.map(_.filter(creationBottomFilter(DateTime.parse(start))))
      case ("", end) => qualityFiltered.map(_.filter(creationTopFilter(DateTime.parse(end))))
      case (start, end) => qualityFiltered.map(
          _.filter(creationBottomFilter(DateTime.parse(start)))
            .filter(creationTopFilter(DateTime.parse(end)))
        )
    }
    creationFiltered
  }

  def marketFilter(marketID: Long)(startup:Startup):Boolean =
    startup.markets.fold{false}{markets=> markets.exists(market => market.id == marketID)}

  def qualityBottomFilter(quality: Int)(startup:Startup):Boolean = startup.quality.fold(true)(_ >= quality)

  def qualityTopFilter(quality: Int)(startup:Startup):Boolean = startup.quality.fold(true)(_ <= quality)

  def creationBottomFilter(date:DateTime)(startup:Startup):Boolean = startup.created.fold(true)(_ isAfter date)

  def creationTopFilter(date:DateTime)(startup:Startup):Boolean = startup.created.fold(true)(_ isBefore date)

  def validResultFilter(json: JsValue): Boolean = (json \\ "success").isEmpty

  def isStartupFilter(startup: JsValue): Boolean = startup.validate[Startup] match {
    case role:JsSuccess[Startup] => true
    case err:JsError => Logger.warn("Failed to read startup json: "+ JsError.toFlatJson(err).toString()); false
    case _ => /*Logger.warn("Failed to read startup json: "+startup.toString());*/ false
  }
}
