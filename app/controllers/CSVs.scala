package controllers

import play.api.mvc.{ResponseHeader, SimpleResult, Action, Controller}
import util.CSVManager
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

import scala.concurrent.Future

/**
 * User: Martin Gutierrez
 * Date: 21/11/14
 * Time: 01:58
 */
object CSVs extends Controller{


  /* People Network CSV ***********************************************************************************************/

  def getPeopleNetworkCSV(locationId: Int, marketId: Int, quality: String, creationDate: String) =
    getCsv(s"people-net-$locationId-$marketId-$quality-$creationDate")

  def getPeopleNetwork2ndOrderCSV(locationId: Int, marketId: Int, quality: String, creationDate: String) =
    getCsv(s"people-net-2-$locationId-$marketId-$quality-$creationDate")

  /* Startup Network CSV **********************************************************************************************/

  def getStartupsNetworkCSV(locationId: Int, marketId: Int, quality: String, creationDate: String) =
    getCsv(s"startup-net-$locationId-$marketId-$quality-$creationDate")

  /* Startups CSV *****************************************************************************************************/

  def getStartupsCSV(locationId: Int, marketId: Int, quality: String, creationDate: String) =
    getCsv(s"startups-$locationId-$marketId-$quality-$creationDate")


  /* Users CSV ********************************************************************************************************/

  def getUsersCSV(locationId: Int, marketId: Int, quality: String, creationDate: String) =
    getCsv(s"users-$locationId-$marketId-$quality-$creationDate")
  
  /* Startups Tags CSV ********************************************************************************************************/

  def getStartupsTagsCSV(locationId: Int, marketId: Int, quality: String, creationDate: String) =
    getCsv(s"startups-tags-$locationId-$marketId-$quality-$creationDate")


  /* Roles CSV ********************************************************************************************************/

  def getStartupRolesCSV(startupId: Long) = getCsv(s"startup-roles-$startupId")

  /* Funding CSV for one or more startups *****************************************************************************/

  def getStartupFundingCSV(startupId: Long) = getCsv(s"startup-funding-$startupId")

  def getStartupsFundingsCSV(locationId: Int, marketId: Int, quality: String, creationDate: String) =
    getCsv(s"startups-funding-$locationId-$marketId-$quality-$creationDate")

  /* Helpers ---------------------------------------------------------------------------------------------------- */

  def getCsv(name: String) = Action.async {
    Future(
      CSVManager.getFile(name).fold {
        NotFound("CSV NOT FOUND")
      } { result =>
        val newName = name.replace(",", "")
        SimpleResult(
          header = ResponseHeader(200,
            Map(
              CONTENT_LENGTH -> result._2.toString,
              CONTENT_TYPE -> "text/csv",
              CONTENT_DISPOSITION -> s"attachment;filename=$newName.csv"
            )
          ),
          body = result._1
        )
      }
    )
  }
}
