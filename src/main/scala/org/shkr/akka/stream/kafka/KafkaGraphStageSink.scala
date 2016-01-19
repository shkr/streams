package org.shkr.akka.stream.kafka

import java.util.UUID
import akka.stream.scaladsl.{Source, Sink}
import akka.stream._
import akka.stream.stage.{InHandler, GraphStageLogic, GraphStage}
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.clients.producer.{ KafkaProducer, ProducerRecord }
import org.apache.kafka.common.serialization.StringSerializer

class KafkaGraphStageSink(topic: String,
                          brokerList: List[String],
                          compression: String = "none",
                          synchronously: Boolean = false,
                          batchSize: Int = 100,
                          messageSendMaxRetries: Integer = 3,
                          requestRequiredAcks: Integer = -1)
  extends GraphStage[SinkShape[String]] with LazyLogging {

  val clientID: String = s"KafkaGraphStageSink__${UUID.randomUUID().toString}"

  val properties: java.util.Properties = new java.util.Properties()
  properties.put("compression.type", compression)
  properties.put("producer.type", if (synchronously) "sync" else "async")
  properties.put("bootstrap.servers", brokerList.mkString(","))
  properties.put("batch.size", batchSize.toString)
  properties.put("retries", messageSendMaxRetries.toString)
  properties.put("acks", requestRequiredAcks.toString)
  properties.put("client.id", clientID)

  val producer = new KafkaProducer[String, String](properties, new StringSerializer, new StringSerializer)

  val closeTimeoutMs = 1000L

  val in: Inlet[String] = Inlet("KafkaGraphStageSink")

  override val shape: SinkShape[String] = SinkShape(in)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    var closed = false

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val element = grab(in)
        val record =  new ProducerRecord[String, String](topic, element)
        try {
          if (!closed) producer.send(record)
        }
        catch {
          case ex: Exception =>
            close()
            failStage(ex)
        }
        logger.warn(s"Written item to Kafka: $record")
        pull(in)
      }

      override def onUpstreamFinish(): Unit = {
        logger.warn(s"Stream finished, closing Kafka resources for topic $topic")
        close()
      }

      override def onUpstreamFailure(ex: Throwable): Unit = {
        logger.warn(s"Stream failed, closing Kafka resources for topic $topic", ex)
        close()
      }
    })

    def close(): Unit =
      if (!closed) {
        producer.close()
        closed = true
      }

    override def afterPostStop(): Unit = {
      close()
      super.afterPostStop()
    }

    override def preStart(): Unit = {
      pull(in)
    }
  }
}

object KafkaGraphStageSink {

  def main(args: Array[String]): Unit={
    import Configuration._
    val topic: String = args(0)
    val brokers: List[String] = args(1).split(",").toList
    val kafkaGraphStageSink: KafkaGraphStageSink = new KafkaGraphStageSink(topic, brokers)
    val graph = Source.fromIterator[String](() => "iterating,over,comma,separated,values".split(",").iterator).to(
      Sink.fromGraph(kafkaGraphStageSink)
    )

    graph.run()

    //Sleep for 10 seconds
    Thread.sleep(10000)

    actorSystem.shutdown()
  }
}
