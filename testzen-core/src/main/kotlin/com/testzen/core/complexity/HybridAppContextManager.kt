package com.testzen.core.complexity

import kotlinx.serialization.Serializable
import io.appium.java_client.AppiumDriver
import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.ios.IOSDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.remote.RemoteWebElement
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Manages context switching between native and web views in hybrid mobile applications.
 *
 * Single Responsibility: Hybrid app context management (native/webview switching).
 *
 * Supports:
 * - Android WebView context switching
 * - iOS UIWebView/WKWebView context switching
 * - Bidirectional context switching (native → web → native)
 * - Context state validation
 * - WebView content injection
 * - Cross-context element handling
 *
 * Common Hybrid App Challenges Addressed:
 * 1. WebView detection and switching
 * 2. Multiple WebView contexts
 * 3. WebView loading state
 * 4. JavaScript execution in WebView
 * 5. Context state management
 * 6. Element reference across contexts
 *
 * Usage:
 * ```kotlin
 * val contextManager = HybridAppContextManager(driver)
 *
 * // Type-safe context switching with results
 * when (val result = contextManager.switchToWebViewSafe()) {
 *     is ContextSwitchResult.Success -> logger.info("Switched to: ${result.context}")
 *     is ContextSwitchResult.Failure -> logger.error("Switch failed: ${result.error}")
 * }
 *
 * // Execute in specific context
 * contextManager.executeInWebView { driver ->
 *     driver.findElement(By.cssSelector(".login-form")).submit()
 * }
 * ```
 */
class HybridAppContextManager(
    private val driver: AppiumDriver,
    private val config: HybridContextConfig = HybridContextConfig()
) : IHybridContextManager {
    private val logger = LoggerFactory.getLogger(HybridAppContextManager::class.java)

    // Context cache
    private var currentContext: String? = null
    private var contextCacheTime: Long = 0
    private var availableContexts: Set<String> = emptySet()
    private var contextCacheExpiry: Long = 0

    // Platform detection
    private val isAndroid: Boolean = driver is AndroidDriver
    private val isIOS: Boolean = driver is IOSDriver

    companion object {
        const val NATIVE_APP = "NATIVE_APP"
        const val WEBVIEW_PREFIX = "WEBVIEW_"
        const val CHROMIUM_WEBVIEW = "CHROMIUM"
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CONTEXT DISCOVERY
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Get all available contexts.
     */
    fun getAvailableContexts(forceRefresh: Boolean = false): Set<String> {
        val now = System.currentTimeMillis()

        if (!forceRefresh && availableContexts.isNotEmpty() && now < contextCacheExpiry) {
            return availableContexts
        }

        return try {
            availableContexts = driver.contextHandles.toSet()
            contextCacheExpiry = now + config.contextCacheTimeout
            logger.debug("Available contexts: $availableContexts")
            availableContexts
        } catch (e: Exception) {
            logger.warn("Failed to get contexts: ${e.message}")
            setOf(NATIVE_APP)
        }
    }

    /**
     * Check if WebView context is available.
     */
    fun hasWebViewContext(): Boolean {
        return getAvailableContexts().any { it.startsWith(WEBVIEW_PREFIX) || it.contains(CHROMIUM_WEBVIEW) }
    }

    /**
     * Get current context.
     */
    fun getCurrentContext(): String {
        val now = System.currentTimeMillis()

        if (currentContext != null && now - contextCacheTime < config.contextCacheTimeout) {
            return currentContext!!
        }

        return try {
            currentContext = driver.context
            contextCacheTime = now
            currentContext!!
        } catch (e: Exception) {
            logger.warn("Failed to get current context: ${e.message}")
            NATIVE_APP
        }
    }

    /**
     * Check if currently in native context.
     */
    fun isInNativeContext(): Boolean = getCurrentContext() == NATIVE_APP

    /**
     * Check if currently in web context.
     */
    fun isInWebContext(): Boolean = !isInNativeContext()

    /**
     * Get all WebView contexts.
     */
    fun getWebViewContexts(): List<String> {
        return getAvailableContexts().filter {
            it.startsWith(WEBVIEW_PREFIX) || it.contains(CHROMIUM_WEBVIEW)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CONTEXT SWITCHING
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Switch to native context.
     */
    fun switchToNative(): Boolean {
        return switchToContext(NATIVE_APP)
    }

    /**
     * Switch to WebView context.
     * Uses first available WebView if multiple exist.
     */
    fun switchToWebView(): Boolean {
        val webviews = getWebViewContexts()
        if (webviews.isEmpty()) {
            logger.warn("No WebView context available")
            return false
        }
        return switchToContext(webviews.first())
    }

    /**
     * Switch to specific WebView by index (0-based).
     */
    fun switchToWebView(index: Int): Boolean {
        val webviews = getWebViewContexts()
        if (index >= webviews.size) {
            logger.warn("WebView index $index out of bounds (available: ${webviews.size})")
            return false
        }
        return switchToContext(webviews[index])
    }

    /**
     * Switch to WebView containing specific URL pattern.
     */
    fun switchToWebViewByUrl(urlPattern: String): Boolean {
        val webviews = getWebViewContexts()

        for (webview in webviews) {
            try {
                driver.context(webview)
                val currentUrl = (driver as? JavascriptExecutor)
                    ?.executeScript("return window.location.href") as? String

                if (currentUrl?.contains(urlPattern, ignoreCase = true) == true) {
                    currentContext = webview
                    contextCacheTime = System.currentTimeMillis()
                    logger.info("Switched to WebView with URL: $currentUrl")
                    return true
                }
            } catch (e: Exception) {
                logger.debug("Failed to check WebView $webview: ${e.message}")
            }
        }

        // Restore to native if not found
        switchToNative()
        logger.warn("No WebView found matching URL pattern: $urlPattern")
        return false
    }

    /**
     * Switch to WebView containing specific page title.
     */
    fun switchToWebViewByTitle(titlePattern: String): Boolean {
        val webviews = getWebViewContexts()

        for (webview in webviews) {
            try {
                driver.context(webview)
                val title = driver.title

                if (title?.contains(titlePattern, ignoreCase = true) == true) {
                    currentContext = webview
                    contextCacheTime = System.currentTimeMillis()
                    logger.info("Switched to WebView with title: $title")
                    return true
                }
            } catch (e: Exception) {
                logger.debug("Failed to check WebView $webview: ${e.message}")
            }
        }

        switchToNative()
        logger.warn("No WebView found matching title pattern: $titlePattern")
        return false
    }

    /**
     * Switch to specific context by name.
     */
    fun switchToContext(contextName: String): Boolean {
        if (getCurrentContext() == contextName) {
            return true
        }

        return try {
            driver.context(contextName)
            currentContext = contextName
            contextCacheTime = System.currentTimeMillis()
            logger.info("Switched to context: $contextName")
            true
        } catch (e: Exception) {
            logger.error("Failed to switch to context $contextName: ${e.message}")
            false
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CONTEXT-AWARE EXECUTION
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Execute action in WebView context and return to original context.
     */
    fun <T> executeInWebView(action: (AppiumDriver) -> T): T? {
        val originalContext = getCurrentContext()

        return try {
            if (!switchToWebView()) {
                logger.warn("Could not switch to WebView")
                return null
            }

            val result = action(driver)

            // Return to original context
            switchToContext(originalContext)
            result
        } catch (e: Exception) {
            logger.error("Error executing in WebView: ${e.message}")
            switchToContext(originalContext)
            null
        }
    }

    /**
     * Execute action in native context and return to original context.
     */
    fun <T> executeInNative(action: (AppiumDriver) -> T): T? {
        val originalContext = getCurrentContext()

        return try {
            if (!switchToNative()) {
                logger.warn("Could not switch to native context")
                return null
            }

            val result = action(driver)

            // Return to original context
            switchToContext(originalContext)
            result
        } catch (e: Exception) {
            logger.error("Error executing in native: ${e.message}")
            switchToContext(originalContext)
            null
        }
    }

    /**
     * Execute action with automatic context detection.
     * Tries native first, then webview if element not found.
     */
    fun <T> executeWithAutoContext(action: (AppiumDriver) -> T): T? {
        // Try in current context first
        try {
            return action(driver)
        } catch (e: Exception) {
            logger.debug("Action failed in current context, trying alternative")
        }

        // Try alternative context
        val currentlyInNative = isInNativeContext()

        return if (currentlyInNative) {
            executeInWebView(action)
        } else {
            executeInNative(action)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // WEBVIEW CONTENT WAITING
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Wait for WebView to become available.
     */
    fun waitForWebView(timeout: Long = config.webViewWaitTimeout): Boolean {
        val endTime = System.currentTimeMillis() + timeout

        while (System.currentTimeMillis() < endTime) {
            if (hasWebViewContext()) {
                return true
            }
            // Refresh context list
            getAvailableContexts(forceRefresh = true)
            Thread.sleep(config.pollingInterval)
        }

        logger.warn("WebView did not become available within ${timeout}ms")
        return false
    }

    /**
     * Wait for WebView to load completely.
     */
    fun waitForWebViewReady(timeout: Long = config.webViewWaitTimeout): Boolean {
        if (!waitForWebView(timeout)) {
            return false
        }

        return executeInWebView { _ ->
            waitForDocumentReady(timeout)
        } ?: false
    }

    /**
     * Wait for document ready state in WebView.
     */
    private fun waitForDocumentReady(timeout: Long): Boolean {
        val endTime = System.currentTimeMillis() + timeout

        while (System.currentTimeMillis() < endTime) {
            try {
                val readyState = (driver as? JavascriptExecutor)
                    ?.executeScript("return document.readyState") as? String

                if (readyState == "complete") {
                    return true
                }
            } catch (e: Exception) {
                logger.debug("Error checking document ready: ${e.message}")
            }
            Thread.sleep(config.pollingInterval)
        }

        return false
    }

    /**
     * Wait for specific element in WebView.
     */
    fun waitForElementInWebView(
        by: By,
        timeout: Long = config.webViewWaitTimeout
    ): WebElement? {
        return executeInWebView { _ ->
            val endTime = System.currentTimeMillis() + timeout

            while (System.currentTimeMillis() < endTime) {
                try {
                    val element = driver.findElement(by)
                    if (element.isDisplayed) {
                        return@executeInWebView element
                    }
                } catch (e: Exception) {
                    // Element not found yet
                }
                Thread.sleep(config.pollingInterval)
            }
            null
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // JAVASCRIPT EXECUTION IN WEBVIEW
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Execute JavaScript in WebView context.
     */
    fun executeJavaScriptInWebView(script: String, vararg args: Any): Any? {
        return executeInWebView { _ ->
            try {
                (driver as? JavascriptExecutor)?.executeScript(script, *args)
            } catch (e: Exception) {
                logger.error("JavaScript execution failed: ${e.message}")
                null
            }
        }
    }

    /**
     * Get WebView page URL.
     */
    fun getWebViewUrl(): String? {
        return executeInWebView { _ ->
            try {
                (driver as? JavascriptExecutor)
                    ?.executeScript("return window.location.href") as? String
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Get WebView page title.
     */
    fun getWebViewTitle(): String? {
        return executeInWebView { _ ->
            try {
                driver.title
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Scroll in WebView.
     */
    fun scrollInWebView(x: Int, y: Int): Boolean {
        return executeInWebView { _ ->
            try {
                (driver as? JavascriptExecutor)
                    ?.executeScript("window.scrollBy($x, $y)")
                true
            } catch (e: Exception) {
                false
            }
        } ?: false
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PLATFORM-SPECIFIC HANDLING
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Android-specific: Enable Chrome debugging for WebView.
     */
    fun enableAndroidWebViewDebugging() {
        if (!isAndroid) return

        executeInNative { _ ->
            try {
                // This requires the app to have WebView.setWebContentsDebuggingEnabled(true)
                logger.info("Android WebView debugging should be enabled in the app")
            } catch (e: Exception) {
                logger.warn("Could not verify WebView debugging: ${e.message}")
            }
        }
    }

    /**
     * iOS-specific: Handle WKWebView.
     */
    fun handleIOSWebView(): Boolean {
        if (!isIOS) return false

        return try {
            // iOS uses WEBVIEW_xxx format for WKWebView
            val webviews = getWebViewContexts()

            if (webviews.isNotEmpty()) {
                // iOS may require additional capabilities:
                // "webviewConnectTimeout": 90000
                // "includeSafariInWebviews": true
                switchToWebView()
            } else {
                logger.warn("No iOS WebView context found. Check capabilities.")
                false
            }
        } catch (e: Exception) {
            logger.error("iOS WebView handling failed: ${e.message}")
            false
        }
    }

    /**
     * Get WebView info for debugging.
     */
    fun getWebViewInfo(): WebViewInfo {
        val contexts = getAvailableContexts()
        val webviews = getWebViewContexts()
        val current = getCurrentContext()

        val webviewDetails = webviews.map { webview ->
            try {
                switchToContext(webview)
                WebViewDetails(
                    contextName = webview,
                    url = (driver as? JavascriptExecutor)
                        ?.executeScript("return window.location.href") as? String,
                    title = driver.title,
                    readyState = (driver as? JavascriptExecutor)
                        ?.executeScript("return document.readyState") as? String
                )
            } catch (e: Exception) {
                WebViewDetails(
                    contextName = webview,
                    url = null,
                    title = null,
                    readyState = null,
                    error = e.message
                )
            }
        }

        // Restore original context
        switchToContext(current)

        return WebViewInfo(
            platform = if (isAndroid) "Android" else if (isIOS) "iOS" else "Unknown",
            currentContext = current,
            availableContexts = contexts.toList(),
            webViewCount = webviews.size,
            webViewDetails = webviewDetails
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CROSS-CONTEXT ELEMENT HANDLING
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Find element that could be in native or webview.
     */
    fun findElementAcrossContexts(
        nativeLocator: By,
        webLocator: By
    ): ContextElement? {
        // Try native first
        try {
            switchToNative()
            val element = driver.findElement(nativeLocator)
            if (element.isDisplayed) {
                return ContextElement(element, NATIVE_APP, nativeLocator)
            }
        } catch (e: Exception) {
            logger.debug("Element not found in native context")
        }

        // Try webview
        val webviews = getWebViewContexts()
        for (webview in webviews) {
            try {
                switchToContext(webview)
                val element = driver.findElement(webLocator)
                if (element.isDisplayed) {
                    return ContextElement(element, webview, webLocator)
                }
            } catch (e: Exception) {
                logger.debug("Element not found in $webview")
            }
        }

        return null
    }

    /**
     * Click element that could be in native or webview.
     */
    fun clickAcrossContexts(
        nativeLocator: By,
        webLocator: By
    ): Boolean {
        val contextElement = findElementAcrossContexts(nativeLocator, webLocator)

        return if (contextElement != null) {
            try {
                contextElement.element.click()
                true
            } catch (e: Exception) {
                logger.error("Failed to click element: ${e.message}")
                false
            }
        } else {
            false
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SEALED RESULT CLASSES
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Result type for context switching operations.
 */
sealed class ContextSwitchResult {
    data class Success(
        val context: String,
        val previousContext: String
    ) : ContextSwitchResult()

    data class Failure(
        val targetContext: String,
        val currentContext: String,
        val error: String
    ) : ContextSwitchResult()
}

/**
 * Result type for cross-context element searches.
 */
sealed class CrossContextSearchResult {
    data class Found(
        val element: WebElement,
        val context: String,
        val locator: By
    ) : CrossContextSearchResult()

    data class NotFound(
        val searchedContexts: List<String>,
        val nativeLocator: By,
        val webLocator: By
    ) : CrossContextSearchResult()
}

/**
 * Result type for WebView execution.
 */
sealed class WebViewExecutionResult<T> {
    data class Success<T>(
        val value: T,
        val executedInContext: String
    ) : WebViewExecutionResult<T>()

    data class Failure<T>(
        val error: String,
        val attemptedContext: String?
    ) : WebViewExecutionResult<T>()
}

// ═══════════════════════════════════════════════════════════════════════════════
// CONFIGURATION & DATA CLASSES
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Configuration for hybrid app context management.
 */
@Serializable
data class HybridContextConfig(
    val contextCacheTimeout: Long = 5000,
    val webViewWaitTimeout: Long = 30000,
    val pollingInterval: Long = 500
) {
    companion object {
        /**
         * Fast configuration for responsive apps.
         */
        fun fast(): HybridContextConfig = HybridContextConfig(
            contextCacheTimeout = 2000,
            webViewWaitTimeout = 15000,
            pollingInterval = 200
        )

        /**
         * Robust configuration for slow-loading WebViews.
         */
        fun robust(): HybridContextConfig = HybridContextConfig(
            contextCacheTimeout = 10000,
            webViewWaitTimeout = 60000,
            pollingInterval = 1000
        )

        /**
         * CI configuration for automated pipelines.
         */
        fun ci(): HybridContextConfig = HybridContextConfig(
            contextCacheTimeout = 5000,
            webViewWaitTimeout = 45000,
            pollingInterval = 500
        )
    }
}

data class WebViewInfo(
    val platform: String,
    val currentContext: String,
    val availableContexts: List<String>,
    val webViewCount: Int,
    val webViewDetails: List<WebViewDetails>
)

data class WebViewDetails(
    val contextName: String,
    val url: String?,
    val title: String?,
    val readyState: String?,
    val error: String? = null
)

data class ContextElement(
    val element: WebElement,
    val context: String,
    val locator: By
)
