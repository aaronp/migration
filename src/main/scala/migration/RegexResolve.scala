package migration

import scala.util.{Failure, Success, Try}

object RegexResolve {
  // captures the 'before', 'after' and the index: a dollar sign followed by an index (e.g. $1)
  private val BackRef = "(.*?)\\$([0-9]+)(.*)".r
  private val BaseName = """(.*)\..{3,3}""".r
  private val QualifiedBaseName = """.*/(.*)\..{3,3}""".r

  /**
    * A capture group (the full text and capture groups).
    *
    * The '$0' has a special meaning, in that rather than the full text, it strips the suffix
    *
    * @param fullText
    * @param groups
    */
  case class Group(fullText: String, groups: IndexedSeq[String]) {
    def baseName = fullText match {
      case QualifiedBaseName(name) => name
      case BaseName(name)          => name
      case other                   => other
    }

    def get(replacementPattern: String, index: Int): String = {
      if (index < 0) {
        baseName
      } else {
        try {
          groups(index)
        } catch {
          case _: ArrayIndexOutOfBoundsException => {
            sys.error(
              s"regex only captured ${groups.size} groups, so cannot replace index $index in pattern '${replacementPattern}', captured group: ${groups
                .mkString("[", ",", "]")}")
          }
        }
      }
    }
  }

  object Group {
    def apply(parts: String*) =
      new Group(parts.mkString(""), parts.toIndexedSeq)
  }

  type Swap = Group => String

  def apply(regex: String, replacement: String): String => Try[String] = {
    (regex, replacement) match {
      case ("", "") =>
        (text: String) =>
          Success(text)
      case (regex, pattern) => {
        val resolve = RegexResolve.forPattern(regex)
        val replacement: Swap = RegexResolve.replace(pattern)
        (fileName: String) =>
          {
            resolve(fileName) match {
              case None =>
                Failure(new Exception(s"'$fileName' didn't match $regex"))
              case Some(list) => Try(replacement(Group(fileName, list)))
            }
          }
      }
    }
  }

  def replace(replacementPattern: String): Swap = {
    createReplacement(replacementPattern) match {
      case Left(constant) =>
        (_: Group) =>
          constant
      case Right(swap) => swap
    }
  }

  private def createReplacement(
      replacementPattern: String): Either[String, Swap] = {

    replacementPattern match {
      case BackRef(before, indexStr, after) =>
        // back-references typically are one-based (e.g. $1 is the first group)
        val index = indexStr.toInt - 1
        createReplacement(after) match {
          case Left(finished) =>
            val swap = (group: Group) =>
              s"$before${group.get(replacementPattern, index)}$finished"
            Right(swap)
          case Right(continuation) =>
            val swap = (group: Group) =>
              s"$before${group.get(replacementPattern, index)}${continuation(group)}"
            Right(swap)
        }
      case other => Left(other)
    }
  }

  def forPattern(pattern: String): CharSequence => Option[IndexedSeq[String]] =
    pattern.r.unapplySeq(_: CharSequence).map(_.toIndexedSeq)

}
