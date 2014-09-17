package controllers

import java.io._

import com.github.tototoshi.csv.CSVWriter
import play.api._
import play.api.libs.iteratee.Enumerator
import play.api.mvc._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

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
    writeCSVWithHeaders(List("titulo1", "titulo2", "titulo3"), List(List("1", "2", "3"), List("4", "5", "6")))
  }

  def writeCSVWithHeaders(headers: List[String], values: List[List[String]]): SimpleResult = {
    val byteArrayOutputStream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val streamWriter: OutputStreamWriter = new OutputStreamWriter(byteArrayOutputStream)

    val writer = CSVWriter.open(streamWriter)

    writer.writeRow(headers)

    writer.writeAll(values)

    writer.close()

    val bytes: Array[Byte] = byteArrayOutputStream.toByteArray

    val streamReader: InputStream = new BufferedInputStream(new ByteArrayInputStream(bytes))

    Ok.chunked(Enumerator.fromStream(streamReader)).as("text/csv")
  }
}