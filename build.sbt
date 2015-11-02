name := """reactive-lab2"""

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.0",
  "com.typesafe.akka" % "akka-testkit_2.11" % "2.4.0" % "test",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test")
