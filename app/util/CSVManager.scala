package util

import java.io._

import play.api.libs.iteratee.Enumerator

/**
 * User: Martin Gutierrez
 * Date: 20/11/14
 * Time: 17:15
 */
object CSVManager {

  val csvSaver = DiskSaver(new File("storedCSVs"), "csvs")

  def put(fileName: String, headers: Seq[String], values: Seq[Seq[String]]) {
    csvSaver.get(fileName).getOrElse {
      csvSaver.put(fileName, makeCSVString(headers, values))
    }
  }

  private def makeCSVString(headers: Seq[String], values: Seq[Seq[String]]): String =
    CSVCreator.createRow(headers) ++ CSVCreator.createAll(values)

  def get(fileName: String): Option[String] = csvSaver.get(fileName)

  def getFile(fileName: String): Option[(Enumerator[Array[Byte]], Int)] = csvSaver.getFile(fileName)
}