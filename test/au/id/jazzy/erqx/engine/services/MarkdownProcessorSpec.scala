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

    "apply CSS classes" in {
      val md =
        """{:.foo.bar}
          |hello world""".stripMargin

      render(md) ===
        """<p class="foo bar">
          |hello world</p>""".stripMargin
    }

    "xxx" in {

      case class Foo(a: String)
      implicit def fooToNode(foo: Foo): Node = Text(s"[${foo.a}]")
      val foo = Foo("x")
      println(<p>{foo}</p>)
      1 === 1
    }

  }

}
