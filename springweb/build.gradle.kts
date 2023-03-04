plugins {
    java
    id("org.springframework.boot") version "2.7.7-SNAPSHOT"
    id("io.spring.dependency-management") version "1.0.15.RELEASE"
}

group = "com.webnori"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}


dependencies {
    implementation("org.projectlombok:lombok:1.18.22")
    val scalaVersion = "2.13"
    val akkaVersion = "2.7.0"

    implementation("org.springframework.boot:spring-boot-starter-web")

    // Akka Actors
    implementation("com.typesafe.akka", "akka-bom_$scalaVersion", akkaVersion)
    implementation("com.typesafe.akka", "akka-actor-typed_$scalaVersion", akkaVersion)

    // Logging
    implementation("ch.qos.logback", "logback-classic", "1.2.3")
    implementation("com.typesafe.akka", "akka-slf4j_$scalaVersion", akkaVersion)

    // Akka Actors TestKit
    testImplementation("com.typesafe.akka", "akka-actor-testkit-typed_$scalaVersion", akkaVersion)
    testImplementation("org.springframework.boot:spring-boot-starter-test")

	// Swagger
	implementation("org.springdoc", "springdoc-openapi-ui","1.6.6")


}

tasks.withType<Test> {
    useJUnitPlatform()
}
