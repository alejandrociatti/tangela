package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  def demo = Action {
    Ok(views.html.demo())
  }

  def javascriptRoutes = Action { implicit request =>
    Ok(
      Routes.javascriptRouter("jsRoutes")(
        routes.javascript.Locations.getChildrenOf
      )
    ).as("text/javascript")
  }

}