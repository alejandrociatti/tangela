package controllers

import scala.concurrent.ExecutionContext
import play.api.libs.ws.WS
import ExecutionContext.Implicits.global
import models.{Kind, Location}
import play.api.mvc._
import play.api.libs.json._
import java.net.URLEncoder


/**
 * User: Martin Gutierrez
 * Date: 22/05/14
 * Time: 20:52
 */

object Locations extends Controller{

  def getCountries = Action {
    loadCountriesToDB
    Ok(
      Json.parse(
        scala.io.Source.fromFile("public/data/countries.json").getLines().mkString
      )
    )
  }

  def loadCountriesToDB = {
    val countries = Json.parse(scala.io.Source.fromFile("public/data/countries-reduced.json").getLines().mkString)
    val names:Seq[JsValue] = countries \\ "name"
    for(i <- 0 until names.size){
      //Next line turns strings like "Sri Lanka" to sri-lanka, so country names match their AngelList 'slugs'
      val uriName = URLEncoder.encode(names(i).as[String].replace(" ", "-").toLowerCase, "UTF-8")
      WS.url(Application.AngelApi+s"/search/slugs?type=LocationTag&query=$uriName").get().map{ response =>
        val id = (response.json \ "id").as[Long]
        val name = (response.json \ "name").as[String]
        val newLocation = Location(name, id, Kind.Country.toString)
        Location.save(newLocation)
      }
    }
  }

  def getCountriesByString(countryName:String) = Action.async {
    WS.url(Application.AngelApi +s"/search?type=LocationTag&query=$countryName").get().map{ response =>
      val ids : Seq[JsValue] = response.json \\ "id"
      val names : Seq[JsValue] = response.json \\ "name"

      var seqAux = Seq.empty[Map[String, String]]

      for(i <- 0 until names.size) {
        seqAux = seqAux .+:(Map( "id" -> ids(i).as[Long].toString, "name"->names(i).as[String]))
      }

      seqAux = seqAux.reverse

      Ok(Json.toJson(seqAux))

      //1643 ES EL NUMERO MAGICO
    }
  }

  def getChildrenOf(countryId:Long) = Action.async {
    WS.url(Application.AngelApi+s"/tags/$countryId/children").get().map{response =>
      val ids:Seq[JsValue] = response.json \\ "id"
      val names:Seq[JsValue] = response.json \\ "display_name"
      var seqAux = Seq.empty[Map[String, String]]
      for(i <- 0 until ids.size){
        seqAux = seqAux .+:(Map("id"->ids(i).as[Long].toString, "name"->names(i).as[String]))
      }
      Ok(Json.toJson(seqAux))
    }
  }


}