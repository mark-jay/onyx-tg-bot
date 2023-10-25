package onyx

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.pattern.StatusReply.Success
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
import com.bot4s.telegram.methods.{ChatAction, ForwardMessage, GetFile, ParseMode, SendAnimation, SendChatAction, SendMessage, SendPhoto, SendVideo}
import com.bot4s.telegram.models.{InputFile, Message, PhotoSize}
import utils.{FileService, UrlService}

import java.io.File
import java.nio.file.{Paths, StandardOpenOption}
import java.util.Date
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.{Await, Future}
import scala.util.Try

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

  private val sent = new AtomicReference[Set[Int]](Set())

  onCommand("getChatId") { implicit msg =>
    replyMd(s"chatId = ${msg.chat.id}").void
  }

  onCommand("help") { implicit msg =>
    replyMd(helpMessage).void
  }

  onEditedMessage {
    implicit msg => {
      sent.synchronized {
        processMessage(msg)
      }
    }
  }

  onMessage {
    implicit msg => {
      sent.synchronized {
        processMessage(msg)
      }
    }
  }

  private def processMessage(msg: Message) = {
    implicit val msg1 = msg
    val messageChatId = msg.chat.id
    println(s"received message in $messageChatId")
    if (messageChatId == config.chatId.toLong) { // only in configured chat

      // msg.text is used for messages, while msg.caption is used for images
      if (msg.caption.exists(_.contains("#public"))) {
        // can not be forwarded either, only a new message
        processIncomingMessageIfNeeded(msg, messageChatId)
          .onComplete(maybeMessages => processMessagesSent(maybeMessages, msg.messageId))
      }

      // publish if replied to your own media
      if (msg.text.exists(_.contains("#public"))) {
        msg.replyToMessage.foreach(originalMessage => {
          if (msg.from.isDefined && originalMessage.from.isDefined && msg.from.get == originalMessage.from.get) {
            processIncomingMessageIfNeeded(originalMessage, messageChatId)
              .onComplete(maybeMessages => processMessagesSent(maybeMessages, msg.messageId))
          } else {
            replyMd("Не могу, запрос публикации должен быть от автора сообщения")
          }
        })
      }
    }
    Future.unit
  }

  private def processMessagesSent(maybeMessages: Try[List[Message]], replyToMessageId: Int) = {
      maybeMessages.foreach(messages => {
        messages.foreach(message => {
          val text = s"[Опубликовано](${makeLink(s"${message.chat.id}", message.messageId)})"
          // println(s"text = ${text}")
          request(SendMessage(
            chatId = config.chatId.toLong,
            text = text,
            replyToMessageId = Some(replyToMessageId),
            parseMode = Some(ParseMode.Markdown),
            disableNotification = Some(true),
          ))
        })
      })
  }

  private def processIncomingMessageIfNeeded(msg: Message, messageChatId: Long): Future[List[Message]] = {
//    println(s"checking if ${sent.get()} contains ${msg.messageId}")
    if (!sent.get().contains(msg.messageId)) {
      val maybeCaption: Option[String] = makeCaption(msg)
      val f1 = publishIfPhoto(msg, maybeCaption, messageChatId)
      val f2 = publishIfVideo(msg, maybeCaption, messageChatId)
      val f3 = publishIfAnimation(msg, maybeCaption, messageChatId)
      val finalResult = for {
        v1 <- f1
        v2 <- f2
        v3 <- f3
      } yield List(v1, v2, v3).flatMap(_.toIterable)
      sent.set(sent.get() + msg.messageId)
      finalResult.failed.foreach(e => {
        println("failed")
        e.printStackTrace()
        sent.set(sent.get() - msg.messageId)
      })
      finalResult
    } else {
      Future.successful(List())
    }
  }

  private def publishIfAnimation(originalMessage: Message, maybeCaption: Option[String], chatId: Long) = {
    originalMessage.animation.map(item => {
      sendAction(ChatAction.UploadVideo, chatId)
      sendAnimation(maybeCaption, item.fileId)
    })
      .map(item => item.map(Some(_)))
      .getOrElse(Future.successful(None))
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
    msg.video.map(item => {
      sendAction(ChatAction.UploadVideoNote, chatId)
      sendVideo(maybeCaption, item.fileId)
    })
      .map(item => item.map(Some(_)))
      .getOrElse(Future.successful(None))
  }

  private def publishIfPhoto(msg: Message, maybeCaption: Option[String], chatId: Long) = {
    msg.photo.map(_.last.fileId).map(fileId => {
      sendAction(ChatAction.UploadPhoto, chatId)
      sendPhoto(maybeCaption, fileId)
    })
      .map(item => item.map(Some(_)))
      .getOrElse(Future.successful(None))
  }

  private def makeLink(chatId: String, messageId: Int): String = {
    //    val chatId = msg.chat.id
    //    val messageId = msg.messageId
    // works, but hacky and probably not documented:)
    val link = s"https://t.me/c/${chatId.replaceAll("-100", "")}/${messageId}" // Создаем ссылку на сообщение
    link
  }

  private def makeCaption(msg: Message) = {
    try {
      val link = makeLink(msg.chat.id.toString, msg.messageId)

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
