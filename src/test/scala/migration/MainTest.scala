package migration

import eie.io._
import zio.Task
import zio.test.Assertion.equalTo
import zio.test._
import zio.test.environment.TestConsole

object MainTest extends DefaultRunnableSpec {
  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("Main")(testM(
      "report failed downloads") {
      for {
        testDir <- Task.effect(s"./target/mainTest-${UniqueIds.next()}/data".asPath)
        exitCode <- Main.run(
          List(s"dir=${testDir}",
               "pattern=\\2@@@\\1",
               "regex=f(.)le(.+)\\.xml"))
//          _ <- Task.effect(testDir.delete())
        output <- TestConsole.output
      } yield {
        println("v" * 120)
        output.foreach(println)
        println("^" * 120)
        assert(output)(
          equalTo(Vector(
            s"""Running with
              |      URL : https://storage.googleapis.com/mygration
              | indexURL : https://storage.googleapis.com/mygration/index.txt
              |   dryRun : false
              |directory : ${testDir}
              |  pattern : s///g""".stripMargin,
            "curl -o ./target/downloads/test.txt someurl && checkStatusIn []"
          )))

      }
    })
  }
}
