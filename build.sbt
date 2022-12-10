import scala.sys.process._
import laika.rewrite.link.LinkConfig
import laika.rewrite.link.ApiLinks
import laika.theme.Theme

// https://typelevel.org/sbt-typelevel/faq.html#what-is-a-base-version-anyway
ThisBuild / tlBaseVersion := "0.0" // your current series x.y

ThisBuild / organization := "creativescala"
ThisBuild / organizationName := "Creative Scala"
ThisBuild / startYear := Some(2022)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
  // your GitHub handle and name
  tlGitHubDev("noelwelsh", "Noel Welsh")
)

ThisBuild / tlSonatypeUseLegacyHost := true

lazy val scala213 = "2.13.8"
lazy val scala3 = "3.1.3"

ThisBuild / crossScalaVersions := Seq(scala213, scala3)
ThisBuild / scalaVersion := scala213 // the default Scala
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
ThisBuild / tlSitePublishBranch := Some("main")

// Run this (build) to do everything involved in building the project
commands += Command.command("build") { state =>
  "dependencyUpdates" ::
    "compile" ::
    "test" ::
    "scalafixAll" ::
    "scalafmtAll" ::
    "docs/tlSite" ::
    state
}
lazy val css = taskKey[Unit]("Build the CSS")

lazy val root = tlCrossRootProject.aggregate(core, docs)

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(
    name := "case-study-parser",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % "2.8.0",
      "org.typelevel" %%% "cats-effect" % "3.3.14",
      "org.scalameta" %%% "munit" % "0.7.29" % Test,
      "org.typelevel" %%% "munit-cats-effect-3" % "1.0.7" % Test
    )
  )

lazy val docs = project
  .in(file("site"))
  .settings(
    mdocIn := file("docs/pages"),
    css := {
      val src = file("docs/css")
      val dest1 = mdocOut.value
      val dest2 = (laikaSite / target).value
      val cmd1 =
        s"npx tailwindcss -i ${src.toString}/creative-scala.css -o ${dest1.toString}/creative-scala.css"
      val cmd2 =
        s"npx tailwindcss -i ${src.toString}/creative-scala.css -o ${dest2.toString}/creative-scala.css"
      cmd1 !

      cmd2 !
    },
    Laika / sourceDirectories += file("docs/templates"),
    laikaTheme := Theme.empty,
    laikaExtensions ++= Seq(
      laika.markdown.github.GitHubFlavor,
      laika.parse.code.SyntaxHighlighting,
      CreativeScalaDirectives
    ),
    tlSite := Def
      .sequential(
        mdoc.toTask(""),
        css,
        laikaSite
      )
      .value
  )
  .enablePlugins(TypelevelSitePlugin)
