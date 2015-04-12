package models

import play.api.libs.json._

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 16/02/15
 * Time: 22:50
 */
case class Connection(role1:AngelRole, role2:AngelRole)

case class StartupsConnection(role1:AngelRole, role2:AngelRole){

  def toCSVRow: Seq[String] = Seq(
    role1.startup.id.toString,
    role2.startup.id.toString,
    role1.startup.name,
    role2.startup.name,
    role1.created.toString,
    role2.created.toString,
    role1.role,
    role2.role,
    role1.user.id.toString,
    role1.user.name
  )
}

object StartupsConnection {

  implicit val connectionWrites: Writes[StartupsConnection] = new Writes[StartupsConnection] {
    override def writes(o: StartupsConnection): JsValue = Json.obj(
      "startupIdOne" -> o.role1.startup.id,
      "startupIdTwo" -> o.role2.startup.id,
      "startupNameOne" -> o.role1.startup.name,
      "startupNameTwo" -> o.role2.startup.name,
      "createdAtOne" -> o.role1.created.toString(),
      "createdAtTwo" -> o.role2.created.toString(),
      "roleOne" -> o.role1.role,
      "roleTwo" -> o.role2.role,
      "userId" -> o.role1.user.id,
      "userName" -> o.role1.user.name
    )
  }

  def getCSVHeader:Seq[String] = Seq(
    "tangela request date",
    "startup ID one", "startup name one", "user role in startup one", "created at one",
    "startup id two", "startup name two", "user role in startup two", "created at two",
    "user in common ID", "user in common name"
  )

}

case class UsersConnection(role1:AngelRole, role2:AngelRole){

  def toCSVRow:Seq[String] = Seq(
    role1.user.id.toString,
    role2.user.id.toString,
    role1.user.name,
    role2.user.name,
    role1.created.toString,
    role2.created.toString,
    role1.role,
    role1.role,
    role1.startup.id.toString,
    role1.startup.name
  )

}

object UsersConnection {

  implicit val connectionWrites: Writes[UsersConnection] = new Writes[UsersConnection] {
    override def writes(o: UsersConnection): JsValue = Json.obj(
      "userIdOne" -> o.role1.user.id,
      "userIdTwo" -> o.role2.user.id,
      "userNameOne" -> o.role1.user.name,
      "userNameTwo" -> o.role2.user.name,
      "createdAtOne" -> o.role1.created.toString(),
      "createdAtTwo" -> o.role2.created.toString(),
      "roleOne" -> o.role1.role,
      "roleTwo" -> o.role1.role,
      "startupId" -> o.role1.startup.id,
      "startupName" -> o.role1.startup.name
    )
  }

  def getCSVHeader:Seq[String] = Seq(
    "tangela request date",
    "user ID one", "user name one", "user role one", "created at one",
    "user id two", "user name two", "user role two", "created at two",
    "startup in common ID", "startup in common name"
  )
}
