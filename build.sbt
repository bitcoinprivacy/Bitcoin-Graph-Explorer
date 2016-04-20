organization := "net.bitcoinprivacy"

name := "bge"

version := "3.0"

scalaVersion := "2.11.8"

// additional libraries
libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-simple" % "1.7.5",
  "org.bitcoinj" % "bitcoinj-core" % "0.13.4",
  "org.xerial.snappy"%"snappy-java"%"1.1.2.4",
  "org.iq80.leveldb"%"leveldb"%"0.7",
  //"org.fusesource.leveldbjni"%"leveldbjni-all"%"1.8",
  "org.postgresql" % "postgresql" % "9.4-1200-jdbc41",
  "com.typesafe.slick" %% "slick" % "2.1.0",
  "com.typesafe" % "config" % "1.2.1",
  "org.scalacheck" %% "scalacheck" % "1.12.4" % "test",
  "org.scalatest" %% "scalatest" % "2.1.5" % "test",
  "org.deephacks.lmdbjni" % "lmdbjni" % "0.4.2",
  "org.deephacks.lmdbjni" % "lmdbjni-linux64" % "0.4.2"
  // change here for different architectures
)

resolvers += "Local Maven Repository" at "file:///"+Path.userHome.absolutePath+"/.m2/repository"

resolvers += "Fakod Snapshots" at "https://raw.github.com/FaKod/fakod-mvn-repo/master/snapshots"

resolvers += "neo4j" at "http://m2.neo4j.org"

resolvers += "bitcoinj" at "http://distribution.bitcoinj.googlecode.com/git/releases"

resolvers += "scala-tools" at "https://oss.sonatype.org/content/groups/scala-tools"

resolvers += "openhft" at "https://oss.sonatype.org/content/groups/public"

resolvers += "sonatype" at "https://oss.sonatype.org/content/repositories/releases/"

// packageOptions in (Compile, packageBin) <+= (target, externalDependencyClasspath in Runtime) map
//  { (targetDirectory: File, classpath: Classpath) =>
//   val relativePaths = classpath map { attrFile: Attributed[File] => targetDirectory.toPath().relativize(attrFile.data.toPath()).toString() };
//   Package.ManifestAttributes(java.util.jar.Attributes.Name.CLASS_PATH -> relativePaths.reduceOption(_ + " " + _).getOrElse(""))
//  }

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
javaOptions in run += "-Dcom.sun.management.jmxremote.port=3333"
javaOptions in run += "-Dcom.sun.management.jmxremote.authenticate=false"
javaOptions in run += "-Dcom.sun.management.jmxremote.ssl=false"
javaOptions in run += "-Djava.rmi.server.hostname=orion2518.startdedicated.de"

//lazy val api = (project in file(".")).enablePlugins(JettyPlugin)

//javaOptions in run += "-XX:+UseParallelGC"

//javaOptions in run += "-XX:-UseGCOverheadLimit"

//javaOptions in run += "-XX:+UseStringDeduplicationJVM"

//javaOptions in run += "-XX:+UseG1GC"

//javaOptions in run += "-XX:+PrintCommandLineFlags"

//javaOptions in run += "-XX:+PrintGCDetails"

fork := true
