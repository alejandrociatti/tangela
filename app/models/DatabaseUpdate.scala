package models

import org.joda.time.DateTime

/**
 * User: Martin Gutierrez
 * Date: 20/11/14
 * Time: 19:21
 */

class DatabaseUpdate(guteDate: DateTime, folder: String)

object DatabaseUpdate {
  def getLast = DateTime.now
}