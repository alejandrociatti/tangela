package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
  val AngelApi = "https://api.angel.co/1"


  def index = Action {
    Ok(views.html.demo())
  }
  
  def startupsByLocation = Action {
    Ok(views.html.startupsByLocation())
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