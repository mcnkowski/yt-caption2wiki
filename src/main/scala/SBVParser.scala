package mcnkowski.wikicaptions

class SBVParser extends CaptionParser {
  private val pattern = "\\d{1,}:\\d{2}:\\d{2}.\\d{3},\\d{1,}:\\d{2}:\\d{2}.\\d{3}"

  def parse(input:String):String = {
    val str = input.replaceAll(pattern,"").trim()
    str.replaceAll("\n+"," ")
  }
}