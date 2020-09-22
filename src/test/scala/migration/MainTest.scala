package migration

import eie.io._
import zio.Task
import zio.test.Assertion.equalTo
import zio.test.environment.TestConsole
import zio.test._

object MainTest extends DefaultRunnableSpec {
  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("Download")(
      testM(
        "dry run: try and create the parent directory if it doesn't already exist") {
        for {
          testDir <- Task.effect("./target/mainTest".asPath)
          exitCode <- Main.run(List(s"dir=${testDir}"))
//          _ <- Task.effect(testDir.delete())
          output <- TestConsole.output
        } yield
          assert(output)(equalTo(Vector(
            """Running with
              |     URL : https://storage.googleapis.com/mygration
              |indexURL : https://storage.googleapis.com/mygration/index.txt
              |  dryRun : false
              |""".stripMargin,
            "curl -o ./target/downloads/test.txt someurl && checkStatusIn []")))
      })
  }
}
