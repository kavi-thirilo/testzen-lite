package com.testzen.core.stability.registry

import com.testzen.core.model.Platform
import com.testzen.core.stability.SmartScrollStrategy
import org.openqa.selenium.Dimension
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.Point
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.PointerInput
import org.openqa.selenium.interactions.Sequence
import org.openqa.selenium.remote.RemoteWebDriver
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.math.abs

/**
 * Mobile-specific stability adapter for Android and iOS.
 *
 * Uses Appium-compatible APIs and mobile-specific techniques:
 * - Element position/size monitoring for stability
 * - Touch-based scrolling with momentum detection
 * - View hierarchy analysis for loading indicators
 * - Resource ID and accessibility ID based element detection
 *
 * Unlike web, mobile platforms don't support JavaScript DOM manipulation,
 * so this adapter uses alternative techniques:
 * - Element attribute monitoring instead of DOM observers
 * - Touch coordinate tracking instead of scroll position JS
 * - Element count changes instead of content hash
 */
class MobileStabilityAdapter(
    override val supportedPlatform: Platform
) : PlatformStabilityAdapter {

    private val logger = LoggerFactory.getLogger(MobileStabilityAdapter::class.java)

    // Track last known states for change detection
    private var lastElementCount = 0
    private var lastContentHash = 0
    private var lastScrollPosition = Point(0, 0)
    private var domMutated = false

    override fun supportsDriver(driver: WebDriver): Boolean {
        return try {
            val capabilities = (driver as? RemoteWebDriver)?.capabilities
            val platformName = capabilities?.getCapability("platformName")?.toString()?.lowercase() ?: ""
            when (supportedPlatform) {
                Platform.ANDROID -> platformName.contains("android")
                Platform.IOS -> platformName.contains("ios")
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PAGE LOAD DETECTION
    // ═══════════════════════════════════════════════════════════════════════════════

    override fun isPageReady(driver: WebDriver): Boolean {
        // For mobile, we check if the screen has stabilized
        return try {
            // Check if there are any visible loading indicators
            val hasLoading = hasLoadingIndicators(driver, getDefaultLoadingSelectors())

            // Check if element count is stable (simple heuristic)
            val currentCount = getVisibleElementCount(driver)
            val countStable = abs(currentCount - lastElementCount) < 5
            lastElementCount = currentCount

            !hasLoading && countStable
        } catch (e: Exception) {
            logger.trace("Error checking page ready: ${e.message}")
            true // Assume ready on error to not block
        }
    }

    override fun waitForPageReady(driver: WebDriver, timeoutMs: Long): Boolean {
        val startTime = System.currentTimeMillis()
        var stableCount = 0
        val requiredStableChecks = 3

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (isPageReady(driver)) {
                stableCount++
                if (stableCount >= requiredStableChecks) {
                    logger.debug("Mobile page ready after ${System.currentTimeMillis() - startTime}ms")
                    return true
                }
            } else {
                stableCount = 0
            }
            Thread.sleep(100)
        }

        logger.debug("Mobile page ready timeout after ${timeoutMs}ms")
        return false
    }

    override fun isNetworkIdle(driver: WebDriver): Boolean {
        // For mobile, we can't directly monitor network
        // Instead, check if visible loading indicators are gone
        return !hasLoadingIndicators(driver, getDefaultLoadingSelectors())
    }

    override fun getPendingNetworkRequests(driver: WebDriver): Int {
        // Not directly available on mobile
        // Return 0 if no loading indicators, 1 otherwise
        return if (hasLoadingIndicators(driver, getDefaultLoadingSelectors())) 1 else 0
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // DOM/VIEW STABILITY
    // ═══════════════════════════════════════════════════════════════════════════════

    override fun isDomStable(driver: WebDriver): Boolean {
        return !hasDomMutations(driver)
    }

    override fun hasDomMutations(driver: WebDriver): Boolean {
        // For mobile, we track view hierarchy changes via element count
        val currentCount = getVisibleElementCount(driver)
        val mutated = abs(currentCount - lastElementCount) > 3
        lastElementCount = currentCount
        return mutated || domMutated
    }

    override fun resetDomMutationTracking(driver: WebDriver) {
        domMutated = false
        lastElementCount = getVisibleElementCount(driver)
    }

    override fun injectMonitors(driver: WebDriver) {
        // Mobile doesn't use injected monitors
        // Initialize tracking state instead
        lastElementCount = getVisibleElementCount(driver)
        lastContentHash = getContentHash(driver)
        domMutated = false
        logger.debug("Initialized mobile stability tracking")
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // SCROLL OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    override fun getScrollPosition(driver: WebDriver, direction: SmartScrollStrategy.ScrollDirection): Int {
        // For mobile, we can't get absolute scroll position
        // Instead, track relative position via content hash changes
        return when (direction) {
            SmartScrollStrategy.ScrollDirection.UP,
            SmartScrollStrategy.ScrollDirection.DOWN -> lastScrollPosition.y
            SmartScrollStrategy.ScrollDirection.LEFT,
            SmartScrollStrategy.ScrollDirection.RIGHT -> lastScrollPosition.x
        }
    }

    override fun getTotalContentSize(driver: WebDriver, direction: SmartScrollStrategy.ScrollDirection): Int {
        // Not directly available on mobile
        // Return a large value to indicate scrollable content exists
        return Int.MAX_VALUE
    }

    override fun getViewportSize(driver: WebDriver, direction: SmartScrollStrategy.ScrollDirection): Int {
        return try {
            val size = driver.manage().window().size
            when (direction) {
                SmartScrollStrategy.ScrollDirection.UP,
                SmartScrollStrategy.ScrollDirection.DOWN -> size.height
                SmartScrollStrategy.ScrollDirection.LEFT,
                SmartScrollStrategy.ScrollDirection.RIGHT -> size.width
            }
        } catch (e: Exception) {
            0
        }
    }

    override fun getContentHash(driver: WebDriver): Int {
        return try {
            // Build hash from visible element texts
            val elements = findAllVisibleElements(driver)
            val contentBuilder = StringBuilder()

            elements.take(50).forEach { element ->
                try {
                    val text = element.text ?: ""
                    val tag = element.tagName ?: "unknown"
                    contentBuilder.append("$tag:${text.take(30)}|")
                } catch (e: Exception) {
                    // Element may have become stale
                }
            }

            val hash = contentBuilder.toString().hashCode()
            lastContentHash = hash
            hash
        } catch (e: Exception) {
            lastContentHash
        }
    }

    override fun getVisibleElementCount(driver: WebDriver): Int {
        return try {
            findAllVisibleElements(driver).size
        } catch (e: Exception) {
            0
        }
    }

    override fun scrollElementIntoView(driver: WebDriver, element: WebElement) {
        try {
            val elementLocation = element.location
            val elementSize = element.size
            val screenSize = driver.manage().window().size

            val elementCenterY = elementLocation.y + elementSize.height / 2
            val screenCenterY = screenSize.height / 2

            val scrollAmount = elementCenterY - screenCenterY

            if (abs(scrollAmount) > 100) {
                val direction = if (scrollAmount > 0) {
                    SmartScrollStrategy.ScrollDirection.DOWN
                } else {
                    SmartScrollStrategy.ScrollDirection.UP
                }

                // Perform a small scroll to center the element
                performMobileScroll(driver, direction, 0.3)
            }
        } catch (e: Exception) {
            logger.debug("Could not scroll element into view: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ANIMATION DETECTION
    // ═══════════════════════════════════════════════════════════════════════════════

    override fun hasActiveAnimations(driver: WebDriver): Boolean {
        // For mobile, detect animations via element position changes
        return try {
            val elements = findAllVisibleElements(driver).take(10)
            val positions1 = elements.map { safeGetElementPosition(it) }

            Thread.sleep(100)

            val positions2 = elements.map { safeGetElementPosition(it) }

            // Check if any positions changed
            positions1.zip(positions2).any { (p1, p2) ->
                p1 != null && p2 != null && (abs(p1.x - p2.x) > 2 || abs(p1.y - p2.y) > 2)
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun isElementAnimating(driver: WebDriver, element: WebElement): Boolean {
        return try {
            val pos1 = safeGetElementPosition(element)
            val size1 = safeGetElementSize(element)

            Thread.sleep(100)

            val pos2 = safeGetElementPosition(element)
            val size2 = safeGetElementSize(element)

            if (pos1 == null || pos2 == null || size1 == null || size2 == null) {
                return false
            }

            val positionChanged = abs(pos1.x - pos2.x) > 2 || abs(pos1.y - pos2.y) > 2
            val sizeChanged = abs(size1.width - size2.width) > 2 || abs(size1.height - size2.height) > 2

            positionChanged || sizeChanged
        } catch (e: Exception) {
            false
        }
    }

    override fun waitForElementAnimations(driver: WebDriver, element: WebElement, timeoutMs: Long): Boolean {
        val startTime = System.currentTimeMillis()
        var stableCount = 0
        val requiredStable = 3

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (!isElementAnimating(driver, element)) {
                stableCount++
                if (stableCount >= requiredStable) {
                    return true
                }
            } else {
                stableCount = 0
            }
            Thread.sleep(50)
        }

        return false
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ASYNC/FRAMEWORK DETECTION
    // ═══════════════════════════════════════════════════════════════════════════════

    override fun isAsyncComplete(driver: WebDriver): Boolean {
        // For mobile, check loading indicators and view stability
        return !hasLoadingIndicators(driver, getDefaultLoadingSelectors()) && isDomStable(driver)
    }

    override fun hasLoadingIndicators(driver: WebDriver, selectors: List<String>): Boolean {
        return try {
            selectors.any { selector ->
                try {
                    val elements = findElementsBySelector(driver, selector)
                    elements.any { it.isDisplayed }
                } catch (e: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════════

    override fun executeScript(driver: WebDriver, script: String, vararg args: Any): Any? {
        // Mobile may support limited JavaScript in hybrid apps
        val js = driver as? JavascriptExecutor
        return if (js != null) {
            try {
                js.executeScript(script, *args)
            } catch (e: Exception) {
                logger.trace("Script execution not supported or failed: ${e.message}")
                null
            }
        } else {
            null
        }
    }

    override fun supportsScriptExecution(driver: WebDriver): Boolean {
        // Limited support in hybrid apps
        return driver is JavascriptExecutor
    }

    override fun getPlatformInfo(driver: WebDriver): Map<String, Any> {
        return try {
            val capabilities = (driver as? RemoteWebDriver)?.capabilities
            val platformName = capabilities?.getCapability("platformName")?.toString() ?: "unknown"
            val platformVersion = capabilities?.getCapability("platformVersion")?.toString() ?: "unknown"
            val deviceName = capabilities?.getCapability("deviceName")?.toString() ?: "unknown"
            val automationName = capabilities?.getCapability("automationName")?.toString() ?: "unknown"

            mapOf(
                "platform" to supportedPlatform.name,
                "platformName" to platformName,
                "platformVersion" to platformVersion,
                "deviceName" to deviceName,
                "automationName" to automationName,
                "scriptSupport" to supportsScriptExecution(driver),
                "visibleElements" to getVisibleElementCount(driver)
            )
        } catch (e: Exception) {
            mapOf(
                "platform" to supportedPlatform.name,
                "error" to (e.message ?: "unknown")
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════════

    private fun findAllVisibleElements(driver: WebDriver): List<WebElement> {
        return try {
            // Use a broad selector to find all interactive elements
            val selector = when (supportedPlatform) {
                Platform.ANDROID -> "//*[@clickable='true' or @text or @content-desc]"
                Platform.IOS -> "//*[@visible='true']"
                else -> "//*"
            }

            driver.findElements(org.openqa.selenium.By.xpath(selector))
                .filter { safeIsDisplayed(it) }
                .take(100) // Limit for performance
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun findElementsBySelector(driver: WebDriver, selector: String): List<WebElement> {
        return try {
            when {
                // Resource ID (Android)
                selector.startsWith("id:") -> {
                    val id = selector.removePrefix("id:")
                    driver.findElements(org.openqa.selenium.By.id(id))
                }
                // Accessibility ID (iOS/Android)
                selector.startsWith("accessibility:") -> {
                    val accessId = selector.removePrefix("accessibility:")
                    driver.findElements(org.openqa.selenium.By.xpath("//*[@content-desc='$accessId' or @name='$accessId']"))
                }
                // XPath
                selector.startsWith("//") || selector.startsWith("(/") -> {
                    driver.findElements(org.openqa.selenium.By.xpath(selector))
                }
                // Class name contains
                selector.startsWith("class:") -> {
                    val className = selector.removePrefix("class:")
                    driver.findElements(org.openqa.selenium.By.xpath("//*[contains(@class, '$className')]"))
                }
                // Text contains
                selector.startsWith("text:") -> {
                    val text = selector.removePrefix("text:")
                    driver.findElements(org.openqa.selenium.By.xpath("//*[contains(@text, '$text') or contains(@name, '$text')]"))
                }
                // Default: try as resource ID
                else -> {
                    driver.findElements(org.openqa.selenium.By.id(selector))
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getDefaultLoadingSelectors(): List<String> {
        return when (supportedPlatform) {
            Platform.ANDROID -> listOf(
                "id:progress",
                "id:loading",
                "id:spinner",
                "class:ProgressBar",
                "class:CircularProgressIndicator",
                "text:Loading",
                "//*[contains(@resource-id, 'progress')]",
                "//*[contains(@resource-id, 'loading')]",
                "//*[contains(@resource-id, 'spinner')]"
            )
            Platform.IOS -> listOf(
                "class:UIActivityIndicatorView",
                "class:UIProgressView",
                "accessibility:loading",
                "accessibility:spinner",
                "//*[contains(@name, 'loading')]",
                "//*[contains(@name, 'spinner')]",
                "//*[contains(@name, 'progress')]"
            )
            else -> emptyList()
        }
    }

    private fun safeGetElementPosition(element: WebElement): Point? {
        return try {
            element.location
        } catch (e: Exception) {
            null
        }
    }

    private fun safeGetElementSize(element: WebElement): Dimension? {
        return try {
            element.size
        } catch (e: Exception) {
            null
        }
    }

    private fun safeIsDisplayed(element: WebElement): Boolean {
        return try {
            element.isDisplayed
        } catch (e: Exception) {
            false
        }
    }

    private fun performMobileScroll(
        driver: WebDriver,
        direction: SmartScrollStrategy.ScrollDirection,
        viewportPercentage: Double
    ) {
        try {
            val screenSize = driver.manage().window().size
            val centerX = screenSize.width / 2
            val centerY = screenSize.height / 2

            val scrollDistance = when (direction) {
                SmartScrollStrategy.ScrollDirection.UP,
                SmartScrollStrategy.ScrollDirection.DOWN ->
                    (screenSize.height * viewportPercentage * 0.7).toInt()
                SmartScrollStrategy.ScrollDirection.LEFT,
                SmartScrollStrategy.ScrollDirection.RIGHT ->
                    (screenSize.width * viewportPercentage * 0.7).toInt()
            }

            val (startX, startY, endX, endY) = when (direction) {
                SmartScrollStrategy.ScrollDirection.DOWN -> listOf(centerX, centerY + scrollDistance / 2, centerX, centerY - scrollDistance / 2)
                SmartScrollStrategy.ScrollDirection.UP -> listOf(centerX, centerY - scrollDistance / 2, centerX, centerY + scrollDistance / 2)
                SmartScrollStrategy.ScrollDirection.LEFT -> listOf(centerX - scrollDistance / 2, centerY, centerX + scrollDistance / 2, centerY)
                SmartScrollStrategy.ScrollDirection.RIGHT -> listOf(centerX + scrollDistance / 2, centerY, centerX - scrollDistance / 2, centerY)
            }

            val finger = PointerInput(PointerInput.Kind.TOUCH, "finger")
            val swipe = Sequence(finger, 1)

            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY))
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(300), PointerInput.Origin.viewport(), endX, endY))
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))

            driver.perform(listOf(swipe))

            // Update tracked scroll position
            when (direction) {
                SmartScrollStrategy.ScrollDirection.DOWN -> lastScrollPosition = Point(lastScrollPosition.x, lastScrollPosition.y + scrollDistance)
                SmartScrollStrategy.ScrollDirection.UP -> lastScrollPosition = Point(lastScrollPosition.x, lastScrollPosition.y - scrollDistance)
                SmartScrollStrategy.ScrollDirection.LEFT -> lastScrollPosition = Point(lastScrollPosition.x - scrollDistance, lastScrollPosition.y)
                SmartScrollStrategy.ScrollDirection.RIGHT -> lastScrollPosition = Point(lastScrollPosition.x + scrollDistance, lastScrollPosition.y)
            }
        } catch (e: Exception) {
            logger.error("Mobile scroll failed: ${e.message}")
        }
    }
}
