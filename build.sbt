organization := "com.gu"

name := "zuora-6for6-modifier"

version := "0.1-SNAPSHOT"

scalaVersion := "2.13.1"

val circeVersion = "0.12.3"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.4.0",
  "dev.zio" %% "zio" % "1.0.0-RC17",
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.scalaj" %% "scalaj-http" % "2.4.2"
)
