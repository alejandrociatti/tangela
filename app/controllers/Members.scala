package controllers

import models.{AngelUserRole, AngelRole, AngelUser, Startup}
import play.api.Logger
import play.api.libs.json._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


/**
 * Created by Javier Isoldi.
 * Date: 5/16/14.
 * Project: Tangela.
 */

object Members {

  /**
   * get users from startup
   * @param startup object representing a startup
   */
  def getFromStartup(startup: Startup): Future[Seq[AngelUser]] =
    AngelListServices.getRolesFromStartupId(startup.id) map { response =>
      (response \ "startup_roles").asOpt[Seq[JsValue]].fold {
        Seq[AngelUser]()
      }{ roles =>
        roles.filter(isUserFilter).map(role => role.validate[AngelUser].get).distinct
      }
    }

  def getFromStartups(startups: Seq[Startup]): Future[Seq[AngelUser]] =
    Future.sequence(startups map getFromStartup).map(_.flatten.distinct)

  def getFromRole(role: AngelRole): Future[AngelUser] = AngelListServices.getUserById(role.user.id).map( jsValue =>
      jsValue.validate[AngelUser].get
    )

  def getFromRoles(roles: Seq[AngelRole]): Future[Seq[AngelUser]] = Future.sequence(roles map getFromRole)

  def getWithRole(role: AngelRole): Future[Option[AngelUserRole]] =
    AngelListServices.getUserById(role.user.id).map{ jsValue =>
      if(isUserFilter(jsValue)) jsValue.validate[AngelUser].get + role
      else None
    }

  def getWithRoles(roles: Seq[AngelRole]) : Future[Seq[AngelUserRole]] =
    Future.sequence(roles map getWithRole).map(_.flatten)

  def userIDsFromStartup(startup: Startup): Future[Seq[Long]] = {
    AngelListServices.getRolesFromStartupId(startup.id) map { response =>
      (response \ "startup_roles").asOpt[JsArray].fold {
        Seq[Long]()
      }{ roles =>
        roles.value.filter(role => (role \ "user") != JsNull).map(role =>
          (role \ "tagged" \ "id").as[Long]
        ).distinct
      }
    }
  }

  def userIDsFromStartups(startups: Seq[Startup]): Future[Seq[Seq[Long]]] =
    Future.sequence(startups map userIDsFromStartup).map(_.distinct)

  def userIDsFromStartupsFlat(startups: Seq[Startup]): Future[Seq[Long]] =
    Future.sequence(startups map userIDsFromStartup).map(_.flatten.distinct)

  def userIDsFromStartupID(startupId: Long): Future[Seq[Long]] = {
    AngelListServices.getRolesFromStartupId(startupId) map { response =>
      (response \ "startup_roles").asOpt[JsArray].fold {
        Seq[Long]()
      }{ roles =>
        roles.value.filter(role => (role \ "user") != JsNull).map(role =>
          (role \ "tagged" \ "id").as[Long]
        ).distinct
      }
    }
  }

  def userIDsFromStartupIDsFlat(startups: Seq[Long]): Future[Seq[Long]] =
    Future.sequence(startups map userIDsFromStartupID).map(_.flatten.distinct)

  def userIDsFromStartupIDs(startups: Seq[Long]): Future[Seq[Seq[Long]]] =
    Future.sequence(startups map userIDsFromStartupID).map(_.distinct)


  def isUserFilter(user: JsValue) =
    user.validate[AngelUser] match {
      case role:JsSuccess[AngelUser] => true
      case err:JsError => Logger.warn("Failed to read user json: "+ JsError.toFlatJson(err).toString()); false
      case _ => Logger.warn("Failed to read role json: "+user.toString()); false
    }

}
