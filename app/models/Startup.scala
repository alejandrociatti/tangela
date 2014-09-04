package models

import anorm.SqlParser._
import anorm._
import org.joda.time.DateTime
import play.api.db.DB
import play.api.Play.current
import util.AnormExtension._

/**
 * Created by Javi on 5/15/14.
 */
case class Startup(id: Pk[Long] = NotAssigned, name: String, angelId:Long, quality:Int, creationDate:DateTime)

object Startup{

  def save(startup: Startup) = {
    if(DB.withConnection{implicit connection =>
      SQL("""
        SELECT COUNT(*) AS Count FROM Startup WHERE angelId={angelId};
          """).on("angelId"->startup.angelId).apply().head[Long]("Count")
    } <= 0){
      DB.withConnection{implicit connection =>
        SQL("""
        INSERT INTO Startup(name, angelId, quality, creationDate)
        VALUES({name}, {angelId}, {quality}, creationDate);
            """).on("name"-> startup.name,
            "angelId"->startup.angelId,
            "quality"->startup.quality,
            "creationDate"->startup.creationDate).executeInsert(scalar[Pk[Long]] single)
      }
    }
  }

  def search(locationId:Pk[Long], marketId:Pk[Long], quality:Int, creationDate:DateTime):List[Startup] = {
    DB.withConnection{implicit connection =>
      SQL(
        """
          SELECT Startup.id, Startup.name, Startup.angelId, startup.quality, startup.creationDate FROM Startup
          INNER JOIN Startup_Location ON Startup_Location.startupId=Startup.Id
          INNER JOIN Startup_Market ON Startup_Market.startupId=Startup.Id
          WHERE locationId={locationId}
          AND marketId={marketId}
          AND quality>={quality}
        """).on("locationId"-> locationId,
                "marketId"-> marketId,
                "quality"-> quality).as(startupParser *)
    }
  }

  private val startupParser: RowParser[Startup] = {
    get[Pk[Long]]("id") ~
      get[String]("name") ~
      get[Long]("angelId") ~
      get[Int]("quality") ~
      get[DateTime]("creationDate") map {
      case id ~ name ~ angelId ~ quality ~ creationDate => Startup(id, name, angelId, quality, creationDate)
    }
  }
}


