package au.id.jazzy.erqx.engine.services

import java.io.{InputStream, File}
import org.eclipse.jgit.lib.{ObjectId, Constants, RepositoryBuilder}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.treewalk.filter.{PathSuffixFilter, TreeFilter, PathFilter}
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.revwalk.RevWalk
import scalax.io.Resource
import play.doc.{FileHandle, FileRepository}
import play.api.i18n.Lang
import play.api.{Logger, Play}
import play.api.Play.current
import scala.util.control.Exception._
import au.id.jazzy.erqx.engine.models._

class GitRepository(gitDir: File, pathPrefix: Option[String], branch: String, remote: Option[String]) {

  private val repository = new RepositoryBuilder().setGitDir(new File(gitDir, ".git")).build()
  private val git = new Git(repository)

  def close = repository.close()

  def fetch: Unit = remote.foreach { r =>
    git.fetch().setRemote(r).call()
  }

  def currentHash: String = Option(repository.getRef(
      remote.map("/refs/remotes/" + _ + "/").getOrElse("") + branch
    )).map(_.getObjectId.name())
    .getOrElse("")

  /**
   * Load the file with the given path in the given ref
   *
   * @param path The path to load
   * @return A tuple of the file size and its input stream, if the file was found
   */
  def loadStream(commitId: String, path: String): Option[(Long, InputStream)] = {
    scanFiles(ObjectId.fromString(commitId), PathFilter.create(pathPrefix.getOrElse("") + path)) { treeWalk =>
      if (!treeWalk.next()) {
        None
      } else {
        val file = repository.open(treeWalk.getObjectId(0))
        Some((file.getSize, file.openStream()))
      }
    }
  }

  def loadContent(commitId: String, path: String): Option[String] = {
    loadStream(commitId, path).map {
      case (length, is) => try {
        Resource.fromInputStream(is).string
      } finally {
        is.close()
      }
    }
  }

  def loadBlog(commitId: String): List[BlogPost] = {
    (listAllFilesInPath(commitId, pathPrefix.getOrElse("") + "_posts").map { files =>
      files.map { file =>
        val path = "_posts/" + file
        val Some((_, is)) = loadStream(commitId, path)
        try {
          MetaDataParser.parseFrontMatter(is, path, path.substring(path.lastIndexOf('/') + 1))
        } finally {
          is.close()
        }
      }
    } getOrElse Nil).toList
  }

  def loadConfig(commitId: String) = {
    val config = loadContent(commitId, "_config.yml").map(Yaml.parse).getOrElse(Yaml.empty)

    val theme = config.getString("theme").flatMap { themeClassName =>
      allCatch.either {
        if (themeClassName.endsWith("$"))
          Play.classloader.loadClass(themeClassName).getMethod("MODULE$").invoke(null).asInstanceOf[BlogTheme]
        else
          Play.classloader.loadClass(themeClassName).newInstance().asInstanceOf[BlogTheme]
      } match {
        case Right(t) => Some(t)
        case Left(e) =>
          Logger.warn("Unable to load theme", e)
          None
      }
    } getOrElse DefaultTheme

    BlogInfo(
      title = config.getString("title").getOrElse("A blog with no name"),
      subTitle = config.getString("subTitle"),
      author = config.getString("author").getOrElse("No one"),
      language = config.getString("language").getOrElse(Lang.defaultLang.code),
      description = config.getString("description"),
      theme = theme,
      properties = config
    )
  }

  // A tree filter that finds files with the given name under the given base path
  private class FileWithNameFilter(basePath: String, name: String) extends TreeFilter {
    val pathRaw = Constants.encode(basePath)

    // Due to this bug: https://bugs.eclipse.org/bugs/show_bug.cgi?id=411999
    // we have to supply a byte array that has one dummy byte at the front of it.
    // As soon as that bug is fixed, this code will break, just remove the #.
    val nameRaw = Constants.encode("#/" + name)

    def shouldBeRecursive() = false

    def include(walker: TreeWalk) = {
      // The way include works is if it's a subtree (directory), then we return true if we want to descend into it,
      // and if it's not, then we return true if the file is the one we want.
      walker.isPathPrefix(pathRaw, pathRaw.length) == 0 &&
        (walker.isSubtree || walker.isPathSuffix(nameRaw, nameRaw.length))
    }

    override def clone() = this
  }

  def findFileWithName(commitId: String, basePath: Option[String], name: String): Option[String] = {
    scanFiles(ObjectId.fromString(commitId),
      basePath.map(new FileWithNameFilter(_, name)).getOrElse(PathSuffixFilter.create("#/" + name))
    ) { treeWalk =>
      if (!treeWalk.next()) {
        None
      } else {
        Some(treeWalk.getPathString.drop(basePath.map(_.length + 1).getOrElse(0)))
      }
    }
  }

  def listAllFilesInPath(commitId: String, path: String): Option[Seq[String]] = {
    scanFiles(ObjectId.fromString(commitId), PathFilter.create(path)) { treeWalk =>
      def extract(list: List[String]): List[String] = {
        if (!treeWalk.next()) {
          list
        } else {
          extract(treeWalk.getPathString.drop(path.length + 1) :: list)
        }
      }
      Some(extract(Nil))
    }
  }

  private def scanFiles[T](commitId: ObjectId, filter: TreeFilter)(block: TreeWalk => Option[T]): Option[T] = {
    // Now find the tree for that commit id
    val revWalk = new RevWalk(repository)
    try {
      val tree = revWalk.parseCommit(commitId).getTree
      // And walk it
      val treeWalk = new TreeWalk(repository)
      try {
        treeWalk.addTree(tree)
        treeWalk.setRecursive(true)
        treeWalk.setFilter(filter)
        block(treeWalk)
      } finally {
        treeWalk.release()
      }
    } finally {
      revWalk.dispose()
    }
  }
}

class GitFileRepository(gitRepository: GitRepository, commitId: String, base: Option[String]) extends FileRepository {
  def loadFile[A](path: String)(loader: (InputStream) => A) = {
    gitRepository.loadStream(commitId, base.map(_ + "/" + path).getOrElse(path)).map { file =>
      try {
        loader(file._2)
      } finally {
        file._2.close()
      }
    }
  }

  def findFileWithName(name: String) = gitRepository.findFileWithName(commitId, base, name)

  def handleFile[A](path: String)(handler: (FileHandle) => A) = {
    gitRepository.loadStream(commitId, base.map(_ + "/" + path).getOrElse(path)).map {
      case (length, is) => handler(FileHandle(path.drop(path.lastIndexOf('/') + 1), length, is, () => is.close()))
    }
  }
}
