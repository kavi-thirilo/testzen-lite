package com.testzen.core.stability.registry

import com.testzen.core.model.Platform
import com.testzen.core.stability.*
import org.slf4j.LoggerFactory

/**
 * Centralized registry for all stability strategies and configurations.
 *
 * The StabilityRegistry provides:
 * - Single source of truth for all stability configurations
 * - Platform-specific stability adapters
 * - Organized storage by category
 * - Easy registration and retrieval
 * - Support for custom stability strategies
 *
 * Stability Categories:
 * - ELEMENT: Element stability waiting strategies
 * - SCROLL: Smart scroll strategies
 * - PAGE_LOAD: Page load intelligence strategies
 * - RETRY: Retry and backoff strategies
 * - ANIMATION: Animation detection strategies
 *
 * Usage:
 * ```kotlin
 * // Get default registry with all built-in strategies
 * val registry = StabilityRegistry.default()
 *
 * // Get orchestrator for a platform
 * val orchestrator = registry.getOrchestrator(Platform.ANDROID)
 *
 * // Register custom strategy
 * registry.register(StabilityCategory.ELEMENT, "custom") {
 *     elementStability {
 *         timeoutMs = 5000
 *         stableReadingsRequired = 4
 *     }
 * }
 * ```
 */
class StabilityRegistry private constructor() {
    private val logger = LoggerFactory.getLogger(StabilityRegistry::class.java)

    // Configurations organized by category
    private val configsByCategory = mutableMapOf<StabilityCategory, MutableMap<String, StabilityConfig>>()

    // Platform-specific adapters
    private val platformAdapters = mutableMapOf<Platform, PlatformStabilityAdapter>()

    // Default adapter for unknown platforms
    private var defaultAdapter: PlatformStabilityAdapter = WebStabilityAdapter()

    // Orchestrators cache by platform
    private val orchestratorCache = mutableMapOf<Platform, StabilityOrchestrator>()

    // Named configurations
    private val namedConfigs = mutableMapOf<String, StabilityConfig>()

    /**
     * Register a stability configuration under a category.
     */
    fun register(category: StabilityCategory, name: String, config: StabilityConfig) {
        configsByCategory.getOrPut(category) { mutableMapOf() }[name] = config
        namedConfigs["$category:$name"] = config
        logger.debug("Registered stability config '$name' in category $category")
    }

    /**
     * Register a configuration using the DSL.
     */
    fun register(category: StabilityCategory, name: String, block: StabilityConfigBuilder.() -> Unit) {
        val builder = StabilityConfigBuilder()
        builder.block()
        register(category, name, builder.build())
    }

    /**
     * Register multiple configurations in a category.
     */
    fun register(category: StabilityCategory, block: StabilityCategoryBuilder.() -> Unit) {
        val builder = StabilityCategoryBuilder(this, category)
        builder.block()
    }

    /**
     * Register a platform-specific adapter.
     */
    fun registerAdapter(platform: Platform, adapter: PlatformStabilityAdapter) {
        platformAdapters[platform] = adapter
        orchestratorCache.remove(platform) // Clear cached orchestrator
        logger.debug("Registered stability adapter for platform $platform: ${adapter.javaClass.simpleName}")
    }

    /**
     * Set the default adapter for unknown platforms.
     */
    fun setDefaultAdapter(adapter: PlatformStabilityAdapter) {
        defaultAdapter = adapter
        logger.debug("Set default stability adapter: ${adapter.javaClass.simpleName}")
    }

    /**
     * Get configuration by category and name.
     */
    fun getConfig(category: StabilityCategory, name: String): StabilityConfig? {
        return configsByCategory[category]?.get(name)
    }

    /**
     * Get configuration by qualified name (category:name).
     */
    fun getConfig(qualifiedName: String): StabilityConfig? {
        return namedConfigs[qualifiedName]
    }

    /**
     * Get all configurations in a category.
     */
    fun getByCategory(category: StabilityCategory): Map<String, StabilityConfig> {
        return configsByCategory[category]?.toMap() ?: emptyMap()
    }

    /**
     * Get platform-specific adapter.
     */
    fun getAdapter(platform: Platform): PlatformStabilityAdapter {
        return platformAdapters[platform] ?: defaultAdapter
    }

    /**
     * Get StabilityOrchestrator for a platform with appropriate adapter.
     */
    fun getOrchestrator(
        platform: Platform,
        config: StabilityConfig = StabilityConfig.default()
    ): StabilityOrchestrator {
        return orchestratorCache.getOrPut(platform) {
            val adapter = getAdapter(platform)
            StabilityOrchestrator(config, platform, adapter)
        }
    }

    /**
     * Get StabilityOrchestrator for a named configuration.
     */
    fun getOrchestrator(
        platform: Platform,
        configName: String
    ): StabilityOrchestrator {
        val config = namedConfigs[configName] ?: StabilityConfig.default()
        return getOrchestrator(platform, config)
    }

    /**
     * Get statistics about registered configurations.
     */
    fun getStats(): StabilityStats {
        return StabilityStats(
            totalConfigs = namedConfigs.size,
            byCategory = configsByCategory.mapValues { it.value.size },
            categories = configsByCategory.keys.toSet(),
            registeredPlatforms = platformAdapters.keys.toSet()
        )
    }

    /**
     * Clear all configurations (useful for testing).
     */
    fun clear() {
        configsByCategory.clear()
        namedConfigs.clear()
        orchestratorCache.clear()
        // Keep platform adapters
    }

    companion object {
        /**
         * Create a new empty registry.
         */
        fun empty(): StabilityRegistry = StabilityRegistry()

        /**
         * Create a registry with all default built-in configurations.
         */
        fun default(): StabilityRegistry {
            val registry = StabilityRegistry()
            DefaultStabilities.registerAll(registry)
            return registry
        }

        /**
         * Create a registry with only specific categories.
         */
        fun withCategories(vararg categories: StabilityCategory): StabilityRegistry {
            val registry = StabilityRegistry()
            categories.forEach { category ->
                DefaultStabilities.registerCategory(registry, category)
            }
            return registry
        }

        /**
         * Create a registry for a specific platform.
         */
        fun forPlatform(platform: Platform): StabilityRegistry {
            val registry = default()
            // Ensure the platform adapter is set
            when (platform) {
                Platform.ANDROID -> registry.registerAdapter(platform, MobileStabilityAdapter(platform))
                Platform.IOS -> registry.registerAdapter(platform, MobileStabilityAdapter(platform))
                Platform.WEB -> registry.registerAdapter(platform, WebStabilityAdapter())
            }
            return registry
        }
    }
}

/**
 * Stability categories for organization.
 */
enum class StabilityCategory {
    /** Element stability waiting strategies */
    ELEMENT,

    /** Smart scroll strategies */
    SCROLL,

    /** Page load intelligence strategies */
    PAGE_LOAD,

    /** Retry and backoff strategies */
    RETRY,

    /** Animation detection strategies */
    ANIMATION,

    /** Network profile configurations */
    NETWORK,

    /** Custom stability strategies */
    CUSTOM
}

/**
 * Statistics about registered stability configurations.
 */
data class StabilityStats(
    val totalConfigs: Int,
    val byCategory: Map<StabilityCategory, Int>,
    val categories: Set<StabilityCategory>,
    val registeredPlatforms: Set<Platform>
) {
    override fun toString(): String = buildString {
        appendLine("StabilityRegistry Stats:")
        appendLine("  Total configurations: $totalConfigs")
        appendLine("  Categories: ${categories.size}")
        byCategory.forEach { (category, count) ->
            appendLine("    $category: $count configs")
        }
        appendLine("  Registered platforms: $registeredPlatforms")
    }
}
