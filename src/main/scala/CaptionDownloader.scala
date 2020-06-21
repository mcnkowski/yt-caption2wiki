package mcnkowski.wikicaptions

trait CaptionDownloader {

  def download(video:String,lang:String):Option[String]
  
  def format:String 
}