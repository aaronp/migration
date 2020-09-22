package migration

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RegexResolveTest extends AnyWordSpec with Matchers {

  "RegexResolve" should {
    "map strings" in {
      val resolver = RegexResolve("foo([0-9]{2,3}).(.)\\.txt")
      resolver("foo12XY.txt") shouldBe Some("12Y")
      resolver("foo124ab.txt") shouldBe Some("124b")
      resolver("foo124ab.tx") shouldBe None
      resolver("bar124ab.txt") shouldBe None
      resolver("foo1_3ab.txt") shouldBe None
    }
  }
}
