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
libraryDependencies += "com.geirsson" %% "metaconfig-core" % "0.6.0"

// Use https://github.com/lightbend/config to parse HOCON
libraryDependencies += "com.geirsson" %% "metaconfig-typesafe-config" % "0.6.0"
```

Use this import to access the metaconfig API

```scala
import metaconfig._
```

All of the following code examples assume that you have `import metaconfig._` in scope.

<!-- TOC -->

* [Metaconfig](#metaconfig)
  * [Getting started](#getting-started)
  * [Conf](#conf)
  * [Conf.parse](#confparse)
  * [ConfDecoder](#confdecoder)
  * [ConfError](#conferror)
  * [Configured](#configured)
  * [generic.deriveSurface](#genericderivesurface)
  * [generic.deriveDecoder](#genericderivedecoder)
    * [Limitations](#limitations)
  * [@DeprecatedName](#deprecatedname)
  * [Docs](#docs)
  * [Conf.parseCliArgs](#confparsecliargs)
  * [Settings.toCliHelp](#settingstoclihelp)

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

You need an implicit `MetaconfigParser` to convert HOCON into `Conf`.
Assuming you depend on the `metaconfig-typesafe-config` module,

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
res3: metaconfig.Configured[metaconfig.Conf] = Ok({"align": "none", "project": {"git": true}, "assumeStandardLibraryStripMargin": true})
```

Note. The example above is JVM-only.
For a Scala.js alternative, depend on the `metaconfig-hocon` module and replace `metaconfig.typesafeconfig` with

```scala
import metaconfig.hocon._
```

## ConfDecoder

To convert `Conf` into higher-level data structures you need a `ConfDecoder[T]` instance.
Convert a partial function from `Conf` to your target type using `ConfDecoder.instance[T]`.

```scala
val number2 = ConfDecoder.instance[Int] {
    case Conf.Str("2") => Configured.Ok(2)
}
```

```scala
scala> number2.read(Conf.fromString("2"))
res4: metaconfig.Configured[Int] = Ok(2)

scala> number2.read(Conf.fromInt(2))
res5: metaconfig.Configured[Int] =
NotOk(Type mismatch;
  found    : Number (value: 2)
  expected : int)
```

Convert a regular function from `Conf` to your target type using `ConfDecoder.instanceF[T]`.

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
res6: metaconfig.Configured[User] = Ok(User(Susan,29))

scala> decoder.read(Conf.parseString("""
     | name = 42
     | age = "Susan"
     | """))
res7: metaconfig.Configured[User] =
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
fileDecoder: metaconfig.ConfDecoder[java.io.File] = metaconfig.ConfDecoder$$anon$1@374affee

scala> fileDecoder.read(Conf.fromString(".scalafmt.conf"))
res8: metaconfig.Configured[java.io.File] = Ok(.scalafmt.conf)

scala> fileDecoder.read(Conf.fromString(".foobar"))
res9: metaconfig.Configured[java.io.File] = NotOk(File /Users/ollie/dev/metaconfig/.foobar does not exist.)
```

## ConfError

`ConfError` is a helper to produce readable and potentially aggregated error messages.

```scala
scala> ConfError.message("Not good!")
res10: metaconfig.ConfError = Not good!

scala> ConfError.exception(new IllegalArgumentException("Expected String!"), stackSize = 2)
res11: metaconfig.ConfError =
java.lang.IllegalArgumentException: Expected String!
	at .<init>(<console>:19)
	at .<clinit>(<console>)

scala> ConfError.typeMismatch("Int", "String", "field")
res12: metaconfig.ConfError =
Type mismatch at 'field';
  found    : String
  expected : Int

scala> ConfError.message("Failure 1").combine(ConfError.message("Failure 2"))
res13: metaconfig.ConfError =
2 errors
[E0] Failure 1
[E1] Failure 2
```

Metaconfig uses Scalameta `Input` to represent an input source and `Position` to represent range positions in a given `Input`

```scala
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

```scala
scala> ConfError.parseError(pos, "No var")
res14: metaconfig.ConfError =
foo.scala:3: error: No var
var x
  ^
```

## Configured

`Configured[T]` is like an `Either[metaconfig.ConfError, T]` which is used througout the metaconfig API to either represent a successfully parsed/decoded value or a failure.

```scala
scala> Configured.ok("Hello world!")
res15: metaconfig.Configured[String] = Ok(Hello world!)

scala> Configured.ok(List(1, 2))
res16: metaconfig.Configured[List[Int]] = Ok(List(1, 2))

scala> val error = ConfError.message("Boom!")
error: metaconfig.ConfError = Boom!

scala> val configured = error.notOk
configured: metaconfig.Configured[Nothing] = NotOk(Boom!)

scala> configured.toEither
res17: Either[metaconfig.ConfError,Nothing] = Left(Boom!)
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
res19: Int = 42
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

The surface is used by metaconfig to support configurable decoding such as alternative fields names.
In the future, the plan is to use `Surface[T]` to automatically generate html/markdown documentation for configuration settings.
For now, you can ignore `Surface[T]` and just consider it as an annoying requirement from metaconfig.

## generic.deriveDecoder

Writing manual decoder by hand grows tiring quickly.
This becomes especially true when you have documentation to keep up-to-date as well.

```scala
implicit val decoder: ConfDecoder[User] =
  generic.deriveDecoder[User](User("John", 42)).noTypos
```

```scala
scala> ConfDecoder[User].read(Conf.parseString("""
     | name = Susan
     | age = 34
     | """))
res20: metaconfig.Configured[User] = Ok(User(Susan,34))

scala> ConfDecoder[User].read(Conf.parseString("""
     | nam = John
     | age = 23
     | """))
res21: metaconfig.Configured[User] = NotOk(Invalid field: nam. Expected one of name, age)

scala> ConfDecoder[User].read(Conf.parseString("""
     | name = John
     | age = Old
     | """))
res22: metaconfig.Configured[User] =
NotOk(Type mismatch;
  found    : String (value: "Old")
  expected : Number)
```

Sometimes automatic derivation fails, for example if your class contains fields that have no `ConfDecoder` instance

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

Observe that the error message is complaining about a missing `metaconfig.ConfDecoder[java.io.File]` implicit.

### Limitations

The following features are not supported by generic derivation

* derivation for objects, sealed traits or non-case classes, only case classes are supported
* parameterized types, it's possible to derive decoders for a concrete parameterized type like `Option[Foo]` but note that the type field (`Field.tpe`) will be pretty-printed to the generic representation of that field: `Option[T].value: T`.

## @DeprecatedName

As your configuration evolves, you may want to rename some settings but you have existing users who are using the old name.
Use the `@DeprecatedName` annotation to continue supporting the old name even if you go ahead with the rename.

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
res23: metaconfig.Configured[EvolvingConfig] = Ok(EvolvingConfig(false))

scala> decoder.read(Conf.Obj("isGoodName" -> Conf.fromBoolean(false)))
res24: metaconfig.Configured[EvolvingConfig] = Ok(EvolvingConfig(false))

scala> decoder.read(Conf.Obj("gooodName" -> Conf.fromBoolean(false)))
res25: metaconfig.Configured[EvolvingConfig] = NotOk(Invalid field: gooodName. Expected one of isGoodName)
```

## Docs

To generate documentation for you configuration, add a dependency to the following module

```scala
libraryDependencies += "com.geirsson" %% "metaconfig-docs" % "0.6.0"
```

First define your configuration

```scala
case class Home(
    @Description("Address description")
    address: String = "Lakelands 2",
    @Description("Country description")
    country: String = "Iceland"
)
implicit val homeSurface = generic.deriveSurface[Home]

case class User(
    @Description("Name description")
    name: String = "John",
    @Description("Age description")
    age: Int = 42,
    home: Home = Home()
)
implicit val userSurface = generic.deriveSurface[User]
```

To generate html documentation, pass in a default value

```scala
scala> docs.Docs.html(User())
res27: String = <table><thead><tr><th>Name</th><th>Type</th><th>Description</th><th>Default value</th></tr></thead><tbody><tr><td><code>name</code></td><td><code>String</code></td><td>Name description</td><td>John</td></tr><tr><td><code>age</code></td><td><code>Int</code></td><td>Age description</td><td>42</td></tr><tr><td><code>home.address</code></td><td><code>String</code></td><td>Address description</td><td>Lakelands 2</td></tr><tr><td><code>home.country</code></td><td><code>String</code></td><td>Country description</td><td>Iceland</td></tr></tbody></table>
```

The output will look like this when rendered in a markdown or html document


<table><thead><tr><th>Name</th><th>Type</th><th>Description</th><th>Default value</th></tr></thead><tbody><tr><td><code>name</code></td><td><code>String</code></td><td>Name description</td><td>John</td></tr><tr><td><code>age</code></td><td><code>Int</code></td><td>Age description</td><td>42</td></tr><tr><td><code>home.address</code></td><td><code>String</code></td><td>Address description</td><td>Lakelands 2</td></tr><tr><td><code>home.country</code></td><td><code>String</code></td><td>Country description</td><td>Iceland</td></tr></tbody></table>


The `Docs.html` method does nothing magical, it's possible to implement custom renderings by inspecting `Settings[T]` directly.

```scala
scala> Settings[User].settings
res29: List[metaconfig.generic.Setting] = List(Setting(Field(name="name",tpe="String",annotations=List(@Description(Name description)),underlying=List())), Setting(Field(name="age",tpe="Int",annotations=List(@Description(Age description)),underlying=List())), Setting(Field(name="home",tpe="Home",annotations=List(),underlying=List(List(Field(name="address",tpe="String",annotations=List(@Description(Address description)),underlying=List()), Field(name="country",tpe="String",annotations=List(@Description(Country description)),underlying=List()))))))

scala> val flat = Settings[User].flat(User())
flat: List[(metaconfig.generic.Setting, Any)] = List((Setting(Field(name="name",tpe="String",annotations=List(@Description(Name description)),underlying=List())),John), (Setting(Field(name="age",tpe="Int",annotations=List(@Description(Age description)),underlying=List())),42), (Setting(Field(name="home.address",tpe="String",annotations=List(@Description(Address description)),underlying=List())),Lakelands 2), (Setting(Field(name="home.country",tpe="String",annotations=List(@Description(Country description)),underlying=List())),Iceland))

scala> flat.map { case (setting, defaultValue) =>
     |   s"Setting ${setting.name} of type ${setting.tpe} has default value $defaultValue"
     | }.mkString("\n==============\n")
res30: String =
Setting name of type String has default value John
==============
Setting age of type Int has default value 42
==============
Setting home.address of type String has default value Lakelands 2
==============
Setting home.country of type String has default value Iceland
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
  @ExtraName("alternativeArgs")
  files: List[String] = Nil
)
implicit val surface = generic.deriveSurface[App]
implicit val decoder = generic.deriveDecoder[App](App())
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
app: metaconfig.Configured[App] = Ok(App(/tmp,true,List()))
```

## Settings.toCliHelp

Generate a --help message with a `Settings[T]`.

```scala
scala> Settings[App].toCliHelp(default = App())
res31: String =
--target: String = out        The directory to output files
--verbose: Boolean = false    Print out debugging diagnostics
--files: List[String] = List()The input files for app
```
