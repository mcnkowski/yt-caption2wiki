An app that downloads youtube captions based on a video ID, and compiles a list of Wikipedia articles based their content.

Can use YoutubeV3 API or YoutubeTimedText API for caption retrieval.
In case of YoutubeV3 a Google client secret credential is required.


Usage example:

```scala
object Example extends App {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val system = ActorSystem("YTStreamSystem")
 
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
```