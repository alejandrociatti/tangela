package models

import controllers.AngelListServices
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import play.api.Play.current
import play.api.libs.json._
import scala.concurrent.Future
import scala.slick.session.Session
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Created with IntelliJ IDEA by: alejandro
 * Date: 29/05/14
 * Time: 14:04
 */

case class Location(name: String, angelId: Long, kind: String, id: Option[Long] = None){

  def getChildren : Future[Seq[Location]] = {

    def futurePageLoader(page: Int):Future[Seq[Location]] =
      AngelListServices.getChildrenOfTagAndPage(angelId)(page).map { response =>
        (response \ "children").as[Seq[JsValue]].map{ jsLocation : JsValue =>
          Location((jsLocation \ "name").as[String], (jsLocation \ "id").as[Long], Kind.Other.toString)
        }
      }

    AngelListServices.getChildrenOfTag(angelId).flatMap { response =>
      val firstPage = (response \ "children").as[Seq[JsValue]].map{ jsLocation : JsValue =>
        Location((jsLocation \ "name").as[String], (jsLocation \ "id").as[Long], Kind.Other.toString)
      }

      Future.sequence(
        (2 to (response \ "last_page").as[Int]).map(futurePageLoader)
      ).map(_.flatten).map(firstPage ++ _)
    }
  }
}

object Locations extends Table[Location]("LOCATION") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def name = column[String]("NAME", O.NotNull)
  def angelId = column[Long]("ANGEL_ID", O.NotNull)
  def kind = column[String]("KIND", O.NotNull)
  def * = name ~ angelId ~ kind ~ id.? <> (Location.apply _, Location.unapply _)
}

object Location {

  def getById(id: Long): Option[Location] = DB.withSession { implicit session: Session =>
    Query(Locations).filter( _.id === id).firstOption
  }

  def getByAngelId(id: Long): Option[Location] = DB.withSession { implicit session: Session =>
    Query(Locations).filter( _.angelId === id).firstOption
  }

  def clearAll() = DB.withSession { implicit  session: Session =>
    Query(Locations).delete
  }

  def getCountries: List[Location] = DB.withSession { implicit session: Session =>
    Query(Locations).filter( _.kind === Kind.Country.toString ).sortBy(_.name).list
  }

  def getOtherThanCountries: List[Location] = DB.withSession { implicit  session: Session =>
    Query(Locations).filter( _.kind === Kind.Other.toString ).sortBy(_.name).list
  }

  def save(location: Location) = DB.withSession { implicit  session: Session =>
    Query(Locations).filter( _.angelId === location.angelId ).firstOption.getOrElse {
      Locations.insert(location)
    }
  }

  def saveRelation(locationId: Long, startupId: Long) = DB.withSession { implicit session: Session =>
    StartupLocations.insert(StartupLocation(startupId, locationId))
  }
}

case class StartupLocation(startup: Long, location: Long, id: Option[Long] = None)

object StartupLocations extends Table[StartupLocation]("STARTUP_LOCATION") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def startup = column[Long]("STARTUP_ID", O.NotNull)
  def location = column[Long]("LOCATION_ID", O.NotNull)
  def * = startup ~ location ~ id.? <> (StartupLocation.apply _, StartupLocation.unapply _)
}

object StartupLocation {}

object Kind extends Enumeration {
  type Kind = Value
  val Country = Value("COUNTRY")
  val Other = Value("OTHER")
}
