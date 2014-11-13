package controllers

import java.io._
import _root_.util.DiskSaver
import com.github.tototoshi.csv.CSVWriter
import controllers.Startups.startupsByCriteriaNonBlocking
import play.api.libs.json._
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

/**
 * Created by Javier Isoldi.
 * Date: 30/10/14.
 * Project: tangela.
 */

object Networks extends Controller {
  val jsonSaver = DiskSaver(new File("storedCSVs"), ".csv")

  /**
   * @param locationId    location tag filter.
   * @param marketId      market tag filter.
   * @param quality       quality Int filter. Value must be between 1 to 10.
   * @param creationDate  creation date filter.
   * @return A Future of a JsArray that contains all the connections between startups by roles/people
   */
  def getStartupsNetwork(locationId: Int, marketId: Int, quality: Int, creationDate: String) = Action.async {
    startupsByCriteriaNonBlocking(locationId, marketId, quality, creationDate) flatMap { startups =>
      getStartupsNetworkFuture(startups) map { startupsToSend =>
        Future(
          jsonSaver.put(
            s"startup-net-$locationId-$marketId-$quality-$creationDate",
            makeStartupsNetworkCSV(startupsToSend)
          )
        )
        Ok(Json.obj(
          "startups" -> startups,
          "rows" -> startupsToSend
        ))
      }
    }
  }

  /**
   * This method gets a Startups Network in jsArray form and converts it to CSV
   *
   * @param startups a jsArray containing startupsConnection jsObjects
   * @return the CSV Result.
   */
  def makeStartupsNetworkCSV(startups:JsArray) = {
    val headers:List[String] = List(
        "startup ID one", "startup name one", "user role in startup one",
        "startup id two", "startup name two", "user role in startup two",
        "user in common ID", "user in common name"
      )
    val values:List[List[String]] = startups.as[List[JsValue]].map{ startup =>
      List(
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
    val byteArrayOutputStream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val writer = CSVWriter.open(new OutputStreamWriter(byteArrayOutputStream))
    writer.writeRow(headers)
    writer.writeAll(values)
    writer.close()
    val streamReader: InputStream = new BufferedInputStream(new ByteArrayInputStream(
      byteArrayOutputStream.toByteArray
    ))
    Source.fromInputStream(streamReader).mkString("")
  }

  def getStartupsNetworkCSV(locationId: Int, marketId: Int, quality: Int, creationDate: String) = Action.async {
      Future(
        jsonSaver.get(s"startup-net-$locationId-$marketId-$quality-$creationDate").fold {
          Ok(Json.obj("error" -> "could not find that CSV"))
        }{ result =>
          Ok(result)
        }
      )
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
   * @param locationId    location tag filter.
   * @param marketId      market tag filter.
   * @param quality       quality Int filter. Can be from 1 to 10.
   * @param creationDate  date filter.
   * @return A Future of a JsArray that contains all the connections between people by startups in common
   */
  def getPeopleNetwork(locationId: Int, marketId: Int, quality: Int, creationDate: String) = Action.async{
    startupsByCriteriaNonBlocking(locationId, marketId, quality, creationDate) flatMap { startups =>
      getPeopleNetworkFuture(startups) map { startupsToSend =>
        Future(
          jsonSaver.put(
            s"people-net-$locationId-$marketId-$quality-$creationDate",
            makePeopleNetworkCSV(startupsToSend)
          )
        )
        Ok(Json.obj(
          "startups" -> startups,
        "rows" -> startupsToSend
        ))
      }
    }
  }

  def makePeopleNetworkCSV(connections:JsArray) = {
    val headers:List[String] = List(
      "user ID one", "user name one", "user role one",
      "user id two", "user name two", "user role two",
      "startup in common ID", "startup in common name"
    )
    val values:List[List[String]] = connections.as[List[JsValue]].map{ row =>
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
    val byteArrayOutputStream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val writer = CSVWriter.open(new OutputStreamWriter(byteArrayOutputStream))
    writer.writeRow(headers)
    writer.writeAll(values)
    writer.close()
    val streamReader: InputStream = new BufferedInputStream(new ByteArrayInputStream(
      byteArrayOutputStream.toByteArray
    ))
    Source.fromInputStream(streamReader).mkString("")
  }

  def getPeopleNetworkCSV(locationId: Int, marketId: Int, quality: Int, creationDate: String) = Action.async {
    Future(
      jsonSaver.get(s"people-net-$locationId-$marketId-$quality-$creationDate").fold {
        Ok(Json.obj("error" -> "could not find that CSV"))
      }{ result =>
        Ok(result)
      }
    )
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
      (response \ "startup_roles").asOpt[JsArray].fold {
        Seq[JsValue]()
      }{ roles =>
        roles.value.filter(notNullUserFilter).map(userRole(_, startup))
      }
    }
  }

  def notNullUserFilter(role: JsValue) = (role \ "user") != JsNull

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
    "userIdTwo" -> (user2 \ "userId").as[Int].toString,
    "userNameOne" -> (user1 \ "userName").as[String],
    "userNameTwo" -> (user2 \ "userName").as[String],
    "roleOne" -> (user1 \ "userRole").as[String],
    "roleTwo" -> (user2 \ "userRole").as[String],
    "startupId" -> (user1 \ "startupId").as[Int].toString,
    "startupName" -> (user1 \ "startupName").as[String]
  )
}
