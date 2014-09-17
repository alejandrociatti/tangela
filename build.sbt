name := "tangela"

version := "1.0-SNAPSHOT"

scalacOptions ++= Seq("-Xmax-classfile-name", "100")

libraryDependencies ++= Seq(
  jdbc,
  cache,
  "org.sorm-framework" % "sorm" % "0.3.15",
  "com.h2database" % "h2" % "1.3.168",
  "com.github.tototoshi" %% "scala-csv" % "1.0.0"
)     

play.Project.playScalaSettings

play.Keys.lessEntryPoints <<= baseDirectory(_ / "app" / "assets" / "stylesheets" / "bootstrap" ** "bootstrap.less")
