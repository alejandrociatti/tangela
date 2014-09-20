package controllers

import models.Startup
import org.joda.time.DateTime
import models.authentication.Role._
import play.api.data.Forms._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future, _}
import scala.concurrent.duration.Duration

/**
 * Created by Javier Isoldi.
 * Date: 5/16/14.
 * Project: Tangela.
 */

object Startups extends Controller with Secured {

  val ANGELAPI = "https://api.angel.co/1"

  val startupForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "angelId" -> ignored(0:Long),
      "quality" -> ignored(0:Int),
      "creationDate" -> ignored(DateTime.now():DateTime),
      "id" -> optional(longNumber)
    )(Startup.apply)(Startup.unapply)
  )

  /**
   * Responds with a JSON containing the minimum amount of information
   * required to show given startup in a network graph.
   */
  def getStartupNetInfo(startupId: Long) = Action.async {
    WS.url(Application.AngelApi + s"/startups/$startupId").get().map { response =>
      val resp: JsValue = response.json.as[JsValue]
      val followers: Int = (resp \ "follower_count").as[Int]
      val name: String = (resp \ "name").as[String]
      Ok(Json.toJson(Map("id" -> startupId.toString, "follower_count" -> followers.toString, "name" -> name)))
    }
  }

  /**
   * This method searches for every startup tagged with a given LocationTag
   * @param locationId id of the LocationTag
   * @return JSON response containing an array of {id, name} of each startup
   */
  def getStartupsByLocationId(locationId: Long) = Action.async {
    WS.url(Application.AngelApi + s"/tags/$locationId/startups").get().map { response =>
      val pages: Int = (response.json \ "last_page").as[Int]

      val startups: JsArray = (response.json \ "startups").as[JsArray]

      var seqAux = Seq.empty[Map[String, String]]

      startups.value.filter { startup => !(startup \ "hidden").as[Boolean]}.map { startup =>
        val id: Int = (startup \ "id").as[Int]
        val name: String = (startup \ "name").as[String]
        seqAux = seqAux.+:(Map("id" -> id.toString, "name" -> name))
      }

      var futures = Seq.empty[Future[_]]

      //AAC: I can confirm this is working with a small amount of pages (2 until 20)
      //TODO: Ensure that this can cope with any amount of pages
      for (i <- 2 until 20) {
        futures = futures.+:(getFutureStartupsByPage(i))
      }

      Await.result(Future.sequence[Any, Seq](futures), Duration.Inf)

      def getFutureStartupsByPage(page: Int) = {
        WS.url(Application.AngelApi + s"/tags/$locationId/startups?page=$page").get().map { response =>
          val startups: JsArray = (response.json \ "startups").as[JsArray]

          startups.value.filter { startup => !(startup \ "hidden").as[Boolean]}.map { startup =>
            val id: Int = (startup \ "id").as[Int]
            val name: String = (startup \ "name").as[String]
            seqAux = seqAux.+:(Map("id" -> id.toString, "name" -> name))
          }
        }
      }
      //TODO: Asychronously load every startup to DB (loadStartupsByLocationIdToDB)
      Ok(Json.toJson(seqAux))
    }
  }

  //TODO: implement this method
  /**
   * This method loads every startup on a given location to the DB
   * @param locationId id of the LocationTag
   */
  def loadStartupsByLocationIdToDB(locationId: Long) = {}


  def getNumberOfStartupsFundraising = withAsyncAuth(Admin, Researcher){username => implicit request =>
    WS.url(Application.AngelApi + "/startups?filter=raising").get().map { response =>

      val total = (response.json \ "total").as[Int]

      Ok(views.html.fundraisingCount(total))
    }
  }

  def getStartupById(startupId: Long) = Action.async {
    val url: String = ANGELAPI + s"/startups/$startupId"
    WS.url(url).get().map { response =>

      val success = response.json \\ "success"
      if (success.size == 0) {
        val fundraising = response.json.\\("fundraising")
        Ok(Json.toJson(fundraising))
      } else {
        Ok("No existe el StartUp")
      }
    }
  }

  def getNumberOfFoundersByStartupId(startupId: Long) = Action.async {
    WS.url(Application.AngelApi + s"/startups/$startupId/roles?role=founder").get().map { response =>

      val success = response.json \\ "success"

      if (success.size == 0) {

        val founders: JsArray = (response.json \ "startup_roles").as[JsArray]
        val numberOfFounders: Int = founders.value.size

        Ok(numberOfFounders.toString)
      } else {
        Ok("No existe el startup")
      }
    }
  }


  def getStartupsByName(startupName: String) = Action.async {
    val name: String = startupName.replaceAll("\\s", "_")
    val url: String = ANGELAPI + s"/search?query=$name&type=Startup"
    WS.url(url).get().map { response =>
      //TODO: que me busque todas las paginas y no solo la primera
      val success = response.json \\ "success"
      if (success.size == 0) {
        val startups: JsArray = response.json.as[JsArray]
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
    WS.url(Application.AngelApi + s"/startups/$startupId/roles").get().map { response =>

      //      val roles:JsArray = (response.json \ "startup_roles").as[JsArray]

      val success = response.json \\ "success"

      if (success.size == 0) {

        val roles: JsArray = (response.json \ "startup_roles").as[JsArray]

        var seqAux = Seq.empty[Map[String, String]]

        for (role <- roles.value) {
          val roleString: String = (role \ "role").as[String]
          val user = (role \ "tagged").as[JsValue]
          val id: Int = (user \ "id").as[Int]
          val name: String = (user \ "name").as[String]
          val followers: Int = (user \ "follower_count").as[Int]

          seqAux = seqAux.+:(Map("id" -> id.toString, "name" -> name, "role" -> roleString, "follower_count" -> followers.toString))
        }

        Ok(Json.toJson(seqAux))
      } else {
        Ok(Json.obj("id" -> "error", "msg" -> s"Startup $startupId does not exist"))
      }
    }
  }


  def getStartupFunding(startupId: Long) = Action.async {
    getStartupFund(startupId).map { startupFund =>
      Ok(startupFund)
    }
  }

  def getStartupFund(startupId: Long, startupName: String = ""): Future[JsValue] = {
    val url: String = ANGELAPI + "/startups/" + startupId + "/funding"
    WS.url(url).get().map { response =>
      val success = response.json \\ "success"
      if (success.size == 0) {
        val funding: JsArray = (response.json \ "funding").as[JsArray]

        var seqFunding = Seq.empty[Map[String, String]]


        for (aFundraisingRound <- funding.value) {
          var seqParticipants = Seq.empty[Map[String, String]]
          val participants: JsArray = (aFundraisingRound \ "participants").as[JsArray]

          println(s"ACTUAL JSON: $aFundraisingRound")

          val id: Int = (aFundraisingRound \ "id").as[Int]
//          val round_type: String = (aFundraisingRound \ "round_type").as[String]
          val round_type: String = (aFundraisingRound \ "round_type").asOpt[String].getOrElse("")
          val amount: Int = (aFundraisingRound \ "amount").as[Int]
          val closed_at: String = (aFundraisingRound \ "closed_at").as[String]

          for (participant <- participants.value) {
            val id: Int = (participant \ "id").as[Int]
            val name: String = (participant \ "name").as[String]
            val aType: String = (participant \ "type").as[String]
            seqParticipants = seqParticipants.+:(Map("id" -> id.toString, "name" -> name, "type" -> aType))
          }

          seqFunding = seqFunding.+:(Map("id" -> id.toString, "name" -> startupName, "round_type" -> round_type, "amount" -> amount.toString,
            "closed_at" -> closed_at, "participants" -> Json.toJson(seqParticipants).toString()))
        }
        Json.toJson(seqFunding.reverse)
      } else {
        Json.obj("id" -> "error", "msg" -> s"Startup $startupId does not exist")
      }

    }
  }

  def startupsFundingByCriteria(locationId: Int, marketId: Int, quality: Int, creationDate: String) = Action.async {
    var startupsToReturn:JsArray = JsArray()

    var futures = Seq.empty[Future[_]]

    def getFutureStartupFunding(startupId:Long, startupName:String) = {
      getStartupFund(startupId, startupName).map {fund =>
        fund.as[JsArray].value.map {fundPosta =>
          startupsToReturn = startupsToReturn.+:(fundPosta)
        }
      }
    }

    startupsByCriteria(locationId, marketId, quality, creationDate).map { filteredJson =>
      filteredJson.value.map { startupJson =>

        futures = futures.+:(getFutureStartupFunding((startupJson \ "id").as[Long], (startupJson \ "name").as[String]))

      }
      Await.result(Future.sequence[Any, Seq](futures), Duration.Inf)

      Ok(startupsToReturn)
    }
  }

  def startupCriteriaSearch(locationId: Int, marketId: Int,
                            quality: Int, creationDate: String) = Action.async {

    startupsByCriteria(locationId, marketId, quality, creationDate).map { json =>
      Ok(json)
    }
  }

  def startupsByCriteria(locationId: Int, marketId: Int,
                         quality: Int, creationDate: String): Future[JsArray] = {
    var startupsToSend: JsArray = JsArray()
    if (locationId != -1) {

      searchByTag(locationId).map { startups =>
        startupsToSend = startups
        if (marketId != -1) startupsToSend = filterArrayByInt(startupsToSend, "markets", "id", marketId)
        if (quality != -1) startupsToSend = filterByInt(startupsToSend, "quality", quality)
        if (creationDate != "") startupsToSend = filterByDate(startupsToSend, "created_at", creationDate)
        startupsToSend
      }
    } else {
      searchByTag(marketId).map { startups =>
        if (quality != -1) startupsToSend = filterByInt(startupsToSend, "quality", quality)
        if (creationDate != "") startupsToSend = filterByDate(startupsToSend, "created_at", creationDate)
        startupsToSend
      }
    }
  }

  def filterByInt(startups: JsArray, filterString: String, filterValue: Int): JsArray = {
    JsArray(startups.value.filter { startup =>
      (startup \ filterString).as[Int].equals(filterValue)
    })
  }

  def filterByDate(startups: JsArray, filterString: String, filterValue: String): JsArray = {
    val date: DateTime = DateTime.parse(filterValue)
    JsArray(startups.value.filter { startup =>
      DateTime.parse((startup \ filterString).as[String]).isAfter(date)
    })
  }

  def filterArrayByInt(startups: JsArray, filterStringForArray: String, filterStringForValue: String, filterValue: Int): JsArray = {
    JsArray(startups.value.filter { startup =>
      (startup \ filterStringForArray).as[JsArray].value.exists { filterArrayValue =>
        (filterArrayValue \ filterStringForValue).as[Int].equals(filterValue)
      }
    })
  }

  def searchByTag(tag: Long): Future[JsArray] = {
    WS.url(Application.AngelApi + s"/tags/$tag/startups").get().map { response =>
      val pages: Int = (response.json \ "last_page").as[Int]
      val startups: JsArray = (response.json \ "startups").as[JsArray]

      var startupsAux: JsArray = JsArray()

      startups.value.filter { startup => !(startup \ "hidden").as[Boolean]}.map { startup =>
        startupsAux = startupsAux.+:(getRelevantStartupInfo(startup))
      }

      var futures = Seq.empty[Future[_]]

      for (i <- 2 until 20) {
        futures = futures.+:(getFutureStartupsByPage(i))
      }

      Await.result(Future.sequence[Any, Seq](futures), Duration.Inf)

      def getFutureStartupsByPage(page: Int) = {
        WS.url(Application.AngelApi + s"/tags/$tag/startups?page=$page").get().map { response =>
          val startups: JsArray = (response.json \ "startups").as[JsArray]

          startups.value.filter { startup => !(startup \ "hidden").as[Boolean]}.map { startup =>
            startupsAux = startupsAux.+:(getRelevantStartupInfo(startup))
          }
        }
      }

      startupsAux
    }
  }

  def getRelevantStartupInfo(startup: JsValue): JsValue = {
    val id: JsNumber = (startup \ "id").as[JsNumber]
    val name: JsString = (startup \ "name").as[JsString]
    val markets: JsArray = (startup \ "markets").as[JsArray]
    val quality: JsNumber = (startup \ "quality").as[JsNumber]
    val creationDate: JsString = (startup \ "created_at").as[JsString]

    Json.obj("id" -> id, "name" -> name, "markets" -> markets, "quality" -> quality, "created_at" -> creationDate)
  }

  def getAllInfoOfPeopleInStartups(startupId: Long) = Action.async {
    WS.url(Application.AngelApi + s"/startup_roles?startup_id=$startupId").get().map { response =>
      val success = response.json \\ "success"
      if (success.size == 0) {
        val roles: JsArray = (response.json \ "startup_roles").as[JsArray]
        var seqAux = Seq.empty[Map[String, String]]
        var futures = Seq.empty[Future[_]]

        for (role <- roles.value) {
          val user: JsValue = (role \ "user").as[JsValue]
          val userRole: String = (role \ "role").as[String]
          val userId: Int = (user \ "id").as[Int]
          futures = futures.+:(getFutureUserInfoById(userId, userRole))

          Await.result(Future.sequence[Any, Seq](futures), Duration.Inf)


          def getFutureUserInfoById(userId: Int, userRole: String) = {
            WS.url(Application.AngelApi + s"/users/$userId").get().map { userResponse =>
              val user = userResponse.json
              val userSuccess = user \\ "success"
              if (userSuccess.size == 0) {
                val name: String = if ((user \ "name").toString() != "null") (user \ "name").as[String] else ""
                val bio: String = if ((user \ "bio").toString() != "null") (user \ "bio").as[String] else ""
                val followerCount: Int = (user \ "follower_count").as[Int]
                val angellistUrl: String = if ((user \ "angellist_url").toString() != "null") (user \ "angellist_url").as[String] else ""
                val image: String = if ((user \ "image").toString() != "null") (user \ "image").as[String] else ""
                val blogUrl: String = if ((user \ "blog_url").toString() != "null") (user \ "blog_url").as[String] else ""
                val onlineBioUrl: String = if ((user \ "online_bio_url").toString() != "null") (user \ "online_bio_url").as[String] else ""
                val twitterUrl: String = if ((user \ "twitter_url").toString() != "null") (user \ "twitter_url").as[String] else ""
                val facebookUrl: String = if ((user \ "facebook_url").toString() != "null") (user \ "facebook_url").as[String] else ""
                val linkedinUrl: String = if ((user \ "linkedin_url").toString() != "null") (user \ "linkedin_url").as[String] else ""
                val whatIBuilt: String = if ((user \ "what_ive_built").toString() != "null") (user \ "what_ive_built").as[String] else ""
                val whatIDo: String = if ((user \ "what_i_do").toString() != "null") (user \ "what_i_do").as[String] else ""
                val investor: Boolean = (user \ "investor").as[Boolean]
                //TODO: se le pueden meter skils , las locations y los roles
                seqAux = seqAux.+:(Map("id" -> userId.toString, "name" -> name, "bio" -> bio, "role" -> userRole,
                  "follower_count" -> followerCount.toString, "angellist_url" -> angellistUrl, "image" -> image,
                  "blog_url" -> blogUrl, "online_bio_url" -> onlineBioUrl, "twitter_url" -> twitterUrl,
                  "facebook_url" -> facebookUrl, "linkedin_url" -> linkedinUrl, "what_ive_built" -> whatIBuilt,
                  "what_i_do" -> whatIDo, "investor" -> investor.toString))
              }
            }
          }

        }

        //AAC: I can confirm this is working with a small amount of pages (2 until 20)
        //TODO: Ensure that this can cope with any amount of pages
        //        for (i <- 2 until 3){
        //          futures = futures.+:(getFutureRolesByPage(i))
        //        }
        //
        //        Await.result(Future.sequence[Any, Seq](futures),Duration.Inf)
        //
        //        def getFutureRolesByPage(page: Int) = {
        //
        //          WS.url(Application.AngelApi + s"/startup_roles?startup_id=$startupId?page=$page").get().map { response =>
        //            print("entre:     "+page)
        //            for (role <- roles.value) {
        //              var user:JsValue= (role \ "user").as[JsValue]
        //              val userRole:String= (role \ "role").as[String]
        //              val userId:Int= (user \ "id").as[Int]
        //              print("user:      "+ userId)
        //              futures = futures.+:(getFutureUserInfoById(userId, userRole))
        //
        //              Await.result(Future.sequence[Any, Seq](futures), Duration.Inf)
        //
        //
        //              def getFutureUserInfoById(userId: Int, userRole:String) = {
        //                WS.url(Application.AngelApi+s"/users/$userId").get().map { userResponse =>
        //                  user= userResponse.json
        //                  val userSuccess= user \\ "success"
        //                  if(userSuccess.size == 0) {
        //                    val name: String = (user \ "name").as[String]
        //                    val bio: String = (user \ "bio").as[String]
        //                    val followerCount: Int = (user \ "follower_count").as[Int]
        //                    val angellistUrl: String = (user \ "angellist_url").as[String]
        //                    val image: String = (user \ "image").as[String]
        //                    val blogUrl: String = (user \ "blog_url").as[String]
        //                    val onlineBioUrl: String = (user \ "online_bio_url").as[String]
        //                    val twitterUrl: String = (user \ "twitter_url").as[String]
        //                    val facebookUrl: String = (user \ "facebook_url").as[String]
        //                    val linkedinUrl: String = (user \ "linkedin_url").as[String]
        //                    val whatIBuilt: String = (user \ "what_ive_built").as[String]
        //                    val whatIDo: String = (user \ "what_i_do").as[String]
        //                    val investor: Boolean = (user \ "investor").as[Boolean]
        //                    //TODO: se le pueden meter skils , las locations y los roles
        //                    seqAux = seqAux.+:(Map("id" -> userId.toString, "name" -> name, "bio" -> bio, "role" -> userRole,
        //                      "follower_count" -> followerCount.toString, "angellist_url" -> angellistUrl, "image" -> image,
        //                      "blog_url" -> blogUrl, "online_bio_url" -> onlineBioUrl, "twitter_url" -> twitterUrl,
        //                      "facebook_url" -> facebookUrl, "linkedin_url" -> linkedinUrl, "what_ive_built" -> whatIBuilt,
        //                      "what_i_do" -> whatIDo, "investor" -> investor.toString))
        //                  }
        //                }
        //              }
        //
        //            }
        //          }
        //
        //        }
        //TODO: q espere a q este el otroo
        print(Json.toJson(seqAux))
        Ok(Json.toJson(seqAux))
      } else {
        Ok(Json.obj("id" -> "error", "msg" -> s"Startup $startupId does not exist"))
      }

    }

  }

  def getStartupsNetwork(locationId: Int, marketId: Int,
                         quality: Int, creationDate: String) = Action.async {
    var startupsToSend: JsArray = JsArray()
    if (locationId != -1) {

      searchByTag(locationId).map { startups =>
        startupsToSend = startups
        if (marketId != -1) startupsToSend = filterArrayByInt(startupsToSend, "markets", "id", marketId)
        if (quality != -1) startupsToSend = filterByInt(startupsToSend, "quality", quality)
        if (creationDate != "") startupsToSend = filterByDate(startupsToSend, "created_at", creationDate)
        startupsToSend = getNetwork(startupsToSend)
        Ok(startupsToSend)
      }
    } else {
      searchByTag(marketId).map { startups =>
        if (quality != -1) startupsToSend = filterByInt(startupsToSend, "quality", quality)
        if (creationDate != "") startupsToSend = filterByDate(startupsToSend, "created_at", creationDate)
        startupsToSend = getNetwork(startupsToSend)
        Ok(startupsToSend)
      }
    }
  }

  def getNetwork(startups: JsArray): JsArray = {
    var result: JsArray = new JsArray()
    var users = new JsArray()
    for (startup <- startups.value) {
      val startupId: Int = (startup \ "id").as[Int]
      val startupName: String = (startup \ "name").as[String]
      var futures = Seq.empty[Future[_]]

      futures = futures.+:(getStartupRolesById(startupId, startupName))

      Await.result(Future.sequence[Any, Seq](futures), Duration.Inf)

      def getStartupRolesById(startupId: Int, startupName: String) = {
        WS.url(Application.AngelApi + s"/startup_roles?startup_id=$startupId").get().map { response =>
          val success = response.json \\ "success"
          if (success.size == 0) {
            val roles: JsArray = (response.json \ "startup_roles").as[JsArray]
            for (role <- roles.value) {
              val user: JsValue = (role \ "user").as[JsValue]
              val userRole: String = (role \ "role").as[String]
              val userId: Int = (user \ "id").as[Int]
              val userName: String = (user \ "name").as[String]
              users = users.+:(Json.obj("userId" -> userId, "startupId" -> startupId,
                "startupName" -> startupName, "userName" -> userName, "userRole" -> userRole))
            }
          }
        }
      }
    }
    for (user <- users.value) {
      val id: Int = (user \ "startupId").as[Int]
      for (user2 <- users.value) {
        val compareId: Int = (user2 \ "startupId").as[Int]
        if (id != compareId) {
          //SI LOS STARTUPS SON DISTINTOS ME FIJO SI EL USUARIO EES EL MISMO
          if ((user \ "userId").as[Int] == (user2 \ "userId").as[Int]) {
            val nameOne: String = (user \ "startupName").as[String]
            val userId: Int = (user \ "userId").as[Int]
            val nameTwo: String = (user2 \ "startupName").as[String]
            val name: String = (user \ "userName").as[String]
            val roleOne: String = (user \ "userRole").as[String]
            val roleTwo: String = (user2 \ "userRole").as[String]
            result = result.+:(Json.obj("startupIdOne" -> id.toString, "startupIdTwo" -> compareId.toString, "startupNameOne" -> nameOne,
              "startupNameTwo" -> nameTwo, "userId" -> userId.toString, "userName" -> name, "roleOne" -> roleOne, "roleTwo" -> roleTwo))
          }
        }
      }
    }

    result
  }

  def getPeopleNetwork(locationId: Int, marketId: Int,
                       quality: Int, creationDate: String) = Action.async{
    var startupsToSend:JsArray = JsArray()
    if(locationId != -1){

      searchByTag(locationId).map { startups =>
        startupsToSend = startups
        if(marketId != -1) startupsToSend = filterArrayByInt(startupsToSend, "markets", "id", marketId)
        if(quality != -1) startupsToSend = filterByInt(startupsToSend, "quality", quality)
        if(creationDate != "") startupsToSend = filterByDate(startupsToSend, "created_at", creationDate)
        startupsToSend = getPNetwork(startupsToSend)
        Ok(startupsToSend)
      }
    } else {
      searchByTag(marketId).map { startups =>
        if(quality != -1) startupsToSend = filterByInt(startupsToSend, "quality", quality)
        if(creationDate != "") startupsToSend = filterByDate(startupsToSend, "created_at", creationDate)
        startupsToSend = getPNetwork(startupsToSend)
        Ok(startupsToSend)
      }
    }
  }

  def getPNetwork(startups:JsArray) : JsArray ={
    var result:JsArray= new JsArray()
    var users = new JsArray()
    for(startup <- startups.value){
      val startupId:Int= (startup \ "id").as[Int]
      val startupName:String= (startup \ "name").as[String]
      var futures = Seq.empty[Future[_]]

      futures= futures.+:(getStartupRolesById(startupId, startupName))

      Await.result(Future.sequence[Any, Seq](futures), Duration.Inf)

      def getStartupRolesById(startupId:Int, startupName:String) = {
        WS.url(Application.AngelApi+s"/startup_roles?startup_id=$startupId").get().map{ response =>
          val success= response.json \\ "success"
          if( success.size == 0) {
            val roles: JsArray = (response.json \ "startup_roles").as[JsArray]
            for(role <- roles.value){
              val user:JsValue= (role \ "user").as[JsValue]
              val userRole:String= (role \ "role").as[String]
              val userId:Int= (user \ "id").as[Int]
              val userName:String= (user \ "name").as[String]
              users= users.+:(Json.obj("userId" -> userId , "startupId" -> startupId,
                "startupName" -> startupName, "userName" -> userName, "userRole" -> userRole))
            }
          }
        }
      }
    }
    for (user <- users.value){
      val id:Int= (user \ "userId").as[Int]
      for (user2 <- users.value) {
        val compareId:Int= (user2 \ "userId").as[Int]
        if(id != compareId){    //SI LOS STARTUPS SON DISTINTOS ME FIJO SI EL USUARIO EES EL MISMO
          if((user \ "startupId").as[Int] == (user2 \ "startupId").as[Int]){
            val nameOne:String= (user \ "userName").as[String]
            val startupId:Int= (user \ "startupId").as[Int]
            val nameTwo:String= (user2 \ "userName").as[String]
            val name:String= (user \ "startupName").as[String]
            val roleOne:String= (user \ "userRole").as[String]
            val roleTwo:String= (user2 \ "userRole").as[String]
            result= result.+:(Json.obj("userIdOne" -> id.toString , "userIdTwo" -> compareId.toString, "userNameOne" -> nameOne,
              "userNameTwo" -> nameTwo, "startupId" -> startupId.toString , "startupName" -> name, "roleOne" -> roleOne, "roleTwo" -> roleTwo))
          }
        }
      }
    }

    result
  }

}
