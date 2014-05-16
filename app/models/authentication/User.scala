package models.authentication

import anorm.Pk

/**
 * Created by Javi on 5/15/14.
 */
class User(id : Pk[Long], username : String, password : String)

case class Admin(id : Pk[Long], username : String, password : String) extends User(id, username, password)
case class Researcher(id : Pk[Long], username : String, password : String) extends User(id, username, password)

object User {

  def authenticate(email: String, password: String): Option[User] = {
    Option[User](null)
  }
}