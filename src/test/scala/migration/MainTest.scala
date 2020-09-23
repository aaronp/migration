package migration

import eie.io._
import zio.test.Assertion.{contains, equalTo}
import zio.test._
import zio.test.environment.TestConsole
import zio.{ExitCode, Task}

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
          contains(
            s"""Running with
               |               URL : https://storage.googleapis.com/mygration
               |          indexURL : https://storage.googleapis.com/mygration/index.txt
               |            dryRun : false
               |         directory : ${testDir}
               |  filename pattern : s///g""".stripMargin
          )) &&
          assert(output)(contains(s"mkdir -p $testDir")) &&
          assert(output)(contains(s"# downloading https://storage.googleapis.com/mygration/backup-2.zip to directory ${testDir}")) &&
          assert(output)(contains(s"# downloading https://storage.googleapis.com/mygration/backup-0.zip to directory ${testDir}")) &&
          assert(output)(contains(s"# downloading https://storage.googleapis.com/mygration/backup-3.zip to directory ${testDir}")) &&
          assert(output)(contains(s"# downloading https://storage.googleapis.com/mygration/backup-1.zip to directory ${testDir}")) &&
          assert(exitCode)(equalTo(ExitCode.failure))
      }
    })
  }
}
