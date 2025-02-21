ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.1"

lazy val root = (project in file("."))
  .settings(
    name := "Twisterella",
    resolvers += "Bitwig Maven Repository" at "https://maven.bitwig.com",
    libraryDependencies += "com.bitwig" % "extension-api" % "18" % "provided",
    libraryDependencies += "org.json" % "json" % "20231013",
    libraryDependencies += "org.tomlj" % "tomlj" % "1.1.0",
    logLevel := Level.Error,
    scalacOptions ++= Seq(
      "-deprecation",
      "-Wunused:imports,privates,locals,explicits"
    )
  )
