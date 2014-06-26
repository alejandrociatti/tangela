package controllers

import play.api.data.Forms._
import anorm.Pk
import play.api.data.Form
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.WS
import anorm.NotAssigned
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.libs.ws.WS
import play.api.mvc.{Controller, Action}
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import com.fasterxml.jackson.annotation.JsonValue
import models.Startup
import play.api.libs.json.JsArray
import scala.concurrent.duration.Duration

/**
 * Created by Javi on 5/16/14.
 */
object Startups extends Controller with Secured{

  val ANGELAPI = "https://api.angel.co/1"

  val startupForm = Form(
    mapping(
      "id" -> ignored(NotAssigned:Pk[Long]),
      "name" -> nonEmptyText,
      "angelId" -> ignored(0:Long)
    )(Startup.apply)(Startup.unapply)
  )

  /**
   * Responds with a JSON containing the minimum amount of information
   * required to show given startup in a network graph.
   */
  def getStartupNetInfo(startupId:Long) = Action.async {
    WS.url(Application.AngelApi + s"/startups/$startupId").get().map{ response =>
      val resp:JsValue = response.json.as[JsValue]
      val followers: Int = (resp \ "follower_count").as[Int]
      val name: String = (resp \ "name").as[String]
      Ok(Json.toJson(Map("id"->startupId.toString, "follower_count"->followers.toString, "name"->name)))
    }
  }

  /**
   * This method searches for every startup tagged with a given LocationTag
   * @param locationId id of the LocationTag
   * @return JSON response containing an array of {id, name} of each startup
   */
  def getStartupsByLocationId(locationId:Long) = Action.async {
    WS.url(Application.AngelApi + s"/tags/$locationId/startups").get().map{ response =>
      val pages:Int = (response.json \ "last_page" ).as[Int]

      val startups:JsArray = (response.json \ "startups").as[JsArray]

      var seqAux = Seq.empty[Map[String, String]]

      startups.value.filter{ startup => !(startup \ "hidden").as[Boolean] }.map{startup =>
        val id:Int = (startup \ "id").as[Int]
        val name:String = (startup \ "name").as[String]
        seqAux = seqAux .+:(Map("id"->id.toString, "name"->name))
      }

      var futures = Seq.empty[Future[_]]

      //AAC: I can confirm this is working with a small amount of pages (2 until 20)
      //TODO: Ensure that this can cope with any amount of pages
      for (i <- 2 until 20){
        futures = futures.+:(getFutureStartupsByPage(i))
      }

      Await.result(Future.sequence[Any, Seq](futures),Duration.Inf)

      def getFutureStartupsByPage(page: Int) = {
          WS.url(Application.AngelApi + s"/tags/$locationId/startups?page=$page").get().map { response =>
            val startups: JsArray = (response.json \ "startups").as[JsArray]

            startups.value.filter { startup => !(startup \ "hidden").as[Boolean] }.map { startup =>
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
  def loadStartupsByLocationIdToDB(locationId:Long) = { }



  def getNumberOfStartupsFundraising() = withAsyncAuth( username => implicit request =>
    WS.url(Application.AngelApi + "/startups?filter=raising").get().map{ response =>

      val total = (response.json \ "total").as[Int]

      Ok(views.html.fundraisingCount(total))
    }
  )

  def getStartupById(startupId: Long) = Action.async {
    val url: String = ANGELAPI + s"/startups/$startupId"
    WS.url(url).get().map{ response =>

      val success= response.json \\ "success"
      if(success.size == 0) {
        val fundraising = response.json.\\("fundraising")
        Ok(Json.toJson(fundraising))
      } else {
        Ok("No existe el StartUp")
      }
    }
  }

  def getNumberOfFoundersByStartupId(startupId: Long) = Action.async {
    WS.url(Application.AngelApi+s"/startups/$startupId/roles?role=founder" ).get().map{ response =>

      val success= response.json \\ "success"

      if(success.size == 0) {

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
    WS.url(url).get().map{ response =>
      //TODO: que me busque todas las paginas y no solo la primera
      //TODO: que en la vista me aparezcan los botones una vez que hay datos para submitear
      val success= response.json \\ "success"
      if(success.size == 0) {
        val startups: JsArray = response.json.as[JsArray]
        var seqAux= Seq.empty[Map[String, String]]
        for (startup <- startups.value){
          val id:Int= (startup \ "id").as[Int]
          val name: String= (startup \ "name").as[String]
          seqAux = seqAux.+:(Map("id" -> id.toString, "name" -> name))
        }
        Ok(Json.toJson(seqAux))
      } else {
        Ok("No existen startups con este nombre")
      }
    }
  }
  def getRolesOfStartup(startupId: Long) = Action.async {
    WS.url(Application.AngelApi+s"/startups/$startupId/roles").get().map{ response =>

      val roles:JsArray = (response.json \ "startup_roles").as[JsArray]

      val success= response.json \\ "success"

      if(success.size == 0) {

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
        Ok(Json.obj("id"->"error","msg"-> s"Startup $startupId does not exist"))
      }
    }
  }


  def getStartupFunding(startupId: Long) = Action.async {
    val url: String = ANGELAPI + "/startups/" + startupId + "/funding"
    WS.url(url).get().map{ response =>
      val success = response.json \\ "success"
      if (success.size == 0) {
        val fundraising: JsArray = (response.json \ "funding").as[JsArray]
        println(response.json)

        var seqFunding = Seq.empty[Map[String, String]]
        var seqParticipants = Seq.empty[Map[String, String]]

        for(fundraise <- fundraising.value){
          val participants: JsArray = (fundraise \ "participants").as[JsArray]

          val id:Int = (fundraise \ "id").as[Int]
          val round_type:String = (fundraise \ "round_type").as[String]
          val amount:Int = (fundraise \ "amount" ).as[Int]
          val closed_at:String = (fundraise \ "closed_at").as[String]

          for (participant <- participants.value){
            val id:Int = (participant \ "id").as[Int]
            val name:String = (participant \ "name").as[String]
            val aType:String = (participant \ "type").as[String]
            seqParticipants = seqParticipants.+:(Map("id" -> id.toString, "name" -> name, "type" -> aType))
          }

          seqFunding = seqFunding.+:(Map("id" -> id.toString, "round_type" -> round_type, "amount" -> amount.toString,
                "closed_at" -> closed_at, "participants" -> Json.toJson(seqParticipants).toString()))

        }
        Ok(Json.toJson(seqFunding))
      } else {
        Ok(Json.obj("id"->"error","msg"-> s"Startup $startupId does not exist"))
      }

    }
  }
}
