package models

import java.sql.Timestamp

import org.joda.time.DateTime
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import play.api.Play.current
import scala.slick.session.Session
import scala.slick.lifted.MappedTypeMapper.base
import scala.slick.lifted.TypeMapper

/**
 * User: Martin Gutierrez
 * Date: 20/11/14
 * Time: 19:21
 */

case class DatabaseUpdate(guteDate: DateTime, folder: String, id: Option[Long] = None)

object DatabaseUpdates extends Table[DatabaseUpdate]("DATABASE_UPDATE") {
  implicit val DateTimeMapper: TypeMapper[DateTime] = base[DateTime, Timestamp](
      d => new Timestamp(d getMillis),
      t => new DateTime(t getTime)
    )

  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def guteDate = column[DateTime]("GUTE_DATE", O.NotNull)
  def folder = column[String]("FOLDER", O.NotNull)
  def * = guteDate ~ folder ~ id.? <> (DatabaseUpdate.apply _, DatabaseUpdate.unapply _)
}

object DatabaseUpdate {
//  def getLastAsString = DateTime.now.toString("dd/MM/YYYY")
  def getLastAsString = DB.withSession { implicit  session: Session =>
    Query(DatabaseUpdates).sortBy(_.guteDate).list.last.guteDate.toString("dd/MM/YYYY")
  }

  def getLastFolder = DB.withSession { implicit  session: Session =>
    Query(DatabaseUpdates).sortBy(_.guteDate).list.last.folder
  }

  def save(databaseUpdate: DatabaseUpdate) = DB.withSession { implicit  session: Session =>
//    Query(DatabaseUpdate).filter( _.id === databaseUpdate.id ).firstOption.getOrElse {
      DatabaseUpdates.insert(databaseUpdate)
//    }
  }
}