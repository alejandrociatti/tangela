package models

import play.api.libs.json._

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 16/02/15
 * Time: 22:50
 */
case class Connection(role1:AngelRole, role2:AngelRole)

case class StartupsConnection(role1:AngelRole, role2:AngelRole)

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

}

case class UsersConnection(role1:AngelRole, role2:AngelRole)

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
}
