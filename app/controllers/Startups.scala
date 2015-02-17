package controllers

import _root_.util.{Tupler, CSVManager}
import _root_.util.JsArrayer.toJsArray
import models.{AngelUser, Startup}
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
    // Helper to grab 'startup_roles' from response
    def responseToRoleArray(response: JsValue):JsArray = (response \ "startup_roles").as[JsArray]
    // Helper to reduce a role's info to {id, name, follower_count, role}
    def reduceRole(role:JsValue) = Json.obj(
      "id" -> (role \ "tagged" \ "id").as[Int],
      "name" -> (role \ "tagged" \ "name").as[String],
      "follower_count" -> (role \ "tagged" \ "follower_count").as[Int].toString,
      "role" -> (role \ "role").as[String]
    )
    // Get all roles from the startup
    val futureRoles:Future[JsArray] = AngelListServices.getRolesFromStartupId(startupId) flatMap { response :JsValue =>
      (response \ "last_page").as[Int] match {                      // Check if there's more than one page
        case 1 => Future(responseToRoleArray(response))             // If there's only one, return this response's roles
        case pages => Future.sequence(                              // If there are at least 2, get the others
            (2 to pages).map(AngelListServices.getRolesFromStartupIdAndPage(startupId))
          ).map{results : IndexedSeq[JsValue] =>                    // Concatenate results
            responseToRoleArray(response).value ++ (results flatMap{response=>responseToRoleArray(response).value})
          }.map(rolesSeq => JsArray(rolesSeq))                      // Convert concatenated Seq[JsValue] to JsArray
      }
    }
    // Map the future, save the CSV and send the response of this Action
    futureRoles.map{roles : JsArray =>
      Future(
        CSVManager.put(
          s"startup-roles-$startupId",
          CSVs.makeStartupRolesCSVHeaders,
          CSVs.makeStartupRolesCSVValues(roles, startupId)
        )
      )
      // Reduce the roles for the response, and convert the Seq[JsObject] to JsArray
      Ok(JsArray(roles.value map reduceRole))
    }
  }

  def getStartupFunding(startupId: Long) = Action.async {
    getStartupFund(startupId).map { fund =>
      val fundJs = JsArray(fund)
      Future(
        CSVManager.put(
          s"startup-funding-$startupId",
          CSVs.makeStartupFundingCSVHeaders,
          CSVs.makeStartupFundingCSVValues(fundJs)
        )
      )

      Ok(fundJs)
    }
  }

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
            CSVs.makeUsersCSVHeaders(),
            CSVs.makeUsersCSVValues(Json.toJson(users).as[Seq[JsValue]]))
        )
        Ok( toJsArray(users.map(_.toTinyJson)) )
      }
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

    Roles.getFromStartupID(startupId).flatMap{roles =>
      Future.sequence(roles.map(role => getFutureUserInfoById(role.user.id)))
    }.map(_.flatten) //By flattening the Seq[Option] we remove the 'None's
  }

  def startupsFundingByCriteria(locationId: Int, marketId: Int, quality: String, creationDate: String) = Action.async {
    //Find all startups with criteria
    val fundingsFuture = startupsByCriteriaNonBlocking(locationId, marketId, Tupler.toQualityTuple(quality), Tupler.toTuple(creationDate)) flatMap { startups =>
      Future.sequence(  //With this we get a Future[Seq[Seq[JsValue]]] instead of a Seq[Future[Seq[JsValue]]]
        //For each startup, get its funding info
        startups.map( startup => getStartupFund( startup.id,  startup.name) )
      ).map(_.flatten) //With this we flatten the inner Seq[Seq[JsValue]] and we end up wit Future[Seq[JsValue]]
    }
    //Once we have a future findings, we map the future to save the CSV and return the results.
    fundingsFuture.map{fundings =>
      val fundingsJs = JsArray(fundings)
      Future{
        Logger.info(s"starting to load $locationId fundings")
        CSVManager.put(
          s"startups-funding-$locationId-$marketId-$quality-$creationDate",
          CSVs.makeStartupFundingCSVHeaders,
          CSVs.makeStartupFundingCSVValues(fundingsJs)
        )
        Logger.info(s"finished loading $locationId fundings")
      }
      Ok(fundingsJs)
    }
  }

  def startupCriteriaSearch(locationId: Int, marketId: Int, quality: String, creationDate: String) = Action.async {
    startupsByCriteriaNonBlocking(locationId, marketId, Tupler.toQualityTuple(quality), Tupler.toTuple(creationDate)).map { startups =>
      val startupsJson = Json.toJson(startups).as[Seq[JsValue]]
      Future {
        val key: String = s"startups-$locationId-$marketId-$quality-$creationDate"
        val headers: List[String] = CSVs.makeStartupsCSVHeaders()
        val values: List[List[String]] = CSVs.makeStartupsCSVValues(startupsJson)
        CSVManager.put(key, headers, values)

        val tagsKey: String = s"startups-tags-$locationId-$marketId-$quality-$creationDate"
        val tagsHeaders: List[String] = CSVs.makeStartupsTagsCSVHeaders()
        val tagsValues: List[List[String]] = CSVs.makeStartupsTagsCSVValues(startupsJson)
        CSVManager.put(tagsKey, tagsHeaders, tagsValues)
      }
      Ok(JsArray(startupsJson))
    }
  }

  def startupCriteriaSearchAndTags(locationId: Int, marketId: Int, quality: String, creationDate: String) = Action.async {
    startupsByCriteriaNonBlocking(locationId, marketId, Tupler.toQualityTuple(quality), Tupler.toTuple(creationDate)).map { startups =>
      val tags = startups flatMap { startup =>
          val allTags  = Json.toJson(startup.markets).as[Seq[JsValue]] ++
            Json.toJson(startup.locations).as[Seq[JsValue]]
          allTags map { tag => tag.as[JsObject] ++ Json.obj("startup" -> startup.id)}
      }
      val startupsJson = Json.toJson(startups).as[Seq[JsValue]]
      Future {
        val key: String = s"startups-$locationId-$marketId-$quality-$creationDate"
        val headers: List[String] = CSVs.makeStartupsCSVHeaders()
        val values: List[List[String]] = CSVs.makeStartupsCSVValues(startupsJson)
        CSVManager.put(key, headers, values)

        val tagsKey: String = s"startups-tags-$locationId-$marketId-$quality-$creationDate"
        val tagsHeaders: List[String] = CSVs.makeStartupsTagsCSVHeaders()
        val tagsValues: List[List[String]] = CSVs.makeStartupsTagsCSVValues(tags)
        CSVManager.put(tagsKey, tagsHeaders, tagsValues)
      }
      Ok(Json.obj("startups" -> startupsJson, "tags" -> tags))
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

  def getStartupFund(startupId: Long, startupName: String = ""): Future[Seq[JsValue]] =
    AngelListServices.getFundingByStartupId(startupId) map { response =>
      (response \ "funding").asOpt[Seq[JsValue]].fold{
        Seq[JsValue]()
      }{ fundings =>
        fundings.map(funding => funding.as[JsObject] ++ Json.obj("name" -> startupName, "startupId" -> startupId))
      }
    }

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

  def qualityBottomFilter(quality: Int)(startup:Startup):Boolean = startup.quality >= quality

  def qualityTopFilter(quality: Int)(startup:Startup):Boolean = startup.quality <= quality

  def creationBottomFilter(date:DateTime)(startup:Startup):Boolean = startup.created isAfter date

  def creationTopFilter(date:DateTime)(startup:Startup):Boolean = startup.created isBefore date

  def validResultFilter(json: JsValue): Boolean = (json \\ "success").isEmpty

  def isStartupFilter(startup: JsValue): Boolean = startup.validate[Startup] match {
    case role:JsSuccess[Startup] => true
    case err:JsError => Logger.warn("Failed to read startup json: "+ JsError.toFlatJson(err).toString()); false
    case _ => Logger.warn("Failed to read startup json: "+startup.toString()); false
  }
}
