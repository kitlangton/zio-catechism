import sbtcrossproject.CrossPlugin.autoImport.crossProject
import sbtcrossproject.CrossPlugin.autoImport.CrossType

name := "zio-catechism"


version in ThisBuild := "0.0.1"

scalaVersion in ThisBuild := Settings.versions.scala
scalacOptions in ThisBuild ++= Settings.scalacOptions


bloopExportJarClassifiers in Global := Some(Set("sources"))
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

val sharedSettings = Seq(
  addCompilerPlugin(
    "org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full
  ),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
)

lazy val frontend =
  (crossProject(JSPlatform).crossType(CrossType.Pure) in file(
    "modules/frontend"
  )).disablePlugins(RevolverPlugin)
    .jsSettings(
      scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
      libraryDependencies ++= Settings.frontendDependencies.value,
      libraryDependencies ++= Settings.sharedDependencies.value,
    )

