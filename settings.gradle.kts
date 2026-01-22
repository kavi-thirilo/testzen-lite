rootProject.name = "testzen-lite"

include(
    ":testzen-core",
    ":testzen-cli"
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
