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
val pekkoJdbc = "1.1.0"
val pekkoJ2dbc = "1.0.0"
val slickVersion = "3.5.1"

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

	// Actor System
	//implementation(platform("com.typesafe.akka:akka-bom_$scalaVersion:$akkaVersion"))
	implementation(platform("org.apache.pekko:pekko-bom_$scalaVersion:$pekkoVersion"))

	// Classic Actor
	//implementation("com.typesafe.akka:akka-actor_$scalaVersion:$akkaVersion")
	//implementation("com.typesafe.akka:akka-stream_$scalaVersion:$akkaVersion")
	implementation("org.apache.pekko:pekko-actor_$scalaVersion:$pekkoVersion")
	implementation("org.apache.pekko:pekko-stream_$scalaVersion:$pekkoVersion")

	// Typed Actor
	//implementation("com.typesafe.akka:akka-actor-typed_$scalaVersion:$akkaVersion")
	implementation("org.apache.pekko:pekko-actor-typed_$scalaVersion:$pekkoVersion")

	implementation("org.apache.pekko:pekko-serialization-jackson_$scalaVersion:$pekkoVersion")


	// Actor Persistence
	//implementation("com.typesafe.akka:akka-persistence-typed_$scalaVersion:$akkaVersion")
	implementation("org.apache.pekko:pekko-persistence-typed_$scalaVersion:$pekkoVersion")
	implementation("org.apache.pekko:pekko-persistence-query_$scalaVersion:$pekkoVersion")
	implementation("org.apache.pekko:pekko-persistence-r2dbc_$scalaVersion:$pekkoJ2dbc")
	implementation("org.apache.pekko:pekko-persistence-jdbc_$scalaVersion:$pekkoJdbc")


	implementation("com.typesafe.slick:slick_$scalaVersion:$slickVersion")
	implementation("com.typesafe.slick:slick-hikaricp_$scalaVersion:$slickVersion")
	implementation("com.h2database:h2:2.2.220")
	implementation("org.postgresql:postgresql:42.7.2")
	implementation("org.postgresql:r2dbc-postgresql:1.0.7.RELEASE")

	// Actor Logging
	//implementation("com.typesafe.akka:akka-slf4j_$scalaVersion:$akkaVersion")
	implementation("org.apache.pekko:pekko-slf4j_$scalaVersion:$pekkoVersion")

	// Actor TestKit
	//testImplementation("com.typesafe.akka:akka-testkit_$scalaVersion:$akkaVersion")
	//testImplementation("com.typesafe.akka:akka-actor-testkit-typed_$scalaVersion:$akkaVersion")
	//testImplementation("com.typesafe.akka:akka-stream-testkit_$scalaVersion:$akkaVersion")
	//testImplementation("com.typesafe.akka:akka-persistence-testkit_$scalaVersion:$akkaVersion")
	testImplementation("org.apache.pekko:pekko-testkit_$scalaVersion:$pekkoVersion")
	testImplementation("org.apache.pekko:pekko-actor-testkit-typed_$scalaVersion:$pekkoVersion")
	testImplementation("org.apache.pekko:pekko-stream-testkit_$scalaVersion:$pekkoVersion")
	testImplementation("org.apache.pekko:pekko-persistence-testkit_$scalaVersion:$pekkoVersion")


	// Logging
	implementation("ch.qos.logback:logback-classic:1.4.12")

	// Test Local DB
	implementation("com.h2database:h2:2.2.220")

	// Only TEST
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
	testImplementation("com.squareup.okhttp3:okhttp:4.12.0")

}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
