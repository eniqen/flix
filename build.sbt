ThisBuild / organization := "com.boom"
name := "simple-problem"
version := "0.1"
scalaVersion := "2.13.5"

scalacOptions += "-Ymacro-annotations"

libraryDependencies ++= Dependencies.live

Dependencies.CompilerPlugins.live.map(addCompilerPlugin)