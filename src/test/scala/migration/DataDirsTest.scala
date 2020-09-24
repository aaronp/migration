package migration

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DataDirsTest extends AnyWordSpec with Matchers {

  "DataDirs.children" should {
    "report the directories to standard out" in {
      import eie.io._

      "./target/files".asPath.mkDirs().deleteAfter { testDir =>
        testDir.resolve("data/000first.zip/001.txt").text = ""
        testDir.resolve("data/000first.zip/002.txt").text = ""
        testDir.resolve("data/001second.zip/001.txt").text = ""
        testDir.resolve("data/001second.zip/002.txt").text = ""
        testDir.resolve("data/001second.zip/003.txt").text = ""

        val out = DataDirs.children(Array(s"dir=${testDir.toString}"))
        out shouldBe
          """./target/files/data/000first.zip
            |./target/files/data/001second.zip""".stripMargin

      }
    }
  }
}
