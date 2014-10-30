name := "tangela"

version := "0.2"

scalacOptions ++= Seq("-Xmax-classfile-name", "100")

libraryDependencies ++= Seq(
  jdbc,
  cache,
  "com.typesafe.play" % "play-slick_2.10" % "0.5.0.8",
  "com.github.tototoshi" %% "scala-csv" % "1.0.0",
  "com.typesafe.slick" % "slick_2.10" % "1.0.1",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "org.bitcoinj" % "orchid" % "1.0"
)

play.Project.playScalaSettings

play.Keys.lessEntryPoints <<= baseDirectory(_ / "app" / "assets" / "stylesheets" / "bootstrap" ** "bootstrap.less")
