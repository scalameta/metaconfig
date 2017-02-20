lazy val ScalaVersions = Seq("2.11.8", "2.12.1")
lazy val MetaVersion = "1.4.0"
lazy val ParadiseVersion = "3.0.0-167.1487606772986"
inThisBuild(
  Seq(
    organization := "com.geirsson",
    version := "0.1.0",
    resolvers += Resolver.bintrayIvyRepo("scalameta", "maven"),
    scalaVersion := ScalaVersions.head,
    crossScalaVersions := ScalaVersions,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % Test
  ) ++
    metaMacroSettings ++
    publishSettings
)

lazy val metaMacroSettings: Seq[Def.Setting[_]] = Seq(
  addCompilerPlugin(
    ("org.scalameta" % "paradise" % ParadiseVersion).cross(CrossVersion.full)),
  libraryDependencies += "org.scalameta" %% "scalameta" % MetaVersion % Provided,
  scalacOptions += "-Xplugin-require:macroparadise",
  scalacOptions in (Compile, console) := Seq(), // macroparadise plugin doesn't work in repl yet.
  sources in (Compile, doc) := Nil // macroparadise doesn't work with scaladoc yet.
)

lazy val publishSettings = Seq(
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (version.value.endsWith("-SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  licenses := Seq(
    "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/olafurpg/metaconfig")),
  autoAPIMappings := true,
  apiURL := Some(url("https://github.com/olafurpg/metaconfig")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/olafurpg/metaconfig"),
      "scm:git:git@github.com:olafurpg/metaconfig.git"
    )
  ),
  pomExtra :=
    <developers>
      <developer>
        <id>olafurpg</id>
        <name>Ólafur Páll Geirsson</name>
        <url>https://geirsson.com</url>
      </developer>
    </developers>
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
