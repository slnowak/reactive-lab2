name := """reactive-lab2"""

version := "1.0"

scalaVersion := "2.11.6"

resolvers += "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.0",
  "com.typesafe.akka" %% "akka-persistence" % "2.4.0",
  "com.typesafe.akka" %% "akka-remote" % "2.4.0",
  "org.iq80.leveldb" % "leveldb" % "0.7",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "com.typesafe.akka" % "akka-testkit_2.11" % "2.4.0" % "test",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
)
