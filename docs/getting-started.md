---
id: getting-started
title: Getting started
---

MOpt is a library to manage configuration options for Scala applications
with the following goals:

- **Model configuration as Scala data structures**: MOpt allows you to
  manage all user configuration options as immutable case classes and sealed
  traits.
- **Limit boilerplate where possible**: MOpt provides automatic
  configuration decoders and encoders to help you avoid copy-pasting the same
  setting name in multiple places like the application implementation,
  configuration parser and configuration documentation. Copy-pasting strings is
  both cumbersome and it also increases the risk of making mistakes resulting in
  a bad end-user experience.
- **Evolve configuration without breaking changes**: it's normal that
  configuration options change as your application evolves (naming is hard).
  MOpt supports several ways to evolve user configuration options in a
  backwards compatible way so that your existing users have an easier time to
  upgrade to the latest versions of your application.
- **Report helpful error messages**: MOpt reports errors using source
  positions in the user-written configuration files, similar to how a compiler
  reports errors.
- **Treat command-line arguments as configuration**: MOpt provides a
  command-line parser with automatic generation of `--help` messages, tab
  completions for bash/zsh and more. Command-line arguments map into Scala case
  classes, just like HOCON and JSON configuration.

## Quick start

```scala
libraryDependencies += "com.geirsson" %% "mopt-typesafe-config" % "@VERSION@"
```

Next, write a case class for your user configuration.

```scala mdoc
case class HelloConfig(
  verbose: Boolean = false,
  name: String = "Susan"
)
object HelloConfig {
  lazy val default = HelloConfig()
  implicit lazy val surface = mopt.generic.deriveSurface[HelloConfig]
  implicit lazy val decoder = mopt.generic.deriveDecoder[HelloConfig](default)
  implicit lazy val encoder = mopt.generic.deriveEncoder[HelloConfig]
}
```

Next, parse HOCON into your case class

```scala mdoc
mopt.Hocon.parseString[HelloConfig](
  """
  verbose = true
  name = John
  """
).get
mopt.Hocon.parseString[HelloConfig](
  """
  verrbose = true # typo is ignored
  name = John
  """
).get
mopt.Hocon.parseString[HelloConfig](
  """
  verrbose = true # typo is an error
  name = John
  """
)(HelloConfig.decoder.noTypos)
```

Use `parseCommandLine` to parse command-line arguments.

```scala mdoc
mopt.Conf.parseCliArgs[HelloConfig](
  List("--verbose", "--name", "Amelie")
).andThen(_.as[HelloConfig]).get
mopt.Conf.parseCliArgs[HelloConfig](
  List("--verbose", "--names", "Amelie")
).andThen(_.as[HelloConfig])
```
