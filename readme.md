# Metaconfig

[![Maven](https://img.shields.io/maven-central/v/com.geirsson/metaconfig_2.12.svg?label=maven)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22metaconfig-core_2.12%22)
[![Travis](https://travis-ci.org/olafurpg/metaconfig.svg?branch=master)](https://travis-ci.org/olafurpg/metaconfig)

Configuration library used by
[scalafix](https://github.com/scalacenter/scalafix) and
[scalafmt](https://github.com/scalameta/scalafmt).
This project will soon be abandoned in favor of a more established
alternative such as

- https://github.com/circe/circe
- https://github.com/pureconfig/pureconfig
- http://cir.is/

## Changelog

* 0.2.0 Introduce `metaconfig.Conf`, a json-like ast, and remove all runtime reflection.
  - To migrate from 0.1.x, replace all matches on `Int/String/Boolean/Seq/Map`
    with `Conf.Num/Str/Bool/Lst/Obj`.
* 0.1.3 Cross-build to Scala.js. NOTE. Depends on scala.meta pre-release which is available
  with `resolvers += Resolver.bintrayIvyRepo("scalameta", "maven")`.
* 0.1.2 Skipped.
* 0.1.1 Upgraded paradise dependency from Bintray snapshot to Sonatype release.
* 0.1.0 First release, moved code out of scalafmt repo into here.
