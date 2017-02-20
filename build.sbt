lazy val ScalaVersions = Seq("2.11.8", "2.12.1")
lazy val MetaVersion = "3.0.0-167.1487606772986"
lazy val metaMacroSettings: Seq[Def.Setting[_]] = Seq(
  addCompilerPlugin(
    ("org.scalameta" % "paradise" % MetaVersion).cross(CrossVersion.full)),
  libraryDependencies += "org.scalameta" %% "scalameta" % "1.4.0" % Provided,
  scalacOptions += "-Xplugin-require:macroparadise",
  scalacOptions in (Compile, console) := Seq(), // macroparadise plugin doesn't work in repl yet.
  sources in (Compile, doc) := Nil // macroparadise doesn't work with scaladoc yet.
)

inThisBuild(
  Seq(
    organization := "com.geirsson",
    version := "0.1.0-SNAPSHOT",
    resolvers += Resolver.bintrayIvyRepo("scalameta", "maven"),
    scalaVersion := ScalaVersions.head,
    crossScalaVersions := ScalaVersions,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % Test
  ) ++ metaMacroSettings
)

lazy val `metaconfig-core` = project
  .settings(
    metaMacroSettings
  )

lazy val `metaconfig-hocon` = project
  .settings(
    libraryDependencies += "com.typesafe" % "config" % "1.3.1"
  )
  .dependsOn(`metaconfig-core`)
