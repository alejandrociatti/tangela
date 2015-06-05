package models

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.Logger
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.util.{Failure, Success, Try}

/**
 * Created by Javier Isoldi.
 * Date: 5/16/14.
 * Project: Tangela.
 */

case class Startup(id: Long, name: String, quality: Option[Int],
                   created: Option[DateTime], angelURL:Option[String], logoURL:Option[String],
                   thumbURL:Option[String], companyURL:Option[String], twitterURL:Option[String],
                   blogURL:Option[String], videoURL:Option[String], description:Option[String],
                   concept:Option[String], followerCount:Option[Int], updated:Option[DateTime],
                   markets:Option[Seq[AngelTag]], locations:Option[Seq[AngelTag]],
                   funding:Option[Seq[Funding]]){

  def toTinyJson = Json.obj("id" -> id, "name" -> name)

  def toCSVRow :Seq[String] = Seq(
    DatabaseUpdate.getLastAsString,
    id.toString(), name,
    angelURL.getOrElse(""), logoURL.getOrElse(""), thumbURL.getOrElse(""),
    quality.fold("")(_.toString()), description.getOrElse(""), concept.getOrElse(""),
    followerCount.fold("")(_.toString()), companyURL.getOrElse(""),
    created.fold("")(_.toString()), updated.toString(), twitterURL.getOrElse(""),
    blogURL.getOrElse(""), videoURL.getOrElse("")
  )

  def getTagsCSVRows : Seq[String] =
    markets.fold(Seq[String]())(_.flatMap(Seq(id.toString())++_.toCSVRow)) ++
    locations.fold(Seq[String]())(_.flatMap(Seq(id.toString())++_.toCSVRow))

  def getTagsJsons : Seq[JsValue] =
    markets.fold(Seq[JsValue]())(_.map(Json.obj("id" -> id.toString()) ++ Json.toJson(_).as[JsObject])) ++
    locations.fold(Seq[JsValue]())(_.map(Json.obj("id" -> id.toString()) ++ Json.toJson(_).as[JsObject]))
}

object Startup{
  implicit val jodaTimeReads:Reads[DateTime] = new Reads[DateTime] {
    override def reads(json: JsValue): JsResult[DateTime] = json match{
      case JsNumber(n) => JsSuccess(new DateTime(n.toLong))
      case JsString(s) =>
        JsSuccess(
          Try(DateTime.parse(s)) match {
            case Success(dateTime) => dateTime
            case Failure(e) =>
              Logger.warn(s"DateTime.parse error: ${e.getMessage}")
              DateTimeFormat.forPattern("yyyy-MM-dd").withZoneUTC().parseDateTime(s)
          }
        )
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.date"))))
    }
  }

  implicit val jodaTimeWrites:Writes[DateTime] = new Writes[DateTime] {
    override def writes(o: DateTime): JsValue = JsString(o.toString)
  }

  implicit val startupReads: Reads[Startup] = (
      (__ \ "id").read[Long] and
      (__ \ "name").read[String] and
      (__ \ "quality").readNullable[Int] and
      (__ \ "created_at").readNullable[DateTime] and
      (__ \ "angellist_url").readNullable[String] and
      (__ \ "logo_url").readNullable[String] and
      (__ \ "thumb_url").readNullable[String] and
      (__ \ "company_url").readNullable[String] and
      (__ \ "twitter_url").readNullable[String] and
      (__ \ "blog_url").readNullable[String] and
      (__ \ "video_url").readNullable[String] and
      (__ \ "product_desc").readNullable[String] and
      (__ \ "high_concept").readNullable[String] and
      (__ \ "follower_count").readNullable[Int] and
      (__ \ "updated_at").readNullable[DateTime] and
      (__ \ "markets").readNullable[Seq[AngelTag]] and
      (__ \ "locations").readNullable[Seq[AngelTag]] and
      (__ \ "funding").readNullable[Seq[Funding]]
    )(Startup.apply _)

  implicit val startupWrites :Writes[Startup] = (
      (__ \ "id").write[Long] and
      (__ \ "name").write[String] and
      (__ \ "quality").writeNullable[Int] and
      (__ \ "created_at").writeNullable[DateTime] and
      (__ \ "angellist_url").writeNullable[String] and
      (__ \ "logo_url").writeNullable[String] and
      (__ \ "thumb_url").writeNullable[String] and
      (__ \ "company_url").writeNullable[String] and
      (__ \ "twitter_url").writeNullable[String] and
      (__ \ "blog_url").writeNullable[String] and
      (__ \ "video_url").writeNullable[String] and
      (__ \ "product_desc").writeNullable[String] and
      (__ \ "high_concept").writeNullable[String] and
      (__ \ "follower_count").writeNullable[Int] and
      (__ \ "updated_at").writeNullable[DateTime] and
      (__ \ "markets").writeNullable[Seq[AngelTag]] and
      (__ \ "locations").writeNullable[Seq[AngelTag]] and
      (__ \ "funding").writeNullable[Seq[Funding]]
    )(unlift(Startup.unapply))

  def getCSVHeader :Seq[String] = Seq(
    "Tangela Request Date",
    "id","name","angellist_url","logo_url","thumb_url","quality",
    "product_desc","high_concept","follower_count","company_url","created_at","updated_at",
    "twitter_url","blog_url","video_url"
  )
}
