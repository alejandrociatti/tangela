package controllers

import java.io.{InputStreamReader, BufferedReader, OutputStreamWriter, BufferedWriter}
import java.net.InetAddress

import _root_.util.MyAsyncCompletionHandler
import com.ning.http.client.{AsyncHttpClient, ProxyServer, AsyncHttpClientConfig}
import com.subgraph.orchid.TorClient
import com.subgraph.orchid.sockets.OrchidSocketFactory
import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.json._
import play.api.libs.ws.{Response, WS}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by Javier Isoldi.
 * Date: 17/09/14.
 * Project: tangela.
 */

object AngelListServices {
  val AngelApi = "https://api.angel.co/1"

  private def responseToJson(response: Response) = response.json

  def sendRequest(request: String): Future[JsValue] =
    Cache.get(AngelApi + request).fold {
    WS.url(AngelApi + request).get().map{ result =>
        val jsonResponse = responseToJson(result)
        //TODO: @Javier can u make this check pretty?
        if((jsonResponse\"error").as[JsString].toString().isEmpty) Cache.set(AngelApi + request, jsonResponse, 82800)
        jsonResponse
      }
    } { result =>
      Future(result.asInstanceOf[JsValue])
    }

  def getStartupById(id: Long) = sendRequest(s"/startups/$id")

  def getUserById(id: Long) = sendRequest(s"/users/$id")

  def getRolesFromStartupId(id: Long) = sendRequest(s"/startup_roles?startup_id=$id")

  def getStartupsByTagId(id: Long) = sendRequest(s"/tags/$id/startups")

  def getStartupsByTagIdAndPage(id: Long)(page: Int) = sendRequest(s"/tags/$id/startups?page=$page")

  def getStartupsWithFoundRaising = sendRequest("/startups?filter=raising")

  def getFoundersByStartupId(id: Long) = sendRequest(s"/startups/$id/roles?role=founder")

  def getFundingByStartupId(id: Long) = sendRequest(s"/startups/$id/funding")

  def searchStartupByName(name: String) = sendRequest(s"/search?type=Startup&query=$name")
  
  def searchLocationByName(name: String) = sendRequest(s"/search?type=LocationTag&query=$name")

  def searchLocationBySlug(name: String) = sendRequest(s"/search/slugs?type=LocationTag&query=$name")

  def getChildrenOfTag(id: Long) = sendRequest(s"/tags/$id/children")

  def getChildrenOfTagAndPage(id: Long)(page: Int) = sendRequest(s"/tags/$id/children?page=$page")

  def searchMarketByName(name: String) = sendRequest(s"/search?type=MarketTag&query=$name")

  def checkTor() = {
    //Test with WS
    WS.url("https://check.torproject.org/").get().map{ result =>
      if(result.body.contains("<h1 class=\"on\">")) println("WS is using the TOR network.")
      else println("WS is not under the TOR network.")
    }
    //Test with NING Asynchronous HTTP client
    val clientConfig = new AsyncHttpClientConfig.Builder().setProxyServer(new ProxyServer("127.0.0.1", 9050)).build()
    val client = new AsyncHttpClient(clientConfig)
    client.prepareGet("https://check.torproject.org/").execute(MyAsyncCompletionHandler.MY_HANDLER)
    //Test with Orchid (Java TOR implementation)
    val torClient = new TorClient()
    torClient.start()
    val socketFactory = new OrchidSocketFactory(torClient)
    val address = InetAddress.getByName("check.torproject.org")
    val socket = socketFactory.createSocket(address, 80)
    val wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream, "UTF8")) // Send headers
    wr.write("GET / HTTP/1.1")
    wr.write("Connection: keep-alive")
    wr.write("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
    wr.write("User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.111 Safari/537.36")
    wr.write("Accept-Encoding: gzip,deflate,sdch")
    wr.write("Accept-Language: es,en-US;q=0.8,en;q=0.6")
    val rd = new BufferedReader(new InputStreamReader(socket.getInputStream))
    val str = Stream.continually(rd.readLine()).takeWhile(_ != null).mkString("\n")
    wr.flush()
    if(str.contains("<h1 class=\"on\">")) println("Orchid is using the TOR network.")
    else println("Orchid is not under the TOR network.")
    wr.close()
    rd.close()
  }

}
