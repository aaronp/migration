package migration

import java.util.concurrent.atomic.AtomicLong

import eie.io._
import zio.Task
import zio.test.Assertion.equalTo
import zio.test._
import zio.test.environment.TestConsole

import scala.concurrent.duration._

object DownloadTest extends DefaultRunnableSpec {
  val unique = new AtomicLong(System.currentTimeMillis())

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("Download")(
      testM("fail the download if the status code doesn't match") {
        val url = "https://storage.googleapis.com/mygration/index.txt"
        for {
          testFile <- Task.effect(
            s"./target/download-${unique.incrementAndGet()}/fileList.txt".asPath)
          downloadResult <- Download
            .indexFile(url,
                       testFile,
                       HttpRequestSettings(10.seconds,
                                           10.seconds,
                                           acceptableStatuses = Set(201)))
            .either
          fileExists <- Task.effect(testFile.exists())
          _ <- Task.effect(testFile.getParent.delete())
          output <- TestConsole.output
        } yield {
          locally {
            MainTest.debug(output)
            println(
              s"MSG IS : >${downloadResult.fold(_.getMessage, _.toString)}<")
          }

          assert(output)(equalTo(Vector(s"mkdir -p ${testFile.getParent}\n"))) &&
          assert(fileExists)(equalTo(false)) &&
          assert(downloadResult.fold(_.getMessage, _.toString))(equalTo(
            s"""Download '$url' returned unexpected status code '200' OK; body:backup-2.zip
                 |backup-1.zip
                 |backup-3.zip
                 |backup-0.zip""".stripMargin))
        }
      }
    )
  }
}
