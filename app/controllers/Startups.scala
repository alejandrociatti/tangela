package controllers

import _root_.util.CSVManager
import models.Startup
import org.joda.time.DateTime
import play.api.data.Form
import play.api.data.Forms._
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

  val startupForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "angelId" -> ignored(0:Long),
      "quality" -> ignored(0:Int),
      "creationDate" -> ignored(DateTime.now():DateTime),
      "id" -> optional(longNumber)
    )(Startup.apply)(Startup.unapply)
  )

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

  /**
   * This method searches for every startup tagged with a given LocationTag
   * @param locationId id of the LocationTag
   * @return JSON response containing an array of {id, name} of each startup
   */
  def getStartupsByLocationId(locationId: Long) = Action.async {
    AngelListServices.getStartupsByTagId(locationId) flatMap { response =>

      // TODO: Replace the logic by searchByTag method
      def getFutureStartupsByPage(locationId: Long)(page: Int): Future[Seq[JsValue]] = {
        AngelListServices.getStartupsByTagIdAndPage(locationId)(page) map { response =>
          val startups: JsArray = (response \ "startups").as[JsArray]
          startups.value.filter { startup => !(startup \ "hidden").as[Boolean]}.map(minimalStartUp)
        }
      }

      val pages: Int = (response \ "last_page").as[Int]
      val firstPageStartups = (response \ "startups").as[JsArray].value
        .filter( startup => !(startup \ "hidden").as[Boolean] )
        .map(minimalStartUp)

      val futureStartups =
        Future.sequence( (2 to pages) map getFutureStartupsByPage(locationId)).map(_.flatten)

      futureStartups.map { startups => Ok(JsArray(firstPageStartups ++ startups)) }
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
    AngelListServices.getRolesFromStartupId(startupId) map { response =>
      // TODO: Check if success check is necessary
      val success = response \\ "success"
      if (success.size == 0) {
        val startupsCompleteData: JsArray = (response \ "startup_roles").as[JsArray]

        Future(
          CSVManager.put(
            s"startup-roles-$startupId",
            CSVs.makeStartupRolesCSVHeaders,
            CSVs.makeStartupRolesCSVValues(startupsCompleteData, startupId)
          )
        )

        val startupsCutData = startupsCompleteData.value map { role =>
          Json.obj(
            "id" -> (role \ "user" \ "id").as[Int],
            "name" -> (role \ "user" \ "name").as[String],
            "follower_count" -> (role \ "user" \ "follower_count").as[Int].toString,
            "role" -> (role \ "role").as[String]
          )
        }

        Ok(JsArray(startupsCutData))
      } else {
        Ok(Json.obj("id" -> "error", "msg" -> s"Startup $startupId does not exist"))
      }
    }
  }

  def getStartupFunding(startupId: Long) = Action.async {
    getStartupFund(startupId).map { startupFund =>
      Future(
        CSVManager.put(
          s"startup-funding-$startupId",
          CSVs.makeStartupFundingCSVHeaders,
          CSVs.makeStartupFundingCSVValues(startupFund)
        )
      )

      Ok(startupFund)
    }
  }

  def getUsersInfoByCriteria(locationId: Int, marketId: Int, quality: Int, creationDate: String) =
    Action.async { getUsersInfoByCriteriaToLoad(locationId, marketId, quality, creationDate)}

  def getUsersInfoByCriteriaToLoad(locationId: Int, marketId: Int, quality: Int, creationDate: String) = {
    startupsByCriteria(locationId, marketId, quality, creationDate).flatMap { startups =>
      val futureUsers = startups.value.map { startup =>
        val id = (startup \ "id").asOpt[Int].getOrElse(0)
        getAllInfoOfPeopleInStartups(id)
      }

      Future.sequence(futureUsers).map { usrs =>
        Future(
          CSVManager.put(s"users-$locationId-$marketId-$quality-$creationDate",
            CSVs.makeUsersCSVHeaders(),
            CSVs.makeUsersCSVValues(usrs.flatten))
        )
        Ok(JsArray(usrs.flatten))
      }
    }
  }

  def getAllInfoOfPeopleInStartups(startupId: Long) :Future[Seq[JsValue]] = {

    def getFutureUserInfoById(userId: Int, userRole: String): Future[JsValue] = {
      AngelListServices.getUserById(userId) map { userResponse =>
        // TODO: Check if success check is necessary
        val user = userResponse
        val userSuccess = user \\ "success"
        if (userSuccess.size == 0) {
          fullUserInfo(user)
        } else {
          Json.obj()
        }
      }
    }

    //    var usersInfo : Seq[JsValue]= Seq()

    AngelListServices.getRolesFromStartupId(startupId) flatMap { response =>

      val userInfoFutures = (response \ "startup_roles").as[JsArray].value map { role =>
        val userRole: String = (role \ "role").as[String]
        val userId: Int = (role \ "user" \ "id").as[Int]
        getFutureUserInfoById(userId, userRole)
      }
      val usersInfo2 = Future.sequence(userInfoFutures)
      usersInfo2
    }
    //    val pages: Int = (response \ "last_page").as[Int]
    //    val futureResponses = (2 to pages).map(AngelListServices.getStartupsByTagIdAndPage(tagId))
    //      .map(futureResponse => futureResponse.map(responseToStartupSeq))
    //    val firstPageResponses = responseToStartupSeq(response).map(Seq(_)).map(Future(_))
    //
    //    // Add first page to result\
    //    firstPageResponses ++ futureResponses
    //    usersInfo

  }

  def startupsFundingByCriteria(locationId: Int, marketId: Int, quality: Int, creationDate: String) = Action.async {

    def getFutureStartupFunding(startupId: Long, startupName: String): Future[Seq[JsValue]] = {
      getStartupFund(startupId, startupName).map(_.as[JsArray].value)
    }
    startupsByCriteria(locationId, marketId, quality, creationDate) flatMap { filteredValues =>
      val startupsFundingFuture = filteredValues.value map { startupJson =>
        getFutureStartupFunding((startupJson \ "id").as[Long], (startupJson \ "name").as[String])
      }
      Future.sequence(startupsFundingFuture) map { startupsFunding =>
        val startupsToSend: JsArray = JsArray(startupsFunding.flatten)

        Future(
          CSVManager.put(
            s"startups-funding-$locationId-$marketId-$quality-$creationDate",
            CSVs.makeStartupFundingCSVHeaders,
            CSVs.makeStartupFundingCSVValues(startupsToSend)
          )
        )

        Ok(startupsToSend)
      }
    }
  }

  def startupCriteriaSearch(locationId: Int, marketId: Int, quality: Int, creationDate: String) = Action.async {
    startupsByCriteria(locationId, marketId, quality, creationDate).map { json =>
      Ok(json)
    }
  }

//  Helpers **********************************************************************************

  def getStartupFund(startupId: Long, startupName: String = ""): Future[JsValue] = {
    AngelListServices.getFundingByStartupId(startupId) map { response =>
      val success = response \\ "success"
      if (success.size == 0) {
        val funding: JsArray = (response \ "funding").as[JsArray]

        var seqFunding = Seq.empty[Map[String, String]]


        for (aFundraisingRound <- funding.value) {
          var seqParticipants = Seq.empty[Map[String, String]]
          val participants: JsArray = (aFundraisingRound \ "participants").as[JsArray]

          val id: Int = (aFundraisingRound \ "id").as[Int]
          val round_type: String = (aFundraisingRound \ "round_type").asOpt[String].getOrElse("")
          val amount: Int = (aFundraisingRound \ "amount").as[Int]
          val closed_at: String = (aFundraisingRound \ "closed_at").as[String]
          val source_url: String = (aFundraisingRound \ "source_url").asOpt[String].getOrElse("")

          for (participant <- participants.value) {
            val id: Int = (participant \ "id").as[Int]
            val name: String = (participant \ "name").as[String]
            val aType: String = (participant \ "type").as[String]
            seqParticipants = seqParticipants.+:(Map("id" -> id.toString, "name" -> name, "type" -> aType))
          }

          seqFunding = seqFunding.+:(Map("startup_id" -> startupId.toString, "id" -> id.toString,
            "name" -> startupName, "round_type" -> round_type, "amount" -> amount.toString,
            "closed_at" -> closed_at, "source_url" -> source_url, "participants" -> Json.toJson(seqParticipants).toString()))
        }
        Json.toJson(seqFunding.reverse)
      } else {
        Json.obj("id" -> "error", "msg" -> s"Startup $startupId does not exist")
      }

    }
  }

   /**
   * This method return all not hidden startups that are associated to a tag
   * @param tagId id of the Tag
   * @return A Future with a JsArray of the startups
   */
  def searchByTag(tagId: Long): Future[JsArray] = {
     searchByTagNonBlocking(tagId).flatMap { result =>
       Future.sequence(result).map(result => JsArray(result.flatten))
     }
  }

  def searchByTagNonBlocking(tagId: Long): Future[Seq[Future[Seq[JsValue]]]] = {
    AngelListServices.getStartupsByTagId(tagId) map { response =>

      def responseToStartupSeq(response: JsValue): Seq[JsValue] =
        (response \ "startups").as[JsArray].value
          .filter { startup => !(startup \ "hidden").as[Boolean]}
          .map(relevantStartupInfo)

      val pages: Int = (response \ "last_page").as[Int]
      val futureResponses = (2 to pages).map(AngelListServices.getStartupsByTagIdAndPage(tagId))
        .map(futureResponse => futureResponse.map(responseToStartupSeq))
      val firstPageResponses = responseToStartupSeq(response).map(Seq(_)).map(Future(_))

      // Add first page to result\
      firstPageResponses ++ futureResponses
    }
  }
  
  def startupsByCriteria(locationId: Int, marketId: Int, quality: Int, creationDate: String): Future[JsArray] = {
    startupsByCriteriaNonBlocking(locationId, marketId, quality, creationDate) map JsArray
  }


  def startupsByCriteriaNonBlocking(locationId: Int, marketId: Int, quality: Int,
                                    creationDate: String): Future[Seq[JsValue]] = {
//    Filter by location and market
    val initialStartupsToSend = if (locationId == -1) {
      searchByTagNonBlocking(marketId).flatMap(Future.sequence(_).map(_.flatten))
    } else {
      searchByTagNonBlocking(locationId).flatMap { startupsByLocation =>
        if (marketId != -1) Future.sequence(startupsByLocation)
          .map(_.map(_.filter(intFilter("markets", "id", marketId))).flatten)
        else Future.sequence(startupsByLocation).map(_.flatten)
      }
    }

//    Filter by quality and creationDate
    val filtered= initialStartupsToSend map { startups =>
      val filteredByQuality = if (quality != -1) startups.filter(intFilter("quality", quality)) else startups
      if (creationDate != "") startups.filter(dateFilter("created_at", DateTime.parse(creationDate)))
      else filteredByQuality
    } map(_ filter validResultFilter)

    filtered map { startups =>
      Future {
        val key: String = s"startups-$locationId-$marketId-$quality-$creationDate"
        val headers: List[String] = CSVs.makeStartupsCSVHeaders()
        val values: List[List[String]] = CSVs.makeStartupsCSVValues(startups)
        CSVManager.put(key, headers, values)
      }
    }
    filtered map { startups =>
      Future {
        val key: String = s"startups-tags-$locationId-$marketId-$quality-$creationDate"
        val headers: List[String] = CSVs.makeStartupsTagsCSVHeaders()
        val values: List[List[String]] = CSVs.makeStartupsTagsCSVValues(startups)
//        println("values = " + values)
        CSVManager.put(key, headers, values)
      }
    }
    filtered
  }

  def intFilter(field: String, value: Int)(json: JsValue): Boolean =
    (json \ field).as[Int] == value

  def intFilter(field: String, subField: String, value: Int)(json: JsValue): Boolean =
    (json \ field).as[JsArray].value.exists(intFilter(subField, value))

  def dateFilter(field: String, value: DateTime)(json: JsValue): Boolean =
    DateTime.parse((json \ field).as[String]) isAfter value

  def validResultFilter(json: JsValue): Boolean = (json \\ "success").isEmpty
  
//  Json Constructors *********************************************************************

  def minimalStartUp(startup: JsValue) = Json.obj(
    "id" -> (startup \ "id").as[Int],
    "name" -> (startup \ "name").as[String]
  )

  def relevantStartupInfo(startup: JsValue) = Json.obj(
    "id" -> (startup \ "id").asOpt[Long].getOrElse[Long](0),
    "hidden" -> (startup \ "hidden").asOpt[Boolean].getOrElse[Boolean](false),
    "community_profile" -> (startup \ "community_profile").asOpt[String].getOrElse[String](""),
    "name" -> (startup \ "name").asOpt[String].getOrElse[String](""),
    "angellist_url" -> (startup \ "angellist_url").asOpt[String].getOrElse[String](""),
    "logo_url" -> (startup \ "logo_url").asOpt[String].getOrElse[String](""),
    "thumb_url" -> (startup \ "thumb_url").asOpt[String].getOrElse[String](""),
    "quality" -> (startup \ "quality").asOpt[Int].getOrElse[Int](-1),
    "product_desc" -> (startup \ "product_desc").asOpt[String].getOrElse[String](""),
    "high_concept" -> (startup \ "high_concept").asOpt[String].getOrElse[String](""),
    "follower_count" -> (startup \ "follower_count").asOpt[Int].getOrElse[Int](0),
    "company_url" -> (startup \ "company_url").asOpt[String].getOrElse[String](""),
    "created_at" -> (startup \ "created_at").asOpt[String].getOrElse[String](""),
    "updated_at" -> (startup \ "updated_at").asOpt[String].getOrElse[String](""),
    "twitter_url" -> (startup \ "twitter_url").asOpt[String].getOrElse[String](""),
    "blog_url" -> (startup \ "blog_url").asOpt[String].getOrElse[String](""),
    "video_url" -> (startup \ "video_url").asOpt[String].getOrElse[String](""),
    "markets" -> (startup \ "markets").asOpt[JsArray].getOrElse[JsArray](JsArray.apply()),
    "locations" -> (startup \ "locations").asOpt[JsArray].getOrElse[JsArray](JsArray.apply()),
    "company_type" -> (startup \ "company_type").asOpt[JsArray].getOrElse[JsArray](JsArray.apply())
  )

  def fullUserInfo(user: JsValue) = Json.obj(
    "id" -> (user \ "id").as[Long],
    "name" -> (user \ "name").asOpt[String].getOrElse[String](""),
    "bio" -> (user \ "bio").asOpt[String].getOrElse[String](""),
    "role" -> (user \ "role").asOpt[String].getOrElse[String](""),
    "follower_count" -> (user \ "follower_count").as[Int].toString,
    "angellist_url" -> (user \ "angellist_url").asOpt[String].getOrElse[String](""),
    "image" -> (user \ "image").asOpt[String].getOrElse[String](""),
    "blog_url" -> (user \ "blog_url").asOpt[String].getOrElse[String](""),
    "online_bio_url" -> (user \ "online_bio_url").asOpt[String].getOrElse[String](""),
    "twitter_url" -> (user \ "twitter_url").asOpt[String].getOrElse[String](""),
    "facebook_url" -> (user \ "facebook_url").asOpt[String].getOrElse[String](""),
    "linkedin_url" -> (user \ "linkedin_url").asOpt[String].getOrElse[String](""),
    "what_ive_built" -> (user \ "what_ive_built").asOpt[String].getOrElse[String](""),
    "what_i_do" -> (user \ "what_i_do").asOpt[String].getOrElse[String](""),
    "investor" -> (user \ "investor").as[Boolean]
  )
}
