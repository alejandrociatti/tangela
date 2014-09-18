package controllers

import models.authentication.Role.Role
import models.authentication.User
import play.api.mvc._
import play.api.data.Forms._
import play.api.data.Form
import scala.concurrent.Future

/**
 * Created by Javier Isoldi.
 * Date: 5/16/14.
 * Project: Tangela.
 */

object Authentication extends Controller{
  val loginForm = Form(
    tuple(
      "email" -> text,
      "password" -> text
    ).verifying ("Invalid email or password", result => result match {
      case (email, password) => check(email, password)
    })
  )

  def check(username: String, password: String) = {
    username == "admin" && password == "1234"
  }

  def login = Action { implicit request =>
    Ok(views.html.login(loginForm))
  }

  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.login(formWithErrors)),
      user => Redirect(routes.Application.index).withSession(Security.username -> user._1)
    )
  }

  def logout = Action {
    Redirect(routes.Authentication.login).withNewSession.flashing(
      "success" -> "You are now logged out."
    )
  }
}

trait Secured {
  def isAuthenticated(request: RequestHeader, roles: Seq[Role]): Option[User] = {
    request.session.get(Security.username)
      .flatMap(username =>
      User.getByUsername(username)
    )
  }

  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Authentication.login)

  def withAuth(roles: Role*)(f: => User => Request[AnyContent] => Result) = {
    Security.Authenticated(request => isAuthenticated(request, roles), onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }

  def withAsyncAuth(roles: Role*)(f: => User => Request[AnyContent] => Future[SimpleResult]) = {
    Security.Authenticated(request => isAuthenticated(request, roles), onUnauthorized) { user =>
      Action.async { request => f(user)(request) }
    }
  }

  def rolesToString(role: Role) = role.toString
}
