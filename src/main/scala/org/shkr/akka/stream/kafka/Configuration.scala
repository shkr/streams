package org.shkr.akka.stream.kafka

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{headers, HttpHeader}
import akka.stream._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

object Configuration {

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher
  val settings: ActorMaterializerSettings = ActorMaterializerSettings(actorSystem)
  implicit val actorMaterializer: Materializer = ActorMaterializer(settings)
}
