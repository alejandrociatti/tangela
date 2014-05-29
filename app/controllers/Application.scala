package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
  val AngelApi = "https://api.angel.co/1"


  def demo = Action {
    Ok(views.html.demo())
  }

  def javascriptRoutes = Action { implicit request =>
    Ok(
      Routes.javascriptRouter("jsRoutes")(
        routes.javascript.Locations.getCountriesByString,
        routes.javascript.Startups.getStartupsByLocationId
      )
    ).as("text/javascript")
  }

}