name := "akka-reddit-wordcount"
 
version := "0.1.0 "
 
scalaVersion := "2.11.5"

val akkaVersion = "2.3.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-agent" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-experimental" % "1.0",
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0",
  "com.typesafe.play" %% "play-json" % "2.4.3",
  "org.scalaz" %% "scalaz-core" % "7.2.0-M3"
)

scalacOptions ++= Seq("-feature")
