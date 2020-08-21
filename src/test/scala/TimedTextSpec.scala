package test

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import org.scalatest._
import matchers._
import flatspec._

import scala.language.reflectiveCalls
import mcnkowski.wikicaptions.{TimedTextParser, YouTubeTimedText}


import scala.concurrent.ExecutionContext


class TimedTextSpec extends AnyFlatSpec with should.Matchers with BeforeAndAfterAll {
  implicit val system:ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "TestSystem")
  implicit val ec:ExecutionContext = ExecutionContext.global

  override def afterAll(): Unit = {
    system.terminate()
  }

  val videoID = "uTQ5fzSNWIc"
  val notavideoID = "fffffffffff"
  val downloader = new YouTubeTimedText
  val xmlparser = new TimedTextParser
  
  "YT TimedText" should "return a Future containing captions in XML format" in {
    val rawcaptions = downloader.download(videoID,"en")
    rawcaptions.map(caps => assert(caps.nonEmpty))
    val plaincaptions = rawcaptions.map(xmlparser.parse)
    plaincaptions.map(caps => assert(caps.nonEmpty))
  }
  
  it should "return an empty string for a video that doesn't have captions in selected language" in {
    val rawcaptions = downloader.download(videoID,"pl")
    rawcaptions.map(caps => assert(caps.isEmpty))
  }
  
  it should "fail the Future if it can't retrieve captions" in {
    val rawcaptions = downloader.download(notavideoID,"en")
    rawcaptions.map(caps => assertThrows(new Exception))
  }

}