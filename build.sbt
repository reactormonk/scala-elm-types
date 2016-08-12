import com.typesafe.sbt.pgp.PgpKeys

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
      "com.lihaoyi" %% "utest" % "0.3.0" % "test",
      "com.github.alexarchambault" %% "argonaut-shapeless_6.1" % "1.1.1"
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
  ).settings(
    libraryDependencies += "com.github.alexarchambault" %% "argonaut-shapeless_6.1" % "1.1.1"
  )

val shapelessVersion = "2.3.1"

lazy val coreName = "elmtypes"

lazy val coreSettings = Seq(
  organization := "org.reactormonk",
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
    url("https://github.com/reactormonk/scala-elm-types.git"),
    "scm:git:github.com/reactormonk/scala-elm-types.git",
    Some("scm:git:git@github.com:reactormonk/scala-elm-types.git")
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
