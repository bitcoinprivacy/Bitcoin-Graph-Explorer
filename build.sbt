organization := "sagesex"

name := "Bitcoin Graph Explorer"

version := "0.5"

scalaVersion := "2.9.1.final"

// additional libraries
libraryDependencies ++= Seq(
	"org.scala-tools.testing" % "specs_2.9.0" % "1.6.8" % "test", // For specs.org tests
	"org.scalatest" % "scalatest_2.9.1" % "1.6.1", // scalatest
//	"junit" % "junit" % "4.8" % "test->default", // For JUnit 4 testing
	"ch.qos.logback" % "logback-classic" % "0.9.26" % "compile->default", // Logging
	"com.google" % "bitcoinj" % "0.3-SNAPSHOT",
	"org.neo4j" % "neo4j-scala" % "0.9.9-SNAPSHOT",
        "org.neo4j" % "neo4j" % "1.5"
)

resolvers += "bitcoinj" at "http://nexus.bitcoinj.org/content/repositories/snapshots"

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"