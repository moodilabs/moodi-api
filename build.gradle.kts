plugins {
	java
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.asciidoctor.jvm.convert") version "4.0.0"
}

group = "com.moodi"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

val snippetsDir = file("build/generated-snippets")
val appDocsOutDir = layout.buildDirectory.dir("docs/app")

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-json")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")

	runtimeOnly("org.postgresql:postgresql")

	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")

	developmentOnly("org.springframework.boot:spring-boot-docker-compose")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
	testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
	testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")

	testRuntimeOnly("com.h2database:h2")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.named("test") {
	outputs.dir(snippetsDir)
	(this as Test).useJUnitPlatform()
}

tasks.register("asciidoctorApp", org.asciidoctor.gradle.jvm.AsciidoctorTask::class) {
	group = "documentation"
	description = "Generate App API docs"

	inputs.dir(snippetsDir)
	dependsOn(tasks.named("test"))
	baseDirFollowsSourceFile()

	attributes(mapOf("snippets" to snippetsDir.absolutePath))

	setSourceDir(file("src/docs/asciidoc/app"))
	sources { include("index.adoc") }
	setOutputDir(appDocsOutDir.get().asFile)
}

tasks.bootJar {
	dependsOn(tasks.named("asciidoctorApp"))
	from(appDocsOutDir.get().asFile) {
		into("static/docs/app")
	}
}

tasks.register<Exec>("setupGitHooks") {
	commandLine("sh", "-c", "git config core.hooksPath .githooks && chmod +x .githooks/pre-push && echo 'Git hooks configured.'")
}
