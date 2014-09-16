name := "tangela"

version := "1.0-SNAPSHOT"

scalacOptions ++= Seq("-Xmax-classfile-name", "100")

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "com.github.tototoshi" %% "scala-csv" % "1.0.0"
)     

play.Project.playScalaSettings

play.Keys.lessEntryPoints <<= baseDirectory(_ / "app" / "assets" / "stylesheets" / "bootstrap" ** "bootstrap.less")
