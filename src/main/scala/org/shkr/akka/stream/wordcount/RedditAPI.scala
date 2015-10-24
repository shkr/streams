package org.shkr.akka.stream.wordcount

import akka.http.scaladsl.model._
import HttpMethods.GET
import org.shkr.akka.stream.wordcount.Messages._
import play.api.libs.json.{JsValue, Json}
import scala.collection.immutable.Seq
import scala.concurrent.Future
import Configuration._

object RedditAPI {

  def popularSubredditsRequest: HttpRequest =
    HttpRequest(method = GET, uri = s"/subreddits/popular.json?limit=$subredditsToFetch", headers = List(userAgent))
  
  def popularLinksRequest(subreddit: String): HttpRequest =
    HttpRequest(method = GET, uri = s"/r/$subreddit/top.json?limit=$linksToFetch&t=all", headers = List(userAgent))

  def popularCommentRequest(link: Link): HttpRequest =
    HttpRequest(method = GET, uri = s"/r/${link.subreddit}/comments/${link.id}.json?depth=$commentDepth&limit=$commentsToFetch", headers = List(userAgent))

  def findPopularLinks(subreddit: String, response: HttpResponse): Future[Seq[Link]] = withRetry(
    timedFuture(s"findPopularLinks for $subreddit"){
    response.entity.toStrict(timeout).map[Seq[Link]](entity => {

        (Json.parse(entity.data.decodeString("UTF-8")).as[JsValue] \ "data" \ "children").as[Seq[JsValue]].toList
          .map(item => Link(id = (item \ "data" \ "id").as[String],
          subreddit = subreddit))})}, Seq.empty[Link])

  def findPopularSubreddits(response: HttpResponse): Future[List[String]] = withRetry(timedFuture(s"TOP_SUBREDDITS")(
    response.entity.toStrict(timeout).map[List[String]](entity => {
      (Json.parse(entity.data.decodeString("UTF-8")) \\ "url").map(_.as[String]).toList
        .map(subreddit => subreddit.drop(3).dropRight(1).mkString)})), List.empty[String])

  def findPopularComments(subreddit:String, response: HttpResponse): Future[List[Comment]] = withRetry(
    timedFuture(s"findPopularComments for $subreddit"){
    response.entity.toStrict(timeout).map[List[Comment]](entity => {
      (Json.parse(entity.data.decodeString("UTF-8")) \\ "body").map(_.as[String]).toList
        .map(commentStr => new Comment(subreddit, commentStr))
    })
  }, List.empty[Comment])
}
