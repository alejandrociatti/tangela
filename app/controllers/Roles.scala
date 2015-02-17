package controllers

import models.{AngelRole, Startup}
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.Controller

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 16/02/15
 * Time: 23:59
 */
object Roles extends Controller {

  /**
   * Obtains all the roles that belong to a startup and the associated users
   * @param startup
   * @return A Future of Seq of all corresponding roles
   */
  def getFromStartup(startup: Startup): Future[Seq[AngelRole]] = getFromStartupID(startup.id)

  def getFromStartupID(startupID: Long): Future[Seq[AngelRole]] =
    AngelListServices.getRolesFromStartupId(startupID) map { response =>
      (response \ "startup_roles").asOpt[Seq[JsValue]].fold {
        Seq[AngelRole]()
      }{ roles =>
        roles.filter(isRoleFilter).map(_.validate[AngelRole].get)
      }
    }

  /**
   * Obtains all the roles that belong to a startup and the associated users
   * @param roles JsValue that contains the startup id and name
   * @return A Future of Seq of all corresponding roles from all people as JsValues
   */
  def getExtendedRoles(roles: Seq[AngelRole]): Future[Seq[AngelRole]] = getRolesFromUserIDs(roles.map(_.id))

  def getRolesFromUserIDs(users: Seq[Long]): Future[Seq[AngelRole]] =
    Future.sequence(
      users.map{id =>
        AngelListServices.getRolesFromUserId( id ) map { response =>
          (response \ "startup_roles").asOpt[Seq[JsValue]].fold {
            Seq[AngelRole]()
          } { roles =>
            roles.filter(isRoleFilter).map(_.validate[AngelRole].get)
          }
        }
      }
    ).map(_.flatten.distinct)

  def isRoleFilter(role: JsValue) =
    role.validate[AngelRole] match {
      case role:JsSuccess[AngelRole] => true
      case err:JsError => Logger.warn("Failed to read role json: "+ JsError.toFlatJson(err).toString()); false
      case _ => Logger.warn("Failed to read role json: "+role.toString()); false
    }

}
