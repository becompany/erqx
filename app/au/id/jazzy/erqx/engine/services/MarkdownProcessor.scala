package au.id.jazzy.erqx.engine.services

import laika.api._
import laika.parse.markdown.Markdown
import laika.render.HTML
import laika.tree.Elements.{ExternalLink, SpanSequence}

object MarkdownProcessor {

  val transformer = Transform from Markdown to HTML

  val noLinkTransformer = transformer usingRule {
    case ExternalLink(content, _, _, _) => Some(SpanSequence(content))
  }

  def render: String => String =
    transformer fromString _ toString

  def renderWithoutLinks: String => String =
    noLinkTransformer fromString _ toString

}
