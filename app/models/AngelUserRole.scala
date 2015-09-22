package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 16/02/15
 * Time: 20:55
 */
case class AngelUserRole(user : AngelUser, role : AngelRole){

  def toCSVRow : Seq[String] = user.toCSVRow ++ Seq(
    role.startup.id.toString(), role.startup.name, role.id.toString(),
    role.role, role.created.toString(), role.started.fold("")(_.toString()),
    role.ended.fold("")(_.toString()), role.confirmed.toString()
  )

  def toTinyJson = Json.obj(
    "user" -> user.toTinyJson,
    "role" -> role.toTinyJson
  )
}

object AngelUserRole{
  implicit val userRoleFormat: Format[AngelUserRole] = (
      (__ \ "user").format[AngelUser] and
      (__ \ "role").format[AngelRole]
    )(AngelUserRole.apply, unlift(AngelUserRole.unapply))

  def getCSVHeader :Seq[String] = AngelUser.getCSVHeader ++ Seq(
    "startup ID", "startup name", "role id",
    "role", "created at", "started at",
    "ended at", "confirmed"
  )
}