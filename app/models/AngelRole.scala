package models

import org.joda.time.DateTime
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 16/02/15
 * Time: 20:55
 */
case class AngelRole(id: Long, role: String, user: AngelUser, startup:Startup, created:DateTime)

object AngelRole{
  implicit val jodaTimeReads:Reads[DateTime] = new Reads[DateTime] {
    override def reads(json: JsValue): JsResult[DateTime] = json match{
      case JsNumber(n) => JsSuccess(new DateTime(n.toLong))
      case JsString(s) => JsSuccess(DateTime.parse(s))
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.date"))))
    }
  }

  implicit val roleReads: Reads[AngelRole] = (
      (__ \ "id").read[Long] and
      (__ \ "role").read[String] and
      (__ \ "tagged").lazyRead[AngelUser](AngelUser.userReads) and
      (__ \ "startup").lazyRead[Startup](Startup.startupReads) and
      (__ \ "created_at").lazyRead[DateTime](Startup.jodaTimeReads)
    )(AngelRole.apply _)

  implicit val roleWrites: Writes[AngelRole] = new Writes[AngelRole] {
    override def writes(o: AngelRole): JsValue = Json.obj(
      "id" -> o.id,
      "role" -> o.role,
      "tagged" -> o.user,
      "startup" -> o.startup,
      "created_at" -> o.created
    )
  }
}