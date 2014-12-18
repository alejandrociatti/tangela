package controllers

import play.api.Logger

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, ExecutionContext}
import ExecutionContext.Implicits.global
import models.{Kind, Location}
import play.api.mvc._
import play.api.libs.json._
import java.net.URLEncoder

import scala.io.Source

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
    val countriesFile = Source.fromFile("storedJsons/countries-reduced.json", "UTF-8")
    val countries = Json.parse(countriesFile.getLines().mkString)
    countriesFile.close()
    val responses = countries.as[Seq[JsValue]].map { jsValue =>
      val name = (jsValue \ "name").as[String]
      val uriName = URLEncoder.encode(name.replace(" ", "-").toLowerCase, "UTF-8")
      AngelListServices.searchLocationBySlug(uriName).map { jsResponse =>
        try {
          Location.save(Location((jsResponse \ "name").as[String], (jsResponse \ "id").as[Long], Kind.Country.toString))
        } catch {
          case e: Exception => Logger.warn(s"Country $name was not found in Angel List.")
        }
      }
    }
    Await.ready(Future.sequence(responses), Duration.Inf)
  }

  def getCountriesByString(countryName:String) = Action.async {
    AngelListServices.searchLocationByName(countryName) map{ jsResponse =>
      //1643 ES EL NUMERO MAGICO
      Ok(
        Json.toJson(
          jsResponse.as[Seq[JsValue]].map{ jsValue =>
            Json.obj("id" -> jsValue \ "id", "name" -> jsValue \ "name")
          }
        )
      )
    }
  }

  def getChildrenOf(countryId:Long) = Action.async {
    AngelListServices.getChildrenOfTag(countryId) map{ jsResponse =>
      Ok(
        Json.toJson(
          jsResponse.as[Seq[JsValue]].map{ jsValue =>
            Json.obj("id" -> jsValue \ "id", "name" -> jsValue \ "name")
          }
        )
      )
    }
  }


}