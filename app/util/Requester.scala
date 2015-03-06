package util

import java.net.{InetSocketAddress, Proxy, URL}

import akka.pattern.ask
import akka.util.Timeout
import akka.actor.{Actor, Props}
import akka.routing.SmallestMailboxRouter
import play.api.Logger

import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future
import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
  * Created by Javier Isoldi.
 * Date: 03/11/14.
 * Project: tangela.
 */

object RequestManager  {
  implicit val timeout = Timeout(7400000)

  lazy val roundRobinRouter = {
    val requestors = (0 to 30).map { index =>
      Akka.system.actorOf(Props(new Requester(index.toString)))
    }
    Akka.system.actorOf(Props.empty.withRouter(SmallestMailboxRouter(requestors)))
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
      if(RequestManager.count % 500 == 0) Logger.info(s"Requester: $id Request = " + RequestManager.count)
      // Try to create an URLConnection with given URL and our TOR proxy
      Try(new URL(url).openConnection(proxy)) match {
        case Success(connection) =>                                       // if URLConnection is created:
          connection.setRequestProperty("User-Agent", "Mozilla/5.0")      // set user agent to connection
          Try(connection.getInputStream) match {                          // Try to open get InputStream from the connection
            case Success(stream) =>                                       // if InputStream is created:
              Try(Source.fromInputStream(stream)) match {                 // Try to create iterable Source from it
                case Success(source) =>                                   // if Source is created
                  Try(source.mkString) match {                            // Try to make a string from it
                    case Success(string) =>                               // if string is created from Source
                      source.close()                                      // close the Source (closes the Stream too)
                      sender ! string                                     // send the string
                    case Failure(e) =>                                    // if string failed to be created
                      source.close()                                          // close the Source (closes Stream)
                      Logger.warn(s"Source.mkString error: ${e.getMessage}")  // log the error,
                      sender ! "{\"success\":false}"                          // send an error string
                  }
                case Failure(e) =>                                        // if Source fails to be created
                  stream.close()                                                // close the Stream
                  Logger.warn(s"Source.fromInputStream error: ${e.getMessage}") // log the error,
                  sender ! "{\"success\":false}"                                // send an error string
              }
            case Failure(e) =>                                            // if InputStream fails to be created
              val message = e.getMessage
              if(message.contains("403")) self forward SocketRequest(url)          // if 403, forward the error
              Logger.warn(s"connection.getInputStream error: $message")            // log the error,
              sender ! "{\"success\":false}"                                       // send an error string
          }
        case Failure(e) =>                                                // if Connection fails to open
          Logger.warn(s"URL.openConnection error: ${e.getMessage}")       // log the error,
          sender ! "{\"success\":false}"                                  // send an error string
      }
  }
}

case class SocketRequest(url: String)

case class UnexpectedResponse(message: String) extends Exception(message)
