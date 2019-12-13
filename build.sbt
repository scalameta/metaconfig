inThisBuild(List(
  scalaVersion := "2.12.10",
  organization := "com.geirsson",
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
