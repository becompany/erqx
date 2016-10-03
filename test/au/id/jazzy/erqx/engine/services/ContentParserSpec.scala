package au.id.jazzy.erqx.engine.services

import au.id.jazzy.erqx.engine.models.BlogPostContent
import org.specs2.mutable.Specification

class ContentParserSpec extends Specification {

  import ContentParser._

  "BlogUtil" should {

    "split content" in {

      val text =
        """ ---
          |
          |Front matter
          | ---
          |Summary
          |READMORE
          |Body
          |""".stripMargin

      val content = splitAt(List("---", "---", "READMORE"))(text)
      content === List("", "Front matter", "Summary", "Body")
    }

  }

}
