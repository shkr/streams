package org.shkr.akka.stream.wordcount

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpHeader, HttpRequest}
import HttpMethods.GET
import org.shkr.akka.stream.wordcount.Messages._
import play.api.libs.json.{JsValue, Json}
import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.concurrent.Future
import akka.http.scaladsl.model.headers
import Main._

object RedditAPI {

  val linksToFetch = 15
  val subredditsToFetch = 5
  val commentsToFetch = 2000
  val commentDepth = 25
  val timeout = 2000.millis
  val userAgent: HttpHeader = headers.`User-Agent`("akka-http")

  def popularLinksRequest(subreddit: String): HttpRequest={
    HttpRequest(method = GET, uri = s"http://www.reddit.com/r/$subreddit/top.json?limit=$linksToFetch&t=all", headers = List(userAgent))
  }

  def popularCommentRequest(link: Link): HttpRequest={
    HttpRequest(method = GET, uri = s"http://www.reddit.com/r/${link.subreddit}/comments/${link.id}.json?depth=$commentDepth&limit=$commentsToFetch", headers = List(userAgent))
  }

  def popularSubredditsRequest: HttpRequest={
    HttpRequest(method = GET, uri = s"http://www.reddit.com/subreddits/popular.json?limit=$subredditsToFetch", headers = List(userAgent))
  }

  def popularLinks(subreddit: String): Future[LinkListing] = withRetry(timedFuture(s"links: r/$subreddit/top"){
    Http().singleRequest(popularLinksRequest(subreddit)).flatMap {
      case response => response.entity.toStrict(timeout).map[LinkListing](entity => {
        LinkListing((Json.parse(entity.data.decodeString("UTF-8")).as[JsValue] \ "data" \ "children").as[Seq[JsValue]].toList
            .map(item => Link(id = (item \ "data" \ "id").as[String],
                                        subreddit = (item \ "data" \ "subreddit").as[String])))
  })}}, LinkListing(Seq.empty))


  def popularComments(link: Link): Future[CommentListing] = withRetry(timedFuture(s"comments: r/${link.subreddit}/${link.id}/comments"){Http().singleRequest(popularCommentRequest(link)).flatMap[CommentListing]{
    case response => response.entity.toStrict(2*timeout).map[CommentListing](entity => {
      CommentListing(link.subreddit, (Json.parse(entity.data.decodeString("UTF-8")) \\ "body").map(_.as[String]).toList
          .map(commentStr => new Comment(link.subreddit, commentStr)))
    })
  }}, CommentListing(link.subreddit, Seq.empty))

  def popularSubreddits: Future[List[String]] = timedFuture("fetch popular subreddits"){Http().singleRequest(popularSubredditsRequest).flatMap[List[String]]{
    case response => response.entity.toStrict(2*timeout).map[List[String]](entity => {
      (Json.parse(entity.data.decodeString("UTF-8")) \\ "url").map(_.as[String]).toList
        .map(subreddit => subreddit.drop(3).dropRight(1).mkString)
    })
  }}
}
