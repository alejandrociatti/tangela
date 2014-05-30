package controllers

import play.api.data.Forms._
import anorm.{Pk, NotAssigned}
import play.api.data.Form
import models.Startup
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
      "name" -> nonEmptyText
    )(Startup.apply)(Startup.unapply)
  )

  /**
   * Responds with a JSON containing the minimum amount of information
   * required to show given startup in a network graph.
   */
  def getStartupNetInfo(startupId:Long) = Action.async {
    WS.url(Application.AngelApi + s"/startups/$startupId").get().map{ response =>

      val success= response.json \\ "success"
      val resp:JsValue = response.json.as[JsValue]
      if(success.size == 0) {
        val followers: Int = (resp \ "follower_count").as[Int]
        val name: String = (resp \ "name").as[String]

        var seq = Seq.empty[Map[String, String]]
        seq= seq.+:(Map("id"->startupId.toString, "follower_count"->followers.toString, "name"->name))
        Ok(Json.toJson(seq))
      } else {
        Ok(Json.toJson(Map("id"->startupId.toString,"follower_count"->"1000","name"->"SelectedStartup")))
      }
    }
  }

  def getStartupsByLocationId(countryId:Long) = Action.async {
    WS.url(Application.AngelApi + "/tags/" + countryId + "/startups").get().map{ response =>
      val pages:Int = (response.json \ "last_page" ).as[Int]

      val startups:JsArray = (response.json \ "startups").as[JsArray]

      var seqAux = Seq.empty[Map[String, String]]

      var hidden:Boolean = false
      for(start <- startups.value){

        hidden = (start \ "hidden").as[Boolean]

        if(!hidden){
            val id:Int = (start \ "id").as[Int]
            val name:String = (start \ "name").as[String]
            seqAux = seqAux .+:(Map("id"->id.toString, "name"->name))
        }
      }

      var futures = Seq.empty[Future[_]]

      for (i <- 2 until pages){
        futures = futures.+:(getFutureStartupsByPage(i))
      }

      Await.result(Future.sequence[Any, Seq](futures),Duration.Inf)

      def getFutureStartupsByPage(i: Int) = {
          WS.url(Application.AngelApi + "/tags/" + countryId + "/startups?page=" + i).get().map { response =>
            val startups: JsArray = (response.json \ "startups").as[JsArray]

            startups.value.filter { startup => !(startup \ "hidden").as[Boolean] }.map { startup =>
                val id: Int = (startup \ "id").as[Int]
                val name: String = (startup \ "name").as[String]
                seqAux = seqAux.+:(Map("id" -> id.toString, "name" -> name))
            }
          }
      }

      Ok(Json.toJson(seqAux))
    }
  }



  def getNumberOfStartupsFundraising() = withAsyncAuth( username => implicit request =>
    WS.url(Application.AngelApi + "/startups?filter=raising").get().map{ response =>

      val total = (response.json \ "total").as[Int]

      Ok(views.html.fundraisingCount(total))
    }
  )

  def getStartupById(startupId: Long) = Action.async {
    val url: String = ANGELAPI + s"/startups/$startupId"
    println(url)
    WS.url(url).get().map{ response =>
      println(response.json.toString())

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

        seqAux = seqAux.reverse
        println(Json.toJson(seqAux))
        Ok(Json.toJson(seqAux))
      } else {
        Ok("""{["name": "No", "role":"existe", "id": "el ", "follower_count": "StartUp"]}"""")
      }
    }
  }
}
