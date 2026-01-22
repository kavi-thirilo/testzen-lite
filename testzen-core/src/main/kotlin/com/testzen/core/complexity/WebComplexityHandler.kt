package com.testzen.core.complexity

import kotlinx.serialization.Serializable
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.By
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.support.ui.ExpectedConditions
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Handles complex web scenarios including Shadow DOM, advanced iframes,
 * and dynamic content loading patterns.
 *
 * Single Responsibility: Complex web scenario handling (Shadow DOM, iframes, async monitoring).
 *
 * Addresses common challenges:
 * - Shadow DOM penetration (Web Components)
 * - Nested and dynamic iframes
 * - Cross-origin iframe handling
 * - WebSocket activity monitoring
 * - Server-Sent Events (SSE) tracking
 * - GraphQL/Apollo request monitoring
 * - Service Worker state
 * - Progressive Web App (PWA) features
 *
 * Usage:
 * ```kotlin
 * val handler = WebComplexityHandler(driver)
 *
 * // Shadow DOM with result handling
 * when (val result = handler.findInShadowDom("host-selector", "shadow-element")) {
 *     is ShadowDomResult.Found -> result.element.click()
 *     is ShadowDomResult.NotFound -> logger.warn("Element not found: ${result.error}")
 * }
 *
 * // iFrames
 * handler.executeInIframe("iframe-id") { driver ->
 *     driver.findElement(By.id("button")).click()
 * }
 *
 * // Dynamic content
 * handler.waitForWebSocketIdle(timeout = 5000)
 * handler.waitForGraphQLComplete()
 * ```
 */
class WebComplexityHandler(
    private val driver: WebDriver,
    private val config: WebComplexityConfig = WebComplexityConfig()
) : IWebComplexityHandler {
    private val logger = LoggerFactory.getLogger(WebComplexityHandler::class.java)
    private val js: JavascriptExecutor = driver as JavascriptExecutor
    private val wait = WebDriverWait(driver, Duration.ofMillis(config.defaultTimeout))

    // ═══════════════════════════════════════════════════════════════════════════════
    // SHADOW DOM HANDLING
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Find an element inside Shadow DOM.
     *
     * @param hostSelector CSS selector for the shadow host element
     * @param shadowSelector CSS selector for element within shadow root
     * @param nestedHosts List of intermediate shadow hosts for deeply nested shadows
     * @return WebElement found in shadow DOM
     */
    fun findInShadowDom(
        hostSelector: String,
        shadowSelector: String,
        nestedHosts: List<String> = emptyList()
    ): WebElement? {
        return try {
            val script = buildShadowDomScript(hostSelector, shadowSelector, nestedHosts)
            js.executeScript(script) as? WebElement
        } catch (e: Exception) {
            logger.warn("Failed to find element in Shadow DOM: ${e.message}")
            null
        }
    }

    /**
     * Find multiple elements inside Shadow DOM.
     */
    @Suppress("UNCHECKED_CAST")
    fun findAllInShadowDom(
        hostSelector: String,
        shadowSelector: String,
        nestedHosts: List<String> = emptyList()
    ): List<WebElement> {
        return try {
            val script = buildShadowDomScriptAll(hostSelector, shadowSelector, nestedHosts)
            (js.executeScript(script) as? List<WebElement>) ?: emptyList()
        } catch (e: Exception) {
            logger.warn("Failed to find elements in Shadow DOM: ${e.message}")
            emptyList()
        }
    }

    /**
     * Click an element inside Shadow DOM.
     */
    fun clickInShadowDom(
        hostSelector: String,
        shadowSelector: String,
        nestedHosts: List<String> = emptyList()
    ): Boolean {
        val element = findInShadowDom(hostSelector, shadowSelector, nestedHosts)
        return if (element != null) {
            try {
                element.click()
                true
            } catch (e: Exception) {
                // Fallback to JavaScript click
                js.executeScript("arguments[0].click();", element)
                true
            }
        } else {
            false
        }
    }

    /**
     * Enter text into an element inside Shadow DOM.
     */
    fun enterTextInShadowDom(
        hostSelector: String,
        shadowSelector: String,
        text: String,
        nestedHosts: List<String> = emptyList()
    ): Boolean {
        val element = findInShadowDom(hostSelector, shadowSelector, nestedHosts)
        return if (element != null) {
            element.clear()
            element.sendKeys(text)
            true
        } else {
            false
        }
    }

    /**
     * Get text from an element inside Shadow DOM.
     */
    fun getTextFromShadowDom(
        hostSelector: String,
        shadowSelector: String,
        nestedHosts: List<String> = emptyList()
    ): String? {
        val element = findInShadowDom(hostSelector, shadowSelector, nestedHosts)
        return element?.text
    }

    /**
     * Wait for an element to be present in Shadow DOM.
     */
    fun waitForShadowElement(
        hostSelector: String,
        shadowSelector: String,
        timeout: Long = config.defaultTimeout,
        nestedHosts: List<String> = emptyList()
    ): WebElement? {
        val endTime = System.currentTimeMillis() + timeout
        while (System.currentTimeMillis() < endTime) {
            val element = findInShadowDom(hostSelector, shadowSelector, nestedHosts)
            if (element != null && element.isDisplayed) {
                return element
            }
            Thread.sleep(config.pollingInterval)
        }
        return null
    }

    private fun buildShadowDomScript(
        hostSelector: String,
        shadowSelector: String,
        nestedHosts: List<String>
    ): String {
        val hostChain = listOf(hostSelector) + nestedHosts
        val shadowTraversal = hostChain.joinToString("") { host ->
            ".querySelector('$host').shadowRoot"
        }
        return "return document$shadowTraversal.querySelector('$shadowSelector');"
    }

    private fun buildShadowDomScriptAll(
        hostSelector: String,
        shadowSelector: String,
        nestedHosts: List<String>
    ): String {
        val hostChain = listOf(hostSelector) + nestedHosts
        val shadowTraversal = hostChain.joinToString("") { host ->
            ".querySelector('$host').shadowRoot"
        }
        return "return Array.from(document$shadowTraversal.querySelectorAll('$shadowSelector'));"
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ADVANCED IFRAME HANDLING
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Execute actions within an iframe context and return to main content.
     */
    fun <T> executeInIframe(
        iframeLocator: IframeLocator,
        action: (WebDriver) -> T
    ): T? {
        return try {
            switchToIframe(iframeLocator)
            val result = action(driver)
            driver.switchTo().defaultContent()
            result
        } catch (e: Exception) {
            logger.error("Failed to execute in iframe: ${e.message}")
            driver.switchTo().defaultContent()
            null
        }
    }

    /**
     * Execute actions in nested iframes.
     */
    fun <T> executeInNestedIframes(
        iframePath: List<IframeLocator>,
        action: (WebDriver) -> T
    ): T? {
        return try {
            // Navigate into nested iframes
            iframePath.forEach { locator ->
                switchToIframe(locator)
            }

            val result = action(driver)

            // Return to main content
            driver.switchTo().defaultContent()
            result
        } catch (e: Exception) {
            logger.error("Failed to execute in nested iframes: ${e.message}")
            driver.switchTo().defaultContent()
            null
        }
    }

    /**
     * Switch to iframe by various locator types.
     */
    fun switchToIframe(locator: IframeLocator) {
        when (locator) {
            is IframeLocator.ById -> {
                wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(locator.id))
            }
            is IframeLocator.ByName -> {
                wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(locator.name))
            }
            is IframeLocator.ByIndex -> {
                wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(locator.index))
            }
            is IframeLocator.ByElement -> {
                val iframe = driver.findElement(locator.by)
                wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(iframe))
            }
            is IframeLocator.ByCssSelector -> {
                val iframe = driver.findElement(By.cssSelector(locator.selector))
                wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(iframe))
            }
        }
    }

    /**
     * Wait for iframe to load its content.
     */
    fun waitForIframeContent(
        locator: IframeLocator,
        contentSelector: String,
        timeout: Long = config.defaultTimeout
    ): Boolean {
        return executeInIframe(locator) { driver ->
            try {
                WebDriverWait(driver, Duration.ofMillis(timeout))
                    .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(contentSelector)))
                true
            } catch (e: TimeoutException) {
                false
            }
        } ?: false
    }

    /**
     * Find element across all iframes on the page.
     */
    fun findElementAcrossIframes(by: By): IframeElementResult? {
        // First try main content
        try {
            driver.switchTo().defaultContent()
            val element = driver.findElement(by)
            if (element.isDisplayed) {
                return IframeElementResult(element, emptyList())
            }
        } catch (e: Exception) {
            // Not found in main content
        }

        // Search in iframes
        return searchIframesRecursively(by, mutableListOf(), 0)
    }

    private fun searchIframesRecursively(
        by: By,
        path: MutableList<Int>,
        depth: Int
    ): IframeElementResult? {
        if (depth > config.maxIframeDepth) return null

        val iframes = driver.findElements(By.tagName("iframe"))
        iframes.forEachIndexed { index, _ ->
            try {
                driver.switchTo().frame(index)
                path.add(index)

                // Try to find element
                try {
                    val element = driver.findElement(by)
                    if (element.isDisplayed) {
                        return IframeElementResult(element, path.toList())
                    }
                } catch (e: Exception) {
                    // Not found, search nested iframes
                }

                // Recursively search nested iframes
                val result = searchIframesRecursively(by, path, depth + 1)
                if (result != null) return result

                path.removeAt(path.lastIndex)
                driver.switchTo().parentFrame()
            } catch (e: Exception) {
                if (path.isNotEmpty()) {
                    path.removeAt(path.lastIndex)
                }
                try {
                    driver.switchTo().parentFrame()
                } catch (e2: Exception) {
                    driver.switchTo().defaultContent()
                }
            }
        }

        return null
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // WEBSOCKET MONITORING
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Inject WebSocket monitor to track active connections.
     */
    fun injectWebSocketMonitor() {
        val script = """
            if (!window.__testzenWsMonitor) {
                window.__testzenWsMonitor = {
                    connections: [],
                    pendingMessages: 0,
                    lastActivity: Date.now()
                };

                const OriginalWebSocket = window.WebSocket;
                window.WebSocket = function(url, protocols) {
                    const ws = protocols ? new OriginalWebSocket(url, protocols) : new OriginalWebSocket(url);
                    const id = window.__testzenWsMonitor.connections.length;

                    window.__testzenWsMonitor.connections.push({
                        id: id,
                        url: url,
                        state: 'connecting',
                        messageCount: 0
                    });

                    ws.addEventListener('open', function() {
                        window.__testzenWsMonitor.connections[id].state = 'open';
                        window.__testzenWsMonitor.lastActivity = Date.now();
                    });

                    ws.addEventListener('message', function() {
                        window.__testzenWsMonitor.connections[id].messageCount++;
                        window.__testzenWsMonitor.pendingMessages++;
                        window.__testzenWsMonitor.lastActivity = Date.now();

                        setTimeout(function() {
                            window.__testzenWsMonitor.pendingMessages--;
                        }, 100);
                    });

                    ws.addEventListener('close', function() {
                        window.__testzenWsMonitor.connections[id].state = 'closed';
                    });

                    ws.addEventListener('error', function() {
                        window.__testzenWsMonitor.connections[id].state = 'error';
                    });

                    return ws;
                };
                window.WebSocket.prototype = OriginalWebSocket.prototype;
            }
        """.trimIndent()
        js.executeScript(script)
    }

    /**
     * Wait for WebSocket connections to be idle.
     */
    fun waitForWebSocketIdle(
        timeout: Long = config.defaultTimeout,
        idleTime: Long = 500
    ): Boolean {
        val script = """
            if (!window.__testzenWsMonitor) return true;
            const now = Date.now();
            const timeSinceLastActivity = now - window.__testzenWsMonitor.lastActivity;
            return window.__testzenWsMonitor.pendingMessages === 0 && timeSinceLastActivity > arguments[0];
        """.trimIndent()

        val endTime = System.currentTimeMillis() + timeout
        while (System.currentTimeMillis() < endTime) {
            val isIdle = js.executeScript(script, idleTime) as? Boolean ?: true
            if (isIdle) return true
            Thread.sleep(config.pollingInterval)
        }
        return false
    }

    /**
     * Get WebSocket connection status.
     */
    @Suppress("UNCHECKED_CAST")
    fun getWebSocketStatus(): WebSocketStatus {
        val script = """
            if (!window.__testzenWsMonitor) {
                return { connections: [], pendingMessages: 0, lastActivity: 0 };
            }
            return {
                connections: window.__testzenWsMonitor.connections,
                pendingMessages: window.__testzenWsMonitor.pendingMessages,
                lastActivity: window.__testzenWsMonitor.lastActivity
            };
        """.trimIndent()

        val result = js.executeScript(script) as? Map<String, Any>
        return WebSocketStatus(
            connections = (result?.get("connections") as? List<Map<String, Any>>)?.map {
                WebSocketConnection(
                    id = (it["id"] as? Number)?.toInt() ?: 0,
                    url = it["url"] as? String ?: "",
                    state = it["state"] as? String ?: "unknown",
                    messageCount = (it["messageCount"] as? Number)?.toInt() ?: 0
                )
            } ?: emptyList(),
            pendingMessages = (result?.get("pendingMessages") as? Number)?.toInt() ?: 0,
            lastActivity = (result?.get("lastActivity") as? Number)?.toLong() ?: 0
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // GRAPHQL/APOLLO MONITORING
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Inject GraphQL request monitor.
     */
    fun injectGraphQLMonitor() {
        val script = """
            if (!window.__testzenGraphQLMonitor) {
                window.__testzenGraphQLMonitor = {
                    pendingRequests: 0,
                    completedRequests: 0,
                    errors: 0,
                    lastActivity: Date.now()
                };

                // Monitor fetch for GraphQL
                const originalFetch = window.fetch;
                window.fetch = function(url, options) {
                    const isGraphQL = (typeof url === 'string' && url.includes('graphql')) ||
                                     (options && options.body && typeof options.body === 'string' &&
                                      (options.body.includes('query') || options.body.includes('mutation')));

                    if (isGraphQL) {
                        window.__testzenGraphQLMonitor.pendingRequests++;
                        window.__testzenGraphQLMonitor.lastActivity = Date.now();

                        return originalFetch.apply(this, arguments)
                            .then(function(response) {
                                window.__testzenGraphQLMonitor.pendingRequests--;
                                window.__testzenGraphQLMonitor.completedRequests++;
                                window.__testzenGraphQLMonitor.lastActivity = Date.now();
                                return response;
                            })
                            .catch(function(error) {
                                window.__testzenGraphQLMonitor.pendingRequests--;
                                window.__testzenGraphQLMonitor.errors++;
                                throw error;
                            });
                    }
                    return originalFetch.apply(this, arguments);
                };

                // Monitor Apollo Client if present
                if (window.__APOLLO_CLIENT__) {
                    const client = window.__APOLLO_CLIENT__;
                    const originalQuery = client.query;
                    const originalMutate = client.mutate;

                    client.query = function() {
                        window.__testzenGraphQLMonitor.pendingRequests++;
                        return originalQuery.apply(this, arguments)
                            .finally(function() {
                                window.__testzenGraphQLMonitor.pendingRequests--;
                            });
                    };

                    client.mutate = function() {
                        window.__testzenGraphQLMonitor.pendingRequests++;
                        return originalMutate.apply(this, arguments)
                            .finally(function() {
                                window.__testzenGraphQLMonitor.pendingRequests--;
                            });
                    };
                }
            }
        """.trimIndent()
        js.executeScript(script)
    }

    /**
     * Wait for all GraphQL requests to complete.
     */
    fun waitForGraphQLComplete(timeout: Long = config.defaultTimeout): Boolean {
        val script = """
            if (!window.__testzenGraphQLMonitor) return true;
            return window.__testzenGraphQLMonitor.pendingRequests === 0;
        """.trimIndent()

        val endTime = System.currentTimeMillis() + timeout
        while (System.currentTimeMillis() < endTime) {
            val isComplete = js.executeScript(script) as? Boolean ?: true
            if (isComplete) return true
            Thread.sleep(config.pollingInterval)
        }
        return false
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // SERVER-SENT EVENTS (SSE) MONITORING
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Inject SSE (Server-Sent Events) monitor.
     */
    fun injectSSEMonitor() {
        val script = """
            if (!window.__testzenSSEMonitor) {
                window.__testzenSSEMonitor = {
                    connections: [],
                    pendingEvents: 0,
                    lastEvent: Date.now()
                };

                const OriginalEventSource = window.EventSource;
                window.EventSource = function(url, config) {
                    const es = new OriginalEventSource(url, config);
                    const id = window.__testzenSSEMonitor.connections.length;

                    window.__testzenSSEMonitor.connections.push({
                        id: id,
                        url: url,
                        state: 'connecting'
                    });

                    es.addEventListener('open', function() {
                        window.__testzenSSEMonitor.connections[id].state = 'open';
                    });

                    es.addEventListener('message', function() {
                        window.__testzenSSEMonitor.pendingEvents++;
                        window.__testzenSSEMonitor.lastEvent = Date.now();
                        setTimeout(function() {
                            window.__testzenSSEMonitor.pendingEvents--;
                        }, 100);
                    });

                    es.addEventListener('error', function() {
                        window.__testzenSSEMonitor.connections[id].state = 'error';
                    });

                    return es;
                };
            }
        """.trimIndent()
        js.executeScript(script)
    }

    /**
     * Wait for SSE to be idle.
     */
    fun waitForSSEIdle(timeout: Long = config.defaultTimeout, idleTime: Long = 500): Boolean {
        val script = """
            if (!window.__testzenSSEMonitor) return true;
            const timeSinceLastEvent = Date.now() - window.__testzenSSEMonitor.lastEvent;
            return window.__testzenSSEMonitor.pendingEvents === 0 && timeSinceLastEvent > arguments[0];
        """.trimIndent()

        val endTime = System.currentTimeMillis() + timeout
        while (System.currentTimeMillis() < endTime) {
            val isIdle = js.executeScript(script, idleTime) as? Boolean ?: true
            if (isIdle) return true
            Thread.sleep(config.pollingInterval)
        }
        return false
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // SERVICE WORKER & PWA
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Check if Service Worker is active.
     */
    fun isServiceWorkerActive(): Boolean {
        val script = """
            if (!('serviceWorker' in navigator)) return false;
            return navigator.serviceWorker.controller !== null;
        """.trimIndent()
        return js.executeScript(script) as? Boolean ?: false
    }

    /**
     * Wait for Service Worker to be ready.
     */
    fun waitForServiceWorkerReady(timeout: Long = config.defaultTimeout): Boolean {
        val script = """
            return new Promise(function(resolve) {
                if (!('serviceWorker' in navigator)) {
                    resolve(false);
                    return;
                }
                navigator.serviceWorker.ready.then(function() {
                    resolve(true);
                }).catch(function() {
                    resolve(false);
                });
            });
        """.trimIndent()

        return try {
            val asyncScript = "var callback = arguments[arguments.length - 1]; $script.then(callback);"
            js.executeAsyncScript(asyncScript) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check PWA install state.
     */
    fun getPWAInstallState(): PWAState {
        val script = """
            const state = {
                isStandalone: window.matchMedia('(display-mode: standalone)').matches,
                hasServiceWorker: 'serviceWorker' in navigator,
                isServiceWorkerActive: navigator.serviceWorker && navigator.serviceWorker.controller !== null,
                isOnline: navigator.onLine
            };
            return state;
        """.trimIndent()

        @Suppress("UNCHECKED_CAST")
        val result = js.executeScript(script) as? Map<String, Boolean>
        return PWAState(
            isStandalone = result?.get("isStandalone") ?: false,
            hasServiceWorker = result?.get("hasServiceWorker") ?: false,
            isServiceWorkerActive = result?.get("isServiceWorkerActive") ?: false,
            isOnline = result?.get("isOnline") ?: true
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // COMPREHENSIVE PAGE READY CHECK
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Wait for page to be fully ready (all async operations complete).
     */
    fun waitForPageFullyReady(timeout: Long = config.defaultTimeout): PageReadyResult {
        injectAllMonitors()

        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeout

        var documentReady = false
        var ajaxComplete = false
        var webSocketIdle = false
        var graphQLComplete = false
        var animationsComplete = false

        while (System.currentTimeMillis() < endTime) {
            // Check document ready state
            documentReady = js.executeScript("return document.readyState") == "complete"

            // Check AJAX/Fetch
            ajaxComplete = checkAjaxComplete()

            // Check WebSocket
            webSocketIdle = checkWebSocketIdle()

            // Check GraphQL
            graphQLComplete = checkGraphQLComplete()

            // Check animations
            animationsComplete = checkAnimationsComplete()

            if (documentReady && ajaxComplete && webSocketIdle && graphQLComplete && animationsComplete) {
                return PageReadyResult(
                    isReady = true,
                    duration = System.currentTimeMillis() - startTime,
                    documentReady = true,
                    ajaxComplete = true,
                    webSocketIdle = true,
                    graphQLComplete = true,
                    animationsComplete = true
                )
            }

            Thread.sleep(config.pollingInterval)
        }

        return PageReadyResult(
            isReady = false,
            duration = timeout,
            documentReady = documentReady,
            ajaxComplete = ajaxComplete,
            webSocketIdle = webSocketIdle,
            graphQLComplete = graphQLComplete,
            animationsComplete = animationsComplete
        )
    }

    private fun injectAllMonitors() {
        try {
            injectWebSocketMonitor()
            injectGraphQLMonitor()
            injectSSEMonitor()
        } catch (e: Exception) {
            logger.warn("Failed to inject some monitors: ${e.message}")
        }
    }

    private fun checkAjaxComplete(): Boolean {
        val script = """
            // jQuery
            if (window.jQuery && window.jQuery.active > 0) return false;
            // Angular
            if (window.angular) {
                var injector = window.angular.element(document.body).injector();
                if (injector) {
                    var http = injector.get('${'$'}http');
                    if (http && http.pendingRequests && http.pendingRequests.length > 0) return false;
                }
            }
            // Fetch/XHR monitor
            if (window.__testzenNetworkMonitor && window.__testzenNetworkMonitor.pending > 0) return false;
            return true;
        """.trimIndent()
        return js.executeScript(script) as? Boolean ?: true
    }

    private fun checkWebSocketIdle(): Boolean {
        val script = """
            if (!window.__testzenWsMonitor) return true;
            return window.__testzenWsMonitor.pendingMessages === 0;
        """.trimIndent()
        return js.executeScript(script) as? Boolean ?: true
    }

    private fun checkGraphQLComplete(): Boolean {
        val script = """
            if (!window.__testzenGraphQLMonitor) return true;
            return window.__testzenGraphQLMonitor.pendingRequests === 0;
        """.trimIndent()
        return js.executeScript(script) as? Boolean ?: true
    }

    private fun checkAnimationsComplete(): Boolean {
        val script = """
            const animations = document.getAnimations ? document.getAnimations() : [];
            return animations.filter(a => a.playState === 'running').length === 0;
        """.trimIndent()
        return js.executeScript(script) as? Boolean ?: true
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SEALED RESULT CLASSES
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Result type for Shadow DOM element searches.
 */
sealed class ShadowDomResult {
    data class Found(
        val element: WebElement,
        val hostSelector: String,
        val shadowSelector: String
    ) : ShadowDomResult()

    data class NotFound(
        val hostSelector: String,
        val shadowSelector: String,
        val error: String? = null
    ) : ShadowDomResult()
}

/**
 * Result type for iframe operations.
 */
sealed class IframeResult<T> {
    data class Success<T>(
        val value: T,
        val iframePath: List<IframeLocator>
    ) : IframeResult<T>()

    data class Failure<T>(
        val error: String,
        val iframePath: List<IframeLocator>
    ) : IframeResult<T>()
}

/**
 * Result type for async monitoring operations.
 */
sealed class AsyncMonitorResult {
    data class Idle(
        val waitDuration: Long,
        val pendingAtEnd: Int = 0
    ) : AsyncMonitorResult()

    data class Timeout(
        val waitDuration: Long,
        val pendingAtTimeout: Int
    ) : AsyncMonitorResult()

    data class Error(
        val message: String
    ) : AsyncMonitorResult()
}

// ═══════════════════════════════════════════════════════════════════════════════
// CONFIGURATION & DATA CLASSES
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Configuration for web complexity handling.
 */
@Serializable
data class WebComplexityConfig(
    val defaultTimeout: Long = 30000,
    val pollingInterval: Long = 100,
    val maxIframeDepth: Int = 5
) {
    companion object {
        /**
         * Fast configuration for quick operations.
         */
        fun fast(): WebComplexityConfig = WebComplexityConfig(
            defaultTimeout = 10000,
            pollingInterval = 50,
            maxIframeDepth = 3
        )

        /**
         * Robust configuration for complex scenarios.
         */
        fun robust(): WebComplexityConfig = WebComplexityConfig(
            defaultTimeout = 60000,
            pollingInterval = 200,
            maxIframeDepth = 10
        )

        /**
         * CI configuration for consistent automated runs.
         */
        fun ci(): WebComplexityConfig = WebComplexityConfig(
            defaultTimeout = 45000,
            pollingInterval = 150,
            maxIframeDepth = 5
        )
    }
}

sealed class IframeLocator {
    data class ById(val id: String) : IframeLocator()
    data class ByName(val name: String) : IframeLocator()
    data class ByIndex(val index: Int) : IframeLocator()
    data class ByElement(val by: By) : IframeLocator()
    data class ByCssSelector(val selector: String) : IframeLocator()
}

data class IframeElementResult(
    val element: WebElement,
    val iframePath: List<Int>
)

data class WebSocketStatus(
    val connections: List<WebSocketConnection>,
    val pendingMessages: Int,
    val lastActivity: Long
)

data class WebSocketConnection(
    val id: Int,
    val url: String,
    val state: String,
    val messageCount: Int
)

data class PWAState(
    val isStandalone: Boolean,
    val hasServiceWorker: Boolean,
    val isServiceWorkerActive: Boolean,
    val isOnline: Boolean
)

data class PageReadyResult(
    val isReady: Boolean,
    val duration: Long,
    val documentReady: Boolean,
    val ajaxComplete: Boolean,
    val webSocketIdle: Boolean,
    val graphQLComplete: Boolean,
    val animationsComplete: Boolean
)
