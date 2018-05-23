# Contributing

Please refer to the
[Scalameta](https://github.com/scalameta/scalameta/blob/master/CONTRIBUTING.md)
contributing guidelines to learn more about how to report tickets and open pull
requests.

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
