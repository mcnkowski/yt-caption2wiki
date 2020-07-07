package mcnkowski.wikicaptions

import java.io.{File,PrintWriter}
import scala.io.Source
import java.util.regex.Pattern 
import collection.mutable.HashSet
import collection.immutable.IndexedSeq
import play.api.libs.json.Json

class SimpleCaptionSystem(downloader:CaptionDownloader,parser:CaptionParser,wiki:MediaWiki,nounExtractor:NounExtractor) {
  private val videopattern = Pattern.compile("[A-Za-z0-9]{11}")
  
  def extractVideoIds(file:File):IndexedSeq[String] = {
    val videoSet = HashSet.empty[String]
    val text = Source.fromFile(file).getLines.mkString("\n")
    val matcher = videopattern.matcher(text)
    while (matcher.find()) {
      videoSet += matcher.group(0)
    }
    videoSet.iterator.toIndexedSeq
  }
  
  def downloadCaptions(videoIds:Seq[String],lang:String):Seq[Option[String]] = {
    videoIds.map(downloader.download(_,lang)).iterator.toSeq
  }
  
  def parseCaptions(captions:Seq[Option[String]]):Seq[String] = {
    captions.map { cap =>
      cap.fold("")(parser.parse(_))
    }
  }

  def searchWiki(captions:String):Seq[Article] = {
    val words = nounExtractor.extractNounsFrom(captions)
    words.map(word => {
      wiki.fetch(word)
    }).collect(Article.dropEmpty)
  }
  
  def execute(file:File,lang:String,path:String):Unit = { 
    val videoIds = extractVideoIds(file)
    val rawCaptions = downloadCaptions(videoIds,lang)
    val plainCaptions = parseCaptions(rawCaptions)
    val articles = plainCaptions.map(art => searchWiki(art))
    
    0 until videoIds.size foreach { n =>
      if (rawCaptions(n).isDefined) {
        val json = Save2Json(videoIds(n),rawCaptions(n).get,plainCaptions(n),articles(n))
        Save2Json.writeTo(path+videoIds(n)+".json",json)
      }
    }
  }
}