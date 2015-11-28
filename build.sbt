import Keys._

import android.Keys._

android.Plugin.androidBuild

name := "reactive-ui-sample"
organization := "com.geteit"
version := "0.0.1"
versionCode := Some(1)

scalaVersion := "2.11.7"

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")
scalacOptions ++= Seq("-feature", "-language:implicitConversions", "-language:postfixOps", "-target:jvm-1.7")
platformTarget in Android := "android-21"

resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.sonatypeRepo("releases")
)

fork in Test := true
publishArtifact in (Compile, packageDoc) := false
publishArtifact in Test := false
publishArtifact in Compile := false

proguardOptions in Android ++= io.Source.fromFile("proguard.txt").getLines.toSeq

// don't include jni libs in apk file
collectJni in Android := { List() }

// don't include assets - we don't use them
collectResources in Android := {
  val (assets, res) = (collectResources in Android).value
  (assets ** "*").get.foreach(_.delete())
  (assets, res)
}

libraryProject in Android := false

transitiveAndroidLibs in Android := true

useProguard in Android := true
useProguardInDebug in Android := (useProguard in Android).value
typedResources in Android := false
dexMulti in Android := false
dexMaxHeap in Android := "2048M"

val supportLibVersion = "22.2.0"

libraryDependencies ++= Seq (
  "com.geteit" %% "geteit-utils" % "0.3",
  "com.geteit" %% "geteit-app" % "0.1",
  "com.koushikdutta.async" % "androidasync" % "2.1.5"
)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full)

