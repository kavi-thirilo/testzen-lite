package com.testzen.core.verification

import com.testzen.core.execution.ElementFinder
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.slf4j.LoggerFactory
import kotlin.system.measureTimeMillis

/**
 * Performs text-based verifications.
 *
 * Supports verifying:
 * - Element text content
 * - Page source/content
 * - Text patterns with regex
 * - Multiple text conditions
 * - Page title and URL
 *
 * Single Responsibility: Text-based verification operations.
 */
class TextVerifier(
    private val driver: WebDriver,
    private val elementFinder: ElementFinder,
    private val matcher: VerificationMatcher = VerificationMatcher.default
) {
    private val logger = LoggerFactory.getLogger(TextVerifier::class.java)

    // ═══════════════════════════════════════════════════════════════
    // ELEMENT TEXT VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verify element text equals expected value.
     */
    fun verifyTextEquals(
        target: String,
        expectedText: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        return verifyElementText(
            target = target,
            expectedText = expectedText,
            operator = ComparisonOperator.EQUALS,
            type = VerificationType.TEXT_EQUALS,
            context = context
        )
    }

    /**
     * Verify element text contains expected substring.
     */
    fun verifyTextContains(
        target: String,
        expectedText: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        return verifyElementText(
            target = target,
            expectedText = expectedText,
            operator = ComparisonOperator.CONTAINS,
            type = VerificationType.TEXT_CONTAINS,
            context = context
        )
    }

    /**
     * Verify element text does NOT contain substring.
     */
    fun verifyTextNotContains(
        target: String,
        unexpectedText: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        return verifyElementText(
            target = target,
            expectedText = unexpectedText,
            operator = ComparisonOperator.NOT_CONTAINS,
            type = VerificationType.TEXT_NOT_CONTAINS,
            context = context
        )
    }

    /**
     * Verify element text starts with expected prefix.
     */
    fun verifyTextStartsWith(
        target: String,
        expectedPrefix: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        return verifyElementText(
            target = target,
            expectedText = expectedPrefix,
            operator = ComparisonOperator.STARTS_WITH,
            type = VerificationType.TEXT_STARTS_WITH,
            context = context
        )
    }

    /**
     * Verify element text ends with expected suffix.
     */
    fun verifyTextEndsWith(
        target: String,
        expectedSuffix: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        return verifyElementText(
            target = target,
            expectedText = expectedSuffix,
            operator = ComparisonOperator.ENDS_WITH,
            type = VerificationType.TEXT_ENDS_WITH,
            context = context
        )
    }

    /**
     * Verify element text matches regex pattern.
     */
    fun verifyTextMatchesRegex(
        target: String,
        pattern: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        return verifyElementText(
            target = target,
            expectedText = pattern,
            operator = ComparisonOperator.MATCHES_REGEX,
            type = VerificationType.TEXT_MATCHES_REGEX,
            context = context
        )
    }

    /**
     * Verify element text is empty.
     */
    fun verifyTextIsEmpty(
        target: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            val element = findElement(target, context.timeoutMs)

            result = if (element == null) {
                VerificationResult.failure(
                    type = VerificationType.TEXT_IS_EMPTY,
                    target = target,
                    message = "Element not found: $target"
                )
            } else {
                val actualText = getElementText(element)
                val matchResult = matcher.match(actualText, null, ComparisonOperator.IS_EMPTY)

                if (matchResult.matched) {
                    VerificationResult.success(
                        type = VerificationType.TEXT_IS_EMPTY,
                        target = target,
                        message = "Element text is empty",
                        actual = actualText
                    )
                } else {
                    VerificationResult.failure(
                        type = VerificationType.TEXT_IS_EMPTY,
                        target = target,
                        message = "Element text is not empty",
                        expected = "empty",
                        actual = actualText,
                        screenshot = captureScreenshot(context)
                    )
                }
            }
        }
        return result.copy(durationMs = duration)
    }

    /**
     * Verify element text is NOT empty.
     */
    fun verifyTextIsNotEmpty(
        target: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            val element = findElement(target, context.timeoutMs)

            result = if (element == null) {
                VerificationResult.failure(
                    type = VerificationType.TEXT_IS_NOT_EMPTY,
                    target = target,
                    message = "Element not found: $target"
                )
            } else {
                val actualText = getElementText(element)
                val matchResult = matcher.match(actualText, null, ComparisonOperator.IS_NOT_EMPTY)

                if (matchResult.matched) {
                    VerificationResult.success(
                        type = VerificationType.TEXT_IS_NOT_EMPTY,
                        target = target,
                        message = "Element text is not empty: '$actualText'",
                        actual = actualText
                    )
                } else {
                    VerificationResult.failure(
                        type = VerificationType.TEXT_IS_NOT_EMPTY,
                        target = target,
                        message = "Element text is empty but should have content",
                        expected = "not empty",
                        actual = "empty",
                        screenshot = captureScreenshot(context)
                    )
                }
            }
        }
        return result.copy(durationMs = duration)
    }

    // ═══════════════════════════════════════════════════════════════
    // PAGE/SCREEN TEXT VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verify text is present anywhere on the page/screen.
     */
    fun verifyTextOnScreen(
        expectedText: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            result = withRetry(context) {
                // First, try to find an element with the text
                val element = findElement(expectedText, context.timeoutMs / 2)
                if (element != null && isDisplayed(element)) {
                    return@withRetry VerificationResult.success(
                        type = VerificationType.PAGE_SOURCE_CONTAINS,
                        target = "screen",
                        message = "Text '$expectedText' found on screen",
                        expected = expectedText
                    )
                }

                // Fallback: check page source
                val pageSource = try {
                    driver.pageSource
                } catch (e: Exception) {
                    ""
                }

                val matchResult = matcher.match(pageSource, expectedText, ComparisonOperator.CONTAINS)

                if (matchResult.matched) {
                    VerificationResult.success(
                        type = VerificationType.PAGE_SOURCE_CONTAINS,
                        target = "screen",
                        message = "Text '$expectedText' found on screen (in page source)",
                        expected = expectedText
                    )
                } else {
                    VerificationResult.failure(
                        type = VerificationType.PAGE_SOURCE_CONTAINS,
                        target = "screen",
                        message = "Text '$expectedText' not found on screen",
                        expected = expectedText,
                        screenshot = captureScreenshot(context)
                    )
                }
            }
        }
        return result.copy(durationMs = duration)
    }

    /**
     * Verify text is NOT present anywhere on the page/screen.
     */
    fun verifyTextNotOnScreen(
        unexpectedText: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            // Use shorter timeout
            val shortTimeout = context.timeoutMs / 3

            val element = try {
                findElement(unexpectedText, shortTimeout)
            } catch (e: Exception) {
                null
            }

            if (element != null && isDisplayed(element)) {
                result = VerificationResult.failure(
                    type = VerificationType.PAGE_SOURCE_CONTAINS,
                    target = "screen",
                    message = "Text '$unexpectedText' should not be on screen but was found",
                    expected = "not present",
                    actual = "present",
                    screenshot = captureScreenshot(context)
                )
            } else {
                // Check page source as well
                val pageSource = try {
                    driver.pageSource
                } catch (e: Exception) {
                    ""
                }

                val containsText = if (context.caseSensitive) {
                    pageSource.contains(unexpectedText)
                } else {
                    pageSource.contains(unexpectedText, ignoreCase = true)
                }

                result = if (!containsText) {
                    VerificationResult.success(
                        type = VerificationType.PAGE_SOURCE_CONTAINS,
                        target = "screen",
                        message = "Text '$unexpectedText' is not on screen"
                    )
                } else {
                    VerificationResult.failure(
                        type = VerificationType.PAGE_SOURCE_CONTAINS,
                        target = "screen",
                        message = "Text '$unexpectedText' found in page source but should not be present",
                        screenshot = captureScreenshot(context)
                    )
                }
            }
        }
        return result.copy(durationMs = duration)
    }

    // ═══════════════════════════════════════════════════════════════
    // PAGE TITLE VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verify page title equals expected value.
     */
    fun verifyPageTitleEquals(
        expectedTitle: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            val actualTitle = try {
                driver.title
            } catch (e: Exception) {
                null
            }

            val matchResult = matcher.match(actualTitle, expectedTitle, ComparisonOperator.EQUALS)

            result = if (matchResult.matched) {
                VerificationResult.success(
                    type = VerificationType.PAGE_TITLE_EQUALS,
                    target = "page title",
                    message = "Page title matches expected value",
                    expected = expectedTitle,
                    actual = actualTitle
                )
            } else {
                VerificationResult.failure(
                    type = VerificationType.PAGE_TITLE_EQUALS,
                    target = "page title",
                    message = "Page title does not match",
                    expected = expectedTitle,
                    actual = actualTitle,
                    errorDetails = matchResult.message
                )
            }
        }
        return result.copy(durationMs = duration)
    }

    /**
     * Verify page title contains expected substring.
     */
    fun verifyPageTitleContains(
        expectedText: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            val actualTitle = try {
                driver.title
            } catch (e: Exception) {
                null
            }

            val matchResult = matcher.match(actualTitle, expectedText, ComparisonOperator.CONTAINS)

            result = if (matchResult.matched) {
                VerificationResult.success(
                    type = VerificationType.PAGE_TITLE_CONTAINS,
                    target = "page title",
                    message = "Page title contains expected text",
                    expected = expectedText,
                    actual = actualTitle
                )
            } else {
                VerificationResult.failure(
                    type = VerificationType.PAGE_TITLE_CONTAINS,
                    target = "page title",
                    message = "Page title does not contain expected text",
                    expected = expectedText,
                    actual = actualTitle,
                    errorDetails = matchResult.message
                )
            }
        }
        return result.copy(durationMs = duration)
    }

    // ═══════════════════════════════════════════════════════════════
    // URL VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verify current URL equals expected value.
     */
    fun verifyUrlEquals(
        expectedUrl: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            val actualUrl = try {
                driver.currentUrl
            } catch (e: Exception) {
                null
            }

            val matchResult = matcher.match(actualUrl, expectedUrl, ComparisonOperator.EQUALS)

            result = if (matchResult.matched) {
                VerificationResult.success(
                    type = VerificationType.URL_EQUALS,
                    target = "URL",
                    message = "URL matches expected value",
                    expected = expectedUrl,
                    actual = actualUrl
                )
            } else {
                VerificationResult.failure(
                    type = VerificationType.URL_EQUALS,
                    target = "URL",
                    message = "URL does not match",
                    expected = expectedUrl,
                    actual = actualUrl,
                    errorDetails = matchResult.message
                )
            }
        }
        return result.copy(durationMs = duration)
    }

    /**
     * Verify current URL contains expected substring.
     */
    fun verifyUrlContains(
        expectedText: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            val actualUrl = try {
                driver.currentUrl
            } catch (e: Exception) {
                null
            }

            val matchResult = matcher.match(actualUrl, expectedText, ComparisonOperator.CONTAINS)

            result = if (matchResult.matched) {
                VerificationResult.success(
                    type = VerificationType.URL_CONTAINS,
                    target = "URL",
                    message = "URL contains expected text",
                    expected = expectedText,
                    actual = actualUrl
                )
            } else {
                VerificationResult.failure(
                    type = VerificationType.URL_CONTAINS,
                    target = "URL",
                    message = "URL does not contain expected text",
                    expected = expectedText,
                    actual = actualUrl,
                    errorDetails = matchResult.message
                )
            }
        }
        return result.copy(durationMs = duration)
    }

    /**
     * Verify URL matches regex pattern.
     */
    fun verifyUrlMatchesRegex(
        pattern: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            val actualUrl = try {
                driver.currentUrl
            } catch (e: Exception) {
                null
            }

            val matchResult = matcher.match(actualUrl, pattern, ComparisonOperator.MATCHES_REGEX)

            result = if (matchResult.matched) {
                VerificationResult.success(
                    type = VerificationType.URL_CONTAINS,
                    target = "URL",
                    message = "URL matches pattern",
                    expected = pattern,
                    actual = actualUrl
                )
            } else {
                VerificationResult.failure(
                    type = VerificationType.URL_CONTAINS,
                    target = "URL",
                    message = "URL does not match pattern",
                    expected = pattern,
                    actual = actualUrl,
                    errorDetails = matchResult.message
                )
            }
        }
        return result.copy(durationMs = duration)
    }

    // ═══════════════════════════════════════════════════════════════
    // NUMERIC TEXT VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verify element text as number equals expected value.
     */
    fun verifyNumericValue(
        target: String,
        expectedValue: Number,
        operator: ComparisonOperator = ComparisonOperator.EQUALS,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            val element = findElement(target, context.timeoutMs)

            result = if (element == null) {
                VerificationResult.failure(
                    type = VerificationType.VALUE_EQUALS,
                    target = target,
                    message = "Element not found: $target"
                )
            } else {
                val textValue = getElementText(element)
                val numericValue = extractNumber(textValue)

                if (numericValue == null) {
                    VerificationResult.failure(
                        type = VerificationType.VALUE_EQUALS,
                        target = target,
                        message = "Element text is not a valid number: '$textValue'",
                        actual = textValue,
                        expected = expectedValue
                    )
                } else {
                    val matchResult = matcher.match(numericValue, expectedValue, operator)

                    if (matchResult.matched) {
                        VerificationResult.success(
                            type = VerificationType.VALUE_EQUALS,
                            target = target,
                            message = "Numeric verification passed: ${matchResult.message}",
                            expected = expectedValue,
                            actual = numericValue
                        )
                    } else {
                        VerificationResult.failure(
                            type = VerificationType.VALUE_EQUALS,
                            target = target,
                            message = "Numeric verification failed",
                            expected = expectedValue,
                            actual = numericValue,
                            errorDetails = matchResult.message,
                            screenshot = captureScreenshot(context)
                        )
                    }
                }
            }
        }
        return result.copy(durationMs = duration)
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════

    private fun verifyElementText(
        target: String,
        expectedText: String,
        operator: ComparisonOperator,
        type: VerificationType,
        context: VerificationContext
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            result = withRetry(context) {
                val element = findElement(target, context.timeoutMs)

                if (element == null) {
                    return@withRetry VerificationResult.failure(
                        type = type,
                        target = target,
                        message = "Element not found: $target"
                    )
                }

                val actualText = getElementText(element)
                val matchResult = createMatcher(context).match(actualText, expectedText, operator)

                if (matchResult.matched) {
                    VerificationResult.success(
                        type = type,
                        target = target,
                        message = "Text verification passed: ${matchResult.message}",
                        expected = expectedText,
                        actual = actualText
                    )
                } else {
                    VerificationResult.failure(
                        type = type,
                        target = target,
                        message = "Text verification failed",
                        expected = expectedText,
                        actual = actualText,
                        errorDetails = matchResult.message,
                        screenshot = captureScreenshot(context)
                    )
                }
            }
        }
        return result.copy(durationMs = duration)
    }

    private fun findElement(target: String, timeoutMs: Long): WebElement? {
        return try {
            elementFinder.find(target, timeoutMs)
        } catch (e: Exception) {
            logger.debug("Element find failed: ${e.message}")
            null
        }
    }

    private fun getElementText(element: WebElement): String {
        return try {
            val text = element.text
            if (text.isNullOrEmpty()) {
                // Try getting value attribute (for input fields)
                element.getAttribute("value") ?: ""
            } else {
                text
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun isDisplayed(element: WebElement): Boolean {
        return try {
            element.isDisplayed
        } catch (e: Exception) {
            false
        }
    }

    private fun extractNumber(text: String): Double? {
        // Extract numeric value from text (handles currency, percentages, etc.)
        val numericPattern = Regex("""[-+]?\d*\.?\d+""")
        return numericPattern.find(text.replace(",", ""))?.value?.toDoubleOrNull()
    }

    private fun createMatcher(context: VerificationContext): VerificationMatcher {
        return VerificationMatcher(
            caseSensitive = context.caseSensitive,
            trimWhitespace = context.trimWhitespace
        )
    }

    private fun <T> withRetry(context: VerificationContext, action: () -> T): T {
        var lastException: Exception? = null
        var lastResult: T? = null

        repeat(context.retryCount + 1) { attempt ->
            try {
                lastResult = action()
                if (lastResult is VerificationResult && (lastResult as VerificationResult).passed) {
                    return lastResult!!
                }
                if (attempt < context.retryCount) {
                    Thread.sleep(context.retryDelayMs)
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < context.retryCount) {
                    Thread.sleep(context.retryDelayMs)
                }
            }
        }

        return lastResult ?: throw (lastException ?: RuntimeException("Verification failed"))
    }

    private fun captureScreenshot(context: VerificationContext): String? {
        if (!context.screenshotOnFailure) return null

        return try {
            (driver as? TakesScreenshot)?.getScreenshotAs(OutputType.BASE64)
        } catch (e: Exception) {
            logger.debug("Screenshot capture failed: ${e.message}")
            null
        }
    }
}
