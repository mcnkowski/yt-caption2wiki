package mcnkowski.wikicaptions

import java.net.{URL,HttpURLConnection}
import scala.io.Source
import scala.util.{Try,Using,Success,Failure}

class YouTubeTimedText extends CaptionDownloader{
  private val API = "https://www.youtube.com/api/timedtext"
  override val format = "xml"
  private var connectionTimeOut:Int = 5000
  private var readTimeOut:Int = 5000
  
  override def download(video:String,lang:String):Option[String] = {
    val URL = API + "?lang=" + lang + "&v=" + video
    val content:Try[String] =
      Using(connect(URL).getInputStream()) { in =>
        Source.fromInputStream(in).mkString
      }
    content.toOption
  }
  
  @throws[java.io.IOException]
  @throws[java.net.SocketTimeoutException]
  private def connect(url:String):HttpURLConnection = {
    val connection = (new URL(url)).openConnection.asInstanceOf[HttpURLConnection]
    connection.setConnectTimeout(connectionTimeOut)
    connection.setReadTimeout(readTimeOut)
    connection.setRequestMethod("GET")
    connection
  }
  
  def connectionTimeout(time:Int):Unit = connectionTimeOut = time
  def connectionTimeout:Int = connectionTimeOut
  
  def readTimeout(time:Int):Unit = readTimeOut = time
  def readTimeout:Int = readTimeOut
}