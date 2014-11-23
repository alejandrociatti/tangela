package controllers

import models.DatabaseUpdate
import play.api.libs.json.{Json, JsValue, JsArray}
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

  /* Startups CSV *****************************************************************************************************/


  def makeStartupsCSVHeaders() = List(
    "Tangela Request Date",
    "id","hidden","community_profile","name","angellist_url","logo_url","thumb_url","quality",
    "product_desc","high_concept","follower_count","company_url","created_at","updated_at",
    "twitter_url","blog_url","video_url"
  )

  def makeStartupsCSVValues(values: Seq[JsValue]):List[List[String]]= values.toList.map { startup =>
    List(
      DatabaseUpdate.getLastAsString,
      (startup \ "id").asOpt[Int].getOrElse(0).toString,
      (startup \ "hidden").asOpt[Boolean].getOrElse(false).toString,
      (startup \ "community_profile").asOpt[Boolean].getOrElse(false).toString,
      (startup \ "name").asOpt[String].getOrElse(""),
      (startup \ "angellist_url").asOpt[String].getOrElse(""),
      (startup \ "logo_url").asOpt[String].getOrElse(""),
      (startup \ "thumb_url").asOpt[String].getOrElse(""),
      (startup \ "quality").asOpt[Int].getOrElse(0).toString,
      (startup \ "product_desc").asOpt[String].getOrElse(""),
      (startup \ "high_concept").asOpt[String].getOrElse(""),
      (startup \ "follower_count").asOpt[Int].getOrElse(0).toString,
      (startup \ "company_url").asOpt[String].getOrElse(""),
      (startup \ "created_at").asOpt[String].getOrElse(""),
      (startup \ "updated_at").asOpt[String].getOrElse(""),
      (startup \ "twitter_url").asOpt[String].getOrElse(""),
      (startup \ "blog_url").asOpt[String].getOrElse(""),
      (startup \ "video_url").asOpt[String].getOrElse("")
    )
  }

  def getStartupsCSV(locationId: Int, marketId: Int, quality: Int, creationDate: String) = Action.async {
    Future(
      CSVManager.get(s"startups-$locationId-$marketId-$quality-$creationDate").fold {
        Ok(Json.obj("error" -> "could not find that CSV"))
      }{ result =>
        Ok(result)
      }
    )
  }


  /* Users CSV ********************************************************************************************************/



  def makeUsersCSVHeaders(): List[String] = List(
    "id", "name", "bio", "role", "follower_count", "angellist_url", "image", "blog_url",
    "online_bio_url", "twitter_url", "facebook_url", "linkedin_url", "what_ive_built",
    "what_i_do", "investor"
  )


  def makeUsersCSVValues(users: Seq[JsValue]): List[List[String]] = users.toList.map {user =>
    List(
      (user \ "id").asOpt[Long].getOrElse(0).toString,
      (user \ "name").asOpt[String].getOrElse[String](""),
      (user \ "bio").asOpt[String].getOrElse[String](""),
      (user \ "role").asOpt[String].getOrElse[String](""),
      (user \ "follower_count").asOpt[Int].getOrElse(0).toString,
      (user \ "angellist_url").asOpt[String].getOrElse[String](""),
      (user \ "image").asOpt[String].getOrElse[String](""),
      (user \ "blog_url").asOpt[String].getOrElse[String](""),
      (user \ "online_bio_url").asOpt[String].getOrElse[String](""),
      (user \ "twitter_url").asOpt[String].getOrElse[String](""),
      (user \ "facebook_url").asOpt[String].getOrElse[String](""),
      (user \ "linkedin_url").asOpt[String].getOrElse[String](""),
      (user \ "what_ive_built").asOpt[String].getOrElse[String](""),
      (user \ "what_i_do").asOpt[String].getOrElse[String](""),
      (user \ "investor").asOpt[Boolean].getOrElse(false).toString
    )
  }

  def getUsersCSV(locationId: Int, marketId: Int, quality: Int, creationDate: String) = Action.async {
    Future(
      CSVManager.get(s"users-$locationId-$marketId-$quality-$creationDate").fold {
        Ok(Json.obj("error" -> "could not find that CSV"))
      }{ result =>
        Ok(result)
      }
    )
  }

  /* Startups Tags CSV ********************************************************************************************************/

  def makeStartupsTagsCSVHeaders(): List[String] = List(
    "Tangela Request Date",
    "startup ID", "Tag Id", "Tag Type",
    "Name", "Display Name", "AngelList Url"
  )

  def makeStartupsTagsCSVValues(values: Seq[JsValue]): List[List[String]] = {
    var group:List[JsValue]= List()
    var group2:List[JsValue]= List()
    values.toList.map { startup =>
      val markets:JsArray= (startup \ "markets").as[JsArray]
      val locations:JsArray= (startup \ "locations").as[JsArray]
      val companies:JsArray= (startup \ "company_type").as[JsArray]
      val id= (startup \ "id").asOpt[Int].getOrElse[Int](0)
      group= group ++ markets.value.toList ++ locations.value.toList ++ companies.value.toList
      group2= group2 ++ group.map { value =>
        Json.obj(
          "startup_id" -> id,
          "id" -> (value \ "id").asOpt[Int].getOrElse[Int](0),
          "tag_type" -> (value \ "tag_type").asOpt[String].getOrElse[String](""),
          "name" -> (value \ "name").asOpt[String].getOrElse[String](""),
          "display_name" -> (value \ "display_name").asOpt[String].getOrElse[String](""),
          "angellist_url" -> (value \ "angellist_url").asOpt[String].getOrElse[String]("")
        )
      }
    }
    group2.map { value =>
      List(
        DatabaseUpdate.getLastAsString,
        (value \ "startup_id").asOpt[Int].getOrElse(0).toString,
        (value \ "id").asOpt[Int].getOrElse(0).toString,
        (value \ "tag_type").asOpt[String].getOrElse(""),
        (value \ "name").asOpt[String].getOrElse(""),
        (value \ "display_name").asOpt[String].getOrElse(""),
        (value \ "angellist_url").asOpt[String].getOrElse("")
      )
    }
  }


  def getStartupsTagsCSV(locationId: Int, marketId: Int, quality: Int, creationDate: String) = Action.async {
    Future(
      CSVManager.get(s"startups-tags-$locationId-$marketId-$quality-$creationDate").fold {
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

  /* Funding CSV for one or more startups *****************************************************************************/

  def makeStartupFundingCSVHeaders = List(
    "tangela request date", "startup ID", "round type", "round raised", "round closed at",
    "round id", "round source url", "participant name", "participant type", "participant id"
  )

  def makeStartupFundingCSVValues(fundings: JsValue): List[List[String]] = fundings.as[List[JsValue]].map{ funding =>
    Json.parse((funding \ "participants").as[String]).as[List[JsValue]] match {
      case Nil => List(emptyParticipant(funding))
      case nonEmpty => nonEmpty map (nonEmptyParticipant(_, funding))
    }
  }.flatten

  def nonEmptyParticipant(participant: JsValue, funding: JsValue): List[String] = startupFundingList(funding) ++ List(
    (participant \ "name").asOpt[String].getOrElse(""),
    (participant \ "type").asOpt[String].getOrElse(""),
    (participant \ "id").asOpt[String].getOrElse("").toString
  )

  def emptyParticipant(funding: JsValue) = startupFundingList(funding) ++ List("", "", "")

  def startupFundingList(funding: JsValue):List[String] = List(
    DatabaseUpdate.getLastAsString,
    (funding \ "startup_id").asOpt[String].getOrElse(""),
    (funding \ "round_type").asOpt[String].getOrElse(""),
    (funding \ "amount").asOpt[Int].getOrElse("").toString,
    (funding \ "closed_at").asOpt[String].getOrElse(""),
    (funding \ "id").asOpt[Int].getOrElse("").toString,
    (funding \ "source_url").asOpt[String].getOrElse("")
  )

  def getStartupFundingCSV(startupId: Long) = Action.async {
    Future(
      CSVManager.get(s"startup-funding-$startupId").fold {
        Ok(Json.obj("error" -> "could not find that CSV"))
      } { result =>
        Ok(result)
      }
    )
  }

  def getStartupsFundingsCSV(locationId: Int, marketId: Int, quality: Int, creationDate: String) = Action.async {
    Future(
      CSVManager.get(s"startups-funding-$locationId-$marketId-$quality-$creationDate").fold {
        Ok(Json.obj("error" -> "could not find that CSV"))
      } { result =>
        Ok(result)
      }
    )
  }
}
