package util

import java.io._

import com.github.tototoshi.csv.CSVWriter

import scala.io.Source

/**
 * User: Martin Gutierrez
 * Date: 20/11/14
 * Time: 17:15
 */
object CSVManager {
  val jsonSaver = DiskSaver(new File("storedCSVs"), ".csv")

  def put(fileName: String, headers: List[String], values: List[List[String]]): Unit = {
    val maybeString: Option[String] = get(fileName)
    println("maybeString = " + maybeString)
    maybeString.getOrElse {
      println("puto !!!")
      jsonSaver.put(fileName, getCSVStringFromHeadersValues(headers, values))
    }
  }

  private def getCSVStringFromHeadersValues(headers: List[String], values: List[List[String]]): String = {
    println("aca empezo a writear")
    val byteArrayOutputStream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val writer = CSVWriter.open(new OutputStreamWriter(byteArrayOutputStream))
    writer.writeRow(headers)
    writer.writeAll(values)
    writer.close()
    val streamReader: InputStream = new BufferedInputStream(new ByteArrayInputStream(
      byteArrayOutputStream.toByteArray
    ))
    println("aca termino de writear")
    Source.fromInputStream(streamReader).mkString("")
  }

  def get(fileName: String): Option[String] = {
    jsonSaver.get(fileName)
  }
}