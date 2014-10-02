package models

import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import play.api.Play.current
import scala.slick.session.Session

/**
  * Created with IntelliJ IDEA by: alejandro
 * Date: 29/05/14
 * Time: 14:04
 */

case class Location(name: String, angelId: Long, kind: String, id: Option[Long] = None)

object Locations extends Table[Location]("LOCATION") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def name = column[String]("NAME", O.NotNull)
  def angelId = column[Long]("ANGEL_ID", O.NotNull)
  def kind = column[String]("KIND", O.NotNull)
  def * = name ~ angelId ~ kind ~ id.? <> (Location.apply _, Location.unapply _)
}

object Location {
  import models.Kind.Country

//  def getById(id: Long): Option[Location] = Database.query[Location].whereEqual("id", id).fetchOne()
  def getById(id: Long): Option[Location] = DB.withSession { implicit  session: Session =>
    Query(Locations).filter( _.id === id).firstOption
  }


  def clearAll() = DB.withSession { implicit  session: Session =>
    Query(Locations).delete
  }
//  def getCountries: List[Location] =
//    Database.query[Location].whereEqual("kind", Country.toString).fetch().toList

  def getCountries: List[Location] = DB.withSession { implicit  session: Session =>
    Query(Locations).filter( _.kind === Kind.Country.toString ).sortBy(_.name).list
  }

//  def getOtherThanCountries: List[Location] =
//    Database.query[Location].whereNotEqual("kind", Country.toString).fetch().toList

  def getOtherThanCountries: List[Location] = DB.withSession { implicit  session: Session =>
    Query(Locations).filter( _.kind === Kind.Country.toString ).list
  }

//  def save(location: Location) =
//    if (Database.query[Location].whereEqual("angelId", location.angelId).count() == 0) {
//      Database.save(location)
//    }

  def save(location: Location) = DB.withSession { implicit  session: Session =>
    Query(Locations).filter( _.angelId === location.angelId ).firstOption.getOrElse {
      Locations.insert(location)
    }
  }

//  def saveRelation(locationId: Long, startupId: Long) =
//    getById(locationId) map { location =>
//      Startup.getById(startupId) map { startup =>
//        Database.save(StartupLocation(startup, location))
//      }
//    }

  def saveRelation(locationId: Long, startupId: Long) = DB.withSession { implicit session: Session =>
    StartupLocations.insert(StartupLocation(startupId, locationId))
  }
}

case class StartupLocation(startup: Long, location: Long, id: Option[Long] = None)

object StartupLocations extends Table[StartupLocation]("STARTUP_LOCATION") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def startup = column[Long]("STARTUP_ID", O.NotNull)
  def location = column[Long]("LOCATION_ID", O.NotNull)
  def * = startup ~ location ~ id.? <> (StartupLocation.apply _, StartupLocation.unapply _)
}

object StartupLocation {
}

object Kind extends Enumeration {
  type Kind = Value
  val Country = Value("COUNTRY")
}
