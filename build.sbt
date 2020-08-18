scalaVersion := "2.13.2"

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq("com.google.apis" % "google-api-services-youtube" % "v3-rev222-1.25.0",
  "com.google.api-client" % "google-api-client" % "1.30.6",
  "com.google.api-client" % "google-api-client-java6" % "1.30.6",
  "com.google.oauth-client" % "google-oauth-client-jetty" % "1.30.6",
  "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
  "com.typesafe.play" %% "play-json" % "2.8.1",
  "org.apache.opennlp" % "opennlp-tools" % "1.9.2",
  "com.typesafe.akka" %% "akka-actor" % "2.6.6",
  "com.typesafe.akka" %% "akka-stream" % "2.6.6",
  "com.typesafe.akka" % "akka-actor-typed_2.13" % "2.6.8",
  "org.scalactic" %% "scalactic" % "3.2.0",
  "org.scalatest" %% "scalatest" % "3.2.0" % "test",
  "com.typesafe.akka" %% "akka-http" % "10.2.0")

parallelExecution in Test := false
scalacOptions += "-deprecation"
scalacOptions += "-feature"
fork in run := true