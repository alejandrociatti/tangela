package models

import sorm.Entity

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 04/09/14
 * Time: 19:04
 */

case class Market(name: String, angelId: Long)

object Market {
  def getEntity = Entity[Market]()

  def getMarkets = Database.query[Market].fetch().toList

  def getById(id: Long): Option[Market] = Database.query[Market].whereEqual("id", id).fetchOne()

  def save(market: Market) =
    if (Database.query[Market].whereEqual("angelId", market.angelId).count() == 0) {
      Database.save(market)
    }

  def saveRelation(marketId: Long, startupId: Long) =
    getById(marketId) map { market =>
      Startup.getById(startupId) map { startup =>
        Database.save(StartupMarket(market, startup))
      }
    }
}

case class StartupMarket(market: Market, startup: Startup)

object StartupMarket {
  def getEntity = Entity[StartupMarket]()
}
