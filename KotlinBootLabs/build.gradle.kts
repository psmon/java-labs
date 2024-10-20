plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.3.4"
	id("io.spring.dependency-management") version "1.1.6"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

val scalaVersion = "2.13"
val akkaVersion = "2.7.0"
val pekkoVersion = "1.1.2"

dependencies {
	// Core
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

	// Jwt
	implementation("io.jsonwebtoken:jjwt-api:0.11.5")
	implementation("io.jsonwebtoken:jjwt-impl:0.11.5")
	implementation("io.jsonwebtoken:jjwt-jackson:0.11.5")
	// or jjwt-gson, jjwt-orgjson, etc.

	// Swagger
	// Swagger / OpenAPI
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")

	// Akka
	//implementation(platform("com.typesafe.akka:akka-bom_$scalaVersion:$akkaVersion"))
	implementation(platform("org.apache.pekko:pekko-bom_$scalaVersion:$pekkoVersion"))

	// Akka UnTyped Actor
	//implementation("com.typesafe.akka:akka-actor_$scalaVersion:$akkaVersion")
	//implementation("com.typesafe.akka:akka-stream_$scalaVersion:$akkaVersion")
	implementation("org.apache.pekko:pekko-actor_$scalaVersion:$pekkoVersion")
	implementation("org.apache.pekko:pekko-stream_$scalaVersion:$pekkoVersion")

	// Akka Typed Actor
	//implementation("com.typesafe.akka:akka-actor-typed_$scalaVersion:$akkaVersion")
	implementation("org.apache.pekko:pekko-actor-typed_$scalaVersion:$pekkoVersion")

	// Actor TestKit
	//testImplementation("com.typesafe.akka:akka-testkit_$scalaVersion:$akkaVersion")
	//testImplementation("com.typesafe.akka:akka-actor-testkit-typed_$scalaVersion:$akkaVersion")
	testImplementation("org.apache.pekko:pekko-testkit_$scalaVersion:$pekkoVersion")
	testImplementation("org.apache.pekko:pekko-actor-testkit-typed_$scalaVersion:$pekkoVersion")

	// Actor Logging
	//implementation("com.typesafe.akka:akka-slf4j_$scalaVersion:$akkaVersion")
	implementation("org.apache.pekko:pekko-slf4j_$scalaVersion:$pekkoVersion")

	// Logging
	implementation("ch.qos.logback:logback-classic:1.4.12")

	// Only RUNTIME
	runtimeOnly("com.h2database:h2")

	// Only TEST
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
	testImplementation("com.squareup.okhttp3:okhttp:4.9.3")

}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
