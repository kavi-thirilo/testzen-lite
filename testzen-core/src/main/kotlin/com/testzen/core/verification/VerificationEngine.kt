package com.testzen.core.verification

import com.testzen.core.config.TestZenConfig
import com.testzen.core.execution.ElementFinder
import com.testzen.core.model.Platform
import com.testzen.core.stability.ElementStabilityWaiter
import com.testzen.core.stability.PageLoadIntelligence
import com.testzen.core.stability.RetryWithBackoff
import com.testzen.core.stability.StabilityConfig
import org.openqa.selenium.WebDriver
import org.slf4j.LoggerFactory

/**
 * Main verification engine that orchestrates all verification operations.
 *
 * Features:
 * - Unified API for all verification types
 * - Soft assertions (collect all failures instead of stopping)
 * - Aggregated reporting
 * - Retry mechanisms for flaky verifications
 * - Screenshot capture on failure
 * - Custom verification conditions
 * - Fluent API for chaining verifications
 * - **Intelligent waiting** for page load and element stability
 * - **Exponential backoff** for transient failures
 * - **Adaptive verification** that handles dynamic content
 *
 * Single Responsibility: Orchestrate and manage verification operations.
 *
 * Usage:
 * ```kotlin
 * val engine = VerificationEngine(driver, elementFinder, config)
 *
 * // Single verifications
 * val result = engine.verify("Login Button").isDisplayed()
 *
 * // Soft assertions (collect all failures)
 * engine.softAssert()
 *     .verify("Welcome").isDisplayed()
 *     .verify("Username").hasText("John")
 *     .verify("Status").containsText("Active")
 *     .assertAll()  // Throws if any failed
 *
 * // Fluent chained verification
 * engine.that("Login Form")
 *     .isDisplayed()
 *     .isEnabled()
 *     .hasAttribute("class", "form-container")
 *
 * // With intelligent waiting (recommended for dynamic content)
 * engine.verifyWithIntelligentWait("Welcome message").isDisplayed()
 * ```
 */
class VerificationEngine(
    private val driver: WebDriver,
    private val elementFinder: ElementFinder,
    private val config: TestZenConfig,
    private val platform: Platform = Platform.ANDROID,
    private val stabilityConfig: StabilityConfig = StabilityConfig.default()
) {
    private val logger = LoggerFactory.getLogger(VerificationEngine::class.java)

    // Verifier components
    private val elementVerifier = ElementVerifier(driver, elementFinder)
    private val textVerifier = TextVerifier(driver, elementFinder)
    private val matcher = VerificationMatcher.default

    // Stability components
    private val stabilityWaiter = ElementStabilityWaiter(stabilityConfig)
    private val pageLoadIntelligence = PageLoadIntelligence(stabilityConfig)
    private val retryMechanism = RetryWithBackoff(stabilityConfig)

    // Soft assertion tracking
    private val softAssertionResults = mutableListOf<VerificationResult>()
    private var softAssertionMode = false

    // Default context with stability awareness
    private var defaultContext = VerificationContext(
        timeoutMs = config.elementTimeoutMs,
        screenshotOnFailure = config.screenshotOnFailure,
        retryCount = stabilityConfig.maxTransientRetries,
        retryDelayMs = stabilityConfig.initialRetryDelayMs,
        waitForStability = stabilityConfig.elementStabilityEnabled,
        useExponentialBackoff = stabilityConfig.exponentialBackoffEnabled
    )

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC API - FLUENT INTERFACE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Start verification for a target element.
     */
    fun verify(target: String): ElementVerificationBuilder {
        return ElementVerificationBuilder(target, this)
    }

    /**
     * Alias for verify() - more readable in some contexts.
     */
    fun that(target: String): ElementVerificationBuilder = verify(target)

    /**
     * Start soft assertion mode.
     * All subsequent verifications will be collected instead of failing immediately.
     */
    fun softAssert(): SoftAssertionBuilder {
        return SoftAssertionBuilder(this)
    }

    /**
     * Start verification with intelligent waiting enabled.
     *
     * This method waits for:
     * - Page to be ready (network idle, DOM stable)
     * - Element to be stable (position/size settled)
     * - Dynamic content to load
     *
     * Recommended for verifications on dynamic content or after navigation.
     */
    fun verifyWithIntelligentWait(target: String): IntelligentVerificationBuilder {
        return IntelligentVerificationBuilder(target, this)
    }

    /**
     * Wait for page to be fully ready before verification.
     *
     * Useful after navigation, form submission, or AJAX operations.
     */
    fun waitForPageReady(timeoutMs: Long = stabilityConfig.pageLoadTimeoutMs): Boolean {
        val result = pageLoadIntelligence.waitForPageReady(driver, timeoutMs)
        return result is PageLoadIntelligence.WaitResult.Success
    }

    /**
     * Wait for element to appear and stabilize.
     */
    fun waitForElement(target: String, timeoutMs: Long = stabilityConfig.stabilityTimeoutMs): Boolean {
        return retryMechanism.waitForCondition(
            condition = {
                try {
                    val element = elementFinder.findElement(target)
                    element != null && element.isDisplayed
                } catch (e: Exception) {
                    false
                }
            },
            timeoutMs = timeoutMs,
            pollIntervalMs = stabilityConfig.stabilityCheckIntervalMs,
            description = "Element '$target' to appear"
        )
    }

    /**
     * Wait for element to disappear (loading indicators, etc.).
     */
    fun waitForElementGone(target: String, timeoutMs: Long = stabilityConfig.stabilityTimeoutMs): Boolean {
        return stabilityWaiter.waitForDisappearance(
            findElement = {
                try {
                    elementFinder.findElement(target)
                } catch (e: Exception) {
                    null
                }
            },
            timeoutMs = timeoutMs
        )
    }

    /**
     * Execute verification with retry and exponential backoff.
     */
    fun <T> verifyWithRetry(
        operation: () -> T,
        maxAttempts: Int = stabilityConfig.maxTransientRetries + 1
    ): T {
        val result = retryMechanism.execute(
            operation = { operation() },
            maxAttempts = maxAttempts,
            timeoutMs = defaultContext.timeoutMs
        )

        return when (result) {
            is RetryWithBackoff.RetryResult.Success -> result.value
            is RetryWithBackoff.RetryResult.Failure -> throw result.lastException
        }
    }

    /**
     * Verify text on screen (not specific to an element).
     */
    fun verifyTextOnScreen(text: String): VerificationResult {
        return processResult(textVerifier.verifyTextOnScreen(text, defaultContext))
    }

    /**
     * Verify text is NOT on screen.
     */
    fun verifyTextNotOnScreen(text: String): VerificationResult {
        return processResult(textVerifier.verifyTextNotOnScreen(text, defaultContext))
    }

    /**
     * Verify page title.
     */
    fun verifyPageTitle(expectedTitle: String): VerificationResult {
        return processResult(textVerifier.verifyPageTitleEquals(expectedTitle, defaultContext))
    }

    /**
     * Verify page title contains text.
     */
    fun verifyPageTitleContains(text: String): VerificationResult {
        return processResult(textVerifier.verifyPageTitleContains(text, defaultContext))
    }

    /**
     * Verify current URL.
     */
    fun verifyUrl(expectedUrl: String): VerificationResult {
        return processResult(textVerifier.verifyUrlEquals(expectedUrl, defaultContext))
    }

    /**
     * Verify URL contains text.
     */
    fun verifyUrlContains(text: String): VerificationResult {
        return processResult(textVerifier.verifyUrlContains(text, defaultContext))
    }

    // ═══════════════════════════════════════════════════════════════
    // ELEMENT VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verify element is displayed.
     */
    fun verifyDisplayed(target: String, context: VerificationContext = defaultContext): VerificationResult {
        return processResult(elementVerifier.verifyDisplayed(target, context))
    }

    /**
     * Verify element is NOT displayed.
     */
    fun verifyNotDisplayed(target: String, context: VerificationContext = defaultContext): VerificationResult {
        return processResult(elementVerifier.verifyNotDisplayed(target, context))
    }

    /**
     * Verify element is enabled.
     */
    fun verifyEnabled(target: String, context: VerificationContext = defaultContext): VerificationResult {
        return processResult(elementVerifier.verifyEnabled(target, context))
    }

    /**
     * Verify element is disabled.
     */
    fun verifyDisabled(target: String, context: VerificationContext = defaultContext): VerificationResult {
        return processResult(elementVerifier.verifyDisabled(target, context))
    }

    /**
     * Verify checkbox/toggle is checked.
     */
    fun verifyChecked(target: String, context: VerificationContext = defaultContext): VerificationResult {
        return processResult(elementVerifier.verifyChecked(target, context))
    }

    /**
     * Verify checkbox/toggle is NOT checked.
     */
    fun verifyNotChecked(target: String, context: VerificationContext = defaultContext): VerificationResult {
        return processResult(elementVerifier.verifyNotChecked(target, context))
    }

    /**
     * Verify element is selected.
     */
    fun verifySelected(target: String, context: VerificationContext = defaultContext): VerificationResult {
        return processResult(elementVerifier.verifySelected(target, context))
    }

    /**
     * Verify element count.
     */
    fun verifyCount(
        target: String,
        expectedCount: Int,
        operator: ComparisonOperator = ComparisonOperator.EQUALS,
        context: VerificationContext = defaultContext
    ): VerificationResult {
        return processResult(elementVerifier.verifyCount(target, expectedCount, operator, context))
    }

    // ═══════════════════════════════════════════════════════════════
    // TEXT VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verify element text equals expected value.
     */
    fun verifyText(
        target: String,
        expectedText: String,
        context: VerificationContext = defaultContext
    ): VerificationResult {
        return processResult(textVerifier.verifyTextEquals(target, expectedText, context))
    }

    /**
     * Verify element text contains substring.
     */
    fun verifyTextContains(
        target: String,
        expectedText: String,
        context: VerificationContext = defaultContext
    ): VerificationResult {
        return processResult(textVerifier.verifyTextContains(target, expectedText, context))
    }

    /**
     * Verify element text matches regex.
     */
    fun verifyTextMatches(
        target: String,
        pattern: String,
        context: VerificationContext = defaultContext
    ): VerificationResult {
        return processResult(textVerifier.verifyTextMatchesRegex(target, pattern, context))
    }

    /**
     * Verify numeric value.
     */
    fun verifyNumericValue(
        target: String,
        expectedValue: Number,
        operator: ComparisonOperator = ComparisonOperator.EQUALS,
        context: VerificationContext = defaultContext
    ): VerificationResult {
        return processResult(textVerifier.verifyNumericValue(target, expectedValue, operator, context))
    }

    // ═══════════════════════════════════════════════════════════════
    // ATTRIBUTE VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verify element attribute value.
     */
    fun verifyAttribute(
        target: String,
        attributeName: String,
        expectedValue: String,
        operator: ComparisonOperator = ComparisonOperator.EQUALS,
        context: VerificationContext = defaultContext
    ): VerificationResult {
        return processResult(elementVerifier.verifyAttribute(target, attributeName, expectedValue, operator, context))
    }

    /**
     * Verify CSS property value.
     */
    fun verifyCssProperty(
        target: String,
        propertyName: String,
        expectedValue: String,
        operator: ComparisonOperator = ComparisonOperator.CONTAINS,
        context: VerificationContext = defaultContext
    ): VerificationResult {
        return processResult(elementVerifier.verifyCssProperty(target, propertyName, expectedValue, operator, context))
    }

    // ═══════════════════════════════════════════════════════════════
    // CUSTOM VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Execute a custom verification.
     */
    fun verifyCustom(
        name: String,
        condition: () -> Boolean,
        failureMessage: String = "Custom verification failed"
    ): VerificationResult {
        return try {
            val passed = condition()
            val result = if (passed) {
                VerificationResult.success(
                    type = VerificationType.CUSTOM,
                    target = name,
                    message = "Custom verification passed: $name"
                )
            } else {
                VerificationResult.failure(
                    type = VerificationType.CUSTOM,
                    target = name,
                    message = failureMessage
                )
            }
            processResult(result)
        } catch (e: Exception) {
            processResult(
                VerificationResult.failure(
                    type = VerificationType.CUSTOM,
                    target = name,
                    message = "Custom verification threw exception",
                    errorDetails = e.message
                )
            )
        }
    }

    /**
     * Execute a custom verification with expected/actual values.
     */
    fun <T> verifyCustom(
        name: String,
        actual: T,
        expected: T,
        comparison: (T, T) -> Boolean = { a, e -> a == e }
    ): VerificationResult {
        val passed = comparison(actual, expected)
        val result = if (passed) {
            VerificationResult.success(
                type = VerificationType.CUSTOM,
                target = name,
                message = "Custom verification passed",
                expected = expected,
                actual = actual
            )
        } else {
            VerificationResult.failure(
                type = VerificationType.CUSTOM,
                target = name,
                message = "Custom verification failed",
                expected = expected,
                actual = actual
            )
        }
        return processResult(result)
    }

    // ═══════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Set default verification context.
     */
    fun withContext(context: VerificationContext): VerificationEngine {
        defaultContext = context
        return this
    }

    /**
     * Set default timeout.
     */
    fun withTimeout(timeoutMs: Long): VerificationEngine {
        defaultContext = defaultContext.copy(timeoutMs = timeoutMs)
        return this
    }

    /**
     * Enable/disable screenshot on failure.
     */
    fun withScreenshotOnFailure(enabled: Boolean): VerificationEngine {
        defaultContext = defaultContext.copy(screenshotOnFailure = enabled)
        return this
    }

    /**
     * Set retry configuration.
     */
    fun withRetry(retryCount: Int, retryDelayMs: Long = 500): VerificationEngine {
        defaultContext = defaultContext.copy(retryCount = retryCount, retryDelayMs = retryDelayMs)
        return this
    }

    // ═══════════════════════════════════════════════════════════════
    // REPORTING
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get verification report for soft assertions.
     */
    fun getReport(): VerificationReport {
        return VerificationReport(results = softAssertionResults.toList())
    }

    /**
     * Clear collected soft assertion results.
     */
    fun clearResults() {
        softAssertionResults.clear()
    }

    /**
     * Get all failures from soft assertions.
     */
    fun getFailures(): List<VerificationResult> {
        return softAssertionResults.filter { it.failed }
    }

    /**
     * Check if all soft assertions passed.
     */
    fun allPassed(): Boolean {
        return softAssertionResults.all { it.passed }
    }

    // ═══════════════════════════════════════════════════════════════
    // INTERNAL METHODS
    // ═══════════════════════════════════════════════════════════════

    internal fun processResult(result: VerificationResult): VerificationResult {
        if (softAssertionMode) {
            softAssertionResults.add(result.copy(isSoftAssertion = true))
        } else if (result.failed) {
            logger.error("Verification failed: ${result.message}")
        }

        return result
    }

    internal fun enableSoftMode() {
        softAssertionMode = true
    }

    internal fun disableSoftMode() {
        softAssertionMode = false
    }

    internal fun getDefaultContext() = defaultContext

    internal fun getElementVerifier() = elementVerifier
    internal fun getTextVerifier() = textVerifier
}

/**
 * Builder for element verifications with fluent API.
 */
class ElementVerificationBuilder(
    private val target: String,
    private val engine: VerificationEngine
) {
    private val context = engine.getDefaultContext()
    private val elementVerifier = engine.getElementVerifier()
    private val textVerifier = engine.getTextVerifier()

    // Presence verifications
    fun isDisplayed(): VerificationResult = engine.processResult(elementVerifier.verifyDisplayed(target, context))
    fun isNotDisplayed(): VerificationResult = engine.processResult(elementVerifier.verifyNotDisplayed(target, context))
    fun exists(): VerificationResult = engine.processResult(elementVerifier.verifyExists(target, context))
    fun notExists(): VerificationResult = engine.processResult(elementVerifier.verifyNotExists(target, context))

    // State verifications
    fun isEnabled(): VerificationResult = engine.processResult(elementVerifier.verifyEnabled(target, context))
    fun isDisabled(): VerificationResult = engine.processResult(elementVerifier.verifyDisabled(target, context))
    fun isSelected(): VerificationResult = engine.processResult(elementVerifier.verifySelected(target, context))
    fun isNotSelected(): VerificationResult = engine.processResult(elementVerifier.verifyNotSelected(target, context))
    fun isChecked(): VerificationResult = engine.processResult(elementVerifier.verifyChecked(target, context))
    fun isNotChecked(): VerificationResult = engine.processResult(elementVerifier.verifyNotChecked(target, context))
    fun isFocused(): VerificationResult = engine.processResult(elementVerifier.verifyFocused(target, context))

    // Text verifications
    fun hasText(text: String): VerificationResult = engine.processResult(textVerifier.verifyTextEquals(target, text, context))
    fun containsText(text: String): VerificationResult = engine.processResult(textVerifier.verifyTextContains(target, text, context))
    fun textStartsWith(prefix: String): VerificationResult = engine.processResult(textVerifier.verifyTextStartsWith(target, prefix, context))
    fun textEndsWith(suffix: String): VerificationResult = engine.processResult(textVerifier.verifyTextEndsWith(target, suffix, context))
    fun textMatches(pattern: String): VerificationResult = engine.processResult(textVerifier.verifyTextMatchesRegex(target, pattern, context))
    fun textDoesNotContain(text: String): VerificationResult = engine.processResult(textVerifier.verifyTextNotContains(target, text, context))
    fun textIsEmpty(): VerificationResult = engine.processResult(textVerifier.verifyTextIsEmpty(target, context))
    fun textIsNotEmpty(): VerificationResult = engine.processResult(textVerifier.verifyTextIsNotEmpty(target, context))

    // Attribute verifications
    fun hasAttribute(name: String, value: String): VerificationResult =
        engine.processResult(elementVerifier.verifyAttribute(target, name, value, ComparisonOperator.EQUALS, context))
    fun attributeContains(name: String, value: String): VerificationResult =
        engine.processResult(elementVerifier.verifyAttribute(target, name, value, ComparisonOperator.CONTAINS, context))
    fun hasAttributePresent(name: String): VerificationResult =
        engine.processResult(elementVerifier.verifyAttributeExists(target, name, context))

    // CSS verifications
    fun hasCssProperty(property: String, value: String): VerificationResult =
        engine.processResult(elementVerifier.verifyCssProperty(target, property, value, ComparisonOperator.EQUALS, context))
    fun cssPropertyContains(property: String, value: String): VerificationResult =
        engine.processResult(elementVerifier.verifyCssProperty(target, property, value, ComparisonOperator.CONTAINS, context))

    // Count verifications
    fun hasCount(count: Int): VerificationResult =
        engine.processResult(elementVerifier.verifyCount(target, count, ComparisonOperator.EQUALS, context))
    fun hasCountGreaterThan(count: Int): VerificationResult =
        engine.processResult(elementVerifier.verifyCount(target, count, ComparisonOperator.GREATER_THAN, context))
    fun hasCountLessThan(count: Int): VerificationResult =
        engine.processResult(elementVerifier.verifyCount(target, count, ComparisonOperator.LESS_THAN, context))
    fun hasCountBetween(min: Int, max: Int): VerificationResult =
        engine.processResult(elementVerifier.verifyCountBetween(target, min, max, context))

    // Numeric verifications
    fun hasValue(value: Number): VerificationResult =
        engine.processResult(textVerifier.verifyNumericValue(target, value, ComparisonOperator.EQUALS, context))
    fun valueGreaterThan(value: Number): VerificationResult =
        engine.processResult(textVerifier.verifyNumericValue(target, value, ComparisonOperator.GREATER_THAN, context))
    fun valueLessThan(value: Number): VerificationResult =
        engine.processResult(textVerifier.verifyNumericValue(target, value, ComparisonOperator.LESS_THAN, context))
}

/**
 * Builder for soft assertions.
 */
class SoftAssertionBuilder(private val engine: VerificationEngine) {
    private val results = mutableListOf<VerificationResult>()

    init {
        engine.enableSoftMode()
    }

    /**
     * Add verification to soft assertion chain.
     */
    fun verify(target: String): SoftElementVerificationBuilder {
        return SoftElementVerificationBuilder(target, this, engine)
    }

    /**
     * Add result to collection.
     */
    internal fun addResult(result: VerificationResult) {
        results.add(result)
    }

    /**
     * Assert all collected verifications passed.
     * @throws AssertionError if any verification failed
     */
    fun assertAll() {
        engine.disableSoftMode()

        val failures = results.filter { it.failed }
        if (failures.isNotEmpty()) {
            val message = buildString {
                appendLine("${failures.size} verification(s) failed:")
                failures.forEachIndexed { index, result ->
                    appendLine("  ${index + 1}. ${result.message}")
                    result.errorDetails?.let { appendLine("     Details: $it") }
                }
            }
            throw AssertionError(message)
        }
    }

    /**
     * Get report without asserting.
     */
    fun getReport(): VerificationReport {
        engine.disableSoftMode()
        return VerificationReport(results = results.toList())
    }

    /**
     * Check if all passed without throwing.
     */
    fun allPassed(): Boolean {
        engine.disableSoftMode()
        return results.all { it.passed }
    }
}

/**
 * Builder for soft element verifications.
 */
class SoftElementVerificationBuilder(
    private val target: String,
    private val parent: SoftAssertionBuilder,
    private val engine: VerificationEngine
) {
    private val builder = ElementVerificationBuilder(target, engine)

    fun isDisplayed(): SoftAssertionBuilder {
        parent.addResult(builder.isDisplayed())
        return parent
    }

    fun isNotDisplayed(): SoftAssertionBuilder {
        parent.addResult(builder.isNotDisplayed())
        return parent
    }

    fun isEnabled(): SoftAssertionBuilder {
        parent.addResult(builder.isEnabled())
        return parent
    }

    fun isDisabled(): SoftAssertionBuilder {
        parent.addResult(builder.isDisabled())
        return parent
    }

    fun hasText(text: String): SoftAssertionBuilder {
        parent.addResult(builder.hasText(text))
        return parent
    }

    fun containsText(text: String): SoftAssertionBuilder {
        parent.addResult(builder.containsText(text))
        return parent
    }

    fun isChecked(): SoftAssertionBuilder {
        parent.addResult(builder.isChecked())
        return parent
    }

    fun isNotChecked(): SoftAssertionBuilder {
        parent.addResult(builder.isNotChecked())
        return parent
    }

    fun hasAttribute(name: String, value: String): SoftAssertionBuilder {
        parent.addResult(builder.hasAttribute(name, value))
        return parent
    }

    fun hasCount(count: Int): SoftAssertionBuilder {
        parent.addResult(builder.hasCount(count))
        return parent
    }

    fun textMatches(pattern: String): SoftAssertionBuilder {
        parent.addResult(builder.textMatches(pattern))
        return parent
    }
}

/**
 * Builder for verifications with intelligent waiting.
 *
 * Automatically waits for page stability and element rendering
 * before performing verification.
 */
class IntelligentVerificationBuilder(
    private val target: String,
    private val engine: VerificationEngine
) {
    private val context = engine.getDefaultContext().copy(
        waitForStability = true,
        useExponentialBackoff = true
    )
    private val elementVerifier = engine.getElementVerifier()
    private val textVerifier = engine.getTextVerifier()

    /**
     * Execute verification with intelligent waiting.
     */
    private fun <T> withIntelligentWait(operation: () -> T): T {
        // Wait for page ready first
        engine.waitForPageReady(context.timeoutMs / 3)

        // Then wait for element and execute verification
        return engine.verifyWithRetry(operation)
    }

    // Presence verifications with intelligent wait
    fun isDisplayed(): VerificationResult = withIntelligentWait {
        engine.processResult(elementVerifier.verifyDisplayed(target, context))
    }

    fun isNotDisplayed(): VerificationResult = withIntelligentWait {
        engine.processResult(elementVerifier.verifyNotDisplayed(target, context))
    }

    fun exists(): VerificationResult = withIntelligentWait {
        engine.processResult(elementVerifier.verifyExists(target, context))
    }

    fun notExists(): VerificationResult = withIntelligentWait {
        engine.processResult(elementVerifier.verifyNotExists(target, context))
    }

    // State verifications with intelligent wait
    fun isEnabled(): VerificationResult = withIntelligentWait {
        engine.processResult(elementVerifier.verifyEnabled(target, context))
    }

    fun isDisabled(): VerificationResult = withIntelligentWait {
        engine.processResult(elementVerifier.verifyDisabled(target, context))
    }

    fun isSelected(): VerificationResult = withIntelligentWait {
        engine.processResult(elementVerifier.verifySelected(target, context))
    }

    fun isChecked(): VerificationResult = withIntelligentWait {
        engine.processResult(elementVerifier.verifyChecked(target, context))
    }

    fun isNotChecked(): VerificationResult = withIntelligentWait {
        engine.processResult(elementVerifier.verifyNotChecked(target, context))
    }

    // Text verifications with intelligent wait
    fun hasText(text: String): VerificationResult = withIntelligentWait {
        engine.processResult(textVerifier.verifyTextEquals(target, text, context))
    }

    fun containsText(text: String): VerificationResult = withIntelligentWait {
        engine.processResult(textVerifier.verifyTextContains(target, text, context))
    }

    fun textStartsWith(prefix: String): VerificationResult = withIntelligentWait {
        engine.processResult(textVerifier.verifyTextStartsWith(target, prefix, context))
    }

    fun textEndsWith(suffix: String): VerificationResult = withIntelligentWait {
        engine.processResult(textVerifier.verifyTextEndsWith(target, suffix, context))
    }

    fun textMatches(pattern: String): VerificationResult = withIntelligentWait {
        engine.processResult(textVerifier.verifyTextMatchesRegex(target, pattern, context))
    }

    // Attribute verifications with intelligent wait
    fun hasAttribute(name: String, value: String): VerificationResult = withIntelligentWait {
        engine.processResult(elementVerifier.verifyAttribute(target, name, value, ComparisonOperator.EQUALS, context))
    }

    fun attributeContains(name: String, value: String): VerificationResult = withIntelligentWait {
        engine.processResult(elementVerifier.verifyAttribute(target, name, value, ComparisonOperator.CONTAINS, context))
    }

    // Count verifications with intelligent wait
    fun hasCount(count: Int): VerificationResult = withIntelligentWait {
        engine.processResult(elementVerifier.verifyCount(target, count, ComparisonOperator.EQUALS, context))
    }

    fun hasCountGreaterThan(count: Int): VerificationResult = withIntelligentWait {
        engine.processResult(elementVerifier.verifyCount(target, count, ComparisonOperator.GREATER_THAN, context))
    }

    fun hasCountLessThan(count: Int): VerificationResult = withIntelligentWait {
        engine.processResult(elementVerifier.verifyCount(target, count, ComparisonOperator.LESS_THAN, context))
    }
}
