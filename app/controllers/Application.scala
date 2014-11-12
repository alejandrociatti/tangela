package controllers

import java.io._

import models.authentication.Role._
import com.github.tototoshi.csv.CSVWriter
import play.api._
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.JsArray
import play.api.mvc._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

object Application extends Controller with Secured{

  def index = withAuth(Admin, Researcher) { user => implicit request =>
    Ok(views.html.index())
  }
  
  def startupsByLocation = withAuth(Admin, Researcher) { user => implicit request =>
    Ok(views.html.startupsByLocation())
  }

  def searchStartups = withAuth(Admin, Researcher) { user => implicit request =>
    Ok(views.html.search())
  }

  def javascriptRoutes = Action { implicit request =>
    Ok(
      Routes.javascriptRouter("jsRoutes")(
        routes.javascript.Locations.getCountriesByString,
        routes.javascript.Locations.getCountries,
        routes.javascript.Locations.getChildrenOf,
        routes.javascript.Startups.getStartupsByLocationId,
        routes.javascript.Startups.getNumberOfFoundersByStartupId,
        routes.javascript.Startups.getRolesOfStartup,
        routes.javascript.Startups.getStartupNetInfo,
        routes.javascript.Startups.getStartupsByName,
        routes.javascript.Startups.getStartupFunding,
        routes.javascript.Startups.startupCriteriaSearch,
        routes.javascript.Networks.getStartupsNetwork,
        routes.javascript.Networks.getStartupsNetworkCSV,
        routes.javascript.Networks.getPeopleNetwork,
        routes.javascript.Networks.getPeopleNetworkCSV,
        routes.javascript.Startups.getAllInfoOfPeopleInStartups,
        routes.javascript.Startups.startupsFundingByCriteria,
        routes.javascript.Application.tableToCSV
      )
    ).as("text/javascript")
  }

  def startupsInfo = withAuth(Admin, Researcher) { user => implicit request =>
    Ok(views.html.startupsInfo())
  }

  def startupsPeopleInfo = withAuth(Admin, Researcher) { user => implicit request =>
    Ok(views.html.startupsPeopleInfo())
  }

  def startupsFundingInfo() = withAuth(Admin, Researcher) { username => implicit request =>
    Ok(views.html.startupsFundingInfo())
  }

  def startupsNetwork = withAuth(Admin, Researcher) { user => implicit request =>
    Ok(views.html.startupNetwork())
  }

  def startupsPeopleNetwork = withAuth(Admin, Researcher) { username => implicit request =>
    Ok(views.html.startupsPeopleNetwork())
  }

  def tableToCSV = Action { request =>
    request.body.asJson.fold(Ok("No Data Available")) { json =>
      val headers: List[String] = (json \ "headers").as[JsArray].value.map(value => value.as[String]).toList
      val values: List[List[String]] = (json \ "values").as[JsArray].value.map(array => array.as[JsArray].value.map(value => value.as[String]).toList).toList

      writeCSVWithHeaders(headers, values)
    }
  }

  def writeCSVWithHeaders(headers: List[String], values: List[List[String]]): SimpleResult = {
    val byteArrayOutputStream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val writer = CSVWriter.open(new OutputStreamWriter(byteArrayOutputStream))
    writer.writeRow(headers)
    writer.writeAll(values)
    writer.close()
    val streamReader: InputStream = new BufferedInputStream(new ByteArrayInputStream(
      byteArrayOutputStream.toByteArray
    ))
    Ok.chunked(Enumerator.fromStream(streamReader)).as("text/csv")
  }


}