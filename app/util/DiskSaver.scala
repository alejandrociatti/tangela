package util

import java.io._
import scala.collection.mutable

/**
 * Created by Javier Isoldi.
 * Date: 05/11/14.
 * Project: tangela.
 */
case class DiskSaver(directory: File) {
  checkDirectory()
  if(!directory.isDirectory) throw new NotDirectoryException(directory.getAbsolutePath)

  val indexFile = new File(directory.getAbsolutePath + File.separator + "index.map")
  val indexMap: mutable.Map[String, Long] = if (indexFile.exists() && indexFile.isFile) {
    val indexFileSource = new ObjectInputStream(new FileInputStream(indexFile))
    indexFileSource.readObject().asInstanceOf[mutable.HashMap[String, Long]]
  } else {
    mutable.HashMap.empty[String, Long]
  }

  // This file contains the data.
  val dataFile = new File(directory.getAbsolutePath + File.separator + "data.file")

  def put(key: String, value: String):Unit = this.synchronized {
    checkDirectory()
    indexMap.getOrElse(key, {
      try {
        indexMap.put(key, writeString(value))
        saveIndex()
      } catch {
        case e:Exception => e.printStackTrace()
      }
    })
  }

  def get(key: String): Option[String] = this.synchronized {
    checkDirectory()
    indexMap.get(key) map readString
  }

  def writeString(value: String): Long = this.synchronized {
    val dataRandomAccessFile = new RandomAccessFile(dataFile, "rw")
    val index = dataRandomAccessFile.length()
    dataRandomAccessFile.seek(index)
    dataRandomAccessFile.writeLong(value.length)
    dataRandomAccessFile.writeChars(value)
    dataRandomAccessFile.close()
    index
  }

  def readString(index: Long): String = this.synchronized {
    val dataRandomAccessFile = new RandomAccessFile(dataFile, "r")
    dataRandomAccessFile.seek(index)
    val length: Long = dataRandomAccessFile.readLong()
    (0l to length).map { index => dataRandomAccessFile.readChar()}.mkString
  }

  def saveIndex(): Unit = {
    indexFile.delete()
    val indexFileSource = new ObjectOutputStream(new FileOutputStream(indexFile))
    indexFileSource.writeObject(indexMap)
    indexFileSource.flush()
    indexFileSource.close()
  }

  def checkDirectory() = if(!directory.exists()) directory.mkdir()
}

case class NotDirectoryException(message: String) extends Exception(message)