import com.typesafe.sbt.packager.docker._


ThisBuild / version := "0.1.0"

ThisBuild / scalaVersion := "2.12.17"

lazy val root = (project in file("."))
  .settings(
    name := "onyx-telegram-bot"
  )

// Core with minimal dependencies, enough to spawn your first bot.
libraryDependencies += "com.bot4s" %% "telegram-core" % "5.1.0"

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.6.7"

// Extra goodies: Webhooks, support for games, bindings for actors.
libraryDependencies += "com.bot4s" %% "telegram-akka" % "5.1.0"

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.2.6"

libraryDependencies += "org.specs2" %% "specs2-core" % "4.12.0" % Test

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.1.0"

libraryDependencies += "org.jsoup" % "jsoup" % "1.14.2"


// https://www.scala-sbt.org/sbt-native-packager/formats/docker.html
enablePlugins(JavaAppPackaging)
//enablePlugins(DockerPlugin)
dockerBaseImage := "openjdk:8-jre" // TODO: make -alpine
dockerExposedPorts := Seq()
//dockerExposedPorts := Seq(8080)
//dockerRepository := Some("127.0.0.1") // "my-docker-repo"
dockerUsername := Some("tg-bot-user")

dockerEnvVars := Map(
  "AUTH_TOKEN" -> "",
  "ENV_VAR_2" -> "value2"
)

//dockerCommands += Cmd("RUN", "apk add --no-cache bash")
//dockerCommands += Cmd("RUN", "apk add --no-cache wget")

//libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.4.2"
//libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.6.16"
//libraryDependencies += "com.bot4s" %% "telegram-akka" % "5.1.0"
