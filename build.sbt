name := "akka-http-client-repro"

version := "0.1"

scalaVersion := "2.12.9"

Compile / run / fork := true
Compile / run / javaOptions ++= List(
  "-Dnewrelic.config.app_name=newrelic-repro-nadavwr",
  "-Dnewrelic.config.agent_enabled=true"
)

val newrelicVersion = "5.4.0"

enablePlugins(JavaAgent)
javaAgents += "com.newrelic.agent.java" % "newrelic-agent" % newrelicVersion % Runtime

libraryDependencies ++= List(
  "com.newrelic.agent.java" % "newrelic-api" % newrelicVersion,
  "com.typesafe.akka" %% "akka-http" % "10.1.9",
  "com.typesafe.akka" %% "akka-stream" % "2.5.23"
)

