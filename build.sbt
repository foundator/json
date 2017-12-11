name := "foundator-json"
organization := "org.foundator"
version := "1.0-SNAPSHOT"
scalaVersion := "2.11.12"
crossScalaVersions := Seq(scalaVersion.value, "2.12.4")
libraryDependencies ++= Seq(

    "org.scalatest" %% "scalatest" % "3.0.4" % Test,
    "junit" % "junit" % "4.11" % Test,

    // Benchmark
    "com.fasterxml.jackson.core" % "jackson-core"  % "2.2.2" % Test,
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.2.2" % Test,
    
    // TODO: REMOVE
    "org.scala-lang" % "scala-reflect" % scalaVersion.value

)