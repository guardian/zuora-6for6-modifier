scalaVersion := "2.13.8"

val circeVersion = "0.14.1"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.4.2",
  "dev.zio" %% "zio" % "1.0.13",
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.scalaj" %% "scalaj-http" % "2.4.2",
  "org.scalameta" %% "munit" % "0.7.27" % Test
)

testFrameworks += new TestFramework("munit.Framework")
