package models

import java.sql.Date

import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import play.api.Play.current
import scala.slick.lifted.MappedTypeMapper
import scala.slick.session.Session

import org.joda.time.DateTime
import util.DateTimeMapper._

/**
 * Created by Javier Isoldi.
 * Date: 5/16/14.
 * Project: Tangela.
 */

case class Startup(name: String, angelId: Long, quality: Int, creationDate: DateTime, id: Option[Long] = None)

object Startups extends Table[Startup]("STARTUP") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def name = column[String]("NAME", O.NotNull)
  def angelId = column[Long]("ANGEL_ID", O.NotNull)
  def quality = column[Int]("QUALITY", O.NotNull)
  def creationDate = column[DateTime]("CREATION_DATE", O.NotNull)
  def * = name ~ angelId ~ quality ~ creationDate ~ id.? <> (Startup.apply _, Startup.unapply _)
}

object Startup {
//  def getById(id: Long): Option[Startup] = Database.query[Startup].whereEqual("id", id).fetchOne()

  def getById(id: Long): Option[Startup] = DB.withSession { implicit  session: Session =>
    Query(Startups).filter(_.id === id).firstOption
  }

//  def save(startup: Startup) =
//    if (Database.query[Startup].whereEqual("angelId", startup.angelId).count() == 0) {
//      Database.save(startup)

  def save(startup: Startup) = DB.withSession { implicit session: Session =>
    Query(Startups).filter( _.angelId === startup.angelId ).firstOption.getOrElse{
      Startups.insert(startup)
    }
  }
}


