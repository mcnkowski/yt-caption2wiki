package mcnkowski.wikicaptions

import scala.concurrent.Future

trait CaptionDownloader {

  def download(video:String,lang:String):Future[String]
  
  def format:String //format of returned captions
}