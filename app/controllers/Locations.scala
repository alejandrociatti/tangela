package controllers

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.WS
import play.api.libs.ws
import ExecutionContext.Implicits.global
import models.Location
import anorm.NotAssigned
import play.api.mvc._
import play.api.libs.json.{JsValue, Json}
import scala.collection


/**
 * User: Martin Gutierrez
 * Date: 22/05/14
 * Time: 20:52
 */
object Locations extends Controller{

  def getCountriesMOCK = {
    Seq(new Location(NotAssigned,"Argentina", 1613), new Location(NotAssigned,"Brazil",1622))
  }

  def getCountriesByString(countryName:String) = Action.async {
    WS.url(Application.AngelApi +s"/search?type=LocationTag&query=$countryName").get().map{ response =>
      val ids : Seq[JsValue] = response.json \\ "id"
      val names : Seq[JsValue] = response.json \\ "name"

      var seqAux = Seq.empty[Map[String, String]]

      for(i <- 0 until names.size) {
        seqAux = seqAux .+:(Map("id"->ids(i).as[Int].toString, "name"->names(i).as[String]))
      }

      seqAux = seqAux.reverse

      Ok(Json.toJson(seqAux))

      //1643 ES EL NUMERO MAGICO
    }
  }

  /*def getChildrenOf(countryId:Long) = Action.async {
    WS.url(Application.AngelApi+s"/tags/$countryId/children").get().map{response =>
      println(response.json.toString())
      val ids = response.json.\\("id")
      val names = response.json.\\("display_name")
      Ok(Json.toJson(ids))
    }
  }*/


}