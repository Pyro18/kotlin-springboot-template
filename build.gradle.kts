import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.kotlin.jpa)
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
	alias(libs.plugins.jib)
	alias(libs.plugins.detekt)
	alias(libs.plugins.ktlint)
	kotlin("kapt") version "1.9.10"
}

group = "com.example"
version = "1.0.0"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/milestone") }
}

dependencies {
	// Spring Boot
	implementation(libs.bundles.spring.web)
	implementation(libs.bundles.spring.data)
	implementation(libs.bundles.spring.security)
	implementation(libs.spring.boot.starter.cache)
	implementation(libs.spring.boot.starter.data.redis)

	// Kotlin
	implementation(libs.kotlin.reflect)
	implementation(libs.kotlin.stdlib)
	implementation(libs.kotlinx.coroutines.core)
	implementation(libs.kotlinx.coroutines.reactor)

	// Documentation
	implementation(libs.springdoc.openapi.starter.webmvc.ui)

	// Jackson
	implementation(libs.jackson.module.kotlin)
	implementation(libs.jackson.datatype.jsr310)

	// MapStruct
	implementation(libs.mapstruct)
	kapt(libs.mapstruct.processor)

	// Monitoring
	implementation(libs.bundles.monitoring)

	// Redis
	implementation(libs.lettuce.core)

	// Functional Programming
	implementation(libs.arrow.core)
	implementation(libs.arrow.fx.coroutines)

	// JWT
	implementation(libs.jjwt.api)
	runtimeOnly(libs.jjwt.impl)
	runtimeOnly(libs.jjwt.jackson)

	// Configuration Processor
	kapt(libs.spring.boot.configuration.processor)

	// Testing
	testImplementation(libs.bundles.testing)
	testImplementation(libs.bundles.testcontainers)
}

kapt {
	arguments {
		arg("mapstruct.defaultComponentModel", "spring")
		arg("mapstruct.unmappedTargetPolicy", "IGNORE")
	}
}

tasks.withType<KotlinCompile> {
	compilerOptions {
		freeCompilerArgs.add("-Xjsr305=strict")
		jvmTarget.set(JvmTarget.JVM_21)
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
	systemProperty("spring.profiles.active", "test")
	testLogging {
		events("passed", "skipped", "failed")
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
		showStackTraces = true
	}
}

springBoot {
	buildInfo()
}

jib {
	from {
		image = "eclipse-temurin:21-jre-alpine"
		platforms {
			platform {
				architecture = "amd64"
				os = "linux"
			}
			platform {
				architecture = "arm64"
				os = "linux"
			}
		}
	}
	to {
		image = "${System.getenv("REGISTRY_URL") ?: "ghcr.io"}/${System.getenv("REGISTRY_NAMESPACE") ?: project.group}/${project.name}"
		tags = setOf(
			project.version.toString(),
			"latest",
			"${project.version}-${System.getenv("GITHUB_SHA")?.take(7) ?: "local"}"
		)
	}
	container {
		jvmFlags = listOf(
			"-XX:+UseContainerSupport",
			"-XX:MaxRAMPercentage=75.0",
			"-XX:InitialRAMPercentage=50.0",
			"-XX:+UseG1GC",
			"-XX:MaxGCPauseMillis=100",
			"-XX:+UseStringDeduplication"
		)
		ports = listOf("8080", "8081")
		environment = mapOf(
			"SPRING_PROFILES_ACTIVE" to "prod"
		)
		creationTime.set("USE_CURRENT_TIMESTAMP")
	}
}

detekt {
	buildUponDefaultConfig = true
	config.setFrom("$projectDir/config/detekt.yml")
	parallel = true
}

ktlint {
	version.set("1.0.1")
	outputToConsole.set(true)
	coloredOutput.set(true)
	reporters {
		reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
		reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.JSON)
	}
}

tasks.register("buildAndPush") {
	dependsOn("build", "jib")
	description = "Build and push Docker image to registry"
}