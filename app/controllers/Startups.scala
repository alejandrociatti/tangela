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
