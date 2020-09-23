package mcnkowski.wikicaptions

import java.awt.Desktop
import java.net.{InetSocketAddress, URI}
import java.nio.charset.Charset

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.`Location`

import scala.concurrent.Future
import akka.actor.{Actor, ActorRef, ActorSystem, ClassicActorSystemProvider, PoisonPill, Props, Stash, Terminated}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import HttpMethods._
import akka.io.IO
import akka.io.Tcp.{Bind, Bound, CommandFailed, Connect, Connected, Received, Register}
import akka.util.{ByteString, Timeout}
import akka.pattern.{ask, pipe}

import scala.concurrent.duration.{Duration, FiniteDuration}

case class Initialize(port:Int,timeout:FiniteDuration)
case class AuthCode(value:String)
case class ErrorCode(message:String)
case class DownloadRequest(videoID:String,lang:String)
case object CodeRequest
case object Proceed

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

/* auth token
POST /token HTTP/1.1
Host: oauth2.googleapis.com
Content-Type: application/x-www-form-urlencoded

code=4/P7q7W91a-oMsCeLvIaQm6bTrgtp7&
client_id=your_client_id&
client_secret=your_client_secret&
redirect_uri=urn%3Aietf%3Awg%3Aoauth%3A2.0%3Aoob%3Aauto&
grant_type=authorization_code
 */

/* refresh
POST /token HTTP/1.1
Host: oauth2.googleapis.com
Content-Type: application/x-www-form-urlencoded

client_id=your_client_id&
client_secret=your_client_secret&
refresh_token=refresh_token&
grant_type=refresh_token
 */

object YouTubeHTTP {

  def requestAuthCode(clientID:String,port:Int,timeout:FiniteDuration)
                     (implicit system:ActorSystem, ec:ExecutionContext):Future[AuthCode] = {
    val scope = "https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fyoutube.force-ssl"
    val redirect = "https%3A%2F%2Flocalhost%3A" + port
    /*val uri = Uri.from(
      scheme = "https", host = "accounts.google.com", path = "/o/oauth2/v2/auth",
      queryString = Some(s"client_id=$clientID&redirect_uri=$redirect&scope=$scope&response_type=code")
    )*/
    val uri = s"https://accounts.google.com/o/oauth2/v2/auth?client_id=$clientID&redirect_uri=$redirect&scope=$scope&response_type=code"

    val authHandler = system.actorOf(Props[AuthCodeHandler],"AuthHandler")

    val tcp = system.actorOf(Props(classOf[TCPConnector],authHandler),"TCPActor")
    tcp ! Bind(tcp,new InetSocketAddress("localhost", port))

    //collection of deathwatch actors for timeout purposes
    val watchers = List(system.actorOf(Props(classOf[DeathWatcher],authHandler),"AuthWatcher"),
    system.actorOf(Props(classOf[TCPConnector],tcp),"TCPWatcher"))

    browse(uri)

    system.scheduler.scheduleOnce(timeout) {
      watchers foreach (actor => actor ! Proceed)
    }

    (authHandler ? CodeRequest)(timeout).mapTo[AuthCode]
    //TODO: on success scheduler will send a poisonpill to a dead actor; removing context.stop might make the actors
    // run for much longer than necessary
  }


  private class TCPConnector(forwardTo: ActorRef)(implicit system:ActorSystem) extends Actor {
    import akka.io.Tcp

    private var actorCount:Int = 0 //just in case more than one connection is registered somehow
    //could use context.become to reject any following connections, but the first connection might not be the correct one

    def receive = {
      case c@Bind(_, _, _, _, _) =>
        IO(Tcp) ! c
        context.become(connected)
    }

    def connected: Receive = {
      case Bound(_) =>
        println("Local address bound.")
      case CommandFailed(_: Connect) =>
        println("Connection failed.")
        context.stop(self)
      case Connected(_, _) =>
        val connection = sender()
        connection ! Register(context.actorOf(Props(classOf[TCPReceiver], forwardTo), s"Receiver$actorCount"))
        actorCount += 1
    }

    private class TCPReceiver(forwardTo:ActorRef) extends Actor {
      def receive = {
        case Received(code:ByteString) =>
          val codeString = code.decodeString(Charset.forName("UTF-8"))
          if (codeString.matches("^code=[\\S]+")) {
            forwardTo ! AuthCode(codeString)
            context.parent ! PoisonPill //job done, kill everything
          } else {
            forwardTo ! ErrorCode(codeString)
            context.stop(self)
          }
        case _ =>
          println("Something went wrong.")
          context.stop(self)
      }
    }

  }


  private class AuthCodeHandler extends Actor with Stash {
    def receive = {
      case CodeRequest =>
        stash()
        context.become(awaitingAuthCode)
      case AuthCode(_) =>
        stash()
        context.become(awaitingRequest)
    }

    val awaitingRequest:Receive = {
      case CodeRequest =>
        val reqSender = sender()
        unstashAll()
        context.become({
          case code@AuthCode(_) =>
            reqSender ! code
        })
    }

    val awaitingAuthCode:Receive = {
      case code@AuthCode(_) =>
        unstashAll()
        context.become({
          case CodeRequest =>
            sender ! code
        })
    }
  }

  private class DeathWatcher(actor:ActorRef) extends Actor {
    // Deathwatch actor used for timeouts; makes sure the actor isn't dead before sending poisonpill
    context.watch(actor)

    def receive = {
      case Proceed =>
        context.unwatch(actor)
        actor ! PoisonPill
        context.stop(self)
      case Terminated(`actor`) =>
        context.become({
          case Proceed =>
            //actor already dead
            context.stop(self)
        })
    }
  }

  private def browse(uri:String):Unit = {
    if (Desktop.isDesktopSupported && Desktop.getDesktop.isSupported(Desktop.Action.BROWSE)) {
      println("Please log in via browser.")
      Desktop.getDesktop.browse(new URI(uri))
    }
  }
}

class YouTubeHTTP(authCode:String,clientID:String,apiKey:String)
                 (implicit system:ActorSystem, ec:ExecutionContext, downloadTimeout:Timeout) extends CaptionDownloader {

  private var token:String = ??? //this is probably going to need to be a VAR so that it can be refreshed every now and then

  private val internalDownloader = system.actorOf(Props(classOf[DownloaderActor]),"CaptionDownloader")

  override def format:String = "sbv"

  override def download(video:String,lang:String):Future[String] = {
    ask(internalDownloader,DownloadRequest(video,lang))(downloadTimeout).mapTo[String] //TODO: timeout?
  }


  private class DownloaderActor extends Actor {

    val ready:Receive = {
      case DownloadRequest(videoID:String,lang:String) =>
        //GET captions
        ???
    }

    def receive = ready

  }
}