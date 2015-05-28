package controllers


import models.authentication.Role._
import play.api._
import play.api.mvc._
import scala.concurrent.ExecutionContext

object Application extends Controller with Secured{

  def index = withAuth(Admin, Researcher) { user => implicit request =>
    Ok(views.html.index())
  }
  
  def startupsByLocation = withAuth(Admin, Researcher) { user => implicit request =>
    Ok(views.html.startups())
  }

  def searchStartups = withAuth(Admin, Researcher) { user => implicit request =>
    Ok(views.html.search())
  }

  def javascriptRoutes = Action { implicit request =>
    Ok(
      Routes.javascriptRouter("jsRoutes")(
        routes.javascript.Locations.getCountriesByString,
        routes.javascript.Locations.getCountries,
        routes.javascript.Locations.getChildrenOf,
        routes.javascript.Startups.getNumberOfFoundersByStartupId,
        routes.javascript.Startups.getRolesOfStartup,
        routes.javascript.Startups.getStartupNetInfo,
        routes.javascript.Startups.getStartupsByName,
        routes.javascript.Startups.getStartupFunding,
        routes.javascript.CSVs.getStartupsCSV,
        routes.javascript.CSVs.getUsersCSV,
        routes.javascript.CSVs.getStartupsTagsCSV,
        routes.javascript.Startups.startupCriteriaSearch,
        routes.javascript.Startups.startupCriteriaSearchAndTags,
        routes.javascript.Networks.getStartupsNetwork,
        routes.javascript.Networks.getPeopleNetwork,
        routes.javascript.Networks.getPeopleNetwork2ndOrder,
        routes.javascript.CSVs.getStartupsNetworkCSV,
        routes.javascript.CSVs.getPeopleNetworkCSV,
        routes.javascript.CSVs.getPeopleNetwork2ndOrderCSV,
        routes.javascript.CSVs.getStartupRolesCSV,
        routes.javascript.CSVs.getStartupFundingCSV,
        routes.javascript.CSVs.getStartupsFundingsCSV,
        routes.javascript.Startups.getUsersInfoByCriteria,
        routes.javascript.Startups.startupsFundingByCriteria
      )
    ).as("text/javascript")
  }

  def startupsInfo = withAuth(Admin, Researcher) { user => implicit request =>
    Ok(views.html.startupsInfo())
  }

  def startupsPeopleInfo = withAuth(Admin, Researcher) { user => implicit request =>
    Ok(views.html.startupsPeopleInfo())
  }

  def startupsFundingInfo() = withAuth(Admin, Researcher) { username => implicit request =>
    Ok(views.html.startupsFundingInfo())
  }

  def startupsNetwork = withAuth(Admin, Researcher) { user => implicit request =>
    Ok(views.html.startupNetwork())
  }

  def startupsPeopleNetwork = withAuth(Admin, Researcher) { username => implicit request =>
    Ok(views.html.startupsPeopleNetwork())
  }
}