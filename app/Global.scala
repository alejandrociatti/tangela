import org.joda.time.{LocalDate, LocalTime}
import play.api._
import play.libs.Akka
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 03/06/14
 * Time: 12:54
 */
object Global extends GlobalSettings{

  override def onStart(app: Application) {
    super.onStart(app)
    //Start On: Today at 3:00 AM (24-hour format)
    val startOn = LocalDate.now().toLocalDateTime(new LocalTime(3,00))
    //Schedule task:
    Akka.system.scheduler.schedule(
      //Initial delay: time remaining from now to startOn(3AM)
      new org.joda.time.Duration(startOn.toDateTime, null).getMillis.millis,
      //Once every day
      1.days
    ){
      //Task:
      //TODO: 'TRUNCATE TABLE X' SQL WHEN WE HAVE THEM
      println("Cron Job just ran.")
    }
  }

}
