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

  def getUsersInfoByCriteria(locationId: Int, marketId: Int, quality: Int, creationDate: String) = Action.async {
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

  def getUsersInfoByCriteriaToLoad(locationId: Int, marketId: Int, quality: Int, creationDate: String): Future[Unit] = {
    startupsByCriteria(locationId, marketId, quality, creationDate).map { startups =>
      startups.value.map { startup =>
        val id = (startup \ "id").asOpt[Int].getOrElse(0)
        getAllInfoOfPeopleInStartups(id)
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
      Future {
        val key: String = s"startups-$locationId-$marketId-$quality-$creationDate"
        val headers: List[String] = CSVs.makeStartupsCSVHeaders()
        val values: List[List[String]] = CSVs.makeStartupsCSVValues(json.value)
        CSVManager.put(key, headers, values)

        val tagsKey: String = s"startups-tags-$locationId-$marketId-$quality-$creationDate"
        val tagsHeaders: List[String] = CSVs.makeStartupsTagsCSVHeaders()
        val tagsValues: List[List[String]] = CSVs.makeStartupsTagsCSVValues(json.value)
        CSVManager.put(tagsKey, tagsHeaders, tagsValues)
      }
      Ok(json)
    }
  }

  def startupCriteriaSearchAndTags(locationId: Int, marketId: Int, quality: Int, creationDate: String) = Action.async {
    startupsByCriteria(locationId, marketId, quality, creationDate).map { startups =>
      val tags = JsArray(startups.value flatMap { startup =>
        val allTags  = (startup \ "markets").as[Seq[JsValue]] ++ (startup \ "locations").as[Seq[JsValue]] ++ (startup \ "company_type").as[Seq[JsValue]]
        allTags map { tag => tag.as[JsObject] +("startup" -> (startup \ "id"))}
      })
      Future {
        val key: String = s"startups-$locationId-$marketId-$quality-$creationDate"
        val headers: List[String] = CSVs.makeStartupsCSVHeaders()
        val values: List[List[String]] = CSVs.makeStartupsCSVValues(startups.value)
        CSVManager.put(key, headers, values)

        val tagsKey: String = s"startups-tags-$locationId-$marketId-$quality-$creationDate"
        val tagsHeaders: List[String] = CSVs.makeStartupsTagsCSVHeaders()
        val tagsValues: List[List[String]] = CSVs.makeStartupsTagsCSVValues(tags.value)
        CSVManager.put(tagsKey, tagsHeaders, tagsValues)
      }
      Ok(Json.obj("startups" -> startups, "tags" -> tags))
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
   * This method returns startups associated to a tag (which are visible)
   * @param tagId id of the Tag
   * @return A Future with a JsArray of the startups
   */
  def searchByTag(tagId: Long): Future[JsArray] = searchByTagNonBlocking(tagId) map JsArray

  def searchByTagNonBlocking(tagId: Long): Future[Seq[JsValue]] = {

    def responseToStartupSeq(response: JsValue): Seq[JsValue] =
      (response \ "startups").as[JsArray].value
        .filter { startup => !(startup \ "hidden").as[Boolean]}
        .map(relevantStartupInfo)

    AngelListServices.getStartupsByTagId(tagId) flatMap { response :JsValue =>
      val pages = (response \ "last_page").as[Int]
      // Get startups for the rest of the pages (wrapped in Future.sequence to convert Seq of Futures to Future of Seqs)
      Future.sequence(
        (2 to pages).map(AngelListServices.getStartupsByTagIdAndPage(tagId))
      ).map{ results : IndexedSeq[JsValue] =>
        // Convert the 1st page (response) and the rest of them (results) to StartupsSeq and join all of them
        responseToStartupSeq(response) ++ (results flatMap responseToStartupSeq)
      } //The result is a Future[Seq[JsValue]] where each JsValue represents a Startup
    }
  }

  def startupsByCriteria(locationId: Int, marketId: Int, quality: Int, creationDate: String): Future[JsArray] = {
    startupsByCriteriaNonBlocking(locationId, marketId, quality, creationDate) map JsArray
  }

  /**
   *  This method fetchs Startups using locationId or marketId
   *  and then filters the results with the given parameters
   */
  def startupsByCriteriaNonBlocking(locationId: Int, marketId: Int, quality: Int,
                                     creationDate: String): Future[Seq[JsValue]] = {
    val seed = (locationId, marketId) match {
      case (location, -1) => searchByTagNonBlocking(location)
      case (-1, market) => searchByTagNonBlocking(market)
      case (location, market) => searchByTagNonBlocking(location).map(_.filter(intFilter("markets", "id", market)))
    }
    val result = (quality, creationDate) match {
      case (-1, "") => seed
      case (q, "") => seed.map(_.filter(intFilter("quality", q)))
      case (-1, d) => seed.map(_.filter(dateFilter("created_at", DateTime.parse(d))))
      case (q, d) => seed.map(_.filter(intFilter("quality", q)).filter(dateFilter("created_at", DateTime.parse(d))))
    }
    result
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
