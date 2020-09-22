package migration

object RegexResolve {
  def replace(fileName: String, list: List[String], pattern: String): _root_.scala.Predef.String = {
    ???
  }


  def apply(pattern: String): CharSequence => Option[String] = {
    forPattern(pattern).andThen(_.map(_.mkString("")))
  }

  def forPattern(pattern: String): CharSequence => Option[List[String]] =
    pattern.r.unapplySeq(_: CharSequence)

}
