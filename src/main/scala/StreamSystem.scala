package mcnkowski.wikicaptions

import play.api.libs.json.Json
import java.nio.file.{Paths,StandardOpenOption}
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.{NotUsed,Done}


class StreamSystem(dl:CaptionDownloader,p:CaptionParser,wiki:MediaWiki,ext:NounExtractor) {
  
  val encoding: Charset = Charset.forName("UTF-8")
  
  val idToCaps:Flow[String,YTCaptions,NotUsed] = 
    Flow[String].mapAsync(2)(downloadAndParse)

  val capsToWords:Flow[YTCaptions,Seq[String],NotUsed] = 
    Flow[YTCaptions].map(caps => ext.extractNounsFrom(caps.plain))

  val capsToWiki:Flow[YTCaptions,Seq[Article],NotUsed] =
    capsToWords.via(Flow[Seq[String]].mapAsync(4)(seq => Future.sequence(seq.map(wiki.fetch))))
    .map(_.collect(Article.nonEmpty))

  val byteStringify:Flow[DownloadContent,ByteString,NotUsed] =
    Flow[DownloadContent].map(x => ByteString(Json.stringify(Save2Json(x))))

  def graph(srcFile:String,destPath:String):RunnableGraph[Future[Done]] = {

    val sink = Sink.foreach[DownloadContent]( result => {
      val content = ByteString(Json.stringify(Save2Json(result)))
      val channel = FileChannel.open(Paths.get(destPath + result.videoId + ".json"),
        StandardOpenOption.CREATE,StandardOpenOption.WRITE,StandardOpenOption.TRUNCATE_EXISTING)
      
      channel.write(content.asByteBuffer)
      channel.close()
    })
    
    RunnableGraph.fromGraph(GraphDSL.create(sink) { implicit b:GraphDSL.Builder[Future[Done]] => sink =>
      import GraphDSL.Implicits._
    
      val source = FileIO.fromPath(Paths.get(srcFile))
        .via(Framing.delimiter(ByteString(" "),maximumFrameLength = 11, allowTruncation = true))
        .map(_.decodeString(encoding))
        .filter(_.matches("[A-Za-z0-9]{11}"))
    
    
      val bcastID = b.add(Broadcast[String](2))
      val bcastCaps = b.add(Broadcast[YTCaptions](2))
      val zip = b.add(ZipWith[String,YTCaptions,Seq[Article],DownloadContent](DownloadContent))
    
    //TODO: can this be done with FlowWithContext
    source ~> bcastID ~> idToCaps ~> bcastCaps ~> capsToWiki ~> zip.in2
              bcastID ~> zip.in0
                                     bcastCaps ~> zip.in1
    
      zip.out ~>  sink
    
      ClosedShape
    })
  }

  private def downloadAndParse(videoId:String):Future[YTCaptions] = {
    dl.download(videoId,"en") map { raw =>
      val plain = p.parse(raw)
      YTCaptions(raw, plain)
    }
  }
}