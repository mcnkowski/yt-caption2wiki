package mcnkowski.wikicaptions

import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.io.{File,PrintWriter}
import scala.util.Using

case class DownloadContent (videoId:String,captions:YTCaptions,articles:Seq[Article])
case class YTCaptions(raw:String,plain:String)

object Save2Json {
  implicit val capWrites:Writes[YTCaptions] = (
    (JsPath \ "raw").write[String] and
    (JsPath \ "plain").write[String]
  )(unlift(YTCaptions.unapply))

  implicit val articleWrites:Writes[Article] = (
    (JsPath \ "url").write[String] and
    (JsPath \ "html").write[String] and
    (JsPath \ "plain").write[String]
  )(unlift(Article.unapply))

  implicit val contentWrites:Writes[DownloadContent] = (
    (JsPath \ "videoId").write[String] and
    (JsPath \ "captions").write[YTCaptions] and
    (JsPath \ "wiki_articles").write[Seq[Article]]
  )(unlift(DownloadContent.unapply))
  
  def apply(videoId:String,raw:String,plain:String,articles:Seq[Article]):JsValue = {
    val content = DownloadContent(videoId,YTCaptions(raw,plain),articles)
    Json.toJson(content)
  }
  
  def apply(content:DownloadContent):JsValue = {
    Json.toJson(content)
  }
  
  def writeTo(path:String,json:JsValue):Unit = {
    Using(new PrintWriter(new File(path))) { writer =>
      writer.write(Json.stringify(json))
    }
  }
}