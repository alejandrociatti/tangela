package controllers

import models.authentication.User
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.Controller
import models.authentication.Role.Admin
import views.html.user.{user, users}

/**
 * Created by Javier Isoldi.
 * Date: 18/09/14.
 * Project: tangela.
 */

object Users extends Controller with Secured {

  val UserForm = Form(
    mapping(
      "Username" -> nonEmptyText,
      "Password" -> nonEmptyText,
      "First Name" -> nonEmptyText,
      "Last Name" -> nonEmptyText,
      "Role" -> nonEmptyText,
      "id" -> optional(longNumber)
    )(User.apply)(User.unapply)
  )

  def listUsers = withAuth(Admin) { username => implicit request =>
    Ok(users(User.getAll))
  }

  def newUser = withAuth(Admin) { username => implicit request =>
    Ok(user(UserForm))
  }

  def editUser(id: Long) = withAuth(Admin) { username => implicit request =>
    User.getById(id).fold {
      Redirect(routes.Users.listUsers)
    } { eUser =>
      Ok(user(UserForm.fill(eUser)))
    }
  }

  def saveUser = withAuth(Admin) {  username => implicit request =>
    val form = UserForm.bindFromRequest()
    if (form.hasErrors) {
      Ok(user(form))
    } else {
      User.save(form.get)
      Redirect(routes.Users.listUsers)
    }
  }

}
