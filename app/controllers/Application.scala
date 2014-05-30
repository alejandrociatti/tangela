package controllers

import play.api._
import play.api.mvc._

object Application extends Controller with Secured{
  val AngelApi = "https://api.angel.co/1"


  def index = withAuth { username => implicit request =>
    Ok(views.html.index())
  }
  
  def startupsByLocation = withAuth { username => implicit request =>
    Ok(views.html.startupsByLocation())
  }

  def javascriptRoutes = Action { implicit request =>
    Ok(
      Routes.javascriptRouter("jsRoutes")(
        routes.javascript.Locations.getCountriesByString,
        routes.javascript.Locations.getCountries,
        routes.javascript.Startups.getStartupsByLocationId
      )
    ).as("text/javascript")
  }

}