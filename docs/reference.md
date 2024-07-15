---
id: reference
title: Reference
---

Metaconfig is a library to read HOCON configuration into Scala case classes. Key
features of Metaconfig include

- helpful error messages on common mistakes like typos or type mismatch
  (expected string, obtained int)
- configurable, semi-automatic derivation of decoders, with support for
  deprecating setting options
- cross-platform, supports JS/JVM. Native support is on the roadmap

The target use-case for metaconfig is tool maintainers who support HOCON
configuration in their tool. Metaconfig is used by scalafmt to read
`.scalafmt.conf` and scalafix to read `.scalafix.conf`. With metaconfig, tool
maintainers should be able to safely evolve their configuration (deprecate old
fields, add new fields) without breaking existing configuration files. Users
should get helpful error messages when they mistype a setting name.

There are alternatives to metaconfig that you might want to give a try first

- https://github.com/circe/circe-config
- https://github.com/pureconfig/pureconfig

## Getting started

```scala
libraryDependencies += "org.scalameta" %% "metaconfig-core" % "@VERSION@"

// Use https://github.com/lightbend/config to parse HOCON
libraryDependencies += "org.scalameta" %% "metaconfig-typesafe-config" % "@VERSION@"
```

Use this import to access the metaconfig API

```scala mdoc:silent
import metaconfig._
```

All of the following code examples assume that you have `import metaconfig._` in
scope.

## Conf

`Conf` is a JSON-like data structure that is the foundation of metaconfig.

```scala mdoc
val string = Conf.fromString("string")
val int = Conf.fromInt(42)
Conf.fromList(int :: string :: Nil)
Conf.fromMap(Map("a" -> string, "b" -> int))
```

## Conf.parse

You need an implicit `MetaconfigParser` to convert HOCON into `Conf`. Assuming
you depend on the `metaconfig-typesafe-config` module,

```scala mdoc
import metaconfig.typesafeconfig._
Conf.parseString("""
a.b.c = 2
a.d = [ 1, 2, 3 ]
reference = ${a}
""")
Conf.parseFile(new java.io.File(".scalafmt.conf"))
```

Note. The example above is JVM-only. For a Scala.js alternative, depend on the
`metaconfig-sconfig` module and replace `metaconfig.typesafeconfig` with

```scala
import metaconfig.sconfig._
```

## Conf.printHocon

It's possible to print `Conf` as
[HOCON](https://github.com/lightbend/config/blob/main/HOCON.md).

```scala mdoc
Conf.printHocon(Conf.Obj(
  "a" -> Conf.Obj(
    "b" -> Conf.Str("3"),
    "c" -> Conf.Num(1),
    "d" -> Conf.Lst(
      Conf.Null(),
      Conf.Bool(true)
))))
```

The printer is tested against the roundtrip property

```
parse(print(conf)) == conf
```

so it should be safe to parse the output from the printer.

## Conf.patch

Imagine the scenario

- your application has many configuration options with default values,
- you have a custom configuration object that overrides only a few specific
  fields.
- you want to pretty-print the minimal HOCON configuration to obtain that custom
  configuration

Use `Conf.patch` compute a minimal `Conf` to go from an original `Conf` to a
revised `Conf`.

```scala mdoc
val original = Conf.Obj(
  "a" -> Conf.Obj(
    "b" -> Conf.Str("c"),
    "d" -> Conf.Str("e")
  ),
  "f" -> Conf.Bool(true)
)
val revised = Conf.Obj(
  "a" -> Conf.Obj(
    "b" -> Conf.Str("c"),
    "d" -> Conf.Str("ee") // <-- only overridden setting
  ),
  "f" -> Conf.Bool(true)
)
val patch = Conf.patch(original, revised)
Conf.printHocon(patch)
val revised2 = Conf.applyPatch(original, patch)
assert(revised == revised2)
```

The `patch` operation is tested against the property

```
applyPatch(original, revised) == applyPatch(original, patch(original, revised))
```

## ConfDecoder

To convert `Conf` into higher-level data structures you need a `ConfDecoder[T]`
instance. Convert a partial function from `Conf` to your target type using
`ConfDecoder.fromPartial[T]`.

```scala mdoc:silent
val number2 = ConfDecoder.fromPartial[Int]("String") {
    case Conf.Str("2") => Configured.Ok(2)
}
```

```scala mdoc
number2.read(Conf.fromString("2"))
number2.read(Conf.fromInt(2))
```

Convert a regular function from `Conf` to your target type using
`ConfDecoder.from[T]`.

```scala mdoc:silent
case class User(name: String, age: Int)
val decoder = ConfDecoder.from[User] { conf =>
  conf.get[String]("name").product(conf.get[Int]("age")).map {
      case (name, age) => User(name, age)
  }
}
```

```scala mdoc
decoder.read(Conf.parseString("""
name = "Susan"
age = 29
"""))
decoder.read(Conf.parseString("""
name = 42
age = "Susan"
"""))
```

You can also use existing decoders to build more complex decoders

```scala mdoc
val fileDecoder = ConfDecoder.stringConfDecoder.flatMap { string =>
  val file = new java.io.File(string)
  if (file.exists()) Configured.ok(file)
  else ConfError.fileDoesNotExist(file).notOk
}
fileDecoder.read(Conf.fromString(".scalafmt.conf"))
fileDecoder.read(Conf.fromString(".foobar"))
```

## ConfDecoderEx and ConfDecoderExT

Similar to `ConfDecoder` but its `read` method takes an initial state as a parameter
rather than as part of the decoder instance definition. `ConfDecoderEx[A]` is an alias
for `ConfDecoderExT[A, A]`.

### Decoding collections

If a decoder for type `T` is defined, the package defines implicits to derive
decoders for `Option[T]`, `Seq[T]` and `Map[String, T]`.

There's also a special for _extending_ collections rather than redefining them
(works only for `ConfDecoderEx`, not the original `ConfDecoder`):
```
// sets list
a = [ ... ]
// sets map
a = {
  b { ... }
  c { ... }
}

// extends list
a = {
  // must be the only key
  "+" = [ ... ]
}
// extends map
a = {
  // must be the only key
  "+" = {
    d { ... }
  }
}
```

## ConfEncoder

To convert a class instance into `Conf` use `ConfEncoder[T]`. It's possible to
automatically derive a `ConfEncoder[T]` instance for any case class with
`generic.deriveEncoder`.

```scala mdoc
implicit val encoder: ConfEncoder[User] = generic.deriveEncoder[User]

ConfEncoder[User].write(User("John", 42))
```

It's possible to compose `ConfEncoder` instances with `contramap`

```scala mdoc:silent
val ageEncoder = ConfEncoder.IntEncoder.contramap[User](user => user.age)
```

```
ageEncoder.write(User("Ignored", 88))
```

## ConfCodec

It's common to have a class that has both a `ConfDecoder[T]` and
`ConfEncoder[T]` instance. For convenience, it's possible to use the
`ConfCodec[T]` typeclass to wrap an encoder and decoder in one instance.

```scala mdoc:silent
case class Bijective(name: String)
implicit val surface: generic.Surface[Bijective] = generic.deriveSurface[Bijective]
implicit val codec: ConfCodec[Bijective] = generic.deriveCodec[Bijective](new Bijective("default"))
```

```scala mdoc
ConfEncoder[Bijective].write(Bijective("John"))
ConfDecoder[Bijective].read(Conf.Obj("name" -> Conf.Str("Susan")))
```

It's possible to compose `ConfCodec` instances with `bimap`

```scala mdoc:silent
val bijectiveString = ConfCodec.StringCodec.bimap[Bijective](_.name, Bijective(_))
```

```scala mdoc
bijectiveString.write(Bijective("write"))
bijectiveString.read(Conf.Str("write"))
```

## ConfCodecEx and ConfCodecExT

Similar to `ConfCodec` but derives from `ConfDecoderExT` instead of `ConfDecoder`.

## ConfError

`ConfError` is a helper to produce readable and potentially aggregated error
messages.

```scala mdoc
ConfError.message("Not good!")
ConfError.exception(new IllegalArgumentException("Expected String!"), stackSize = 2)
ConfError.typeMismatch("Int", "String", "field")
ConfError.message("Failure 1").combine(ConfError.message("Failure 2"))
```

Metaconfig uses `Input` to represent a source that can be parsed and `Position`
to represent range positions in a given `Input`

```scala mdoc:silent
val input = Input.VirtualFile(
  "foo.scala",
  """
    |object A {
    |  var x
    |}
  """.stripMargin
)
val i = input.text.indexOf('v')
val pos = Position.Range(input, i, i)
```

```scala mdoc
ConfError.parseError(pos, "No var")
```

## Configured

`Configured[T]` is like an `Either[metaconfig.ConfError, T]` which is used
throughout the metaconfig API to either represent a successfully parsed/decoded
value or a failure.

```scala mdoc
Configured.ok("Hello world!")
Configured.ok(List(1, 2))
val error = ConfError.message("Boom!")
val configured = error.notOk
configured.toEither
```

To skip error handling, use the nuclear `.get`

```scala mdoc:crash
configured.get
```

```scala mdoc
Configured.ok(42).get
```

## generic.deriveSurface

To use automatic derivation, you first need a `Surface[T]` typeclass instance

```scala mdoc
import metaconfig.generic._
implicit val userSurface: Surface[User] =
  generic.deriveSurface[User]
```

The surface is used by metaconfig to support configurable decoding such as
alternative fields names. In the future, the plan is to use `Surface[T]` to
automatically generate html/markdown documentation for configuration settings.
For now, you can ignore `Surface[T]` and just consider it as an annoying
requirement from metaconfig.

## generic.deriveDecoder

Writing manual decoder by hand grows tiring quickly. This becomes especially
true when you have documentation to keep up-to-date as well.

```scala mdoc:silent:nest
implicit val decoder: ConfDecoder[User] =
  generic.deriveDecoder[User](User("John", 42)).noTypos
implicit val decoderEx: ConfDecoderEx[User] =
  generic.deriveDecoderEx[User](User("Jane", 24)).noTypos
```

```scala mdoc
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
ConfDecoderEx[User].read(
  Some(User(name = "Jack", age = 33)),
  Conf.parseString("name = John")
)
ConfDecoderEx[User].read(
  None,
  Conf.parseString("name = John")
)
```

Sometimes automatic derivation fails, for example if your class contains fields
that have no `ConfDecoder` instance

```scala mdoc
import java.io.File
case class Funky(file: File)
implicit val surface = generic.deriveSurface[Funky]
```

This will fail with a fail cryptic compile error

```scala mdoc:fail
implicit val decoder = generic.deriveDecoder[Funky](Funky(new File("")))
```

Observe that the error message is complaining about a missing
`metaconfig.ConfDecoder[java.io.File]` implicit.

### Limitations

The following features are not supported by generic derivation

- derivation for objects, sealed traits or non-case classes, only case classes
  are supported
- parameterized types, it's possible to derive decoders for a concrete
  parameterized type like `Option[Foo]` but note that the type field
  (`Field.tpe`) will be pretty-printed to the generic representation of that
  field: `Option[T].value: T`.

## @DeprecatedName

As your configuration evolves, you may want to rename some settings but you have
existing users who are using the old name. Use the `@DeprecatedName` annotation
to continue supporting the old name even if you go ahead with the rename.

```scala mdoc:silent:nest
import metaconfig.annotation._
case class EvolvingConfig(
    @DeprecatedName("goodName", "Use isGoodName instead", "1.0")
    isGoodName: Boolean
)
implicit val surface: generic.Surface[EvolvingConfig] =
  generic.deriveSurface[EvolvingConfig]
implicit val decoder: ConfDecoder[EvolvingConfig] =
  generic.deriveDecoder[EvolvingConfig](EvolvingConfig(true)).noTypos
```

```scala mdoc
decoder.read(Conf.Obj("goodName" -> Conf.fromBoolean(false)))
decoder.read(Conf.Obj("isGoodName" -> Conf.fromBoolean(false)))
decoder.read(Conf.Obj("gooodName" -> Conf.fromBoolean(false)))
```

## Conf.parseCliArgs

Metaconfig can parse command line arguments into a `Conf`.

```scala mdoc:silent:nest
case class App(
  @Description("The directory to output files")
  target: String = "out",
  @Description("Print out debugging diagnostics")
  @ExtraName("v")
  verbose: Boolean = false,
  @Description("The input files for app")
  @ExtraName("remainingArgs")
  files: List[String] = Nil
)
implicit val surface = generic.deriveSurface[App]
implicit val codec = generic.deriveCodec[App](App())
```

```scala mdoc
val conf = Conf.parseCliArgs[App](List(
  "--verbose",
  "--target", "/tmp",
  "input.txt"
))
```

Decode the cli args into `App` like normal

```scala mdoc
val app = decoder.read(conf.get)
```

## Settings.toCliHelp

Generate a --help message with a `Settings[T]`.

```scala mdoc
Settings[App].toCliHelp(default = App())
```

## @Inline

If you have multiple cli apps that all share a base set of fields you can use
`@Inline`.

```scala mdoc:silent:nest
case class Common(
  @Description("The working directory")
  cwd: String = "",
  @Description("The output directory")
  out: String = ""
)
implicit val surface = generic.deriveSurface[Common]
implicit val codec = generic.deriveCodec[Common](Common())

case class AgeApp(
  @Description("The user's age")
  age: Int = 0,
  @Inline
  common: Common = Common()
)
implicit val ageSurface = generic.deriveSurface[AgeApp]
implicit val ageCodec = generic.deriveCodec[AgeApp](AgeApp())

case class NameApp(
  @Description("The user's name")
  name: String = "John",
  @Inline
  common: Common = Common()
)
implicit val nameSurface = generic.deriveSurface[NameApp]
implicit val nameCodec = generic.deriveCodec[NameApp](NameApp())
```

Observe that `NameApp` and `AgeApp` both have an `@Inline common: Common` field.
It is not necessary to prefix cli args with the name of `@Inline` fields. In the
example above, it's possible to pass in `--out target` instead of
`--common.out target` to override the common output directory.

```scala mdoc
Conf.parseCliArgs[NameApp](List("--out", "/tmp", "--cwd", "working-dir"))
val conf = Conf.parseCliArgs[AgeApp](List("--out", "target", "--cwd", "working-dir"))
conf.get.as[AgeApp].get
```

The generated --help message does not display `@Inline` fields. Instead, the
nested fields inside the type of the `@Inline` field are shown in the --help
message.

```scala mdoc
Settings[NameApp].toCliHelp(default = NameApp())
```

## Docs

To generate documentation for you configuration, add a dependency to the
following module

```scala
libraryDependencies += "org.scalameta" %% "metaconfig-docs" % "@VERSION@"
```

First define your configuration

```scala mdoc:silent:reset
import metaconfig._
import metaconfig.annotation._
import metaconfig.generic._

case class Home(
    @Description("Address description")
    address: String = "Lakelands 2",
    @Description("Country description")
    country: String = "Iceland"
)
implicit val homeSurface: Surface[Home] = generic.deriveSurface[Home]
implicit val homeEncoder: ConfEncoder[Home] = generic.deriveEncoder[Home]

case class User(
    @Description("Name description")
    name: String = "John",
    @Description("Age description")
    age: Int = 42,
    home: Home = Home()
)
implicit val userSurface: Surface[User] = generic.deriveSurface[User]
implicit val userEncoder: ConfEncoder[User] = generic.deriveEncoder[User]
```

To generate html documentation, pass in a default value

```scala mdoc
docs.Docs.html(User())
```

The output will look like this when rendered in a markdown or html document

```scala mdoc:passthrough
println(docs.Docs.html(User()))
```

The `Docs.html` method does nothing magical, it's possible to implement custom
renderings by inspecting `Settings[T]` directly.

```scala mdoc
Settings[User].settings
val flat = Settings[User].flat(ConfEncoder[User].writeObj(User()))
flat.map { case (setting, defaultValue) =>
  s"Setting ${setting.name} of type ${setting.tpe} has default value $defaultValue"
}.mkString("\n==============\n")
```
