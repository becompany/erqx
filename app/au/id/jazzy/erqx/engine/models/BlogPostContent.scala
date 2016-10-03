package au.id.jazzy.erqx.engine.models

class BlogPostContent(text: String) {

  import au.id.jazzy.erqx.engine.services.MarkdownProcessor._
  import au.id.jazzy.erqx.engine.services.ContentParser._
  val splitContent = splitAt(List("---", "---", "READMORE")) _

  private lazy val parts = splitContent(text)

  private lazy val summaryMarkdown: String = parts(2)
  private lazy val bodyMarkdown: String = parts(3)

  lazy val summary = renderWithoutLinks(summaryMarkdown)
  lazy val lead = render(summaryMarkdown)
  lazy val body = render(bodyMarkdown)

}
