package mcnkowski.wikicaptions

import scala.xml.XML
import scala.util.Try
import javax.xml.parsers.SAXParserFactory

class TimedTextParser extends CaptionParser {
  private val spf = SAXParserFactory.newInstance()
  spf.setFeature("http://xml.org/sax/features/external-general-entities", false)
  spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
  private val saxParser = spf.newSAXParser()
  
  def parse (input:String):String = {
    Try {
      val xml = XML.withSAXParser(saxParser).loadString(input)
      (xml \ "text").map(text => text.text).toSeq.mkString(" ")
    }.getOrElse("").replaceAll("\n+"," ")
  }

}