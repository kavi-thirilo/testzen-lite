package com.testzen.core.stability

import com.testzen.core.model.Platform
import com.testzen.core.stability.registry.MobileStabilityAdapter
import com.testzen.core.stability.registry.PlatformStabilityAdapter
import com.testzen.core.stability.registry.WebStabilityAdapter
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.slf4j.LoggerFactory

/**
 * Central orchestrator for all stability features.
 *
 * Coordinates element stability, scroll strategy, page load intelligence,
 * and retry mechanisms to provide a unified, resilient interaction layer.
 *
 * Supports all platforms (Android, iOS, Web) through platform-specific adapters
 * that handle the differences in page load detection, scrolling, and animation
 * detection.
 *
 * Usage:
 * ```kotlin
 * // Create with platform-specific adapter
 * val orchestrator = StabilityOrchestrator.forPlatform(Platform.ANDROID)
 *
 * // Or create with explicit adapter
 * val webOrchestrator = StabilityOrchestrator(config, Platform.WEB, WebStabilityAdapter())
 *
 * // Find element with full stability handling
 * val element = orchestrator.findElementStable(driver, "Login") {
 *     driver.findElement(By.xpath("//*[contains(text(),'Login')]"))
 * }
 *
 * // Click with stability
 * orchestrator.clickStable(driver, element)
 *
 * // Wait for page ready after navigation
 * orchestrator.waitForPageStable(driver)
 * ```
 */
class StabilityOrchestrator(
    private val config: StabilityConfig = StabilityConfig.default(),
    private val platform: Platform? = null,
    private val platformAdapter: PlatformStabilityAdapter = createDefaultAdapter(platform)
) {
    private val logger = LoggerFactory.getLogger(StabilityOrchestrator::class.java)

    // Component instances
    val elementWaiter = ElementStabilityWaiter(config)
    val scrollStrategy = SmartScrollStrategy(config, platformAdapter)
    val pageLoadIntelligence = PageLoadIntelligence(config, platformAdapter)
    val retryMechanism = RetryWithBackoff(config)

    /**
     * Result of a stable find operation.
     */
    sealed class FindResult {
        data class Found(
            val element: WebElement,
            val scrolled: Boolean,
            val scrollCount: Int,
            val stabilityWaitMs: Long,
            val totalTimeMs: Long
        ) : FindResult()

        data class NotFound(
            val reason: String,
            val scrollAttempted: Boolean,
            val scrollCount: Int,
            val totalTimeMs: Long
        ) : FindResult()
    }

    /**
     * Find element with full stability handling.
     *
     * - Waits for page to be ready
     * - Attempts to find element
     * - Scrolls if not found and scrolling enabled
     * - Waits for element to stabilize
     * - Returns stable element ready for interaction
     *
     * @param driver WebDriver instance
     * @param description Description for logging
     * @param timeoutMs Maximum time for entire operation
     * @param scrollEnabled Enable scrolling to find
     * @param finder Function to find the element
     * @return FindResult with element or failure details
     */
    fun findElementStable(
        driver: WebDriver,
        description: String,
        timeoutMs: Long = config.getActionTimeout("DEFAULT", platform),
        scrollEnabled: Boolean = config.smartScrollEnabled,
        finder: () -> WebElement?
    ): FindResult {
        val startTime = System.currentTimeMillis()
        var scrollCount = 0

        logger.debug("Finding element with stability: '$description' (timeout: ${timeoutMs}ms)")

        // Phase 1: Ensure page is ready
        if (config.pageLoadIntelligenceEnabled) {
            val pageReadyResult = pageLoadIntelligence.waitForPageReady(driver, timeoutMs / 4)
            if (pageReadyResult is PageLoadIntelligence.WaitResult.Timeout) {
                logger.debug("Page not fully ready, continuing anyway: ${pageReadyResult.reason}")
            }
        }

        // Phase 2: Try to find element directly
        val directResult = findWithStability(driver, finder, timeoutMs / 3)
        if (directResult is FindResult.Found) {
            val totalTime = System.currentTimeMillis() - startTime
            logger.debug("Element '$description' found directly in ${totalTime}ms")
            return directResult.copy(totalTimeMs = totalTime)
        }

        // Phase 3: Try scrolling if enabled
        if (scrollEnabled) {
            logger.debug("Element not found directly, attempting scroll search")

            val scrollResult = scrollStrategy.scrollAllDirectionsToFind(
                driver,
                findElement = {
                    try {
                        val element = finder()
                        if (element != null && element.isDisplayed) element else null
                    } catch (e: Exception) {
                        null
                    }
                },
                primaryDirection = SmartScrollStrategy.ScrollDirection.DOWN,
                maxAttempts = config.maxScrollAttempts
            )

            when (scrollResult) {
                is SmartScrollStrategy.ScrollResult.ElementFound -> {
                    scrollCount = scrollResult.scrollCount

                    // Wait for element stability after scroll
                    val stabilityResult = elementWaiter.waitForStability(
                        scrollResult.element,
                        config.stabilityTimeoutMs
                    )

                    val totalTime = System.currentTimeMillis() - startTime

                    return when (stabilityResult) {
                        is ElementStabilityWaiter.StabilityResult.Stable -> {
                            logger.debug("Element '$description' found after $scrollCount scrolls, stable in ${totalTime}ms")
                            FindResult.Found(
                                element = stabilityResult.element,
                                scrolled = true,
                                scrollCount = scrollCount,
                                stabilityWaitMs = stabilityResult.waitTimeMs,
                                totalTimeMs = totalTime
                            )
                        }
                        else -> {
                            // Element found but not stable, return anyway with warning
                            logger.warn("Element '$description' found but not stable")
                            FindResult.Found(
                                element = scrollResult.element,
                                scrolled = true,
                                scrollCount = scrollCount,
                                stabilityWaitMs = 0,
                                totalTimeMs = totalTime
                            )
                        }
                    }
                }

                is SmartScrollStrategy.ScrollResult.EndOfContent -> {
                    scrollCount = scrollResult.scrollCount
                    val totalTime = System.currentTimeMillis() - startTime
                    logger.debug("Element '$description' not found, reached end of content after $scrollCount scrolls")
                    return FindResult.NotFound(
                        reason = "End of scrollable content reached",
                        scrollAttempted = true,
                        scrollCount = scrollCount,
                        totalTimeMs = totalTime
                    )
                }

                else -> {
                    scrollCount = (scrollResult as? SmartScrollStrategy.ScrollResult.Failure)?.scrollCount ?: 0
                }
            }
        }

        val totalTime = System.currentTimeMillis() - startTime
        return FindResult.NotFound(
            reason = "Element not found within timeout",
            scrollAttempted = scrollEnabled,
            scrollCount = scrollCount,
            totalTimeMs = totalTime
        )
    }

    /**
     * Find element and wait for stability.
     */
    private fun findWithStability(
        driver: WebDriver,
        finder: () -> WebElement?,
        timeoutMs: Long
    ): FindResult {
        val startTime = System.currentTimeMillis()

        // Use element waiter to find and stabilize
        val result = elementWaiter.waitForElementAndStability(finder, timeoutMs)

        return when (result) {
            is ElementStabilityWaiter.StabilityResult.Stable -> {
                val totalTime = System.currentTimeMillis() - startTime
                FindResult.Found(
                    element = result.element,
                    scrolled = false,
                    scrollCount = 0,
                    stabilityWaitMs = result.waitTimeMs,
                    totalTimeMs = totalTime
                )
            }
            else -> {
                val totalTime = System.currentTimeMillis() - startTime
                FindResult.NotFound(
                    reason = "Element not found or not stable",
                    scrollAttempted = false,
                    scrollCount = 0,
                    totalTimeMs = totalTime
                )
            }
        }
    }

    /**
     * Perform click with stability handling.
     *
     * - Waits for element stability
     * - Waits for animations
     * - Performs click with retry on stale element
     * - Waits for page stability after click
     */
    fun clickStable(
        driver: WebDriver,
        element: WebElement,
        waitForPageAfter: Boolean = true
    ): Boolean {
        val timeout = config.getActionTimeout("CLICK", platform)

        logger.debug("Performing stable click (timeout: ${timeout}ms)")

        // Ensure element is stable before clicking
        val stabilityResult = elementWaiter.waitForInteractive(element, driver, timeout / 3)
        if (!stabilityResult) {
            logger.warn("Element not stable/interactive before click")
            // Continue anyway, but log warning
        }

        // Perform click with retry
        val clickResult = retryMechanism.execute(
            operation = {
                element.click()
            },
            maxAttempts = config.maxTransientRetries + 1,
            timeoutMs = timeout
        )

        val success = clickResult is RetryWithBackoff.RetryResult.Success

        // Wait for page stability after click
        if (success && waitForPageAfter) {
            Thread.sleep(config.defaultAnimationDurationMs)  // Wait for any immediate animation
            pageLoadIntelligence.waitForPageReady(driver, config.pageLoadTimeoutMs / 3)
        }

        return success
    }

    /**
     * Enter text with stability handling.
     *
     * - Waits for element stability
     * - Clears existing text if requested
     * - Enters text with retry on stale element
     */
    fun enterTextStable(
        driver: WebDriver,
        element: WebElement,
        text: String,
        clearFirst: Boolean = true
    ): Boolean {
        val timeout = config.getActionTimeout("ENTER_TEXT", platform)

        logger.debug("Entering text with stability (length: ${text.length})")

        // Ensure element is stable
        val stabilityResult = elementWaiter.waitForInteractive(element, driver, timeout / 3)
        if (!stabilityResult) {
            logger.warn("Element not stable before text entry")
        }

        val result = retryMechanism.execute(
            operation = {
                if (clearFirst) {
                    element.clear()
                }
                element.sendKeys(text)
            },
            maxAttempts = config.maxTransientRetries + 1,
            timeoutMs = timeout
        )

        return result is RetryWithBackoff.RetryResult.Success
    }

    /**
     * Wait for page to be fully stable.
     */
    fun waitForPageStable(
        driver: WebDriver,
        timeoutMs: Long = config.pageLoadTimeoutMs
    ): Boolean {
        val result = pageLoadIntelligence.waitForPageReady(driver, timeoutMs)
        return result is PageLoadIntelligence.WaitResult.Success
    }

    /**
     * Wait for element to disappear (loading spinners, etc.).
     */
    fun waitForElementGone(
        finder: () -> WebElement?,
        timeoutMs: Long = config.stabilityTimeoutMs
    ): Boolean {
        return elementWaiter.waitForDisappearance(finder, timeoutMs)
    }

    /**
     * Scroll to find element with intelligence.
     */
    fun scrollToFind(
        driver: WebDriver,
        finder: () -> WebElement?,
        direction: SmartScrollStrategy.ScrollDirection = SmartScrollStrategy.ScrollDirection.DOWN
    ): SmartScrollStrategy.ScrollResult {
        return scrollStrategy.scrollToFind(driver, finder, direction, config.maxScrollAttempts)
    }

    /**
     * Execute any operation with full stability handling.
     */
    fun <T> executeStable(
        driver: WebDriver,
        description: String,
        timeoutMs: Long = config.getActionTimeout("DEFAULT", platform),
        waitForPageBefore: Boolean = true,
        waitForPageAfter: Boolean = true,
        operation: () -> T
    ): T {
        logger.debug("Executing stable operation: $description")

        // Wait for page ready before operation
        if (waitForPageBefore) {
            pageLoadIntelligence.waitForPageReady(driver, timeoutMs / 4)
        }

        // Execute with retry
        val result = retryMechanism.execute(
            operation = { operation() },
            maxAttempts = config.maxTransientRetries + 1,
            timeoutMs = timeoutMs
        )

        // Wait for page ready after operation
        if (waitForPageAfter && result is RetryWithBackoff.RetryResult.Success) {
            pageLoadIntelligence.waitForPageReady(driver, timeoutMs / 4)
        }

        return when (result) {
            is RetryWithBackoff.RetryResult.Success -> result.value
            is RetryWithBackoff.RetryResult.Failure -> throw result.lastException
        }
    }

    /**
     * Execute operation on element with stale element recovery.
     */
    fun <T> executeOnElement(
        description: String,
        refind: () -> WebElement?,
        operation: (WebElement) -> T
    ): T {
        val result = retryMechanism.executeWithStaleRecovery(refind, operation)

        return when (result) {
            is RetryWithBackoff.RetryResult.Success -> result.value
            is RetryWithBackoff.RetryResult.Failure -> throw result.lastException
        }
    }

    /**
     * Verify element state with intelligent waiting.
     */
    fun verifyWithWait(
        driver: WebDriver,
        finder: () -> WebElement?,
        verification: (WebElement) -> Boolean,
        timeoutMs: Long = config.getActionTimeout("VERIFY", platform),
        description: String = "verification"
    ): Boolean {
        val startTime = System.currentTimeMillis()

        // Wait for page ready first
        if (config.pageLoadIntelligenceEnabled) {
            pageLoadIntelligence.waitForPageReady(driver, timeoutMs / 4)
        }

        // Poll until verification passes or timeout
        return retryMechanism.waitForCondition(
            condition = {
                try {
                    val element = finder()
                    element != null && verification(element)
                } catch (e: StaleElementReferenceException) {
                    false  // Will retry
                } catch (e: Exception) {
                    logger.trace("Verification check failed: ${e.message}")
                    false
                }
            },
            timeoutMs = timeoutMs - (System.currentTimeMillis() - startTime),
            pollIntervalMs = 200,
            description = description
        )
    }

    companion object {
        /**
         * Create the default platform adapter based on platform.
         */
        private fun createDefaultAdapter(platform: Platform?): PlatformStabilityAdapter {
            return when (platform) {
                Platform.ANDROID -> MobileStabilityAdapter(Platform.ANDROID)
                Platform.IOS -> MobileStabilityAdapter(Platform.IOS)
                Platform.WEB -> WebStabilityAdapter()
                null -> WebStabilityAdapter() // Default to web
            }
        }

        /**
         * Create orchestrator for a specific platform with optimized settings.
         */
        fun forPlatform(platform: Platform): StabilityOrchestrator {
            val config = when (platform) {
                Platform.ANDROID -> StabilityConfig.default().copy(
                    platformMultipliers = mapOf("ANDROID" to 1.0, "IOS" to 1.2, "WEB" to 0.8),
                    waitForNetworkIdle = false,
                    waitForDomStable = false
                )
                Platform.IOS -> StabilityConfig.default().copy(
                    platformMultipliers = mapOf("ANDROID" to 1.0, "IOS" to 1.2, "WEB" to 0.8),
                    waitForNetworkIdle = false,
                    waitForDomStable = false,
                    scrollMomentumSettleMs = 700
                )
                Platform.WEB -> StabilityConfig.default()
            }
            return StabilityOrchestrator(config, platform, createDefaultAdapter(platform))
        }

        /**
         * Create orchestrator optimized for fast environments.
         */
        fun fast(platform: Platform? = null): StabilityOrchestrator {
            return StabilityOrchestrator(StabilityConfig.fast(), platform, createDefaultAdapter(platform))
        }

        /**
         * Create orchestrator optimized for slow/flaky environments.
         */
        fun robust(platform: Platform? = null): StabilityOrchestrator {
            return StabilityOrchestrator(StabilityConfig.robust(), platform, createDefaultAdapter(platform))
        }

        /**
         * Create orchestrator optimized for CI/CD.
         */
        fun ci(platform: Platform? = null): StabilityOrchestrator {
            return StabilityOrchestrator(StabilityConfig.ci(), platform, createDefaultAdapter(platform))
        }

        /**
         * Create default orchestrator.
         */
        fun default(platform: Platform? = null): StabilityOrchestrator {
            return StabilityOrchestrator(StabilityConfig.default(), platform, createDefaultAdapter(platform))
        }
    }
}
