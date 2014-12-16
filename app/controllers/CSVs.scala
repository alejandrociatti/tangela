package controllers

import models.DatabaseUpdate
import play.api.libs.json.{JsUndefined, Json, JsValue, JsArray}
import play.api.mvc.{Action, Controller}
import util.CSVManager
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

import scala.concurrent.Future

/**
 * User: Martin Gutierrez
 * Date: 21/11/14
 * Time: 01:58
 */
object CSVs extends Controller{


  /* People Network CSV ***********************************************************************************************/

  def makePeopleNetworkCSVHeaders() = List(
    "user ID one", "user name one", "user role one",
    "user id two", "user name two", "user role two",
    "startup in common ID", "startup in common name"
  )

  def makePeopleNetworkCSVValues(connections: JsArray) = connections.as[List[JsValue]].map{ row =>
    valueListFromJsValue(row, Seq("userIdOne", "userNameOne", "roleOne", "userIdTwo", "userNameTwo", "roleTwo",
      "startupId", "startupName"))
  }

  def getPeopleNetworkCSV(locationId: Int, marketId: Int, quality: Int, creationDate: String) =
    getCsv(s"people-net-$locationId-$marketId-$quality-$creationDate")

  /* Startup Network CSV **********************************************************************************************/

  def makeStartupsNetworkCSVHeaders = List(
    "tangela request date",
    "startup ID one", "startup name one", "user role in startup one",
    "startup id two", "startup name two", "user role in startup two",
    "user in common ID", "user in common name"
  )

  def makeStartupsNetworkCSVValues(startups: JsArray) =
    makeCsvValues(startups.value, Seq("startupIdOne", "startupNameOne", "roleOne", "startupIdTwo", "startupNameTwo",
      "roleTwo", "userId", "userName"))

  def getStartupsNetworkCSV(locationId: Int, marketId: Int, quality: Int, creationDate: String) =
    getCsv(s"startup-net-$locationId-$marketId-$quality-$creationDate")

  /* Startups CSV *****************************************************************************************************/


  def makeStartupsCSVHeaders() = List(
    "Tangela Request Date",
    "id","hidden","community_profile","name","angellist_url","logo_url","thumb_url","quality",
    "product_desc","high_concept","follower_count","company_url","created_at","updated_at",
    "twitter_url","blog_url","video_url"
  )

  def makeStartupsCSVValues(values: Seq[JsValue]):List[List[String]]= values.toList.map { startup =>
    List(DatabaseUpdate.getLastAsString,
      (startup \ "id").asOpt[Int].getOrElse(0).toString,
      (startup \ "hidden").asOpt[Boolean].getOrElse(false).toString,
      (startup \ "community_profile").asOpt[Boolean].getOrElse(false).toString) ++
    valueListFromJsValue(startup, Seq("name", "angellist_url", "logo_url", "thumb_url", "quality", "product_desc",
      "high_concept", "follower_count", "company_url", "created_at", "updated_at", "twitter_url", "blog_url",
      "video_url"))
  }

  def getStartupsCSV(locationId: Int, marketId: Int, quality: Int, creationDate: String) = 
    getCsv(s"startups-$locationId-$marketId-$quality-$creationDate")

  /* Users CSV ********************************************************************************************************/

  def makeUsersCSVHeaders(): List[String] = List(
    "id", "name", "bio", "role", "follower_count", "angellist_url", "image", "blog_url",
    "online_bio_url", "twitter_url", "facebook_url", "linkedin_url", "what_ive_built",
    "what_i_do", "investor"
  )

  def makeUsersCSVValues(users: Seq[JsValue]): List[List[String]] = users.toList.map { user =>
    valueListFromJsValue(user, Seq("id", "name", "bio", "role", "follower_count", "angellist_url", "image",
      "blog_url", "online_bio_url", "twitter_url", "facebook_url", "linkedin_url", "what_ive_built", "what_i_do")) ++
      List((user \ "investor").asOpt[Boolean].getOrElse(false).toString)
  }

  def getUsersCSV(locationId: Int, marketId: Int, quality: Int, creationDate: String) = 
    getCsv(s"users-$locationId-$marketId-$quality-$creationDate")
  
  /* Startups Tags CSV ********************************************************************************************************/

  def makeStartupsTagsCSVHeaders(): List[String] = List(
    "Tangela Request Date",
    "startup ID", "Tag Id", "Tag Type",
    "Name", "Display Name", "AngelList Url"
  )

  /**
   * This method returns a List full of Tags in the form of a List of that tags attributes
   *
   * @param values: Seq[JsValue] containing all the tags to return (markets, locations, company_type)
   * @return List[List[String]] each tag is a List[String], each string is the value of an attribute.
   */
  def makeStartupsTagsCSVValues(values: Seq[JsValue]): List[List[String]] =
    makeCsvValues(values, Seq("startup", "id", "tag_type", "name", "display_name", "angellist_url"))

  def getStartupsTagsCSV(locationId: Int, marketId: Int, quality: Int, creationDate: String) = 
    getCsv(s"startups-tags-$locationId-$marketId-$quality-$creationDate")

  /* Roles CSV ********************************************************************************************************/

  def makeStartupRolesCSVHeaders = List(
    "tangela request date",
    "startup ID", "id", "role",
    "created at", "started at", "ended at",
    "title", "confirmed", "user name", "user id", "user bio",
    "user follower count", "user angel list url", "user image url"
  )

  def makeStartupRolesCSVValues(startups: JsArray, startupId: Long) = startups.as[List[JsValue]].map{ startup =>
    List(DatabaseUpdate.getLastAsString, startupId.toString) ++
      valueListFromJsValue(startup, Seq("id", "role", "created_at", "started_at", "ended_at", "title", "confirmed")) ++
      valueListFromJsValue(startup \ "user", Seq("name", "id", "bio", "follower_count", "angellist_url", "image"))
  }

  def getStartupRolesCSV(startupId: Long) = getCsv(s"startup-roles-$startupId")

  /* Funding CSV for one or more startups *****************************************************************************/

  def makeStartupFundingCSVHeaders = List(
    "tangela request date", "startup ID", "startup name", "round type", "round raised", "round closed at",
    "round id", "round source url", "participant name", "participant type", "participant id"
  )

  def makeStartupFundingCSVValues(fundings: JsValue): List[List[String]] = fundings.as[List[JsValue]].map{ funding =>
    Json.parse((funding \ "participants").as[String]).as[List[JsValue]] match {
      case Nil => List(emptyParticipant(funding))
      case nonEmpty => nonEmpty map (nonEmptyParticipant(_, funding))
    }
  }.flatten

  def nonEmptyParticipant(participant: JsValue, funding: JsValue): List[String] = startupFundingList(funding) ++
    valueListFromJsValue(participant, Seq("name", "type", "id"))

  def emptyParticipant(funding: JsValue) = startupFundingList(funding) ++ List("", "", "")

  def startupFundingList(funding: JsValue):List[String] = List(DatabaseUpdate.getLastAsString) ++
    valueListFromJsValue(funding, Seq("startup_id", "name", "round_type", "amount", "closed_at", "id", "source_url"))

  def getStartupFundingCSV(startupId: Long) = getCsv(s"startup-funding-$startupId")

  def getStartupsFundingsCSV(locationId: Int, marketId: Int, quality: Int, creationDate: String) =
    getCsv(s"startups-funding-$locationId-$marketId-$quality-$creationDate")

  /* Helpers ---------------------------------------------------------------------------------------------------- */

  def getCsv(name: String) = Action.async {
    Future(
      CSVManager.get(name).fold {
        Ok(Json.obj("error" -> "could not find that CSV"))
      } { result =>
        Ok(result)
      }
    )
  }

  def valueToString(value: JsValue)(name: String): String = {
    val result = if ((value \ name).isInstanceOf[JsUndefined]) "" else {
      (value \ name).asOpt[String].getOrElse(value \ name).toString
    }
    if(result == "null") "" else result
  }

  def valueListFromJsValue(value: JsValue, names: Seq[String]) = (names map valueToString(value)).toList

  def makeCsvValues(values: Seq[JsValue], names: Seq[String]): List[List[String]] = (values map { value: JsValue =>
    List(DatabaseUpdate.getLastAsString) ++ valueListFromJsValue(value, names)
  }).toList
}
