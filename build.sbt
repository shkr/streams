name := "akka-reddit-wordcount"
 
version := "0.1.0 "
 
scalaVersion := "2.11.5"

val akkaVersion = "2.3.14"
val akkaStreamVersion = "2.0.1"
val akkaHttpVersion = "2.0.2"
val kafkaVersion = "0.9.0.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-agent" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-experimental" % akkaHttpVersion,
  "com.typesafe.scala-logging" % "scala-logging_2.11" % "3.1.0",
  "com.typesafe.akka" %% "akka-stream-experimental" % akkaStreamVersion,
  "com.typesafe.play" %% "play-json" % "2.4.3",
  "org.apache.kafka" %% "kafka" % kafkaVersion,
  "org.scalaz" %% "scalaz-core" % "7.2.0-M3"
)

scalacOptions ++= Seq("-feature")
