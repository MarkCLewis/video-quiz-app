name := """video-quiz-play-2.6"""

version := "0.3"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += Resolver.jcenterRepo
resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
		guice,
		"org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
		"mysql" % "mysql-connector-java" % "6.0.6",
		"com.typesafe.play" %% "play-slick" % "3.0.0",
		"com.typesafe.slick" %% "slick-codegen" % "3.2.1",
		"com.mohiva" %% "play-silhouette" % "5.0.3",
		"com.mohiva" %% "play-silhouette-password-bcrypt" % "5.0.3",
		"com.mohiva" %% "play-silhouette-crypto-jca" % "5.0.3",
		"com.mohiva" %% "play-silhouette-persistence" % "5.0.3",
		"com.mohiva" %% "play-silhouette-testkit" % "5.0.3" % "test",
		"net.codingwell" %% "scala-guice" % "4.1.1",
		"com.iheart" %% "ficus" % "1.4.1"
	)
