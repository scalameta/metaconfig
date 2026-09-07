# Contributing

Please refer to the
[Scalameta](https://github.com/scalameta/scalameta/blob/master/CONTRIBUTING.md)
contributing guidelines to learn more about how to report tickets and open pull
requests.

## sbt

Every matrix cell carries its platform and its Scala version, so a command has
to name one: `testsJVM2_13/testFull`, not `tests/testFull`. The build generates
an alias per version for the sets CI runs, such as `test-jvm-2_13`, `test-2_13`
and `compile-2_12`, so no workflow step spells out a cell id. `++` selects
nothing: it switches the Scala version on the cells that accept it and leaves
aggregation alone.

## IntelliJ

IntelliJ imports the project for one Scala version, 2.13 by default,
because it cannot import the whole matrix: it puts the sources that
several rows use into one module, and then compiles the Scala 2 and the
Scala 3 copies of `metaconfig.generic` and `metaconfig.pprint` together.
To change what it imports, set the properties below under
`Settings -> Build, Execution, Deployment -> Build Tools -> sbt -> VM parameters`
and reload the sbt project:

- `-Dide.scala=X`: sbt keeps only the rows for Scala version `X`, such as
  `2.12`, `2.13` or `3`. Without it, IntelliJ gets 2.13 and every other tool
  gets every version.
- `-Dide.platform=Y`: sbt keeps only the rows for the platforms in `Y`, a
  comma-separated list such as `jvm,js`. Without it sbt keeps every platform.

The build sets `bspEnabled := false` on the rows it drops. An sbt server that
is already running uses the properties from its own command line, so run `sbt
shutdown` before you test a change from the shell.

## Website

The website is built with [GitBook](https://www.npmjs.com/package/gitbook-cli).
To install GitBook

```
npm install -g gitbook-cli
```

A the base directory of this repo

```
gitbook install
```

Open an sbt shell session and run `website/makeSite`

```
sbt
> website/makeSite
```

This will generate a static GitBook site in the directory `website/target/site`.
To preview the website locally

```
cd website/target/site
gitbook serve
open http://localhost:4000
```

Re-run `makeSite` for every edit in `docs/README.md`. Generating the website can
take ~10 seconds since the code examples are type-checked with tut.
