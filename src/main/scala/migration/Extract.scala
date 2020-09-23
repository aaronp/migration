package migration

import java.nio.file.Path

import eie.io._
import zio.console.{Console, putStrLn}
import zio.{Task, URIO, ZIO}

import scala.util.Try

object Extract {

  /**
   * The business logic of extracting/validating zip files
   *
   * @param config
   * @return
   */
  def apply(config: ParsedConfig): ZIO[Console, Throwable, Unit] = {
    import config._
    val fileNameForEntry = {
      // throw exceptions for bad regex replacements
      RegexResolve(config.fileNameRegex.trim, config.fileNamePattern.trim)
    }

    for {
      zipFilesNames <- Download.indexFile(indexURL,
        targetDirectory.resolve("index.txt"))
      forks <- ZIO.foreach(zipFilesNames.zipWithIndex) {
        case (zipEntry, index) =>
          process(config.url,
            targetDirectory,
            index,
            zipEntry,
            checkDownloadStatus,
            fileNameForEntry).fork
      }
      _ <- ZIO.foreach(forks.map(_.join))(identity)
    } yield ()
  }
  case class UnzipResult(zipFile: Path, dataDir: Path, errors: Seq[String])

  def process(url: String,
              targetDirectory: Path,
              index: Int,
              zipFileName: String,
              acceptableStatusCodes: Set[Int],
              fileNameForEntry: String => Try[String]) = {
    zio.console.putStr(s"Processing $index: $url into $targetDirectory ")
    val zipUrl =
      if (url.endsWith("/")) s"$url$zipFileName" else s"$url/$zipFileName"

    def unzip(zipFile: Path): URIO[Any, Either[Throwable, Path]] = {
      val paddedIndex = index.toString.reverse.padTo(4, '0').reverse
      Task.fromTry(
        Unzip.to(zipFile,
                 targetDirectory
                   .resolve("data")
                   .resolve(s"${paddedIndex}-$zipFileName")
                   .mkDirs())(fileNameForEntry.andThen(_.get))).either
    }

    def validate(downloadResult : Either[Throwable, Seq[String]]) =  {
      downloadResult match {
        case Left(err) =>
        case Right(err) =>
      }

    }

    def onDownloadSuccess(dataDir : Path): URIO[Any, Either[Throwable, Seq[String]]] =        ValidateXml(dataDir).either

    for {
      _ <- putStrLn(s"# Downloading entry #$index : $zipUrl")
      zipFile <- Download.toFile(zipUrl,
                                 targetDirectory.resolve(zipFileName),
                                 acceptableStatusCodes)
      dataDirEither <- unzip(zipFile)
      errorMessages <- dataDirEither.fold()
    } yield UnzipResult(zipFile, dataDir, errorMessages)
  }

}
