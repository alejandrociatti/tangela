import java.io.File
import java.util.UUID

import controllers.{Locations, Markets, Networks, Startups}
import models.authentication.{Role, User, Users}
import models.{DatabaseUpdate, Location, Market}
import org.apache.commons.io.FileUtils
import org.joda.time.DateTimeConstants.SUNDAY
import org.joda.time.{DateTime, LocalDate, LocalTime}
import play.api.Play.current
import play.api._
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick._
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
    clearCSVs()
    loadNetworks()
    dropTablesCRON()
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
      println("Cron Job just ran.")
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
    println("Countries Loaded!")
  }

  def populateMarket() = {
    Market.clearAll()
    Markets.loadMarketsToDB()
    println("Markets Loaded!")
  }

  def loadNetworks() = {
    Future({
      println("Loading countries.")
      Location.getCountries foreach { country =>
        Await.ready(Networks.getStartupsNetworkToLoad(country.angelId.toInt, -1, -1, ""), Duration.Inf)
        Await.ready(Startups.getUsersInfoByCriteriaToLoad(country.angelId.toInt, -1, -1, ""), Duration.Inf)

        val name = country.name
        println(s"Country $name loaded.")
      }
    })
  }

  def clearCSVs() = {
    val directory = new File("storedCSVs")
    FileUtils.deleteDirectory(directory)
  }
}
