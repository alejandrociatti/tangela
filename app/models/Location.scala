package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current

import models.Kind.Kind

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 29/05/14
 * Time: 14:04
 */

case class Location(id: Pk[Long] = NotAssigned, name:String, angelId:Long, kind:Kind)

object Location{

  def getCountries:List[Location] = {
    DB.withConnection{implicit connection =>
      SQL("""
         SELECT * FROM Location WHERE kind={kind}
         ORDER BY name
          """).on("kind" -> Kind.COUNTRY.toString).as(locationParser *)
    }
  }

  def save(location: Location) = {
    if(DB.withConnection{implicit connection =>
      SQL("""
        SELECT COUNT(*) AS Count FROM Location WHERE angelId={angelId};
          """).on("angelId"->location.angelId).apply().head[Long]("Count")
    } <= 0){
      DB.withConnection{implicit connection =>
        SQL("""
        INSERT INTO Location(name, angelId, kind)
        VALUES({name}, {angelId}, {kind});
            """).on("name"-> location.name,
            "angelId"->location.angelId,
            "kind"->location.kind.toString).executeInsert(scalar[Pk[Long]] single)
      }
    }
  }

  def saveRelation(locationId:Long, startupId:Long) = {
    DB.withConnection{implicit connection =>
      SQL(
        """
        INSERT INTO Startup_Location(locationId, startupId)
        VALUES({locationId},{startupId})
        """).on("locationId" -> locationId, "startupId"->startupId).execute()
    }
  }

  private val locationParser: RowParser[Location] = {
      get[Pk[Long]]("id") ~
      get[String]("name") ~
      get[Long]("angelId") ~
      get[String]("kind") map {
      case id ~ name ~ angelId ~ kind => Location(id, name, angelId, Kind.withName(kind))
    }
  }

}

object Kind extends Enumeration{
  type Kind = Value
  val COUNTRY = Value("COUNTRY")
}
