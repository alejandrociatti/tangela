package controllers

import models.Market

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, ExecutionContext}
import play.api.libs.ws.WS
import ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.libs.json._

object Markets extends Controller{

  def getMarkets = Action{
    loadMarketsToDB()
    Ok(
      Json.toJson("hola")
    )
  }

  def getMarketsByString(marketName:String) = Action.async {
    WS.url(Application.AngelApi +s"/search?type=MarketTag&query=$marketName").get().map{ response =>
      val ids : Seq[JsValue] = response.json \\ "id"
      val names : Seq[JsValue] = response.json \\ "name"

      var seqAux = Seq.empty[Map[String, String]]

      for(i <- 0 until names.size) {
        seqAux = seqAux .+:(Map( "id" -> ids(i).as[Long].toString, "name"->names(i).as[String]))
      }

      seqAux = seqAux.reverse

      Ok(Json.toJson(seqAux))

      //9217 ES EL NUMERO MAGICO
    }
  }

  def loadMarketsToDB() = {
    val magicNumber = 9217
    WS.url(Application.AngelApi + s"/tags/$magicNumber/children").get().map { response =>
      val pages: Int = (response.json \ "last_page").as[Int]
      val markets: JsArray = (response.json \ "children").as[JsArray]

      markets.value.filter { market => (market \ "statistics" \ "direct" \ "startups").as[Int] > 10}.map { market =>
        saveMarketToDB(market)
      }

      var futures = Seq.empty[Future[_]]

      for (i <- 2 until 20) {
        futures = futures.+:(getFutureMarketsByPage(i))
      }

      Await.result(Future.sequence[Any, Seq](futures), Duration.Inf)

      def getFutureMarketsByPage(page: Int) = {
        WS.url(Application.AngelApi + s"/tags/$magicNumber/children?page=$page").get().map { response =>
          val markets: JsArray = (response.json \ "children").as[JsArray]

          markets.value.filter { market => (market \ "statistics" \ "direct" \ "startups").as[Int] > 10}.map { market =>
            saveMarketToDB(market)
          }
        }
      }
    }
  }

  def saveMarketToDB(market:JsValue) = {
    val newMarket = Market((market \ "name").as[String], (market \ "id").as[Long])
    Market.save(newMarket)
  }

}

