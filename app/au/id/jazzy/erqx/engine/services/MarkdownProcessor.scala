package au.id.jazzy.erqx.engine.services

import laika.api._
import laika.parse.kramdown.Kramdown
import laika.render.HTML
import laika.tree.Elements._

object MarkdownProcessor {

  private val cssRegex = """(?s)^\{\:\.((?:\.?[a-zA-Z\-]+)+)\}(.*)$""".r

  private val cssRule: RewriteRule = {
    case Paragraph(Text(cssRegex(classes, text), _) +: tail, paragraphOptions) =>
      Some(Paragraph(Text(text) +: tail, paragraphOptions + Styles(classes.split('.'): _*)))
  }

  private val transformer = Transform from Kramdown to HTML usingRule cssRule

  private val noLinkRule: RewriteRule = {
    case ExternalLink(content, _, _, _) => Some(SpanSequence(content))
  }

  private val noLinkTransformer = transformer usingRule noLinkRule

  def render: String => String =
    transformer fromString _ toString

  def renderWithoutLinks: String => String =
    noLinkTransformer fromString _ toString

}
