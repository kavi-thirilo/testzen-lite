plugins {
    kotlin("jvm") version "1.9.22" apply false
    kotlin("plugin.serialization") version "1.9.22" apply false
}

group = "com.testzen"
version = "1.0.0"

allprojects {
    repositories {
        mavenCentral()
    }
}

tasks.register("clean") {
    group = "build"
    description = "Cleans all subprojects"
    dependsOn(subprojects.map { it.tasks.named("clean") })
}
