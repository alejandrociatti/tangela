package models

import sorm.Entity

/**
  * Created with IntelliJ IDEA by: alejandro
 * Date: 29/05/14
 * Time: 14:04
 */

case class Location(name: String, angelId: Long, kind: String)

object Location {
  import models.Kind.Country

  def getEntity = Entity[Location]()

  def getById(id: Long): Option[Location] = Database.query[Location].whereEqual("id", id).fetchOne()

  def getCountries: List[Location] =
    Database.query[Location].whereEqual("kind", Country.toString).fetch().toList

  def getOtherThanCountries: List[Location] =
    Database.query[Location].whereNotEqual("kind", Country.toString).fetch().toList

  def save(location: Location) =
    if (Database.query[Location].whereEqual("angelId", location.angelId).count() == 0) {
      Database.save(location)
    }

  def saveRelation(locationId: Long, startupId: Long) =
    getById(locationId) map { location =>
      Startup.getById(startupId) map { startup =>
        Database.save(StartupLocation(startup, location))
      }
    }
}

case class StartupLocation(startup: Startup, location: Location)

object StartupLocation {
  def getEntity = Entity[StartupLocation]()
}

object Kind extends Enumeration {
  type Kind = Value
  val Country = Value("COUNTRY")
}
