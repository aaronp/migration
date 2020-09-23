import org.scoverage.coveralls.Imports.CoverallsKeys._
import eie.io._

ThisBuild / organization := "aaronp"
ThisBuild / scalaVersion := "2.13.3"

val projectName = "migration"
val username = "aaronp"
val scalaTwelve = "2.12.10"
val scalaThirteen = "2.13.3"
val defaultScalaVersion = scalaThirteen

name := projectName

organization := s"com.github.$username"
scalaVersion := defaultScalaVersion
crossScalaVersions := Seq(scalaThirteen)

mainClass in (Compile, run) := Some("migration.Main")
mainClass in (assembly) := Some("migration.Main")

libraryDependencies ++= List(
  "com.typesafe" % "config" % "1.3.4",
  "com.github.aaronp" %% "args4c" % "0.7.0",
  "com.lihaoyi" %% "requests" % "0.6.5",
  "dev.zio" %% "zio" % "1.0.1",
  "org.scala-lang.modules" %% "scala-xml" % "1.3.0",
  "com.github.aaronp" %% "eie" % "1.0.0"
)

libraryDependencies ++= List(
  "org.scalactic" %% "scalactic" % "3.2.0" % "test",
  "org.scalatest" %% "scalatest" % "3.2.0" % "test",
  "org.pegdown" % "pegdown" % "1.6.0" % "test",
  "junit" % "junit" % "4.12" % "test",
  "dev.zio" %% "zio-test" % "1.0.1" % "test",
  "dev.zio" %% "zio-test-sbt" % "1.0.1" % "test",
  "dev.zio" %% "zio-test-junit" % "1.0.1" % "test"
)

testFrameworks += (new TestFramework("zio.test.sbt.ZTestFramework"))

addArtifact(Artifact(projectName, "assembly"),
            sbtassembly.AssemblyKeys.assembly)

artifact in (Compile, assembly) ~= { art =>
  art.withClassifier(Some("assembly"))
}

publishMavenStyle := true
releaseCrossBuild := true
coverageMinimum := 90
coverageFailOnMinimum := true
git.remoteRepo := s"git@github.com:$username/migration.git"
releasePublishArtifactsAction := PgpKeys.publishSigned.value
publishConfiguration := publishConfiguration.value.withOverwrite(true)
publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)

// test in assembly := {}
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

// https://coveralls.io/github/aaronp/migration
// https://github.com/scoverage/sbt-coveralls#specifying-your-repo-token
coverallsTokenFile := Option(
  (Path.userHome / ".sbt" / ".coveralls.migration").asPath.toString)

buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)
buildInfoPackage := "migration.build"

// see http://scalameta.org/scalafmt/
scalafmtOnCompile in ThisBuild := true
scalafmtVersion in ThisBuild := "1.4.0"

pomExtra := {
  <url>https://github.com/{username}/{projectName}</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <developers>
      <developer>
        <id>{username}</id>
        <name>{username}</name>
        <url>http://github.com/{username}</url>
      </developer>
    </developers>
}
