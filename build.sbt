import java.util.Date
import sbtcrossproject.CrossPlugin.autoImport.crossProject
val scala211 = "2.11.12"
val scala212 = "2.12.8"
val scala213 = "2.13.0"
val ScalaVersions = List(scala211, scala212, scala213)
inThisBuild(
  List(
    organization := "com.geirsson",
    version ~= { old =>
      old.replace('+', '-')
    },
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    homepage := Some(url("https://github.com/olafurpg/metaconfig")),
    autoAPIMappings := true,
    apiURL := Some(url("https://github.com/olafurpg/metaconfig")),
    developers += Developer(
      "olafurpg",
      "Ólafur Páll Geirsson",
      "olafurpg@gmail.com",
      url("https://geirsson.com")
    ),
    scalaVersion := ScalaVersions.head,
    crossScalaVersions := ScalaVersions,
    resolvers += Resolver.sonatypeRepo("snapshots")
  )
)

lazy val testSettings = List(
  testOptions.in(Test) +=
    Tests.Argument(TestFrameworks.ScalaCheck, "-verbosity", "2"),
  libraryDependencies ++= {
    if (SettingKey[Boolean]("nativeLinkStubs").?.value.contains(true))
      List(
        "org.scalatest" %%% "scalatest" % "3.2.0-SNAP10" % Test,
        "com.github.lolgab" %%% "scalacheck" % "1.14.1" % Test
      )
    else
      List(
        "org.scalatest" %%% "scalatest" % "3.0.8" % Test,
        "org.scalacheck" %%% "scalacheck" % "1.14.0" % Test,
        "com.github.alexarchambault" %%% "scalacheck-shapeless_1.14" % "1.2.3" % Test
      )
  }
)

lazy val nativeSettings = List(
  nativeLinkStubs := true,
  scalaVersion := scala211,
  crossScalaVersions := List(scala211),
  test.in(Test) := {}
)

skip.in(publish) := true

lazy val docs = project
  .settings(
    moduleName := "metaconfig-docs",
    libraryDependencies += (CrossVersion
      .partialVersion(scalaVersion.value) match {
      case Some((2, 11 | 12)) => "com.lihaoyi" %% "scalatags" % "0.6.7"
      case _ => "com.lihaoyi" %% "scalatags" % "0.7.0"
    })
  )
  .dependsOn(coreJVM)

lazy val json = project
  .in(file("metaconfig-json"))
  .settings(
    testSettings,
    moduleName := "metaconfig-json",
    libraryDependencies ++= List(
      "org.scalameta" %% "testkit" % "4.1.12" % Test
    ) :+ (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11 | 12)) => "com.lihaoyi" %%% "upickle" % "0.7.4"
      case _ => "com.lihaoyi" %% "upickle" % "0.7.5"
    })
  )
  .dependsOn(coreJVM)

lazy val website = project
  .settings(
    crossScalaVersions := List(scala212),
    skip.in(publish) := true,
    tutNameFilter := "README.md".r,
    tutSourceDirectory := baseDirectory.in(ThisBuild).value / "docs",
    sourceDirectory.in(Preprocess) := tutTargetDirectory.value,
    sourceDirectory.in(GitBook) := target.in(Preprocess).value,
    preprocessVars in Preprocess := Map(
      "VERSION" -> version.value.replaceAll("-.*", ""),
      "DATE" -> new Date().toString
    ),
    siteSourceDirectory := target.in(GitBook).value,
    makeSite := makeSite.dependsOn(tut, compile.in(Compile)).value,
    ghpagesPushSite := ghpagesPushSite.dependsOn(makeSite).value,
    git.remoteRepo := "git@github.com:olafurpg/metaconfig.git"
  )
  .enablePlugins(
    GhpagesPlugin,
    PreprocessPlugin,
    GitBookPlugin,
    TutPlugin
  )
  .dependsOn(
    docs,
    json,
    typesafe
  )

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("metaconfig-core"))
  .settings(
    testSettings,
    moduleName := "metaconfig-core",
    libraryDependencies ++= List(
      "org.typelevel" %%% "paiges-core" % "0.2.4",
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.1.2",
      scalaOrganization.value % "scala-reflect" % scalaVersion.value % Provided
    ) :+ (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11 | 12)) => "com.lihaoyi" %%% "pprint" % "0.5.3"
      case _ => "com.lihaoyi" %%% "pprint" % "0.5.5"
    })
  )
  .nativeSettings(nativeSettings)
  .jvmSettings(
    mimaPreviousArtifacts := {
      // TODO(olafur) enable mima check in CI after 0.6.0 release.
      val previousArtifactVersion = "0.6.0"
      val binaryVersion =
        if (crossVersion.value.isInstanceOf[CrossVersion.Full])
          scalaVersion.value
        else scalaBinaryVersion.value
      Set(
        organization.value % s"${moduleName.value}_$binaryVersion" % previousArtifactVersion
      )
    },
    mimaBinaryIssueFilters ++= Mima.ignoredABIProblems,
    libraryDependencies += "org.scalameta" %% "testkit" % "4.1.12" % Test
  )
lazy val coreJVM = core.jvm
lazy val coreJS = core.js
lazy val coreNative = core.native

lazy val typesafeConfig = "com.typesafe" % "config" % "1.2.1"

lazy val typesafe = project
  .in(file("metaconfig-typesafe-config"))
  .settings(
    testSettings,
    moduleName := "metaconfig-typesafe-config",
    description := "Integration for HOCON using typesafehub/config.",
    libraryDependencies += typesafeConfig
  )
  .dependsOn(coreJVM % "test->test;compile->compile")

lazy val sconfig = crossProject(JVMPlatform, NativePlatform)
  .in(file("metaconfig-sconfig"))
  .settings(
    testSettings,
    moduleName := "metaconfig-sconfig",
    description := "Integration for HOCON using ekrich/sconfig.",
    libraryDependencies ++= List(
      "org.ekrich" %%% "sconfig" % "1.0.0"
    )
  )
  .nativeSettings(nativeSettings)
  .dependsOn(core % "test->test;compile->compile")
lazy val sconfigJVM = sconfig.jvm
lazy val sconfigNative = sconfig.native
