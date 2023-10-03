package onyx

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.IOResult
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import cats.instances.future._
import cats.syntax.functor._
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.api.{AkkaTelegramBot, RequestHandler}
import com.bot4s.telegram.clients.AkkaHttpClient
import com.bot4s.telegram.future.Polling
import com.bot4s.telegram.methods.{ForwardMessage, GetFile, SendAnimation, SendPhoto, SendVideo}
import com.bot4s.telegram.models.{InputFile, Message, PhotoSize}
import utils.{FileService, UrlService}

import java.io.File
import java.nio.file.{Paths, StandardOpenOption}
import scala.concurrent.Future

case class OnyxBotConfig(chatId: String, chatIdToForward: String)

class OnyxBot(
               val token: String,
               val config: OnyxBotConfig,
            ) extends AkkaTelegramBot
  with Polling
  with Commands[Future] {

  val helpMessage =
    """
      |Доступные команды:
      |/help - это сообщение
      |/getChatId - выводит chatId
      |Этот бот реагирует на медиа сообщения(картинки и видео) в основном чате с текстом содержащим #public и дублирует сообщение в публичный чат
      |Аналогичным образом работает если ответить на свое же сообщение(картинку, видео или GIF) текстом содержащим #public
      |""".stripMargin

  onCommand("getChatId") { implicit msg =>
    replyMd(s"chatId = ${msg.chat.id}").void
  }

  onCommand("help") { implicit msg =>
    replyMd(helpMessage).void
  }

  onMessage {
    implicit msg => {
      println(s"received message in ${msg.chat.id}")
      if (msg.chat.id == config.chatId.toLong) { // only in configured chat

        // msg.text is used for messages, while msg.caption is used for images
        if (msg.caption.exists(_.contains("#public"))) {
          // can not be forwarded either, only a new message

          val maybeCaption: Option[String] = makeCaption(msg)
          publishIfPhoto(msg, maybeCaption)
          publishIfVideo(msg, maybeCaption)
        }

        // publish if replied to your own media
        if (msg.text.exists(_.contains("#public"))) {
          msg.replyToMessage.foreach(originalMessage => {
            if (msg.from.isDefined && originalMessage.from.isDefined && msg.from.get == originalMessage.from.get) {
//              println("can be forwarded")
              val maybeCaption: Option[String] = makeCaption(originalMessage)
              publishIfPhoto(originalMessage, maybeCaption)
              publishIfVideo(originalMessage, maybeCaption)
              publishIfAnimation(originalMessage, maybeCaption)
            } else {
              replyMd("Не могу, запросить должен автор")
            }
          })
        }
      }
      Future.unit
    }
  }

  private def publishIfAnimation(originalMessage: Message, maybeCaption: Option[String]) = {
    originalMessage.animation.foreach(item => {
      sendAnimation(maybeCaption, item.fileId)
    })
  }

  private def publishIfVideo(msg: Message, maybeCaption: Option[String]) = {
    msg.video.foreach(item => {
      sendVideo(maybeCaption, item.fileId)
    })
  }

  private def publishIfPhoto(msg: Message, maybeCaption: Option[String]) = {
    msg.photo.map(_.last.fileId).foreach(fileId => {
      sendPhoto(maybeCaption, fileId)
    })
  }

  private def makeCaption(msg: Message) = {
    // Handle incoming messages (optional, as shown in the previous examples)
    val fromPart = msg.from.flatMap(user => user.username).map(username => s"[from @${username}] ").getOrElse("")

    // Send the message with the image
    val maybeCaption = msg.caption.map(caption => fromPart + caption.replaceAll("#public", ""))
    maybeCaption
  }

  private def sendAnimation(maybeCaption: Option[String], fileId: String) = {
    request(SendAnimation(
      chatId = config.chatIdToForward.toLong,
      animation = InputFile(fileId),
      caption = maybeCaption
    ))
  }

  private def sendVideo(maybeCaption: Option[String], fileId: String) = {
    request(SendVideo(
      chatId = config.chatIdToForward.toLong,
      video = InputFile(fileId),
      caption = maybeCaption
    ))
  }

  private def sendPhoto(maybeCaption: Option[String], fileId: String) = {
    request(SendPhoto(
      chatId = config.chatIdToForward.toLong,
      photo = InputFile(fileId),
      caption = maybeCaption
    ))
  }

  override val client: RequestHandler[Future] = new AkkaHttpClient(token)
}
