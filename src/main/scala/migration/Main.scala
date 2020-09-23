package migration

import args4c.implicits._
import zio.{ExitCode, URIO, ZIO}

object Main extends zio.App {

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val config = args.toArray.asConfig()
    val appIO = config.showIfSpecified() match {
      case None        => apply(ParsedConfig(config))
      case Some(value) => ZIO.effect(println(value))
    }

    appIO.either.map {
      case Left(_)  => ExitCode.failure
      case Right(_) => ExitCode.success
    }
  }

  def debug(config: ParsedConfig) = {
    import config._
    Download.debug(indexURL, targetDirectory)
  }

  def apply(config: ParsedConfig) = {
    import config._
    for {
      _ <- zio.console.putStrLn(s"""Running with\n$config""")
      _ <- if (dryRun) debug(config) else Extract(config)
    } yield ()
  }
}
