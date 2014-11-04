package util

import java.io.{InputStreamReader, BufferedReader}
import java.net.{URL, InetSocketAddress, Proxy}

import scala.collection.mutable
import scala.collection.mutable.Queue
import scala.concurrent.{Promise, Future}
import akka.actor.{ActorRef, Actor, Props}
import akka.routing.{SmallestMailboxRouter, RoundRobinRouter}
import play.api.libs.concurrent.Akka
import play.api.Play.current

import play.api.libs.concurrent.Execution.Implicits._
import akka.util.Timeout
import akka.pattern.ask

import scala.io.Source

/**
  * Created by Javier Isoldi.
 * Date: 03/11/14.
 * Project: tangela.
 */

object RequestManager  {
  implicit val timeout = Timeout(300000)

  lazy val roundRobinRouter = {
    val requesters = (0 to 15).map { index =>
      Akka.system.actorOf(Props(new Requester(index.toString)))
    }
    Akka.system.actorOf(Props.empty.withRouter(SmallestMailboxRouter(requesters)))
  }
  var count = 0

  def sendRequest(url: String): Future[String] = (roundRobinRouter ? SocketRequest(url)) map {
    case response: String => response
    case response => throw UnexpectedResponse(response.toString)
  }
}

class Requester(id: String) extends Actor{
  val proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("localhost", 9050))

  override def receive: Receive = {
    case SocketRequest(url) =>
      RequestManager.count = RequestManager.count + 1
      println(s"Requester: $id Request = " + RequestManager.count)
      val connection = new URL(url).openConnection(proxy)
      connection.setRequestProperty("User-Agent", "Mozilla/5.0")
      sender ! Source.fromInputStream(connection.getInputStream).mkString("")
  }
}

//object RequestManager {
//  implicit val timeout = Timeout(3000000)
//
//  val router = new RequestRouter()
//
//  var count = 0
//
//  def sendRequest(url: String): Future[String] =
//    (router sendRequest SocketRequest(url)) map {
//      case response: String => response
//      case response => "error"
//    }
//
//}

//class RequestRouter(val maxPoolSize: Int = 15){
//  implicit val timeout = Timeout(3000000)
//  var poolSize = 0
//
//  val pendingRequests: mutable.Queue[(SocketRequest, Promise[Any])] = mutable.Queue.empty
//
//  def sendRequest(request: SocketRequest): Future[Any] = this.synchronized {
//      val response = Promise[Any]()
//      if (poolSize >= maxPoolSize) {
//        pendingRequests.+=: (request, response)
//      } else {
//        poolSize = poolSize + 1
//        Requester.sendRequest(request) foreach requesterResponded(response)
//      }
//      response.future
//  }
//
//  def requesterResponded(futureResponse: Promise[Any])(response: Any): Unit = this.synchronized {
//      futureResponse.success(response)
//      pendingRequests.headOption.fold {
//        poolSize = poolSize - 1
//      }{ request =>
//        pendingRequests.dequeue()
//        Requester.sendRequest(request._1) foreach requesterResponded(request._2)
//      }
//  }
//}
//
//object Requester {
//
//  def sendRequest(request: SocketRequest): Future[String] = Future {
//      RequestManager.count = RequestManager.count + 1
//      println(s"count in RequestRouter = " + RequestManager.count)
//      val proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("localhost", 9050))
//      val connection = new URL(request.url).openConnection(proxy)
//      connection.setRequestProperty("User-Agent", "Mozilla/5.0")
//      val rd = new BufferedReader(new InputStreamReader(connection.getInputStream))
//      Stream.continually(rd.readLine()).takeWhile(_ != null).mkString(" ")
//  }
//}

case class SocketRequest(url: String)

case class UnexpectedResponse(message: String) extends Exception(message)
