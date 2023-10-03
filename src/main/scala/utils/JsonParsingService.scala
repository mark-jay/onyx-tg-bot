package utils

import com.typesafe.scalalogging.Logger
import org.json4s
import org.json4s.{DefaultFormats, _}
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization.{write, writePretty}
import org.slf4j.LoggerFactory

import scala.reflect.ClassTag
import scala.util.Try


object JsonParsingService {

  private val logger = Logger(LoggerFactory.getLogger(getClass.getName))

  def parse2Map(prefix: String, value: String): Map[String, String] = {
    try {
      implicit val formats = org.json4s.DefaultFormats

      val result = try {
        val res: Map[String, Any] = parse(value).extract[Map[String, Any]]
        res
      } catch {
        case e: Exception => {
          // can't be parsed, that fine
          Map()
        }
      }

      def flattenMap(prefix: String, inputMap: Any): Map[String, String] = {
        val stringToString = inputMap match {
          case map: Map[String, Any] => {
            val result: Map[String, String] = map
              .flatMap(pair => {
                val localPrefix = s"${prefix}_${pair._1}"
                flattenMap(localPrefix, pair._2)
              })
            result
          }
          case array: List[Any] => {
            val result: Map[String, String] = array
              .zipWithIndex
              .flatMap(pair => {
                val (value, number) = pair
                val localPrefix = s"${prefix}_${number}"
                flattenMap(localPrefix, value)
              })
              .toMap
            result
          }
          case any => Map(s"${prefix}" -> s"${any}")
        }
        stringToString
      }
      flattenMap(prefix, result)
    } catch {
      case e: Exception => {
        logger.info(s"failedToParseJson: failed to parse json for prefix ${prefix} and value = ${value}")
        Map()
      }
    }
  }

  def parseDouble(value: JValue): Double = {
    Try {
      val JInt(asInt) = value
      asInt.toDouble
    } getOrElse {
      Try {
        val JDouble(asDouble) = value
        asDouble
      } getOrElse {
        val JString(asString) = value
        BigDecimal(asString).doubleValue()
      }
    }
  }


  implicit val formats = DefaultFormats

  def extract[T: Manifest](json: String)(implicit ct: ClassTag[T]) = {
    try {
      parse(json).extract[T]
    } catch {
      case e: Exception => throw new RuntimeException(s"Could not parse json, expected ${ct.runtimeClass.getCanonicalName()}, found '${json}'", e)
    }
  }

  def extract[T: Manifest](value: JValue)(implicit ct: ClassTag[T]) = {
    try {
      value.extract[T]
    } catch {
      case e: Exception => throw new RuntimeException(s"Could not parse json, expected ${ct.runtimeClass.getCanonicalName()}, found '${value}'", e)
    }
  }

  def format(objectToFormat: AnyRef): String = {
    write(objectToFormat)
  }

  def formatPretty(objectToFormat: AnyRef): String = {
    reformatString(format(objectToFormat))
  }

  def parseJson(string: String) = {
    parse(string)
  }

  def reformatString(objectToFormat: String): String = {
    try {
      writePretty(parse(objectToFormat))
    } catch {
      case e: Exception => {
        objectToFormat
      }
    }
  }
}
