package controllers

import java.io.{InputStreamReader, BufferedReader}
import java.net.{URL, InetSocketAddress, Proxy}

import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.json._
import play.api.libs.ws.{Response, WS}

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

  private def responseToJson(response: Response) = response.json

  def sendRequest(request: String): Future[JsValue] =
    Cache.get(AngelApi + request).fold {
      scala.concurrent.Future {
        val connection = new URL("https://check.torproject.org/").openConnection(proxy)
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        val rd = new BufferedReader(new InputStreamReader(connection.getInputStream))
        Json.parse(Stream.continually(rd.readLine()).takeWhile(_ != null).mkString(" "))
      }
    } { result =>
      Future(result.asInstanceOf[JsValue])
    }


  def sendRequestOld(request: String): Future[JsValue] =
    Cache.get(AngelApi + request).fold {
    WS.url(AngelApi + request).get().map{ result =>
        val jsonResponse = responseToJson(result)
        //TODO: @Javier can u make this check pretty?
        if((jsonResponse\"error").as[JsString].toString().isEmpty) Cache.set(AngelApi + request, jsonResponse, 82800)
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
