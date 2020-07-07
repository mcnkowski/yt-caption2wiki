import mcnkowski.wikicaptions._
import java.io.File
import akka.actor.ActorSystem

object Example extends App {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  //file containing video IDs
  val file = new File("videos.txt")
  
  //CaptionDownloader instance
  val youtubes = new YouTubeTimedText
  
  //XML-to-plain caption parser
  val parser = new TimedTextParser
  
  val wiki = new MediaWiki
  
  val extractor = new NounExtractor(NounExtractor.defaultTokenizer,NounExtractor.posTagger("en-pos-perceptron.bin"))
  //val system = new SimpleCaptionSystem(youtubes,parser,wiki,extractor)
  
  /*val system = new ActorCaptionSystem("YTActorSystem",youtubes,parser,wiki,extractor)
  system.continue { json => 
    (json \\ "url").foreach(println(_))
  }
  
  system.execute(file,"en",???)*/
  
  implicit val system = ActorSystem("YTStreamSystem") //actor system can't be nested in the class apparently
  val stream = new StreamSystem(youtubes,parser,wiki,extractor)
  val mat = stream.graph("videos.txt","testcontent\\video_").run()
  mat.foreach(_ => system.terminate())
}