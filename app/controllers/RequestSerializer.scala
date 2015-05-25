package controllers

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import org.joda.time.DateTime
import play.api.libs.concurrent.Akka
import play.api.mvc.SimpleResult

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 24/05/15
 * Time: 13:58
 */
object RequestSerializer {
  var requests = Queue[Request]()
  val schedule = Akka.system.scheduler.schedule(Duration.Zero, Duration(5, TimeUnit.SECONDS))(digest)

  def serialize(key: String, request: () => Future[SimpleResult]) = {
    val kind = key match {
      case k if k.startsWith("startup-net") => RequestKind.StartupNetwork
      case k if k.startsWith("people-net") => RequestKind.PeopleNetwork
    }
    requests enqueue Request(kind, RequestStatus.Queued, DateTime.now(), None, request)
  }

  private def digest = {
    requests.dequeueOption.map{ tuple =>
      val request = tuple._1
      val queue = tuple._2
      request.status match {
        case RequestStatus.Executing => None
        case RequestStatus.Queued => run(request)
        case _ => requests = queue; None
      }

    }
  }

  private def run(request:Request) = {
    request.action().onSuccess{
      case _ => request.status = RequestStatus.Completed
    }
  }

  def serialiseFutures[A, B](fn: A => Future[B])(implicit ec: ExecutionContext): Future[List[B]] =
    requests.foldLeft(Future(List.empty[B])) {
      (previousFuture, next) =>
        for {
          previousResults <- previousFuture
          next <- fn(next)
        } yield previousResults :+ next
    }
}

class Serializer extends Actor{
  val requests = new mutable.Queue[Request]()

  override def receive: Receive = {
    case Request(_, status, _, ended, action) =>
  }
}

case class Request(
                    kind: RequestKind,
                    status: RequestStatus,
                    started: DateTime,
                    ended: Option[DateTime],
                    action: () => Future[SimpleResult]
                     )

sealed trait RequestKind
object RequestKind {
  case object StartupNetwork extends RequestKind
  case object PeopleNetwork extends RequestKind
}

sealed trait RequestStatus
object RequestStatus {
  case object Queued extends RequestStatus
  case object Executing extends RequestStatus
  case object Completed extends RequestStatus
  case object Failed extends RequestStatus
}