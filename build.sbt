import java.util.Date
import sbtcrossproject.{crossProject, CrossType}
lazy val ScalaVersions = Seq("2.11.12", "2.12.7")
def customVersion = sys.props.get("metaconfig.version")
inThisBuild(
  List(
    organization := "com.geirsson",
    version ~= { old =>
      customVersion.getOrElse(old).replace('+', '-')
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
    crossScalaVersions := ScalaVersions
  )
)

lazy val testSettings = List(
  testOptions.in(Test) +=
    Tests.Argument(TestFrameworks.ScalaCheck, "-verbosity", "2"),
  libraryDependencies ++= List(
    "org.scalatest" %%% "scalatest" % "3.0.5" % Test,
    "org.scalacheck" %%% "scalacheck" % "1.14.0" % Test,
    "com.github.alexarchambault" %%% "scalacheck-shapeless_1.13" % "1.1.6" % Test
  )
)

skip.in(publish) := true

lazy val docs = project
  .settings(
    skip.in(publish) := true,
    libraryDependencies ++= List(
      "com.lihaoyi" %% "scalatags" % "0.6.7"
    )
  )
  .dependsOn(coreJVM)

lazy val json = project
  .in(file("metaconfig-json"))
  .settings(
    testSettings,
    moduleName := "metaconfig-json",
    libraryDependencies ++= List(
      "com.lihaoyi" %%% "ujson" % "0.6.5",
      "org.scalameta" %% "testkit" % "4.0.0-M11" % Test
    )
  )
  .dependsOn(coreJVM)

lazy val website = project
  .settings(
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

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .in(file("metaconfig-core"))
  .settings(
    testSettings,
    moduleName := "metaconfig-core",
    libraryDependencies ++= List(
      "com.lihaoyi" %%% "pprint" % "0.5.3",
      "org.typelevel" %%% "paiges-core" % "0.2.0",
      scalaOrganization.value % "scala-reflect" % scalaVersion.value % Provided
    )
  )
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
    libraryDependencies += "org.scalameta" %% "testkit" % "3.7.3" % Test
  )
lazy val coreJVM = core.jvm
lazy val coreJS = core.js

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

lazy val hocon = crossProject(JVMPlatform, JSPlatform)
  .in(file("metaconfig-hocon"))
  .settings(
    testSettings,
    moduleName := "metaconfig-hocon",
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
  .dependsOn(core % "test->test;compile->compile")
lazy val hoconJVM = hocon.jvm
lazy val hoconJS = hocon.js
