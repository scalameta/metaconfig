import java.util.Date
import sbtcrossproject.CrossPlugin.autoImport.crossProject
import com.typesafe.tools.mima.core._

lazy val V = new {
  def munit = "1.0.0"
  def scalacheck = "1.18.0"
}
val scala212 = "2.12.19"

val scala213 = "2.13.14"

val scala3 = "3.3.3"

val ScalaVersions = List(scala213, scala212, scala3)
inThisBuild(
  List(
    useSuperShell := false,
    organization := "org.scalameta",
    version ~= { old => old.replace('+', '-') },
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    homepage := Some(url("https://github.com/scalameta/metaconfig")),
    autoAPIMappings := true,
    apiURL := Some(url("https://github.com/scalameta/metaconfig")),
    developers += Developer(
      "olafurpg",
      "Ólafur Páll Geirsson",
      "olafurpg@gmail.com",
      url("https://geirsson.com")
    ),
    resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
    versionScheme := Some("early-semver")
  )
)

addCommandAlias(
  "scalafixAll",
  s"; ++$scala212 ; scalafixEnable ; all scalafix test:scalafix"
)
addCommandAlias(
  "scalafixCheckAll",
  s"; ++$scala212 ;  scalafixEnable ; scalafix --check ; test:scalafix --check"
)

addCommandAlias(
  "native-image",
  "; tests/graalvm-native-image:packageBin ; taskready"
)

commands += Command.command("taskready") { s =>
  import scala.sys.process._
  "afplay /System/Library/Sounds/Hero.aiff".!
  s
}

lazy val warnUnusedImport = Def.setting {
  if (
    scalaVersion.value.startsWith("2.13") ||
    scalaVersion.value.startsWith("3.")
  ) "-Wunused:imports"
  else "-Ywarn-unused-import"
}

lazy val yRangePos = Def.setting {
  if (scalaVersion.value.startsWith("3.")) Seq.empty[String]
  else Seq("-Yrangepos")
}

val languageAgnosticCompatibilityPolicy: ProblemFilter = (problem: Problem) => {
  val (ref, fullName) = problem match {
    case problem: TemplateProblem => (problem.ref, problem.ref.fullName)
    case problem: MemberProblem => (problem.ref, problem.ref.fullName)
  }
  val public = ref.isPublic
  val include = fullName.startsWith("metaconfig.")
  val exclude = fullName.contains(".internal.") ||
    fullName.startsWith("metaconfig.cli")
  public && include && !exclude
}

lazy val sharedSettings = List[Setting[_]](
  scalacOptions ++= yRangePos.value,
  scalacOptions ++= Seq(
    "-deprecation",
    warnUnusedImport.value
  ),
  mimaBinaryIssueFilters ++= List[ProblemFilter](
    languageAgnosticCompatibilityPolicy
  ),
  crossScalaVersions := ScalaVersions,
  scalaVersion := scala213
)

lazy val mimaSettings = List[Setting[_]](
  mimaPreviousArtifacts := Set("com.geirsson" %% moduleName.value % "0.9.10")
)

publish / skip := true
disablePlugins(MimaPlugin)

lazy val pprint = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("metaconfig-pprint"))
  .settings(
    sharedSettings,
    mimaSettings,
    moduleName := "metaconfig-pprint",
    libraryDependencies += "com.lihaoyi" %%% "fansi" % "0.5.0",
    libraryDependencies ++= {
      if (scalaVersion.value.startsWith("2."))
        List(
          "org.scala-lang" % "scala-reflect" % scalaVersion.value,
          "org.scala-lang" % "scala-compiler" % scalaVersion.value
        )
      else Nil
    }
  )

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("metaconfig-core"))
  .settings(
    sharedSettings,
    mimaSettings,
    moduleName := "metaconfig-core",
    libraryDependencies ++= List(
      "org.typelevel" %%% "paiges-core" % "0.4.4",
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.12.0"
    )
  )
  .settings(
    libraryDependencies += {
      if (scalaVersion.value.startsWith("3."))
        "org.scala-lang" % "scala-reflect" % scala213
      else "org.scala-lang" % "scala-reflect" % scalaVersion.value
    },
    Compile / unmanagedSourceDirectories ++= {
      // TODO: why isn't sbt-crossproject adding epoch scala version
      // sources? it should
      if (scalaVersion.value.startsWith("2"))
        Seq(
          (ThisBuild / baseDirectory).value / "metaconfig-core" / "shared" / "src" / "main" / "scala-2"
        )
      else Seq.empty
    }
  )
  .dependsOn(pprint)

lazy val typesafe = project
  .in(file("metaconfig-typesafe-config"))
  .settings(
    sharedSettings,
    mimaSettings,
    moduleName := "metaconfig-typesafe-config",
    description := "Integration for HOCON using typesafehub/config.",
    libraryDependencies += "com.typesafe" % "config" % "1.4.3"
  )
  .dependsOn(core.jvm)

lazy val sconfig = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("metaconfig-sconfig"))
  .settings(
    sharedSettings,
    mimaSettings,
    moduleName := "metaconfig-sconfig",
    description := "Integration for HOCON using ekrich/sconfig.",
    libraryDependencies ++= List(
      "org.ekrich" %%% "sconfig" % "1.7.0"
    )
  )
  .platformsSettings(JSPlatform, NativePlatform)(
    libraryDependencies ++= List(
      "org.ekrich" %%% "sjavatime" % "1.3.0"
    )
  )
  .dependsOn(core)

lazy val tests = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("metaconfig-tests"))
  .disablePlugins(MimaPlugin)
  .settings(
    sharedSettings,
    publish / skip := true,
    Compile / packageDoc / publishArtifact := false,
    testFrameworks := List(new TestFramework("munit.Framework")),
    libraryDependencies ++= List(
      "org.scalacheck" %%% "scalacheck" % V.scalacheck,
      "org.scalameta" %%% "munit-scalacheck" % V.munit % Test
    )
  )
  .jsSettings(
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule))
  )
  .jvmSettings(
    GraalVMNativeImage / mainClass := Some("metaconfig.tests.ExampleMain"),
    Compile / doc / sources := Seq.empty,
    libraryDependencies ++= {
      if (scalaVersion.value.startsWith("2."))
        Seq(
          "com.github.alexarchambault" %%% "scalacheck-shapeless_1.15" % "1.3.0"
        )
      else Seq("org.typelevel" %% "shapeless3-deriving" % "3.4.1")
    },
    graalVMNativeImageOptions ++= {
      val reflectionFile =
        (Compile / Keys.sourceDirectory).value / "graal" / "reflection.json"
      assert(reflectionFile.exists, "no such file: " + reflectionFile)
      List(
        "-H:+ReportUnsupportedElementsAtRuntime",
        "--initialize-at-build-time",
        "--initialize-at-run-time=metaconfig",
        "--no-server",
        "--enable-http",
        "--enable-https",
        "-H:EnableURLProtocols=http,https",
        "--enable-all-security-services",
        "--no-fallback",
        s"-H:ReflectionConfigurationFiles=$reflectionFile",
        "--allow-incomplete-classpath",
        "-H:+ReportExceptionStackTraces"
      )
    }
  )
  .jvmConfigure(
    _.enablePlugins(GraalVMNativeImagePlugin)
      .dependsOn(typesafe, sconfig.jvm)
  )
  .dependsOn(core)

lazy val docs = project
  .in(file("metaconfig-docs"))
  .settings(
    sharedSettings,
    libraryDependencies ++= List(
      "com.lihaoyi" %%% "scalatags" % "0.13.1",
      "org.scalacheck" %%% "scalacheck" % V.scalacheck,
      "org.scalameta" %%% "munit-scalacheck" % V.munit % Test
    ),
    moduleName := "metaconfig-docs",
    mdocVariables := Map(
      "VERSION" -> version.value.replaceFirst("\\+.*", ""),
      "SCALA_VERSION" -> scalaVersion.value
    ),
    mdocOut := (ThisBuild / baseDirectory).value / "website" / "target" / "docs",
    mdocExtraArguments := List("--no-link-hygiene"),
    // mdoc's metaconfig might (and will eventually) lag behind the current version, causing eviction errors
    evictionErrorLevel := Level.Warn
  )
  .dependsOn(core.jvm, typesafe, sconfig.jvm)
  .enablePlugins(DocusaurusPlugin)
  .disablePlugins(MimaPlugin)
