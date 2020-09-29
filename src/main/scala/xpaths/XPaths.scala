package xpaths

import xpaths.XPaths.XPath

import scala.xml.Node

case class XPaths(byPath: Map[XPath, Node]) {
  override def toString: String = {
    sorted.map {
      case (key, value) => s"$key : $value"
    }.mkString("\n")
  }

  def sorted(): Seq[(String, String)] = {
    byPath.toSeq.sortBy(_._1).flatMap {
      case (path, node) =>
        Option(node.text).map(_.trim).collect {
          case text if text.nonEmpty =>
            val xpath = path.hierarchy.map(_.shortLabel) match {
              case seq if seq.last == "#PCDATA" => seq.init
              case seq => seq
            }
            xpath.mkString(".") -> text
        }
    }
  }
}

object XPaths {

  case class XPath(hierarchy: List[PathElm]) extends Ordered[XPath] {
    override def toString: String = hierarchy.mkString(".")

    override def compare(that: XPath): Int = {
      hierarchy.size.compareTo(that.hierarchy.size) match {
        case 0 =>
          val results = hierarchy.zip(that.hierarchy).view.map {
            case (a, b) => a.compareTo(b)
          }
          results.find(_ != 0).getOrElse(0)
        case n => n
      }
    }
  }

  sealed trait PathElm extends Ordered[PathElm] {
    def shortLabel: String

    override def compare(that: PathElm): Int = {
      (this, that) match {
        case (a: Label, b: Label) => a.verbose.compareTo(b.verbose)
        case (a: ArrayElm, b: ArrayElm) =>
          a.label.compareTo(b.label) match {
            case 0 => a.index.compareTo(b.index)
            case n => n
          }
        case (a: ArrayElm, b: Label) => a.label.compareTo(b)
        case (a: Label, b: ArrayElm) => a.compareTo(b.label)
      }
    }
  }

  case class Label(namespace: String, prefix: String, label: String) extends PathElm {
    override def toString: String = s"$prefix:$label"

    def verbose: String = s"$prefix=$namespace:$label"

    override def shortLabel: String = label
  }

  case class ArrayElm(label: PathElm, index: Int) extends PathElm {
    override def shortLabel: String = s"${label.shortLabel}[$index]"

    override def toString: String = s"$label[$index]"
  }

  object Label {
    def apply(node: Node): Label = Label(
      Option(node.namespace).getOrElse(""),
      Option(node.prefix).getOrElse(""),
      Option(node.label).getOrElse(""))
  }

  def apply(xml: Node): XPaths = {
    val result: Map[List[PathElm], Node] = forNode(Label(xml) -> xml)
    new XPaths(result.map {
      case (list, node) => XPath(list.reverse) -> node
    })
  }

  def forNode(xmlAndLabel: (PathElm, Node), ancestors: List[PathElm] = Nil): Map[List[PathElm], Node] = {
    val (thisLabel, xml) = xmlAndLabel
    val path: List[PathElm] = thisLabel :: ancestors
    if (xml.child.isEmpty) {
      Map(path -> xml)
    } else {
      val childrenByKey: Map[PathElm, Seq[Node]] = xml.child.foldLeft(Map[PathElm, Seq[Node]]()) {
        case (map, child) =>
          val key = Label(child)
          val newList = child +: map.getOrElse(key, Vector())
          map.updated(key, newList)
      }

      childrenByKey.flatMap {
        case (label, Seq(only)) => forNode((label, only), path)
        case (label, many) =>
          val size = many.size
          many.zipWithIndex.flatMap {
            case (child, i) => forNode((ArrayElm(label, size - i - 1), child), path)
          }
      }
    }
  }
}
