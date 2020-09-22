package migration

import java.nio.file.Path

import args4c.implicits._
import com.typesafe.config.Config
import eie.io._
import zio.console.Console
import zio.{ExitCode, Fiber, URIO, ZIO}

object Main extends zio.App {

  class ParsedConfig(config: Config) {

    val url = config.getString("url")
    val targetDirectory = config.getString("dest").asPath
    val dryRun = config.getBoolean("dryRun")
    val checkDownloadStatus = config.asList("checkDownloadStatus").toSet.filterNot(_.trim.isEmpty).map(_.toInt)
    val indexURL = config.getString("indexURL")

    override def toString: String = {
      s"""     URL : $url
         |indexURL : $indexURL
         |  dryRun : $dryRun
         |""".stripMargin
    }
  }

  object ParsedConfig {
    def apply(config: Config) = new ParsedConfig(config)
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val config = asConfig(args)
    val appIO = config.showIfSpecified() match {
      case None => apply(ParsedConfig(config))
      case Some(value) => ZIO.effect(println(value))
    }

    appIO.either.map {
      case Left(_) => ExitCode.failure
      case Right(_) => ExitCode.success
    }
  }

  def debug(config: ParsedConfig) = {
    import config._
    Download.debug(indexURL, targetDirectory)
  }

  def process(url: String, targetDirectory: Path, index: Int, zipEntry: String, acceptableStatusCodes: Set[Int]) = {
    zio.console.putStr(s"Processing $index: $url into $targetDirectory ")
    val zipUrl = if (url.endsWith("/")) s"$url$zipEntry" else s"$url/$zipEntry"
    for {
      zipFile <- Download.toFile(zipUrl, targetDirectory, acceptableStatusCodes)
    } yield zipFile
  }

  /**
   * The 'actually do this' application
   *
   * @param config
   * @return
   */
  def live(config: ParsedConfig) = {
    import config._
    for {
      zipFilesNames <- Download.indexFile(indexURL, targetDirectory.resolve("index.txt"))
      forks <- ZIO.foreach(zipFilesNames.zipWithIndex) {
        case (zipEntry, index) =>
          process(config.url, targetDirectory, index, zipEntry, checkDownloadStatus).fork
      }
      _ <- ZIO.foreach(forks.map(_.join))(identity)
    } yield ()
  }

  def apply(config: ParsedConfig) = {
    import config._
    for {
      _ <- zio.console.putStr(s"""Running with\n$config""")
      _ <- if (dryRun) debug(config) else live(config)
    } yield ()
  }

  def asConfig(args: List[String]) = {
    //    val loader = Thread.currentThread.getContextClassLoader
    //    val app = ConfigFactory.defaultApplication(loader)
    //    val default = ConfigFactory.defaultOverrides(loader).withFallback(app).withFallback(ConfigFactory.defaultReference(loader))
    args.toArray.asConfig()
  }
}
