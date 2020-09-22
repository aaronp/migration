package migration

import args4c.implicits._
import com.typesafe.config.Config
import eie.io._

class ParsedConfig(config: Config) {

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
    case "/index.txt" => if (url.endsWith("/")) s"${url}index.txt" else s"$url/index.txt"
    case url => url
  }

  override def toString: String = {
    s"""      URL : $url
       | indexURL : $indexURL
       |   dryRun : $dryRun
       |directory : $targetDirectory
       |  pattern : $fileNamePattern
       |""".stripMargin
  }
}

object ParsedConfig {
  def apply(config: Config) = new ParsedConfig(config)
}

