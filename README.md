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
libraryDependencies += "com.geirsson" %% "metaconfig-core" % "0.8.1"

// Use https://github.com/lightbend/config to parse HOCON
libraryDependencies += "com.geirsson" %% "metaconfig-typesafe-config" % "0.8.1"
```

Use this import to access the metaconfig API

```scala
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

```scala
scala> val string = Conf.fromString("string")
string: metaconfig.Conf = "string"

scala> val int = Conf.fromInt(42)
int: metaconfig.Conf = 42

scala> Conf.fromList(int :: string :: Nil)
res0: metaconfig.Conf = [42, "string"]

scala> Conf.fromMap(Map("a" -> string, "b" -> int))
res1: metaconfig.Conf = {"a": "string", "b": 42}
```

## Conf.parse

You need an implicit `MetaconfigParser` to convert HOCON into `Conf`. Assuming
you depend on the `metaconfig-typesafe-config` module,

```scala
scala> import metaconfig.typesafeconfig._
import metaconfig.typesafeconfig._

scala> Conf.parseString("""
     | a.b.c = 2
     | a.d = [ 1, 2, 3 ]
     | reference = ${a}
     | """)
res2: metaconfig.Configured[metaconfig.Conf] = Ok({"a": {"d": [1, 2, 3], "b": {"c": 2}}, "reference": {"d": [1, 2, 3], "b": {"c": 2}}})

scala> Conf.parseFile(new java.io.File(".scalafmt.conf"))
res3: metaconfig.Configured[metaconfig.Conf] = Ok({"project": {"git": true}, "assumeStandardLibraryStripMargin": true, "align": "none"})
```

Note. The example above is JVM-only. For a Scala.js alternative, depend on the
`metaconfig-hocon` module and replace `metaconfig.typesafeconfig` with

```scala
import metaconfig.hocon._
```

## Conf.printHocon

It's possible to print `Conf` as
[HOCON](https://github.com/lightbend/config/blob/master/HOCON.md).

```scala
scala> Conf.printHocon(Conf.Obj(
     |   "a" -> Conf.Obj(
     |     "b" -> Conf.Str("3"),
     |     "c" -> Conf.Num(1),
     |     "d" -> Conf.Lst(
     |       Conf.Null(),
     |       Conf.Bool(true)
     | ))))
res4: String =
a.b = "3"
a.c = 1
a.d = [
  null
  true
]
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

```scala
scala> val original = Conf.Obj(
     |   "a" -> Conf.Obj(
     |     "b" -> Conf.Str("c"),
     |     "d" -> Conf.Str("e")
     |   ),
     |   "f" -> Conf.Bool(true)
     | )
original: metaconfig.Conf.Obj = {"a": {"b": "c", "d": "e"}, "f": true}

scala> val revised = Conf.Obj(
     |   "a" -> Conf.Obj(
     |     "b" -> Conf.Str("c"),
     |     "d" -> Conf.Str("ee") // <-- only overridden setting
     |   ),
     |   "f" -> Conf.Bool(true)
     | )
revised: metaconfig.Conf.Obj = {"a": {"b": "c", "d": "ee"}, "f": true}

scala> val patch = Conf.patch(original, revised)
patch: metaconfig.Conf = {"a": {"d": "ee"}}

scala> Conf.printHocon(patch)
res5: String = a.d = ee

scala> val revised2 = Conf.applyPatch(original, patch)
revised2: metaconfig.Conf = {"f": true, "a": {"b": "c", "d": "ee"}}

scala> assert(revised == revised2)
```

The `patch` operation is tested against the property

```
applyPatch(original, revised) == applyPatch(original, patch(original, revised))
```

## ConfDecoder

To convert `Conf` into higher-level data structures you need a `ConfDecoder[T]`
instance. Convert a partial function from `Conf` to your target type using
`ConfDecoder.instance[T]`.

```scala
val number2 = ConfDecoder.instance[Int] {
    case Conf.Str("2") => Configured.Ok(2)
}
```

```scala
scala> number2.read(Conf.fromString("2"))
res7: metaconfig.Configured[Int] = Ok(2)

scala> number2.read(Conf.fromInt(2))
res8: metaconfig.Configured[Int] =
NotOk(Type mismatch;
  found    : Number (value: 2)
  expected : int)
```

Convert a regular function from `Conf` to your target type using
`ConfDecoder.instanceF[T]`.

```scala
case class User(name: String, age: Int)
val decoder = ConfDecoder.instanceF[User] { conf =>
  conf.get[String]("name").product(conf.get[Int]("age")).map {
      case (name, age) => User(name, age)
  }
}
```

```scala
scala> decoder.read(Conf.parseString("""
     | name = "Susan"
     | age = 29
     | """))
res9: metaconfig.Configured[User] = Ok(User(Susan,29))

scala> decoder.read(Conf.parseString("""
     | name = 42
     | age = "Susan"
     | """))
res10: metaconfig.Configured[User] =
NotOk(2 errors
[E0] Type mismatch;
  found    : Number (value: 42)
  expected : String
[E1] Type mismatch;
  found    : String (value: "Susan")
  expected : Number
)
```

You can also use existing decoders to build more complex decoders

```scala
scala> val fileDecoder = ConfDecoder.stringConfDecoder.flatMap { string =>
     |   val file = new java.io.File(string)
     |   if (file.exists()) Configured.ok(file)
     |   else ConfError.fileDoesNotExist(file).notOk
     | }
fileDecoder: metaconfig.ConfDecoder[java.io.File] = metaconfig.ConfDecoder$$anon$1@9fb2ef

scala> fileDecoder.read(Conf.fromString(".scalafmt.conf"))
res11: metaconfig.Configured[java.io.File] = Ok(.scalafmt.conf)

scala> fileDecoder.read(Conf.fromString(".foobar"))
res12: metaconfig.Configured[java.io.File] = NotOk(File /Users/ollie/dev/metaconfig/.foobar does not exist.)
```

## ConfEncoder

To convert a class instance into `Conf` use `ConfEncoder[T]`. It's possible to
automatically derive a `ConfEncoder[T]` instance for any case class with
`generic.deriveEncoder`.

```scala
scala> implicit val encoder = generic.deriveEncoder[User]
encoder: metaconfig.ConfEncoder[User] = $anon$1@7b826771

scala> ConfEncoder[User].write(User("John", 42))
res13: metaconfig.Conf = {"name": "John", "age": 42}
```

It's possible to compose `ConfEncoder` instances with `contramap`

```scala
val ageEncoder = ConfEncoder.IntEncoder.contramap[User](user => user.age)
```

```
ageEncoder.write(User("Ignored", 88))
```

## ConfCodec

It's common to have a class that has both a `ConfDecoder[T]` and
`ConfEncoder[T]` instance. For convenience, it's possible to use the
`ConfCodec[T]` typeclass to wrap an encoder and decoder in one instance.

```scala
case class Bijective(name: String)
implicit val surface = generic.deriveSurface[Bijective]
implicit val codec = generic.deriveCodec[Bijective](new Bijective("default"))
```

```scala
scala> ConfEncoder[Bijective].write(Bijective("John"))
res14: metaconfig.Conf = {"name": "John"}

scala> ConfDecoder[Bijective].read(Conf.Obj("name" -> Conf.Str("Susan")))
res15: metaconfig.Configured[Bijective] = Ok(Bijective(Susan))
```

It's possible to compose `ConfCodec` instances with `bimap`

```scala
val bijectiveString = ConfCodec.StringCodec.bimap[Bijective](_.name, Bijective(_))
```

```scala
scala> bijectiveString.write(Bijective("write"))
res16: metaconfig.Conf = "write"

scala> bijectiveString.read(Conf.Str("write"))
res17: metaconfig.Configured[Bijective] = Ok(Bijective(write))
```

## ConfError

`ConfError` is a helper to produce readable and potentially aggregated error
messages.

```scala
scala> ConfError.message("Not good!")
res18: metaconfig.ConfError = Not good!

scala> ConfError.exception(new IllegalArgumentException("Expected String!"), stackSize = 2)
res19: metaconfig.ConfError =
java.lang.IllegalArgumentException: Expected String!
	at .<init>(<console>:22)
	at .<clinit>(<console>)

scala> ConfError.typeMismatch("Int", "String", "field")
res20: metaconfig.ConfError =
Type mismatch at 'field';
  found    : String
  expected : Int

scala> ConfError.message("Failure 1").combine(ConfError.message("Failure 2"))
res21: metaconfig.ConfError =
2 errors
[E0] Failure 1
[E1] Failure 2
```

Metaconfig uses `Input` to represent a source that can be parsed and `Position`
to represent range positions in a given `Input`

```scala
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

```scala
scala> ConfError.parseError(pos, "No var")
res22: metaconfig.ConfError =
foo.scala:2:2 error: No var
  var x
  ^
```

## Configured

`Configured[T]` is like an `Either[metaconfig.ConfError, T]` which is used
througout the metaconfig API to either represent a successfully parsed/decoded
value or a failure.

```scala
scala> Configured.ok("Hello world!")
res23: metaconfig.Configured[String] = Ok(Hello world!)

scala> Configured.ok(List(1, 2))
res24: metaconfig.Configured[List[Int]] = Ok(List(1, 2))

scala> val error = ConfError.message("Boom!")
error: metaconfig.ConfError = Boom!

scala> val configured = error.notOk
configured: metaconfig.Configured[Nothing] = NotOk(Boom!)

scala> configured.toEither
res25: Either[metaconfig.ConfError,Nothing] = Left(Boom!)
```

To skip error handling, use the nuclear `.get`

```scala
scala> configured.get
java.util.NoSuchElementException: Boom!
  at metaconfig.Configured.get(Configured.scala:11)
  ... 45 elided
```

```scala
scala> Configured.ok(42).get
res27: Int = 42
```

## generic.deriveSurface

To use automatic derivation, you first need a `Surface[T]` typeclass instance

```scala
scala> import metaconfig.generic._
import metaconfig.generic._

scala> implicit val userSurface: Surface[User] =
     |   generic.deriveSurface[User]
userSurface: metaconfig.generic.Surface[User] = Surface(List(List(Field(name="name",tpe="String",annotations=List(),underlying=List()), Field(name="age",tpe="Int",annotations=List(),underlying=List()))))
```

The surface is used by metaconfig to support configurable decoding such as
alternative fields names. In the future, the plan is to use `Surface[T]` to
automatically generate html/markdown documentation for configuration settings.
For now, you can ignore `Surface[T]` and just consider it as an annoying
requirement from metaconfig.

## generic.deriveDecoder

Writing manual decoder by hand grows tiring quickly. This becomes especially
true when you have documentation to keep up-to-date as well.

```scala
implicit val decoder: ConfDecoder[User] =
  generic.deriveDecoder[User](User("John", 42)).noTypos
```

```scala
scala> ConfDecoder[User].read(Conf.parseString("""
     | name = Susan
     | age = 34
     | """))
res28: metaconfig.Configured[User] = Ok(User(Susan,34))

scala> ConfDecoder[User].read(Conf.parseString("""
     | nam = John
     | age = 23
     | """))
res29: metaconfig.Configured[User] = NotOk(Invalid field: nam. Expected one of name, age)

scala> ConfDecoder[User].read(Conf.parseString("""
     | name = John
     | age = Old
     | """))
res30: metaconfig.Configured[User] =
NotOk(Type mismatch;
  found    : String (value: "Old")
  expected : Number)
```

Sometimes automatic derivation fails, for example if your class contains fields
that have no `ConfDecoder` instance

```scala
scala> import java.io.File
import java.io.File

scala> case class Funky(file: File)
defined class Funky

scala> implicit val surface = generic.deriveSurface[Funky]
surface: metaconfig.generic.Surface[Funky] = Surface(List(List(Field(name="file",tpe="java.io.File",annotations=List(),underlying=List()))))
```

This will fail wiith a fail cryptic compile error

```scala
scala> implicit val decoder = generic.deriveDecoder[Funky](Funky(new File("")))
<console>:30: error: could not find implicit value for parameter ev: metaconfig.ConfDecoder[java.io.File]
       implicit val decoder = generic.deriveDecoder[Funky](Funky(new File("")))
                                                          ^
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

```scala
import metaconfig.annotation._
case class EvolvingConfig(
    @DeprecatedName("goodName", "Use isGoodName instead", "1.0")
    isGoodName: Boolean
)
implicit val surface = generic.deriveSurface[EvolvingConfig]
implicit val decoder = generic.deriveDecoder[EvolvingConfig](EvolvingConfig(true)).noTypos
```

```scala
scala> decoder.read(Conf.Obj("goodName" -> Conf.fromBoolean(false)))
res31: metaconfig.Configured[EvolvingConfig] = Ok(EvolvingConfig(false))

scala> decoder.read(Conf.Obj("isGoodName" -> Conf.fromBoolean(false)))
res32: metaconfig.Configured[EvolvingConfig] = Ok(EvolvingConfig(false))

scala> decoder.read(Conf.Obj("gooodName" -> Conf.fromBoolean(false)))
res33: metaconfig.Configured[EvolvingConfig] = NotOk(Invalid field: gooodName. Expected one of isGoodName)
```

## Conf.parseCliArgs

Metaconfig can parse command line arguments into a `Conf`.

```scala
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

```scala
scala> val conf = Conf.parseCliArgs[App](List(
     |   "--verbose",
     |   "--target", "/tmp",
     |   "input.txt"
     | ))
conf: metaconfig.Configured[metaconfig.Conf] = Ok({"remainingArgs": ["input.txt"], "target": "/tmp", "verbose": true})
```

Decode the cli args into `App` like normal

```scala
scala> val app = decoder.read(conf.get)
app: metaconfig.Configured[EvolvingConfig] = NotOk(Invalid fields: remainingArgs, target, verbose. Expected one of isGoodName)
```

## Settings.toCliHelp

Generate a --help message with a `Settings[T]`.

```scala
scala> Settings[App].toCliHelp(default = App())
res34: String =
--target: String = "out"   The directory to output files
--verbose: Boolean = false Print out debugging diagnostics
--files: List[String] = [] The input files for app
```

## @Inline

If you have multiple cli apps that all share a base set of fields you can use
`@Inline`.

```scala
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

```scala
scala> Conf.parseCliArgs[NameApp](List("--out", "/tmp", "--cwd", "working-dir"))
res37: metaconfig.Configured[metaconfig.Conf] = Ok({"common": {"cwd": "working-dir", "out": "/tmp"}})

scala> val conf = Conf.parseCliArgs[AgeApp](List("--out", "target", "--cwd", "working-dir"))
conf: metaconfig.Configured[metaconfig.Conf] = Ok({"common": {"cwd": "working-dir", "out": "target"}})

scala> conf.get.as[AgeApp].get
res38: AgeApp = AgeApp(0,Common(working-dir,target))
```

The generated --help message does not display `@Inline` fields. Instead, the
nested fields inside the type of the `@Inline` field are shown in the --help
message.

```scala
scala> Settings[NameApp].toCliHelp(default = NameApp())
res39: String =
--name: String = "John" The user's name
--cwd: String = ""      The working directory
--out: String = ""      The output directory
```

## Docs

To generate documentation for you configuration, add a dependency to the
following module

```scala
libraryDependencies += "com.geirsson" %% "metaconfig-docs" % "0.8.1"
```

First define your configuration

```scala
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

```scala
scala> docs.Docs.html(User())
res2: String = <table><thead><tr><th>Name</th><th>Type</th><th>Description</th><th>Default value</th></tr></thead><tbody><tr><td><code>name</code></td><td><code>String</code></td><td>Name description</td><td>&quot;John&quot;</td></tr><tr><td><code>age</code></td><td><code>Int</code></td><td>Age description</td><td>42</td></tr><tr><td><code>home.address</code></td><td><code>String</code></td><td>Address description</td><td>&quot;Lakelands 2&quot;</td></tr><tr><td><code>home.country</code></td><td><code>String</code></td><td>Country description</td><td>&quot;Iceland&quot;</td></tr></tbody></table>
```

The output will look like this when rendered in a markdown or html document


<table><thead><tr><th>Name</th><th>Type</th><th>Description</th><th>Default value</th></tr></thead><tbody><tr><td><code>name</code></td><td><code>String</code></td><td>Name description</td><td>&quot;John&quot;</td></tr><tr><td><code>age</code></td><td><code>Int</code></td><td>Age description</td><td>42</td></tr><tr><td><code>home.address</code></td><td><code>String</code></td><td>Address description</td><td>&quot;Lakelands 2&quot;</td></tr><tr><td><code>home.country</code></td><td><code>String</code></td><td>Country description</td><td>&quot;Iceland&quot;</td></tr></tbody></table>


The `Docs.html` method does nothing magical, it's possible to implement custom
renderings by inspecting `Settings[T]` directly.

```scala
scala> Settings[User].settings
res4: List[metaconfig.generic.Setting] = List(Setting(Field(name="name",tpe="String",annotations=List(@Description(Name description)),underlying=List())), Setting(Field(name="age",tpe="Int",annotations=List(@Description(Age description)),underlying=List())), Setting(Field(name="home",tpe="Home",annotations=List(),underlying=List(List(Field(name="address",tpe="String",annotations=List(@Description(Address description)),underlying=List()), Field(name="country",tpe="String",annotations=List(@Description(Country description)),underlying=List()))))))

scala> val flat = Settings[User].flat(User())
warning: there was one deprecation warning; re-run with -deprecation for details
flat: List[(metaconfig.generic.Setting, Any)] = List((Setting(Field(name="name",tpe="String",annotations=List(@Description(Name description)),underlying=List())),John), (Setting(Field(name="age",tpe="Int",annotations=List(@Description(Age description)),underlying=List())),42), (Setting(Field(name="home.address",tpe="String",annotations=List(@Description(Address description)),underlying=List())),Lakelands 2), (Setting(Field(name="home.country",tpe="String",annotations=List(@Description(Country description)),underlying=List())),Iceland))

scala> flat.map { case (setting, defaultValue) =>
     |   s"Setting ${setting.name} of type ${setting.tpe} has default value $defaultValue"
     | }.mkString("\n==============\n")
res5: String =
Setting name of type String has default value John
==============
Setting age of type Int has default value 42
==============
Setting home.address of type String has default value Lakelands 2
==============
Setting home.country of type String has default value Iceland
```

## JSON

To parse JSON instead of HOCON use the `metaconfig-json` module.

```scala
// JVM-only
libraryDependencies += "com.geirsson" %% "metaconfig-json" % "0.8.1"
```

To parse JSON into `metaconfig.Conf`

```scala
scala> import metaconfig.json.parser
import metaconfig.json.parser

scala> import metaconfig._
import metaconfig._
```

```scala
scala> Conf.parseString("""
     | {
     |   "a": 1,
     |   "b": [
     |     2,
     |     3,
     |     true,
     |     null
     |   ]
     | }
     | """)
res6: metaconfig.Configured[metaconfig.Conf] = Ok({"a": 1.0, "b": [2.0, 3.0, true, null]})
```

The JSON parser supports comments and trailing commas

```scala
scala> Conf.parseString("""
     | {
     |   // NOTE: don't set this to false!
     |   "important": true,
     |   "dependencies": [
     |     "a",
     |     "b", // TODO: get rid of this dependency at some point
     |   ],
     | }
     | """)
res7: metaconfig.Configured[metaconfig.Conf] = Ok({"important": true, "dependencies": ["a", "b"]})
```

## JSON Schema

It's possible to automatically generate a JSON schema

```scala
scala> val js = metaconfig.JsonSchema.generate(
     |   title = "My User App",
     |   description = "My User APP description",
     |   url = Some("http://my.user.app/schema.json"),
     |   default = User()
     | )
js: ujson.Js.Obj = {"$id":"http://my.user.app/schema.json","title":"My User App","description":"My User APP description","type":"object","properties":{"name":{"title":"name","description":"Name description","default":"John","required":false,"type":"string"},"age":{"title":"age","description":"Age description","default":42,"required":false,"type":"number"},"home":{"title":"home","description":null,"default":{"address":"Lakelands 2","country":"Iceland"},"required":false,"type":"object","properties":{"address":{"title":"address","description":"Address description","default":"Lakelands 2","required":false,"type":"string"},"country":{"title":"country","description":"Country description","default":"Iceland","required":false,"type":"string"}}}}}

scala> ujson.write(js, indent = 2)
res8: String =
{
  "$id": "http://my.user.app/schema.json",
  "title": "My User App",
  "description": "My User APP description",
  "type": "object",
  "properties": {
    "name": {
      "title": "name",
      "description": "Name description",
      "default": "John",
      "required": false,
      "type": "string"
    },
    "age": {
      "title": "age",
      "description": "Age description",
      "default": 42,
      "required": false,
      "type": "number"
    },
    "home": {
      "title": "home",
      "description": null,
      "default": {
        "address": "Lakelands 2",
        "country": "Iceland"
      },
      "required": false,
      "type": "object",
      "properties": {
        "address": {
          "title": "address",
          "description": "Address descripti...
```
