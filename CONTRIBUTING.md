# Contributing

Please refer to the
[Scalameta](https://github.com/scalameta/scalameta/blob/master/CONTRIBUTING.md)
contributing guidelines to learn more about how to report tickets and open pull
requests.

## IntelliJ

IntelliJ imports the project for one Scala version, 2.13 by default,
because it puts the sources that the matrix cells share into a single
module: importing every cell compiles the Scala 2 and the Scala 3 copies
of `metaconfig.generic` and `metaconfig.pprint` together. If you need to
modify the defaults, set the properties below under
`Settings -> Build, Execution, Deployment -> Build Tools -> sbt -> VM parameters`
and reload the sbt project:

- `-Dide.scala=X`: imports Scala version `X` instead (could be `2.12`, `2.13`,
  or `3`). `-Dide.scala=`, with no value, imports the default.
- `-Dide.platform=Y`: if `Y` is empty, imports all platforms; otherwise, `Y` is
  a comma-separated list of platforms to import, and `jvm` is implied, whether
  or not it is explicitly listed, while `js` and `native` are optional.

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
