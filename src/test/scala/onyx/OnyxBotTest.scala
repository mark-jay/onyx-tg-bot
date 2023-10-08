package onyx

import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.api.{AkkaTelegramBot, RequestHandler}
import com.bot4s.telegram.clients.AkkaHttpClient
import com.bot4s.telegram.future.Polling
import com.bot4s.telegram.methods.ChatAction.ChatAction
import com.bot4s.telegram.methods.{ChatAction, SendAnimation, SendChatAction, SendPhoto, SendVideo}
import com.bot4s.telegram.models.{ChatId, InputFile, Message}
import org.specs2.mutable.Specification
import utils.FileService

import java.io.File
import java.nio.file.Paths
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

import scala.concurrent.duration._


class OnyxBotTest extends Specification {

  "OnyxBotTest" should {
    "run" in {
      val token = FileService.readFile(new File("/home/mark/.external-secrets/telegram-keys/onyx/token.txt"))
      val mainChatId = "-1001891958439"
      val targetChatId = "-1001845453886"

      val bot = new OnyxBot(
        token,
        OnyxBotConfig(mainChatId, targetChatId),
      )
      val eol = bot.run()
      Thread.sleep(1000000)

      ok
    }
  }
}
