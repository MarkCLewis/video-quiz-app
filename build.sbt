name := """video-quizzes"""

version := "0.2"

lazy val scalaV = "2.11.8" 

lazy val root = (project in file(".")).settings(
	scalaVersion := scalaV,
//	scalaJSProjects := Seq(client),
//	pipelineStages := Seq(scalaJSProd),
	libraryDependencies ++= Seq(
	  "com.typesafe.play" %% "play-slick" % "2.0.0",
	  //"org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
	  cache,
	  ws,
	  specs2 % Test,
	  "mysql" % "mysql-connector-java" % "5.1.36",
	  "com.typesafe.slick" %% "slick-codegen" % "3.1.1",

      "net.codingwell" %% "scala-guice" % "4.0.1",	  
	  "com.mohiva" %% "play-silhouette" % "4.0.0",
      "com.mohiva" %% "play-silhouette-password-bcrypt" % "4.0.0",
      "com.mohiva" %% "play-silhouette-crypto-jca" % "4.0.0",
      "com.mohiva" %% "play-silhouette-persistence" % "4.0.0",
      "com.mohiva" %% "play-silhouette-testkit" % "4.0.0" % "test"
	)
).enablePlugins(PlayScala)

//lazy val client = (project in file("modules/client")).settings(
//  scalaVersion := scalaV,
//  persistLauncher := true,
//  persistLauncher in Test := false,
//  scalaJSStage in Global := FastOptStage,
//  libraryDependencies ++= Seq(
//    "org.scala-js" %%% "scalajs-dom" % "0.8.0"
//  ),
//  skip in packageJSDependencies := false,
//  testFrameworks += new TestFramework("utest.runner.Framework")
//).enablePlugins(ScalaJSPlugin, ScalaJSPlay, SbtWeb)

//resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
//resolvers += Resolver.jcenterRepo

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator


fork in run := false
