package controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import util.RequestSerializer
import util.DummyRequest._
import models.authentication.Role._

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 24/05/15
 * Time: 13:58
 */
object Requests extends Controller with Secured{

  def requests = withAuth(Admin, Researcher) { user => implicit request =>
    Ok(views.html.requests.render(RequestSerializer.getRequests))
  }

  def getRequests = Action{
    Ok(Json.toJson(RequestSerializer.getRequests))
  }

}
