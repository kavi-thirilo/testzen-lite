package com.testzen.core.stability

import org.openqa.selenium.Dimension
import org.openqa.selenium.Point
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.JavascriptExecutor
import org.slf4j.LoggerFactory
import kotlin.math.abs

/**
 * Intelligent element stability waiter.
 *
 * Waits for elements to stabilize before interaction by monitoring:
 * - Position stability (element not moving/animating)
 * - Size stability (element not resizing)
 * - Visibility state
 * - CSS animations/transitions
 *
 * This prevents flaky tests caused by interactions with animating elements.
 */
class ElementStabilityWaiter(
    private val config: StabilityConfig = StabilityConfig.default()
) {
    private val logger = LoggerFactory.getLogger(ElementStabilityWaiter::class.java)

    /**
     * Element snapshot for stability comparison.
     */
    data class ElementSnapshot(
        val location: Point,
        val size: Dimension,
        val isDisplayed: Boolean,
        val isEnabled: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        /**
         * Check if this snapshot is stable compared to another.
         */
        fun isStableComparedTo(other: ElementSnapshot, positionTolerance: Int, sizeTolerance: Int): Boolean {
            val locationStable = abs(location.x - other.location.x) <= positionTolerance &&
                    abs(location.y - other.location.y) <= positionTolerance

            val sizeStable = abs(size.width - other.size.width) <= sizeTolerance &&
                    abs(size.height - other.size.height) <= sizeTolerance

            val stateStable = isDisplayed == other.isDisplayed && isEnabled == other.isEnabled

            return locationStable && sizeStable && stateStable
        }
    }

    /**
     * Result of stability wait operation.
     */
    sealed class StabilityResult {
        data class Stable(
            val element: WebElement,
            val finalSnapshot: ElementSnapshot,
            val waitTimeMs: Long,
            val checksPerformed: Int
        ) : StabilityResult()

        data class Unstable(
            val element: WebElement?,
            val reason: String,
            val snapshots: List<ElementSnapshot>,
            val waitTimeMs: Long
        ) : StabilityResult()

        data class ElementGone(
            val reason: String,
            val lastSnapshot: ElementSnapshot?
        ) : StabilityResult()
    }

    /**
     * Wait for element to stabilize (position, size, visibility).
     *
     * @param element The element to wait for
     * @param timeoutMs Maximum time to wait (uses config default if not specified)
     * @return StabilityResult indicating success or failure
     */
    fun waitForStability(
        element: WebElement,
        timeoutMs: Long = config.stabilityTimeoutMs
    ): StabilityResult {
        if (!config.elementStabilityEnabled) {
            return try {
                val snapshot = takeSnapshot(element)
                StabilityResult.Stable(element, snapshot, 0, 1)
            } catch (e: Exception) {
                StabilityResult.ElementGone("Element not accessible: ${e.message}", null)
            }
        }

        val startTime = System.currentTimeMillis()
        val snapshots = mutableListOf<ElementSnapshot>()
        var stableCount = 0

        logger.debug("Starting stability wait for element (timeout: ${timeoutMs}ms)")

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                val currentSnapshot = takeSnapshot(element)
                snapshots.add(currentSnapshot)

                // Check if we have enough stable readings
                if (snapshots.size >= 2) {
                    val previousSnapshot = snapshots[snapshots.size - 2]

                    if (currentSnapshot.isStableComparedTo(
                            previousSnapshot,
                            config.positionTolerancePx,
                            config.sizeTolerancePx
                        )
                    ) {
                        stableCount++
                        logger.trace("Element stable reading $stableCount/${config.stableReadingsRequired}")

                        if (stableCount >= config.stableReadingsRequired) {
                            val waitTime = System.currentTimeMillis() - startTime
                            logger.debug("Element stabilized after ${waitTime}ms (${snapshots.size} checks)")
                            return StabilityResult.Stable(element, currentSnapshot, waitTime, snapshots.size)
                        }
                    } else {
                        // Element moved/resized, reset stable count
                        stableCount = 0
                        logger.trace("Element position/size changed, resetting stability counter")
                    }
                }

                // Wait before next check
                Thread.sleep(config.stabilityCheckIntervalMs)

            } catch (e: StaleElementReferenceException) {
                val waitTime = System.currentTimeMillis() - startTime
                logger.warn("Element became stale during stability wait after ${waitTime}ms")
                return StabilityResult.ElementGone(
                    "Element became stale: ${e.message}",
                    snapshots.lastOrNull()
                )
            } catch (e: Exception) {
                val waitTime = System.currentTimeMillis() - startTime
                logger.warn("Error during stability check after ${waitTime}ms: ${e.message}")
                return StabilityResult.Unstable(
                    element,
                    "Error during check: ${e.message}",
                    snapshots,
                    waitTime
                )
            }
        }

        val waitTime = System.currentTimeMillis() - startTime
        logger.warn("Element did not stabilize within ${timeoutMs}ms (${snapshots.size} checks, $stableCount stable)")

        return StabilityResult.Unstable(
            element,
            "Element did not stabilize within timeout (last stable count: $stableCount)",
            snapshots,
            waitTime
        )
    }

    /**
     * Wait for element to appear and stabilize.
     *
     * @param findElement Function to find/re-find the element
     * @param timeoutMs Maximum time to wait
     * @return StabilityResult
     */
    fun waitForElementAndStability(
        findElement: () -> WebElement?,
        timeoutMs: Long = config.stabilityTimeoutMs + 5000
    ): StabilityResult {
        val startTime = System.currentTimeMillis()
        var lastException: Exception? = null

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                val element = findElement()
                if (element != null) {
                    // Element found, wait for it to stabilize
                    val remainingTime = timeoutMs - (System.currentTimeMillis() - startTime)
                    val result = waitForStability(element, minOf(remainingTime, config.stabilityTimeoutMs))

                    when (result) {
                        is StabilityResult.Stable -> return result
                        is StabilityResult.ElementGone -> {
                            // Element disappeared, continue trying
                            logger.debug("Element disappeared, will retry finding")
                        }
                        is StabilityResult.Unstable -> {
                            // Still unstable, but might become stable - continue
                            logger.debug("Element unstable, will retry")
                        }
                    }
                }
            } catch (e: Exception) {
                lastException = e
                logger.trace("Element not yet available: ${e.message}")
            }

            Thread.sleep(config.stabilityCheckIntervalMs)
        }

        val waitTime = System.currentTimeMillis() - startTime
        return StabilityResult.ElementGone(
            "Element not found or not stable within ${timeoutMs}ms: ${lastException?.message}",
            null
        )
    }

    /**
     * Wait for CSS animations to complete on element.
     *
     * @param driver WebDriver instance
     * @param element Element to check
     * @param timeoutMs Maximum time to wait
     * @return true if animations completed, false if timeout
     */
    fun waitForAnimations(
        driver: WebDriver,
        element: WebElement,
        timeoutMs: Long = config.animationTimeoutMs
    ): Boolean {
        if (!config.waitForAnimationsEnabled) {
            return true
        }

        if (driver !is JavascriptExecutor) {
            logger.debug("Driver doesn't support JavaScript, skipping animation wait")
            return true
        }

        val startTime = System.currentTimeMillis()

        try {
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                val isAnimating = isElementAnimating(driver, element)

                if (!isAnimating) {
                    val waitTime = System.currentTimeMillis() - startTime
                    logger.debug("Animations completed after ${waitTime}ms")
                    return true
                }

                Thread.sleep(50)
            }

            logger.warn("Animation wait timeout after ${timeoutMs}ms")
            return false

        } catch (e: Exception) {
            logger.debug("Error checking animation state: ${e.message}")
            // Wait default animation duration as fallback
            Thread.sleep(config.defaultAnimationDurationMs)
            return true
        }
    }

    /**
     * Check if element has active CSS animations or transitions.
     */
    private fun isElementAnimating(driver: JavascriptExecutor, element: WebElement): Boolean {
        return try {
            val script = """
                var elem = arguments[0];
                var style = window.getComputedStyle(elem);

                // Check for running animations
                var animationName = style.getPropertyValue('animation-name');
                var animationDuration = style.getPropertyValue('animation-duration');
                var hasAnimation = animationName && animationName !== 'none' &&
                                   animationDuration && animationDuration !== '0s';

                // Check for running transitions
                var transitionDuration = style.getPropertyValue('transition-duration');
                var hasTransition = transitionDuration && transitionDuration !== '0s';

                // Check for CSS transform changes (indicates animation in progress)
                var transform = style.getPropertyValue('transform');

                return hasAnimation || (hasTransition && transform && transform !== 'none');
            """.trimIndent()

            driver.executeScript(script, element) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Take a snapshot of element state.
     */
    private fun takeSnapshot(element: WebElement): ElementSnapshot {
        return ElementSnapshot(
            location = element.location,
            size = element.size,
            isDisplayed = try { element.isDisplayed } catch (e: Exception) { false },
            isEnabled = try { element.isEnabled } catch (e: Exception) { false }
        )
    }

    /**
     * Wait for element to become interactive (displayed, enabled, stable).
     *
     * @param element Element to wait for
     * @param driver WebDriver for animation checks
     * @param timeoutMs Maximum time to wait
     * @return true if element is interactive, false otherwise
     */
    fun waitForInteractive(
        element: WebElement,
        driver: WebDriver? = null,
        timeoutMs: Long = config.stabilityTimeoutMs
    ): Boolean {
        // First wait for position stability
        val stabilityResult = waitForStability(element, timeoutMs / 2)

        if (stabilityResult !is StabilityResult.Stable) {
            logger.debug("Element not stable: $stabilityResult")
            return false
        }

        // Then wait for animations if driver available
        if (driver != null) {
            val remainingTime = timeoutMs - stabilityResult.waitTimeMs
            if (!waitForAnimations(driver, element, remainingTime)) {
                logger.debug("Animation wait timeout")
                return false
            }
        }

        // Final check: is element actually interactive?
        return try {
            element.isDisplayed && element.isEnabled
        } catch (e: Exception) {
            logger.debug("Element not interactive: ${e.message}")
            false
        }
    }

    /**
     * Wait for element to disappear (for wait-for-gone scenarios).
     *
     * @param findElement Function to find the element
     * @param timeoutMs Maximum time to wait for disappearance
     * @return true if element disappeared, false if still present
     */
    fun waitForDisappearance(
        findElement: () -> WebElement?,
        timeoutMs: Long = config.stabilityTimeoutMs
    ): Boolean {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                val element = findElement()
                if (element == null || !element.isDisplayed) {
                    val waitTime = System.currentTimeMillis() - startTime
                    logger.debug("Element disappeared after ${waitTime}ms")
                    return true
                }
            } catch (e: StaleElementReferenceException) {
                // Element is gone
                val waitTime = System.currentTimeMillis() - startTime
                logger.debug("Element became stale (disappeared) after ${waitTime}ms")
                return true
            } catch (e: Exception) {
                // Element likely gone
                val waitTime = System.currentTimeMillis() - startTime
                logger.debug("Element not accessible (likely gone) after ${waitTime}ms")
                return true
            }

            Thread.sleep(config.stabilityCheckIntervalMs)
        }

        logger.warn("Element did not disappear within ${timeoutMs}ms")
        return false
    }
}
