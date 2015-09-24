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
case class AngelRole(id: Long, role: String, user: AngelUser, startup:Startup,
                     created:DateTime, started:Option[DateTime], ended:Option[DateTime],
                      confirmed:Boolean){

  def toCSVRow : Seq[String] = Seq(
    DatabaseUpdate.getLastAsString,
    startup.id.toString(), id.toString(), role,
    created.toString(), started.fold("")(_.toString()), ended.fold("")(_.toString()),
    confirmed.toString(), user.name, user.id.toString(), user.bio.getOrElse(""),
    user.followerCount.fold("")(_.toString()), user.angelURL.getOrElse(""), user.image.getOrElse("")
  )

  def toAngelTagCSVRow : Seq[String] = Seq(
    DatabaseUpdate.getLastAsString,
    id.toString(), "RoleTag", role, ""
  )

  def toTinyJson = Json.obj(
    "id" -> id,
    "role" -> role,
    "user" -> user.toTinyJson,
    "startup" -> startup.toTinyJson
  )

  def +(add : AngelUser) : Option[AngelUserRole] = {
    if(user.id == add.id) Some(AngelUserRole(add, this))
    else None
  }
}

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
      (__ \ "created_at").lazyRead[DateTime](Startup.jodaTimeReads) and
      (__ \ "started_at").lazyReadNullable[DateTime](Startup.jodaTimeReads) and
      (__ \ "ended_at").lazyReadNullable[DateTime](Startup.jodaTimeReads) and
      (__ \ "confirmed").read[Boolean]
    )(AngelRole.apply _)

  implicit val roleWrites: Writes[AngelRole] = new Writes[AngelRole] {
    override def writes(o: AngelRole): JsValue = Json.obj(
      "id" -> o.id,
      "role" -> o.role,
      "tagged" -> o.user,
      "startup" -> o.startup,
      "created_at" -> o.created.toString(),
      "started_at" -> o.started.fold("")(_.toString()),
      "ended_at" -> o.ended.fold("")(_.toString()),
      "confirmed" -> o.confirmed.toString()
    )
  }

  def getCSVHeader :Seq[String] = Seq(
    "tangela request date",
    "startup ID", "id", "role",
    "created at", "started at", "ended at",
    "confirmed", "user name", "user id", "user bio",
    "user follower count", "user angel list url", "user image url"
  )
}
