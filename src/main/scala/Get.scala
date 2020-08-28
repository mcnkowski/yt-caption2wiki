package mcnkowski.wikicaptions

import akka.actor.ClassicActorSystemProvider
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse, Uri}

import scala.concurrent.{ExecutionContext, Future}

object Get {
  def apply(uri:Uri,headers:Seq[HttpHeader] = Nil)
           (implicit system:ClassicActorSystemProvider, executionContext:ExecutionContext):Future[HttpResponse] = {
      Http().singleRequest(HttpRequest(GET,uri = uri,headers = headers))
  }
}
