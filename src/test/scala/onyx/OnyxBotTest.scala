package onyx

import org.specs2.mutable.Specification
import utils.FileService

import java.io.File
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class OnyxBotTest extends Specification {

  "OnyxBotTest" should {
    "run" in {
      val token = FileService.readFile(new File("/home/mark/.external-secrets/telegram-keys/onyx/token.txt"))
      val bot = new OnyxBot(
        token,
        OnyxBotConfig("-1001891958439", "-4076585971"),
      )

      val eol = bot.run()
      println("Press [ENTER] to shutdown the bot, it may take a few seconds...")
      scala.io.StdIn.readLine()
      bot.shutdown() // initiate shutdown
      // Wait for the bot end-of-life
      Await.result(eol, Duration.Inf)

      ok
    }
  }
}
