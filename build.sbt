organization := "net.bitcoinprivacy"

name := "bge"

version := "3.3-SNAPSHOT"

scalaVersion := "2.11.8"

maintainer := "Bitcoinprivacy <info@bitcoinprivacy.net>"

// additional libraries
libraryDependencies ++= Seq(
  "org.bitcoinj" % "bitcoinj-core" % "0.15-SNAPSHOT",
  "org.xerial.snappy"%"snappy-java"%"1.1.2.4",
  "org.iq80.leveldb"%"leveldb"%"0.7",
  "org.fusesource.leveldbjni"%"leveldbjni-all"%"1.8",
  "org.postgresql" % "postgresql" % "9.4-1200-jdbc41",
  "com.typesafe.slick" %% "slick" % "2.1.0",
  "com.typesafe" % "config" % "1.2.1",
  "org.scalacheck" %% "scalacheck" % "1.12.4" % "test",
  "org.scalatest" %% "scalatest" % "2.1.5" % "test",
  "org.deephacks.lmdbjni" % "lmdbjni" % "0.4.6",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
  "ch.qos.logback" %  "logback-classic" % "1.1.7",
  ////////////////////////////////////////////////////////////////////
  // select one of the following depending on your architecture
  //"org.deephacks.lmdbjni" % "lmdbjni-osx64" % "0.4.6"
  "org.deephacks.lmdbjni" % "lmdbjni-linux64" % "0.4.6"
////////////////////////////////////////////////////////////////////

)

enablePlugins(JavaAppPackaging)

libraryDependencies ~= { _.map(_.exclude("org.slf4j", "slf4j-simple")) }

resolvers += "Local Maven Repository" at "file:///root/.m2/repository"

resolvers += "Fakod Snapshots" at "https://raw.github.com/FaKod/fakod-mvn-repo/master/snapshots"

resolvers += "neo4j" at "http://m2.neo4j.org"

resolvers += "bitcoinj" at "http://distribution.bitcoinj.googlecode.com/git/releases"

resolvers += "scala-tools" at "https://oss.sonatype.org/content/groups/scala-tools"

resolvers += "openhft" at "https://oss.sonatype.org/content/groups/public"

resolvers += "sonatype" at "https://oss.sonatype.org/content/repositories/releases/"

// replicated the original sbt merge strategy to pick the first file per default

assemblyMergeStrategy in assembly := { 
case x if Assembly.isConfigFile(x) =>
    MergeStrategy.concat
  case PathList(ps @ _*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
    MergeStrategy.rename
  case PathList("META-INF", xs @ _*) =>
    (xs map {_.toLowerCase}) match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
        MergeStrategy.discard
      case ps @ (x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
        MergeStrategy.discard
      case "plexus" :: xs =>
        MergeStrategy.discard
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines
      case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
        MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.deduplicate

    }
  case _ => MergeStrategy.first
  }


scalacOptions ++= Seq(
  "-encoding",
  "UTF-8",
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:postfixOps",
  "-language:implicitConversions"
    ,"-Xcheckinit"
    ,"-target:jvm-1.8"
//    ,"-Xlog-implicit-conversions"
)

javaOptions in run += "-Xmx16G"
javaOptions in run += "-Xms1G"

fork := true
