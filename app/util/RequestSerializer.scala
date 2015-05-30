package util

import java.io.File

import models.DatabaseUpdate
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.mvc.SimpleResult

import scala.collection.mutable
import scala.concurrent.Future
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
  requestSaver.indexMap.keys.foreach( key =>
    Json.parse(requestSaver.get(key).get).validate[DummyRequest] match {
      case r: JsSuccess[DummyRequest] => done enqueue r.get
    }
  )

  def serialize(key: String, description:String, action: () => Future[SimpleResult]) = {
    queue enqueue new RealRequest(new DummyRequest(key, description, DateTime.now()), action)
    nextJob()
  }

  private def nextJob() : Unit = if(ready && queue.nonEmpty) work(queue.headOption.get)

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
