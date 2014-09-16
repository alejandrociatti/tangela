package controllers

import java.io.File

import com.github.tototoshi.csv.CSVWriter
import play.api._
import play.api.mvc._

object Application extends Controller with Secured{
  val AngelApi = "https://api.angel.co/1"


  def index = withAuth { username => implicit request =>
    Ok(views.html.index())
  }
  
  def startupsByLocation = withAuth { username => implicit request =>
    Ok(views.html.startupsByLocation())
  }

  def searchStartups = withAuth{ username => implicit request =>
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
        routes.javascript.Startups.getStartupFunding
      )
    ).as("text/javascript")
  }

  def startupsInfo = withAuth { username => implicit request =>
    Ok(views.html.startupsInfo())
  }

  def testCSV() = Action {
    val file: File = new File("/Users/martingutierrez/Desktop/test.csv")

    writeCSVWithHeaders(file, List("titulo1", "titulo2", "titulo3"), List(List("1", "2", "3"), List("4", "5", "6")))

    Ok.sendFile(file)
  }

  def writeCSVWithHeaders(file: File, headers: List[String], values: List[List[String]]) = {
    val writer = CSVWriter.open(file)

    writer.writeRow(headers)

    writer.writeAll(values)

    writer.close()
  }
}