scalaVersion := "2.13.5"

val circeVersion = "0.13.0"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.4.1",
  "dev.zio" %% "zio" % "1.0.7",
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.scalaj" %% "scalaj-http" % "2.4.2",
  "org.scalameta" %% "munit" % "0.7.25" % Test
)

testFrameworks += new TestFramework("munit.Framework")
