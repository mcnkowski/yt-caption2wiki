package mcnkowski.wikicaptions

import scala.concurrent.Future
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import HttpMethods._

class YouTubeTimedText(implicit system:akka.actor.ClassicActorSystemProvider, executionContext:scala.concurrent.ExecutionContext) extends CaptionDownloader{

  private val API = "https://www.youtube.com/api/timedtext"

  override val format = "xml"


  override def download(video:String,lang:String):Future[String] ={
    Http().singleRequest(HttpRequest(GET,uri = API + "?lang=" + lang +"&v=" + video))
      .flatMap(response => Unmarshal(response.entity).to[String])
  }

  /*override def download(video:String,lang:String):Option[String] = {
    val URL = API + "?lang=" + lang + "&v=" + video
    val content:Try[String] =
      Using(connect(URL).getInputStream()) { in =>
        Source.fromInputStream(in).mkString
      }
    content.toOption
  }*/
  
  /*@throws[java.io.IOException]
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
  def readTimeout:Int = readTimeOut*/
}