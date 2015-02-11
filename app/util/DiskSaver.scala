package util

import java.io._
import play.api.libs.iteratee.Enumerator
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by Javier Isoldi.
 * Date: 05/11/14.
 * Project: tangela.
 */
case class DiskSaver(directory: File, fileName: String) {
  checkDirectory()
  if(!directory.isDirectory) throw new NotDirectoryException(directory.getAbsolutePath)

  val indexFile = new File(directory.getAbsolutePath + File.separator + fileName +"-index.map")
  val indexMap: mutable.Map[String, Long] = if (indexFile.exists() && indexFile.isFile) {
    retrieveIndex()
  } else {
    mutable.HashMap.empty[String, Long]
  }

  // This file contains the data.
  val dataFile = new File(directory.getAbsolutePath + File.separator + fileName + "-data.file")

  def put(key: String, value: String):Unit = this.synchronized {
    checkDirectory()
    indexMap.getOrElse(key, { saveNewIndex((key, writeString(value)))})
  }

  def get(key: String): Option[String] = {
    checkDirectory()
    indexMap.get(key) map readString
  }

  def getFile(key: String): Option[(Enumerator[Array[Byte]], Int)] = {
    checkDirectory()
    indexMap.get(key) map{
      value =>
        val stringBytes = readString(value).getBytes
        (Enumerator.fromStream(new ByteArrayInputStream(stringBytes)), stringBytes.length)
      }
  }

  def writeString(value: String): Long = this.synchronized {
    val dataRandomAccessFile = new RandomAccessFile(dataFile, "rw")
    val fileLength = dataRandomAccessFile.length()
    val index = if(fileLength <= 0) 0 else fileLength
    dataRandomAccessFile.seek(index)
    dataRandomAccessFile.writeLong(value.length)
    dataRandomAccessFile.writeChars(value)
    dataRandomAccessFile.close()
    index
  }

  def readString(index: Long): String = {
    val dataRandomAccessFile = new RandomAccessFile(dataFile, "r")
    dataRandomAccessFile.seek(index)
    val length: Long = dataRandomAccessFile.readLong()
    val string = (0l until length).map { index => dataRandomAccessFile.readChar()}.mkString
    dataRandomAccessFile.close()
    string
  }

  def saveNewIndex(value: (String, Long)): Unit = {
    val indexSource = new RandomAccessFile(indexFile, "rw")
    if (indexSource.length() == 0)  indexSource.writeLong(0)
    indexSource.seek(indexSource.length())
    indexSource.writeUTF(value._1)
    indexSource.writeLong(value._2)
    indexMap += value
//    Store the size of the map at the beginning of the file
    indexSource.seek(0)
    indexSource.writeLong(indexMap.size)
    indexSource.close()
  }

  def retrieveIndex(): mutable.HashMap[String, Long] = {
    val indexSource = new RandomAccessFile(indexFile, "r")
    indexSource.seek(0)
//    Retrieves the size of the map
    val size = indexSource.readLong()
    val values = (0l until size) map {index => (indexSource.readUTF(), indexSource.readLong())}
    indexSource.close()
    mutable.HashMap[String, Long](values: _*)
  }

  def checkDirectory() = if(!directory.exists()) directory.mkdir()
}

case class NotDirectoryException(message: String) extends Exception(message)