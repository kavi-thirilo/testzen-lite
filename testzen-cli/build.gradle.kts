plugins {
    kotlin("jvm") version "1.9.22"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.testzen"
version = "1.0.0"
description = "TestZen CLI - Command-line interface for the no-code test automation framework"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
}

dependencies {
    // Core library
    implementation(project(":testzen-core"))

    // CLI argument parsing
    implementation("com.github.ajalt.clikt:clikt:4.2.2")

    // JSON output formatting
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.11")
    runtimeOnly("ch.qos.logback:logback-classic:1.4.14")

    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.22")
}

application {
    mainClass.set("com.testzen.cli.MainKt")
}

tasks.shadowJar {
    archiveBaseName.set("testzen-cli")
    archiveClassifier.set("all")
    archiveVersion.set(project.version.toString())

    manifest {
        attributes(
            "Main-Class" to "com.testzen.cli.MainKt",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }

    // Merge service files for proper SPI support
    mergeServiceFiles()
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.testzen.cli.MainKt",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
}

// Make 'run' task use command line arguments
tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}
