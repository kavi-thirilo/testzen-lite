plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    `maven-publish`
    `java-library`
    signing
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.testzen"
version = "1.0.0"
description = "TestZen Core - Lightweight no-code test automation framework"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withJavadocJar()
    withSourcesJar()
}

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
}

dependencies {
    // Core automation dependencies
    api("io.appium:java-client:9.0.0")
    api("org.seleniumhq.selenium:selenium-java:4.15.0")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // YAML parsing for test definitions
    implementation("org.yaml:snakeyaml:2.2")
    implementation("com.charleskorn.kaml:kaml:0.57.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.11")
    runtimeOnly("ch.qos.logback:logback-classic:1.4.14")

    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.22")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.9")
}

// ═══════════════════════════════════════════════════════════════
// MAVEN PUBLISHING CONFIGURATION
// ═══════════════════════════════════════════════════════════════

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = "testzen-core"
            version = project.version.toString()

            pom {
                name.set("TestZen Core")
                description.set("Lightweight no-code test automation framework for mobile and web applications")
                url.set("https://github.com/your-org/testzen-lite")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("testzen-team")
                        name.set("TestZen Team")
                        email.set("team@testzen.dev")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/your-org/testzen-lite.git")
                    developerConnection.set("scm:git:ssh://github.com/your-org/testzen-lite.git")
                    url.set("https://github.com/your-org/testzen-lite")
                }
            }
        }
    }

    repositories {
        // Local repository for testing
        maven {
            name = "local"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}

// Signing for Maven Central (optional - requires GPG key)
signing {
    isRequired = false
    sign(publishing.publications["mavenJava"])
}

// ═══════════════════════════════════════════════════════════════
// SHADOW JAR (Fat JAR for convenience distribution)
// ═══════════════════════════════════════════════════════════════

tasks.shadowJar {
    archiveClassifier.set("all")
    mergeServiceFiles()

    // Relocate dependencies to avoid conflicts
    relocate("org.yaml.snakeyaml", "com.testzen.shaded.snakeyaml")
    relocate("com.charleskorn.kaml", "com.testzen.shaded.kaml")
}

// ═══════════════════════════════════════════════════════════════
// BUILD CONFIGURATION
// ═══════════════════════════════════════════════════════════════

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "TestZen",
            "Automatic-Module-Name" to "com.testzen.core"
        )
    }
}
