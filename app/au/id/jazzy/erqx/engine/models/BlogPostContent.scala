package au.id.jazzy.erqx.engine.models

class BlogPostContent(text: String) {

  import au.id.jazzy.erqx.engine.services.MarkdownProcessor._
  import au.id.jazzy.erqx.engine.services.ContentParser._
  val splitContent = splitAt(List("---", "---", "READMORE")) _

  private lazy val parts = splitContent(text)

  private lazy val summary: String = parts(2)
  private lazy val body: String = parts(3)

  val summaryHtml = renderWithoutLinks(summary)
  val bodyHtml = render(summary + body)

}
