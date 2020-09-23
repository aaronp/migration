package migration

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Success

class RegexResolveTest extends AnyWordSpec with Matchers {

  "RegexResolve.replace" should {
    "replace back-references from one string in another" in {
      val replacement = RegexResolve.replace("first:$1 second:$2.txt")
      replacement(Array("foo", "bar", "bazz")) shouldBe "first:foo second:bar.txt"
      RegexResolve.replace("$2$1")(Array("foo", "bar")) shouldBe "barfoo"
      RegexResolve.replace("$2")(Array("foo", "bar")) shouldBe "bar"
    }
    "ignore the group if there are no back-references" in {
      RegexResolve.replace("this is constant")(Array("foo", "bar")) shouldBe "this is constant"
      RegexResolve.replace("this is constant")(Array[String]()) shouldBe "this is constant"
    }
    "throw an exception if the replacement string don't capture the group" in {
      val bang = intercept[Exception] {
        RegexResolve.replace("$2$1")(Array("just one"))
      }
      bang.getMessage shouldBe "regex only captured 1 groups, so cannot replace index 1 in pattern '$2$1', captured group: [just one]"
    }
  }
  "RegexResolve.apply" should {
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
