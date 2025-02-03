// scalafmt: { maxColumn = 100, align.preset = more, align.allowOverflow = true }

val crossProjectV = "1.3.2"

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.0")

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.11.0")
addSbtPlugin("com.github.sbt" % "sbt-ci-release"      % "1.9.2")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.4")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject"      % crossProjectV)
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % crossProjectV)

addSbtPlugin("org.scalameta"    % "sbt-mdoc"         % "2.6.3")
addSbtPlugin("org.scala-js"     % "sbt-scalajs"      % "1.18.2")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.5.6")
