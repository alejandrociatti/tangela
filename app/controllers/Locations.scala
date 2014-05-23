package controllers

import play.api.mvc.{Action, Controller}
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.WS
import play.api.libs.ws
import ExecutionContext.Implicits.global


/**
 * User: Martin Gutierrez
 * Date: 22/05/14
 * Time: 20:52
 */
object Locations extends Controller{

  def getCountryIdByName(countryName:String):Future[ws.Response] = {
    WS.url("https://api.angel.co/1/search?type=LocationTag&query=" + countryName).get()
  }

  def getCountryChildren(countryName:String) = Action {
    val promiseOfCountryId:Future[ws.Response] = getCountryIdByName(countryName)

    Async{
      promiseOfCountryId.map{response =>
        val countryId = response.json.\\("id").head
        Async {
          WS.url("https://api.angel.co/1/tags/" + countryId + "/children").get().map { resp =>
            val ids = resp.json.\\("id")
            val names = resp.json.\\("display_name")

            Ok()
          }
        }
      }
    }
  }
}