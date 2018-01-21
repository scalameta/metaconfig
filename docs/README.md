# Metaconfig

Metaconfig is a library to read HOCON configuration into Scala case classes.
Key features of Metaconfig include

* helpful error messages on common mistakes like typos or type mismatch (expected string, obtained int)
* configurable, semi-automatic derivation of decoders, with support for deprecating setting options
* cross-platform, supports JS/JVM. Native support is on the roadmap

The target use-case for metaconfig is tool maintainers who support HOCON configuration in their tool.
Metaconfig is used by scalafmt to read `.scalafmt.conf` and scalafix to read `.scalafix.conf`.
With metaconfig, tool maintainers should be able to safely evolve their configuration (deprecate old fields, add new fields) without breaking existing configuration files.
Users should get helpful error messages when they mistype a setting name.

There are alternatives to metaconfig that you might want to give a try first

* https://github.com/circe/circe-config
* https://github.com/pureconfig/pureconfig

## Getting started

```scala
libraryDependencies += "com.geirsson" %% "metaconfig-core" % "@VERSION@"

// Use https://github.com/lightbend/config to parse HOCON
libraryDependencies += "com.geirsson" %% "metaconfig-typesafe-config" % "@VERSION@"
```

Use this import to access the metaconfig API

```tut:silent
import metaconfig._
```

All of the following code examples assume that you have `import metaconfig._` in scope.

<!-- TOC -->

* [Metaconfig](#metaconfig)
  * [Getting started](#getting-started)
  * [Conf](#conf)
  * [Conf.parse](#confparse)
  * [ConfDecoder.instance](#confdecoderinstance)
  * [ConfError](#conferror)
  * [Configured](#configured)
  * [generic.deriveSurface](#genericderivesurface)
  * [generic.deriveDecoder](#genericderivedecoder)
  * [DeprecatedName](#deprecatedname)

<!-- /TOC -->

## Conf

`Conf` is a JSON-like data structure that is the foundation of metaconfig.

```tut
val string = Conf.fromString("string")
val int = Conf.fromInt(42)
Conf.fromList(int :: string :: Nil)
Conf.fromMap(Map("a" -> string, "b" -> int))
```

## Conf.parse

You need an implicit `MetaconfigParser` to convert HOCON into `Conf`.
Assuming you depend on the `metaconfig-typesafe-config` module,

```tut
import metaconfig.typesafeconfig._
Conf.parseString("""
a.b.c = 2
a.d = [ 1, 2, 3 ]
reference = ${a}
""")
Conf.parseFile(new java.io.File(".scalafmt.conf"))
```

Note. The example above is JVM-only.
For a Scala.js alternative, depend on the `metaconfig-hocon` module and replace `metaconfig.typesafeconfig` with

```scala
import metaconfig.hocon._
```

## ConfDecoder.instance

To convert `Conf` into higher-level data structures you need a `ConfDecoder[T]` instance.
Convert a partial function from `Conf` to your target type using `ConfDecoder.instance[T]`.

```tut:silent
val number2 = ConfDecoder.instance[Int] {
    case Conf.Str("2") => Configured.Ok(2)
}
```

```tut
number2.read(Conf.fromString("2"))
number2.read(Conf.fromInt(2))
```

Convert a regular function from `Conf` to your target type using `ConfDecoder.instanceF[T]`.

```tut:silent
case class User(name: String, age: Int)
val decoder = ConfDecoder.instanceF[User] { conf =>
  conf.get[String]("name").product(conf.get[Int]("age")).map {
      case (name, age) => User(name, age)
  }
}
```

```tut
decoder.read(Conf.parseString("""
name = "Susan"
age = 29
"""))
decoder.read(Conf.parseString("""
name = 42
age = "Susan"
"""))
```

## ConfError

`ConfError` is a helper to produce readable and potentially aggregated error messages.

```tut
ConfError.message("Not good!")
ConfError.exception(new IllegalArgumentException("Expected String!"), stackSize = 2)
ConfError.typeMismatch("Int", "String", "field")
ConfError.message("Failure 1").combine(ConfError.message("Failure 2"))
```

Metaconfig uses Scalameta `Input` to represent an input source and `Position` to represent range positions in a given `Input`

```tut:silent
import scala.meta.inputs._
val input = Input.VirtualFile(
  "foo.scala",
  """
    |object A {
    |  var x
    |}
  """.stripMargin
)
val i = input.value.indexOf('v')
val pos = Position.Range(input, i, i)
```

```tut
ConfError.parseError(pos, "No var")
```

## Configured

`Configured[T]` is like an `Either[metaconfig.ConfError, T]` which is used througout the metaconfig API to either represent a successfully parsed/decoded value or a failure.

```tut
Configured.ok("Hello world!")
Configured.ok(List(1, 2))
val error = ConfError.message("Boom!")
val configured = error.notOk
configured.toEither
```

To skip error handling, use the nuclear `.get`

```tut:fail
configured.get
```

```tut
Configured.ok(42).get
```

## generic.deriveSurface

To use automatic derivation, you first need a `Surface[T]` typeclass instance

```tut
implicit val userSurface: Surface[User] =
  generic.deriveSurface[User]
```

The surface is used by metaconfig to support configurable decoding such as alternative fields names.
In the future, the plan is to use `Surface[T]` to automatically generate html/markdown documentation for configuration settings.
For now, you can ignore `Surface[T]` and just consider it as an annoying requirement from metaconfig.

## generic.deriveDecoder

Writing manual decoder by hand grows tiring quickly.
This becomes especially true when you have documentation to keep up-to-date as well.

```tut:silent
implicit val decoder: ConfDecoder[User] =
  generic.deriveDecoder[User](User("John", 42)).noTypos
```

```tut
ConfDecoder[User].read(Conf.parseString("""
name = Susan
age = 34
"""))
ConfDecoder[User].read(Conf.parseString("""
nam = John
age = 23
"""))
ConfDecoder[User].read(Conf.parseString("""
name = John
age = Old
"""))
```

Sometimes automatic derivation fails, for example if your class contains fields that have no `ConfDecoder` instance

```tut
import java.io.File
case class Funky(file: File)
implicit val surface = generic.deriveSurface[Funky]
```

This will fail wiith a fail cryptic compile error

```tut:fail
implicit val decoder = generic.deriveDecoder[Funky](Funky(new File("")))
```

Observer that the error message is complaining about a missing `metaconfig.ConfDecoder[java.io.File]` implicit.

## DeprecatedName

As your configuration evolves, you may want to rename some settings but you have existing users who are using the old name.
Use the `@DeprecatedName` annotation to continue supporting the old name even if you go ahead with the rename.

```tut:silent
case class EvolvingConfig(
    @DeprecatedName("goodName", "Use isGoodName instead", "1.0")
    isGoodName: Boolean
)
implicit val surface = generic.deriveSurface[EvolvingConfig]
implicit val decoder = generic.deriveDecoder[EvolvingConfig](EvolvingConfig(true)).noTypos
```

```tut
decoder.read(Conf.Obj("goodName" -> Conf.fromBoolean(false)))
decoder.read(Conf.Obj("isGoodName" -> Conf.fromBoolean(false)))
decoder.read(Conf.Obj("gooodName" -> Conf.fromBoolean(false)))
```
