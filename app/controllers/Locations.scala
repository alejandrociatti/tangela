package controllers


import scala.concurrent.{Future, ExecutionContext}
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
    countries.as[Seq[JsValue]].map { jsValue =>
      val name = (jsValue \ "name").as[String]
      val uriName = URLEncoder.encode(name.replace(" ", "-").toLowerCase, "UTF-8")
      AngelListServices.searchLocationBySlug(uriName).map { jsResponse =>
        try {
          Location.save(Location((jsResponse \ "name").as[String], (jsResponse \ "id").as[Long], Kind.Country.toString))
        } catch {
          case e: Exception =>
        }
      }
    }
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

  def getChildrenOf(locationId:Long) = Action.async {

    val childrenFuture = AngelListServices.getChildrenOfTag(locationId).flatMap { response =>
      val firstPage = (response \ "children").as[Seq[JsValue]]

      Future.sequence(
        (2 to (response \ "last_page").as[Int]).map{page =>
          AngelListServices.getChildrenOfTagAndPage(locationId)(page).map { response =>
            (response \ "children").as[Seq[JsValue]]
          }
        }
      ).map(_.flatten).map(firstPage ++ _)
    }

    childrenFuture.map{ children :Seq[JsValue] =>
      // Save used locations to DB so Networks.generateDescription can access their names
      Future( children.map( locationJs =>
          Location.save(Location((locationJs \ "name").as[String], (locationJs \ "id").as[Long], Kind.Other.toString()))
      ))
      Ok(JsArray(children)) // the Location.save above is wrapped in Future so we instantly respond with the Locations
    }
  }
}