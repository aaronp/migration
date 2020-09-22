package migration

import java.nio.file.Path

import eie.io._
import zio.test.Assertion.{contains, containsString, equalTo, exists}
import zio.test._
import zio.test.environment.TestConsole
import zio.{ExitCode, Task}

/**
 * There's some static test data available for testing under the 'https://storage.googleapis.com/mygration/' URL:
 *
 * * valid.txt --> contains 3 links to 'backup-*zip' zips in a weird order. The backup*zip files are valid save the last xml file (malformed)
 * * index.txt --> like 'valid.txt' but it (1) is the default index name and (2) references a missing zip file (backup-3.zip)
 * * small-valid.txt --> links to a single small-valid.zip which is small and valid
 * * noperms.txt --> an non-public (but valid/existing) index file
 * * noperms-zip.txt --> an accessible zip file, but one of the zip entries is not publicly accessible
 * * mix.txt --> contains two zip entries: small-valid.zip and invalid.zip, which are what they say on the tin.
 *
 */
object MainTest extends DefaultRunnableSpec {
  def rmdir(nestedDir: Path) = {
    val testDir = nestedDir.parents.takeWhile(_.fileName != "target").take(3).headOption
    zio.console.putStrLn(s"Deleting ${testDir.map(_.toAbsolutePath)}") *> Task.effect(testDir.foreach(_.delete())).retryN(3).either.unit
  }

  private def debug(output: Vector[String]): Unit = {
    println("v" * 120)
    output.zipWithIndex.foreach {
      case (out, i) =>
        println(s"Output $i: >${out}<")
    }
    println("^" * 120)

  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("Main")(testM("successfully download/extract small-valid.txt") {
      for {
        testDir <- Task.effect(s"./target/mainTest-${UniqueIds.next()}/foo".asPath)
        exitCode <- Main.run(
          List(s"dir=${testDir}",
            "indexURL=small-valid.txt",
            "fileNamePattern=$1:$0",
            "fileNameRegex=AB_CD_([0-9]{3,3})_([0-9]+).xml"))
        output <- TestConsole.output
        extractedFileCount <- Task.effect(testDir.resolve("data/0000-small-valid.zip").children.filter(_.isFile).count(_.fileName.matches("[0-9][0-9][0-9]:AB_CD_.*")))
        _ <- rmdir(testDir)
      } yield {
        assert(output)(
          contains(
            s"""Running with
               |               URL : https://storage.googleapis.com/mygration
               |          indexURL : https://storage.googleapis.com/mygration/small-valid.txt
               |         directory : ${testDir}
               |  filename pattern : s|AB_CD_([0-9]{3,3})_([0-9]+).xml|$$1:$$0|g
               |""".stripMargin
          )) &&
          assert(output)(contains(s"mkdir -p $testDir/downloads\n")) &&
          assert(output)(contains(s"# downloading https://storage.googleapis.com/mygration/small-valid.zip to directory ${testDir}/downloads\n")) &&
          assert(output)(contains("0000: small-valid.zip OK\n")) &&
          assert(extractedFileCount)(equalTo(21)) &&
          assert(exitCode)(equalTo(ExitCode.success))
      }
    },
      testM("report failures when the filenames don't match the extraction regex") {
        for {
          testDir <- Task.effect(s"./target/mainTest-${UniqueIds.next()}/foo".asPath)
          exitCode <- Main.run(
            List(s"dir=${testDir}",
              "indexURL=valid.txt",
              "fileNamePattern=$1:$0",
              "fileNameRegex=this_regex_doesnt_match_([0-9]+).xml"))
          output <- TestConsole.output
          extractedFileCount <- Task.effect(testDir.resolve("data/0000-small-valid.zip").children.filter(_.isFile).count(_.fileName.matches("[0-9][0-9][0-9]:AB_CD_.*")))
          _ <- rmdir(testDir)
        } yield {
          assert(output)(
            contains(
              s"""Running with
                 |               URL : https://storage.googleapis.com/mygration
                 |          indexURL : https://storage.googleapis.com/mygration/valid.txt
                 |         directory : ${testDir}
                 |  filename pattern : s|this_regex_doesnt_match_([0-9]+).xml|$$1:$$0|g
                 |""".stripMargin
            )) &&
            assert(output)(contains(s"mkdir -p $testDir/downloads\n")) &&
            assert(extractedFileCount)(equalTo(0)) &&
            assert(output)(contains(s"# downloading https://storage.googleapis.com/mygration/backup-2.zip to directory ${testDir}/downloads\n")) &&
            assert(output)(contains(s"# downloading https://storage.googleapis.com/mygration/backup-1.zip to directory ${testDir}/downloads\n")) &&
            assert(output)(contains(s"# downloading https://storage.googleapis.com/mygration/backup-0.zip to directory ${testDir}/downloads\n")) &&
            assert(output)(contains(s"0000: backup-2.zip failed: 'file0.xml' didn't match this_regex_doesnt_match_([0-9]+).xml\n")) &&
            assert(output)(contains(s"0001: backup-1.zip failed: 'file0.xml' didn't match this_regex_doesnt_match_([0-9]+).xml\n")) &&
            assert(output)(contains(s"0002: backup-0.zip failed: 'file0.xml' didn't match this_regex_doesnt_match_([0-9]+).xml\n")) &&
            assert(exitCode)(equalTo(ExitCode.failure))
        }
      },
      testM("show a sensible error when not permissioned for the index file (e.g. noperms.txt)") {
        def countDir(testDir: Path, index: String) = {
          Task.effect(testDir.resolve(s"data/$index").children.filter(_.isFile).count(_.fileName.matches("[0-9]{1,4}\\.xml")))
        }

        for {
          testDir <- Task.effect(s"./target/mainTest-${UniqueIds.next()}/foo".asPath)
          exitCode <- Main.run(
            List(s"dir=${testDir}",
              "indexURL=noperms.txt",
              "fileNamePattern=$1.xml",
              "fileNameRegex=file([0-9]+).xml"))
          output <- TestConsole.output
          _ <- rmdir(testDir)
        } yield {
          assert(output)(
            contains(
              s"""Running with
                 |               URL : https://storage.googleapis.com/mygration
                 |          indexURL : https://storage.googleapis.com/mygration/noperms.txt
                 |         directory : ${testDir}
                 |  filename pattern : s|file([0-9]+).xml|$$1.xml|g
                 |""".stripMargin
            )) &&
            assert(output)(contains(
              """Extraction failed with requests.RequestFailedException: Request to https://storage.googleapis.com/mygration/noperms.txt failed with status code 403
                |<?xml version='1.0' encoding='UTF-8'?><Error><Code>AccessDenied</Code><Message>Access denied.</Message><Details>Anonymous caller does not have storage.objects.get access to the Google Cloud Storage object.</Details></Error>
                |""".stripMargin)) &&
            assert(exitCode)(equalTo(ExitCode.failure))
        }
      },
      testM("report each zip file's failures from index.txt") {
        def countDir(testDir: Path, index: String) = {
          Task.effect(testDir.resolve(s"data/$index").children.filter(_.isFile).count(_.fileName.matches("[0-9]{1,4}\\.xml")))
        }

        for {
          testDir <- Task.effect(s"./target/mainTest-${UniqueIds.next()}/foo".asPath)
          exitCode <- Main.run(
            List(s"dir=${testDir}",
              "indexURL=index.txt",
              "fileNamePattern=$1.xml",
              "fileNameRegex=file([0-9]+).xml"))
          output <- TestConsole.output
          count0 <- countDir(testDir, "0000-backup-2.zip")
          count1 <- countDir(testDir, "0001-backup-1.zip")
          count2 <- countDir(testDir, "0003-backup-0.zip")
          _ <- rmdir(testDir)
        } yield {
          assert(output)(
            contains(
              s"""Running with
                 |               URL : https://storage.googleapis.com/mygration
                 |          indexURL : https://storage.googleapis.com/mygration/index.txt
                 |         directory : ${testDir}
                 |  filename pattern : s|file([0-9]+).xml|$$1.xml|g
                 |""".stripMargin
            )) &&
            assert(output)(contains(s"mkdir -p $testDir/downloads\n")) &&
            assert(count0)(equalTo(1002)) &&
            assert(count1)(equalTo(1002)) &&
            assert(count2)(equalTo(1002)) &&
            assert(output)(contains(s"# downloading https://storage.googleapis.com/mygration/backup-2.zip to directory ${testDir}/downloads\n")) &&
            assert(output)(contains(s"# downloading https://storage.googleapis.com/mygration/backup-1.zip to directory ${testDir}/downloads\n")) &&
            assert(output)(contains(s"# downloading https://storage.googleapis.com/mygration/backup-0.zip to directory ${testDir}/downloads\n")) &&
            assert(output)(contains(s"# downloading https://storage.googleapis.com/mygration/backup-3.zip to directory ${testDir}/downloads\n")) &&
            assert(output)(contains(
              s"""0001: backup-1.zip failed w/ 1 error(s):
                 |	1001.xml : The end-tag for element type "data" must end with a '>' delimiter.
                 |
                 |""".stripMargin)) &&
            assert(output)(contains(
              s"""0001: backup-1.zip failed w/ 1 error(s):
                 |	1001.xml : The end-tag for element type "data" must end with a '>' delimiter.
                 |
                 |""".stripMargin)) &&
            assert(output)(contains(
              s"""0002: backup-3.zip failed: Request to https://storage.googleapis.com/mygration/backup-3.zip failed with status code 403
                 |<?xml version='1.0' encoding='UTF-8'?><Error><Code>AccessDenied</Code><Message>Access denied.</Message><Details>Anonymous caller does not have storage.objects.get access to the Google Cloud Storage object.</Details></Error>
                 |""".stripMargin)) &&
            assert(output)(contains(
              s"""0003: backup-0.zip failed w/ 1 error(s):
                 |	1001.xml : The end-tag for element type "data" must end with a '>' delimiter.
                 |
                 |""".stripMargin)) &&
            assert(exitCode)(equalTo(ExitCode.failure))
        }
      },
      testM("report only the failures from mix.txt") {
        for {
          testDir <- Task.effect(s"./target/mainTest-${UniqueIds.next()}/foo".asPath)
          exitCode <- Main.run(
            List(s"dir=${testDir}",
              "indexURL=mix.txt",
              "fileNamePattern=$0.xml",
              "fileNameRegex=.*[0-9].*"))
          output <- TestConsole.output
          _ <- rmdir(testDir)
        } yield {
          assert(output)(
            contains(
              s"""Running with
                 |               URL : https://storage.googleapis.com/mygration
                 |          indexURL : https://storage.googleapis.com/mygration/mix.txt
                 |         directory : ${testDir}
                 |  filename pattern : s|.*[0-9].*|$$0.xml|g
                 |""".stripMargin
            )) &&
            assert(output)(contains(s"mkdir -p $testDir/downloads\n")) &&
            assert(output)(contains(s"# downloading https://storage.googleapis.com/mygration/small-valid.zip to directory $testDir/downloads\n")) &&
            assert(output)(contains(s"# downloading https://storage.googleapis.com/mygration/invalid.zip to directory $testDir/downloads\n")) &&
            assert(output)(contains(s"0000: small-valid.zip OK\n")) &&
            assert(output)(contains(
              s"""0001: invalid.zip failed: 'bad_file_name.xml' didn't match .*[0-9].*
                 |""".stripMargin)) &&
            assert(exitCode)(equalTo(ExitCode.failure))
        }
      },
      testM("show=? should just display the config") {
        for {
          testDir <- Task.effect(s"./target/mainTest-${UniqueIds.next()}/foo".asPath)
          exitCode <- Main.run(
            List(s"dir=${testDir}",
              "fileNamePattern=$1.xml",
              "show=fileNamePattern"))
          output <- TestConsole.output
          _ <- rmdir(testDir)
        } yield {
          assert(output)(contains(s"""fileNamePattern : $$1.xml # command-line\n""")) &&
            assert(testDir.exists())(equalTo(false)) &&
            assert(exitCode)(equalTo(ExitCode.success))
        }
      },
      testM("fail when trying to extract when a file is the parent") {
        for {
          testDir <- Task.effect(s"./target/imAFile${UniqueIds.next()}.file".asPath.text = "I'm a file, actually")
          exitCode <- Main.run(List(s"dir=${testDir}"))
          output <- TestConsole.output
          _ <- rmdir(testDir)
        } yield {
          locally {
            debug(output)
          }
          assert(output)(exists(containsString("""Not a directory"""))) &&
            assert(exitCode)(equalTo(ExitCode.failure))
        }
      }
    )
  }
}
