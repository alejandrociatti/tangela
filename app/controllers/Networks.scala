package controllers

import controllers.Startups.startupsByCriteriaNonBlocking
import play.api.libs.json.{JsValue, Json, JsArray}
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by Javier Isoldi.
 * Date: 30/10/14.
 * Project: tangela.
 */

object Networks extends Controller {

  /**
   *
   * @param locationId    location tag filter.
   * @param marketId      market tag filter.
   * @param quality       quality Int filter. Can be from 1 to 10.
   * @param creationDate  date filter.
   * @return A Future of a JsArray that contains all the connections between
   *         startups throw people
   */
  
  def getStartupsNetwork(locationId: Int, marketId: Int, quality: Int, creationDate: String) = Action.async {
    startupsByCriteriaNonBlocking(locationId, marketId, quality, creationDate) flatMap { startups =>
      getStartupsNetworkFuture(startups) map { startupsToSend =>
        Ok("["+startupsToSend+","+JsArray(startups)+"]")
      }
    }
  }

  def getStartupsNetworkFuture(startups: Seq[JsValue]): Future[JsArray] = {

    def differentStartupFilter(user1: JsValue, user2: JsValue) =
      (user1 \ "startupId").as[Int] != (user2 \ "startupId").as[Int]
    def equalUserFilter(user1: JsValue, user2: JsValue) =
      (user1 \ "userId").as[Int] == (user2 \ "userId").as[Int]

    val userRoles = Future.sequence(
      startups map getStartupRoles
    ).map(_.flatten)

    val startupConnections = userRoles map { userRoles =>
      def getMatches(userRoles: Seq[JsValue], matches: Seq[JsValue]): Seq[JsValue] =
        userRoles.toList match {
          case Nil => matches
          case userRole :: userRolesTail =>
            val startupConnections = userRolesTail
              .filter(differentStartupFilter(_, userRole))
              .filter(equalUserFilter(_, userRole))
              .map(startupsConnection(_, userRole))
            getMatches(userRolesTail, matches ++ startupConnections)
        }
      getMatches(userRoles, Seq())
    }

    startupConnections map JsArray
  }

  /**
   *
   * @param locationId    location tag filter.
   * @param marketId      market tag filter.
   * @param quality       quality Int filter. Can be from 1 to 10.
   * @param creationDate  date filter.
   * @return A Future of a JsArray that contains all the connections between
   *         people throw startups
   */

  def getPeopleNetwork(locationId: Int, marketId: Int, quality: Int, creationDate: String) = Action.async{
    startupsByCriteriaNonBlocking(locationId, marketId, quality, creationDate) flatMap { startups =>
      getPeopleNetworkFuture(startups) map { startupsToSend =>
        Ok("["+startupsToSend+","+startups+"]")
      }
    }
  }

  def getPeopleNetworkFuture(startups: Seq[JsValue]) : Future[JsArray] = {

    def equalStartupFilter(user1: JsValue, user2: JsValue) =
      (user1 \ "startupId").as[Int] == (user2 \ "startupId").as[Int]
    def differentUserFilter(user1: JsValue, user2: JsValue) =
      (user1 \ "userId").as[Int] != (user2 \ "userId").as[Int]

    val userRoles = Future.sequence(
      startups map getStartupRoles
    ).map(_.flatten)

    val peopleConnections = userRoles map { userRoles =>
      def getMatches(userRoles: Seq[JsValue], matches: Seq[JsValue]): Seq[JsValue] =
        userRoles.toList match {
          case Nil => matches
          case userRole :: userRolesTail =>
            val peopleConnections = userRolesTail
              .filter(differentUserFilter(_, userRole))
              .filter(equalStartupFilter(_, userRole))
              .map(usersConnection(_, userRole))
            getMatches(userRoles, matches ++ peopleConnections)
        }
      getMatches(userRoles, Seq())
    }

    peopleConnections map JsArray
  }

  // Helpers *************************************************************

  /**
   * Obtains all the roles that belong to a startup and the associated users
   * @param startup JsValue that contains the startup id and name
   * @return A Future of Seq of all corresponding roles as JsValues
   */
  
  def getStartupRoles(startup: JsValue): Future[Seq[JsValue]] = {
    val startupId: Int = (startup \ "id").as[Int]
    AngelListServices.getRolesFromStartupId(startupId) map { response =>
      (response \ "startup_roles").as[JsArray].value map { role =>
        userRole(role, startup)
      }
    }
  }

  def userRole(role: JsValue, startup: JsValue ) = Json.obj(
    "userId" -> (role \ "user" \ "id").as[Int],
    "userName" -> (role \ "user" \ "name").as[String],
    "userRole" -> (role \ "role").as[String],
    "startupId" -> (startup \ "id").as[Int],
    "startupName" -> (startup \ "name").as[String]
  )

  def startupsConnection(user1: JsValue, user2: JsValue) = Json.obj(
    "startupIdOne" -> (user1 \ "startupId").as[Int].toString,
    "startupIdTwo" -> (user2 \ "startupId").as[Int].toString,
    "startupNameOne" -> (user1 \ "startupName").as[String],
    "startupNameTwo" -> (user2 \ "startupName").as[String],
    "roleOne" -> (user1 \ "userRole").as[String],
    "roleTwo" -> (user2 \ "userRole").as[String],
    "userId" -> (user1 \ "userId").as[Int].toString,
    "userName" -> (user1 \ "userName").as[String]
  )

  def usersConnection(user1: JsValue, user2: JsValue) = Json.obj(
    "userIdOne" -> (user1 \ "userId").as[Int].toString,
    "userIdTwp" -> (user2 \ "userId").as[Int].toString,
    "userNameOne" -> (user1 \ "userName").as[String],
    "userNameTwo" -> (user2 \ "userName").as[String],
    "roleOne" -> (user1 \ "userRole").as[String],
    "roleTwo" -> (user2 \ "userRole").as[String],
    "startupId" -> (user1 \ "startupId").as[Int].toString,
    "startupName" -> (user1 \ "startupName").as[String]
  )
}
