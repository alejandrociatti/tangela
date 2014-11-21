package controllers

import controllers.Networks._
import models.DatabaseUpdate
import play.api.libs.json.{Json, JsValue, JsArray}
import play.api.mvc.Action
import util.CSVManager
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

import scala.concurrent.Future

/**
 * User: Martin Gutierrez
 * Date: 21/11/14
 * Time: 01:58
 */
object CSVs {

  /* People Network CSV ***********************************************************************************************/

  def makePeopleNetworkCSVHeaders() = List(
    "user ID one", "user name one", "user role one",
    "user id two", "user name two", "user role two",
    "startup in common ID", "startup in common name"
  )

  def makePeopleNetworkCSVValues(connections: JsArray) = connections.as[List[JsValue]].map{ row =>
    List(
      (row \ "userIdOne").as[String],
      (row \ "userNameOne").as[String],
      (row \ "roleOne").as[String],
      (row \ "userIdTwo").as[String],
      (row \ "userNameTwo").as[String],
      (row \ "roleTwo").as[String],
      (row \ "startupId").as[String],
      (row \ "startupName").as[String]
    )
  }

  def getPeopleNetworkCSV(locationId: Int, marketId: Int, quality: Int, creationDate: String) = Action.async {
    Future(
      CSVManager.get(s"people-net-$locationId-$marketId-$quality-$creationDate").fold {
        Ok(Json.obj("error" -> "could not find that CSV"))
      }{ result =>
        Ok(result)
      }
    )
  }

  /* Startup Network CSV **********************************************************************************************/

  def makeStartupsNetworkCSVHeaders = List(
    "tangela request date",
    "startup ID one", "startup name one", "user role in startup one",
    "startup id two", "startup name two", "user role in startup two",
    "user in common ID", "user in common name"
  )

  def makeStartupsNetworkCSVValues(startups: JsArray) = startups.as[List[JsValue]].map{ startup =>
    List(
      DatabaseUpdate.getLastAsString,
      (startup \ "startupIdOne").as[String],
      (startup \ "startupNameOne").as[String],
      (startup \ "roleOne").as[String],
      (startup \ "startupIdTwo").as[String],
      (startup \ "startupNameTwo").as[String],
      (startup \ "roleTwo").as[String],
      (startup \ "userId").as[String],
      (startup \ "userName").as[String]
    )
  }

  def getStartupsNetworkCSV(locationId: Int, marketId: Int, quality: Int, creationDate: String) = Action.async {
    Future(
      CSVManager.get(s"startup-net-$locationId-$marketId-$quality-$creationDate").fold {
        Ok(Json.obj("error" -> "could not find that CSV"))
      }{ result =>
        Ok(result)
      }
    )
  }

  /* Roles CSV ********************************************************************************************************/

  def makeStartupRolesCSVHeaders = List(
    "tangela request date",
    "startup ID", "id", "role",
    "created at", "started at", "ended at",
    "title", "confirmed", "user name", "user id", "user bio",
    "user follower count", "user angel list url", "user image url"
  )

  def makeStartupRolesCSVValues(startups: JsArray, startupId: Long) = startups.as[List[JsValue]].map{ startup =>
    List(
      DatabaseUpdate.getLastAsString,
      startupId.toString,
      (startup \ "id").asOpt[Int].getOrElse("").toString,
      (startup \ "role").asOpt[String].getOrElse(""),
      (startup \ "created_at").asOpt[String].getOrElse(""),
      (startup \ "started_at").asOpt[String].getOrElse(""),
      (startup \ "ended_at").asOpt[String].getOrElse(""),
      (startup \ "title").asOpt[String].getOrElse(""),
      (startup \ "confirmed").asOpt[Boolean].getOrElse("").toString,
      (startup \ "user" \ "name").asOpt[String].getOrElse(""),
      (startup \ "user" \ "id").asOpt[Int].getOrElse("").toString,
      (startup \ "user" \ "bio").asOpt[String].getOrElse(""),
      (startup \ "user" \ "follower_count").asOpt[Int].getOrElse("").toString,
      (startup \ "user" \ "angellist_url").asOpt[String].getOrElse(""),
      (startup \ "user" \ "image").asOpt[String].getOrElse("")
    )
  }

  def getStartupRolesCSV(startupId: Long) = Action.async {
    Future(
      CSVManager.get(s"startup-roles-$startupId").fold {
        Ok(Json.obj("error" -> "could not find that CSV"))
      } { result =>
        Ok(result)
      }
    )
  }
}
