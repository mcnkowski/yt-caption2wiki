package test

import org.scalatest._
import scala.io.Source
import matchers._
import flatspec._
import scala.language.reflectiveCalls
import mcnkowski.wikicaptions.{YouTubeTimedText,TimedTextParser}


class TimedTextSpec extends AnyFlatSpec with should.Matchers{
  /*
  val videoID = "uTQ5fzSNWIc"
  val notavideoID = "fffffffffff"
  val downloader = new YouTubeTimedText
  val xmlparser = new TimedTextParser
  
  "YT TimedText" should "return a Some(string) containing captions in XML format" in {
    val rawcaptions = downloader.download(videoID,"en")
    assert(!rawcaptions.get.isEmpty)
    val plaincaptions = xmlparser.parse(rawcaptions.get)
    assert(!plaincaptions.isEmpty)
  }
  
  it should "return an empty string for a video that doesn't have captions in selected language" in {
    val rawcaptions = downloader.download(videoID,"pl")
    assert(rawcaptions.get.isEmpty)
  }
  
  it should "return a None if it fails to make a connection" in {
    val rawcaptions = downloader.download(notavideoID,"en")
    rawcaptions should be theSameInstanceAs None
  }
   */
}