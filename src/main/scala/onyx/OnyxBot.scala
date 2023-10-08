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
import com.bot4s.telegram.methods.ChatAction.ChatAction
import com.bot4s.telegram.methods.{ChatAction, ForwardMessage, GetFile, ParseMode, SendAnimation, SendChatAction, SendPhoto, SendVideo}
import com.bot4s.telegram.models.{InputFile, Message, PhotoSize}
import utils.{FileService, UrlService}

import java.io.File
import java.nio.file.{Paths, StandardOpenOption}
import java.util.Date
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
          publishIfPhoto(msg, maybeCaption, msg.chat.id)
          publishIfVideo(msg, maybeCaption, msg.chat.id)
          publishIfAnimation(msg, maybeCaption, msg.chat.id)
        }

        // publish if replied to your own media
        if (msg.text.exists(_.contains("#public"))) {
          msg.replyToMessage.foreach(originalMessage => {
            if (msg.from.isDefined && originalMessage.from.isDefined && msg.from.get == originalMessage.from.get) {
//              println("can be forwarded")
              val maybeCaption: Option[String] = makeCaption(originalMessage)
              publishIfPhoto(originalMessage, maybeCaption, msg.chat.id)
              publishIfVideo(originalMessage, maybeCaption, msg.chat.id)
              publishIfAnimation(originalMessage, maybeCaption, msg.chat.id)
            } else {
              replyMd("Не могу, запрос публикации должен быть от автора сообщения")
            }
          })
        }
      }
      Future.unit
    }
  }

  private def publishIfAnimation(originalMessage: Message, maybeCaption: Option[String], chatId: Long) = {
    originalMessage.animation.foreach(item => {
      sendAction(ChatAction.UploadVideo, chatId)
      sendAnimation(maybeCaption, item.fileId)
    })
  }

  private def sendAction(action: ChatAction, chatId: Long) = {
    request(
      SendChatAction(
        chatId,
        action,
      )
    )
  }

  private def publishIfVideo(msg: Message, maybeCaption: Option[String], chatId: Long) = {
    msg.video.foreach(item => {
      sendAction(ChatAction.UploadVideoNote, chatId)
      sendVideo(maybeCaption, item.fileId)
    })
  }

  private def publishIfPhoto(msg: Message, maybeCaption: Option[String], chatId: Long) = {
    msg.photo.map(_.last.fileId).foreach(fileId => {
      sendAction(ChatAction.UploadPhoto, chatId)
      sendPhoto(maybeCaption, fileId)
    })
  }

  private def makeCaption(msg: Message) = {
    try {
      //    val chatId = msg.chat.id
      //    val messageId = msg.messageId
      val link = s"https://t.me/c/${msg.chat.id.toString.replaceAll("-100", "")}/${msg.messageId}" // Создаем ссылку на сообщение

      // Handle incoming messages (optional, as shown in the previous examples)
      val fromPart = msg.from.flatMap(user => user.username)
        .map(username => s"[Сообщение](${link}) от [@${username}]")
        .getOrElse(s"[Сообщение](${link})")

      val finalCaption = msg.caption.map(_.replaceAll("#public", "")) match {
        case Some(value) if value.replaceAll(" ", "").nonEmpty => {
          s" :\n${value.trim.replaceAll("  ", " ")}"
        }
        case _ => ""
      }
      val result = s"${fromPart}${finalCaption}"

      println(s"making caption, result = ${result}")
      Some(result)
    } catch {
      case e: Exception => {
        System.err.println(s"${new Date().toString} Warn: Failed to make a caption")
        e.printStackTrace()
        None
      }
    }
  }

  private def sendAnimation(maybeCaption: Option[String], fileId: String) = {
    request(SendAnimation(
      chatId = config.chatIdToForward.toLong,
      animation = InputFile(fileId),
      caption = maybeCaption,
      parseMode = Some(ParseMode.Markdown),
      disableNotification = Some(true),
    ))
  }

  private def sendVideo(maybeCaption: Option[String], fileId: String) = {
    request(SendVideo(
      chatId = config.chatIdToForward.toLong,
      video = InputFile(fileId),
      caption = maybeCaption,
      parseMode = Some(ParseMode.Markdown),
      disableNotification = Some(true),
    ))
  }

  private def sendPhoto(maybeCaption: Option[String], fileId: String) = {
    request(SendPhoto(
      chatId = config.chatIdToForward.toLong,
      photo = InputFile(fileId),
      caption = maybeCaption,
      parseMode = Some(ParseMode.Markdown),
      disableNotification = Some(true),
    ))
  }

  override val client: RequestHandler[Future] = new AkkaHttpClient(token)
}
