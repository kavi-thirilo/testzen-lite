package com.testzen.core.stability

import com.testzen.core.stability.registry.PlatformStabilityAdapter
import com.testzen.core.stability.registry.WebStabilityAdapter
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.ExpectedCondition
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Intelligent page load and network latency handler.
 *
 * Features:
 * - Wait for network idle (no pending requests)
 * - Wait for DOM stability (no mutations)
 * - Detect and wait for lazy-loaded content
 * - Adaptive timeout adjustment based on observed latency
 * - Page readiness verification
 * - Cross-platform support (Android, iOS, Web)
 */
class PageLoadIntelligence(
    private val config: StabilityConfig = StabilityConfig.default(),
    private val platformAdapter: PlatformStabilityAdapter = WebStabilityAdapter()
) {
    private val logger = LoggerFactory.getLogger(PageLoadIntelligence::class.java)

    // Adaptive timeout tracking
    private val operationTimings = ConcurrentHashMap<String, MutableList<Long>>()
    private val adaptiveMultiplier = AtomicLong(java.lang.Double.doubleToLongBits(1.0))

    /**
     * Page state for readiness checks.
     */
    data class PageState(
        val documentReady: Boolean,
        val networkIdle: Boolean,
        val domStable: Boolean,
        val noAnimations: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        val isFullyReady: Boolean
            get() = documentReady && networkIdle && domStable && noAnimations
    }

    /**
     * Wait result with timing information.
     */
    sealed class WaitResult {
        data class Success(
            val waitTimeMs: Long,
            val pageState: PageState
        ) : WaitResult()

        data class Timeout(
            val waitTimeMs: Long,
            val partialState: PageState,
            val reason: String
        ) : WaitResult()

        data class Error(
            val message: String,
            val exception: Exception?
        ) : WaitResult()
    }

    /**
     * Wait for page to be fully loaded and stable.
     *
     * @param driver WebDriver instance
     * @param timeoutMs Maximum time to wait
     * @return WaitResult indicating outcome
     */
    fun waitForPageReady(
        driver: WebDriver,
        timeoutMs: Long = config.pageLoadTimeoutMs
    ): WaitResult {
        if (!config.pageLoadIntelligenceEnabled) {
            return WaitResult.Success(0, PageState(true, true, true, true))
        }

        val startTime = System.currentTimeMillis()
        val adjustedTimeout = getAdaptiveTimeout(timeoutMs)

        logger.debug("Waiting for page ready (timeout: ${adjustedTimeout}ms)")

        try {
            // Phase 1: Wait for document ready state
            if (!waitForDocumentReady(driver, adjustedTimeout / 3)) {
                val elapsed = System.currentTimeMillis() - startTime
                return WaitResult.Timeout(elapsed, getCurrentPageState(driver), "Document not ready")
            }

            // Phase 2: Wait for network idle
            if (config.waitForNetworkIdle) {
                val networkTimeout = adjustedTimeout / 3
                if (!waitForNetworkIdle(driver, networkTimeout)) {
                    logger.debug("Network not idle within timeout, continuing...")
                }
            }

            // Phase 3: Wait for DOM stability
            if (config.waitForDomStable) {
                val domTimeout = adjustedTimeout / 4
                if (!waitForDomStability(driver, domTimeout)) {
                    logger.debug("DOM not stable within timeout, continuing...")
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            val finalState = getCurrentPageState(driver)

            // Track timing for adaptive adjustment
            trackTiming("pageLoad", elapsed)

            logger.debug("Page ready after ${elapsed}ms")
            return WaitResult.Success(elapsed, finalState)

        } catch (e: Exception) {
            logger.error("Error waiting for page ready: ${e.message}")
            return WaitResult.Error(e.message ?: "Unknown error", e)
        }
    }

    /**
     * Wait for document.readyState to be 'complete'.
     * Uses platform adapter for cross-platform support.
     */
    fun waitForDocumentReady(driver: WebDriver, timeoutMs: Long): Boolean {
        return platformAdapter.waitForPageReady(driver, timeoutMs)
    }

    /**
     * Wait for network to become idle (no pending XHR/fetch requests).
     * Uses platform adapter for cross-platform support.
     */
    fun waitForNetworkIdle(driver: WebDriver, timeoutMs: Long): Boolean {
        // Inject monitors (no-op on mobile)
        platformAdapter.injectMonitors(driver)

        val startTime = System.currentTimeMillis()
        var idleStartTime: Long? = null

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val pendingRequests = platformAdapter.getPendingNetworkRequests(driver)

            if (pendingRequests == 0) {
                if (idleStartTime == null) {
                    idleStartTime = System.currentTimeMillis()
                } else if (System.currentTimeMillis() - idleStartTime >= config.networkIdleThresholdMs) {
                    logger.debug("Network idle for ${config.networkIdleThresholdMs}ms")
                    return true
                }
            } else {
                idleStartTime = null
                logger.trace("$pendingRequests pending network requests")
            }

            Thread.sleep(50)
        }

        return false
    }

    /**
     * Wait for DOM to stabilize (no mutations).
     * Uses platform adapter for cross-platform support.
     */
    fun waitForDomStability(driver: WebDriver, timeoutMs: Long): Boolean {
        // Inject observers (no-op on mobile)
        platformAdapter.injectMonitors(driver)

        val startTime = System.currentTimeMillis()
        var stableStartTime: Long? = null

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val hasMutations = platformAdapter.hasDomMutations(driver)

            if (!hasMutations) {
                if (stableStartTime == null) {
                    stableStartTime = System.currentTimeMillis()
                } else if (System.currentTimeMillis() - stableStartTime >= config.domStabilityThresholdMs) {
                    logger.debug("DOM stable for ${config.domStabilityThresholdMs}ms")
                    return true
                }
            } else {
                stableStartTime = null
                platformAdapter.resetDomMutationTracking(driver)
            }

            Thread.sleep(50)
        }

        return false
    }

    /**
     * Wait for AJAX/async content to load.
     * Detects jQuery, Angular, React loading states.
     * Uses platform adapter for cross-platform support.
     */
    fun waitForAsyncContent(driver: WebDriver, timeoutMs: Long): Boolean {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val asyncComplete = platformAdapter.isAsyncComplete(driver)
            if (asyncComplete) {
                return true
            }
            Thread.sleep(100)
        }

        return false
    }

    /**
     * Wait for specific loading indicators to disappear.
     * Uses platform adapter for cross-platform support.
     */
    fun waitForLoadingIndicatorsGone(
        driver: WebDriver,
        selectors: List<String> = DEFAULT_LOADING_SELECTORS,
        timeoutMs: Long = config.pageLoadTimeoutMs / 2
    ): Boolean {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val hasLoadingIndicator = platformAdapter.hasLoadingIndicators(driver, selectors)

            if (!hasLoadingIndicator) {
                val elapsed = System.currentTimeMillis() - startTime
                logger.debug("Loading indicators gone after ${elapsed}ms")
                return true
            }

            Thread.sleep(100)
        }

        return false
    }

    /**
     * Get current page state.
     * Uses platform adapter for cross-platform support.
     */
    fun getCurrentPageState(driver: WebDriver): PageState {
        val documentReady = try {
            platformAdapter.isPageReady(driver)
        } catch (e: Exception) {
            false
        }

        val networkIdle = try {
            platformAdapter.isNetworkIdle(driver)
        } catch (e: Exception) {
            true
        }

        val domStable = try {
            platformAdapter.isDomStable(driver)
        } catch (e: Exception) {
            true
        }

        val noAnimations = try {
            !platformAdapter.hasActiveAnimations(driver)
        } catch (e: Exception) {
            true
        }

        return PageState(documentReady, networkIdle, domStable, noAnimations)
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ADAPTIVE TIMEOUT MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Track operation timing for adaptive adjustment.
     */
    fun trackTiming(operation: String, durationMs: Long) {
        if (!config.adaptiveTimeoutsEnabled) return

        val timings = operationTimings.computeIfAbsent(operation) { mutableListOf() }
        synchronized(timings) {
            timings.add(durationMs)
            // Keep last N samples
            while (timings.size > 100) {
                timings.removeAt(0)
            }
        }

        updateAdaptiveMultiplier()
    }

    /**
     * Get adaptive timeout based on observed latency.
     */
    fun getAdaptiveTimeout(baseTimeout: Long): Long {
        if (!config.adaptiveTimeoutsEnabled) {
            return baseTimeout
        }

        val multiplier = java.lang.Double.longBitsToDouble(adaptiveMultiplier.get())
        return (baseTimeout * multiplier).toLong()
    }

    private fun updateAdaptiveMultiplier() {
        val allTimings = operationTimings.values.flatten()
        if (allTimings.size < config.adaptiveMinSamples) {
            return
        }

        // Calculate average and adjust multiplier
        val avgTiming = allTimings.average()
        val expectedTiming = 1000.0  // Expected 1 second average

        val newMultiplier = when {
            avgTiming > expectedTiming * 2 -> 1.5  // Much slower than expected
            avgTiming > expectedTiming * 1.5 -> 1.3
            avgTiming > expectedTiming -> 1.1
            avgTiming < expectedTiming * 0.5 -> 0.8  // Much faster than expected
            else -> 1.0
        }.coerceIn(0.7, 2.0)

        adaptiveMultiplier.set(java.lang.Double.doubleToLongBits(newMultiplier))
        logger.trace("Adaptive multiplier updated to $newMultiplier (avg timing: ${avgTiming}ms)")
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PLATFORM ADAPTER DELEGATE METHODS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Get pending network request count.
     * Delegates to platform adapter.
     */
    private fun getPendingRequestCount(driver: WebDriver): Int {
        return platformAdapter.getPendingNetworkRequests(driver)
    }

    /**
     * Check if DOM has mutations.
     * Delegates to platform adapter.
     */
    private fun hasDomMutations(driver: WebDriver): Boolean {
        return platformAdapter.hasDomMutations(driver)
    }

    /**
     * Check for active animations.
     * Delegates to platform adapter.
     */
    private fun hasActiveAnimations(driver: WebDriver): Boolean {
        return platformAdapter.hasActiveAnimations(driver)
    }

    companion object {
        /**
         * Common loading indicator CSS selectors.
         */
        val DEFAULT_LOADING_SELECTORS = listOf(
            ".loading",
            ".spinner",
            ".loader",
            "[class*='loading']",
            "[class*='spinner']",
            "[class*='skeleton']",
            ".progress",
            "[role='progressbar']",
            ".MuiCircularProgress-root",
            ".MuiLinearProgress-root",
            ".ant-spin",
            ".el-loading-mask"
        )
    }
}
