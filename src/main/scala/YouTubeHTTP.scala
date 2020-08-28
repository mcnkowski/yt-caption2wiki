package mcnkowski.wikicaptions

import java.net.InetSocketAddress
import java.nio.charset.Charset

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.`Location`

import scala.concurrent.Future
import akka.actor.{Actor, ActorRef, ActorSystem, ClassicActorSystemProvider, PoisonPill, Props}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import HttpMethods._
import akka.io.IO
import akka.util.ByteString

case class AuthCode(value:String)
case class ErrorCode(message:String)

object YouTubeHTTP {
  import java.awt.Desktop
  import java.net.URI
  import akka.io.Tcp._
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
  def requestToken(clientID:String,port:Int,forwardTo:ActorRef)
                  (implicit system:ActorSystem, executionContext:ExecutionContext):Unit = {
    val scope = "https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fyoutube.force-ssl"
    val redirect = "https%3A%2F%2Flocalhost%3A" + port
    /*val uri = Uri.from(
      scheme = "https", host = "accounts.google.com", path = "/o/oauth2/v2/auth",
      queryString = Some(s"client_id=$clientID&redirect_uri=$redirect&scope=$scope&response_type=code")
    )*/
    val uri = s"https://accounts.google.com/o/oauth2/v2/auth?client_id=$clientID&redirect_uri=$redirect&scope=$scope&response_type=code"

    val tcp = system.actorOf(Props(classOf[TCPListener],forwardTo),"TCPActor")
    tcp ! Bind(tcp,new InetSocketAddress("localhost", port))

    browse(uri)
    //TODO: do something if access is denied; set a timeout maybe
  }

  //TODO: CAN THIS BE DONE WITHOUT JAVA
  private def browse(uri:String):Unit = {
    if (Desktop.isDesktopSupported && Desktop.getDesktop.isSupported(Desktop.Action.BROWSE)) {
      println("Please log in via browser.")
      Desktop.getDesktop.browse(new URI(uri))
    }
  }

  private class TCPListener(forwardTo:ActorRef) extends Actor {
    import akka.io.Tcp
    implicit val sys = context.system

    def receive = {
      case c @ Bind(_, _, _, _, _) =>
        IO(Tcp) ! c
        context.become(connected)
    }

    def connected:PartialFunction[Any,Unit] = {
      case Bound(_) =>
        println("Local address bound.")
      case CommandFailed(_: Connect) =>
        println("Connection failed.")
        context.stop(self)
      case Connected(_,_) =>
        val connection = sender()
        connection ! Register(self)
        context.become {
          case Received(code:ByteString) =>
            val codeString = code.decodeString(Charset.forName("UTF-8"))
            if (codeString.matches("^code=[\\S]+")) {
              forwardTo ! AuthCode(codeString)
              context.stop(self)
            } else {
              //TODO: send some message informing about an error
              forwardTo ! ErrorCode(codeString)
              context.stop(self)
            }
          case _ =>
            println("Something went wrong.")
            context.stop(self)
        }
    }
  }
}

class YouTubeHTTP(apiKey:String)(implicit system:ActorSystem, executionContext:ExecutionContext) {
  ???
}