package controllers

import java.io.{InputStreamReader, BufferedReader}
import java.net.{URL, InetSocketAddress, Proxy}

import _root_.util.RequestManager
import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.json._
import play.api.libs.ws.Response

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by Javier Isoldi.
 * Date: 17/09/14.
 * Project: tangela.
 */

object AngelListServices {
  val AngelApi = "https://api.angel.co/1"
  val proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("localhost", 9050))
//  var count = 0

  private def responseToJson(response: Response) = response.json

  def sendRequest(request: String): Future[JsValue] =
    Cache.get(AngelApi + request).fold {
//      count = count + 1
//      println("general count = " + count)
      RequestManager.sendRequest(AngelApi + request) map { response =>
        val jsonResponse = Json.parse(response)
        if((jsonResponse\"error").isInstanceOf[JsUndefined]) Cache.set(AngelApi + request, jsonResponse, 82800)
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
}
