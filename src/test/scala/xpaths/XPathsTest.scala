package xpaths

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class XPathsTest extends AnyWordSpec with Matchers {

  "XPaths" should {
    "work" ignore {
      val xml = <root xmlns="base">
        <children>
          <child>first</child>
          <child xmlns="base">second</child>
          <child att="3rd">third</child>
        </children>
      </root>

      val map = XPaths(xml)
      println(map)
    }
    "namespaces" in {
      val xml = <h:table xmlns:h="http://www.w3.org/TR/html4/" xmlns:f="https://www.w3schools.com/furniture">
        <h:tr>
          <h:td att="apples" isApple="true">Apples</h:td>
          <h:td att="banana">Bananas</h:td>
          <h:td att="empty"></h:td>
        </h:tr>
        <f:width>80</f:width>
      </h:table>


      val map = XPaths(xml)
      println(map)
    }

  }
}
