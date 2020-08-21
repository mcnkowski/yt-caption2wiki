package test

import java.io.File

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import org.scalatest._
import matchers._
import flatspec._
import scala.language.reflectiveCalls
import mcnkowski.wikicaptions.YouTubeV3

import scala.concurrent.ExecutionContext

class V3Spec extends AnyFlatSpec with should.Matchers with BeforeAndAfterAll {
  implicit val system:ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "TestSystem")
  implicit val ec:ExecutionContext = ExecutionContext.global
  //REQUIRES LOGGING IN
  override def afterAll(): Unit = {
    system.terminate()
  }

  assume(new File("""src\test\resources\client_secret.json""").isFile())
  val ytV3 = YouTubeV3("testuser","testapp","""src\test\resources\client_secret.json""")
  val videoID = "uTQ5fzSNWIc"
  val otherVideoID = "px5LZldjOTw"
  
  "YTV3" should "return captions from videos" in {
    val caps = ytV3.download(videoID, "en")
    caps.map(caps => assert(caps.nonEmpty))
  }
  
  it should "return None if no captions exist for given language" in {
    val caps = ytV3.download(videoID,"es")
    caps.map { cap =>
      assertThrows(new java.io.IOException)
    }
  }
}