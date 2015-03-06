package controllers

import models.{Startup, Funding}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.Controller

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 05/03/15
 * Time: 17:54
 */
object Fundings extends Controller with Secured{

  def getFromStartup(startup:Startup) : Future[Seq[Funding]] = getFromStartupID(startup.id)

  def getFromStartupID(id:Long) : Future[Seq[Funding]] =
    AngelListServices.getFundingByStartupId(id) map responseToFunding

  private def responseToFunding(response:JsValue):Seq[Funding] =
    (response \ "funding").asOpt[Seq[JsValue]].fold{
      Seq[Funding]()
    }{ funding:Seq[JsValue] =>
      funding.filter(isFundingFilter).map(_.validate[Funding].get)
    }

  private def isFundingFilter(funding:JsValue) =
    funding.validate[Funding] match {
      case funding:JsSuccess[Funding] => true
      case err:JsError => Logger.warn("Failed to read role json: "+ JsError.toFlatJson(err).toString()); false
      case _ => Logger.warn("Failed to read role json: "+funding.toString()); false
    }
}
