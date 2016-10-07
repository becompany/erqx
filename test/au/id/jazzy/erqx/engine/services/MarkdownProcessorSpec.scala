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
        """<p class="foo bar">hello world</p>""".stripMargin
    }

    "render code blocks without language" in {
      val md =
        """~~~~
          |code
          |~~~~""".stripMargin
      render(md) ===
        """<pre><code>code</code></pre>"""
    }

    "render code blocks with language" in {
      val md =
        """~~~~ scala
          |code
          |~~~~""".stripMargin
      render(md) ===
         """<pre><code class="language-scala">code</code></pre>"""
    }

  }

}
