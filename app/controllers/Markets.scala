package controllers

import models.Market

import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.libs.json._

object Markets extends Controller{

  def getMarkets = Action{
    loadMarketsToDB()
    Ok(
      Json.toJson("Markets loaded")
    )
  }

  def getMarketsByString(marketName:String) = Action.async {
    AngelListServices.searchMarketByName(marketName) map{ jsResponse =>
      val ids : Seq[JsValue] = jsResponse \\ "id"
      val names : Seq[JsValue] = jsResponse \\ "name"

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
    AngelListServices.getChildrenOfTag(magicNumber).map { jsResponse =>
      val pages: Int = (jsResponse \ "last_page").as[Int]

      (jsResponse \ "children").as[Seq[JsValue]].filter { market =>
        (market \ "statistics" \ "direct" \ "startups").as[Int] > 10
      }.map { market => saveMarketToDB(market) }

      for (i <- 2 until pages) {
        getFutureMarketsByPage(i)
      }

      def getFutureMarketsByPage(page: Int) = {
        AngelListServices.getChildrenOfTagAndPage(magicNumber)(page).map { jsResponse =>
          (jsResponse \ "children").as[Seq[JsValue]].filter { market =>
            (market \ "statistics" \ "direct" \ "startups").as[Int] > 10
          }.map { market => saveMarketToDB(market) }
        }
      }
    }
  }

  def saveMarketToDB(market:JsValue) = {
    val newMarket = Market((market \ "name").as[String], (market \ "id").as[Long])
    Market.save(newMarket)
  }

}

