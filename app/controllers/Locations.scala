package controllers

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.WS
import play.api.libs.ws
import ExecutionContext.Implicits.global
import models.Location
import anorm.NotAssigned
import play.api.mvc._
import play.api.libs.json.Json


/**
 * User: Martin Gutierrez
 * Date: 22/05/14
 * Time: 20:52
 */
object Locations extends Controller{
  val ANGELAPI = "https://api.angel.co/1"

  def getCountriesMOCK = {
    Seq(new Location(NotAssigned,"Argentina", 1613), new Location(NotAssigned,"Brazil",1622))
  }

  def getCountryIdByName(countryName:String):Future[ws.Response] = {
    WS.url("https://api.angel.co/1/search?type=LocationTag&query=$countryName").get()
  }

  def getChildrenOf(countryId:Long) = Action.async {
    WS.url(ANGELAPI+"/tags/$countryId/children").get().map{response =>
      println(response.json.toString())
      val ids = response.json.\\("id")
      val names = response.json.\\("display_name")
      Ok(Json.toJson(Map(ids, names)))
    }
  }


}