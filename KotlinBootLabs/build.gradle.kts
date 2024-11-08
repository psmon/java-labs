
plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("kapt") version "2.0.0"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.3.4"
	id("io.spring.dependency-management") version "1.1.6"
	id("me.champeau.jmh") version "0.7.0"
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
val akkaR2DBC = "1.1.2"
val alphakkaVersion = "4.0.0"

dependencies {
	// Core
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

	// Reactive
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")


	// Jwt
	implementation("io.jsonwebtoken:jjwt-api:0.11.5")
	implementation("io.jsonwebtoken:jjwt-impl:0.11.5")
	implementation("io.jsonwebtoken:jjwt-jackson:0.11.5")
	// or jjwt-gson, jjwt-orgjson, etc.

	// Swagger
	// Swagger / OpenAPI
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")

	// Actor System
	implementation(platform("com.typesafe.akka:akka-bom_$scalaVersion:$akkaVersion"))

	// Classic Actor
	implementation("com.typesafe.akka:akka-actor_$scalaVersion:$akkaVersion")
	implementation("com.typesafe.akka:akka-stream_$scalaVersion:$akkaVersion")

	// Typed Actor
	implementation("com.typesafe.akka:akka-actor-typed_$scalaVersion:$akkaVersion")
	implementation("com.typesafe.akka:akka-serialization-jackson_$scalaVersion:$akkaVersion")

	// Cluster Actor
	implementation("com.typesafe.akka:akka-cluster-typed_$scalaVersion:$akkaVersion")


	// Actor Persistence
	implementation("com.typesafe.akka:akka-persistence-typed_$scalaVersion:$akkaVersion")
	implementation("com.typesafe.akka:akka-persistence-query_$scalaVersion:$akkaVersion")
	implementation("com.lightbend.akka:akka-persistence-r2dbc_$scalaVersion:$akkaR2DBC")

	// Actor Logging
	implementation("com.typesafe.akka:akka-slf4j_$scalaVersion:$akkaVersion")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Actor TestKit
	testImplementation("com.typesafe.akka:akka-testkit_$scalaVersion:$akkaVersion")
	testImplementation("com.typesafe.akka:akka-actor-testkit-typed_$scalaVersion:$akkaVersion")
	testImplementation("com.typesafe.akka:akka-stream-testkit_$scalaVersion:$akkaVersion")
	testImplementation("com.typesafe.akka:akka-persistence-testkit_$scalaVersion:$akkaVersion")
	implementation("com.typesafe.akka:akka-multi-node-testkit_$scalaVersion:$akkaVersion")


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

	// Performance TEST
	//implementation("org.openjdk.jmh:jmh-core:1.35")
	//annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.35")
	testImplementation("org.openjdk.jmh:jmh-core:1.35")
	testAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.35")


}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<JavaExec> {
	if (name == "jmhRun") {
		val classpathFile = layout.buildDirectory.file("tmp/jmh-classpath.txt").get().asFile
		doFirst {
			classpathFile.writeText(classpath.asPath)
		}
		classpath = files(classpathFile)
		args = listOf("-cp", "@${classpathFile.path}") + (args ?: emptyList())
	}
}

jmh {
	jvmArgs = listOf("-Xms1g", "-Xmx1g")
	includes.add(".*")
}
