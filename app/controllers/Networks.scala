package controllers

import _root_.util.{Tupler, CSVManager}
import controllers.Startups.startupsByCriteriaNonBlocking
import controllers.Roles._
import models._
import play.api.Logger
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
        Future(
          CSVManager.put(
            s"startup-net-$locationId-$marketId-$quality-$creationDate",
            StartupsConnection.getCSVHeader,
            connections.map(_.toCSVRow)
          )
        )
        Ok(Json.obj("startups" -> startups.map(_.toTinyJson), "rows" -> Json.toJson(connections)))
      }
    }
  }

  def getNetworksToLoad(location:Location) =
    startupsByCriteriaNonBlocking(location.angelId.toInt, -1, (-1,-1), ("","")).map{ startups =>
      getStartupsNetworkFuture(startups)
      getPeopleNetworkFuture(startups)
    }


  def getStartupsNetworkFuture(startups: Seq[Startup]): Future[Seq[StartupsConnection]] = {
    // userIDsFuture represents U1
    val userIDsFuture:Future[Seq[Long]] = Members.userIDsFromStartupsFlat(startups)
    // rolesFuture uses U1, we end up with Future[Seq[Seq[AngelRole]] with different Seq for different users
    val rolesFuture = userIDsFuture.flatMap(userIDs => Future.sequence(userIDs map getRolesFromUserID))
    rolesFuture map getStartupNetMatches // We map those roles to get the network connections
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
            UsersConnection.getCSVHeader,
            connections.map(_.toCSVRow)
          )
        )
        Ok(Json.obj("startups" -> Json.toJson(startups), "rows" -> connectionsJson))
      }
    }
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
      getPeopleNetworkFuture2ndOrder(startups) map { connections =>
        val startupsToSendJson = Json.toJson(connections)
        Future(
          CSVManager.put(
            s"people-net-2-$locationId-$marketId-$quality-$creationDate",
            UsersConnection.getCSVHeader,
            connections.map(_.toCSVRow)
          )
        )
        Ok(Json.obj("startups" -> Json.toJson(startups), "rows" -> startupsToSendJson))
      }
    }
  }

  /**
   * This method makes a people's network, going from S1->R1->U1->R2->
   * @param startups seed Seq[Startup] (representing S1)
   * @return a Future[Seq] of the obtained UserConnections
   */
  def getPeopleNetworkFuture(startups: Seq[Startup]) : Future[Seq[UsersConnection]] = {
    // userIDsFuture represents U1, with different Seq for different Startups
    val userIDsFuture:Future[Seq[Seq[Long]]] = Members.userIDsFromStartups(startups)
    // rolesFuture uses U1, we end up with Future[Seq[Seq[AngelRole]] again with different Seq for different Startups
    val rolesFuture = userIDsFuture.flatMap(userIDs => Future.sequence(userIDs map getRolesFromUserIDsFlat))
    rolesFuture map getPeopleNetMatches // We map those roles to get the network connections
  }

  /**
   * This method makes a people network, going from S1->R1->U1->R2->S2->R3 to N1
   * @param startups seed Seq[Startup] (representing S1)
   * @return a Future[Seq] of the obtained UserConnections
   */
  private def getPeopleNetworkFuture2ndOrder(startups: Seq[Startup]) : Future[Seq[UsersConnection]] = {
    // All the users involved in startups
    val userIDs = Members.userIDsFromStartupsFlat(startups) // Represents U1
    // All the startups in which the above users are involved
    val startupIDs = userIDs flatMap Startups.getStartupIDsFromUserIDs // Represents S2
    // The roles of those startups
    val extendedRoles:Future[Seq[Seq[AngelRole]]] = startupIDs flatMap getRolesFromStartupIDs // Represents R3
    // Using R3 roles, match them to represent the network
    extendedRoles map { userRoles =>
      println(s"FINISHED GETTING R3: \nR3 has: ${userRoles.length} startups")
      getPeopleNetMatches(userRoles)
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
   * @param userRoles a Seq that has a Seq of AngelRoles for each different startup
   * @return          a Seq[UsersConnection] that consists of two roles that share a startup.
   */
  private def getPeopleNetMatches(userRoles: Seq[Seq[AngelRole]]): Seq[UsersConnection] = {

    def getMatches(roles: Seq[AngelRole], matches: Seq[UsersConnection]):Seq[UsersConnection] =
      roles match {
        case Nil => matches
        case userRole :: userRolesTail =>
          val peopleConnections = userRolesTail
            .filter(differentUserFilter(_, userRole))
            .map( UsersConnection(_, userRole))
          getMatches(userRolesTail, matches ++ peopleConnections)
        case other =>
          Logger.warn("This did not match: "+ other.toString)
          Logger.warn("Length: "+ other.length)
          matches
      }

    userRoles.flatMap(roles => getMatches(roles, Seq()))
  }



  private def getStartupNetMatches(startupRoles: Seq[Seq[AngelRole]]): Seq[StartupsConnection] = {

    def getMatches(roles: Seq[AngelRole], matches: Seq[StartupsConnection]):Seq[StartupsConnection] =
      roles match {
        case Nil => matches
        case startupRole :: startupRolesTail =>
          val startupConnections = startupRolesTail
            .filter(differentStartupFilter(_, startupRole))
            .map( StartupsConnection(_, startupRole) )
          getMatches(startupRolesTail, matches ++ startupConnections)
      }

    startupRoles.flatMap(roles => getMatches(roles, Seq()))
  }

}