package org.shkr.akka.stream

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import scala.collection.immutable._
import scala.concurrent._
import scala.util.{Failure, Success, Try}
import scalaz.Scalaz._

package object wordcount {

  private val tZero = System.currentTimeMillis()

  def printlnC(s: Any): Unit = println(Console.GREEN + s + Console.RESET)
  def printlnE(s: Any): Unit = println(Console.RED + s + Console.RESET)

  def timedFuture[T](name: String)(f: Future[T])(implicit ec: ExecutionContext): Future[T] = {
    val start = System.currentTimeMillis()
    printlnC(s"--> started $name at t0 + ${start - tZero}")
    f.andThen{
      case Success(t) =>
        val end = System.currentTimeMillis()
        printlnC(s"\t<-- finished $name after ${end - start} millis")
      case Failure(ex) =>
        val end = System.currentTimeMillis()
        printlnE(s"\t<X> failed $name, total time elapsed: ${end - start}\n$ex")
    }
  }

  def withRetry[T](f: => Future[T], onFail: T, n: Int = 3)(implicit ec: ExecutionContext): Future[T] =
    if (n > 0){ f.recoverWith{ case err: Exception => 
      printlnE(s"future attempt $n/3 failed with $err, retrying")
      withRetry(f, onFail, n - 1)
    }} else{
      printlnE(s"WARNING: failed to run future, substituting $onFail")
      Future.successful(onFail)
    }

  def writeTsv(fname: String, wordcount: WordCount) = {
    val tsv = wordcount.toList.sortBy{ case (_, n) => n }.reverse.map{ case (s, n) => s"$s\t$n"}.mkString("\n")
    Files.write(Paths.get(fname), tsv.getBytes(StandardCharsets.UTF_8))
  }

  def clearOutputDir() =
    for {
      files <- Option(new File("res").listFiles)
      file <- files if file.getName.endsWith(".tsv")
    } file.delete()

  type WordCount = Map[String, Int]

  def mergeWordCounts(a: Map[String, WordCount], b: Map[String, WordCount]) = a |+| b

  def writeResults(tryWordcounts: Try[Map[String, WordCount]]) = tryWordcounts match {
    case Success(wordcounts) =>
      clearOutputDir()
      wordcounts.foreach{ case (key, wordcount) =>
        val fname = s"res/$key.tsv"
        printlnC(s"write wordcount for $key to $fname")
        val p = Paths.get("res")
        if (!Files.exists(p)) Files.createDirectory(p)
        writeTsv(fname, wordcount)
        printlnC(s"${wordcount.size} distinct words and ${wordcount.values.sum} total words for $key")
      }

    case Failure(f) => 
      printlnE(s"failed with $f")
  }
}
