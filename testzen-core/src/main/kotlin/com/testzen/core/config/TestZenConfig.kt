package com.testzen.core.config

import kotlinx.serialization.Serializable
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileReader

/**
 * Configuration for TestZen execution.
 *
 * Can be loaded from JSON/YAML file or constructed programmatically.
 */
@Serializable
data class TestZenConfig(
    // Timeouts (in seconds)
    val implicitWait: Long = 20,
    val explicitWait: Long = 30,
    val pageLoadTimeout: Long = 120,
    val actionTimeout: Long = 60,

    // Retry configuration
    val retryFailedSteps: Int = 2,
    val retryDelayMs: Long = 1000,

    // Screenshot configuration
    val screenshotOnFailure: Boolean = true,
    val screenshotOnSuccess: Boolean = false,
    val screenshotDirectory: String = "./screenshots",

    // Element finding configuration
    val fuzzyMatchEnabled: Boolean = true,
    val fuzzyMatchThreshold: Double = 0.8,
    val scrollToFindElement: Boolean = true,
    val maxScrollAttempts: Int = 5,

    // Self-healing locator configuration
    val selfHealingEnabled: Boolean = true,
    val maxFallbackAttempts: Int = 5,
    val locatorCacheDirectory: String = ".testzen-cache",
    val learnFromHealing: Boolean = true,  // Extract and save locators from healed elements

    // Page Object Repository configuration (for large projects)
    val usePageObjectRepository: Boolean = false,
    val pageObjectsDirectory: String = "./page-objects",
    val cacheMode: String = "READ_WRITE",  // READ_WRITE, READ_ONLY, DISABLED
    val autoSavePageObjects: Boolean = false,

    // Smart element finding configuration
    val smartFindEnabled: Boolean = true,
    val smartFindMinimumScore: Double = 0.4,
    val elementTimeoutMs: Long = 10000,

    // Logging
    val logLevel: String = "INFO",
    val logToFile: Boolean = false,
    val logDirectory: String = "./logs",

    // Output
    val outputFormat: String = "json",
    val outputDirectory: String = "./results",

    // Appium/Selenium specific
    val appiumHost: String = "127.0.0.1",
    val appiumPort: Int = 4723,
    val browserType: String = "chrome",
    val headless: Boolean = false
) {
    companion object {
        /**
         * Load configuration from a file (YAML or JSON).
         */
        fun fromFile(path: String): TestZenConfig {
            val file = File(path)
            require(file.exists()) { "Config file not found: $path" }

            return when (file.extension.lowercase()) {
                "yaml", "yml" -> fromYaml(file)
                "json" -> fromJson(file)
                else -> throw IllegalArgumentException("Unsupported config format: ${file.extension}")
            }
        }

        /**
         * Load configuration from YAML file.
         */
        @Suppress("UNCHECKED_CAST")
        private fun fromYaml(file: File): TestZenConfig {
            val yaml = Yaml()
            val data = FileReader(file).use { yaml.load<Map<String, Any?>>(it) }
            return fromMap(data)
        }

        /**
         * Load configuration from JSON file.
         */
        private fun fromJson(file: File): TestZenConfig {
            val content = file.readText()
            return kotlinx.serialization.json.Json.decodeFromString(content)
        }

        /**
         * Create configuration from a map (for YAML parsing).
         */
        @Suppress("UNCHECKED_CAST")
        private fun fromMap(data: Map<String, Any?>): TestZenConfig {
            val execution = data["execution"] as? Map<String, Any?> ?: emptyMap()
            val output = data["output"] as? Map<String, Any?> ?: emptyMap()
            val appium = data["appium"] as? Map<String, Any?> ?: emptyMap()
            val elements = data["elements"] as? Map<String, Any?> ?: emptyMap()
            val logging = data["logging"] as? Map<String, Any?> ?: emptyMap()

            return TestZenConfig(
                // Timeouts
                implicitWait = (execution["implicit_wait"] as? Number)?.toLong() ?: 20,
                explicitWait = (execution["explicit_wait"] as? Number)?.toLong() ?: 30,
                pageLoadTimeout = (execution["page_load_timeout"] as? Number)?.toLong() ?: 120,
                actionTimeout = (execution["action_timeout"] as? Number)?.toLong() ?: 60,

                // Retry
                retryFailedSteps = (execution["retry_failed_steps"] as? Number)?.toInt() ?: 2,
                retryDelayMs = (execution["retry_delay_ms"] as? Number)?.toLong() ?: 1000,

                // Screenshots
                screenshotOnFailure = execution["screenshot_on_failure"] as? Boolean ?: true,
                screenshotOnSuccess = execution["screenshot_on_success"] as? Boolean ?: false,
                screenshotDirectory = execution["screenshot_directory"]?.toString() ?: "./screenshots",

                // Elements
                fuzzyMatchEnabled = elements["fuzzy_match_enabled"] as? Boolean ?: true,
                fuzzyMatchThreshold = (elements["fuzzy_match_threshold"] as? Number)?.toDouble() ?: 0.8,
                scrollToFindElement = elements["scroll_to_find"] as? Boolean ?: true,
                maxScrollAttempts = (elements["max_scroll_attempts"] as? Number)?.toInt() ?: 5,

                // Self-healing
                selfHealingEnabled = elements["self_healing_enabled"] as? Boolean ?: true,
                maxFallbackAttempts = (elements["max_fallback_attempts"] as? Number)?.toInt() ?: 5,
                locatorCacheDirectory = elements["locator_cache_directory"]?.toString() ?: ".testzen-cache",
                learnFromHealing = elements["learn_from_healing"] as? Boolean ?: true,

                // Page Object Repository
                usePageObjectRepository = elements["use_page_object_repository"] as? Boolean ?: false,
                pageObjectsDirectory = elements["page_objects_directory"]?.toString() ?: "./page-objects",
                cacheMode = elements["cache_mode"]?.toString() ?: "READ_WRITE",
                autoSavePageObjects = elements["auto_save_page_objects"] as? Boolean ?: false,

                // Smart element finding
                smartFindEnabled = elements["smart_find_enabled"] as? Boolean ?: true,
                smartFindMinimumScore = (elements["smart_find_minimum_score"] as? Number)?.toDouble() ?: 0.4,
                elementTimeoutMs = (elements["element_timeout_ms"] as? Number)?.toLong() ?: 10000,

                // Logging
                logLevel = logging["level"]?.toString() ?: "INFO",
                logToFile = logging["log_to_file"] as? Boolean ?: false,
                logDirectory = logging["directory"]?.toString() ?: "./logs",

                // Output
                outputFormat = output["format"]?.toString() ?: "json",
                outputDirectory = output["directory"]?.toString() ?: "./results",

                // Appium
                appiumHost = appium["host"]?.toString() ?: "127.0.0.1",
                appiumPort = (appium["port"] as? Number)?.toInt() ?: 4723,
                browserType = appium["browser"]?.toString() ?: "chrome",
                headless = appium["headless"] as? Boolean ?: false
            )
        }

        /**
         * Create default configuration.
         */
        fun default(): TestZenConfig = TestZenConfig()
    }
}
