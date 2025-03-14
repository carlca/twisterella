ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.4"

lazy val root = (project in file("."))
  .settings(
    name                := "Twisterella",
    resolvers           += "Bitwig Maven Repository" at "https://maven.bitwig.com",

    libraryDependencies += "com.bitwig" % "extension-api" % "20" % "provided",
    libraryDependencies += "org.json" % "json" % "20231013",
    libraryDependencies += "org.tomlj" % "tomlj" % "1.1.0",
    libraryDependencies += "ch.epfl.scala" %% "compat-metaconfig-macros" % "0.14.2",

    logLevel := Level.Error,
    semanticdbEnabled := true,
    scalacOptions ++= Seq(
      "-deprecation",
      "-Wunused:all"
    ),
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) =>
        List(
          "-Yrangepos",
          "-P:semanticdb:synthetics:on",
          "-P:semanticdb:text:on"
        )
      case Some((3, _)) =>
        List(
          "-Xsemanticdb",
          "-sourcetype:tasty"
        )
      case _ =>
        List()
    }
  }
)
