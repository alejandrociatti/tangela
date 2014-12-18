package controllers

import java.io.File

import _root_.util.{DiskSaver, RequestManager}
import models.DatabaseUpdate
import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by Javier Isoldi.
 * Date: 17/09/14.
 * Project: tangela.
 */

object AngelListServices {
  val AngelApi = "https://api.angel.co/1"
  val jsonSaver = DiskSaver(new File(DatabaseUpdate.getLastFolder + "_jsons"))

  private def cache(key: String, value: JsValue) =
    if((value\"error").isInstanceOf[JsUndefined]) Cache.set(key, value, 300)

  private def parseAndCache(key: String, value: String) = {
      val jsonResponse = Json.parse(value)
      cache(key, jsonResponse)
      jsonResponse
  }

  private def sendRequestToAngelList(request: String): Future[JsValue] =
    RequestManager.sendRequest(AngelApi + request) map { response =>
      Future(jsonSaver.put(request, response))
      parseAndCache(request, response)
    }

  def sendRequest(request: String): Future[JsValue] =
    Cache.get(request).fold {
      jsonSaver.get(request).fold {
        sendRequestToAngelList(request)
      }{ value =>
        Future(parseAndCache(request, value))
      }
    }{ result =>
      Future(result.asInstanceOf[JsValue])
    }

  def getStartupById(id: Long) = sendRequest(s"/startups/$id")

  def getUserById(id: Long) = sendRequest(s"/users/$id")

  def getRolesFromStartupId(id: Long) = sendRequest(s"/startup_roles?startup_id=$id")

  def getRolesFromUserId(id: Long) = sendRequest(s"/startup_roles?user_id=$id")

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
