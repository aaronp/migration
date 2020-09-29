package xpaths

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class XPathsTest extends AnyWordSpec with Matchers {

  "XPaths" should {
    "map arrays of nested xml" in {
      val xml = <root>
        <coll1>
          <list>
            <item>one</item>
            <item>two</item>
          </list>
        </coll1>
        <coll2>
          <list>
            <item>one</item>
            <item>two</item>
          </list>
        </coll2>
        <coll1>
          <list>
            <item>one</item>
            <item>two</item>
          </list>
        </coll1>
      </root>

      XPaths(xml).sorted() should contain inOrderOnly(
        ("root.coll1[0].list.item[0]", "one"),
        ("root.coll1[0].list.item[1]", "two"),
        ("root.coll1[1].list.item[0]", "one"),
        ("root.coll1[1].list.item[1]", "two"),
        ("root.coll2.list.item[0]", "one"),
        ("root.coll2.list.item[1]", "two")
      )
    }
    "map multiple namespaces" in {
      val xml = <h:table xmlns:h="http://www.w3.org/TR/html4/" xmlns:f="https://www.example.com/foo">
        <h:tr>
          <h:td att="apples" isApple="true">Apples</h:td>
          <h:td att="banana">Bananas</h:td>
          <h:td att="empty"></h:td>
        </h:tr>
        <f:width>80</f:width>
      </h:table>

      XPaths(xml).sorted() should contain inOrderOnly(
        ("table.width", "80"),
        ("table.tr.td[0]", "Apples"),
        ("table.tr.td[1]", "Bananas")
      )
    }

  }
}
