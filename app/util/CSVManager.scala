package util

import java.io._

import models.DatabaseUpdate
import play.api.Play
import play.api.libs.iteratee.Enumerator

import scala.concurrent.duration.Duration.Inf
import scala.concurrent.Await
import play.api.Play.current

/**
 * User: Martin Gutierrez
 * Date: 20/11/14
 * Time: 17:15
 */
object CSVManager {

  val csvSaver = DiskSaver(new File("storedCSVs"), "csvs")

  def put(fileName: String, headers: Seq[String], values: Seq[Seq[String]]): Unit =  {
    val directory: File = Play.application.getFile(s"storedCSVs/")
    if (directory.exists()) directory.mkdir()
    val file: File = Play.application.getFile(s"storedCSVs/$fileName.csv")
    if (file.exists()) file.delete()
//    val fileOutputStream: FileOutputStream = new FileOutputStream(file)
    val fileWriter: FileWriter = new FileWriter(file.getAbsoluteFile)
    val bufferedWriter: BufferedWriter = new BufferedWriter(fileWriter)
    bufferedWriter.write(CSVCreator.createRow(headers))
    values foreach { row =>
      bufferedWriter.write(CSVCreator.createRow(row))
    }
    bufferedWriter.close()
//    fileOutputStream.write(makeCSVString(headers, values).getBytes("UTF-8"))

    csvSaver.get(fileName).getOrElse {
      csvSaver.put(fileName, makeCSVString(headers, values))
    }
  }

  def put(fileName: String, headers: String, values: Seq[String]): Unit = {
    val directory: File = Play.application.getFile(s"storedCSVs/")
    if (directory.exists()) directory.mkdir()
    val file: File = Play.application.getFile(s"storedCSVs/$fileName.csv")
    if (file.exists()) file.delete()
    //    val fileOutputStream: FileOutputStream = new FileOutputStream(file)
    val fileWriter: FileWriter = new FileWriter(file.getAbsoluteFile)
    val bufferedWriter: BufferedWriter = new BufferedWriter(fileWriter)
    bufferedWriter.write(headers)
    values foreach { row =>
      bufferedWriter.write(row)
    }
    bufferedWriter.close()
    //    fileOutputStream.write(makeCSVString(headers, values).getBytes("UTF-8"))

    //    csvSaver.get(fileName).getOrElse {
    //      csvSaver.put(fileName, makeCSVString(headers, values))
    //    }
  }

  private def makeCSVString(headers: Seq[String], values: Seq[Seq[String]]): String = {
    val stringBuilder = new StringBuilder(CSVCreator.createRow(headers))
    values.foreach(rowValues => stringBuilder.append(CSVCreator.createRow(rowValues)))
    stringBuilder.toString()
  }

  def get(fileName: String): Option[String] = csvSaver.get(fileName).map(Await.result(_, Inf))

  def getFile(fileName: String): Option[(Enumerator[Array[Byte]], Int)] = csvSaver.getFile(fileName)
}
