package migration

import args4c.implicits._
import com.typesafe.config.Config
import eie.io._

class ParsedConfig(config: Config) {

  val fileNameRegex = config.getString("fileNameRegex")
  val fileNamePattern = config.getString("fileNamePattern")
  val url = config.getString("url")
  val targetDirectory = config.getString("dir").asPath
  val dryRun = config.getBoolean("dryRun")
  val checkDownloadStatus = config
    .asList("checkDownloadStatus")
    .toSet
    .filterNot(_.trim.isEmpty)
    .map(_.toInt)
  val indexURL = config.getString("indexURL") match {
    case name if !name.contains("/") =>
      if (url.endsWith("/")) s"${url}$name" else s"$url/$name"
    case name if name.startsWith("/") =>
      if (url.endsWith("/")) s"${url}${name.tail}" else s"$url$name"
    case url => url
  }

  override def toString: String = {
    s"""               URL : $url
       |          indexURL : $indexURL
       |            dryRun : $dryRun
       |         directory : $targetDirectory
       |  filename pattern : s/$fileNameRegex/$fileNamePattern/g""".stripMargin
  }
}

object ParsedConfig {
  def apply(config: Config) = new ParsedConfig(config)
}
