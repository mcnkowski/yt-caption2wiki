package mcnkowski.wikicaptions

import akka.actor.{Actor,ActorRef,Props,ActorSystem,Stash,Terminated}
import akka.actor.Actor.Receive
import akka.routing.BalancingPool
import java.io.{File,PrintWriter}
import scala.concurrent.Future
import scala.util.Using
import java.util.regex.Pattern 
import scala.collection.mutable.HashSet
import scala.io.Source
import scala.collection.mutable.ListBuffer
import play.api.libs.json.{Json,JsValue}



private case class CaptionRequest(id:String,language:String,path:String)
private case class WordsRequest(id:String,raw:String,plain:String,path:String)
private case class WikiRequest(id:String,raw:String,plain:String,words:Seq[String],path:String)
private case class WikiWord(word:String)
private case class WikiResponse(a:Option[Article])
private case class SaveRequest(dl:DownloadContent,path:String)
private case class Finished(result:JsValue)
private case class Continue(run:JsValue => Unit)
 
class ActorCaptionSystem(systemName:String,dl:CaptionDownloader,p:CaptionParser,wiki:MediaWiki,ext:NounExtractor) {
  private val videopattern = Pattern.compile("[A-Za-z0-9]{11}")
  
  private val system = ActorSystem(systemName)
  private val writeActor = system.actorOf(Props(classOf[WriteActor]),"Write")
  private val wikiActorPool = system.actorOf(Props(classOf[WikiActor],wiki),"Wiki")
  private val nounActor = system.actorOf(Props(classOf[NounActor],ext),"Nouns")
  private val ytActor = system.actorOf(Props(classOf[YTActor],dl,p),"Captions")
  private val overseer = system.actorOf(Props(classOf[OverseerActor],
    ytActor,wikiActorPool,nounActor,writeActor),"ActorOverseer")

  def continue(fun:JsValue=>Unit):Unit = {
    overseer ! Continue(fun)
  }

  //extract 11-character-long alphanumeric strings from File
  //use HashSet to avoid duplicates
  def extractVideoIds(file:File) = {
    val videoSet = HashSet.empty[String]
    val text = Source.fromFile(file).getLines.mkString("\n")
    val matcher = videopattern.matcher(text)
    while (matcher.find()) {
      videoSet += matcher.group(0)
    }
    videoSet
  }
  
  def execute(file:File,lang:String,path:String):Unit = {
    extractVideoIds(file) foreach {id => overseer ! CaptionRequest(id,lang,path)}
  }
  
  def terminate:Future[Terminated] = {
    system.terminate()
  }
}



  class OverseerActor(yt:ActorRef,wiki:ActorRef,noun:ActorRef,write:ActorRef) extends Actor {
    
    //wrapper object for the continue method
    private var continue = new Continue(x => {})
  
    def setContinue(c:Continue):Unit = continue = c

    def receive = {
      case c @ Continue(_) => setContinue(c)             //set a behavior to execute when a task is complete
      case cap @ CaptionRequest(_,_,_) => yt ! cap       //send a download request to the CaptionDownloader
      case words @ WordsRequest(_,_,_,_) => noun ! words //send an "extract nouns" request to the NounExtractor
      case w @ WikiRequest(_,_,_,_,_) => wiki ! w        //send a "get wiki articles" request to the MediaWiki
      case save @ SaveRequest(_,_) => write ! save       //send a "write file" request to the writer actor
      case Finished(result) => continue.run(result)      //when a task is finished call the continue method
    }
  }
  
  //caption downloader actor
  //downloads raw captions and parses them to plain text
  class YTActor(downloader:CaptionDownloader,parser:CaptionParser) extends Actor {
    def receive = {
      case CaptionRequest(id,lang,path) =>
        try {
          val raw = downloader.download(id,lang)
          val plain = raw.fold("")(parser.parse(_))
          sender ! WordsRequest(id,raw.getOrElse(""),plain,path)
        } catch {
          case e:java.io.IOException => println("Couldn't retrieve captions from video " + id)
        }
    }
  }

  //noun extractor actor
  class NounActor(extractor:NounExtractor) extends Actor {
    def receive = {
      case WordsRequest(id,raw,plain,path) =>
        val words = extractor.extractNounsFrom(plain)
        sender ! WikiRequest(id,raw,plain,words,path)
    }
  }

  //MediaWiki call actor
  class WikiActor(wiki:MediaWiki) extends Actor with Stash {
    
    val cache = ListBuffer.empty[Option[Article]]
    var counter:Int = 0
    
    //create a pool of child actors that will perform MediaWiki GET calls
    val wikiPool = context.actorOf(BalancingPool(4)
      .props(Props(classOf[WikiLookupActor],wiki)),"WikiArticle")
      
    def receive = {
      case WikiRequest(id,raw,plain,words,path) =>
        val overseer:ActorRef = sender //store the original sender
        
        //send requests to child actors and change behavior to one that waits for children to finish before processing another set of words
        words.foreach(word => {
          wikiPool ! WikiWord(word)
        })
        context.become({
          case WikiResponse(article) =>
            
            cache += article
            counter = counter + 1
            
            if (counter == words.size) { //send results to writer actor and reset to previous state
              overseer ! SaveRequest(DownloadContent(id,YTCaptions(raw,plain),cache.collect(Article.dropEmpty).toSeq),path)
              counter = 0
              cache.clear()
              unstashAll()
              context.unbecome()
            }
            
          case WikiRequest(_,_,_,_,_) => stash()
        }, discardOld = false)
    }
  }
  
  //actor that performs MediaWiki lookups
  //used as child actors in WikiActor
  class WikiLookupActor(wiki:MediaWiki) extends Actor {
    def receive = {
      case WikiWord(word) =>
        val result = wiki.fetch(word)
        sender ! WikiResponse(result)
    }
  }

  //actor that writes results to a file and reports to the overseer when it's done
  class WriteActor extends Actor {
    def receive = {
      case SaveRequest(content,path) =>
        if (!content.captions.raw.isEmpty) {
          val json = Save2Json(content)
          Using(new PrintWriter(new File(path+content.videoId+".json"))) { writer =>
            writer.write(Json.stringify(json))
          }
          sender ! Finished(json)
        }
    }
  }