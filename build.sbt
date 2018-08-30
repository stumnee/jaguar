name := "jaguar"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.4"

val reactiveMongoVer = "0.13.0-play26"

libraryDependencies ++= Seq(
  guice,
  "org.reactivemongo"      %% "play2-reactivemongo" % reactiveMongoVer,
  "io.swagger"             %% "swagger-play2"       % "1.6.0",
  "org.webjars"            %  "swagger-ui"          % "3.2.2",
  "com.typesafe.play" % "play-json-joda_2.12" % "2.6.0",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "org.scalatestplus.play" %% "scalatestplus-play"  % "3.1.1" % Test
)

import play.sbt.routes.RoutesKeys

RoutesKeys.routesImport += "play.modules.reactivemongo.PathBindables._"
