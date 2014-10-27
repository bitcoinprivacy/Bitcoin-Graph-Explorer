organization := "sagesex"

name := "Bitcoin Graph Explorer"

version := "2.0"

scalaVersion := "2.11.2"

// additional libraries
libraryDependencies ++= Seq(
 //	"org.scala-tools.testing" % "specs_2.9.0" % "1.6.8" % "test", // For specs.org tests
//	"org.scalatest" % "scalatest_2.9.1" % "1.6.1", // scalatest
//	"junit" % "junit" % "4.8" % "test->default", // For JUnit 4 testing
//	"ch.qos.logback" % "logback-classic" % "0.9.26" % "compile->default", // Logging
    "org.slf4j" % "slf4j-simple" % "1.7.5",
	"org.bitcoinj" % "bitcoinj-core" % "0.12",
//	"org.neo4j" % "neo4j-scala" % "0.2.0-M2-SNAPSHOT",
  //  "org.iq80.leveldb"%"leveldb"%"0.6",
    //"mysql"%"mysql-connector-java"%"5.1.26",
  "org.xerial" % "sqlite-jdbc" % "3.7.15-M1",
     // "com.sagesex" %% "json-rpc-client" % "0.0.1",
   // "org.scala-lang" % "scala-actors" % "2.10.3",
    "com.typesafe.slick" %% "slick" % "2.1.0",
  "com.typesafe" % "config" % "1.2.1",
      //"com.typesafe.play" %% "play" % "2.2.0"
      "org.scalacheck" %% "scalacheck" % "1.11.3" % "test",
      "org.scalatest" %% "scalatest" % "2.1.5" % "test" 
)

resolvers += "Local Maven Repository" at "file:///"+Path.userHome.absolutePath+"/.m2/repository"

resolvers += "Fakod Snapshots" at "https://raw.github.com/FaKod/fakod-mvn-repo/master/snapshots"

resolvers += "neo4j" at "http://m2.neo4j.org"

resolvers += "bitcoinj" at "httpexportJars := true://distribution.bitcoinj.googlecode.com/git/releases"

resolvers += "scala-tools" at "https://oss.sonatype.org/content/groups/scala-tools"


packageOptions in (Compile, packageBin) <+= (target, externalDependencyClasspath in Runtime) map
 { (targetDirectory: File, classpath: Classpath) =>
  val relativePaths = classpath map { attrFile: Attributed[File] => targetDirectory.toPath().relativize(attrFile.data.toPath()).toString() }; 
  Package.ManifestAttributes(java.util.jar.Attributes.Name.CLASS_PATH -> relativePaths.reduceOption(_ + " " + _).getOrElse(""))
 }

javaOptions in run += "-Xmx1G"

javaOptions in run += "-Xms512M"

javaOptions in run += "-XX:-UseGCOverheadLimit"
//javaOptions in run += "-XX:+PrintCommandLineFlags"

//javaOptions in run += "-XX:+PrintGCDetails"

fork in run := true
