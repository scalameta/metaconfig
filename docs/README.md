# Metaconfig

Metaconfig is a library to read HOCON configuration into Scala case classes. Key
features of Metaconfig include

* helpful error messages on common mistakes like typos or type mismatch
  (expected string, obtained int)
* configurable, semi-automatic derivation of decoders, with support for
  deprecating setting options
* cross-platform, supports JS/JVM. Native support is on the roadmap

The target use-case for metaconfig is tool maintainers who support HOCON
configuration in their tool. Metaconfig is used by scalafmt to read
`.scalafmt.conf` and scalafix to read `.scalafix.conf`. With metaconfig, tool
maintainers should be able to safely evolve their configuration (deprecate old
fields, add new fields) without breaking existing configuration files. Users
should get helpful error messages when they mistype a setting name.

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

All of the following code examples assume that you have `import metaconfig._` in
scope.

<!-- TOC -->

* [Metaconfig](#metaconfig)
  * [Getting started](#getting-started)
  * [Conf](#conf)
  * [Conf.parse](#confparse)
  * [Conf.printHocon](#confprinthocon)
  * [Conf.patch](#confpatch)
  * [ConfDecoder](#confdecoder)
  * [ConfEncoder](#confencoder)
  * [ConfCodec](#confcodec)
  * [ConfError](#conferror)
  * [Configured](#configured)
  * [generic.deriveSurface](#genericderivesurface)
  * [generic.deriveDecoder](#genericderivedecoder)
    * [Limitations](#limitations)
  * [@DeprecatedName](#deprecatedname)
  * [Conf.parseCliArgs](#confparsecliargs)
  * [Settings.toCliHelp](#settingstoclihelp)
  * [@Inline](#inline)
  * [Docs](#docs)
  * [JSON](#json)
  * [JSON Schema](#json-schema)

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

You need an implicit `MetaconfigParser` to convert HOCON into `Conf`. Assuming
you depend on the `metaconfig-typesafe-config` module,

```tut
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
[HOCON](https://github.com/lightbend/config/blob/master/HOCON.md).

```tut
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

* your application has many configuration options with default values,
* you have a custom configuration object that overrides only a few specific
  fields.
* you want to pretty-print the minimal HOCON configuration to obtain that custom
  configuration

Use `Conf.patch` compute a minimal `Conf` to go from an original `Conf` to a
revised `Conf`.

```tut
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
`ConfDecoder.instance[T]`.

```tut:silent
val number2 = ConfDecoder.instance[Int] {
    case Conf.Str("2") => Configured.Ok(2)
}
```

```tut
number2.read(Conf.fromString("2"))
number2.read(Conf.fromInt(2))
```

Convert a regular function from `Conf` to your target type using
`ConfDecoder.instanceF[T]`.

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

You can also use existing decoders to build more complex decoders

```tut
val fileDecoder = ConfDecoder.stringConfDecoder.flatMap { string =>
  val file = new java.io.File(string)
  if (file.exists()) Configured.ok(file)
  else ConfError.fileDoesNotExist(file).notOk
}
fileDecoder.read(Conf.fromString(".scalafmt.conf"))
fileDecoder.read(Conf.fromString(".foobar"))
```

## ConfEncoder

To convert a class instance into `Conf` use `ConfEncoder[T]`. It's possible to
automatically derive a `ConfEncoder[T]` instance for any case class with
`generic.deriveEncoder`.

```tut
implicit val encoder = generic.deriveEncoder[User]

ConfEncoder[User].write(User("John", 42))
```

It's possible to compose `ConfEncoder` instances with `contramap`

```tut:silent
val ageEncoder = ConfEncoder.IntEncoder.contramap[User](user => user.age)
```

```
ageEncoder.write(User("Ignored", 88))
```

## ConfCodec

It's common to have a class that has both a `ConfDecoder[T]` and
`ConfEncoder[T]` instance. For convenience, it's possible to use the
`ConfCodec[T]` typeclass to wrap an encoder and decoder in one instance.

```tut:silent
case class Bijective(name: String)
implicit val surface = generic.deriveSurface[Bijective]
implicit val codec = generic.deriveCodec[Bijective](new Bijective("default"))
```

```tut
ConfEncoder[Bijective].write(Bijective("John"))
ConfDecoder[Bijective].read(Conf.Obj("name" -> Conf.Str("Susan")))
```

It's possible to compose `ConfCodec` instances with `bimap`

```tut:silent
val bijectiveString = ConfCodec.StringCodec.bimap[Bijective](_.name, Bijective(_))
```

```tut
bijectiveString.write(Bijective("write"))
bijectiveString.read(Conf.Str("write"))
```

## ConfError

`ConfError` is a helper to produce readable and potentially aggregated error
messages.

```tut
ConfError.message("Not good!")
ConfError.exception(new IllegalArgumentException("Expected String!"), stackSize = 2)
ConfError.typeMismatch("Int", "String", "field")
ConfError.message("Failure 1").combine(ConfError.message("Failure 2"))
```

Metaconfig uses `Input` to represent a source that can be parsed and `Position`
to represent range positions in a given `Input`

```tut:silent
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

```tut
ConfError.parseError(pos, "No var")
```

## Configured

`Configured[T]` is like an `Either[metaconfig.ConfError, T]` which is used
througout the metaconfig API to either represent a successfully parsed/decoded
value or a failure.

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

Sometimes automatic derivation fails, for example if your class contains fields
that have no `ConfDecoder` instance

```tut
import java.io.File
case class Funky(file: File)
implicit val surface = generic.deriveSurface[Funky]
```

This will fail wiith a fail cryptic compile error

```tut:fail
implicit val decoder = generic.deriveDecoder[Funky](Funky(new File("")))
```

Observe that the error message is complaining about a missing
`metaconfig.ConfDecoder[java.io.File]` implicit.

### Limitations

The following features are not supported by generic derivation

* derivation for objects, sealed traits or non-case classes, only case classes
  are supported
* parameterized types, it's possible to derive decoders for a concrete
  parameterized type like `Option[Foo]` but note that the type field
  (`Field.tpe`) will be pretty-printed to the generic representation of that
  field: `Option[T].value: T`.

## @DeprecatedName

As your configuration evolves, you may want to rename some settings but you have
existing users who are using the old name. Use the `@DeprecatedName` annotation
to continue supporting the old name even if you go ahead with the rename.

```tut:silent
import metaconfig.annotation._
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

## Conf.parseCliArgs

Metaconfig can parse command line arguments into a `Conf`.

```tut:silent
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

```tut
val conf = Conf.parseCliArgs[App](List(
  "--verbose",
  "--target", "/tmp",
  "input.txt"
))
```

Decode the cli args into `App` like normal

```tut
val app = decoder.read(conf.get)
```

## Settings.toCliHelp

Generate a --help message with a `Settings[T]`.

```tut
Settings[App].toCliHelp(default = App())
```

## @Inline

If you have multiple cli apps that all share a base set of fields you can use
`@Inline`.

```tut:silent
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

```tut
Conf.parseCliArgs[NameApp](List("--out", "/tmp", "--cwd", "working-dir"))
val conf = Conf.parseCliArgs[AgeApp](List("--out", "target", "--cwd", "working-dir"))
conf.get.as[AgeApp].get
```

The generated --help message does not display `@Inline` fields. Instead, the
nested fields inside the type of the `@Inline` field are shown in the --help
message.

```tut
Settings[NameApp].toCliHelp(default = NameApp())
```

## Docs

To generate documentation for you configuration, add a dependency to the
following module

```scala
libraryDependencies += "com.geirsson" %% "metaconfig-docs" % "@VERSION@"
```

First define your configuration

```tut:silent:reset
import metaconfig._
import metaconfig.annotation._
import metaconfig.generic._

case class Home(
    @Description("Address description")
    address: String = "Lakelands 2",
    @Description("Country description")
    country: String = "Iceland"
)
implicit val homeSurface = generic.deriveSurface[Home]
implicit val homeEncoder = generic.deriveEncoder[Home]

case class User(
    @Description("Name description")
    name: String = "John",
    @Description("Age description")
    age: Int = 42,
    home: Home = Home()
)
implicit val userSurface = generic.deriveSurface[User]
implicit val userEncoder = generic.deriveEncoder[User]
```

To generate html documentation, pass in a default value

```tut
docs.Docs.html(User())
```

The output will look like this when rendered in a markdown or html document

```tut:passthrough
println(docs.Docs.html(User()))
```

The `Docs.html` method does nothing magical, it's possible to implement custom
renderings by inspecting `Settings[T]` directly.

```tut
Settings[User].settings
val flat = Settings[User].flat(User())
flat.map { case (setting, defaultValue) =>
  s"Setting ${setting.name} of type ${setting.tpe} has default value $defaultValue"
}.mkString("\n==============\n")
```

## JSON

To parse JSON instead of HOCON use the `metaconfig-json` module.

```scala
// JVM-only
libraryDependencies += "com.geirsson" %% "metaconfig-json" % "@VERSION@"
```

To parse JSON into `metaconfig.Conf`

```tut
import metaconfig.json.parser
import metaconfig._
```

```tut
Conf.parseString("""
{
  "a": 1,
  "b": [
    2,
    3,
    true,
    null
  ]
}
""")
```

The JSON parser supports comments and trailing commas

```tut
Conf.parseString("""
{
  // NOTE: don't set this to false!
  "important": true,
  "dependencies": [
    "a",
    "b", // TODO: get rid of this dependency at some point
  ],
}
""")
```

## JSON Schema

It's possible to automatically generate a JSON schema

```tut
val js = metaconfig.JsonSchema.generate(
  title = "My User App",
  description = "My User APP description",
  url = Some("http://my.user.app/schema.json"),
  default = User()
)
ujson.write(js, indent = 2)
```
