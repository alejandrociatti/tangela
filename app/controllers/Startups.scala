package controllers

import play.api.data.Forms._
import anorm.{Pk, NotAssigned}
import play.api.data.Form
import models.Startup
import scala.concurrent.{ExecutionContext, Future}
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

/**
 * Created by Javi on 5/16/14.
 */
object Startups extends Controller{

  val ANGELAPI = "https://api.angel.co/1"

  val startupForm = Form(
    mapping(
      "id" -> ignored(NotAssigned:Pk[Long]),
      "name" -> nonEmptyText
    )(Startup.apply)(Startup.unapply)
  )

  def getStartupsByLocationId(countryId:Int) = Action.async {
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

      seqAux = seqAux.reverse

      Ok(Json.toJson(seqAux))
    }
  }

  def getStartupById(sturtupId: Long) = Action.async {
    val url: String = ANGELAPI + "/startups/" + sturtupId
    println(url)
    WS.url(url).get().map{ response =>
      println(response.json.toString())
      val fundraising = response.json.\\("fundraising")
      Ok(Json.toJson(fundraising))

      
    }
  }
}
