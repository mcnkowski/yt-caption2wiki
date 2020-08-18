import mcnkowski.wikicaptions._
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import java.io.File
//import akka.actor.ActorSystem

object Example extends App {
  //implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  //implicit val system = ActorSystem("YTStreamSystem")
  implicit val system:ActorSystem[Nothing] = ActorSystem(Behaviors.empty,"StreamSystem")
  implicit val ec:scala.concurrent.ExecutionContext = system.executionContext
  //CaptionDownloader instance
  val youtubes = new YouTubeTimedText
  
  //XML-to-plain caption parser
  val parser = new TimedTextParser
  
  val wiki = new MediaWiki
  
  val extractor = new NounExtractor(NounExtractor.defaultTokenizer,NounExtractor.posTagger("en-pos-perceptron.bin"))
  

  val stream = new StreamSystem(youtubes,parser,wiki,extractor)
  val mat = stream.graph("videos.txt","testcontent\\video_").run()
  mat.foreach(_ => system.terminate())
}