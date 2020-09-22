package migration

import java.nio.file.{Files, Path}
import java.util.zip.ZipInputStream

import eie.io._

import scala.util.{Try, Using}

object Unzip {

  def to(zip: Path, dest: Path)(
    fileNameForEntry: String => String): Try[Path] = {

    val useZip: Try[Unit] =
      Using(new ZipInputStream(Files.newInputStream(zip))) {
        in: ZipInputStream =>
          var entry = in.getNextEntry()
          while (entry != null) {
            try {
              if (!entry.isDirectory) {
                val target = dest.resolve(fileNameForEntry(entry.getName))
                if (!target.exists()) {
                  target.mkParentDirs()
                  Files.copy(in, target)
                }
              }
            } finally {
              in.closeEntry()
              entry = in.getNextEntry()
            }
          }
      }
    useZip.map(_ => dest)
  }
}
