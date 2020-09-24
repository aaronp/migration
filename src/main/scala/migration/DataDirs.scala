package migration

import args4c.implicits._
import eie.io._

object DataDirs extends App {
  def children(cmdLine: Array[String] = args) =
    ParsedConfig(cmdLine.asConfig().resolve()).dataDirectory.children
      .sortBy(_.fileName)
      .mkString("\n")
  println(children())
}
