package com.testzen.core.stability

import com.testzen.core.stability.registry.PlatformStabilityAdapter
import com.testzen.core.stability.registry.WebStabilityAdapter
import org.openqa.selenium.Dimension
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.Point
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.PointerInput
import org.openqa.selenium.interactions.Sequence
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.math.abs

/**
 * Intelligent scroll strategy with end-of-content detection.
 *
 * Features:
 * - Automatic end-of-scrollable-area detection
 * - Momentum/inertia settling wait
 * - Dynamic content loading wait
 * - Partial visibility detection
 * - Scroll direction optimization
 * - Content hash comparison for change detection
 * - Cross-platform support (Android, iOS, Web)
 */
class SmartScrollStrategy(
    private val config: StabilityConfig = StabilityConfig.default(),
    private val platformAdapter: PlatformStabilityAdapter = WebStabilityAdapter()
) {
    private val logger = LoggerFactory.getLogger(SmartScrollStrategy::class.java)

    /**
     * Scroll direction.
     */
    enum class ScrollDirection {
        UP, DOWN, LEFT, RIGHT
    }

    /**
     * Content snapshot for end-of-scroll detection.
     */
    data class ContentSnapshot(
        val visibleElementCount: Int,
        val contentHash: Int,
        val scrollPosition: Int,
        val viewportSize: Int,
        val totalContentSize: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isEndOfContent(other: ContentSnapshot): Boolean {
            // Content hash unchanged and scroll position unchanged = end of content
            return contentHash == other.contentHash &&
                    abs(scrollPosition - other.scrollPosition) < 10
        }
    }

    /**
     * Result of scroll operation.
     */
    sealed class ScrollResult {
        data class Success(
            val direction: ScrollDirection,
            val scrollCount: Int,
            val reachedEnd: Boolean,
            val contentChanged: Boolean
        ) : ScrollResult()

        data class ElementFound(
            val element: WebElement,
            val scrollCount: Int,
            val direction: ScrollDirection
        ) : ScrollResult()

        data class EndOfContent(
            val direction: ScrollDirection,
            val scrollCount: Int,
            val finalSnapshot: ContentSnapshot?
        ) : ScrollResult()

        data class Failure(
            val reason: String,
            val scrollCount: Int
        ) : ScrollResult()
    }

    /**
     * Scroll to find an element with intelligent end detection.
     *
     * @param driver WebDriver instance
     * @param findElement Function to check if target element is found
     * @param direction Initial scroll direction
     * @param maxAttempts Maximum scroll attempts
     * @return ScrollResult indicating outcome
     */
    fun scrollToFind(
        driver: WebDriver,
        findElement: () -> WebElement?,
        direction: ScrollDirection = ScrollDirection.DOWN,
        maxAttempts: Int = config.maxScrollAttempts
    ): ScrollResult {
        if (!config.smartScrollEnabled) {
            // Fallback to simple scroll
            return simpleScrollToFind(driver, findElement, direction, maxAttempts)
        }

        val snapshots = mutableListOf<ContentSnapshot>()
        var endOfContentCount = 0
        var scrollCount = 0

        logger.debug("Starting smart scroll to find element (direction: $direction, max: $maxAttempts)")

        // First check if element is already visible
        try {
            val element = findElement()
            if (element != null && isElementFullyVisible(driver, element)) {
                logger.debug("Element already visible, no scroll needed")
                return ScrollResult.ElementFound(element, 0, direction)
            }
        } catch (e: Exception) {
            logger.trace("Element not initially visible: ${e.message}")
        }

        while (scrollCount < maxAttempts) {
            // Take content snapshot before scroll
            val beforeSnapshot = takeContentSnapshot(driver, direction)
            snapshots.add(beforeSnapshot)

            // Perform scroll
            performScroll(driver, direction)
            scrollCount++

            // Wait for scroll to settle (momentum)
            waitForScrollSettle(driver)

            // Wait for potential dynamic content loading
            waitForContentLoad(driver)

            // Check if element is now visible
            try {
                val element = findElement()
                if (element != null) {
                    // Wait for element to stabilize after scroll
                    val stabilityWaiter = ElementStabilityWaiter(config)
                    val stabilityResult = stabilityWaiter.waitForStability(element, config.scrollSettleTimeMs * 2)

                    if (stabilityResult is ElementStabilityWaiter.StabilityResult.Stable) {
                        if (isElementFullyVisible(driver, element)) {
                            logger.debug("Element found after $scrollCount scrolls")
                            return ScrollResult.ElementFound(element, scrollCount, direction)
                        } else if (isElementPartiallyVisible(driver, element)) {
                            // Element partially visible, do a small scroll to center it
                            scrollElementIntoView(driver, element)
                            logger.debug("Element found and centered after $scrollCount scrolls")
                            return ScrollResult.ElementFound(element, scrollCount, direction)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.trace("Element not found after scroll $scrollCount: ${e.message}")
            }

            // Take snapshot after scroll
            val afterSnapshot = takeContentSnapshot(driver, direction)

            // Check for end of content
            if (beforeSnapshot.isEndOfContent(afterSnapshot)) {
                endOfContentCount++
                logger.debug("Potential end of content detected ($endOfContentCount/${config.endOfContentSnapshots})")

                if (endOfContentCount >= config.endOfContentSnapshots) {
                    logger.info("End of scrollable content reached after $scrollCount scrolls")
                    return ScrollResult.EndOfContent(direction, scrollCount, afterSnapshot)
                }
            } else {
                endOfContentCount = 0  // Reset if content changed
            }
        }

        logger.warn("Max scroll attempts ($maxAttempts) reached without finding element")
        return ScrollResult.Failure("Max scroll attempts reached", scrollCount)
    }

    /**
     * Scroll in multiple directions to find element.
     *
     * Tries primary direction first, then reverses if end reached.
     */
    fun scrollAllDirectionsToFind(
        driver: WebDriver,
        findElement: () -> WebElement?,
        primaryDirection: ScrollDirection = ScrollDirection.DOWN,
        maxAttempts: Int = config.maxScrollAttempts
    ): ScrollResult {
        // Try primary direction first
        val primaryResult = scrollToFind(driver, findElement, primaryDirection, maxAttempts / 2)

        when (primaryResult) {
            is ScrollResult.ElementFound -> return primaryResult
            is ScrollResult.EndOfContent -> {
                // Try opposite direction
                val oppositeDirection = getOppositeDirection(primaryDirection)
                logger.debug("End of content in $primaryDirection, trying $oppositeDirection")

                // First scroll back to start
                scrollToStart(driver, primaryDirection)

                // Then try opposite direction
                return scrollToFind(driver, findElement, oppositeDirection, maxAttempts / 2)
            }
            else -> return primaryResult
        }
    }

    /**
     * Perform a single scroll operation.
     */
    fun performScroll(
        driver: WebDriver,
        direction: ScrollDirection,
        viewportPercentage: Double = config.scrollViewportPercentage
    ): Boolean {
        return try {
            val screenSize = getScreenSize(driver)
            val centerX = screenSize.width / 2
            val centerY = screenSize.height / 2

            val scrollDistance = when (direction) {
                ScrollDirection.UP, ScrollDirection.DOWN ->
                    (screenSize.height * viewportPercentage * 0.7).toInt()
                ScrollDirection.LEFT, ScrollDirection.RIGHT ->
                    (screenSize.width * viewportPercentage * 0.7).toInt()
            }

            val (startX, startY, endX, endY) = when (direction) {
                ScrollDirection.DOWN -> listOf(centerX, centerY + scrollDistance / 2, centerX, centerY - scrollDistance / 2)
                ScrollDirection.UP -> listOf(centerX, centerY - scrollDistance / 2, centerX, centerY + scrollDistance / 2)
                ScrollDirection.LEFT -> listOf(centerX - scrollDistance / 2, centerY, centerX + scrollDistance / 2, centerY)
                ScrollDirection.RIGHT -> listOf(centerX + scrollDistance / 2, centerY, centerX - scrollDistance / 2, centerY)
            }

            performSwipe(driver, startX, startY, endX, endY, 300)
            true
        } catch (e: Exception) {
            logger.error("Scroll failed: ${e.message}")
            false
        }
    }

    /**
     * Wait for scroll animation/momentum to settle.
     */
    private fun waitForScrollSettle(driver: WebDriver) {
        if (!config.detectScrollMomentum) {
            Thread.sleep(config.scrollSettleTimeMs)
            return
        }

        val startTime = System.currentTimeMillis()
        var lastScrollPosition = getScrollPosition(driver)

        while (System.currentTimeMillis() - startTime < config.scrollMomentumSettleMs) {
            Thread.sleep(50)
            val currentPosition = getScrollPosition(driver)

            if (abs(currentPosition - lastScrollPosition) < 5) {
                // Position stable
                Thread.sleep(config.scrollSettleTimeMs / 2)  // Additional settle time
                return
            }
            lastScrollPosition = currentPosition
        }
    }

    /**
     * Wait for dynamic content to load after scroll.
     */
    private fun waitForContentLoad(driver: WebDriver) {
        if (!config.detectLazyLoading) {
            return
        }

        val startTime = System.currentTimeMillis()
        var lastContentHash = getContentHash(driver)

        while (System.currentTimeMillis() - startTime < config.scrollDynamicLoadTimeoutMs) {
            Thread.sleep(100)
            val currentHash = getContentHash(driver)

            if (currentHash == lastContentHash) {
                // Content stable, check for a bit longer
                Thread.sleep(200)
                if (getContentHash(driver) == currentHash) {
                    return  // Content settled
                }
            }
            lastContentHash = currentHash
        }
    }

    /**
     * Take snapshot of current visible content.
     */
    private fun takeContentSnapshot(driver: WebDriver, direction: ScrollDirection): ContentSnapshot {
        return try {
            val scrollPosition = getScrollPosition(driver, direction)
            val viewportSize = getViewportSize(driver, direction)
            val totalSize = getTotalContentSize(driver, direction)

            ContentSnapshot(
                visibleElementCount = getVisibleElementCount(driver),
                contentHash = getContentHash(driver),
                scrollPosition = scrollPosition,
                viewportSize = viewportSize,
                totalContentSize = totalSize
            )
        } catch (e: Exception) {
            logger.trace("Error taking content snapshot: ${e.message}")
            ContentSnapshot(0, 0, 0, 0, 0)
        }
    }

    /**
     * Check if element is fully visible in viewport.
     */
    fun isElementFullyVisible(driver: WebDriver, element: WebElement): Boolean {
        return try {
            val location = element.location
            val size = element.size
            val screenSize = getScreenSize(driver)

            val fullyVisible = location.x >= 0 &&
                    location.y >= 0 &&
                    location.x + size.width <= screenSize.width &&
                    location.y + size.height <= screenSize.height

            fullyVisible && element.isDisplayed
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if element is partially visible in viewport.
     */
    fun isElementPartiallyVisible(driver: WebDriver, element: WebElement): Boolean {
        return try {
            val location = element.location
            val size = element.size
            val screenSize = getScreenSize(driver)

            val horizontalOverlap = location.x < screenSize.width && location.x + size.width > 0
            val verticalOverlap = location.y < screenSize.height && location.y + size.height > 0

            horizontalOverlap && verticalOverlap && element.isDisplayed
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Scroll element into center of view.
     * Uses platform adapter for cross-platform support.
     */
    fun scrollElementIntoView(driver: WebDriver, element: WebElement) {
        try {
            platformAdapter.scrollElementIntoView(driver, element)
            Thread.sleep(config.scrollSettleTimeMs)
        } catch (e: Exception) {
            logger.debug("Could not center element: ${e.message}")
        }
    }

    /**
     * Scroll back to start of content.
     */
    fun scrollToStart(driver: WebDriver, currentDirection: ScrollDirection) {
        val oppositeDirection = getOppositeDirection(currentDirection)
        var scrollCount = 0
        val maxScrolls = config.maxScrollAttempts * 2

        while (scrollCount < maxScrolls) {
            val beforePosition = getScrollPosition(driver, currentDirection)
            performScroll(driver, oppositeDirection, 0.8)
            waitForScrollSettle(driver)
            val afterPosition = getScrollPosition(driver, currentDirection)

            if (abs(afterPosition - beforePosition) < 10) {
                break  // Reached start
            }
            scrollCount++
        }
    }

    /**
     * Simple scroll without intelligence (fallback).
     */
    private fun simpleScrollToFind(
        driver: WebDriver,
        findElement: () -> WebElement?,
        direction: ScrollDirection,
        maxAttempts: Int
    ): ScrollResult {
        for (i in 1..maxAttempts) {
            performScroll(driver, direction)
            Thread.sleep(config.scrollSettleTimeMs)

            try {
                val element = findElement()
                if (element != null && element.isDisplayed) {
                    return ScrollResult.ElementFound(element, i, direction)
                }
            } catch (e: Exception) {
                // Continue scrolling
            }
        }
        return ScrollResult.Failure("Element not found after $maxAttempts scrolls", maxAttempts)
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════════

    private fun getOppositeDirection(direction: ScrollDirection): ScrollDirection {
        return when (direction) {
            ScrollDirection.UP -> ScrollDirection.DOWN
            ScrollDirection.DOWN -> ScrollDirection.UP
            ScrollDirection.LEFT -> ScrollDirection.RIGHT
            ScrollDirection.RIGHT -> ScrollDirection.LEFT
        }
    }

    private fun getScreenSize(driver: WebDriver): Dimension {
        return driver.manage().window().size
    }

    /**
     * Get scroll position using platform adapter.
     * Works on both web and mobile platforms.
     */
    private fun getScrollPosition(driver: WebDriver, direction: ScrollDirection = ScrollDirection.DOWN): Int {
        return platformAdapter.getScrollPosition(driver, direction)
    }

    /**
     * Get viewport size using platform adapter.
     */
    private fun getViewportSize(driver: WebDriver, direction: ScrollDirection): Int {
        return platformAdapter.getViewportSize(driver, direction)
    }

    /**
     * Get total content size using platform adapter.
     */
    private fun getTotalContentSize(driver: WebDriver, direction: ScrollDirection): Int {
        return platformAdapter.getTotalContentSize(driver, direction)
    }

    /**
     * Get visible element count using platform adapter.
     */
    private fun getVisibleElementCount(driver: WebDriver): Int {
        return platformAdapter.getVisibleElementCount(driver)
    }

    /**
     * Get content hash using platform adapter.
     * Used for detecting content changes during scroll.
     */
    private fun getContentHash(driver: WebDriver): Int {
        return platformAdapter.getContentHash(driver)
    }

    private fun performSwipe(
        driver: WebDriver,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        durationMs: Long
    ): Boolean {
        return try {
            val finger = PointerInput(PointerInput.Kind.TOUCH, "finger")
            val swipe = Sequence(finger, 1)

            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY))
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(durationMs), PointerInput.Origin.viewport(), endX, endY))
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))

            driver.perform(listOf(swipe))
            true
        } catch (e: Exception) {
            logger.error("Swipe failed: ${e.message}")
            false
        }
    }
}
