package au.id.jazzy.erqx.engine.services

import org.specs2.mutable.Specification

import scala.xml.{Node, Text}

class MarkdownProcessorSpec extends Specification {

  import MarkdownProcessor._

  "A markdown processor" should {

    "render auto links" in {
      val md = """foo http://www.bar.com baz"""
      renderWithoutLinks(md) === "<p>foo http://www.bar.com baz</p>"
    }

    "render explicit links" in {
      val md = """foo [bar](http://www.bar.com) baz"""
      renderWithoutLinks(md) === "<p>foo bar baz</p>"
    }

    "parse Kramdown CSS classes" in {
      val md =
        """{:.foo.bar}
          |hello world""".stripMargin

      render(md) ===
        """<p class="foo bar">
          |hello world</p>""".stripMargin
    }

  }

}
