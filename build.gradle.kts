plugins {
	java
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.myfitness"
version = "0.0.1-SNAPSHOT"
description = "Personal fitness tracking application"

java {
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

repositories {
	mavenCentral()
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.modulith:spring-modulith-bom:2.1.1")
		mavenBom("org.springframework.ai:spring-ai-bom:2.0.1")
	}
}

dependencies {
	implementation("org.springframework.modulith:spring-modulith-api")
	implementation("org.springframework.ai:spring-ai-starter-model-openai")
	implementation("org.springframework.ai:spring-ai-starter-model-ollama")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation("org.springframework.boot:spring-boot-starter-session-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	runtimeOnly("org.flywaydb:flyway-database-postgresql")
	runtimeOnly("org.postgresql:postgresql")
	testRuntimeOnly("com.h2database:h2")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("com.tngtech.archunit:archunit-junit5:1.4.2")
	testImplementation("org.springframework.modulith:spring-modulith-core")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

val frontendDir = layout.projectDirectory.dir("frontend")
val frontendOutputDir = frontendDir.dir("out")

val frontendInstall = tasks.register<Exec>("frontendInstall") {
	workingDir(frontendDir.asFile)
	commandLine("npm", "ci")
	inputs.files(frontendDir.file("package.json"), frontendDir.file("package-lock.json"))
	outputs.file(frontendDir.file("node_modules/.package-lock.json"))
}

val frontendBuild = tasks.register<Exec>("frontendBuild") {
	dependsOn(frontendInstall)
	workingDir(frontendDir.asFile)
	commandLine("npm", "run", "build")
	inputs.file(frontendDir.file("next.config.ts"))
	inputs.dir(frontendDir.dir("src"))
	inputs.dir(frontendDir.dir("public"))
	outputs.dir(frontendOutputDir)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
	dependsOn(frontendBuild)
	from(frontendOutputDir) {
		into("BOOT-INF/classes/static")
	}
}
