package test

import java.io.File
import org.scalatest._
import matchers._
import flatspec._
import scala.language.reflectiveCalls
import mcnkowski.wikicaptions.{YouTubeV3,SBVParser}

class V3Spec extends AnyFlatSpec with should.Matchers with BeforeAndAfter {
  
  //REQUIRES LOGGING IN
  /*
  assume(new File("""src\test\resources\client_secret.json""").isFile())
  var ytV3 = YouTubeV3("""src\test\resources\client_secret.json""")
  val videoID = "uTQ5fzSNWIc"
  val otherVideoID = "px5LZldjOTw"
  
  "YTV3" should "return captions from videos" in {
    val caps = ytV3.download(videoID,"en")
    assert(caps.isDefined)
  }
  
  //turns out it does download captions someone else's videos
  ignore should "return None if user has no permissions to the video" in {
    val caps = ytV3.download(otherVideoID,"en")
    caps should be theSameInstanceAs None
  }
  
  it should "return None if no captions exist for given language" in {
    val caps = ytV3.download(videoID,"es")
    caps should be theSameInstanceAs None
  }*/
}