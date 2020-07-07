package mcnkowski.wikicaptions

import com.google.api.client.http.{HttpRequest,HttpRequestInitializer}
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.{YouTube,YouTubeRequestInitializer}
import com.google.api.services.youtube.model.{Caption,CaptionListResponse}
import com.google.api.client.auth.oauth2.Credential
import java.io.InputStream
import scala.jdk.CollectionConverters._
import scala.io.Source
import collection.mutable.Buffer
import scala.util.{Try,Using}

object YouTubeV3 {
  private val factory = JacksonFactory.getDefaultInstance()
  private val httptransport = GoogleNetHttpTransport.newTrustedTransport()
  private val scopes = java.util.Arrays.asList("https://www.googleapis.com/auth/youtube.force-ssl")

  //get a YouTube API credential based on client secret
  @throws[java.io.IOException]
  private def authorize(httpTr:NetHttpTransport,secret:String):Credential = {
    val in = Source.fromFile(secret)
    val clientsecrets:GoogleClientSecrets = GoogleClientSecrets
      .load(factory,in.reader())
    val flow:GoogleAuthorizationCodeFlow = new GoogleAuthorizationCodeFlow
      .Builder(httpTr,factory,clientsecrets,scopes).build()
    in.close()
    new AuthorizationCodeInstalledApp(flow,new LocalServerReceiver()).authorize("test user")
  }
  
  @throws[java.io.IOException]
  def getService(secret:String):YouTube = {
    val credential = authorize(httptransport,secret)
    new YouTube.Builder(httptransport,factory,credential).setApplicationName("test").build()
  }
  
  @throws[java.io.IOException]
  def apply(secret:String):YouTubeV3 = {
    new YouTubeV3(getService(secret))
  }
}

class YouTubeV3(_service:YouTube) extends CaptionDownloader {

  override val format = "sbv"

  @throws[java.io.IOException]
  override def download(video:String,lang:String):Option[String] = {
      
      //retrieve a list of all available captions; convert to scala object
      val captionResponse = _service.captions().list("snippet",video).execute() //throws IOException
      val captionList:Buffer[Caption] = captionResponse.getItems().asScala
      
      //find captions that match given language
      val captionOption = captionList.find(_.getSnippet().getLanguage() == lang)
      captionOption match {
        case Some(cap) => 
          //if found captions make a download object using caption ID; set caption format to sbv
          val download_request = _service.captions().download(cap.getId()) //throws IOException
          download_request.setTfmt(format)
          download_request.getMediaHttpDownloader()
          
          val download_result:Try[String] = 
            Using(download_request.executeMediaAsInputStream()) { stream =>
              Source.fromInputStream(stream).mkString
            }
          download_result.toOption
        case None => 
          //didn't find captions
          println("No captions were retrieved for video "+video)
          None
      }
  }
  
  def service:YouTube = _service
}