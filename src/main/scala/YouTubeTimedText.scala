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
      .flatMap { response =>
        if (response._1.intValue == 200) {
          Unmarshal(response.entity).to[String]
        } else {
          throw new Exception(s"Video captions could not be retrieved.\nVideo ID: $video\nCode: ${response._1.intValue}")
        }
      }
  }

}