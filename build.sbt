name := "reactive-mongo"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.1",
  ("org.reactivemongo" %% "reactivemongo" % "0.10.0").excludeAll(ExclusionRule(organization = "org.apache.logging.log4j")),
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "com.typesafe.akka" %% "akka-slf4j" % "2.2.3",
  "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.0-rc1"
)

