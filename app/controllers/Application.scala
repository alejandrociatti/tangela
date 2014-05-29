package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
  val AngelApi = "https://api.angel.co/1"


  def index = Action {
    Ok(views.html.index())
  }
  
  def startupsByLocation = Action {
    Ok(views.html.demo())
  }

  def javascriptRoutes = Action { implicit request =>
    Ok(
      Routes.javascriptRouter("jsRoutes")(
        routes.javascript.Locations.getCountriesByString,
        routes.javascript.Locations.getCountries,
        routes.javascript.Locations.getChildrenOf,
        routes.javascript.Startups.getStartupsByLocationId,
        routes.javascript.Startups.getNumberOfFoundersByStartupId,
        routes.javascript.Startups.getRolesOfStartup
      )
    ).as("text/javascript")
  }

  def startupsInfo() = Action {
    Ok(views.html.startups_info())
  }
}