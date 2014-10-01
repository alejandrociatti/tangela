package controllers

import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.json.JsValue
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

  def sendRequest(request: String): Future[Response] =
    Cache.get(AngelApi + request).fold {
      WS.url(AngelApi + request).get().map{ result =>
        Cache.set(AngelApi + request, result, 82800)
        result
      }
    } { result =>
      Future(result.asInstanceOf[Response])
    }

  def getStartupById(id: Long): Future[JsValue] = sendRequest(s"/startups/$id").map(responseToJson)

  private def responseToJson(response: Response) = response.json

}
