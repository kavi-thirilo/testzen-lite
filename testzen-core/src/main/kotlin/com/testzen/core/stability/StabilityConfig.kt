package com.testzen.core.stability

import com.testzen.core.model.Platform

/**
 * Configuration for intelligent stability and latency handling.
 *
 * Provides fine-grained control over waiting, scrolling, and retry strategies
 * to handle real-world scenarios like network latency, slow rendering, and animations.
 */
data class StabilityConfig(
    // ═══════════════════════════════════════════════════════════════════════════════
    // ELEMENT STABILITY SETTINGS
    // ═══════════════════════════════════════════════════════════════════════════════

    /** Enable element position/size stability verification before interaction */
    val elementStabilityEnabled: Boolean = true,

    /** Maximum time to wait for element position to stabilize (ms) */
    val stabilityTimeoutMs: Long = 3000,

    /** Interval between stability checks (ms) */
    val stabilityCheckIntervalMs: Long = 100,

    /** Number of consecutive stable readings required to consider element stable */
    val stableReadingsRequired: Int = 3,

    /** Position tolerance in pixels (element considered stable if movement < this) */
    val positionTolerancePx: Int = 2,

    /** Size tolerance in pixels (element considered stable if size change < this) */
    val sizeTolerancePx: Int = 2,

    // ═══════════════════════════════════════════════════════════════════════════════
    // ANIMATION DETECTION SETTINGS
    // ═══════════════════════════════════════════════════════════════════════════════

    /** Enable waiting for CSS/native animations to complete */
    val waitForAnimationsEnabled: Boolean = true,

    /** Maximum time to wait for animations (ms) */
    val animationTimeoutMs: Long = 2000,

    /** Default animation duration assumption when not detectable (ms) */
    val defaultAnimationDurationMs: Long = 300,

    // ═══════════════════════════════════════════════════════════════════════════════
    // SMART SCROLL SETTINGS
    // ═══════════════════════════════════════════════════════════════════════════════

    /** Enable intelligent scrolling with end-of-content detection */
    val smartScrollEnabled: Boolean = true,

    /** Maximum scroll attempts before giving up */
    val maxScrollAttempts: Int = 10,

    /** Minimum wait after scroll for content to settle (ms) */
    val scrollSettleTimeMs: Long = 300,

    /** Maximum wait after scroll for dynamic content to load (ms) */
    val scrollDynamicLoadTimeoutMs: Long = 2000,

    /** Percentage of viewport to scroll (0.0-1.0) */
    val scrollViewportPercentage: Double = 0.6,

    /** Number of identical content snapshots to detect end of scrollable area */
    val endOfContentSnapshots: Int = 2,

    /** Enable momentum/inertia scrolling detection */
    val detectScrollMomentum: Boolean = true,

    /** Time to wait for scroll momentum to settle (ms) */
    val scrollMomentumSettleMs: Long = 500,

    // ═══════════════════════════════════════════════════════════════════════════════
    // PAGE LOAD INTELLIGENCE SETTINGS
    // ═══════════════════════════════════════════════════════════════════════════════

    /** Enable intelligent page load detection */
    val pageLoadIntelligenceEnabled: Boolean = true,

    /** Wait for network idle (no pending requests) */
    val waitForNetworkIdle: Boolean = true,

    /** Network idle threshold - time with no network activity (ms) */
    val networkIdleThresholdMs: Long = 500,

    /** Maximum time to wait for page load (ms) */
    val pageLoadTimeoutMs: Long = 30000,

    /** Wait for DOM to be stable (no mutations) */
    val waitForDomStable: Boolean = true,

    /** DOM stability threshold - time with no DOM mutations (ms) */
    val domStabilityThresholdMs: Long = 300,

    /** Detect and wait for lazy-loaded content */
    val detectLazyLoading: Boolean = true,

    // ═══════════════════════════════════════════════════════════════════════════════
    // RETRY AND BACKOFF SETTINGS
    // ═══════════════════════════════════════════════════════════════════════════════

    /** Enable exponential backoff for retries */
    val exponentialBackoffEnabled: Boolean = true,

    /** Initial retry delay (ms) */
    val initialRetryDelayMs: Long = 100,

    /** Maximum retry delay (ms) */
    val maxRetryDelayMs: Long = 5000,

    /** Backoff multiplier (delay = previousDelay * multiplier) */
    val backoffMultiplier: Double = 2.0,

    /** Add jitter to prevent thundering herd (0.0-1.0) */
    val retryJitter: Double = 0.1,

    /** Maximum number of retries for transient failures */
    val maxTransientRetries: Int = 3,

    // ═══════════════════════════════════════════════════════════════════════════════
    // STALE ELEMENT HANDLING
    // ═══════════════════════════════════════════════════════════════════════════════

    /** Enable automatic stale element recovery */
    val staleElementRecoveryEnabled: Boolean = true,

    /** Maximum retries for stale element recovery */
    val staleElementMaxRetries: Int = 3,

    /** Delay before re-finding stale element (ms) */
    val staleElementRefindDelayMs: Long = 200,

    // ═══════════════════════════════════════════════════════════════════════════════
    // ACTION-SPECIFIC TIMEOUTS
    // ═══════════════════════════════════════════════════════════════════════════════

    /** Per-action timeout overrides (action -> timeout in ms) */
    val actionTimeouts: Map<String, Long> = mapOf(
        "CLICK" to 5000,
        "ENTER_TEXT" to 10000,
        "CLEAR_TEXT" to 5000,
        "SCROLL" to 3000,
        "SWIPE" to 3000,
        "VERIFY" to 15000,
        "WAIT" to 30000,
        "LAUNCH_APP" to 30000,
        "DEFAULT" to 10000
    ),

    // ═══════════════════════════════════════════════════════════════════════════════
    // PLATFORM-SPECIFIC ADJUSTMENTS
    // ═══════════════════════════════════════════════════════════════════════════════

    /** Platform-specific timing multipliers */
    val platformMultipliers: Map<String, Double> = mapOf(
        "ANDROID" to 1.0,
        "IOS" to 1.2,      // iOS animations are typically slower
        "WEB" to 0.8       // Web is typically faster
    ),

    // ═══════════════════════════════════════════════════════════════════════════════
    // NETWORK LATENCY PROFILES
    // ═══════════════════════════════════════════════════════════════════════════════

    /** Current network latency profile */
    val networkProfile: NetworkProfile = NetworkProfile.NORMAL,

    /** Enable adaptive timeout adjustment based on observed latency */
    val adaptiveTimeoutsEnabled: Boolean = true,

    /** Minimum observed operations before adjusting timeouts */
    val adaptiveMinSamples: Int = 10
) {
    /**
     * Network latency profiles for different environments.
     */
    enum class NetworkProfile(
        val multiplier: Double,
        val description: String
    ) {
        FAST(0.7, "High-speed connection (5G, WiFi)"),
        NORMAL(1.0, "Standard connection (4G, broadband)"),
        SLOW(1.5, "Slow connection (3G, congested)"),
        VERY_SLOW(2.5, "Very slow connection (2G, high latency)"),
        OFFLINE_FIRST(0.5, "App works offline, minimal network dependency")
    }

    /**
     * Get timeout for a specific action, adjusted for platform and network.
     */
    fun getActionTimeout(action: String, platform: Platform? = null): Long {
        val baseTimeout = actionTimeouts[action] ?: actionTimeouts["DEFAULT"] ?: 10000L
        val platformMultiplier = platform?.let { platformMultipliers[it.name] } ?: 1.0
        val networkMultiplier = networkProfile.multiplier

        return (baseTimeout * platformMultiplier * networkMultiplier).toLong()
    }

    /**
     * Calculate retry delay with exponential backoff.
     */
    fun calculateRetryDelay(attemptNumber: Int): Long {
        if (!exponentialBackoffEnabled) {
            return initialRetryDelayMs
        }

        val exponentialDelay = initialRetryDelayMs * Math.pow(backoffMultiplier, (attemptNumber - 1).toDouble())
        val cappedDelay = minOf(exponentialDelay, maxRetryDelayMs.toDouble())

        // Add jitter
        val jitterRange = cappedDelay * retryJitter
        val jitter = (Math.random() * jitterRange * 2) - jitterRange

        return (cappedDelay + jitter).toLong().coerceAtLeast(initialRetryDelayMs)
    }

    companion object {
        /**
         * Create configuration optimized for fast, stable environments.
         */
        fun fast(): StabilityConfig = StabilityConfig(
            stabilityTimeoutMs = 1500,
            stableReadingsRequired = 2,
            scrollSettleTimeMs = 150,
            networkProfile = NetworkProfile.FAST,
            maxTransientRetries = 2
        )

        /**
         * Create configuration for slow/unstable environments.
         */
        fun robust(): StabilityConfig = StabilityConfig(
            stabilityTimeoutMs = 5000,
            stableReadingsRequired = 4,
            scrollSettleTimeMs = 500,
            scrollDynamicLoadTimeoutMs = 3000,
            networkProfile = NetworkProfile.SLOW,
            maxTransientRetries = 5,
            pageLoadTimeoutMs = 60000
        )

        /**
         * Create configuration for CI/CD environments.
         */
        fun ci(): StabilityConfig = StabilityConfig(
            stabilityTimeoutMs = 4000,
            stableReadingsRequired = 3,
            scrollSettleTimeMs = 400,
            networkProfile = NetworkProfile.NORMAL,
            maxTransientRetries = 4,
            adaptiveTimeoutsEnabled = false  // Consistent timing in CI
        )

        /**
         * Create default configuration.
         */
        fun default(): StabilityConfig = StabilityConfig()
    }
}
