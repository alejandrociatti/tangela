package util

import java.io.{FileNotFoundException, IOException}
import java.net.{InetSocketAddress, Proxy, URL}

import akka.pattern.ask
import akka.util.Timeout
import akka.actor.{Actor, Props}
import akka.routing.SmallestMailboxRouter

import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future
import scala.io.Source

/**
  * Created by Javier Isoldi.
 * Date: 03/11/14.
 * Project: tangela.
 */

object RequestManager  {
  implicit val timeout = Timeout(7400000)

  lazy val roundRobinRouter = {
    val requesters = (0 to 30).map { index =>
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
      if(RequestManager.count % 100 == 0) println(s"Requester: $id Request = " + RequestManager.count)
      val connection = new URL(url).openConnection(proxy)
      connection.setRequestProperty("User-Agent", "Mozilla/5.0")
      try {
        sender ! Source.fromInputStream(connection.getInputStream, "UTF-8").mkString("")
      } catch {
        case exception: FileNotFoundException =>
          sender ! "{\"success\": false}"
        case exception: IOException =>
          println("ioException = " + exception)
          self forward SocketRequest(url)
      }
  }
}

case class SocketRequest(url: String)

case class UnexpectedResponse(message: String) extends Exception(message)
