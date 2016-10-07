package au.id.jazzy.erqx.engine.services

import laika.api._
import laika.parse.kramdown.Kramdown
import laika.render.{HTML, HTMLWriter}
import laika.tree.Elements._

object MarkdownProcessor {

  val renderer: HTMLWriter => RenderFunction = { out => {
    case CodeBlock(language, content, opt) =>
      out <<@ ("pre", opt) <<@ ("code", Styles(s"language-$language")) <<< content << "</code></pre>"
  }}

  private val transformer = Transform from Kramdown to HTML rendering renderer

  private val noLinkRule: RewriteRule = {
    case ExternalLink(content, _, _, _) => Some(SpanSequence(content))
  }

  private val noLinkTransformer = transformer usingRule noLinkRule

  def render: String => String =
    transformer fromString _ toString

  def renderWithoutLinks: String => String =
    noLinkTransformer fromString _ toString

}
