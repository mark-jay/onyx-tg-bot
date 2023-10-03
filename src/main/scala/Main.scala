import onyx.{OnyxBot, OnyxBotConfig}
import utils.FileService

import java.io.File
import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App {
  println("starting ...")
  private val token: String = System.getenv("TOKEN")
  private val mainChatId: String = System.getenv("MAIN_CHAT_ID")
  private val chatIdToForwardTo: String = System.getenv("CHAT_ID_TO_FORWARD_TO")
  val bot = new OnyxBot(
    token,
    OnyxBotConfig(mainChatId, chatIdToForwardTo),
  )
  val eol = bot.run()
//  println("Press [ENTER] to shutdown the bot, it may take a few seconds...")
//  scala.io.StdIn.readLine()
//  bot.shutdown() // initiate shutdown
  // Wait for the bot end-of-life
  Await.result(eol, Duration.Inf)
}
