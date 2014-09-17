package models

import org.joda.time.DateTime
import sorm.Entity

/**
 * Created by Javier Isoldi.
 * Date: 5/16/14.
 * Project: Tangela.
 */

case class Startup(name: String, angelId: Long, quality: Int, creationDate: DateTime)

object Startup {
  def getEntity = Entity[Startup]()

  def getById(id: Long): Option[Startup] = Database.query[Startup].whereEqual("id", id).fetchOne()

  def save(startup: Startup) =
    if (Database.query[Startup].whereEqual("angelId", startup.angelId).count() == 0) {
      Database.save(startup)
    }
}


