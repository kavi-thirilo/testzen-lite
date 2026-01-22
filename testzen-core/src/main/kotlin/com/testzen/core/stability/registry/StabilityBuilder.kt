package com.testzen.core.stability.registry

import com.testzen.core.stability.StabilityConfig

/**
 * DSL builder for creating stability configurations in a clean, readable way.
 *
 * Usage:
 * ```kotlin
 * val config = stabilityConfig {
 *     elementStability {
 *         enabled = true
 *         timeoutMs = 5000
 *         stableReadingsRequired = 3
 *         positionTolerancePx = 2
 *     }
 *     scrolling {
 *         enabled = true
 *         maxAttempts = 10
 *         settleTimeMs = 300
 *     }
 *     pageLoad {
 *         enabled = true
 *         networkIdle = true
 *         domStability = true
 *     }
 *     retry {
 *         exponentialBackoff = true
 *         maxRetries = 3
 *         initialDelayMs = 100
 *     }
 *     network(NetworkProfile.SLOW)
 * }
 * ```
 *
 * Or for environment-specific configs:
 * ```kotlin
 * val fastConfig = stabilityConfig(StabilityPreset.FAST)
 * val ciConfig = stabilityConfig(StabilityPreset.CI)
 * val robustConfig = stabilityConfig(StabilityPreset.ROBUST)
 * ```
 */
@DslMarker
annotation class StabilityDsl

/**
 * Preset configurations for common scenarios.
 */
enum class StabilityPreset {
    /** Fast, minimal waits for stable environments */
    FAST,

    /** Default balanced configuration */
    DEFAULT,

    /** Extended waits for slow/unstable environments */
    ROBUST,

    /** Optimized for CI/CD pipelines */
    CI
}

/**
 * Builder for StabilityConfig with DSL support.
 */
@StabilityDsl
class StabilityConfigBuilder {
    // Element stability settings
    private var elementStabilityEnabled = true
    private var stabilityTimeoutMs = 3000L
    private var stabilityCheckIntervalMs = 100L
    private var stableReadingsRequired = 3
    private var positionTolerancePx = 2
    private var sizeTolerancePx = 2

    // Animation settings
    private var waitForAnimationsEnabled = true
    private var animationTimeoutMs = 2000L
    private var defaultAnimationDurationMs = 300L

    // Scroll settings
    private var smartScrollEnabled = true
    private var maxScrollAttempts = 10
    private var scrollSettleTimeMs = 300L
    private var scrollDynamicLoadTimeoutMs = 2000L
    private var scrollViewportPercentage = 0.6
    private var endOfContentSnapshots = 2
    private var detectScrollMomentum = true
    private var scrollMomentumSettleMs = 500L

    // Page load settings
    private var pageLoadIntelligenceEnabled = true
    private var waitForNetworkIdle = true
    private var networkIdleThresholdMs = 500L
    private var pageLoadTimeoutMs = 30000L
    private var waitForDomStable = true
    private var domStabilityThresholdMs = 300L
    private var detectLazyLoading = true

    // Retry settings
    private var exponentialBackoffEnabled = true
    private var initialRetryDelayMs = 100L
    private var maxRetryDelayMs = 5000L
    private var backoffMultiplier = 2.0
    private var retryJitter = 0.1
    private var maxTransientRetries = 3

    // Stale element settings
    private var staleElementRecoveryEnabled = true
    private var staleElementMaxRetries = 3
    private var staleElementRefindDelayMs = 200L

    // Action timeouts
    private var actionTimeouts = mutableMapOf(
        "CLICK" to 5000L,
        "ENTER_TEXT" to 10000L,
        "CLEAR_TEXT" to 5000L,
        "SCROLL" to 3000L,
        "SWIPE" to 3000L,
        "VERIFY" to 15000L,
        "WAIT" to 30000L,
        "LAUNCH_APP" to 30000L,
        "DEFAULT" to 10000L
    )

    // Platform multipliers
    private var platformMultipliers = mutableMapOf(
        "ANDROID" to 1.0,
        "IOS" to 1.2,
        "WEB" to 0.8
    )

    // Network profile
    private var networkProfile = StabilityConfig.NetworkProfile.NORMAL
    private var adaptiveTimeoutsEnabled = true
    private var adaptiveMinSamples = 10

    /**
     * Configure element stability settings.
     */
    fun elementStability(block: ElementStabilityBuilder.() -> Unit) {
        val builder = ElementStabilityBuilder()
        builder.block()
        elementStabilityEnabled = builder.enabled
        stabilityTimeoutMs = builder.timeoutMs
        stabilityCheckIntervalMs = builder.checkIntervalMs
        stableReadingsRequired = builder.stableReadingsRequired
        positionTolerancePx = builder.positionTolerancePx
        sizeTolerancePx = builder.sizeTolerancePx
    }

    /**
     * Configure animation detection settings.
     */
    fun animation(block: AnimationBuilder.() -> Unit) {
        val builder = AnimationBuilder()
        builder.block()
        waitForAnimationsEnabled = builder.enabled
        animationTimeoutMs = builder.timeoutMs
        defaultAnimationDurationMs = builder.defaultDurationMs
    }

    /**
     * Configure scrolling settings.
     */
    fun scrolling(block: ScrollingBuilder.() -> Unit) {
        val builder = ScrollingBuilder()
        builder.block()
        smartScrollEnabled = builder.enabled
        maxScrollAttempts = builder.maxAttempts
        scrollSettleTimeMs = builder.settleTimeMs
        scrollDynamicLoadTimeoutMs = builder.dynamicLoadTimeoutMs
        scrollViewportPercentage = builder.viewportPercentage
        endOfContentSnapshots = builder.endOfContentSnapshots
        detectScrollMomentum = builder.detectMomentum
        scrollMomentumSettleMs = builder.momentumSettleMs
    }

    /**
     * Configure page load intelligence settings.
     */
    fun pageLoad(block: PageLoadBuilder.() -> Unit) {
        val builder = PageLoadBuilder()
        builder.block()
        pageLoadIntelligenceEnabled = builder.enabled
        waitForNetworkIdle = builder.networkIdle
        networkIdleThresholdMs = builder.networkIdleThresholdMs
        pageLoadTimeoutMs = builder.timeoutMs
        waitForDomStable = builder.domStability
        domStabilityThresholdMs = builder.domStabilityThresholdMs
        detectLazyLoading = builder.lazyLoadDetection
    }

    /**
     * Configure retry settings.
     */
    fun retry(block: RetryBuilder.() -> Unit) {
        val builder = RetryBuilder()
        builder.block()
        exponentialBackoffEnabled = builder.exponentialBackoff
        initialRetryDelayMs = builder.initialDelayMs
        maxRetryDelayMs = builder.maxDelayMs
        backoffMultiplier = builder.multiplier
        retryJitter = builder.jitter
        maxTransientRetries = builder.maxRetries
    }

    /**
     * Configure stale element recovery.
     */
    fun staleElementRecovery(block: StaleElementBuilder.() -> Unit) {
        val builder = StaleElementBuilder()
        builder.block()
        staleElementRecoveryEnabled = builder.enabled
        staleElementMaxRetries = builder.maxRetries
        staleElementRefindDelayMs = builder.refindDelayMs
    }

    /**
     * Set action timeout.
     */
    fun actionTimeout(action: String, timeoutMs: Long) {
        actionTimeouts[action] = timeoutMs
    }

    /**
     * Set platform multiplier.
     */
    fun platformMultiplier(platform: String, multiplier: Double) {
        platformMultipliers[platform] = multiplier
    }

    /**
     * Set network profile.
     */
    fun network(profile: StabilityConfig.NetworkProfile) {
        networkProfile = profile
    }

    /**
     * Configure adaptive timeouts.
     */
    fun adaptiveTimeouts(enabled: Boolean, minSamples: Int = 10) {
        adaptiveTimeoutsEnabled = enabled
        adaptiveMinSamples = minSamples
    }

    /**
     * Apply a preset configuration.
     */
    fun preset(preset: StabilityPreset) {
        when (preset) {
            StabilityPreset.FAST -> applyFast()
            StabilityPreset.DEFAULT -> { /* Already default */ }
            StabilityPreset.ROBUST -> applyRobust()
            StabilityPreset.CI -> applyCi()
        }
    }

    private fun applyFast() {
        stabilityTimeoutMs = 1500
        stableReadingsRequired = 2
        scrollSettleTimeMs = 150
        networkProfile = StabilityConfig.NetworkProfile.FAST
        maxTransientRetries = 2
    }

    private fun applyRobust() {
        stabilityTimeoutMs = 5000
        stableReadingsRequired = 4
        scrollSettleTimeMs = 500
        scrollDynamicLoadTimeoutMs = 3000
        networkProfile = StabilityConfig.NetworkProfile.SLOW
        maxTransientRetries = 5
        pageLoadTimeoutMs = 60000
    }

    private fun applyCi() {
        stabilityTimeoutMs = 4000
        stableReadingsRequired = 3
        scrollSettleTimeMs = 400
        networkProfile = StabilityConfig.NetworkProfile.NORMAL
        maxTransientRetries = 4
        adaptiveTimeoutsEnabled = false
    }

    /**
     * Build the StabilityConfig.
     */
    fun build(): StabilityConfig {
        return StabilityConfig(
            elementStabilityEnabled = elementStabilityEnabled,
            stabilityTimeoutMs = stabilityTimeoutMs,
            stabilityCheckIntervalMs = stabilityCheckIntervalMs,
            stableReadingsRequired = stableReadingsRequired,
            positionTolerancePx = positionTolerancePx,
            sizeTolerancePx = sizeTolerancePx,
            waitForAnimationsEnabled = waitForAnimationsEnabled,
            animationTimeoutMs = animationTimeoutMs,
            defaultAnimationDurationMs = defaultAnimationDurationMs,
            smartScrollEnabled = smartScrollEnabled,
            maxScrollAttempts = maxScrollAttempts,
            scrollSettleTimeMs = scrollSettleTimeMs,
            scrollDynamicLoadTimeoutMs = scrollDynamicLoadTimeoutMs,
            scrollViewportPercentage = scrollViewportPercentage,
            endOfContentSnapshots = endOfContentSnapshots,
            detectScrollMomentum = detectScrollMomentum,
            scrollMomentumSettleMs = scrollMomentumSettleMs,
            pageLoadIntelligenceEnabled = pageLoadIntelligenceEnabled,
            waitForNetworkIdle = waitForNetworkIdle,
            networkIdleThresholdMs = networkIdleThresholdMs,
            pageLoadTimeoutMs = pageLoadTimeoutMs,
            waitForDomStable = waitForDomStable,
            domStabilityThresholdMs = domStabilityThresholdMs,
            detectLazyLoading = detectLazyLoading,
            exponentialBackoffEnabled = exponentialBackoffEnabled,
            initialRetryDelayMs = initialRetryDelayMs,
            maxRetryDelayMs = maxRetryDelayMs,
            backoffMultiplier = backoffMultiplier,
            retryJitter = retryJitter,
            maxTransientRetries = maxTransientRetries,
            staleElementRecoveryEnabled = staleElementRecoveryEnabled,
            staleElementMaxRetries = staleElementMaxRetries,
            staleElementRefindDelayMs = staleElementRefindDelayMs,
            actionTimeouts = actionTimeouts.toMap(),
            platformMultipliers = platformMultipliers.toMap(),
            networkProfile = networkProfile,
            adaptiveTimeoutsEnabled = adaptiveTimeoutsEnabled,
            adaptiveMinSamples = adaptiveMinSamples
        )
    }
}

/**
 * Builder for element stability settings.
 */
@StabilityDsl
class ElementStabilityBuilder {
    var enabled: Boolean = true
    var timeoutMs: Long = 3000
    var checkIntervalMs: Long = 100
    var stableReadingsRequired: Int = 3
    var positionTolerancePx: Int = 2
    var sizeTolerancePx: Int = 2
}

/**
 * Builder for animation settings.
 */
@StabilityDsl
class AnimationBuilder {
    var enabled: Boolean = true
    var timeoutMs: Long = 2000
    var defaultDurationMs: Long = 300
}

/**
 * Builder for scrolling settings.
 */
@StabilityDsl
class ScrollingBuilder {
    var enabled: Boolean = true
    var maxAttempts: Int = 10
    var settleTimeMs: Long = 300
    var dynamicLoadTimeoutMs: Long = 2000
    var viewportPercentage: Double = 0.6
    var endOfContentSnapshots: Int = 2
    var detectMomentum: Boolean = true
    var momentumSettleMs: Long = 500
}

/**
 * Builder for page load settings.
 */
@StabilityDsl
class PageLoadBuilder {
    var enabled: Boolean = true
    var networkIdle: Boolean = true
    var networkIdleThresholdMs: Long = 500
    var timeoutMs: Long = 30000
    var domStability: Boolean = true
    var domStabilityThresholdMs: Long = 300
    var lazyLoadDetection: Boolean = true
}

/**
 * Builder for retry settings.
 */
@StabilityDsl
class RetryBuilder {
    var exponentialBackoff: Boolean = true
    var initialDelayMs: Long = 100
    var maxDelayMs: Long = 5000
    var multiplier: Double = 2.0
    var jitter: Double = 0.1
    var maxRetries: Int = 3
}

/**
 * Builder for stale element recovery.
 */
@StabilityDsl
class StaleElementBuilder {
    var enabled: Boolean = true
    var maxRetries: Int = 3
    var refindDelayMs: Long = 200
}

/**
 * Builder for registering multiple configs in a category.
 */
@StabilityDsl
class StabilityCategoryBuilder(
    private val registry: StabilityRegistry,
    private val category: StabilityCategory
) {
    /**
     * Register a named configuration.
     */
    fun config(name: String, block: StabilityConfigBuilder.() -> Unit) {
        val builder = StabilityConfigBuilder()
        builder.block()
        registry.register(category, name, builder.build())
    }

    /**
     * Register a preset configuration.
     */
    fun preset(name: String, preset: StabilityPreset) {
        val builder = StabilityConfigBuilder()
        builder.preset(preset)
        registry.register(category, name, builder.build())
    }
}

/**
 * DSL entry point for creating a stability configuration.
 */
fun stabilityConfig(block: StabilityConfigBuilder.() -> Unit): StabilityConfig {
    val builder = StabilityConfigBuilder()
    builder.block()
    return builder.build()
}

/**
 * DSL entry point for creating a preset stability configuration.
 */
fun stabilityConfig(preset: StabilityPreset): StabilityConfig {
    val builder = StabilityConfigBuilder()
    builder.preset(preset)
    return builder.build()
}

/**
 * Type alias for concise DSL usage.
 */
typealias S = StabilityConfigBuilder
typealias SP = StabilityPreset
typealias NP = StabilityConfig.NetworkProfile
