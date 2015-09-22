package models

import play.api.libs.json._
import play.api.libs.functional.syntax._


/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 16/02/15
 * Time: 23:41
 */
case class AngelTag(id: Long, name:String, tagType:String, angelURL:Option[String]){

  def toCSVRow : Seq[String] = Seq(
    DatabaseUpdate.getLastAsString,
    id.toString(), tagType, name, angelURL.getOrElse("")
  )
}

object AngelTag{

  implicit val tagReads:Reads[AngelTag] = (
      (__ \ "id").read[Long] and
      (__ \ "name").read[String] and
      (__ \ "tag_type").read[String] and
      (__ \ "tag_type").readNullable[String]
    )(AngelTag.apply _)

  implicit val tagWrites:Writes[AngelTag] = new Writes[AngelTag] {
    override def writes(o: AngelTag): JsValue = Json.obj(
      "id" -> o.id,
      "name" -> o.name,
      "tag_type" -> o.tagType
    )
  }

  def getCSVHeader: Seq[String] = Seq(
    "Tangela Request Date",
    "startup ID", "Tag Id", "Tag Type",
    "Name", "AngelList Url"
  )
}