package models.authentication

import sorm.Entity

/**
 * Created by Javier Isoldi.
 * Date: 5/15/14.
 * Project: Tangela.
 */

case class User(username: String, password: String, firstName: String, lastName: String, role: Role)

object User {
  def getEntity = Entity[User](unique = Set(Seq("username")))
}

case class Role(name: String)

object Role {
  def getEntity = Entity[Role]()
}