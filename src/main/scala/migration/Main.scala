package migration

import args4c.implicits._
import com.typesafe.config.Config
import eie.io._
import zio.{ExitCode, URIO, ZIO}

object Main extends zio.App {

  class ParsedConfig(config: Config) {

    val url = config.getString("url")
    val dest = config.getString("dest")
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
    Download.debug(indexURL, dest.asPath)
  }

  def live(config: ParsedConfig) = {
    import config._
    for {
      files <- Download.indexList(indexURL, dest.asPath)
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
