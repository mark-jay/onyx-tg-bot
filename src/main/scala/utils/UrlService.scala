package utils

import java.io.FileOutputStream

import java.net.URL
import java.io.FileOutputStream

object UrlService {
  def downloadFile(url: String, filename: String): Unit = {
    val urlObject = new URL(url)
    val outputStream = new FileOutputStream(filename)

    try {
      val inputStream = urlObject.openStream()
      val buffer = new Array[Byte](1024)
      var bytesRead = inputStream.read(buffer)

      while (bytesRead != -1) {
        outputStream.write(buffer, 0, bytesRead)
        bytesRead = inputStream.read(buffer)
      }
    } finally {
      outputStream.close()
    }
  }
}
