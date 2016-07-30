import com.typesafe.sbt.pgp.PgpKeys

lazy val `elm-types` = project.in(file("."))
  .aggregate(core, test, doc)
  .settings(commonSettings)
  .settings(compileSettings)
  .settings(noPublishSettings)

lazy val core = project
  .settings(commonSettings)
  .settings(coreSettings)
  .settings(projectSettings)

lazy val test = project
  .dependsOn(core)
  .settings(commonSettings)
  .settings(coreSettings)
  .settings(projectSettings)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % "1.1.0-RC3" % "test",
      "com.lihaoyi" %% "utest" % "0.3.0" % "test"
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val doc = project
  .dependsOn(core)
  .settings(commonSettings)
  .settings(compileSettings)
  .settings(noPublishSettings)
  .settings(tutSettings)
  .settings(
    tutSourceDirectory := baseDirectory.value,
    tutTargetDirectory := baseDirectory.value / ".."
  )

val shapelessVersion = "2.3.1"

lazy val coreName = "elm-types"

lazy val coreSettings = Seq(
  organization := "com.github.alexarchambault",
  name := coreName,
  moduleName := coreName,
  libraryDependencies ++= Seq(
    "com.chuusai" %% "shapeless" % shapelessVersion
  )
)

lazy val projectSettings =
  compileSettings ++
  releaseSettings ++
  extraReleaseSettings

lazy val compileSettings = Seq(
  scalaVersion := "2.11.8",
  scalacOptions += "-target:jvm-1.7",
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases")
  ),
  libraryDependencies +=
    compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
)

lazy val commonSettings = Seq(
  licenses := Seq(
    "BSD-3-Clause" -> url("http://www.opensource.org/licenses/BSD-3-Clause")
  ),
  scmInfo := Some(ScmInfo(
    url("https://github.com/reactormonk/argonaut-shapeless.git"),
    "scm:git:github.com/reactormonk/argonaut-shapeless.git",
    Some("scm:git:git@github.com:reactormonk/argonaut-shapeless.git")
  )),
  developers := List(Developer(
    "reactormonk",
    "Simon Hafner",
    "",
    url("https://github.com/reactormonk")
  )),
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  credentials ++= {
    Seq("SONATYPE_USER", "SONATYPE_PASS").map(sys.env.get) match {
      case Seq(Some(user), Some(pass)) =>
        Seq(Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", user, pass))
      case _ =>
        Seq.empty
    }
  }
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val extraReleaseSettings = Seq(
  ReleaseKeys.versionBump := sbtrelease.Version.Bump.Bugfix,
  sbtrelease.ReleasePlugin.ReleaseKeys.publishArtifactsAction := PgpKeys.publishSigned.value
)

// build.sbt shamelessly inspired by https://github.com/fthomas/refined/blob/master/build.sbt
addCommandAlias("validate", Seq(
  "test",
  "tut"
).mkString(";", ";", ""))
