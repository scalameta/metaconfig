inThisBuild(List(
  scalaVersion := "2.12.10",
  organization := "com.geirsson",
  homepage := Some(url("https://github.com/olafurpg/scalatest-framework")),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  developers := List(
    Developer(
      "olafurpg",
      "Ólafur Páll Geirsson",
      "olafurpg@gmail.com",
      url("https://geirsson.com")
    )
  ),
  useSuperShell := false
))
skip in publish := true
lazy val framework = project
  .settings(
    moduleName := "scalatest-framework",
    libraryDependencies ++= List(
      "org.scala-sbt" % "test-interface" % "1.0",
      "org.scalatest" %% "scalatest" % "3.0.8"
      )
    )
