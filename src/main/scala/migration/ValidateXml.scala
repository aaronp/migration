package migration

import java.nio.file.Path

import zio.{Task, ZIO}

import scala.xml.XML
import eie.io._

object ValidateXml {

  def apply(dataDir: Path): ZIO[Any, Throwable, Seq[String]] = {
    for {
      files <- Task.effect(dataDir.children.filter(_.isFile))
      lintErrorsFork <- Task.foreach(files.toSeq) { file =>
        Task
          .effect(XML.loadFile(file.toFile))
          .either
          .map {
            case Left(err) =>
              println(s"${Thread.currentThread().getName} : $file is fucked")
              Some(file -> err.getMessage)
            case _ =>
//            println(s"${Thread.currentThread().getName} : $file ok")
              None
          }
          .fork
      }
      lintErrors: Seq[Option[(Path, String)]] <- Task.foreach(lintErrorsFork) {
        fiber =>
          fiber.join
      }
    } yield {
      lintErrors.collect {
        case Some((path, msg)) => s"$path : $msg"
      }
    }
  }
}
