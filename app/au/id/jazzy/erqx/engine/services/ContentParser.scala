package au.id.jazzy.erqx.engine.services

import au.id.jazzy.erqx.engine.models.BlogPostContent

object ContentParser {

  private def toText(lines: Array[String]): String =
    lines.mkString("\n").trim

  private def splitLinesAt(delimiters: List[String])(lines: Array[String]): List[String] =
    delimiters match {
      case Nil => List(toText(lines))
      case head :: tail =>
        lines.indexWhere(_.trim == head) match {
          case -1 => List(toText(lines))
          case pos =>
            val (current, next) = lines.splitAt(pos)
            toText(current) :: splitLinesAt(tail)(next.drop(1))
        }
    }

  def splitAt(delimiters: List[String])(text: String): List[String] =
    splitLinesAt(delimiters)(text.split("\n"))

  /*
  private val linkRegex = "(<a(\\s+.*)?>)(.*)(<\\/a>)".r

  def replaceLinks(s: String): String
    = linkRegex.replaceAllIn(s, _.group(3))
    */

}
