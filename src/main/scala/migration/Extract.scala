package migration

import java.nio.file.Path

import eie.io._
import zio.console.{Console, putStrLn}
import zio.{Fiber, Task, URIO, ZIO}

import scala.util.Try

object Extract {

  def apply(config: ParsedConfig): ZIO[Console, Throwable, Unit] = {
    extractAndValidate(config).either.flatMap(prepareFinalReport)
  }

  case class ValidationResult(zipFile: Path, errorMessages: Seq[String]) {
    def desc: String = errorMessages match {
      case Seq() => "OK"
      case errors => errors.mkString(s"failed w/ ${errors.size} error(s):\n\t", "\n\t", "\n")
    }
  }

  case class ZipEntryResult(entryZipFileName: String, index: Int, result: Either[Throwable, ValidationResult]) {
    def header = s"${index.toString.reverse.padTo(4, '0').reverse}: $entryZipFileName"

    def success: Boolean = result.fold(_ => false, _.errorMessages.isEmpty)

    def desc: String = result match {
      case Left(err) => s"${header} failed: ${err.getMessage}"
      case Right(result) => s"${header} ${result.desc}"
    }
  }

  private def prepareFinalReport(either: Either[Throwable, List[ZipEntryResult]]) = either match {
    case Left(err) =>
      val result = putStrLn(s"Extraction failed with ${err}") *> Task.fail(err)
      result.unit
    case Right(results) =>
      val outputs: ZIO[Console, Nothing, Unit] = ZIO.foreach(results) { result =>
        putStrLn(result.desc)
      }.unit
      val isSuccess: ZIO[Any, Throwable, List[Boolean]] = ZIO.foreach(results) { result =>
        ZIO(result.success)
      }

      val finalTask: ZIO[Any, Throwable, Unit] = isSuccess.flatMap { successes =>
        successes.count(_ == false) match {
          case 0 => ZIO.unit
          case 1 => Task.fail(new Exception("One of the tasks failed"))
          case n => Task.fail(new Exception(s"$n tasks failed"))
        }
      }

      outputs *> finalTask
  }

  /**
   * The business logic of extracting/validating zip files
   *
   * @param config
   * @return
   */
  def extractAndValidate(config: ParsedConfig): ZIO[Console, Throwable, List[ZipEntryResult]] = {
    import config._

    val fileNameForEntry = RegexResolve(config.fileNameRegex.trim, config.fileNamePattern.trim)

    for {
      zipFilesNames <- Download.indexFile(indexURL,
        downloadDirectory.resolve(indexFileName))
      forks <- ZIO.foreach(zipFilesNames.zipWithIndex) {
        case (zipEntry, index) =>
          val forked: URIO[Console, Fiber.Runtime[Nothing, Either[Throwable, ValidationResult]]] = extractAndValidateEntry(config.url,
            downloadDirectory,
            dataDirectory,
            index,
            zipEntry,
            checkDownloadStatus,
            fileNameForEntry).either.fork

          forked.map(fiber => (zipEntry, index, fiber))
      }
      result <- ZIO.foreach(forks.map {
        case (zipEntry, index, fiber) => fiber.join.map { result =>
          ZipEntryResult(zipEntry, index, result)
        }
      })(identity)
    } yield result.sortBy(_.index)
  }

  private def extractAndValidateEntry(url: String,
                                      downloadDirectory: Path,
                                      dataDirectory: Path,
                                      index: Int,
                                      zipFileName: String,
                                      acceptableStatusCodes: Set[Int],
                                      fileNameForEntry: String => Try[String]) = {
    zio.console.putStr(s"Processing $index: $url into $dataDirectory ")
    val zipUrl =
      if (url.endsWith("/")) s"$url$zipFileName" else s"$url/$zipFileName"

    def unzip(zipFile: Path) = {
      val paddedIndex = index.toString.reverse.padTo(4, '0').reverse
      Task.fromTry(
        Unzip.to(zipFile,
          dataDirectory
            .resolve(s"${paddedIndex}-$zipFileName")
            .mkDirs())(fileNameForEntry.andThen(_.get)))
    }

    for {
      zipFile <- Download.toFile(zipUrl,
        downloadDirectory.resolve(zipFileName),
        acceptableStatusCodes)
      dataDir <- unzip(zipFile)
      errorMessages <- ValidateXml(dataDir)
    } yield ValidationResult(zipFile, errorMessages)
  }

}
