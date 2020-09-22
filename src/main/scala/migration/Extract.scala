package migration

import java.nio.file.Path

import eie.io._
import zio.{Task, ZIO}

object Extract {

  case class UnzipResult(zipFile: Path, dataDir: Path, errors: Seq[String])

  def process(url: String,
              targetDirectory: Path,
              index: Int,
              zipFileName: String,
              acceptableStatusCodes: Set[Int],
              fileNameForEntry: String => String) = {
    zio.console.putStr(s"Processing $index: $url into $targetDirectory ")
    val zipUrl = if (url.endsWith("/")) s"$url$zipFileName" else s"$url/$zipFileName"

    def unzip(zipFile: Path): Task[Path] = {
      val paddedIndex = index.toString.reverse.padTo(4, '0').reverse
      Task.fromTry(Unzip.to(zipFile, targetDirectory.resolve("data").resolve(s"${paddedIndex}-$zipFileName").mkDirs())(fileNameForEntry))
    }

    for {
      zipFile <- Download.toFile(zipUrl, targetDirectory.resolve(zipFileName), acceptableStatusCodes)
      dataDir <- unzip(zipFile)
      errorMessages <- ValidateXml(dataDir)
    } yield UnzipResult(zipFile, dataDir, errorMessages)
  }

  /**
   * The 'actually do this' application
   *
   * @param config
   * @return
   */
  def apply(config: ParsedConfig) = {
    import config._
    val fileNameForEntry: String => String = config.fileNamePattern.trim match {
      case "" => identity[String]
      case pattern => {
        val resolve = RegexResolve(pattern)
        (fileName: String) => {
          resolve(fileName).getOrElse(sys.error(s"file '$fileName' didn't match $pattern"))
        }
      }
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
}
