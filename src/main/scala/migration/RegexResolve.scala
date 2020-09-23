package migration

import scala.util.{Failure, Success, Try}

object RegexResolve {
  private val BackRef = "(.*?)\\$([0-9]+)(.*)".r
  type Group = IndexedSeq[String]
  type Swap = Group => String

  def apply(regex: String, replacement: String): String => Try[String] = {
    (regex, replacement) match {
      case ("", "") => (text: String) => Success(text)
      case (regex, pattern) => {
        val resolve = RegexResolve.forPattern(regex)
        val replacement = RegexResolve.replace(pattern)
        (fileName: String) => {
          resolve(fileName) match {
            case None => Failure(new Exception(s"'$fileName' didn't match $regex"))
            case Some(list) => Try(replacement(list))
          }
        }
      }
    }
  }

  def replace(replacementPattern: String): Swap = {
    createReplacement(replacementPattern) match {
      case Left(constant) => (_: Group) => constant
      case Right(swap) => swap
    }
  }


  private def createReplacement(replacementPattern: String): Either[String, Swap] = {
    def groupAt(capture: Group, index: Int): String = {
      try {
        capture(index)
      } catch {
        case _: ArrayIndexOutOfBoundsException => {
          sys.error(s"regex only captured ${capture.size} groups, so cannot replace index $index in pattern '${replacementPattern}', captured group: ${capture.mkString("[", ",", "]")}")
        }
      }
    }

    replacementPattern match {
      case BackRef(before, indexStr, after) =>
        // back-references typically are one-based (e.g. $1 is the first group)
        val index = indexStr.toInt - 1
        createReplacement(after) match {
          case Left(finished) =>
            val swap = (group: Group) => s"$before${groupAt(group, index)}$finished"
            Right(swap)
          case Right(continuation) =>
            val swap = (group: Group) => s"$before${groupAt(group, index)}${continuation(group)}"
            Right(swap)
        }
      case other => Left(other)
    }
  }

  def forPattern(pattern: String): CharSequence => Option[IndexedSeq[String]] =
    pattern.r.unapplySeq(_: CharSequence).map(_.toIndexedSeq)

}
