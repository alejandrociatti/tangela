package util

import java.io._

import com.github.tototoshi.csv.CSVWriter
import play.api.libs.iteratee.Enumerator

import scala.io.Source

/**
 * User: Martin Gutierrez
 * Date: 20/11/14
 * Time: 17:15
 */
object CSVManager {

  val csvSaver = DiskSaver(new File("storedCSVs"), "csvs")

  def put(fileName: String, headers: List[String], values: List[List[String]]) {
    val maybeString: Option[String] = get(fileName)
    maybeString.getOrElse {
      csvSaver.put(fileName, makeCSVString(headers, values))
    }
  }

  private def makeCSVString(headers: List[String], values: List[List[String]]): String = {
    val byteArrayOutputStream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val writer = CSVWriter.open(new OutputStreamWriter(byteArrayOutputStream))
    writer.writeRow(headers)
    writer.writeAll(values)
    writer.close()
    val streamReader: InputStream = new BufferedInputStream(new ByteArrayInputStream(
      byteArrayOutputStream.toByteArray
    ))
    Source.fromInputStream(streamReader).mkString("")
  }

  def get(fileName: String): Option[String] = csvSaver.get(fileName)

  def getFile(fileName: String): Option[(Enumerator[Array[Byte]], Int)] = csvSaver.getFile(fileName)
}