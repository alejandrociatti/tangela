package controllers

import play.api.data.Forms._
import anorm.{Pk, NotAssigned}
import play.api.data.Form
import models.Startup
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.libs.ws.WS
import play.api.mvc.{Controller, Action}
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global


/**
 * Created by Javi on 5/16/14.
 */
object Startups extends Controller{

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
}
