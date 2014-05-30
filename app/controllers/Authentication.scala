package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.Form
import scala.concurrent.Future

/**
 * Created by Javi on 5/16/14.
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
    (username == "admin" && password == "1234")
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
  def username(request: RequestHeader) = request.session.get(Security.username)

  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Authentication.login)

  def withAuth(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }

  def withAsyncAuth(f: => String => Request[AnyContent] => Future[SimpleResult]) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action.async { request => f(user)(request) }
    }
  }
}
