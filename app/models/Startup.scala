package models

import org.joda.time.DateTime
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Created by Javier Isoldi.
 * Date: 5/16/14.
 * Project: Tangela.
 */

case class Startup(id: Long, name: String, quality: Int, created: DateTime,
                   angelURL:Option[String], logoURL:Option[String], thumbURL:Option[String],
                   companyURL:Option[String], twitterURL:Option[String], blogURL:Option[String],
                   videoURL:Option[String],
                   description:Option[String], concept:Option[String], followerCount:Option[Int],
                   updated:Option[DateTime], markets:Option[Seq[AngelTag]], locations:Option[Seq[AngelTag]]){

  def toTinyJson = Json.obj("id" -> id, "name" -> name)
}

object Startup{
  implicit val jodaTimeReads:Reads[DateTime] = new Reads[DateTime] {
    override def reads(json: JsValue): JsResult[DateTime] = json match{
      case JsNumber(n) => JsSuccess(new DateTime(n.toLong))
      case JsString(s) => JsSuccess(DateTime.parse(s))
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.date"))))
    }
  }

  implicit val jodaTimeWrites:Writes[DateTime] = new Writes[DateTime] {
    override def writes(o: DateTime): JsValue = JsString(o.toString)
  }

  implicit val startupReads: Reads[Startup] = (
      (__ \ "id").read[Long] and
      (__ \ "name").read[String] and
      (__ \ "quality").read[Int] and
      (__ \ "created_at").read[DateTime] and
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
      (__ \ "locations").readNullable[Seq[AngelTag]]
    )(Startup.apply _)

  implicit val startupWrites :Writes[Startup] = (
      (__ \ "id").write[Long] and
      (__ \ "name").write[String] and
      (__ \ "quality").write[Int] and
      (__ \ "created_at").write[DateTime] and
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
      (__ \ "locations").writeNullable[Seq[AngelTag]]
    )(unlift(Startup.unapply))
}
