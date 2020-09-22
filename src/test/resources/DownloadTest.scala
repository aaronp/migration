package migration

import java.util.concurrent.atomic.AtomicLong

import eie.io._
import zio.Task
import zio.test.Assertion.equalTo
import zio.test._
import zio.test.environment.TestConsole

object DownloadTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("Download")(
      testM(
        "dry run: try and create the parent directory if it doesn't already exist") {
        for {
          testDir <- Task.effect("./target/downloads/test.txt".asPath)
          _ <- Download.debug("someurl", testDir)
          _ <- Task.effect(testDir.delete())
          output <- TestConsole.output
        } yield
          assert(output)(equalTo(Vector(
            "mkdir -p ./target/downloads",
            "curl -o ./target/downloads/test.txt someurl && checkStatusIn []")))
      },
      testM("dry run: only download if the parent directory already exists") {
        for {
          dirName <- Task.effect(s"./target/download-${UniqueIds.next()}")
          testDir <- Task.effect(dirName.asPath.mkDirs().resolve("test.txt"))
          _ <- Download.debug("http://foo", testDir)
          _ <- Task.effect(testDir.getParent.delete())
          output <- TestConsole.output
        } yield
          assert(output)(
            equalTo(
              Vector(
                s"# downloading http://foo to directory ${dirName}",
                s"curl -o ${dirName}/test.txt http://foo && checkStatusIn []")))
      },
      testM("dry run: not download if the target file already exists") {
        for {
          testFile <- Task.effect(
            s"./target/download-${UniqueIds.next()}/exists.txt".asPath.text =
              "already exists")
          _ <- Download.debug("http://foo", testFile)
          _ <- Task.effect(testFile.getParent.delete())
          output <- TestConsole.output
        } yield
          assert(output)(
            equalTo(Vector(s"# ${testFile} already exists, skipping download")))
      },
      testM("download a file to a particular location") {
        val url = "https://storage.googleapis.com/mygration/index.txt"
        for {
          testFile <- Task.effect(
            s"./target/download-${UniqueIds.next()}/fileList.txt".asPath)
          _ <- Download.toFile(url, testFile, acceptableStatuses = Set(200))
          content <- Task.effect(testFile.text)
          _ <- Task.effect(testFile.getParent.delete())
          output <- TestConsole.output
        } yield {
          //          assert(output)(equalTo(Vector(s"# downloading $url to directory ${testFile.getParent}"))) &&
          assert(content)(equalTo("""backup-2.zip
                                    |backup-1.zip
                                    |backup-3.zip
                                    |backup-0.zip""".stripMargin))
        }
      },
      testM("not download a file to a particular location if it already exists") {
        val url = "https://storage.googleapis.com/mygration/index.txt"
        for {
          testFile <- Task.effect(
            s"./target/download-${UniqueIds.next()}/fileList.txt".asPath.text =
              "foo")
          _ <- Download.toFile(url, testFile, acceptableStatuses = Set(200))
          content <- Task.effect(testFile.text)
          _ <- Task.effect(testFile.getParent.delete())
        } yield {
          //          assert(output)(equalTo(Vector(s"# downloading $url to directory ${testFile.getParent}"))) &&
          assert(content)(equalTo("foo"))
        }
      },
      testM("fail the download if the status code doesn't match") {
        val url = "https://storage.googleapis.com/mygration/index.txt"
        for {
          testFile <- Task.effect(
            s"./target/download-${UniqueIds.next()}/fileList.txt".asPath)
          downloadResult <- Download
            .toFile(url, testFile, acceptableStatuses = Set(201))
            .either
          fileExists <- Task.effect(testFile.exists())
          _ <- Task.effect(testFile.getParent.delete())
          output <- TestConsole.output
        } yield {
          assert(output)(equalTo(Vector(s"mkdir -p ${testFile.getParent}"))) &&
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
