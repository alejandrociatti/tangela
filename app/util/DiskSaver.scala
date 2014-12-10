package util

import java.io.{File, PrintWriter}
import java.io.File.separator

import com.fasterxml.jackson.core.JsonParseException
import play.api.libs.json.Json

import scala.io.Source

/**
 * Created by Javier Isoldi.
 * Date: 05/11/14.
 * Project: tangela.
 */
case class DiskSaver(directory: File, extension: String) {
  if(directory.exists && !directory.isDirectory) throw new NotDirectoryException(directory.getAbsolutePath)

  def put(key: String, value: String):Unit = {
    checkDirectory()
    val fileToSave = fileFromKey(key)
    if(fileToSave.exists()) fileToSave.delete()
    val printWriter = new PrintWriter(fileToSave)
    printWriter.write(value)
    printWriter.flush()
    printWriter.close()
  }

  def get(key: String): Option[String] = {
    checkDirectory()
    val fileToRead = fileFromKey(key)
    if (fileToRead.exists()) {
      val file = Source.fromFile(fileToRead, "ISO-8859-1")
      val result = file.mkString("")
      // Check json for errors
      if(extension.equals(".json")){
        try {
          Json.parse(result)
        } catch {
          case e: JsonParseException =>
            fileToRead.delete()
            return None
        }
      }
      file.close()

      Some(result)
    } else {
      None
    }
  }

  private def fileFromKey(key: String):File = new File(directory.getPath + separator + keyToFileName(key) + extension)

  private def keyToFileName(key: String) =  key.replaceAll("/", "-")

  def checkDirectory() = if(!directory.exists()) directory.mkdir()
}

case class NotDirectoryException(message: String) extends Exception(message)