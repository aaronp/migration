package migration

import eie.io._
import zio.test.Assertion.{contains, equalTo}
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
        _ <- Task.effect(testDir.getParent.getParent.delete())
      } yield {
        println("v" * 120)
        output.foreach(println)
        println("^" * 120)
        assert(output)(
          contains(
            s"""Running with
               |               URL : https://storage.googleapis.com/mygration
               |          indexURL : https://storage.googleapis.com/mygration/small-valid.txt
               |            dryRun : false
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
//          _ <- Task.effect(testDir.getParent.getParent.delete())
        } yield {
          println("v" * 120)
          output.foreach(println)
          println("^" * 120)
          assert(output)(
            contains(
              s"""Running with
                 |               URL : https://storage.googleapis.com/mygration
                 |          indexURL : https://storage.googleapis.com/mygration/valid.txt
                 |            dryRun : false
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
      }
    )
  }
}
