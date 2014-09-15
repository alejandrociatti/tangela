package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 04/09/14
 * Time: 19:04
 */
case class Market(id: Pk[Long] = NotAssigned, name:String, angelId:Long)

object Market{

  def getMarkets:List[Market] = {
    DB.withConnection{implicit connection =>
      SQL("SELECT * FROM Market ORDER BY name").as(marketParser *)
    }
  }

  def save(market: Market) = {
    if(DB.withConnection{implicit connection =>
      SQL("""
        SELECT COUNT(*) AS Count FROM Market WHERE angelId={angelId};
          """).on("angelId"->market.angelId).apply().head[Long]("Count")
    } <= 0){
      DB.withConnection{implicit connection =>
        SQL("""
        INSERT INTO Market(name, angelId)
        VALUES({name}, {angelId});
            """).on("name"-> market.name,
            "angelId"->market.angelId).executeInsert(scalar[Pk[Long]] single)
      }
    }
  }

  def saveRelation(marketId:Long, startupId:Long) = {
    DB.withConnection{implicit connection =>
      SQL(
        """
        INSERT INTO Startup_Market(marketId, startupId)
        VALUES({marketId},{startupId})
        """).on("marketId" -> marketId, "startupId"->startupId).execute()
    }
  }

  private val marketParser: RowParser[Market] = {
    get[Pk[Long]]("id") ~
      get[String]("name") ~
      get[Long]("angelId") map {
      case id ~ name ~ angelId => Market(id, name, angelId)
    }
  }
}
