package migration

import java.nio.file.Path

import eie.io._
import zio.{Task, ZIO}

import scala.xml.XML

object ValidateXml {

  def apply(dataDir: Path): ZIO[Any, Throwable, Seq[String]] = {
    for {
      files <- Task.effect(dataDir.children.filter(_.isFile))
      lintErrorsFork <- Task.foreach(files.toSeq) { file =>
        Task
          .effect(XML.loadFile(file.toFile))
          .either
          .map {
            case Left(err) => Some(file -> err.getMessage)
            case _ => None
          }
          .fork
      }
      lintErrors: Seq[Option[(Path, String)]] <- Task.foreach(lintErrorsFork)(_.join)
    } yield {
      lintErrors.collect {
        case Some((path, msg)) => s"${dataDir.relativize(path)} : $msg"
      }
    }
  }
}
