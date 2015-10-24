# streams

A Word Count example using the Reddit API and the akka-stream flows;
The project is extended from https://github.com/pkinsky/akka-streams-example
It served as a great spring board. Currently the only change is the use of akka-http instead of dispatcher
library for Client access to the Reddit API.  

```
> sbt clean

> sbt compile
```

Note : this project uses the `akka.cluster.ClusterActorRefProvider` as the default actor provider.
 

### org.shkr.akka.streams.wordcount : Word Count
---

  * A simple word count example

```
> sbt "runMain org.shkr.akka.stream.wordcount.RedditFlow"
 
```

