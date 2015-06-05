package util

import java.io.File

import models.DatabaseUpdate
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.mvc.SimpleResult
import play.api.mvc.Results.Ok

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.duration.Duration.Inf
import scala.concurrent.{Await, Future}
import play.api.libs.functional.syntax._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 24/05/15
 * Time: 13:58
 */
object RequestSerializer{
  val requestSaver = DiskSaver(new File(DatabaseUpdate.getLastFolder + "_jsons"), "request")
  implicit val requestFormat = DummyRequest.requestFormat
  private val queue = new mutable.Queue[RealRequest]()
  private val done = new mutable.Queue[DummyRequest]()
  private var ready = true

  // Load previous jobs.
  Await.ready(Future.sequence(
    requestSaver.indexMap.keys.map( key =>
      requestSaver.get(key).get.map(request => Json.parse(request).validate[DummyRequest] match {
          case r: JsSuccess[DummyRequest] => done enqueue r.get
        }
      )
    )
  ), Inf)

  def serialize(key: String, description:String, action: () => Future[SimpleResult]) : Future[SimpleResult] = {
    // If the job has already been completed, skip it (we keep the latest result with no duplicates on the list)
    if(done.dequeueFirst(_.key.equalsIgnoreCase(key)).isEmpty){
      // If the job wasn't already on the list, we proceed to enqueue the job, and send a signal to process jobs
      queue enqueue new RealRequest(new DummyRequest(key, description, DateTime.now()), action)
      nextJob()
    } //TODO: find a way to save the SimpleResult and return that instead if job is done.
    Future(Ok(Json.obj("queued" -> true)))  // Finally, return our default result
  }

  private def nextJob() : Unit = if(ready && queue.nonEmpty) work(queue.head)

  private def work(request : RealRequest) = {
    ready = false
    val action = request.action.apply()
    action.onSuccess{ case _ =>
      val successfulRequest = request.request.copy(ended = Some(DateTime.now()))
      done enqueue successfulRequest
      Future(requestSaver.put(successfulRequest.key, Json.toJson(successfulRequest).toString()))
    }
    action.onComplete{ case _ =>
      queue.dequeue()
      ready = true
      nextJob()
    }
  }

  def getRequests : Seq[DummyRequest] = (queue.map(_.request) ++ done).toSeq

  def isWorking = !ready
}


trait Request

case class RealRequest(request: DummyRequest, action: () => Future[SimpleResult]) extends Request

case class DummyRequest(key: String, description: String, started: DateTime, ended: Option[DateTime] = None) extends Request

object DummyRequest {
  implicit val requestFormat : Format[DummyRequest] = (
      (__ \ "key").format[String] and
      (__ \ "description").format[String] and
      (__ \ "started").format[DateTime] and
      (__ \ "ended").formatNullable[DateTime]
    )(DummyRequest.apply, unlift(DummyRequest.unapply))
}
