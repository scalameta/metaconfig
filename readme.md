# Metaconfig

[![Latest version](https://index.scala-lang.org/olafurpg/metaconfix/metaconfig-core/latest.svg)](https://index.scala-lang.org/olafurpg/metaconfig/metaconfig-core) 
[![Travis](https://travis-ci.org/olafurpg/metaconfig.svg?branch=master)](https://travis-ci.org/olafurpg/metaconfig)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/metaconfig/Lobby)

Aim of metaconfig:

- Untie case class from configuration format (hocon, yaml, toml, ...)
- User does not need to define all fields of case class
- Missing fields use default value from a runtime instance of of case class. Note, any runtime value, not only fallback to default parameter value in case class definition.
- Unknown fields in configuration file trigger error with useful message, e.g., "Invalid field 'maxs' in class T, did you mean 'max'?"
- Scala.js support, no runtime reflection

To use metaconfig:
```scala
libraryDependencies += "com.geirsson" % "metaconfig-core" % "latest.integration"
libraryDependencies += "com.geirsson" % "metaconfig-hocon" % "latest.integration"

// pass settings to projects using @ConfigReader annotation.
lazy val metaMacroSettings: Seq[Def.Setting[_]] = Seq(
  addCompilerPlugin(
    ("org.scalameta" % "paradise" % ParadiseVersion).cross(CrossVersion.full)),
  libraryDependencies += "org.scalameta" %% "scalameta" % MetaVersion % Provided,
  scalacOptions += "-Xplugin-require:macroparadise",
  scalacOptions in (Compile, console) := Seq(), // macroparadise plugin doesn't work in repl yet.
  sources in (Compile, doc) := Nil // macroparadise doesn't work with scaladoc yet.
)
```

## Example

Using `metaconfig-hocon`
```scala
@metaconfig.ConfigReader
case class MyConfig(
    a: Int,
    b: String
)
val default = MyConfig(22, "banana")
val config =
  """
    |a = 666
  """.stripMargin

test("field 'a' is overwritten") {
  val Right(obtained) =
    metaconfig.hocon.Hocon2Class.gimmeClass[MyConfig](config, default.reader)
  val expected = default.copy(a = 666)
  assert(obtained == expected)
}
```

## Modules

- `metaconfig-core` provides the `@metaconfig.ConfigReader` macro annotation to automatically generate a reader from a `Map[String, Any]` to your case class.
- `metaconfig-hocon` provides a way to convert `com.typesafe.Config` to `Map[String, Any]`.

## Used by

* scalafmt to read [over 60 configuration options](https://olafurpg.github.io/scalafmt/#Other)
* scalafix (soon)

## Alternatives

There are ton of great alternatives to metaconfig, to name a few:

- [Pureconfig](https://github.com/melrief/pureconfig)
- [Ficus](https://github.com/iheartradio/ficus)
- [Circe](https://github.com/circe/circe) "patch" readers, see [this SO answer](http://stackoverflow.com/a/39639397/1469245)

I did not find an alternative that fits all requirements in the "aim of metaconfig" listed above.
I also wanted to experiment with new-style macro annotations using scala.meta.

