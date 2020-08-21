package test

import scala.language.reflectiveCalls
import org.scalatest._
import scala.io.Source
import matchers._
import flatspec._
import mcnkowski.wikicaptions.{WikiText,RealWikiText,NullWikiText}


class WikiTextSpec extends AnyFlatSpec with should.Matchers {

    def fromResource(name:String):String = {
      val src = Source.fromResource(name)
      val str = src.getLines.mkString
      src.close
      str
    }
    
    val jsonstring = fromResource("api-result-pizza.json")
    val jsonwrong = fromResource("api-result-wrong.json")
    val jsondisamb = fromResource("api-result-disamb.json")
      
    val notjson = "Random text goes here"
  
  "WikiText object" should "return a RealWikiText given a proper json file" in {
    val text = WikiText(jsonstring)
    text shouldBe a [RealWikiText]
  }
  
  it should "accept an Option[String]" in {
    val text = WikiText(Some(jsonstring))
    text shouldBe a [RealWikiText]
  }
  
  it should "return a NullWikiText when passed a None" in {
    val text = WikiText(None)
    text should be theSameInstanceAs NullWikiText
  }
  
  it should "return a NullWikiText when passed a non-JSON" in {
    val text = WikiText(notjson)
    text should be theSameInstanceAs NullWikiText
  }
  
  it should "return a RealWikiText with no contents when passed a JSON without an extract" in {
    val text = WikiText(jsonwrong)
    assert(text.getExtracts.isEmpty)
    assert(!text.isDisamb)
    text.getLinkTitle(0) should be theSameInstanceAs None
  }
  
  it should "recognise a disambiguation page" in {
    val nonDisamb = WikiText(jsonstring)
    val disamb = WikiText(jsondisamb)
    assert(!nonDisamb.isDisamb)
    assert(disamb.isDisamb)
  }
  
}