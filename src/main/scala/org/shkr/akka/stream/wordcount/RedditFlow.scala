package org.shkr.akka.stream.wordcount

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.scaladsl._
import org.shkr.akka.stream.wordcount.Messages.{Comment, Link}
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}
/**
 * RedditFlow
 * Created by shkr
 */
object RedditFlow {

  import RedditAPI._
  import Configuration._

  /**
   * @define Source of top [[Configuration.subredditsToFetch]] subreddit names
   */
  val subreddits: Source[String, Unit] = Source.single[(HttpRequest, String)](popularSubredditsRequest -> "TOP_SUBREDDIT")
    .via(poolClientFlow)
    .mapAsyncUnordered[List[String]](Configuration.parallelism)({
      case (Success(response), _) => findPopularSubreddits(response)
      case (Failure(response), _) => Future { List.empty[String] }
    }).mapConcat[String](identity[List[String]])

  /**
   * @define a Flow which consumes a Stream of incoming subreddit names and maps it into
   *         a stream of top [[Configuration.linksToFetch]] posts in the incoming subreddit.

   */
  val topPosts: Flow[String, Link, Unit] = Flow[String]
    .map[(HttpRequest, String)](subreddit => popularLinksRequest(subreddit) -> subreddit)
    .via(poolClientFlow)
    .mapAsyncUnordered[Seq[Link]](parallelism)({
      case (Success(response), subreddit) => findPopularLinks(subreddit, response)
      case (Failure(response), subreddit) => Future { Seq.empty[Link] }
    }).mapConcat[Link](identity[Seq[Link]])

  /**
   * @define a Flow which consumes a Stream of Links and maps it into a stream
   *         top [[Configuration.commentsToFetch]] comments in the incoming links
   *         The no. of replies consumed for each comment can also be configured using
   *         [[Configuration.commentDepth]]
   */
  val topComments: Flow[Link, Comment, Unit] = Flow[Link]
    .map[(HttpRequest, String)](link => popularCommentRequest(link) -> link.subreddit)
    .via(poolClientFlow)
    .mapAsyncUnordered[List[Comment]](parallelism)({
      case (Success(response), subreddit) => findPopularComments(subreddit, response)
      case (Failure(response), subreddit) => Future { List.empty[Comment] }
    }).mapConcat[Comment](identity[List[Comment]])

  /**
   * @define a Sink which consumes a stream of incoming Comments identifieable by their
   *         subreddits and then creates a Map of WordCount table for each unique subreddit
   *         identified in the incoming Comments Stream
   */
  val wordCountSink: Sink[Comment, Future[Map[String, WordCount]]] =
    Sink.fold(Map.empty[String, WordCount])(
      (acc: Map[String, WordCount], c: Comment) =>
        mergeWordCounts(acc, Map(c.subreddit -> c.toWordCount))
    )

  /**

    @define This is the Stream Processing Graph for throttle which unlike
            other flows uses the [[Zip]] junction. This throttles the
            Stream by a [[Configuration.redditAPIRate]] ticker
    +------------+
    | tickSource +-Unit-+
    +------------+      +---> +-----+            +-----+      +-----+
                              | zip +-(T,Unit)-> | map +--T-> | out |
    +----+              +---> +-----+            +-----+      +-----+
    | in +----T---------+
    +----+
    tickSource emits one element per `rate` time units and zip only emits when an element is present from its left and right
    input stream, so the resulting stream can never emit more than 1 element per `rate` time units.
    */
  def throttle[T](rate: FiniteDuration): Flow[T, T, Unit] = {
    Flow() { implicit builder =>
      import akka.stream.scaladsl.FlowGraph.Implicits._
      val zip = builder.add(Zip[T, Unit.type]())
      Source(rate, rate, Unit) ~> zip.in1
      (zip.in0, zip.out)
    }.map(_._1)
  }


  def main(args: Array[String]): Unit={

    val wordCountKeyByReddit: Future[Map[String, WordCount]] = subreddits
      .via(topPosts)
      .via(topComments)
      .via(throttle(redditAPIRate))
      .runWith(wordCountSink)

    wordCountKeyByReddit.onComplete(f => {
      writeResults(f)
      Http().shutdownAllConnectionPools().onComplete({
        case Success(_) => {
          printlnC("Successfuly shutdown Http Host level Connection")
          actorSystem.shutdown()
        }
        case Failure(_) => {
          printlnE("Failed in properly shutting down Http Host level Connection")
          actorSystem.shutdown()
        }
      })
    })
  }
}
