package controllers

import _root_.util.{Tupler, CSVManager}
import controllers.Startups.startupsByCriteriaNonBlocking
import controllers.Roles._
import models.{Startup, UsersConnection, StartupsConnection, AngelRole}
import play.api.libs.json._
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
   * @param locationId    location tag filter.
   * @param marketId      market tag filter.
   * @param quality       quality Int filter. Value must be between 1 to 10.
   * @param creationDate  creation date filter.
   * @return A Future of a JsArray that contains all the connections between startups by roles/people
   */
  def getStartupsNetwork(locationId: Int, marketId: Int, quality: String, creationDate: String) = Action.async {
    startupsByCriteriaNonBlocking(locationId, marketId, Tupler.toQualityTuple(quality), Tupler.toTuple(creationDate)) flatMap { startups =>
      getStartupsNetworkFuture(startups) map { connections =>
        val connectionsJson = Json.toJson(connections).as[JsArray]
        Future(
          CSVManager.put(
            s"startup-net-$locationId-$marketId-$quality-$creationDate",
            CSVs.makeStartupsNetworkCSVHeaders,
            CSVs.makeStartupsNetworkCSVValues(connectionsJson)
          )
        )
        Ok(Json.obj("startups" -> startups.map(_.toTinyJson), "rows" -> connectionsJson))
      }
    }
  }

  def getStartupsNetworkToLoad(locationId: Int, marketId: Int, quality: String, creationDate: String) =
    startupsByCriteriaNonBlocking(locationId, marketId, Tupler.toQualityTuple(quality), Tupler.toTuple(creationDate)) map getStartupsNetworkFuture


  def getStartupsNetworkFuture(startups: Seq[Startup]): Future[Seq[StartupsConnection]] = {

    val userRoles = Future.sequence(startups map getFromStartup).map(_.flatten).flatMap(getExtendedRoles)

    val startupConnections = userRoles map { userRoles =>
      def getMatches(userRoles: Seq[AngelRole], matches: Seq[StartupsConnection]): Seq[StartupsConnection] =
        userRoles.toList match {
          case Nil => matches
          case userRole :: userRolesTail =>
            val startupConnections = userRolesTail
              .filter(equalUserFilter(_, userRole))
              .filter(differentStartupFilter(_, userRole))
              .map( StartupsConnection(_, userRole))
            getMatches(userRolesTail, matches ++ startupConnections)
        }
      getMatches(userRoles, Seq())
    }
    startupConnections
  }

  /**
   * @param locationId    location tag filter.
   * @param marketId      market tag filter.
   * @param quality       quality Int filter. Can be from 1 to 10.
   * @param creationDate  date filter.
   * @return A Future of a JsArray that contains all the connections between people by startups in common
   */
  def getPeopleNetwork(locationId: Int, marketId: Int, quality: String, creationDate: String) = Action.async{
    val qualityT = Tupler.toQualityTuple(quality) // We convert the quality string to a tuple representing a range.
    val dateT = Tupler.toTuple(creationDate)      // Same as above for creation date.
    startupsByCriteriaNonBlocking(locationId, marketId, qualityT, dateT) flatMap { startups =>
      getPeopleNetworkFuture(startups) map { connections =>
        val connectionsJson = Json.toJson(connections).as[JsArray]
        Future(
          CSVManager.put(
            s"people-net-$locationId-$marketId-$quality-$creationDate",
            CSVs.makePeopleNetworkCSVHeaders(),
            CSVs.makePeopleNetworkCSVValues(connectionsJson)
          )
        )
        Ok(Json.obj("startups" -> Json.toJson(startups), "rows" -> connectionsJson))
      }
    }
  }

  def getPeopleNetworkFuture(startups: Seq[Startup]) : Future[Seq[UsersConnection]] = {
    val userRoles = Future.sequence(startups map getFromStartup).map(_.flatten).flatMap(getExtendedRoles)
    userRoles map ( userRoles => getPeopleNetMatches(userRoles, Seq()) )
  }

  /**
   * This '2nd Order' network makes a second list of startups, using the roles from the users of the first list.
   * @param locationId    location tag filter.
   * @param marketId      market tag filter.
   * @param quality       quality Int filter. Can be from 1 to 10.
   * @param creationDate  date filter.
   * @return A Future of a JsArray that contains all the connections between people by startups in common
   */
  def getPeopleNetwork2ndOrder(locationId: Int, marketId: Int, quality: String, creationDate: String) = Action.async{
    val qualityT = Tupler.toQualityTuple(quality) // We convert the quality string to a tuple representing a range.
    val dateT = Tupler.toTuple(creationDate)      // Same as above for creation date.
    startupsByCriteriaNonBlocking(locationId, marketId, qualityT, dateT) flatMap { startups =>
      getPeopleNetworkFuture2ndOrder(startups) map { startupsToSend =>
        val startupsToSendJson = Json.toJson(startupsToSend)
        Future(
          CSVManager.put(
            s"people-net-2-$locationId-$marketId-$quality-$creationDate",
            CSVs.makePeopleNetworkCSVHeaders(),
            CSVs.makePeopleNetworkCSVValues(startupsToSendJson.as[JsArray])
          )
        )
        Ok(Json.obj("startups" -> Json.toJson(startups), "rows" -> startupsToSendJson))
      }
    }
  }

  /**
   * This method makes a people network, going from S1->R1->U1->R2->S2->R3 to N1
   * @param startups seed Seq[Startup] (representing S1)
   * @return a Future[Seq] of the obtained UserConnections
   */
  private def getPeopleNetworkFuture2ndOrder(startups: Seq[Startup]) : Future[Seq[UsersConnection]] = {
    // All the users involved in startups
    val userIDs = Members.userIDsFromStartups(startups) // Represents U1
    // All the startups in which the above users are involved
    val startupIDs = userIDs flatMap Startups.getStartupIDsFromUserIDs // Represents S2
    // The roles of those startups
    val extendedRoles:Future[Seq[AngelRole]] = startupIDs flatMap getRolesFromStartupIDs // Represents R3
    // Using R3 roles, match them to represent the network
    extendedRoles map { userRoles =>
      println("FINISHED GETTING R3: \nR3 length: "+userRoles.length)
      getPeopleNetMatches(userRoles, Seq())
    } // Represents N1
  }

  // Helpers *************************************************************

  private def equalStartupFilter(role1: AngelRole, role2: AngelRole) = role1.startup.id == role2.startup.id

  private def differentStartupFilter(role1: AngelRole, role2: AngelRole) = !equalStartupFilter(role1, role2)

  private def equalUserFilter(role1: AngelRole, role2: AngelRole) = role1.user.id == role2.user.id

  private def differentUserFilter(role1: AngelRole, role2: AngelRole) = !equalUserFilter(role1, role2)

  /**
   * This method creates matches for ppl networks by combining all userRoles,
   * and filtering them so that the only pairs that remain consist of different users in the same startup.
   * @param userRoles the list of roles
   * @param matches   the up-to-now found matches (for tail recursion's magic)
   * @return          a Seq[UsersConnection] that consists of two roles that share a startup.
   */
  private def getPeopleNetMatches(userRoles: Seq[AngelRole], matches: Seq[UsersConnection]): Seq[UsersConnection] =
    userRoles match {
      case Nil => matches
      case userRole :: userRolesTail =>
        val peopleConnections = userRolesTail
          .filter(equalStartupFilter(_, userRole))
          .filter(differentUserFilter(_, userRole))
          .map( UsersConnection(_, userRole))
        getPeopleNetMatches(userRolesTail, matches ++ peopleConnections)
    }
}