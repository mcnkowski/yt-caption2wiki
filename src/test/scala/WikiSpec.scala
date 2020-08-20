package test

import java.io.File
import org.scalatest._
import matchers._
import flatspec._
import scala.language.reflectiveCalls
import mcnkowski.wikicaptions.{MediaWiki,Article,IGNORE,SKIP,FIRST}
import java.net.URLEncoder 

//OUTDATED

class WikiSpec extends AnyFlatSpec with should.Matchers with BeforeAndAfterAll{
  /*
  def fixture = new {
    val wiki = new MediaWiki
  }
  */
  /*"MediaWiki" should "return a non-empty Article object" in {
    val wiki = fixture.wiki
    val article = wiki.fetch("pizza")
    assert(article.isDefined)
  }
  
  it should "return an empty object if requested article doesn't exist" in {
    val wiki = fixture.wiki
    val article = wiki.fetch("asdfb")
    assert(!article.isDefined)
  }
  
  it should "automatically convert requested titles to UTF-8" in {
    val wiki = fixture.wiki
    val article = wiki.fetch("Günther")
    assertResult("https://en.wikipedia.org/wiki/G%C3%BCnther"){
      article.url
    }
  }
  
  it should "differentiate between a regular page and a disambiguation page" in {
    val wiki = fixture.wiki
    wiki.disambiguation(SKIP)
    assert(wiki.fetch("pizza").isDefined != wiki.fetch("transform").isDefined)
  }
  
  
  it should "use the specified disambiguation handling" in {
    val wiki = fixture.wiki
    val word = "Transform"
    wiki.disambiguation(FIRST)
    assertResult("https://en.wikipedia.org/wiki/Integral+transform"){
      wiki.fetch(word).url
    }
    
    wiki.disambiguation(SKIP)
    assert(!wiki.fetch(word).isDefined)
    
    wiki.disambiguation(IGNORE)
    assertResult("https://en.wikipedia.org/wiki/Transform"){
      wiki.fetch(word).url
    }
    
  }
  
  it should "use the specified disambiguation handling (continued)" in {
    val wiki = fixture.wiki
    val word = "Transform"
    
    val artIgnore = wiki.fetch(word)
    
    wiki.disambiguation(FIRST)
    val artFirst = wiki.fetch(word)
    
    assert(artIgnore.isDefined)
    assert(artFirst.isDefined)
    assert(artIgnore.html != artFirst.html && artIgnore.plain != artFirst.plain)
  }
  */
}