package au.id.jazzy.erqx.engine.services

import laika.api._
import laika.parse.markdown.Markdown
import laika.render.HTML
import laika.tree.Elements._

import scala.util.matching.Regex.Match

object MarkdownProcessor {

  private val cssRegex = """(?s)^\{\:((?:\.[a-zA-Z\-])+)\}(.*)$""".r

  private val f = """^(.+)$""".r
  private val cssRule: RewriteRule = {
    case Paragraph(Text(cssRegex(classes, rest), _) +: tail, paragraphOptions) =>
      println("here: " + classes)
      Some(Paragraph(Text(rest) +: tail, paragraphOptions + Styles(classes.split('.'): _*)))

  }

  private val transformer = Transform from Markdown to HTML usingRule cssRule

  private val noLinkRule: RewriteRule = {
    case ExternalLink(content, _, _, _) => Some(SpanSequence(content))
  }

  private val noLinkTransformer = transformer usingRule noLinkRule

  def render: String => String =
    transformer fromString _ toString

  def renderWithoutLinks: String => String =
    noLinkTransformer fromString _ toString

}
