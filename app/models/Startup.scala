package models

import anorm.{NotAssigned, Pk}
import play.api.libs.json.Json

/**
 * Created by Javi on 5/15/14.
 */
case class Startup(id: Pk[Long] = NotAssigned, name: String) {

}

case class Location(id: Pk[Long] = NotAssigned, name: String, angelId:Long) {

}

object Location{

  def mockLocations = {
    Seq(new Location(NotAssigned,"Argentina", 1613), new Location(NotAssigned,"Brazil",1622))
  }
}