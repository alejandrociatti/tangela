package controllers

import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Await, ExecutionContext}
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
    loadCountriesToDB()
    Ok("Countries loaded!")
  }

  def loadCountriesToDB() = {
    val countries = Json.parse(scala.io.Source.fromFile("storedJsons/countries-reduced.json").getLines().mkString)
    val names:Seq[JsValue] = countries \\ "name"
    val locations = (0 until names.size) map { i =>
      //Next line turns strings like "Sri Lanka" to sri-lanka, so country names match their AngelList 'slugs'
      val uriName = URLEncoder.encode(names(i).as[String].replace(" ", "-").toLowerCase, "UTF-8")
        AngelListServices.searchLocationBySlug(uriName) map { jsResponse =>
          try {
            val id = (jsResponse \ "id").as[Long]
            val name = (jsResponse \ "name").as[String]
            val newLocation = Location(name, id, Kind.Country.toString)
            Location.save(newLocation)
          } catch {
            case e: Exception =>
          }
        }
    }
    Await.result( Future.sequence(locations), Duration.Inf
      )
  }

  def getCountriesByString(countryName:String) = Action.async {
    AngelListServices.searchLocationByName(countryName) map{ jsResponse =>
      val ids : Seq[JsValue] = jsResponse \\ "id"
      val names : Seq[JsValue] = jsResponse \\ "name"

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
    AngelListServices.getChildrenOfTag(countryId) map{ jsResponse =>
      val ids:Seq[JsValue] = jsResponse \\ "id"
      val names:Seq[JsValue] = jsResponse \\ "display_name"
      var seqAux = Seq.empty[Map[String, String]]
      for(i <- 0 until ids.size){
        seqAux = seqAux .+:(Map("id"->ids(i).as[Long].toString, "name"->names(i).as[String]))
      }
      Ok(Json.toJson(seqAux))
    }
  }


}