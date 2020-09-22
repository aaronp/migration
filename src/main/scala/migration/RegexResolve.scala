package migration

object RegexResolve {

  def apply(pattern: String): CharSequence => Option[String] = {
    forPattern(pattern).andThen(_.map(_.mkString("")))
  }

  def forPattern(pattern: String): CharSequence => Option[List[String]] =
    pattern.r.unapplySeq(_: CharSequence)

}
