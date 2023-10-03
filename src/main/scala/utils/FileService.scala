package utils

import java.io.File
import scala.io.Source
import java.io._

object FileService {

  /** TODO: test me when needed */
  def readFile(filename: File): String = {
    val source = Source.fromFile(filename)
    val content = try source.mkString finally source.close()
    content
  }

  /** TODO: test me when needed */
  def writeToFile(filename: File, content: String): Unit = {
    val pw = new PrintWriter(filename)
    pw.write(content)
    pw.close()
  }

}
