import sbt._
import Keys._
import sbtprotobuf.{ProtobufPlugin=>PB}
import sbtassembly.Plugin._
import AssemblyKeys._

object Versions {
  val scala     = "2.10.1"
  val scalatest = "1.9.1"
}

object Build extends Build {
  
  lazy val google_play = Project("GooglePlay", file("."), settings =  marketCrawlerSettings)
  
  val projectName = "googleplaycrawler"
  val ver = "0.1-SNAPSHOT"

  lazy val marketCrawlerSettings = Defaults.defaultSettings ++ assemblySettings ++ PB.protobufSettings ++ Seq(
    name         := projectName,
    version      := ver,
    aggregate    := false,
    jarName in assembly := projectName + "-" + ver + ".jar",
    autoScalaLibrary in assembly <<=  (autoScalaLibrary in Global),
	assembleArtifact in packageScala := false,
    mainClass in assembly := Some("com.akdeniz.googleplaycrawler.cli.googleplay"),
    mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
      {
        case "application.conf" => MergeStrategy.concat
        case "reference.conf"   => MergeStrategy.concat
        case PathList(ps @ _*) if ps.last.toLowerCase.startsWith("notice") || ps.last.toLowerCase == "license" || ps.last.toLowerCase == "license.txt" => MergeStrategy.rename 
        case PathList("META-INF", xs @ _*) =>
          (xs map {_.toLowerCase}) match {
            case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
              MergeStrategy.discard
            case ps @ (x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa")  =>
              MergeStrategy.discard
            case "services" :: xs =>
              MergeStrategy.filterDistinctLines
            case _ => MergeStrategy.deduplicate
          }
        case _ => MergeStrategy.deduplicate
      }
    },
    version in PB.protobufConfig := "2.5.0",
    libraryDependencies ++= Seq(
      "org.apache.httpcomponents" % "httpclient" % "4.2.2",
      "commons-logging" % "commons-logging" % "1.1.1",
      "net.sourceforge.argparse4j" % "argparse4j" % "0.2.2",
      "org.apache.httpcomponents" % "httpcore" % "4.2.2",
      "org.apache.httpcomponents" % "httpclient-cache" % "4.2.2",
      "org.apache.httpcomponents" % "httpmime" % "4.2.2",
      "org.apache.mina" % "mina-core" % "2.0.7",
      "org.slf4j" % "slf4j-simple" % "1.7.2"
      )
  ) 
}
