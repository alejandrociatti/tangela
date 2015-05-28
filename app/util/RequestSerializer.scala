package util

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.mvc.SimpleResult

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 24/05/15
 * Time: 13:58
 */
object RequestSerializer{
  implicit val requestWrites = Request.RequestWrites
  private val queue = new mutable.Queue[Request]()
  private val done = new mutable.Queue[Request]()
  private var ready = true

  def serialize(key: String, description:String, action: () => Future[SimpleResult]) = {
    queue enqueue new Request(key, description, action, DateTime.now())
    nextJob()
  }

  private def nextJob():Option[Request] = if(ready) queue.headOption map work else None

  private def work(request : Request) = {
    ready = false
    val action = request.action.apply()
    action.onSuccess{ case _ =>
      done enqueue request.copy(ended = Some(DateTime.now()))
    }
    action.onComplete{ case _ =>
      ready = true
      nextJob()
    }
    queue.dequeue()
  }

  def getRequests = (queue ++ done).toSeq

  def isWorking = !ready
}


case class Request(
                    key: String,
                    description: String,
                    action: () => Future[SimpleResult],
                    started: DateTime,
                    ended: Option[DateTime] = None
                  )

object Request {
  implicit val RequestWrites = new Writes[Request] {
    override def writes(o: Request): JsValue = Json.obj(
      "key" -> o.key,
      "started" -> o.started.toString(),
      "ended" -> o.ended.fold("")(_.toString())
    )
  }
}
