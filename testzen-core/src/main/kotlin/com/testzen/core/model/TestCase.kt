package com.testzen.core.model

import kotlinx.serialization.Serializable

/**
 * Represents a test case loaded from YAML.
 */
@Serializable
data class TestCase(
    val testId: String,
    val name: String,
    val description: String? = null,
    val platform: Platform? = null,
    val appName: String? = null,
    val packageName: String? = null,
    val bundleId: String? = null,
    val baseUrl: String? = null,
    val steps: List<TestStep>,
    val tags: List<String> = emptyList(),
    val module: String? = null
)

/**
 * Represents a single test step.
 */
@Serializable
data class TestStep(
    val order: Int,
    val instruction: String,
    val description: String? = null,
    val screenshot: Boolean = false,
    val optional: Boolean = false,
    val timeout: Long? = null
)

/**
 * Target platform for test execution.
 */
@Serializable
enum class Platform {
    ANDROID,
    IOS,
    WEB;

    companion object {
        fun fromString(value: String): Platform {
            return when (value.lowercase()) {
                "android" -> ANDROID
                "ios" -> IOS
                "web" -> WEB
                else -> throw IllegalArgumentException("Unknown platform: $value")
            }
        }
    }
}
