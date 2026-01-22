package com.testzen.core.complexity

import org.openqa.selenium.By
import org.openqa.selenium.WebElement

/**
 * Interface for web complexity handling operations.
 *
 * Implementations handle complex web scenarios including Shadow DOM,
 * iframes, and async monitoring.
 */
interface IWebComplexityHandler {

    /**
     * Find element within Shadow DOM.
     */
    fun findInShadowDom(
        hostSelector: String,
        shadowSelector: String,
        nestedHosts: List<String> = emptyList()
    ): WebElement?

    /**
     * Execute action within iframe context.
     */
    fun <T> executeInIframe(
        iframeLocator: IframeLocator,
        action: (org.openqa.selenium.WebDriver) -> T
    ): T?

    /**
     * Wait for WebSocket connections to be idle.
     */
    fun waitForWebSocketIdle(timeout: Long = 30000, idleTime: Long = 500): Boolean

    /**
     * Wait for GraphQL requests to complete.
     */
    fun waitForGraphQLComplete(timeout: Long = 30000): Boolean

    /**
     * Wait for page to be fully ready.
     */
    fun waitForPageFullyReady(timeout: Long = 30000): PageReadyResult
}

/**
 * Interface for hybrid app context management.
 *
 * Implementations handle native/webview context switching
 * for hybrid mobile applications.
 */
interface IHybridContextManager {

    /**
     * Get all available contexts.
     */
    fun getAvailableContexts(forceRefresh: Boolean = false): Set<String>

    /**
     * Check if WebView context is available.
     */
    fun hasWebViewContext(): Boolean

    /**
     * Get current context.
     */
    fun getCurrentContext(): String

    /**
     * Switch to native context.
     */
    fun switchToNative(): Boolean

    /**
     * Switch to WebView context.
     */
    fun switchToWebView(): Boolean

    /**
     * Execute action in WebView context.
     */
    fun <T> executeInWebView(action: (io.appium.java_client.AppiumDriver) -> T): T?

    /**
     * Execute action in native context.
     */
    fun <T> executeInNative(action: (io.appium.java_client.AppiumDriver) -> T): T?

    /**
     * Wait for WebView to be ready.
     */
    fun waitForWebViewReady(timeout: Long = 30000): Boolean
}

/**
 * Interface for dynamic content handling.
 *
 * Implementations handle loading states, infinite scroll,
 * lazy loading, and modal/overlay management.
 */
interface IDynamicContentHandler {

    /**
     * Wait for loading indicators to complete.
     */
    fun waitForLoadingComplete(timeout: Long = 30000): Boolean

    /**
     * Check if loading is complete.
     */
    fun isLoadingComplete(): Boolean

    /**
     * Scroll to load more content (infinite scroll).
     */
    fun scrollToLoadMore(
        maxScrolls: Int = 10,
        scrollPauseMs: Long = 1000,
        contentSelector: String? = null
    ): Int

    /**
     * Wait for lazy images to load.
     */
    fun waitForLazyImages(timeout: Long = 30000): Boolean

    /**
     * Wait for modal dialog.
     */
    fun waitForModal(modalSelector: String, timeout: Long = 30000): WebElement?

    /**
     * Close modal if present.
     */
    fun closeModalIfPresent(): Boolean

    /**
     * Wait for element to be stable.
     */
    fun waitForElementStable(
        element: WebElement,
        timeout: Long = 5000,
        stabilityThresholdMs: Long = 500
    ): Boolean

    /**
     * Retry action on stale element.
     */
    fun <T> retryOnStale(
        locator: By,
        maxRetries: Int = 3,
        action: (WebElement) -> T
    ): T?
}

/**
 * Composite interface for full complexity handling capabilities.
 */
interface IComplexityManager : IWebComplexityHandler, IDynamicContentHandler {
    /**
     * Get hybrid context manager if available.
     */
    fun getHybridContextManager(): IHybridContextManager?
}
