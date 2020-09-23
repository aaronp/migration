package migration

import migration.RegexResolve.Group
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Success

class RegexResolveTest extends AnyWordSpec with Matchers {

  "RegexResolve.Group.baseName" should {
    "strip the path and suffix from a filename" in {
      Group("/root/file.txt", IndexedSeq()).baseName shouldBe "file"
      Group("./file.txt", IndexedSeq()).baseName shouldBe "file"
      Group("file.txt", IndexedSeq()).baseName shouldBe "file"
      Group("./file.xml", IndexedSeq()).baseName shouldBe "file"
      Group("./file.xml/dave", IndexedSeq()).baseName shouldBe "./file.xml/dave"
      Group("foo", IndexedSeq()).baseName shouldBe "foo"
    }
  }
  "RegexResolve.replace" should {

    "replace back-references from one string in another" in {
      val replacement = RegexResolve.replace("first:$1 second:$2.txt")
      replacement(Group("foo", "bar", "bazz")) shouldBe "first:foo second:bar.txt"
      RegexResolve.replace("$2$1")(Group("foo", "bar")) shouldBe "barfoo"
      RegexResolve.replace("$2")(Group("foo", "bar")) shouldBe "bar"
    }
    "ignore the group if there are no back-references" in {
      RegexResolve.replace("this is constant")(Group("foo", "bar")) shouldBe "this is constant"
      RegexResolve.replace("this is constant")(Group()) shouldBe "this is constant"
    }
    "throw an exception if the replacement string don't capture the group" in {
      val bang = intercept[Exception] {
        RegexResolve.replace("$2$1")(Group("just one"))
      }
      bang.getMessage shouldBe "regex only captured 1 groups, so cannot replace index 1 in pattern '$2$1', captured group: [just one]"
    }
  }
  "RegexResolve.apply" should {
    "replace $0 references with the whole text" in {
      val resolver = RegexResolve(".*/?ABC_([0-9]{9,9})_[0-9]+\\.xml", "$1:$0")
      resolver("ABC_123456789_987654321.xml") shouldBe Success("123456789:ABC_123456789_987654321")
      resolver("/path/to/file/ABC_123456789_987654321.xml") shouldBe Success("123456789:ABC_123456789_987654321")
      resolver("./file/ABC_123456789_987654321.xml") shouldBe Success("123456789:ABC_123456789_987654321")
      resolver("/ABC_123456789_987654321.xml") shouldBe Success("123456789:ABC_123456789_987654321")
      resolver("ABC_12345678_987654321.xml").toEither.swap.map(_.getMessage) shouldBe Right("'ABC_12345678_987654321.xml' didn't match .*/?ABC_([0-9]{9,9})_[0-9]+\\.xml")
    }
    "replace text in a string" in {
      val resolver = RegexResolve("foo([0-9]{2,3}).(.)\\.txt", "$2 after $1")
      resolver("foo12XY.txt") shouldBe Success("Y after 12")
      resolver("foo124ab.txt") shouldBe Success("b after 124")
      resolver("foo124ab.tx").toEither.swap.map(_.getMessage) shouldBe Right("'foo124ab.tx' didn't match foo([0-9]{2,3}).(.)\\.txt")
      resolver("bar124ab.txt").toEither.swap.map(_.getMessage) shouldBe Right("'bar124ab.txt' didn't match foo([0-9]{2,3}).(.)\\.txt")
      resolver("foo1_3ab.txt").toEither.swap.map(_.getMessage) shouldBe Right("'foo1_3ab.txt' didn't match foo([0-9]{2,3}).(.)\\.txt")
    }
  }
}
