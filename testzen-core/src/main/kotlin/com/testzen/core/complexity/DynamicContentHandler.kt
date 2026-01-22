package com.testzen.core.complexity

import kotlinx.serialization.Serializable
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.By
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.support.ui.ExpectedConditions
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Handles dynamic content loading scenarios commonly found in modern web applications.
 *
 * Single Responsibility: Dynamic content and loading state management.
 *
 * Addresses challenges:
 * - Infinite scrolling
 * - Lazy loading images/content
 * - Virtual scrolling (React Virtual, Angular CDK)
 * - Skeleton loaders
 * - Loading spinners
 * - Toast notifications
 * - Modal dialogs
 * - Overlay blockers
 * - Stale element recovery
 * - Content mutation detection
 *
 * Usage:
 * ```kotlin
 * val handler = DynamicContentHandler(driver)
 *
 * // Type-safe loading wait
 * when (val result = handler.waitForLoadingCompleteSafe()) {
 *     is LoadingResult.Complete -> logger.info("Loaded in ${result.duration}ms")
 *     is LoadingResult.Timeout -> logger.warn("Still loading: ${result.pendingIndicators}")
 * }
 *
 * // Handle infinite scroll
 * handler.scrollToLoadMore(maxScrolls = 5)
 *
 * // Wait for lazy images
 * handler.waitForLazyImages()
 * ```
 */
class DynamicContentHandler(
    private val driver: WebDriver,
    private val config: DynamicContentConfig = DynamicContentConfig()
) : IDynamicContentHandler {
    private val logger = LoggerFactory.getLogger(DynamicContentHandler::class.java)
    private val js: JavascriptExecutor = driver as JavascriptExecutor
    private val wait = WebDriverWait(driver, Duration.ofMillis(config.defaultTimeout))
    private val actions = Actions(driver)

    // ═══════════════════════════════════════════════════════════════════════════════
    // LOADING STATE DETECTION
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Wait for page loading to complete (spinners, loaders, etc.)
     */
    fun waitForLoadingComplete(timeout: Long = config.defaultTimeout): Boolean {
        val endTime = System.currentTimeMillis() + timeout

        // Inject loading detector
        injectLoadingDetector()

        while (System.currentTimeMillis() < endTime) {
            if (isLoadingComplete()) {
                return true
            }
            Thread.sleep(config.pollingInterval)
        }

        logger.warn("Loading did not complete within ${timeout}ms")
        return false
    }

    /**
     * Check if loading is complete.
     */
    fun isLoadingComplete(): Boolean {
        // Check common loading indicators
        val loadingSelectors = config.loadingIndicatorSelectors

        for (selector in loadingSelectors) {
            try {
                val elements = driver.findElements(By.cssSelector(selector))
                val visibleLoaders = elements.filter {
                    try {
                        it.isDisplayed
                    } catch (e: Exception) {
                        false
                    }
                }
                if (visibleLoaders.isNotEmpty()) {
                    return false
                }
            } catch (e: Exception) {
                // Selector not found, continue
            }
        }

        // Check skeleton loaders
        if (hasVisibleSkeletons()) {
            return false
        }

        // Check for blocking overlays
        if (hasBlockingOverlay()) {
            return false
        }

        return true
    }

    /**
     * Wait for specific loading indicator to disappear.
     */
    fun waitForLoaderToDisappear(selector: String, timeout: Long = config.defaultTimeout): Boolean {
        return try {
            WebDriverWait(driver, Duration.ofMillis(timeout))
                .until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(selector)))
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun injectLoadingDetector() {
        val script = """
            if (!window.__testzenLoadingDetector) {
                window.__testzenLoadingDetector = {
                    checkSkeletons: function() {
                        const skeletons = document.querySelectorAll('[class*="skeleton"], [class*="shimmer"], [class*="placeholder"]');
                        return Array.from(skeletons).filter(el => {
                            const style = window.getComputedStyle(el);
                            return style.display !== 'none' && style.visibility !== 'hidden';
                        }).length;
                    },
                    checkSpinners: function() {
                        const spinners = document.querySelectorAll('.spinner, .loader, .loading, [class*="spin"]');
                        return Array.from(spinners).filter(el => el.offsetParent !== null).length;
                    }
                };
            }
        """.trimIndent()
        js.executeScript(script)
    }

    private fun hasVisibleSkeletons(): Boolean {
        val script = """
            if (!window.__testzenLoadingDetector) return false;
            return window.__testzenLoadingDetector.checkSkeletons() > 0;
        """.trimIndent()
        return js.executeScript(script) as? Boolean ?: false
    }

    private fun hasBlockingOverlay(): Boolean {
        val script = """
            const overlays = document.querySelectorAll('.overlay, .modal-backdrop, .loading-overlay, [class*="backdrop"]');
            return Array.from(overlays).filter(el => {
                const style = window.getComputedStyle(el);
                return style.display !== 'none' && style.visibility !== 'hidden' && parseFloat(style.opacity) > 0;
            }).length > 0;
        """.trimIndent()
        return js.executeScript(script) as? Boolean ?: false
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // INFINITE SCROLL HANDLING
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Scroll to load more content (infinite scroll).
     *
     * @param maxScrolls Maximum number of scroll attempts
     * @param scrollPauseMs Time to wait after each scroll
     * @param contentSelector Selector for content items to track
     * @return Number of items loaded
     */
    fun scrollToLoadMore(
        maxScrolls: Int = 10,
        scrollPauseMs: Long = 1000,
        contentSelector: String? = null
    ): Int {
        var previousCount = getContentCount(contentSelector)
        var noChangeCount = 0
        var scrollCount = 0

        while (scrollCount < maxScrolls && noChangeCount < 3) {
            // Scroll to bottom
            scrollToBottom()
            Thread.sleep(scrollPauseMs)

            // Wait for loading if spinner appears
            waitForLoadingComplete(5000)

            val currentCount = getContentCount(contentSelector)

            if (currentCount == previousCount) {
                noChangeCount++
            } else {
                noChangeCount = 0
                previousCount = currentCount
            }

            scrollCount++
            logger.debug("Scroll $scrollCount: $currentCount items loaded")
        }

        return previousCount
    }

    /**
     * Scroll to load all content (continues until no more loads).
     */
    fun scrollToLoadAll(
        contentSelector: String,
        maxAttempts: Int = 50,
        scrollPauseMs: Long = 1000
    ): List<WebElement> {
        var previousCount = 0
        var sameCountAttempts = 0

        while (sameCountAttempts < 3 && previousCount < maxAttempts * 10) {
            scrollToBottom()
            Thread.sleep(scrollPauseMs)
            waitForLoadingComplete(5000)

            val elements = driver.findElements(By.cssSelector(contentSelector))
            val currentCount = elements.size

            if (currentCount == previousCount) {
                sameCountAttempts++
            } else {
                sameCountAttempts = 0
                previousCount = currentCount
            }
        }

        return driver.findElements(By.cssSelector(contentSelector))
    }

    /**
     * Scroll to specific element with lazy loading support.
     */
    fun scrollToElement(element: WebElement, waitForStable: Boolean = true): Boolean {
        return try {
            js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element)

            if (waitForStable) {
                Thread.sleep(500)
                waitForElementStable(element)
            }

            true
        } catch (e: Exception) {
            logger.warn("Failed to scroll to element: ${e.message}")
            false
        }
    }

    private fun scrollToBottom() {
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);")
    }

    private fun getContentCount(selector: String?): Int {
        return if (selector != null) {
            driver.findElements(By.cssSelector(selector)).size
        } else {
            val script = "return document.body.scrollHeight;"
            (js.executeScript(script) as? Number)?.toInt() ?: 0
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // LAZY LOADING HANDLING
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Wait for lazy-loaded images to load.
     */
    fun waitForLazyImages(timeout: Long = config.defaultTimeout): Boolean {
        val endTime = System.currentTimeMillis() + timeout

        while (System.currentTimeMillis() < endTime) {
            val unloadedCount = countUnloadedLazyImages()
            if (unloadedCount == 0) {
                return true
            }

            // Scroll to trigger lazy loading
            triggerLazyLoadByScrolling()
            Thread.sleep(config.pollingInterval)
        }

        return false
    }

    /**
     * Force load all lazy images.
     */
    fun forceLoadAllLazyImages(): Int {
        val script = """
            let loadedCount = 0;
            const lazyImages = document.querySelectorAll('img[data-src], img[data-lazy], img[loading="lazy"], img.lazy');

            lazyImages.forEach(img => {
                if (img.dataset.src) {
                    img.src = img.dataset.src;
                    loadedCount++;
                }
                if (img.dataset.lazy) {
                    img.src = img.dataset.lazy;
                    loadedCount++;
                }
                // Trigger native lazy loading
                if (img.loading === 'lazy') {
                    img.loading = 'eager';
                    loadedCount++;
                }
            });

            return loadedCount;
        """.trimIndent()

        return (js.executeScript(script) as? Number)?.toInt() ?: 0
    }

    private fun countUnloadedLazyImages(): Int {
        val script = """
            const lazyImages = document.querySelectorAll('img[data-src], img[data-lazy], img.lazy');
            return Array.from(lazyImages).filter(img => !img.complete || img.naturalHeight === 0).length;
        """.trimIndent()
        return (js.executeScript(script) as? Number)?.toInt() ?: 0
    }

    private fun triggerLazyLoadByScrolling() {
        // Scroll slightly to trigger intersection observers
        js.executeScript("window.scrollBy(0, 100);")
        Thread.sleep(200)
        js.executeScript("window.scrollBy(0, -100);")
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // VIRTUAL SCROLL HANDLING
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Handle virtual scrolling (React Virtual, Angular CDK Virtual Scroll).
     * Virtual scrolling only renders visible items.
     */
    fun findInVirtualList(
        containerSelector: String,
        itemSelector: String,
        matcher: (WebElement) -> Boolean,
        maxScrolls: Int = 50
    ): WebElement? {
        val container = driver.findElement(By.cssSelector(containerSelector))

        for (i in 0 until maxScrolls) {
            val items = driver.findElements(By.cssSelector(itemSelector))

            for (item in items) {
                try {
                    if (matcher(item)) {
                        return item
                    }
                } catch (e: Exception) {
                    // Item might be stale due to virtual rendering
                }
            }

            // Scroll within container
            js.executeScript(
                "arguments[0].scrollTop += arguments[0].clientHeight * 0.8;",
                container
            )
            Thread.sleep(300)

            // Check if we've reached the end
            val atEnd = js.executeScript(
                "return arguments[0].scrollTop + arguments[0].clientHeight >= arguments[0].scrollHeight - 10;",
                container
            ) as? Boolean ?: false

            if (atEnd) break
        }

        return null
    }

    /**
     * Get all items from virtual list by scrolling through it.
     */
    fun collectVirtualListItems(
        containerSelector: String,
        itemSelector: String,
        itemIdentifier: (WebElement) -> String
    ): List<String> {
        val container = driver.findElement(By.cssSelector(containerSelector))
        val collectedIds = mutableSetOf<String>()

        // Scroll to top first
        js.executeScript("arguments[0].scrollTop = 0;", container)
        Thread.sleep(300)

        var atEnd = false
        while (!atEnd) {
            val items = driver.findElements(By.cssSelector(itemSelector))

            for (item in items) {
                try {
                    collectedIds.add(itemIdentifier(item))
                } catch (e: Exception) {
                    // Item stale, skip
                }
            }

            // Scroll down
            js.executeScript(
                "arguments[0].scrollTop += arguments[0].clientHeight * 0.5;",
                container
            )
            Thread.sleep(300)

            atEnd = js.executeScript(
                "return arguments[0].scrollTop + arguments[0].clientHeight >= arguments[0].scrollHeight - 10;",
                container
            ) as? Boolean ?: true
        }

        return collectedIds.toList()
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // MODAL & OVERLAY HANDLING
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Wait for modal dialog and optionally close it.
     */
    fun waitForModal(
        modalSelector: String = config.modalSelectors.first(),
        timeout: Long = config.defaultTimeout
    ): WebElement? {
        for (selector in config.modalSelectors) {
            try {
                val modal = WebDriverWait(driver, Duration.ofMillis(timeout))
                    .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(selector)))
                return modal
            } catch (e: Exception) {
                // Try next selector
            }
        }
        return null
    }

    /**
     * Close modal if present.
     */
    fun closeModalIfPresent(): Boolean {
        for (closeSelector in config.modalCloseSelectors) {
            try {
                val closeButton = driver.findElement(By.cssSelector(closeSelector))
                if (closeButton.isDisplayed) {
                    closeButton.click()
                    Thread.sleep(500)
                    return true
                }
            } catch (e: Exception) {
                // Try next selector
            }
        }

        // Try pressing Escape
        try {
            actions.sendKeys(org.openqa.selenium.Keys.ESCAPE).perform()
            Thread.sleep(500)

            // Verify modal closed
            return !hasVisibleModal()
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Check if modal is visible.
     */
    fun hasVisibleModal(): Boolean {
        for (selector in config.modalSelectors) {
            try {
                val modal = driver.findElement(By.cssSelector(selector))
                if (modal.isDisplayed) {
                    return true
                }
            } catch (e: Exception) {
                // Not found
            }
        }
        return false
    }

    /**
     * Wait for overlay to disappear.
     */
    fun waitForOverlayToDisappear(timeout: Long = config.defaultTimeout): Boolean {
        for (selector in config.overlaySelectors) {
            try {
                WebDriverWait(driver, Duration.ofMillis(timeout))
                    .until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(selector)))
            } catch (e: Exception) {
                // Overlay might not exist, which is fine
            }
        }
        return !hasBlockingOverlay()
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // TOAST/NOTIFICATION HANDLING
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Wait for toast notification.
     */
    fun waitForToast(timeout: Long = 10000): WebElement? {
        for (selector in config.toastSelectors) {
            try {
                return WebDriverWait(driver, Duration.ofMillis(timeout))
                    .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(selector)))
            } catch (e: Exception) {
                // Try next selector
            }
        }
        return null
    }

    /**
     * Get toast message text.
     */
    fun getToastMessage(): String? {
        val toast = waitForToast(5000)
        return toast?.text
    }

    /**
     * Wait for toast to disappear.
     */
    fun waitForToastToDisappear(timeout: Long = 10000): Boolean {
        for (selector in config.toastSelectors) {
            try {
                WebDriverWait(driver, Duration.ofMillis(timeout))
                    .until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(selector)))
                return true
            } catch (e: Exception) {
                // Continue
            }
        }
        return true
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ELEMENT STABILITY
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Wait for element to be stable (position not changing).
     */
    fun waitForElementStable(
        element: WebElement,
        timeout: Long = 5000,
        stabilityThresholdMs: Long = 500
    ): Boolean {
        var lastPosition: Pair<Int, Int>? = null
        var stableSince: Long = 0
        val endTime = System.currentTimeMillis() + timeout

        while (System.currentTimeMillis() < endTime) {
            try {
                val location = element.location
                val currentPosition = location.x to location.y

                if (lastPosition == currentPosition) {
                    if (stableSince == 0L) {
                        stableSince = System.currentTimeMillis()
                    } else if (System.currentTimeMillis() - stableSince >= stabilityThresholdMs) {
                        return true
                    }
                } else {
                    lastPosition = currentPosition
                    stableSince = 0
                }
            } catch (e: Exception) {
                return false
            }

            Thread.sleep(50)
        }

        return false
    }

    /**
     * Retry action on stale element.
     */
    fun <T> retryOnStale(
        locator: By,
        maxRetries: Int = 3,
        action: (WebElement) -> T
    ): T? {
        var lastException: Exception? = null

        for (i in 0 until maxRetries) {
            try {
                val element = driver.findElement(locator)
                return action(element)
            } catch (e: org.openqa.selenium.StaleElementReferenceException) {
                lastException = e
                logger.debug("Stale element, retrying... (${i + 1}/$maxRetries)")
                Thread.sleep(500)
            } catch (e: Exception) {
                lastException = e
                break
            }
        }

        logger.warn("Action failed after $maxRetries retries: ${lastException?.message}")
        return null
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CONTENT MUTATION DETECTION
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Wait for content to stop changing.
     */
    fun waitForContentStable(
        selector: String,
        timeout: Long = config.defaultTimeout,
        stabilityTime: Long = 1000
    ): Boolean {
        var lastContent: String? = null
        var stableSince: Long = 0
        val endTime = System.currentTimeMillis() + timeout

        while (System.currentTimeMillis() < endTime) {
            try {
                val element = driver.findElement(By.cssSelector(selector))
                val currentContent = element.text + element.getAttribute("innerHTML").hashCode()

                if (lastContent == currentContent) {
                    if (stableSince == 0L) {
                        stableSince = System.currentTimeMillis()
                    } else if (System.currentTimeMillis() - stableSince >= stabilityTime) {
                        return true
                    }
                } else {
                    lastContent = currentContent
                    stableSince = 0
                }
            } catch (e: Exception) {
                lastContent = null
                stableSince = 0
            }

            Thread.sleep(100)
        }

        return false
    }

    /**
     * Setup mutation observer and wait for mutations to complete.
     */
    fun waitForMutationsComplete(
        targetSelector: String,
        timeout: Long = config.defaultTimeout
    ): Boolean {
        val script = """
            return new Promise((resolve) => {
                const target = document.querySelector(arguments[0]);
                if (!target) {
                    resolve(true);
                    return;
                }

                let timeoutId;
                const observer = new MutationObserver(() => {
                    clearTimeout(timeoutId);
                    timeoutId = setTimeout(() => {
                        observer.disconnect();
                        resolve(true);
                    }, 500);
                });

                observer.observe(target, {
                    childList: true,
                    subtree: true,
                    attributes: true
                });

                // Initial timeout if no mutations
                timeoutId = setTimeout(() => {
                    observer.disconnect();
                    resolve(true);
                }, 1000);
            });
        """.trimIndent()

        return try {
            val asyncScript = "var callback = arguments[arguments.length - 1]; $script.then(callback);"
            js.executeAsyncScript(asyncScript, targetSelector) as? Boolean ?: true
        } catch (e: Exception) {
            true
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SEALED RESULT CLASSES
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Result type for loading state operations.
 */
sealed class LoadingResult {
    data class Complete(
        val duration: Long,
        val checkedIndicators: Int
    ) : LoadingResult()

    data class Timeout(
        val duration: Long,
        val pendingIndicators: List<String>
    ) : LoadingResult()
}

/**
 * Result type for infinite scroll operations.
 */
sealed class InfiniteScrollResult {
    data class Complete(
        val itemsLoaded: Int,
        val scrollsPerformed: Int,
        val reachedEnd: Boolean
    ) : InfiniteScrollResult()

    data class Partial(
        val itemsLoaded: Int,
        val scrollsPerformed: Int,
        val stoppedReason: String
    ) : InfiniteScrollResult()
}

/**
 * Result type for virtual list operations.
 */
sealed class VirtualListResult<T> {
    data class Found<T>(
        val item: T,
        val scrollPosition: Int
    ) : VirtualListResult<T>()

    data class NotFound<T>(
        val scrollsPerformed: Int,
        val itemsChecked: Int
    ) : VirtualListResult<T>()
}

/**
 * Result type for modal operations.
 */
sealed class ModalResult {
    data class Found(
        val element: WebElement,
        val selector: String
    ) : ModalResult()

    data class Closed(
        val closeMethod: String
    ) : ModalResult()

    object NotPresent : ModalResult()

    data class CloseFailure(
        val error: String
    ) : ModalResult()
}

/**
 * Result type for stale element retry operations.
 */
sealed class StaleRetryResult<T> {
    data class Success<T>(
        val value: T,
        val attempts: Int
    ) : StaleRetryResult<T>()

    data class Failure<T>(
        val attempts: Int,
        val lastError: String
    ) : StaleRetryResult<T>()
}

// ═══════════════════════════════════════════════════════════════════════════════
// CONFIGURATION
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Configuration for dynamic content handling.
 */
@Serializable
data class DynamicContentConfig(
    val defaultTimeout: Long = 30000,
    val pollingInterval: Long = 200,

    val loadingIndicatorSelectors: List<String> = listOf(
        ".loading",
        ".loader",
        ".spinner",
        "[class*='loading']",
        "[class*='spinner']",
        ".sk-spinner",
        ".fa-spinner",
        ".MuiCircularProgress-root",
        ".ant-spin",
        ".el-loading-spinner"
    ),

    val modalSelectors: List<String> = listOf(
        ".modal.show",
        ".modal.in",
        "[role='dialog']",
        ".MuiDialog-root",
        ".ant-modal",
        ".el-dialog",
        ".ReactModal__Content"
    ),

    val modalCloseSelectors: List<String> = listOf(
        ".modal .close",
        ".modal .btn-close",
        "[aria-label='Close']",
        ".modal-close",
        ".MuiDialog-root button[aria-label='close']",
        ".ant-modal-close"
    ),

    val overlaySelectors: List<String> = listOf(
        ".modal-backdrop",
        ".overlay",
        ".loading-overlay",
        "[class*='backdrop']",
        ".MuiBackdrop-root"
    ),

    val toastSelectors: List<String> = listOf(
        ".toast",
        ".notification",
        "[role='alert']",
        ".Toastify__toast",
        ".ant-message",
        ".el-message",
        ".MuiSnackbar-root"
    )
) {
    companion object {
        /**
         * Fast configuration for simple SPAs.
         */
        fun fast(): DynamicContentConfig = DynamicContentConfig(
            defaultTimeout = 10000,
            pollingInterval = 100
        )

        /**
         * Robust configuration for complex applications.
         */
        fun robust(): DynamicContentConfig = DynamicContentConfig(
            defaultTimeout = 60000,
            pollingInterval = 300
        )

        /**
         * CI configuration for automated pipelines.
         */
        fun ci(): DynamicContentConfig = DynamicContentConfig(
            defaultTimeout = 45000,
            pollingInterval = 250
        )
    }
}
