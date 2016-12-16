import com.typesafe.sbt.pgp.PgpKeys

crossScalaVersions in ThisBuild := Seq("2.12.1", "2.11.8")

lazy val core = project
  .settings(publishSettings)
  .settings(coreSettings)
  .settings(compileSettings)

lazy val test = project
  .dependsOn(core)
  .settings(coreSettings)
  .settings(compileSettings)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "utest" % "0.4.4" % "test",
      "com.github.alexarchambault" %% "argonaut-shapeless_6.2" % "1.2.0-M4"
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val doc = project
  .dependsOn(core)
  .settings(compileSettings)
  .settings(noPublishSettings)
  .settings(tutSettings)
  .settings(
    tutSourceDirectory := baseDirectory.value,
    tutTargetDirectory := baseDirectory.value / ".."
  ).settings(
    libraryDependencies += "com.github.alexarchambault" %% "argonaut-shapeless_6.2" % "1.2.0-M4"
  )

val shapelessVersion = "2.3.2"

lazy val coreName = "elmtypes"

lazy val coreSettings = Seq(
  organization := "org.reactormonk",
  name := coreName,
  libraryDependencies ++= Seq(
    "com.chuusai" %% "shapeless" % shapelessVersion
  )
)

lazy val compileSettings = Seq(
  scalaVersion := "2.12.1",
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases")
  ),
  libraryDependencies +=
    compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
)

lazy val publishSettings = Seq(
  licenses := Seq(
    "BSD-3-Clause" -> url("http://www.opensource.org/licenses/BSD-3-Clause")
  ),
  scmInfo := Some(ScmInfo(
    url("https://github.com/reactormonk/scala-elm-types.git"),
    "scm:git:github.com/reactormonk/scala-elm-types.git",
    Some("scm:git:git@github.com:reactormonk/scala-elm-types.git")
  )),
  homepage := Some(url("https://github.com/reactormonk/scala-elm-types")),
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

// build.sbt shamelessly inspired by https://github.com/fthomas/refined/blob/master/build.sbt
addCommandAlias("validate", Seq(
  "test",
  "tut"
).mkString(";", ";", ""))
