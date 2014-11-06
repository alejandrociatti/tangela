package util

import java.io.File.separator
import java.io.{File, PrintWriter}

import com.fasterxml.jackson.core.JsonParseException
import play.api.libs.json.Json

import scala.io.Source

/**
 * Created by Javier Isoldi.
 * Date: 05/11/14.
 * Project: tangela.
 */
case class DiskSaver(directory: File) {
  if(directory.exists && !directory.isDirectory) throw new NotDirectoryException(directory.getAbsolutePath)

  def put(key: String, value: String): Unit = {
    checkDirectory()

    val fileToSave = fileFromKey(key)
    if (fileToSave.exists()) {
      fileToSave.delete()
    }

    val printWriter = new PrintWriter(fileToSave)
    printWriter.write(value)
    printWriter.flush()
    printWriter.close()
  }

  private def fileFromKey(key: String): File =
    new File(directory.getPath + separator + keyToFileName(key) + ".json")


  def get(key: String): Option[String] = {
    checkDirectory()

    val fileToSave = fileFromKey(key)
    if (fileToSave.exists()) {
      val file = Source.fromFile(fileToSave)
      val result = file.mkString("")

      // To clear some errors
      try {
        Json.parse(result)
      } catch {
        case e: JsonParseException =>
          fileToSave.delete()
          return None
      }

      file.close()
      Some(result)
    } else {
      None
    }
  }

  private def keyToFileName(key: String) = {
    key.replaceAll("/", "-")
  }

  def checkDirectory() {
    if (!directory.exists()) {
      directory.mkdir()
    }
  }
}

case class NotDirectoryException(message: String) extends Exception(message)