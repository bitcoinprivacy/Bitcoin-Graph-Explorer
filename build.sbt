organization := "sagesex"

name := "Bitcoin Graph Explorer"

version := "0.5"

scalaVersion := "2.9.1"

// additional libraries
libraryDependencies ++= Seq(
	"org.scala-tools.testing" % "specs_2.9.0" % "1.6.8" % "test", // For specs.org tests
	"org.scalatest" % "scalatest_2.9.0" % "1.6.1", // scalatest
	"junit" % "junit" % "4.8" % "test->default", // For JUnit 4 testing
	"ch.qos.logback" % "logback-classic" % "0.9.26" % "compile->default" // Logging
)