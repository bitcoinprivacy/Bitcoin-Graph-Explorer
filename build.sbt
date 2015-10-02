organization := "sagesex"

name := "Bitcoin Graph Explorer"

version := "2.0"

scalaVersion := "2.11.6"

// additional libraries
libraryDependencies ++= Seq(
 //	"org.scala-tools.testing" % "specs_2.9.0" % "1.6.8" % "test", // For specs.org tests
//	"org.scalatest" % "scalatest_2.9.1" % "1.6.1", // scalatest
//	"junit" % "junit" % "4.8" % "test->default", // For JUnit 4 testing
//      "ch.qos.logback" % "logback-classic" % "0.9.26" % "compile->default", // Logging
    "org.slf4j" % "slf4j-simple" % "1.7.5",
        "org.bitcoinj" % "bitcoinj-core" % "0.13.1",
//	"org.neo4j" % "neo4j-scala" % "0.2.0-M2-SNAPSHOT",
  //  "org.iq80.leveldb"%"leveldb"%"0.6",
"org.postgresql" % "postgresql" % "9.4-1200-jdbc41",
  //"org.xerial" % "sqlite-jdbc" % "3.7.15-M1",
  //"org.mariadb.jdbc" % "mariadb-java-client" % "1.1.8",
     // "com.sagesex" %% "json-rpc-client" % "0.0.1",
   // "org.scala-lang" % "scala-actors" % "2.10.3",
    "com.typesafe.slick" %% "slick" % "2.1.0",
  "com.typesafe" % "config" % "1.2.1",
      //"com.typesafe.play" %% "play" % "2.2.0"
      "org.scalacheck" %% "scalacheck" % "1.12.4" % "test",
      "org.scalatest" %% "scalatest" % "2.1.5" % "test",
  "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.3-1",
  "org.deephacks.lmdbjni" % "lmdbjni" % "0.4.2",
      "org.deephacks.lmdbjni" % "lmdbjni-linux64" % "0.4.2"
  //  "org.mapdb" % "mapdb" % "2.0-alpha2"
//  "net.openhft" % "koloboke-api-jdk6-7" % "0.6.7",
// "net.openhft" % "koloboke-impl-jdk6-7" % "0.6.7"
//  "com.github.vlsi.compactmap" % "compactmap" % "1.2.1"

)

resolvers += "Local Maven Repository" at "file:///"+Path.userHome.absolutePath+"/.m2/repository"

resolvers += "Fakod Snapshots" at "https://raw.github.com/FaKod/fakod-mvn-repo/master/snapshots"

resolvers += "neo4j" at "http://m2.neo4j.org"

resolvers += "bitcoinj" at "http://distribution.bitcoinj.googlecode.com/git/releases"

resolvers += "scala-tools" at "https://oss.sonatype.org/content/groups/scala-tools"

resolvers += "openhft" at "https://oss.sonatype.org/content/groups/public"

resolvers += "sonatype" at "https://oss.sonatype.org/content/repositories/releases/"

packageOptions in (Compile, packageBin) <+= (target, externalDependencyClasspath in Runtime) map
 { (targetDirectory: File, classpath: Classpath) =>
  val relativePaths = classpath map { attrFile: Attributed[File] => targetDirectory.toPath().relativize(attrFile.data.toPath()).toString() };
  Package.ManifestAttributes(java.util.jar.Attributes.Name.CLASS_PATH -> relativePaths.reduceOption(_ + " " + _).getOrElse(""))
 }

javaOptions in run += "-Xmx8G"


javaOptions in run += "-Xms1G"

javaOptions in run += "-Dcom.sun.management.jmxremote.port=3333"
javaOptions in run += "-Dcom.sun.management.jmxremote.authenticate=false"
javaOptions in run += "-Dcom.sun.management.jmxremote.ssl=false"



//javaOptions in run += "-XX:+UseParallelGC"

//javaOptions in run += "-XX:-UseGCOverheadLimit"

//javaOptions in run += "-XX:+UseStringDeduplicationJVM"

//javaOptions in run += "-XX:+UseG1GC"

//javaOptions in run += "-XX:+PrintCommandLineFlags"

//javaOptions in run += "-XX:+PrintGCDetails"

fork in run := true
