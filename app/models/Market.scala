package models

import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import play.api.Play.current
import scala.slick.session.Session

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 04/09/14
 * Time: 19:04
 */

case class Market(name: String, angelId: Long, id: Option[Long] = None)

object Markets extends Table[Market]("MARKET") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def name = column[String]("NAME", O.NotNull)
  def angelId = column[Long]("ANGEL_ID", O.NotNull)
  def * = name ~ angelId ~ id.? <> (Market.apply _ , Market.unapply _)
}

object Market {

  def getMarkets = DB.withSession { implicit  session: Session =>
    Query(Markets).sortBy(_.name).list
  }

  def clearAll() = DB.withSession { implicit session: Session =>
    Query(Markets).delete
  }

  def getById(id: Long): Option[Market] = DB.withSession { implicit  session: Session =>
    Query(Markets).filter( _.id === id ).firstOption
  }

  def save(market: Market) = DB.withSession { implicit session: Session =>
    Query(Markets).filter( _.angelId === market.angelId ).firstOption.getOrElse {
      Markets.insert(market)
    }
  }

  def saveRelation(marketId: Long, startupId: Long) = DB.withSession { implicit session: Session =>
    StartupMarkets.insert(StartupMarket(marketId, startupId))
  }
}

case class StartupMarket(market: Long, startup: Long, id: Option[Long] = None)

object StartupMarkets extends Table[StartupMarket]("STARTUP_MARKET") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def market = column[Long]("MARKET_ID", O.NotNull)
  def startup = column[Long]("STARTUP_ID", O.NotNull)
  def * = market ~ startup ~ id.? <> (StartupMarket.apply _, StartupMarket.unapply _)
}

object StartupMarket {}
