# Contributing

Please refer to the [Scalameta](https://github.com/scalameta/scalameta/blob/master/CONTRIBUTING.md)
contributing guidelines to learn more about how to report tickets and open pull requests.

## Website

The website is built with [GitBook](https://www.npmjs.com/package/gitbook-cli).
To install GitBook

```
npm install -g gitbook-cli
```

Then inside sbt

```
> website/makeSite
```
This will generate a static GitBook site in the directory `website/target/site`.

After running `website/makeSite`, preview the website locally with

```
cd website/target/site
gitbook serve
```
And open [localhost:4000](http://localhost:4000).


