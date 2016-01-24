import Keys._

import android.Keys._

android.Plugin.androidBuild

name := "reactive-ui-sample"
organization := "com.geteit"
version := "0.0.2"

scalaVersion := "2.11.7"

platformTarget in Android := "android-23"

publishArtifact in (Compile, packageDoc) := false
publishArtifact in Test := false
publishArtifact in Compile := false

proguardOptions in Android ++= io.Source.fromFile("proguard.txt").getLines.toSeq
proguardCache := Nil

// don't include jni libs in apk file
collectJni in Android := { List() }

// don't include assets - we don't use them
collectResources in Android := {
  val (assets, res) = (collectResources in Android).value
  (assets ** "*").get.foreach(_.delete())
  (assets, res)
}

typedResources in Android := false
dexMulti in Android := false
dexMaxHeap in Android := "2048M"

libraryDependencies ++= Seq (
  "com.android.support" % "support-v4" % "23.1.0",
  "com.android.support" % "appcompat-v7" % "23.1.0",
  "com.geteit" %% "geteit-utils" % "0.3",
  "com.geteit" %% "geteit-app" % "0.1"
)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full)
