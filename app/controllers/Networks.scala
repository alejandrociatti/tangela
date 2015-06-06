package controllers

import _root_.util.{RequestSerializer, Tupler, CSVManager}
import _root_.util.Tupler.toTuple
import _root_.util.{CSVCreator, DiskSaver, Tupler, CSVManager}
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
    val key = s"startup-net-$locationId-$marketId-$quality-$creationDate"
    val qualityT = Tupler.toQualityTuple(quality)
    val creationDateT = Tupler.toTuple(creationDate)
    val description = generateDescription("Startup Network", locationId, marketId, qualityT, creationDateT)
    RequestSerializer.serialize(key, description, () => startupsByCriteriaNonBlocking(locationId, marketId, qualityT, creationDateT) flatMap { startups =>
        getStartupsNetworkFuture(startups) map { connections =>
          CSVManager.put(
            key,
            StartupsConnection.getCSVHeader,
            connections.map(_.toCSVRow)
          )
          Ok(Json.obj("startups" -> startups.map(_.toTinyJson), "rows" -> Json.toJson(connections)))
        }
      }
    )
  }

  def getNetworksToLoad(location:Location) =
    startupsByCriteriaNonBlocking(location.angelId.toInt, -1, (-1,-1), ("","")).map { startups =>
      prepareStartupsNetworkFuture(startups)
      preparePeopleNetworkFuture(startups)
    }

  /**
   * With a startups seed (S1), this finds its associated users (U1), and their roles (R2)
   * This method is used to pre-load the S1,R1,U1,R2, and to create startups network
   * @param startups the startups seed (S1)
   * @return R2, split in a different Seq for each different user
   */
  private def prepareStartupsNetworkFuture(startups: Seq[Startup]): Future[Seq[Seq[AngelRole]]] = {
    // userIDsFuture represents U1
    val userIDsFuture:Future[Seq[Long]] = Members.userIDsFromStartupsFlat(startups)
    // using U1, we get a Future[Seq[Seq[AngelRole]] with different Seq for different users
    userIDsFuture.flatMap(userIDs => Future.sequence(userIDs map getRolesFromUserID))
  }

  def getStartupsNetworkFuture(startups: Seq[Startup]): Future[Seq[StartupsConnection]] =
    // We map those roles to get the network connections
    prepareStartupsNetworkFuture(startups) map getStartupNetMatches

  /**
   * @param locationId    location tag filter.
   * @param marketId      market tag filter.
   * @param quality       quality Int filter. Can be from 1 to 10.
   * @param creationDate  date filter.
   * @return A Future of a JsArray that contains all the connections between people by startups in common
   */
  def getPeopleNetwork(locationId: Int, marketId: Int, quality: String, creationDate: String) = Action.async{
    val key = s"people-net-$locationId-$marketId-$quality-$creationDate"
    val qualityT = Tupler.toQualityTuple(quality) // We convert the quality string to a tuple representing a range.
    val dateT = Tupler.toTuple(creationDate)      // Same as above for creation date.
    val description = generateDescription("People Network", locationId, marketId, qualityT, dateT)
    RequestSerializer.serialize(key, description, () =>
      startupsByCriteriaNonBlocking(locationId, marketId, qualityT, dateT) flatMap { startups =>
        getPeopleNetworkFuture(startups) map { connections =>
          val connectionsJson = Json.toJson(connections).as[JsArray]
          CSVManager.put(
            key,
            UsersConnection.getCSVHeader,
            connections.map(_.toCSVRow)
          )
          Ok(Json.obj("startups" -> Json.toJson(startups), "rows" -> connectionsJson))
        }
      }
    )
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
    val key = s"people-net-2-$locationId-$marketId-$quality-$creationDate"
    val qualityT = Tupler.toQualityTuple(quality) // We convert the quality string to a tuple representing a range.
    val dateT = Tupler.toTuple(creationDate)      // Same as above for creation date.
    val description = generateDescription("People Network 2", locationId, marketId, qualityT, dateT)
    RequestSerializer.serialize(key, description, () =>
      startupsByCriteriaNonBlocking(locationId, marketId, qualityT, dateT) flatMap { startups =>
        getPeopleNetworkFuture2ndOrder(startups) map { connections =>
          val startupsToSendJson = Json.toJson(connections)
          CSVManager.put(
            key,
            UsersConnection.getCSVHeader,
            connections.map(_.toCSVRow)
          )
          Ok(Json.obj("startups" -> Json.toJson(startups), "rows" -> startupsToSendJson))
        }
      }
    )
  }

  /**
   * With a startups seed (S1), this finds its associated users (U1), and their roles (R2)
   * This method is used to pre-load the S1,R1,U1,R2 only
   * @param startups the startups seed (S1)
   * @return we don't use the result: its a Future Seq Future Seq Seq AngelRole (or something like that)
   */
  private def preparePeopleNetworkFuture(startups: Seq[Startup]) = {
    // userIDsFuture represents U1, with different Seq for different Startups
    val userIDsFuture:Future[Seq[Seq[Long]]] = Members.userIDsFromStartups(startups)
    // rolesFuture uses U1,
    userIDsFuture.map(_ map getRolesFromUserIDs)
  }

  /**
   * This method makes a people's network, going from S1->R1->U1->R2->
   * @param startups seed Seq[Startup] (representing S1)
   * @return a Future[Seq] of the obtained UserConnections
   */
  def getPeopleNetworkFuture(startups: Seq[Startup]) : Future[Seq[UsersConnection]] = {
    val startupIDs:Seq[Long] = startups.map(startup => startup.id)
    // userIDsFuture represents U1, with different Seq for different Startups
    val userIDsFuture:Future[Seq[Long]] = Members.userIDsFromStartupIDsFlat(startupIDs)
    // we find the startups not included in the seed, in which these people are involved
    val extraStartups = userIDsFuture.flatMap( Startups.getStartupIDsFromUserIDs(_).map(_.filter{ id =>
      // We filter the IDs already in the seed
      !startupIDs.contains(id)
    }) )

    // with the extra startups + the seed startups, we find the user roles
    val rolesFuture = extraStartups.flatMap(extraIDs => Future.sequence(
      (extraIDs ++ startupIDs).map( startupID =>
        // we ONLY search for those user roles of users in U1, as we don't want to expand our search from there
        userIDsFuture.map( Roles.getRolesFromStartupIDFiltered(startupID, _) )
      )
    )).flatMap(Future.sequence(_))      // We end up with a Future[Seq[Seq[AngelRole]]] with diff Seq for diff Startups
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
          Logger.warn("This did not match: " + other.toString)
          Logger.warn("Length: " + other.length)
          matches
      }

    userRoles.flatMap(roles => getMatches(roles, Seq()))
  }

  private def getStartupNetMatches(startupRoles: Seq[Seq[AngelRole]]): Seq[StartupsConnection] = {

    def getMatches(roles: Seq[AngelRole], matches: Seq[StartupsConnection]): Seq[StartupsConnection] =
      roles match {
        case Nil => matches
        case startupRole :: startupRolesTail =>
          val startupConnections = startupRolesTail
            .filter(differentStartupFilter(_, startupRole))
            .map(StartupsConnection(_, startupRole))
          getMatches(startupRolesTail, matches ++ startupConnections)
      }

    startupRoles.flatMap(roles => getMatches(roles, Seq()))
  }

  private def generateDescription(kind: String, locID: Int, mktID: Int, qual: (Int,Int), date: (String,String)) :String = {
    val string = new StringBuilder(s"$kind")
    Location.getByAngelId(locID).map(location => string.append(" Location: "+location.name+", "))
    Market.getByAngelId(mktID).map(market=> string.append("Market: "+market.name+", "))
    if(qual._1 != -1 || qual._2 != -1) string.append("Quality range: ("+qual._1+","+qual._2+") ")
    if(date._1 != "" || date._2 != "") string.append("Date range: ("+date._1+","+date._2+") ")
    string.mkString
  }

}
