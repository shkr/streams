Scraping Reddit with Akka Streams 0.9
=====================================


motivation: Reddit is kind enough to provide simple APIs for accessing popular content. We're going to grab a list of the top 100 subreddits, the top 100 posts for each subreddit, and the top 2000 comments for each post. This will require 1 + 100 + 100 * 100 API calls. Since we'd like to be good internet citizens, (and not get our IP banned) we want to issue these api calls at consistient intervals. First, I'll show a simple future-based approach and why it's likely to fail.

API Sketch:
-----------
    //types
    type WordCount = Map[String, Int] 
    type Subreddit = String  
    case class LinkListing(links: Seq[Link])
    case class Link(id: String, subreddit: Subreddit)
    case class CommentListing(subreddit: Subreddit, comments: Seq[Comment])
    case class Comment(subreddit: Subreddit, body: String)

    //reddit API
    def popularLinks(subreddit: Subreddit)(implicit ec: ExecutionContext): Future[LinkListing]
    def popularComments(link: Link)(implicit ec: ExecutionContext): Future[CommentListing]
    def popularSubreddits(implicit ec: ExecutionContext): Future[Seq[Subreddit]]

    // mock KV store
    def addWords(subreddit: Subreddit, words: WordCount): Future[Unit]
    def wordCounts: Future[Map[Subreddit, WordCount]]


Naive Solution:
--------------

future based solution: use flatMap and Future.sequence to build a Future[Seq[Comment]] (link to code, don't go into depth)
    - gets a subreddit list, immediately issues requests for each subreddit. When that's done, gets a link listing, immediately issues requests for comments for each link.
    - bursty. That's the key point, and our motivation.


This works fine at first, then starts failing with 503: service unavailable errors as rate limiting kicks in. Try it for yourself: open up a console and type 'main.Simple.run()' (you'll want to kill the process after seeing the expected stream of 503 errors)


Streams
-------

What we want is to issue requests at configurable intervals. This will prevent the rate limiting that is a predicatable result of berzerking their servers with 10k requests in a short amount of time. (However you feel about reddit as a community, DDOS'ing them is still rude)

To accomplish this we're going to use akka's new stream library. 

First, we will create some Ducts, each a description of stream transformations from an In type to an Out type. (I say description because a Duct is not instantiated until it is materialized. (link to info)). We're going to need a Duct[Subreddit, Comment] to turn our starting stream of subreddits into a stream of comments. We're also going to use a Duct[Comment, Int] to persist batches of comments, outputing the size of the persisted batches. Having created these high-level descriptions of computations to be performed, we can then append them to a Flow[Subreddit] (created using the result of the popularSubreddits api call or the list of subreddits provided as command line arguments if present)


1. First, we need to transform a flow of subreddit names into a flow of the top comments in the top threads of each subreddit. We also want to limit our calls to 4 per second, or one every 250 milliseconds. Here's how: (I'll break it down later)

        val redditAPIRate = 250 millis
         
        case object Tick
        val throttle = Flow(redditAPIRate, redditAPIRate, () => Tick)
         
        /** transforms a stream of subreddits into a stream of the top comments
         *  posted in each of the top threads in that subreddit
         */
        def fetchComments: Duct[Subreddit, Comment] = 
          Duct[Subreddit] // create a Duct[Subreddit, Subreddit]
              .zip(throttle.toPublisher).map{ case (t, Tick) => t }
              .mapFuture( subreddit => RedditAPI.popularLinks(subreddit) )
              .mapConcat( listing => listing.links )
              .zip(throttle.toPublisher).map{ case (t, Tick) => t }
              .mapFuture( link => RedditAPI.popularComments(link) )
              .mapConcat( listing => listing.comments )



1. Duct[Subreddit, Comment]
    - alt: explain building blocks (mapFuture, mapConcat, zip w/ throttle & discard. Then: here's how it fits together.
        - Duct.apply
        - mapFuture
        - mapConcat
        - zip (w/ throttle)

7. Duct[Comment, Int]
    - explain new building block, groupedWithin
        - groupedWithin
    - show how it fits together, explain semi-complex logic in mapFuture

8. final append: 
    - no processing has occured, no api calls made. We've just described what we want to do. Now make it so.
    - 

9. closing: things that could be better, mention new flow graph for complex topologies. (this is scalaDSL, see scalaDSL2 for the newness)