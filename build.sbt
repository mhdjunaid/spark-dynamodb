javacOptions ++= Seq("-source", "1.7", "-target", "1.7", "-Xlint")

lazy val root = (project in file(".")).
  settings(
    organization:="com.dcmn",
    name := "spark-dynamodb",
    version := "0.1.0",
    scalaVersion := "2.10.4",
    retrieveManaged := true,
    libraryDependencies += "com.amazonaws" % "aws-java-sdk" % "1.10.11",
    libraryDependencies += "org.apache.spark" %% "spark-sql" % "1.4.1",
    libraryDependencies += "com.google.guava" % "guava" % "14.0.1"
  )
publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}