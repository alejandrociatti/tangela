package models.authentication

import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import play.api.Play.current
import scala.slick.session.Session


/**
 * Created by Javier Isoldi.
 * Date: 5/15/14.
 * Project: Tangela.
 */

case class User(
                 username: String,
                 password: String,
                 firstName: String,
                 lastName: String,
                 role: String,
                 id: Option[Long] = None
                 )

object Users extends Table[User]("USERS") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def username = column[String]("USERNAME", O.NotNull)
  def password = column[String]("PASSWORD", O.NotNull)
  def firstName = column[String]("FIRST_NAME", O.NotNull)
  def lastName = column[String]("LAST_NAME", O.NotNull)
  def role = column[String]("ROLE", O.NotNull)
  def * = username ~ password ~ firstName ~ lastName ~ role ~ id.? <> (User.apply _, User.unapply _)
}

object User {
//  def getByUsername(username: String) = Database.query[User].whereEqual("username", username).fetchOne()
  def getByUsername(username: String) = DB.withSession { implicit  session: Session =>
    Query(Users).filter( _.username === username).firstOption
  }
}

object Role extends Enumeration {
  type Role = Value
  val Admin = Value("Admin")
  val Researcher = Value("Researcher")
}