package mcnkowski.wikicaptions

import scala.collection.mutable.Set
import java.net.{URL,HttpURLConnection}
import scala.io.Source
import scala.util.{Try,Using,Success,Failure}
import java.net.URLEncoder 

/*
Class used to make MediaWiki API calls to retrieve Wikipedia article contents
Article extracts are returned in HTML and plain text format
*/

case class Article(url:String,html:String,plain:String)

object Article {
  val dropEmpty:PartialFunction[Option[Article],Article] = {
    case Some(a:Article) if (!(a.html.isEmpty || a.plain.isEmpty || a.url.isEmpty)) => a
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

class MediaWiki(disamb:Disambiguation = IGNORE) {

  private val htmlCall = """https://en.wikipedia.org/w/api.php?action=query&format=json&prop=extracts|pageprops|links&exlimit=1&redirects=&titles="""
  private val plainCall = """https://en.wikipedia.org/w/api.php?action=query&format=json&prop=extracts&exlimit=1&redirects=&explaintext=&exsectionformat=plain&titles="""
  private val wiki = """https://en.wikipedia.org/wiki/"""
   
  private var connectionTimeOut:Int = 5000
  private var readTimeOut:Int = 5000
  
  def fetch(word:String):Option[Article] = {
    val title = URLEncoder.encode(word,"UTF-8") //if the title isn't encoded it might fail to make a GET call
    val html = get(htmlCall+title)
    val plain = get(plainCall+title)

    //handle disambiguation pages
      disamb match {
        case IGNORE =>
          val extracts = html.getExtracts zip plain.getExtracts
          extracts.map(ext => Article(wiki+title,ext._1,ext._2))
        
        case SKIP =>
          if(html.isDisamb) {
            None
          } else {
            val extracts = html.getExtracts zip plain.getExtracts
            extracts.map(ext => Article(wiki+title,ext._1,ext._2))
          }
        
        case FIRST =>
          if (html.isDisamb) {
            val newtitle = URLEncoder.encode(html.getLinkTitle(0),"UTF-8") //if the article is a disambiguation page, then it should contain at least two links, so the collection shouldn't be empty
            
            //get new contents based on the title of the first article on the disambiguation page
            val newhtml = get(htmlCall+newtitle)
            val newplain = get(plainCall+newtitle)
            
            val newextracts = newhtml.getExtracts zip newplain.getExtracts
            newextracts.map(ext => Article(wiki+newtitle,ext._1,ext._2))
          } else {
            val extracts = html.getExtracts zip plain.getExtracts
            extracts.map(ext => Article(wiki+title,ext._1,ext._2))
        }
      }
  }
  
  //make a single MediaWiki API call
  private def get(call:String):WikiText = {
    val content:Try[String] =
      Using(connect(call).getInputStream()) { in =>
        Source.fromInputStream(in).mkString
      }
    WikiText(content.toOption)
  }
  
  def connectTimeout(time:Int):Unit = connectionTimeOut = time
  def connectTimeout:Int = connectionTimeOut
  
  def readTimeout(time:Int):Unit = readTimeOut = time
  def readTimeout:Int = readTimeOut
  
  
  private def connect(url:String):HttpURLConnection = {
    val connection = (new URL(url)).openConnection.asInstanceOf[HttpURLConnection]
    connection.setConnectTimeout(connectionTimeOut)
    connection.setReadTimeout(readTimeOut)
    connection.setRequestMethod("GET")
    connection
  }
  
}