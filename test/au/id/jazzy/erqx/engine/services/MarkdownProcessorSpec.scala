package au.id.jazzy.erqx.engine.services

import org.specs2.mutable.Specification

class MarkdownProcessorSpec extends Specification {

  import MarkdownProcessor._

  "A markdown processor" should {

    "render auto links" in {
      val md = """foo http://www.bar.com baz"""
      val html = renderWithoutLinks(md)
      html === "<p>foo http://www.bar.com baz</p>"
    }

    "render explicit links" in {
      val md = """foo [bar](http://www.bar.com) baz"""
      val html = renderWithoutLinks(md)
      html === "<p>foo bar baz</p>"
    }

  }

}
