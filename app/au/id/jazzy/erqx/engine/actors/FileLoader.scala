package au.id.jazzy.erqx.engine.actors

import akka.actor.Actor

import scala.concurrent.blocking
import play.doc.PlayDoc
import au.id.jazzy.erqx.engine.services.git.{GitFileRepository, GitRepository}
import au.id.jazzy.erqx.engine.models.{Blog, BlogPostContent}
import au.id.jazzy.erqx.engine.services.ContentParser

/**
 * Actor responsible for loading and rendering files
 */
class FileLoader(gitRepository: GitRepository) extends Actor {

  import BlogActor._

  def receive = {

    case LoadContent(blog, path) =>
      sender ! blocking(gitRepository.loadContent(blog.hash, path))

    case LoadStream(blog, path) =>
      sender ! blocking(gitRepository.loadStream(blog.hash, path).map {
        case (length, is) => FileStream(length, is, context.dispatcher)
      })

    case RenderPost(blog, post, absoluteUri) =>
      render(blog, post.path, post.format, absoluteUri)

    case RenderPage(blog, page) =>
      render(blog, page.path, page.format, None)
  }

  private def render(blog: Blog, path: String, format: String, absoluteUri: Option[String]) = {
    val content = blocking {
      gitRepository.loadContent(blog.hash, path).map { content =>
        format match {
          case "md" =>
            /*
            val repo = new GitFileRepository(gitRepository, blog.hash, None)
            val doc = new PlayDoc(repo, repo, "", "")
            val rendered = postContent.map(doc.render(_, None))
            BlogPostContent(ContentParser.replaceLinks(rendered.summary), rendered.body)
            */
            content
          case _ =>
            content
        }
      }
    }
    val uriAdjusted = absoluteUri.flatMap { uri =>
      content.map(replaceCommonUris(_, uri))
    }.orElse(content)

    sender ! uriAdjusted.map(new BlogPostContent(_))
  }

  private def replaceCommonUris(s: String, uri: String): String = {
    s.replaceAll("href=\"\\./", "href=\"" + uri)
      .replaceAll("href='\\./", "href='" + uri)
      .replaceAll("src=\"\\./", "src=\"" + uri)
      .replaceAll("src='\\./", "src='" + uri)
  }

}
