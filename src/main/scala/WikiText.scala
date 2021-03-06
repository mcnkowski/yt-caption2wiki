package mcnkowski.wikicaptions

import play.api.libs.json.Json
import play.api.libs.json.JsValue
import scala.util.{Try,Success,Failure}

//TODO: Parser can't parse an empty string. Make a Null Object instead

object WikiText {
  def apply(option:Option[String]):WikiText = {
    this(option.getOrElse(""))
  }
  def apply(string:String):WikiText = {
    //If string can be parsed into a JsValue return RealWikiText; otherwise NullWikiText
    val json = Try{Json.parse(string)}
    json match {
      case Success(value) => new RealWikiText(value)
      case Failure(_) => NullWikiText
    }
  }
}

trait WikiText{
  
  def getExtracts:Option[String]
  
  def getLinkTitle(n:Int):Option[String]
  
  def isDisamb:Boolean 
}

//non-null WikiText object
//can be empty if the supplied JSON is not malformatted but doesn't contain expected fields
class RealWikiText(json:JsValue) extends WikiText {

  override def getExtracts:Option[String] = {  
    val extracts = (json \ "query" \ "pages" \\ "extract")
    if (extracts.nonEmpty) {
      Some(extracts.head.as[String])
    } else {
      None
    }
  }

  override def getLinkTitle(n:Int):Option[String] = {
    Try {
      val jsonseq = (json \ "query" \ "pages" \\ "links")
      (jsonseq.head \ n \ "title").get.as[String]
    }.toOption
  }
  
  override def isDisamb:Boolean = {
    val disamb = (json \\ "disambiguation")
    if (disamb.nonEmpty) {
      true
    } else {
      false
    }
  }
}

//empty WikiText object
//used when JsValue can't be created out of the supplied string
object NullWikiText extends WikiText {
  
  override def getExtracts:Option[String] ={
    None
  }
  
  override def getLinkTitle(n:Int):Option[String] = {
    None
  }
  
  override def isDisamb:Boolean = false
}
