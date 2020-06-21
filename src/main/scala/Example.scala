import mcnkowski.wikicaptions._
import java.io.File


object Example extends App {

  //file containing video IDs
  val file = new File("videos.txt")
  
  //CaptionDownloader instance
  val youtubes = new YouTubeTimedText
  
  //XML-to-plain caption parser
  val parser = new TimedTextParser
  
  val wiki = new MediaWiki
  wiki.disambiguation(IGNORE)
  
  val extractor = new NounExtractor(NounExtractor.defaultTokenizer,NounExtractor.posTagger("en-pos-perceptron.bin"))
  //val system = new SimpleCaptionSystem(youtubes,parser,wiki,extractor)
  
  val system = new ActorCaptionSystem("YTActorSystem",youtubes,parser,wiki,extractor)
  system.continue { json => 
    (json \\ "url").foreach(println(_))
  }
  
  system.execute(file,"en",???)
}