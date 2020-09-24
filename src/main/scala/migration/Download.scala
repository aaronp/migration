package migration

import java.nio.file.Path

import eie.io._
import geny.Bytes
import zio.console.{Console, putStrLn}
import zio.{Task, ZIO}

import scala.util.Try

object Download {

  /**
    *
    * @param url      the index list URL (a url containing a plain text, newline separated list of files to download
    * @param dest     the target directory
    * @param settings the http settings
    * @return a list of file names from the given url
    */
  def indexFile(
      url: String,
      dest: Path,
      settings: HttpRequestSettings): ZIO[Console, Throwable, List[String]] = {
    for {
      _ <- toFile(url, dest, settings)
      fileList <- Task.effect(
        dest.text.linesIterator.map(_.trim).filterNot(_.isEmpty).toList)
    } yield fileList
  }

  def toFile(url: String,
             dest: Path,
             settings: HttpRequestSettings): ZIO[Console, Throwable, Path] = {
    plan(Download(url, dest, settings))
      .flatMap { actions =>
        ZIO.foreach(actions)(eval)
      }
      .map(_ => dest)
  }

  private sealed trait Action

  private case class MkDir(dest: Path) extends Action

  private case class Download(url: String,
                              dest: Path,
                              settings: HttpRequestSettings)
      extends Action

  private case class Log(message: String) extends Action

  private case class Fail(msg: String) extends Exception(msg) with Action

  private def eval(action: Action): ZIO[Console, Throwable, Any] =
    action match {
      case MkDir(dir) =>
        putStrLn(s"mkdir -p ${dir}") *> Task.effect(dir.mkDirs())
      case error @ Fail(_) =>
        putStrLn(s"download failed with: ${error.msg}") *> Task.fail(error)
      case Log(msg)                     => zio.console.putStrLn(msg)
      case download @ Download(_, _, _) => asTask(download)
    }

  private def write(data: Bytes, dest: Path): Task[Path] = {
    val array = data.array
    if (array.isEmpty) {
      Task.fail(new IllegalStateException(s"No data read, won't save to $dest"))
    } else {
      Task.effect(dest.bytes = array)
    }
  }

  private def asTask(download: Download): ZIO[Any, Throwable, Path] = {
    import download._
    Task
      .effect(
        requests.get(
          url,
          readTimeout = settings.readTimeout.toMillis.toInt,
          connectTimeout = settings.connectionTimeout.toMillis.toInt))
      .flatMap { r =>
        if (settings.acceptableStatuses.isEmpty || settings.acceptableStatuses
              .contains(r.statusCode)) {
          write(r.data, dest)
        } else {
          val body =
            Try("; body:" + new String(r.bytes).take(200)).getOrElse("")
          val err = new IllegalStateException(
            s"Download '$url' returned unexpected status code '${r.statusCode}' ${r.statusMessage}${body}")
          Task.fail(err)
        }
      }
  }

  /**
    * This was initially done this way to produce a list of actions which could
    * then just be evaluated by doing actual IO or just logging for a 'dry run' scenario.
    *
    * In practice doing a 'dry run' on downloads was as useful as the number 9 on a microwave.
    *
    * @param download the download to download. Download.
    * @return a list of actions to perform for a given download
    */
  private def plan(download: Download): Task[Seq[Action]] = {
    import download._
    Task.effect {
      if (dest.exists()) {
        if (dest.isFile) {
          Log(s"# ${dest} already exists, skipping download") :: Nil
        } else {
          Fail(s"${dest} exists but is not a directory") :: Nil
        }
      } else {
        dest.parent match {
          case Some(p) if p.isDir =>
            Log(s"# downloading $url to directory $p") :: download :: Nil
          case Some(p) if p.isFile =>
            Fail(s"${dest} exists but is a file") :: Nil
          case Some(p) if !p.exists() => MkDir(p) :: download :: Nil
          case _                      => Fail(s"$dest is an invalid download path") :: Nil
        }
      }
    }
  }
}
