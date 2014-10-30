package util

import java.io.{InputStreamReader, BufferedReader}
import java.net.{URL, Proxy, InetSocketAddress}

import com.ning.http.client.{AsyncHttpClient, ProxyServer, AsyncHttpClientConfig}
import play.api.libs.ws.WS

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 30/10/14
 * Time: 14:32
 */
object TorTester {

  def checkTor() = {
    //Test with WS
    WS.url("https://check.torproject.org/").get().map{ result =>
      //println(result.body)
      if(result.body.contains("<h1 class=\"on\">") || result.body.contains("<h1 class=\"not\">"))
        println("WS is using the TOR network.")
      else println("WS is not under the TOR network.")
    }
    //Test with NING Asynchronous HTTP client
    val clientConfig = new AsyncHttpClientConfig.Builder()
      .setProxyServer(new ProxyServer("localhost", 9090))
      .setUseProxyProperties(true)
      .build()
    val client = new AsyncHttpClient(clientConfig)
    client.prepareGet("https://check.torproject.org/").execute(MyAsyncCompletionHandler.MY_HANDLER)
    //Test the java way?
    val address = new InetSocketAddress("localhost", 9050)
    val proxy = new Proxy(Proxy.Type.SOCKS, address)
    val connection = new URL("https://check.torproject.org/").openConnection(proxy)
    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
    val rd = new BufferedReader(new InputStreamReader(connection.getInputStream()))
    val str = Stream.continually(rd.readLine()).takeWhile(_ != null).mkString("\n")
    if(str.contains("<h1 class=\"on\">") || str.contains("<h1 class=\"not\">")) println("POJ is using the TOR network.")
    else println("POJ is not under the TOR network.")
    rd.close()
  }
}
