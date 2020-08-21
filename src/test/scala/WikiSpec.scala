package test

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import org.scalatest._
import matchers._
import flatspec._
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.language.reflectiveCalls
import mcnkowski.wikicaptions.{Article, FIRST, IGNORE, MediaWiki, SKIP}


class WikiSpec extends AnyFlatSpec with should.Matchers with BeforeAndAfterAll{
  implicit val system:ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "TestSystem")
  implicit val ec:ExecutionContext = ExecutionContext.global

  def fixture = new {
    val wikiIgnore = new MediaWiki(IGNORE)
    val wikiSkip = new MediaWiki(SKIP)
    val wikiFirst = new MediaWiki(FIRST)
  }

  override def afterAll(): Unit = {
    system.terminate()
  }

  "MediaWiki" should "return a non-empty Article object" in {
    val wiki = fixture.wikiIgnore
    val article = wiki.fetch("pizza")
    article.map(art => assert(Article.nonEmpty.isDefinedAt(art)))
  }
  
  it should "return an empty object if requested article doesn't exist" in {
    val wiki = fixture.wikiIgnore
    val article = wiki.fetch("asdfb")
    article.map(art => assert(!Article.nonEmpty.isDefinedAt(art)))
  }
  
  it should "automatically convert requested titles to UTF-8" in {
    val wiki = fixture.wikiIgnore
    val article = wiki.fetch("Günther")

    article map { art =>
      assertResult("https://en.wikipedia.org/wiki/G%C3%BCnther")(art.get.url)
    }
  }
  
  it should "differentiate between a regular page and a disambiguation page" in {
    val wiki = fixture.wikiSkip
    val articles = wiki.fetch("pizza") zip wiki.fetch("transform")

    articles map { case (art1, art2) =>
      assert(Article.nonEmpty.isDefinedAt(art1) != Article.nonEmpty.isDefinedAt(art2))
    }
  }
  
  
  it should "use the specified disambiguation handling" in {
    val word = "Transform"
    val first = fixture.wikiFirst.fetch(word)

    first map {
      article => assertResult("https://en.wikipedia.org/wiki/Integral+transform")(article.get.url)
    }

    val skip = fixture.wikiSkip.fetch(word)
    skip map {
      article => assert(!Article.nonEmpty.isDefinedAt(article))
    }

    val ignore = fixture.wikiIgnore.fetch(word)
    ignore map {
      article => assertResult("https://en.wikipedia.org/wiki/Transform")(article.get.url)
    }
  }
  
  it should "use the specified disambiguation handling (continued)" in {
    val word = "Transform"
    
    val artIgnore = fixture.wikiIgnore.fetch(word)

    val artFirst = fixture.wikiFirst.fetch(word)

    artIgnore.map(art => assert(Article.nonEmpty.isDefinedAt(art)))
    artFirst.map(art => assert(Article.nonEmpty.isDefinedAt(art)))

    (artIgnore zip artFirst) map { case (ign,first) =>
      assert(ign.get.html != first.get.html && ign.get.plain != first.get.plain)
    }
  }

}