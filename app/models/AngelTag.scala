package models

import play.api.libs.json._
import play.api.libs.functional.syntax._


/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 16/02/15
 * Time: 23:41
 */
case class AngelTag(id: Long, name:String, tagType:String)


object AngelTag{

  implicit val tagReads:Reads[AngelTag] = (
      (__ \ "id").read[Long] and
      (__ \ "name").read[String] and
      (__ \ "tag_type").read[String]
    )(AngelTag.apply _)

  implicit val tagWrites:Writes[AngelTag] = new Writes[AngelTag] {
    override def writes(o: AngelTag): JsValue = Json.obj(
      "id" -> o.id,
      "name" -> o.name,
      "tag_type" -> o.tagType
    )
  }
}