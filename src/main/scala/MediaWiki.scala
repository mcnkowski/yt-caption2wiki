package mcnkowski.wikicaptions

import scala.concurrent.Future
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshal,Unmarshaller}
import java.net.URLEncoder

/*
Class used to make MediaWiki API calls to retrieve Wikipedia article contents
Article extracts are returned in HTML and plain text format
*/

case class Article(url:String,html:String,plain:String)

object Article {
  val nonEmpty:PartialFunction[Option[Article],Article] = {
    case Some(a:Article) if a.html.nonEmpty && a.plain.nonEmpty && a.url.nonEmpty => a
  }
}

//Used to check what should be done if a disambiguation page was retrieved
//IGNORE - Treat it as a regular page
//SKIP - Discard it
//FIRST - Get the first article listed on the disambiguation page
sealed trait Disambiguation
case object IGNORE extends Disambiguation
case object SKIP extends Disambiguation
case object FIRST extends Disambiguation

class MediaWiki(disamb:Disambiguation = IGNORE)(implicit system:akka.actor.ClassicActorSystemProvider, executionContext:scala.concurrent.ExecutionContext) {

  implicit val toWikiText:Unmarshaller[HttpEntity,WikiText] = Unmarshaller.stringUnmarshaller.map {
    string => WikiText(string)
  }

  private val htmlCall = """https://en.wikipedia.org/w/api.php?action=query&format=json&prop=extracts|pageprops|links&exlimit=1&redirects=&titles="""
  private val plainCall = """https://en.wikipedia.org/w/api.php?action=query&format=json&prop=extracts&exlimit=1&redirects=&explaintext=&exsectionformat=plain&titles="""
  private val wiki = """https://en.wikipedia.org/wiki/"""
   
  //private var connectionTimeOut:Int = 5000
  //private var readTimeOut:Int = 5000
  
  def fetch(word:String):Future[Option[Article]] = {
    val title = URLEncoder.encode(word,"UTF-8") //if the title isn't encoded it might fail to make a GET call
    val requests = get(htmlCall+title).zip(get(plainCall+title))

    requests.flatMap { case (html, plain) => //had to use flatMap for the repeated GET call in case FIRST

      //handle disambiguation pages
      disamb match {
        case IGNORE =>
          Future((html.getExtracts zip plain.getExtracts) map { ext => Article(wiki + title, ext._1, ext._2) })

        case SKIP =>
          if (html.isDisamb) {
            Future.successful(None)
          } else {
            Future((html.getExtracts zip plain.getExtracts) map { ext => Article(wiki + title, ext._1, ext._2) })
          }

        case FIRST =>
          if (html.isDisamb) {
            html.getLinkTitle(0).fold(Future[Option[Article]](None)){ title =>
              val newtitle = URLEncoder.encode(title, "UTF-8")

              get(htmlCall + newtitle).zipWith(get(plainCall + newtitle)) { case (html, plain) =>
                (html.getExtracts zip plain.getExtracts).map(ext => Article(wiki + newtitle, ext._1, ext._2))
              }
            }
          } else {
            Future((html.getExtracts zip plain.getExtracts).map(ext => Article(wiki + title, ext._1, ext._2)))
          }
      }
    }
  }

  private def get(call:String):Future[WikiText] = {
    Get(call).flatMap(response => Unmarshal(response.entity).to[WikiText])
  }
}