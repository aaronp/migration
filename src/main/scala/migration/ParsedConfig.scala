package migration

import args4c.implicits._
import com.typesafe.config.Config
import eie.io._

class ParsedConfig(config: Config) {

  val fileNameRegex = config.getString("fileNameRegex")
  val fileNamePattern = config.getString("fileNamePattern")
  val url = config.getString("url")
  val baseDirectory = config.getString("dir").asPath
  val dataDirectory = baseDirectory.resolve("data")
  val downloadDirectory = baseDirectory.resolve("downloads")
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

  private val BaseNameR = ".*/(.*)".r

  def indexFileName: String = indexURL match {
    case BaseNameR(base) => base
    case other           => other
  }

  override def toString: String = {
    s"""               URL : $url
       |          indexURL : $indexURL
       |         directory : $baseDirectory
       |  filename pattern : s|$fileNameRegex|$fileNamePattern|g""".stripMargin
  }
}

object ParsedConfig {
  def apply(config: Config) = new ParsedConfig(config)
}
