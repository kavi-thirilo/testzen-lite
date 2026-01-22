package com.testzen.core.stability.registry

import com.testzen.core.model.Platform
import com.testzen.core.stability.SmartScrollStrategy
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.ExpectedCondition
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Web-specific stability adapter using JavaScript execution.
 *
 * Provides full stability features for web browsers including:
 * - Document ready state detection
 * - Network idle detection (XHR/Fetch monitoring)
 * - DOM mutation observation
 * - CSS animation detection
 * - Framework-specific loading (jQuery, Angular, React)
 */
class WebStabilityAdapter : PlatformStabilityAdapter {

    private val logger = LoggerFactory.getLogger(WebStabilityAdapter::class.java)

    override val supportedPlatform: Platform = Platform.WEB

    override fun supportsDriver(driver: WebDriver): Boolean {
        return driver is JavascriptExecutor && !isMobileDriver(driver)
    }

    private fun isMobileDriver(driver: WebDriver): Boolean {
        return try {
            val capabilities = (driver as? RemoteWebDriver)?.capabilities
            val platformName = capabilities?.getCapability("platformName")?.toString()?.lowercase() ?: ""
            platformName.contains("android") || platformName.contains("ios")
        } catch (e: Exception) {
            false
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PAGE LOAD DETECTION
    // ═══════════════════════════════════════════════════════════════════════════════

    override fun isPageReady(driver: WebDriver): Boolean {
        val js = driver as? JavascriptExecutor ?: return true
        return try {
            js.executeScript("return document.readyState") == "complete"
        } catch (e: Exception) {
            logger.trace("Error checking page ready: ${e.message}")
            false
        }
    }

    override fun waitForPageReady(driver: WebDriver, timeoutMs: Long): Boolean {
        if (driver !is JavascriptExecutor) return true

        return try {
            val wait = WebDriverWait(driver, Duration.ofMillis(timeoutMs))
            wait.until(ExpectedCondition { d ->
                val state = (d as? JavascriptExecutor)?.executeScript("return document.readyState")
                state == "complete"
            })
            true
        } catch (e: Exception) {
            logger.debug("Document ready wait timeout: ${e.message}")
            false
        }
    }

    override fun isNetworkIdle(driver: WebDriver): Boolean {
        return getPendingNetworkRequests(driver) == 0
    }

    override fun getPendingNetworkRequests(driver: WebDriver): Int {
        val js = driver as? JavascriptExecutor ?: return 0
        return try {
            (js.executeScript("return window.__testzenPendingRequests || 0") as? Number)?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // DOM/VIEW STABILITY
    // ═══════════════════════════════════════════════════════════════════════════════

    override fun isDomStable(driver: WebDriver): Boolean {
        return !hasDomMutations(driver)
    }

    override fun hasDomMutations(driver: WebDriver): Boolean {
        val js = driver as? JavascriptExecutor ?: return false
        return try {
            js.executeScript("return window.__testzenDomMutated || false") as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    override fun resetDomMutationTracking(driver: WebDriver) {
        val js = driver as? JavascriptExecutor ?: return
        try {
            js.executeScript("window.__testzenDomMutated = false;")
        } catch (e: Exception) {
            logger.trace("Could not reset DOM mutation flag: ${e.message}")
        }
    }

    override fun injectMonitors(driver: WebDriver) {
        val js = driver as? JavascriptExecutor ?: return

        // Inject network monitor
        try {
            js.executeScript(NETWORK_MONITOR_SCRIPT)
        } catch (e: Exception) {
            logger.trace("Could not inject network monitor: ${e.message}")
        }

        // Inject DOM observer
        try {
            js.executeScript(DOM_OBSERVER_SCRIPT)
        } catch (e: Exception) {
            logger.trace("Could not inject DOM observer: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // SCROLL OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    override fun getScrollPosition(driver: WebDriver, direction: SmartScrollStrategy.ScrollDirection): Int {
        val js = driver as? JavascriptExecutor ?: return 0
        return try {
            val script = when (direction) {
                SmartScrollStrategy.ScrollDirection.UP,
                SmartScrollStrategy.ScrollDirection.DOWN ->
                    "return window.pageYOffset || document.documentElement.scrollTop || 0;"
                SmartScrollStrategy.ScrollDirection.LEFT,
                SmartScrollStrategy.ScrollDirection.RIGHT ->
                    "return window.pageXOffset || document.documentElement.scrollLeft || 0;"
            }
            (js.executeScript(script) as? Number)?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    override fun getTotalContentSize(driver: WebDriver, direction: SmartScrollStrategy.ScrollDirection): Int {
        val js = driver as? JavascriptExecutor ?: return 0
        return try {
            val script = when (direction) {
                SmartScrollStrategy.ScrollDirection.UP,
                SmartScrollStrategy.ScrollDirection.DOWN ->
                    "return document.documentElement.scrollHeight || document.body.scrollHeight || 0;"
                SmartScrollStrategy.ScrollDirection.LEFT,
                SmartScrollStrategy.ScrollDirection.RIGHT ->
                    "return document.documentElement.scrollWidth || document.body.scrollWidth || 0;"
            }
            (js.executeScript(script) as? Number)?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
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
        val js = driver as? JavascriptExecutor ?: return System.currentTimeMillis().toInt()
        return try {
            val content = js.executeScript(CONTENT_HASH_SCRIPT) as? String ?: ""
            content.hashCode()
        } catch (e: Exception) {
            System.currentTimeMillis().toInt()
        }
    }

    override fun getVisibleElementCount(driver: WebDriver): Int {
        val js = driver as? JavascriptExecutor ?: return 0
        return try {
            (js.executeScript(
                "return document.querySelectorAll('*:not(script):not(style)').length;"
            ) as? Number)?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    override fun scrollElementIntoView(driver: WebDriver, element: WebElement) {
        val js = driver as? JavascriptExecutor ?: return
        try {
            js.executeScript(
                "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center', inline: 'center'});",
                element
            )
        } catch (e: Exception) {
            logger.debug("Could not scroll element into view: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ANIMATION DETECTION
    // ═══════════════════════════════════════════════════════════════════════════════

    override fun hasActiveAnimations(driver: WebDriver): Boolean {
        val js = driver as? JavascriptExecutor ?: return false
        return try {
            js.executeScript(ACTIVE_ANIMATIONS_SCRIPT) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    override fun isElementAnimating(driver: WebDriver, element: WebElement): Boolean {
        val js = driver as? JavascriptExecutor ?: return false
        return try {
            js.executeScript(ELEMENT_ANIMATING_SCRIPT, element) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    override fun waitForElementAnimations(driver: WebDriver, element: WebElement, timeoutMs: Long): Boolean {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (!isElementAnimating(driver, element)) {
                return true
            }
            Thread.sleep(50)
        }

        return false
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ASYNC/FRAMEWORK DETECTION
    // ═══════════════════════════════════════════════════════════════════════════════

    override fun isAsyncComplete(driver: WebDriver): Boolean {
        val js = driver as? JavascriptExecutor ?: return true
        return try {
            js.executeScript(ASYNC_COMPLETE_SCRIPT) as? Boolean ?: true
        } catch (e: Exception) {
            true
        }
    }

    override fun hasLoadingIndicators(driver: WebDriver, selectors: List<String>): Boolean {
        val js = driver as? JavascriptExecutor ?: return false
        return selectors.any { selector ->
            try {
                val visible = js.executeScript(
                    """
                    var el = document.querySelector(arguments[0]);
                    return el && el.offsetParent !== null;
                    """.trimIndent(),
                    selector
                ) as? Boolean ?: false
                visible
            } catch (e: Exception) {
                false
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════════

    override fun executeScript(driver: WebDriver, script: String, vararg args: Any): Any? {
        val js = driver as? JavascriptExecutor ?: return null
        return try {
            js.executeScript(script, *args)
        } catch (e: Exception) {
            logger.trace("Script execution failed: ${e.message}")
            null
        }
    }

    override fun supportsScriptExecution(driver: WebDriver): Boolean {
        return driver is JavascriptExecutor
    }

    override fun getPlatformInfo(driver: WebDriver): Map<String, Any> {
        val js = driver as? JavascriptExecutor ?: return mapOf("platform" to "web", "scriptSupport" to false)

        return try {
            val userAgent = js.executeScript("return navigator.userAgent") as? String ?: "unknown"
            val url = driver.currentUrl ?: "unknown"

            mapOf(
                "platform" to "web",
                "scriptSupport" to true,
                "userAgent" to userAgent,
                "currentUrl" to url,
                "documentReady" to isPageReady(driver),
                "networkIdle" to isNetworkIdle(driver)
            )
        } catch (e: Exception) {
            mapOf("platform" to "web", "scriptSupport" to true, "error" to (e.message ?: "unknown"))
        }
    }

    companion object {
        /**
         * JavaScript to monitor XHR and Fetch requests.
         */
        private val NETWORK_MONITOR_SCRIPT = """
            if (!window.__testzenNetworkMonitor) {
                window.__testzenPendingRequests = 0;
                window.__testzenNetworkMonitor = true;

                // Monitor XHR
                var originalXHR = window.XMLHttpRequest;
                window.XMLHttpRequest = function() {
                    var xhr = new originalXHR();
                    xhr.addEventListener('loadstart', function() {
                        window.__testzenPendingRequests++;
                    });
                    xhr.addEventListener('loadend', function() {
                        window.__testzenPendingRequests = Math.max(0, window.__testzenPendingRequests - 1);
                    });
                    return xhr;
                };

                // Monitor Fetch
                var originalFetch = window.fetch;
                window.fetch = function() {
                    window.__testzenPendingRequests++;
                    return originalFetch.apply(this, arguments).finally(function() {
                        window.__testzenPendingRequests = Math.max(0, window.__testzenPendingRequests - 1);
                    });
                };
            }
        """.trimIndent()

        /**
         * JavaScript to observe DOM mutations.
         */
        private val DOM_OBSERVER_SCRIPT = """
            if (!window.__testzenDomObserver) {
                window.__testzenDomMutated = false;
                window.__testzenDomObserver = new MutationObserver(function(mutations) {
                    window.__testzenDomMutated = true;
                });
                window.__testzenDomObserver.observe(document.body, {
                    childList: true,
                    subtree: true,
                    attributes: true
                });
            }
        """.trimIndent()

        /**
         * JavaScript to get content hash.
         */
        private val CONTENT_HASH_SCRIPT = """
            var texts = [];
            var elements = document.querySelectorAll('*');
            for (var i = 0; i < Math.min(elements.length, 100); i++) {
                var el = elements[i];
                if (el.offsetParent !== null) {
                    texts.push(el.tagName + ':' + (el.textContent || '').substring(0, 50));
                }
            }
            return texts.join('|');
        """.trimIndent()

        /**
         * JavaScript to check for active animations.
         */
        private val ACTIVE_ANIMATIONS_SCRIPT = """
            var animations = document.getAnimations ? document.getAnimations() : [];
            return animations.some(function(a) { return a.playState === 'running'; });
        """.trimIndent()

        /**
         * JavaScript to check if element is animating.
         */
        private val ELEMENT_ANIMATING_SCRIPT = """
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

        /**
         * JavaScript to check async framework completion.
         */
        private val ASYNC_COMPLETE_SCRIPT = """
            // Check jQuery
            if (typeof jQuery !== 'undefined' && jQuery.active > 0) return false;

            // Check Angular
            if (typeof angular !== 'undefined') {
                var injector = angular.element(document).injector();
                if (injector) {
                    var http = injector.get('${'$'}http');
                    if (http && http.pendingRequests && http.pendingRequests.length > 0) return false;
                }
            }

            // Check AngularJS testability
            if (typeof angular !== 'undefined' && angular.getTestability) {
                try {
                    var testability = angular.getTestability(document.body);
                    if (testability && !testability.isStable()) return false;
                } catch(e) {}
            }

            // Check React (basic heuristic)
            var reactRoot = document.querySelector('[data-reactroot], #root, #app');
            if (reactRoot) {
                var loadingIndicators = reactRoot.querySelectorAll('[class*="loading"], [class*="spinner"], [class*="skeleton"]');
                if (loadingIndicators.length > 0) {
                    for (var i = 0; i < loadingIndicators.length; i++) {
                        if (loadingIndicators[i].offsetParent !== null) return false;
                    }
                }
            }

            return true;
        """.trimIndent()

        /**
         * Common loading indicator selectors for web.
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
