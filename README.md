# streams

[API Documentation](http://shkr.github.io/streams)

A Word Count example using the Reddit API and the akka-stream flows;
The project is extended from https://github.com/pkinsky/akka-streams-example
It served as a great spring board. Currently the only change is the use of akka-http instead of dispatcher
library for Client access to the Reddit API.  

```
> sbt clean

> sbt compile
```

### org.shkr.akka.streams.wordcount : Word Count
---

  * A simple word count example

```
> sbt "runMain org.shkr.akka.stream.wordcount.RedditFlow"
 
```

