import java.io.File
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.UUID

import controllers.{Locations, Markets, Networks, Startups}
import models.authentication.{Role, User, Users}
import models._
import org.apache.commons.io.FileUtils
import org.joda.time.DateTimeConstants.SUNDAY
import org.joda.time.{DateTime, LocalDate, LocalTime}
import play.api.Application
import play.api.Play.current
import play.api._
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick._
import play.api.libs.json.JsValue
import play.api.mvc.SimpleResult
import play.libs.Akka

import scala.concurrent.{Future, Await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.slick.lifted.Query

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 03/06/14
 * Time: 12:54
 */

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    super.onStart(app)
    DatabaseUpdate.getLastFolderOption.getOrElse({
      DatabaseUpdate.save(DatabaseUpdate(DateTime.now(), UUID.randomUUID().toString))
    })
    createAdmin()
    populateCountries()
    populateMarket()
  }

  def createAdmin() = {
    //val admins = Database.query[User].whereEqual("role", Role.Admin.toString).fetch()
    DB.withSession { implicit  session: scala.slick.session.Session =>
      Query(Users).filter( _.role === Role.Admin.toString).firstOption.getOrElse {
        Users.insert(User("admin", "secret", "Tangela", "Admin", Role.Admin.toString))
      }
    }
  }

  def dropTablesCRON() = {
    //Start On: Today at 3:00 AM (24-hour format)
    val startDay = calculateNextFriday(LocalDate.now())
    val startTime = LocalTime.fromMillisOfDay(10800000l)
    val startDate = startDay.toDateTime(startTime)

    val initialDelay = new org.joda.time.Duration(null, startDate).getMillis.millis
    val repeatDelay = new org.joda.time.Duration(2419200000l /* 4 weeks*/).getMillis.millis

    Akka.system.scheduler.schedule(initialDelay, repeatDelay) { () =>
      Logger.info("Cron Job just ran.")
      DatabaseUpdate.save(DatabaseUpdate(DateTime.now(), UUID.randomUUID().toString))
      populateCountries()
      populateMarket()
      clearCSVs()
      loadNetworks()
    }
  }

  private def calculateNextFriday(initialDate: LocalDate): LocalDate  =
    if (initialDate.getDayOfWeek < SUNDAY) {
      initialDate
    } else {
      initialDate.plusWeeks(1)
    }
    .withDayOfWeek(SUNDAY)


  def populateCountries() = {
    Location.clearAll()
    Locations.loadCountriesToDB()
    Logger.info("Countries Loaded!")
  }

  def populateMarket() = {
    Market.clearAll()
    Markets.loadMarketsToDB()
    Logger.info("Markets Loaded!")
  }
//
//  def loadSomeStates(): Unit = {
//    val ids: Seq[Int] = Seq(1624, 1708, 1699, 1855, 1666, 2071, 1839, 1686, 1699, 1841, 1837)
//    def makeNetwork(id: Int): Unit = {
//      val network = Networks.getStartupsNetworkInternal(id, -1, "", "")
//      Await.ready(network, Duration.Inf)
//      network.onFailure {
//        case e => e.printStackTrace()
//      }
//      Logger.info("Network \"" + id + "\" saved.")
//    }
//    ids foreach makeNetwork
//  }

  def loadNetworks() = {
    def loadLocation(location: Location) = Future({
      Await.ready(Networks.getNetworksToLoad(location), Duration.Inf)
      Logger.info("Location \""+location.name+"\" loaded.")
    })
    Location.getCountries foreach {
      case country@Location("United States", _, _, _) => country.getChildren.map(_.map{loc =>loadLocation(loc)})
      case country => loadLocation(country)
    }
  }

  def clearCSVs() = {
    val directory = new File("storedCSVs")
    FileUtils.deleteDirectory(directory)
  }
}
