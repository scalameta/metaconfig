lazy val ScalaVersions = Seq("2.11.11", "2.12.2")

organization in ThisBuild := "com.geirsson"
version in ThisBuild := customVersion.getOrElse(version.in(ThisBuild).value)
allSettings
noPublish

commands += Command.command("release") { s =>
  "clean" ::
    "sonatypeOpen metaconfig-release" ::
    "very publishSigned" ::
    "sonatypeReleaseAll" ::
    s
}

lazy val MetaVersion = "2.0.0-M3"

lazy val baseSettings = Seq(
  scalaVersion := ScalaVersions.head,
  crossScalaVersions := ScalaVersions,
  libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.13.5" % Test,
  libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.1" % Test
)

lazy val publishSettings = Seq(
  publishTo := Some(
    "releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"),
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
  developers +=
    Developer(
      "olafurpg",
      "Ólafur Páll Geirsson",
      "olafurpg@gmail.com",
      url("https://geirsson.com")
    )
)

lazy val allSettings = baseSettings ++ publishSettings

lazy val `metaconfig-core` = crossProject
  .settings(
    allSettings,
    // Position/Input
    libraryDependencies += "org.scalameta" %%% "inputs" % MetaVersion
  )
lazy val `metaconfig-coreJVM` = `metaconfig-core`.jvm
lazy val `metaconfig-coreJS` = `metaconfig-core`.js

lazy val typesafeConfig = "com.typesafe" % "config" % "1.2.1"

lazy val `metaconfig-typesafe-config` = project
  .settings(
    allSettings,
    description := "Integration for HOCON using typesafehub/config.",
    libraryDependencies += typesafeConfig
  )
  .dependsOn(`metaconfig-coreJVM` % "test->test;compile->compile")

lazy val `metaconfig-hocon` = crossProject
  .settings(
    allSettings,
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "fastparse" % "0.4.3"
    ),
    description := "EXPERIMENTAL Integration for HOCON using custom parser. On JVM, use metaconfig-typesafe-config."
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      typesafeConfig % Test
    )
  )
  .dependsOn(`metaconfig-core` % "test->test;compile->compile")
lazy val `metaconfig-hoconJVM` = `metaconfig-hocon`.jvm
lazy val `metaconfig-hoconJS` = `metaconfig-hocon`.js

lazy val noPublish = Seq(
  publishArtifact := false,
  publish := {},
  publishLocal := {}
)
def customVersion = sys.props.get("metaconfig.version")

inScope(Global)(
  Seq(
    credentials ++= (for {
      username <- sys.env.get("SONATYPE_USERNAME")
      password <- sys.env.get("SONATYPE_PASSWORD")
    } yield
      Credentials(
        "Sonatype Nexus Repository Manager",
        "oss.sonatype.org",
        username,
        password)).toSeq,
    PgpKeys.pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toCharArray())
  )
)
