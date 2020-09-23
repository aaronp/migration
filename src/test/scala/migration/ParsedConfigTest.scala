package migration

import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ParsedConfigTest extends AnyWordSpec with Matchers {

  def conf(str: String) = {
    ConfigFactory.parseString(str).withFallback(ConfigFactory.load())
  }

  "ParsedConfig.indexUrl" should {
    "take into account the slash ending of the url param" in {
      ParsedConfig(conf(
        """url : "http://foo"
          |indexUrl : "index.text
          |""".stripMargin)).indexURL shouldBe "http://foo/index.text"
      ParsedConfig(conf(
        """url : "http://foo/"
          |indexUrl : "/index.text
          |""".stripMargin)).indexURL shouldBe "http://foo/index.text"
      ParsedConfig(conf(
        """url : "http://foo/"
          |indexUrl : "index.text
          |""".stripMargin)).indexURL shouldBe "http://foo/index.text"
    }
  }
}
