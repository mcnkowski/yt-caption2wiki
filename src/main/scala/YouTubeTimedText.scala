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
      .flatMap {
        case HttpResponse(StatusCodes.OK,_,entity,_) =>
          Unmarshal(entity).to[String]
        case HttpResponse(code,_,_,_) =>
          throw new Exception(s"Video captions could not be retrieved.\nVideo ID: $video\nCode: $code")
      }
  }

}