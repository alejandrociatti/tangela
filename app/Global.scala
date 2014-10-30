import controllers.{AngelListServices, Markets, Locations}
import models.{Market, Location, Database}
import models.authentication.{Users, Role, User}
import org.joda.time.{LocalDate, LocalTime}
import play.api._
import play.api.db.slick._
import play.libs.Akka
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import scala.slick.lifted.Query
import play.api.db.slick.Config.driver.simple._

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 03/06/14
 * Time: 12:54
 */

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    super.onStart(app)

    //Uncomment for TOR check...
    //AngelListServices.checkTor()

    createAdmin()

    dropTablesCRON()
  }

  def createAdmin() = {
//    val admins = Database.query[User].whereEqual("role", Role.Admin.toString).fetch()
    DB.withSession { implicit  session: scala.slick.session.Session =>
      Query(Users).filter( _.role === Role.Admin.toString).firstOption.getOrElse {
        Users.insert(User("admin", "secret", "Tangela", "Admin", Role.Admin.toString))
      }
    }
  }

  def dropTablesCRON() = {
    //Start On: Today at 3:00 AM (24-hour format)
    val startOn = LocalDate.now().toLocalDateTime(new LocalTime(3,00))
    //Schedule task:
    Akka.system.scheduler.schedule(
      //Initial delay: time remaining from now to startOn(3AM)
      new org.joda.time.Duration(startOn.toDateTime, null).getMillis.millis,
      //Once every day
      1.days
    ){
      //Task:
      //TODO: 'TRUNCATE TABLE X' SQL WHEN WE HAVE THEM
      println("Cron Job just ran.")
    }
  }

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

}
