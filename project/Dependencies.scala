import sbt._
object Dependencies {

  object Versions {
    val akka = "2.4.1"
    val akkaStream = "2.0.2"
    val scalatest = "3.0.0-M15"
    val upickle = "0.3.6"
    val pickling = "0.10.1"
  }

  val upickle = "com.lihaoyi" %% "upickle" % Versions.upickle
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % Versions.akka
  val akkaStream = "com.typesafe.akka" %% "akka-stream-experimental" % Versions.akkaStream
  val akkaHttpCore = "com.typesafe.akka" %% "akka-http-core-experimental" % Versions.akkaStream
  val akkaHttp = "com.typesafe.akka" %% "akka-http-experimental" % Versions.akkaStream
  val akkaHttpTestkit = "com.typesafe.akka" %% "akka-http-testkit-experimental" % Versions.akkaStream
  val scalatest = "org.scalatest" %% "scalatest" % Versions.scalatest % "test"
  val pickling = "org.scala-lang.modules" %% "scala-pickling" % Versions.pickling

  val backend = Seq(upickle, akkaActor, akkaStream, akkaHttp, scalatest, pickling)

}