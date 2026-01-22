package com.testzen.core.stability.registry

import com.testzen.core.model.Platform
import com.testzen.core.stability.SmartScrollStrategy
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement

/**
 * Platform-agnostic interface for stability operations.
 *
 * Different platforms (Web, Android, iOS) require different approaches
 * for detecting page load, scrolling, animation detection, etc.
 * This interface abstracts those platform-specific details.
 *
 * Usage:
 * ```kotlin
 * val adapter = when (platform) {
 *     Platform.WEB -> WebStabilityAdapter()
 *     Platform.ANDROID, Platform.IOS -> MobileStabilityAdapter(platform)
 * }
 *
 * // Platform-agnostic operations
 * adapter.waitForPageReady(driver, timeout)
 * adapter.getScrollPosition(driver, direction)
 * adapter.isElementAnimating(driver, element)
 * ```
 */
interface PlatformStabilityAdapter {

    /**
     * The platform this adapter supports.
     */
    val supportedPlatform: Platform

    /**
     * Check if this adapter supports the given driver.
     */
    fun supportsDriver(driver: WebDriver): Boolean

    // ═══════════════════════════════════════════════════════════════════════════════
    // PAGE LOAD DETECTION
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Check if page/screen is fully loaded.
     *
     * Web: document.readyState === 'complete'
     * Mobile: Activity/ViewController loaded, no pending animations
     */
    fun isPageReady(driver: WebDriver): Boolean

    /**
     * Wait for page to be ready.
     *
     * @param driver WebDriver instance
     * @param timeoutMs Maximum time to wait
     * @return true if page ready, false if timeout
     */
    fun waitForPageReady(driver: WebDriver, timeoutMs: Long): Boolean

    /**
     * Check if network is idle (no pending requests).
     *
     * Web: No pending XHR/Fetch
     * Mobile: No pending API calls (if detectable)
     */
    fun isNetworkIdle(driver: WebDriver): Boolean

    /**
     * Get count of pending network requests.
     */
    fun getPendingNetworkRequests(driver: WebDriver): Int

    // ═══════════════════════════════════════════════════════════════════════════════
    // DOM/VIEW STABILITY
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Check if DOM/View hierarchy is stable (no mutations).
     *
     * Web: MutationObserver
     * Mobile: View hierarchy stable
     */
    fun isDomStable(driver: WebDriver): Boolean

    /**
     * Check if there are recent DOM mutations.
     */
    fun hasDomMutations(driver: WebDriver): Boolean

    /**
     * Reset the DOM mutation tracking flag.
     */
    fun resetDomMutationTracking(driver: WebDriver)

    /**
     * Inject necessary observers/monitors for the platform.
     */
    fun injectMonitors(driver: WebDriver)

    // ═══════════════════════════════════════════════════════════════════════════════
    // SCROLL OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Get current scroll position.
     *
     * @param direction Scroll direction context
     * @return Scroll position in pixels, or 0 if not determinable
     */
    fun getScrollPosition(driver: WebDriver, direction: SmartScrollStrategy.ScrollDirection): Int

    /**
     * Get total scrollable content size.
     *
     * @param direction Scroll direction context
     * @return Total content size in pixels
     */
    fun getTotalContentSize(driver: WebDriver, direction: SmartScrollStrategy.ScrollDirection): Int

    /**
     * Get viewport size for scroll calculations.
     */
    fun getViewportSize(driver: WebDriver, direction: SmartScrollStrategy.ScrollDirection): Int

    /**
     * Get a hash representing current visible content.
     * Used for end-of-content detection.
     */
    fun getContentHash(driver: WebDriver): Int

    /**
     * Get count of visible elements (for content change detection).
     */
    fun getVisibleElementCount(driver: WebDriver): Int

    /**
     * Scroll element into view.
     */
    fun scrollElementIntoView(driver: WebDriver, element: WebElement)

    // ═══════════════════════════════════════════════════════════════════════════════
    // ANIMATION DETECTION
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Check if there are active animations on the page/screen.
     */
    fun hasActiveAnimations(driver: WebDriver): Boolean

    /**
     * Check if a specific element is currently animating.
     */
    fun isElementAnimating(driver: WebDriver, element: WebElement): Boolean

    /**
     * Wait for element animations to complete.
     *
     * @param element Element to wait for
     * @param timeoutMs Maximum time to wait
     * @return true if animations completed, false if timeout
     */
    fun waitForElementAnimations(driver: WebDriver, element: WebElement, timeoutMs: Long): Boolean

    // ═══════════════════════════════════════════════════════════════════════════════
    // ASYNC/FRAMEWORK DETECTION
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Check if async operations are complete.
     *
     * Web: jQuery, Angular, React loading states
     * Mobile: Background tasks, pending API calls
     */
    fun isAsyncComplete(driver: WebDriver): Boolean

    /**
     * Check for visible loading indicators.
     *
     * @param selectors CSS selectors (web) or resource IDs (mobile)
     * @return true if any loading indicator is visible
     */
    fun hasLoadingIndicators(driver: WebDriver, selectors: List<String>): Boolean

    // ═══════════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Execute platform-specific script/command.
     *
     * @param script JavaScript (web) or mobile script
     * @param args Arguments to pass
     * @return Result of execution, or null if not supported
     */
    fun executeScript(driver: WebDriver, script: String, vararg args: Any): Any?

    /**
     * Check if script execution is supported.
     */
    fun supportsScriptExecution(driver: WebDriver): Boolean

    /**
     * Get platform-specific capabilities or information.
     */
    fun getPlatformInfo(driver: WebDriver): Map<String, Any>
}

/**
 * Result of a platform stability check.
 */
data class StabilityCheckResult(
    val isStable: Boolean,
    val reason: String,
    val platform: Platform,
    val checkTimeMs: Long,
    val details: Map<String, Any> = emptyMap()
)
