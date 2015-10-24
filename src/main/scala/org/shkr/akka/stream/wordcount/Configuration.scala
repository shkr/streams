package org.shkr.akka.stream.wordcount

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{headers, HttpHeader}
import akka.stream._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

object Configuration {

  val userAgent: HttpHeader = headers.`User-Agent`("akka-http")

  val linksToFetch = 15
  val subredditsToFetch = 5
  val commentsToFetch = 2000
  val commentDepth = 25
  val timeout = 5000.millis

  val redditAPIRate = 500 millis
  val parallelism: Int = 10

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher
  val settings: ActorMaterializerSettings = ActorMaterializerSettings(actorSystem)
  implicit val actorMetrializer: Materializer = ActorMaterializer(settings)

  val poolClientFlow = Http().cachedHostConnectionPool[String]("www.reddit.com")
}
