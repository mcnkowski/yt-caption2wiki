import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import scala.concurrent.Future
import scala.util.{ Failure, Success }
import HttpMethods._

class YouTubeHTTP (implicit system:akka.actor.ActorSystem, executionContext:scala.concurrent.ExecutionContext) {
  /*
  https://accounts.google.com/o/oauth2/v2/auth
  client_id from dev credentials
  redirect_uri http://127.0.0.1:port or is it urn:ietf:wg:oauth:2.0:oob:auto ?
  response_type ??? "code" ?
  */

  /*
  https://www.googleapis.com/youtube/v3/captions?key=[YOUR_API_KEY] HTTP/1.1
  Authorization: Bearer [YOUR_ACCESS_TOKEN]
  Accept: application/json
  */

  def get(uri:Uri,headers:Seq[HttpHeader] = Nil):Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(GET,uri = uri,headers = headers))
  }
}
