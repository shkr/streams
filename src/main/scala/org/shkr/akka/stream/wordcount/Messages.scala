package org.shkr.akka.stream.wordcount

/**
 * Copyright (c) 2015 Lumiata Inc.
 */
object Messages {

  case class LinkListing(links: scala.collection.immutable.Seq[Link])

  case class Link(id: String, subreddit: String)

  case class CommentListing(subreddit: String, comments: scala.collection.immutable.Seq[Comment])

  case class Comment(subreddit: String, body: String){
    val alpha = (('a' to 'z') ++ ('A' to 'Z')).toSet

    def normalize(s: Seq[String]): Seq[String] =
      s.map(_.filter(alpha.contains).map(_.toLower)).filterNot(_.isEmpty)

    def toWordCount: WordCount =
      normalize(body.split(" ").to[Seq])
        .groupBy(identity)
        .mapValues(_.length)
  }
}
