package mcnkowski.wikicaptions

import java.io.FileInputStream
import scala.util.Using
import opennlp.tools.tokenize.Tokenizer
import opennlp.tools.postag.POSModel
import opennlp.tools.postag.POSTagger
import opennlp.tools.postag.POSTaggerME
import opennlp.tools.tokenize.SimpleTokenizer

object NounExtractor {
  def defaultTokenizer:SimpleTokenizer = {
    SimpleTokenizer.INSTANCE
  }
  
  @throws[java.io.FileNotFoundException]
  @throws[NoSuchElementException]
  def posTagger(path:String):POSTaggerME = {
    val posmodel =
      Using(new FileInputStream(path)) { model =>
        new POSModel(model)
    }
    new POSTaggerME(posmodel.get)
  }
  
  @throws[java.io.FileNotFoundException]
  @throws[NoSuchElementException]
  def apply(path:String):NounExtractor = {
    val tokenizer = this.defaultTokenizer
    val tagger = this.posTagger(path)
    new NounExtractor(tokenizer,tagger)
  }
}

class NounExtractor(tokenizer:Tokenizer,tagger:POSTagger) {

  def tokenize(text:String):Array[String] = {
    tokenizer.tokenize(text)
  }
  
  def tag(tokens:Array[String]):Seq[String] = {
    tagger.tag(tokens).toIndexedSeq
  }
  
  def extractNounsFrom(text:String):Set[String] = {
    val tokens = tokenize(text)
    val tags = tag(tokens)
    
    //get words tagged NN, NNS, NNP, NNPS
    val taggedNouns = tags.zip(tokens)
      .filter { case (tag,_) => tag.startsWith("NN") }
    
    taggedNouns.map(_._2).iterator.toSet
  }
}