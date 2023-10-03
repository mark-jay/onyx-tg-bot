package utils

import java.util.Base64

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

object CryptoService {

  def base64Encode(stringToEncode: String): String = {
    base64Encode(stringToEncode.getBytes("UTF-8"))
  }

  def base64Encode(bytesToEncode: Array[Byte]): String = {
    Base64.getEncoder.encodeToString(bytesToEncode)
  }

  def base64Decode(stringToDecode: String): Array[Byte] = {
    Base64.getDecoder.decode(stringToDecode)
  }

  def hmacSha1(content: String, secretKey: String): Array[Byte] = {
    hmacSha1(content.getBytes("UTF-8"), secretKey.getBytes("UTF-8"))
  }

  def hmacSha1(content: Array[Byte], secretKey: Array[Byte]): Array[Byte] = {
    val secret = new SecretKeySpec(secretKey, "HmacSHA1")
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(secret)
    mac.doFinal(content)
  }

  def hmacSha256(content: String, secretKey: String): Array[Byte] = {
    hmacSha256(content.getBytes("UTF-8"), secretKey.getBytes("UTF-8"))
  }

  def hmacSha384(content: String, secretKey: String): Array[Byte] = {
    hmacSha384(content.getBytes("UTF-8"), secretKey.getBytes("UTF-8"))
  }

  def hmacSha512(content: String, secretKey: String): Array[Byte] = {
    hmacSha512(content.getBytes("UTF-8"), secretKey.getBytes("UTF-8"))
  }

  def hmacSha256(content: Array[Byte], secretKey: Array[Byte]): Array[Byte] = {
    val secret = new SecretKeySpec(secretKey, "HmacSHA256")
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(secret)
    mac.doFinal(content)
  }

  def hmacSha384(content: Array[Byte], secretKey: Array[Byte]): Array[Byte] = {
    val secret = new SecretKeySpec(secretKey, "HmacSHA384")
    val mac = Mac.getInstance("HmacSHA384")
    mac.init(secret)
    mac.doFinal(content)
  }

  def hmacSha512(content: Array[Byte], secretKey: Array[Byte]): Array[Byte] = {
    val secret = new SecretKeySpec(secretKey, "HmacSHA512")
    val mac = Mac.getInstance("HmacSHA512")
    mac.init(secret)
    mac.doFinal(content)
  }

  def sha512(content: Array[Byte]): Array[Byte] = {
    val md = MessageDigest.getInstance("SHA-512")
    md.digest(content)
  }

  def toHex(buf: Array[Byte]): String = buf.map("%02X" format _).mkString

}
