package migration

import args4c.implicits._
import zio.console.Console
import zio.{ExitCode, URIO, ZIO}

object Main extends zio.App {

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val config = args.toArray.asConfig().resolve()
    val appIO: ZIO[Console, Throwable, Unit] = config.showIfSpecified() match {
      case None        => apply(ParsedConfig(config))
      case Some(value) => zio.console.putStrLn(value)
    }

    appIO.either.map {
      case Left(_)  => ExitCode.failure
      case Right(_) => ExitCode.success
    }
  }

  def apply(config: ParsedConfig) = {
    for {
      _ <- zio.console.putStrLn(s"""Running with\n$config""")
      _ <- Extract(config)
    } yield ()
  }
}
