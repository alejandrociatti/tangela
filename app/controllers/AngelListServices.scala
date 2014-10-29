package controllers

import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.json._
import play.api.libs.ws.{Response, WS}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
 * Created by Javier Isoldi.
 * Date: 17/09/14.
 * Project: tangela.
 */

object AngelListServices {
  val AngelApi = "https://api.angel.co/1"

  private def responseToJson(response: Response) = response.json

  def sendRequest(request: String): Future[JsValue] =
    Cache.get(AngelApi + request).fold {
      WS.url(AngelApi + request).get().map{ result =>
        val jsonResponse = responseToJson(result)
        //TODO: @Javier can u make this check pretty?
        if((jsonResponse\"error").as[JsString].toString().isEmpty) Cache.set(AngelApi + request, jsonResponse, 82800)
        println(jsonResponse)
        jsonResponse
      }
    } { result =>
      Future(result.asInstanceOf[JsValue])
    }

  def getStartupById(id: Long) = sendRequest(s"/startups/$id")

  def getUserById(id: Long) = sendRequest(s"/users/$id")

  def getRolesFromStartupId(id: Long) = sendRequest(s"/startup_roles?startup_id=$id")

  def getStartupsByTagId(id: Long) = sendRequest(s"/tags/$id/startups")

  def getStartupsByTagIdAndPage(id: Long)(page: Int) = sendRequest(s"/tags/$id/startups?page=$page")

  def getStartupsWithFoundRaising = sendRequest("/startups?filter=raising")

  def getFoundersByStartupId(id: Long) = sendRequest(s"/startups/$id/roles?role=founder")

  def getFundingByStartupId(id: Long) = sendRequest(s"/startups/$id/funding")

  def searchStartupByName(name: String) = sendRequest(s"/search?type=Startup&query=$name")
  
  def searchLocationByName(name: String) = sendRequest(s"/search?type=LocationTag&query=$name")

  def searchLocationBySlug(name: String) = sendRequest(s"/search/slugs?type=LocationTag&query=$name")

  def getChildrenOfTag(id: Long) = sendRequest(s"/tags/$id/children")

  def getChildrenOfTagAndPage(id: Long)(page: Int) = sendRequest(s"/tags/$id/children?page=$page")

  def searchMarketByName(name: String) = sendRequest(s"/search?type=MarketTag&query=$name")

  def checkTor() = WS.url("https://check.torproject.org/").get().map{ result =>
    if(result.body.contains("<h1 class=\"on\">")) println("You are using the TOR network.")
    else println("You are not under the TOR network.")
  }
}
