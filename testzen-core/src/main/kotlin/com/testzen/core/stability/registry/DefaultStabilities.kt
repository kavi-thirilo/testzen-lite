package com.testzen.core.stability.registry

import com.testzen.core.model.Platform
import com.testzen.core.stability.StabilityConfig

/**
 * Default stability configurations following module patterns.
 *
 * Provides pre-configured stability settings for common scenarios:
 * - Environment-based: FAST, DEFAULT, ROBUST, CI
 * - Platform-based: ANDROID, IOS, WEB
 * - Network-based: FAST_NETWORK, SLOW_NETWORK, OFFLINE_FIRST
 * - Feature-based: ELEMENT, SCROLL, PAGE_LOAD, ANIMATION
 *
 * Usage:
 * ```kotlin
 * // Register all defaults
 * DefaultStabilities.registerAll(registry)
 *
 * // Register only specific category
 * DefaultStabilities.registerCategory(registry, StabilityCategory.ELEMENT)
 *
 * // Get a specific preset configuration
 * val config = DefaultStabilities.robust()
 * ```
 */
object DefaultStabilities {

    /**
     * Register all default stability configurations.
     */
    fun registerAll(registry: StabilityRegistry) {
        registerEnvironmentConfigs(registry)
        registerPlatformConfigs(registry)
        registerNetworkConfigs(registry)
        registerFeatureConfigs(registry)
        registerPlatformAdapters(registry)
    }

    /**
     * Register configurations for a specific category.
     */
    fun registerCategory(registry: StabilityRegistry, category: StabilityCategory) {
        when (category) {
            StabilityCategory.ELEMENT -> registerElementConfigs(registry)
            StabilityCategory.SCROLL -> registerScrollConfigs(registry)
            StabilityCategory.PAGE_LOAD -> registerPageLoadConfigs(registry)
            StabilityCategory.RETRY -> registerRetryConfigs(registry)
            StabilityCategory.ANIMATION -> registerAnimationConfigs(registry)
            StabilityCategory.NETWORK -> registerNetworkConfigs(registry)
            StabilityCategory.CUSTOM -> { /* User-defined */ }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ENVIRONMENT CONFIGURATIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    private fun registerEnvironmentConfigs(registry: StabilityRegistry) {
        // Fast environment - minimal waits
        registry.register(StabilityCategory.CUSTOM, "FAST") {
            preset(StabilityPreset.FAST)
        }

        // Default balanced environment
        registry.register(StabilityCategory.CUSTOM, "DEFAULT") {
            preset(StabilityPreset.DEFAULT)
        }

        // Robust environment - extended waits
        registry.register(StabilityCategory.CUSTOM, "ROBUST") {
            preset(StabilityPreset.ROBUST)
        }

        // CI/CD environment
        registry.register(StabilityCategory.CUSTOM, "CI") {
            preset(StabilityPreset.CI)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PLATFORM CONFIGURATIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    private fun registerPlatformConfigs(registry: StabilityRegistry) {
        // Android-optimized settings
        registry.register(StabilityCategory.CUSTOM, "ANDROID") {
            elementStability {
                timeoutMs = 4000
                stableReadingsRequired = 3
            }
            scrolling {
                settleTimeMs = 400
                momentumSettleMs = 600
                detectMomentum = true
            }
            pageLoad {
                networkIdle = false  // Not available on native
                domStability = false // Use element-based detection
            }
            animation {
                timeoutMs = 2500
                defaultDurationMs = 350
            }
            platformMultiplier("ANDROID", 1.0)
        }

        // iOS-optimized settings
        registry.register(StabilityCategory.CUSTOM, "IOS") {
            elementStability {
                timeoutMs = 4500
                stableReadingsRequired = 3
            }
            scrolling {
                settleTimeMs = 450
                momentumSettleMs = 700  // iOS has smoother, longer momentum
                detectMomentum = true
            }
            pageLoad {
                networkIdle = false
                domStability = false
            }
            animation {
                timeoutMs = 3000
                defaultDurationMs = 400  // iOS animations typically longer
            }
            platformMultiplier("IOS", 1.2)
        }

        // Web-optimized settings
        registry.register(StabilityCategory.CUSTOM, "WEB") {
            elementStability {
                timeoutMs = 3000
                stableReadingsRequired = 3
            }
            scrolling {
                settleTimeMs = 250
                momentumSettleMs = 400
            }
            pageLoad {
                enabled = true
                networkIdle = true
                domStability = true
                lazyLoadDetection = true
            }
            animation {
                timeoutMs = 2000
                defaultDurationMs = 300
            }
            platformMultiplier("WEB", 0.8)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // NETWORK CONFIGURATIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    private fun registerNetworkConfigs(registry: StabilityRegistry) {
        // Fast network (5G, WiFi)
        registry.register(StabilityCategory.NETWORK, "FAST_NETWORK") {
            network(NP.FAST)
            retry {
                maxRetries = 2
                initialDelayMs = 50
            }
            pageLoad {
                timeoutMs = 15000
                networkIdleThresholdMs = 300
            }
        }

        // Normal network (4G, broadband)
        registry.register(StabilityCategory.NETWORK, "NORMAL_NETWORK") {
            network(NP.NORMAL)
            retry {
                maxRetries = 3
                initialDelayMs = 100
            }
            pageLoad {
                timeoutMs = 30000
                networkIdleThresholdMs = 500
            }
        }

        // Slow network (3G, congested)
        registry.register(StabilityCategory.NETWORK, "SLOW_NETWORK") {
            network(NP.SLOW)
            retry {
                maxRetries = 4
                initialDelayMs = 200
            }
            pageLoad {
                timeoutMs = 45000
                networkIdleThresholdMs = 800
            }
            elementStability {
                timeoutMs = 5000
            }
        }

        // Very slow network (2G, high latency)
        registry.register(StabilityCategory.NETWORK, "VERY_SLOW_NETWORK") {
            network(NP.VERY_SLOW)
            retry {
                maxRetries = 5
                initialDelayMs = 500
            }
            pageLoad {
                timeoutMs = 60000
                networkIdleThresholdMs = 1500
            }
            elementStability {
                timeoutMs = 8000
            }
        }

        // Offline-first apps
        registry.register(StabilityCategory.NETWORK, "OFFLINE_FIRST") {
            network(NP.OFFLINE_FIRST)
            pageLoad {
                networkIdle = false
                timeoutMs = 10000
            }
            retry {
                maxRetries = 2
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // FEATURE-SPECIFIC CONFIGURATIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    private fun registerFeatureConfigs(registry: StabilityRegistry) {
        registerElementConfigs(registry)
        registerScrollConfigs(registry)
        registerPageLoadConfigs(registry)
        registerRetryConfigs(registry)
        registerAnimationConfigs(registry)
    }

    private fun registerElementConfigs(registry: StabilityRegistry) {
        // Strict element stability
        registry.register(StabilityCategory.ELEMENT, "STRICT") {
            elementStability {
                enabled = true
                timeoutMs = 5000
                stableReadingsRequired = 4
                positionTolerancePx = 1
                sizeTolerancePx = 1
            }
        }

        // Relaxed element stability
        registry.register(StabilityCategory.ELEMENT, "RELAXED") {
            elementStability {
                enabled = true
                timeoutMs = 2000
                stableReadingsRequired = 2
                positionTolerancePx = 5
                sizeTolerancePx = 5
            }
        }

        // Disabled element stability (for speed)
        registry.register(StabilityCategory.ELEMENT, "DISABLED") {
            elementStability {
                enabled = false
            }
        }
    }

    private fun registerScrollConfigs(registry: StabilityRegistry) {
        // Aggressive scrolling
        registry.register(StabilityCategory.SCROLL, "AGGRESSIVE") {
            scrolling {
                enabled = true
                maxAttempts = 20
                settleTimeMs = 200
                viewportPercentage = 0.8
            }
        }

        // Conservative scrolling
        registry.register(StabilityCategory.SCROLL, "CONSERVATIVE") {
            scrolling {
                enabled = true
                maxAttempts = 5
                settleTimeMs = 500
                viewportPercentage = 0.4
            }
        }

        // Disabled scrolling
        registry.register(StabilityCategory.SCROLL, "DISABLED") {
            scrolling {
                enabled = false
            }
        }
    }

    private fun registerPageLoadConfigs(registry: StabilityRegistry) {
        // Full page load intelligence
        registry.register(StabilityCategory.PAGE_LOAD, "FULL") {
            pageLoad {
                enabled = true
                networkIdle = true
                domStability = true
                lazyLoadDetection = true
                timeoutMs = 45000
            }
        }

        // Minimal page load checks
        registry.register(StabilityCategory.PAGE_LOAD, "MINIMAL") {
            pageLoad {
                enabled = true
                networkIdle = false
                domStability = false
                lazyLoadDetection = false
                timeoutMs = 15000
            }
        }

        // Disabled page load intelligence
        registry.register(StabilityCategory.PAGE_LOAD, "DISABLED") {
            pageLoad {
                enabled = false
            }
        }
    }

    private fun registerRetryConfigs(registry: StabilityRegistry) {
        // Aggressive retry
        registry.register(StabilityCategory.RETRY, "AGGRESSIVE") {
            retry {
                exponentialBackoff = true
                maxRetries = 5
                initialDelayMs = 50
                multiplier = 1.5
            }
            staleElementRecovery {
                maxRetries = 5
            }
        }

        // Conservative retry
        registry.register(StabilityCategory.RETRY, "CONSERVATIVE") {
            retry {
                exponentialBackoff = true
                maxRetries = 2
                initialDelayMs = 200
                multiplier = 2.0
            }
            staleElementRecovery {
                maxRetries = 2
            }
        }

        // No retry
        registry.register(StabilityCategory.RETRY, "DISABLED") {
            retry {
                maxRetries = 0
            }
            staleElementRecovery {
                enabled = false
            }
        }
    }

    private fun registerAnimationConfigs(registry: StabilityRegistry) {
        // Full animation waiting
        registry.register(StabilityCategory.ANIMATION, "FULL") {
            animation {
                enabled = true
                timeoutMs = 3000
                defaultDurationMs = 500
            }
        }

        // Minimal animation waiting
        registry.register(StabilityCategory.ANIMATION, "MINIMAL") {
            animation {
                enabled = true
                timeoutMs = 1000
                defaultDurationMs = 200
            }
        }

        // Disabled animation waiting
        registry.register(StabilityCategory.ANIMATION, "DISABLED") {
            animation {
                enabled = false
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PLATFORM ADAPTERS
    // ═══════════════════════════════════════════════════════════════════════════════

    private fun registerPlatformAdapters(registry: StabilityRegistry) {
        registry.registerAdapter(Platform.WEB, WebStabilityAdapter())
        registry.registerAdapter(Platform.ANDROID, MobileStabilityAdapter(Platform.ANDROID))
        registry.registerAdapter(Platform.IOS, MobileStabilityAdapter(Platform.IOS))
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CONVENIENCE FACTORY METHODS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Get fast environment configuration.
     */
    fun fast(): StabilityConfig = stabilityConfig(StabilityPreset.FAST)

    /**
     * Get default configuration.
     */
    fun default(): StabilityConfig = stabilityConfig(StabilityPreset.DEFAULT)

    /**
     * Get robust environment configuration.
     */
    fun robust(): StabilityConfig = stabilityConfig(StabilityPreset.ROBUST)

    /**
     * Get CI environment configuration.
     */
    fun ci(): StabilityConfig = stabilityConfig(StabilityPreset.CI)

    /**
     * Get Android-optimized configuration.
     */
    fun android(): StabilityConfig = stabilityConfig {
        elementStability {
            timeoutMs = 4000
            stableReadingsRequired = 3
        }
        scrolling {
            settleTimeMs = 400
            momentumSettleMs = 600
        }
        pageLoad {
            networkIdle = false
            domStability = false
        }
        platformMultiplier("ANDROID", 1.0)
    }

    /**
     * Get iOS-optimized configuration.
     */
    fun ios(): StabilityConfig = stabilityConfig {
        elementStability {
            timeoutMs = 4500
            stableReadingsRequired = 3
        }
        scrolling {
            settleTimeMs = 450
            momentumSettleMs = 700
        }
        pageLoad {
            networkIdle = false
            domStability = false
        }
        platformMultiplier("IOS", 1.2)
    }

    /**
     * Get web-optimized configuration.
     */
    fun web(): StabilityConfig = stabilityConfig {
        pageLoad {
            enabled = true
            networkIdle = true
            domStability = true
        }
        platformMultiplier("WEB", 0.8)
    }

    /**
     * Get configuration for a specific platform.
     */
    fun forPlatform(platform: Platform): StabilityConfig = when (platform) {
        Platform.ANDROID -> android()
        Platform.IOS -> ios()
        Platform.WEB -> web()
    }
}
