package controllers

import models.Startup
import org.joda.time.DateTime
import models.authentication.Role._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration.Duration

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
    AngelListServices.getStartupsByTagId(locationId) map { response =>
      val pages: Int = (response \ "last_page").as[Int]

      val startups: JsArray = (response \ "startups").as[JsArray]

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
        AngelListServices.getStartupsByTagIdAndPage(locationId)(page) map { response =>
          val startups: JsArray = (response \ "startups").as[JsArray]

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
    AngelListServices.getStartupsWithFoundRaising map { response =>
      val total = (response \ "total").as[Int]
      Ok(views.html.fundraisingCount(total))
    }
  }

  def getStartupById(startupId: Long) = Action.async {
    AngelListServices.getStartupById(startupId).map { jsResponse =>
      println("jsResponse = " + jsResponse)
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

      val success = response \\ "success"

      if (success.size == 0) {

        val roles: JsArray = (response \ "startup_roles").as[JsArray]

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
    AngelListServices.getFundingByStartupId(startupId) map { response =>
      val success = response \\ "success"
      if (success.size == 0) {
        val funding: JsArray = (response \ "funding").as[JsArray]

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
    var startupsToReturn: JsArray = JsArray()

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
//    Filter by location and market
    val initialStartupsToSend = if (locationId == -1) {
      searchByTag(marketId)
    } else {
      searchByTag(locationId).map { startupsByLocation =>
        if (marketId != -1) filterArrayByInt(startupsByLocation, "markets", "id", marketId)
        else startupsByLocation
      }
    }

//    Filter by quality and creationDate
    initialStartupsToSend map { startups =>
      val filteredByQuality = if (quality != -1) filterByInt(startups, "quality", quality) else startups
      if (creationDate != "") filterByDate(filteredByQuality, "created_at", creationDate) else filteredByQuality
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

  /**
   * This method return all not hidden startups that are associated to a tag
   * @param tagId id of the Tag
   * @return A Future with a JsArray of the startups
   */

  def searchByTag(tagId: Long): Future[JsArray] = {
    AngelListServices.getStartupsByTagId(tagId) flatMap { response =>

      def responseToStartupSeq(response: JsValue): Seq[JsValue] =
        (response \ "startups").as[JsArray].value
          .filter { startup => !(startup \ "hidden").as[Boolean]}
          .map(getRelevantStartupInfo)

      def responsesToJsArray(responses: Seq[JsValue]) = JsArray(responses flatMap responseToStartupSeq)

      val pages: Int = (response \ "last_page").as[Int]
      val futureResponses = (2 to Math.min(5, pages)) map AngelListServices.getStartupsByTagIdAndPage(tagId)
      val startups = Future.sequence[JsValue, Seq](futureResponses) map responsesToJsArray

      // Add first page to result
      startups map { startups => JsArray(responseToStartupSeq(response)) ++ startups}
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
    AngelListServices.getRolesFromStartupId(startupId) map { response =>
      val success = response \\ "success"
      if (success.size == 0) {
        val roles: JsArray = (response \ "startup_roles").as[JsArray]
        var seqAux = Seq.empty[Map[String, String]]
        var futures = Seq.empty[Future[_]]

        for (role <- roles.value) {
          val user: JsValue = (role \ "user").as[JsValue]
          val userRole: String = (role \ "role").as[String]
          val userId: Int = (user \ "id").as[Int]
          futures = futures.+:(getFutureUserInfoById(userId, userRole))

          Await.result(Future.sequence[Any, Seq](futures), Duration.Inf)


          def getFutureUserInfoById(userId: Int, userRole: String) = {
            AngelListServices.getUserById(userId) map { userResponse =>
              val user = userResponse
              val userSuccess = user \\ "success"
              if (userSuccess.size == 0) {
                val name: String = (user \ "name").asOpt[String].getOrElse("")
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
        val startups2 = startupsToSend
        startupsToSend = getNetwork(startupsToSend)
        Ok("["+startupsToSend+","+startups2+"]")
      }
    } else {
      searchByTag(marketId).map { startups =>
        if (quality != -1) startupsToSend = filterByInt(startupsToSend, "quality", quality)
        if (creationDate != "") startupsToSend = filterByDate(startupsToSend, "created_at", creationDate)
        val startups2 = startupsToSend
        startupsToSend = getNetwork(startupsToSend)
        Ok("["+startupsToSend+","+startups2+"]")
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
        AngelListServices.getRolesFromStartupId(startupId) map { response =>
          val success = response \\ "success"
          if (success.size == 0) {
            val roles: JsArray = (response \ "startup_roles").as[JsArray]
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
            var possible:Boolean= true
            for(aux <- result.value){
              if(id.toString == (aux \ "startupIdTwo").as[String] && compareId.toString == (aux \ "startupIdOne").as[String] ){
                possible= false
              }
            }
            if(possible) {
              val nameOne: String = (user \ "startupName").as[String]
              val userId: Int = (user \ "userId").as[Int]
              val nameTwo: String = (user2 \ "startupName").as[String]
              val name: String = (user \ "userName").as[String]
              val roleOne: String = (user \ "userRole").as[String]
              val roleTwo: String = (user2 \ "userRole").as[String]
              result = result.+:(Json.obj("startupIdOne" -> id.toString, "startupIdTwo" -> compareId.toString,
                "startupNameOne" -> nameOne, "startupNameTwo" -> nameTwo, "userId" -> userId.toString, "userName" -> name,
                "roleOne" -> roleOne, "roleTwo" -> roleTwo))
            }
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
        val startups2 = startupsToSend
        startupsToSend = getPNetwork(startupsToSend)
        Ok("["+startupsToSend+","+startups2+"]")
      }
    } else {
      searchByTag(marketId).map { startups =>
        if(quality != -1) startupsToSend = filterByInt(startupsToSend, "quality", quality)
        if(creationDate != "") startupsToSend = filterByDate(startupsToSend, "created_at", creationDate)
        val startups2 = startupsToSend
        startupsToSend = getPNetwork(startupsToSend)
        Ok("["+startupsToSend+","+startups2+"]")
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
        AngelListServices.getRolesFromStartupId(startupId) map { response =>
          val success= response \\ "success"
          if( success.size == 0) {
            val roles: JsArray = (response \ "startup_roles").as[JsArray]
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
            var possible:Boolean= true
            for(aux <- result.value){
              if(id.toString == (aux \ "userIdTwo").as[String] && compareId.toString == (aux \ "userIdOne").as[String] ){
                possible= false
              }
            }
            if(possible) {
              val nameOne: String = (user \ "userName").as[String]
              val startupId: Int = (user \ "startupId").as[Int]
              val nameTwo: String = (user2 \ "userName").as[String]
              val name: String = (user \ "startupName").as[String]
              val roleOne: String = (user \ "userRole").as[String]
              val roleTwo: String = (user2 \ "userRole").as[String]
              result = result.+:(Json.obj("userIdOne" -> id.toString, "userIdTwo" -> compareId.toString, "userNameOne" -> nameOne,
                "userNameTwo" -> nameTwo, "startupId" -> startupId.toString, "startupName" -> name, "roleOne" -> roleOne, "roleTwo" -> roleTwo))
            }
          }
        }
      }
    }

    result
  }

}
