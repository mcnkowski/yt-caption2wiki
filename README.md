An app that downloads captions from youtube videos and looks up wikipedia articles based on their contents.

APIs and libraries used:
* [YouTube Data API v3](https://developers.google.com/youtube/v3)
* [Scala XML](https://github.com/scala/scala-xml)
* [Play JSON](https://github.com/playframework/play-json)
* [Apache OpenNLP](http://opennlp.apache.org/)
* [Akka Actors](https://akka.io/docs/)


## Project files

* `SimpleCaptionSystem`: Downloads captions and related wiki articles without the use of multithreading.
* `ActorCaptionSystem`: Downloads captions and related wiki articles with the use of Akka Actors.
* `CaptionDownloader`: Trait used for classes that download YouTube captions.
* `YouTubeTimedText`: Used to download captions in XML format from any YouTube video.
* `YouTubeV3`: Downloads captions in SBV format using the official YouTube Data API. Requires a Google authentication token.
* `CaptionParser`: Trait used for classes that remove formatting from captions, turning them into plain text.
* `TimedTextParser`: Removes XML formatting from captions returned by YouTubeTimedText.
* `SBVParser`: Removes timestamps from SBV captions returned by YouTubeV3.
* `MediaWiki`: Used do make MediaWiki calls in order to get Wikipedia article contents.
* `WikiText`: Used to retrieve information from MediaWiki API responses.
* `NounExtractor`: Tokenizes captions and returns nouns.
* `Save2Json`: Writes contents into a JSON.

## ActorCaptionSystem

ActorCaptionSystem makes use of Akka Actor library.
Each action (downloading captions, extracting nouns, MediaWiki calls, writing to file) is done by a different actor.
Additionally MediaWiki actor makes use of a Balancing Pool to perform several calls concurrently.


ActorCaptionSystem is instantiated similarly to SimpleCaptionSystem, with the addition of ActorSystem name
```scala
val captionDownloader = new YouTubeTimedText //YouTubeV3("client_secret.json")
val captionParser = new TimedTextParser //new SBVParser
val mediaWiki = new MediaWiki
val nounExtractor = NounExtractor("en-pos-perceptron.bin")
val actSystem = new ActorCaptionSystem(systemName,captionDownloader,captionParser,mediaWiki,nounExtractor)
```

To download and save captions and wikipedia articles `.execute()` method is used.

```scala
val videoIdFile = new File("FileContainingVideoIds.txt")
val captionLanguage = "en"
val savePath = "C:\\example\\path\\"

simpleSystem.execute(videoIdFile,captionLanguage,savePath)
```
The function accepts a java File object containting video IDs, a string specifying caption language, and path to which the results should be saved.
Video IDs are extracted from the file via pattern matching. IDs are expected to be 11-character-long alphanumeric strings.
Caption language needs to be specified using a BCP 47 language tag.

For each video ID one JSON file is created in the specified path, with the video ID in its title.
Each file contains video ID, raw captions retrieved from the video, plain text captions, and a list of wikiepdia articles containing the article URL, raw version of the article, and a plain text version.


A message is sent to an overseer actor every time a set of captions and Wiki articles is saved to a JSON.
`.continue()` method allows user to set a behavior to execute each time overseer is informed about a finished task.
```scala
system.continue(x => println("Done"))
```
The `continue` method accepts `JsValue => Unit` functions as an input. 
The `JsValue` in question is a Play JSON object containing results of a finished task.


## SimpleCaptionSystem

SimpleCaptionSystem performs tasks sequentially in a single thread.

SimpleCaptionSystem can be instantiated as follows:
```scala
val captionDownloader = new YouTubeTimedText //YouTubeV3("client_secret.json")
val captionParser = new TimedTextParser //new SBVParser
val mediaWiki = new MediaWiki
val nounExtractor = NounExtractor("en-pos-perceptron.bin")
val simpleSystem = new SimpleCaptionSystem(captionDownloader,captionParser,mediaWiki,nounExtractor)
```

To download and save captions and wikipedia articles `.execute()` method is used.

```scala
val videoIdFile = new File("FileContainingVideoIds.txt")
val captionLanguage = "en"
val savePath = "C:\\example\\path\\"

simpleSystem.execute(videoIdFile,captionLanguage,savePath)
```
The function accepts a java File object containting video IDs, a string specifying caption language, and path to which the results should be saved.
Video IDs are extracted from the file via pattern matching. IDs are expected to be 11-character-long alphanumeric strings.
Caption language needs to be specified using a BCP 47 language tag.

For each video ID a JSON containing its captions and related wiki articles is written to the specified path.
`.execute()` doesn't return any values



## CaptionDownloader classes

YouTubeTimedText and YouTubeV3 are classes used to make Google API calls to get captions from YouTube videos.
To retrieve captions from a video `.download()` method is used.
```scala
def download(video:String,lang:String):Option[String]
```
If download fails to retrieve captions it should return a `None`. YouTubeTimedText however will return a `Some` of an empty string if an invalid language is requested.


YouTubeTimedText as its name suggests, makes use of YouTube's timedtext API.
Timedtext allows you to get captions from any video in XML format. However due to complete lack of any documentation for the API, it is possible that it will become unavailable.

YouTubeTimedText allows you to set connection timeout and read timeout variables with `connectionTimeout(time:Int)` and `readTimeout(time:Int)` methods respectively.


YouTubeV3 makes use of Google's official YouTube Data API.

YouTubeV3 requires OAuth 2.0 credentials in order to establish connection with the API. 
An OAuth 2.0 Client with youtube.force-ssl scope is required.

YouTubeV3 can be instantiated via the companion object by passing it the path to the file containing client secret.
```scala
val client_secret = "\\path\\to\\client_secret.json"
val captionDownloader = YouTubeV3(client_secret)
```

YouTubeV3 will only start working after the user has logged in and gave the app necessary permissions.

YoutubeV3 returns captions in SBV format

Both CaptionDownloader classes need to be paired with their respective CaptionParsers


## MediaWiki

MediaWiki is a class used to make MediaWiki API calls.

Using `.fetch(word)` method returns an article URL, contents in HTML format, and contents in plain text format, wrapped in an `Article(url,html,plain)` object.
```scala
val wiki = new MediaWiki
val wikiArticle = MediaWiki.fetch("Pizza")
println(wikiArticle.url)
println(wikiArticle.html)
println(wikiArticle.plain)
```

If an API call is unsuccessful the method will return an empty Article object: `Article("","","")`.


MediaWiki class has three ways of handling disambiguation pages:
* `IGNORE` - retrieve disambiguation page as if it was a regular article; default
* `SKIP` - don't retrieve disambiguation pages at all
* `FIRST` - retrieve the first article listed on the disambiguation page

Behavior is set by calling `.disambiguation()` method
```scala
wiki.disambiguation(FIRST)
```

MediaWiki allows you to set connection timeout and read timeout variables with `connectionTimeout(time:Int)` and `readTimeout(time:Int)` methods respectively.


## NounExtractor

NounExtractor class makes use of the Apache Natural Language Processing library to extract nouns from text.

The class constructor requires a tokenizer object and a part-of-speech tagger object. A simple tokenizer is provided by the OpenNLP library and can be accessed via the NounExtractor companion object. POS tagger on the other hand needs to be imported from a file.

```scala
val token = NounExtractor.defaultTokenizer
val tagger = NounExtractor.posTagger("\\path\\to\\POS-Tagger.bin")
val NNExtractor = new NounExtractor(token,tagger)
```

Alternatively the path to the POS tagger can be passed directly to companion object's `apply` method.
```scala
val NNExtractor = NounExtractor("\\path\\to\\POS-Tagger.bin")
```

## Example

```scala
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
  
  system.execute(file,"en","C:\\save\\path\\)
}
```