package com.testzen.core.verification

import com.testzen.core.execution.ElementFinder
import com.testzen.core.locator.smart.ActionType
import org.openqa.selenium.By
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.slf4j.LoggerFactory
import kotlin.system.measureTimeMillis

/**
 * Performs element-based verifications.
 *
 * Supports verifying:
 * - Element presence and visibility
 * - Element state (enabled, selected, checked, focused)
 * - Element attributes
 * - CSS properties
 * - Element count
 *
 * Single Responsibility: Element-based verification operations.
 */
class ElementVerifier(
    private val driver: WebDriver,
    private val elementFinder: ElementFinder,
    private val matcher: VerificationMatcher = VerificationMatcher.default
) {
    private val logger = LoggerFactory.getLogger(ElementVerifier::class.java)

    // ═══════════════════════════════════════════════════════════════
    // PRESENCE/VISIBILITY VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verify element is displayed on screen.
     */
    fun verifyDisplayed(
        target: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            result = withRetry(context) {
                val element = findElement(target, context.timeoutMs)

                if (element == null) {
                    VerificationResult.failure(
                        type = VerificationType.DISPLAYED,
                        target = target,
                        message = "Element not found: $target",
                        errorDetails = "Element could not be located using any strategy"
                    )
                } else if (!isDisplayed(element)) {
                    VerificationResult.failure(
                        type = VerificationType.DISPLAYED,
                        target = target,
                        message = "Element exists but is not displayed: $target",
                        actual = "hidden/invisible",
                        expected = "displayed"
                    )
                } else {
                    VerificationResult.success(
                        type = VerificationType.DISPLAYED,
                        target = target,
                        message = "Element is displayed: $target"
                    )
                }
            }
        }
        return result.copy(durationMs = duration)
    }

    /**
     * Verify element is NOT displayed (hidden or not present).
     */
    fun verifyNotDisplayed(
        target: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            // Use shorter timeout for "not displayed" checks
            val shortTimeout = context.timeoutMs / 3

            val element = try {
                findElement(target, shortTimeout)
            } catch (e: Exception) {
                null
            }

            result = if (element == null || !isDisplayed(element)) {
                VerificationResult.success(
                    type = VerificationType.NOT_DISPLAYED,
                    target = target,
                    message = "Element is not displayed: $target"
                )
            } else {
                VerificationResult.failure(
                    type = VerificationType.NOT_DISPLAYED,
                    target = target,
                    message = "Element is displayed but should be hidden: $target",
                    actual = "displayed",
                    expected = "not displayed",
                    screenshot = captureScreenshot(context)
                )
            }
        }
        return result.copy(durationMs = duration)
    }

    /**
     * Verify element exists in DOM (may or may not be visible).
     */
    fun verifyExists(
        target: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            result = withRetry(context) {
                val element = findElement(target, context.timeoutMs)

                if (element != null) {
                    VerificationResult.success(
                        type = VerificationType.EXISTS,
                        target = target,
                        message = "Element exists: $target"
                    )
                } else {
                    VerificationResult.failure(
                        type = VerificationType.EXISTS,
                        target = target,
                        message = "Element does not exist: $target"
                    )
                }
            }
        }
        return result.copy(durationMs = duration)
    }

    /**
     * Verify element does NOT exist in DOM.
     */
    fun verifyNotExists(
        target: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            val shortTimeout = context.timeoutMs / 3

            val element = try {
                findElement(target, shortTimeout)
            } catch (e: Exception) {
                null
            }

            result = if (element == null) {
                VerificationResult.success(
                    type = VerificationType.NOT_EXISTS,
                    target = target,
                    message = "Element does not exist: $target"
                )
            } else {
                VerificationResult.failure(
                    type = VerificationType.NOT_EXISTS,
                    target = target,
                    message = "Element exists but should not: $target",
                    screenshot = captureScreenshot(context)
                )
            }
        }
        return result.copy(durationMs = duration)
    }

    // ═══════════════════════════════════════════════════════════════
    // STATE VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verify element is enabled.
     */
    fun verifyEnabled(
        target: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        return verifyElementState(
            target = target,
            type = VerificationType.ENABLED,
            context = context,
            stateCheck = { it.isEnabled },
            stateName = "enabled"
        )
    }

    /**
     * Verify element is disabled.
     */
    fun verifyDisabled(
        target: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        return verifyElementState(
            target = target,
            type = VerificationType.DISABLED,
            context = context,
            stateCheck = { !it.isEnabled },
            stateName = "disabled"
        )
    }

    /**
     * Verify element is selected (for dropdowns, radio buttons, etc.).
     */
    fun verifySelected(
        target: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        return verifyElementState(
            target = target,
            type = VerificationType.SELECTED,
            context = context,
            stateCheck = { it.isSelected },
            stateName = "selected"
        )
    }

    /**
     * Verify element is NOT selected.
     */
    fun verifyNotSelected(
        target: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        return verifyElementState(
            target = target,
            type = VerificationType.NOT_SELECTED,
            context = context,
            stateCheck = { !it.isSelected },
            stateName = "not selected"
        )
    }

    /**
     * Verify checkbox/toggle is checked.
     */
    fun verifyChecked(
        target: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        return verifyElementState(
            target = target,
            type = VerificationType.CHECKED,
            context = context,
            stateCheck = { isChecked(it) },
            stateName = "checked"
        )
    }

    /**
     * Verify checkbox/toggle is NOT checked.
     */
    fun verifyNotChecked(
        target: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        return verifyElementState(
            target = target,
            type = VerificationType.NOT_CHECKED,
            context = context,
            stateCheck = { !isChecked(it) },
            stateName = "not checked"
        )
    }

    /**
     * Verify element has focus.
     */
    fun verifyFocused(
        target: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        return verifyElementState(
            target = target,
            type = VerificationType.FOCUSED,
            context = context,
            stateCheck = { isFocused(it) },
            stateName = "focused"
        )
    }

    /**
     * Verify element does NOT have focus.
     */
    fun verifyNotFocused(
        target: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        return verifyElementState(
            target = target,
            type = VerificationType.NOT_FOCUSED,
            context = context,
            stateCheck = { !isFocused(it) },
            stateName = "not focused"
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // ATTRIBUTE VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verify element attribute equals expected value.
     */
    fun verifyAttribute(
        target: String,
        attributeName: String,
        expectedValue: String,
        operator: ComparisonOperator = ComparisonOperator.EQUALS,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            result = withRetry(context) {
                val element = findElement(target, context.timeoutMs)

                if (element == null) {
                    return@withRetry VerificationResult.failure(
                        type = VerificationType.ATTRIBUTE_EQUALS,
                        target = target,
                        message = "Element not found: $target"
                    )
                }

                val actualValue = element.getAttribute(attributeName)
                val matchResult = matcher.match(actualValue, expectedValue, operator)

                if (matchResult.matched) {
                    VerificationResult.success(
                        type = VerificationType.ATTRIBUTE_EQUALS,
                        target = target,
                        message = "Attribute '$attributeName' verification passed",
                        expected = expectedValue,
                        actual = actualValue,
                        metadata = mapOf("attribute" to attributeName)
                    )
                } else {
                    VerificationResult.failure(
                        type = VerificationType.ATTRIBUTE_EQUALS,
                        target = target,
                        message = "Attribute '$attributeName' verification failed",
                        expected = expectedValue,
                        actual = actualValue,
                        errorDetails = matchResult.message,
                        metadata = mapOf("attribute" to attributeName)
                    )
                }
            }
        }
        return result.copy(durationMs = duration)
    }

    /**
     * Verify attribute exists on element.
     */
    fun verifyAttributeExists(
        target: String,
        attributeName: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            val element = findElement(target, context.timeoutMs)

            result = if (element == null) {
                VerificationResult.failure(
                    type = VerificationType.ATTRIBUTE_EXISTS,
                    target = target,
                    message = "Element not found: $target"
                )
            } else {
                val attrValue = element.getAttribute(attributeName)
                if (attrValue != null) {
                    VerificationResult.success(
                        type = VerificationType.ATTRIBUTE_EXISTS,
                        target = target,
                        message = "Attribute '$attributeName' exists on element",
                        actual = attrValue,
                        metadata = mapOf("attribute" to attributeName)
                    )
                } else {
                    VerificationResult.failure(
                        type = VerificationType.ATTRIBUTE_EXISTS,
                        target = target,
                        message = "Attribute '$attributeName' does not exist on element",
                        metadata = mapOf("attribute" to attributeName)
                    )
                }
            }
        }
        return result.copy(durationMs = duration)
    }

    // ═══════════════════════════════════════════════════════════════
    // CSS PROPERTY VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verify CSS property value.
     */
    fun verifyCssProperty(
        target: String,
        propertyName: String,
        expectedValue: String,
        operator: ComparisonOperator = ComparisonOperator.CONTAINS,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            val element = findElement(target, context.timeoutMs)

            result = if (element == null) {
                VerificationResult.failure(
                    type = VerificationType.CSS_PROPERTY_EQUALS,
                    target = target,
                    message = "Element not found: $target"
                )
            } else {
                val actualValue = element.getCssValue(propertyName)
                val matchResult = matcher.match(actualValue, expectedValue, operator)

                if (matchResult.matched) {
                    VerificationResult.success(
                        type = VerificationType.CSS_PROPERTY_EQUALS,
                        target = target,
                        message = "CSS property '$propertyName' verification passed",
                        expected = expectedValue,
                        actual = actualValue,
                        metadata = mapOf("cssProperty" to propertyName)
                    )
                } else {
                    VerificationResult.failure(
                        type = VerificationType.CSS_PROPERTY_EQUALS,
                        target = target,
                        message = "CSS property '$propertyName' verification failed",
                        expected = expectedValue,
                        actual = actualValue,
                        errorDetails = matchResult.message,
                        metadata = mapOf("cssProperty" to propertyName)
                    )
                }
            }
        }
        return result.copy(durationMs = duration)
    }

    // ═══════════════════════════════════════════════════════════════
    // COUNT VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verify element count.
     */
    fun verifyCount(
        target: String,
        expectedCount: Int,
        operator: ComparisonOperator = ComparisonOperator.EQUALS,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            val elements = findAllElements(target)
            val actualCount = elements.size
            val matchResult = matcher.match(actualCount, expectedCount, operator)

            result = if (matchResult.matched) {
                VerificationResult.success(
                    type = VerificationType.COUNT_EQUALS,
                    target = target,
                    message = "Element count verification passed: $actualCount elements",
                    expected = expectedCount,
                    actual = actualCount
                )
            } else {
                VerificationResult.failure(
                    type = VerificationType.COUNT_EQUALS,
                    target = target,
                    message = "Element count verification failed",
                    expected = expectedCount,
                    actual = actualCount,
                    errorDetails = matchResult.message,
                    screenshot = captureScreenshot(context)
                )
            }
        }
        return result.copy(durationMs = duration)
    }

    /**
     * Verify element count is within range.
     */
    fun verifyCountBetween(
        target: String,
        minCount: Int,
        maxCount: Int,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            val elements = findAllElements(target)
            val actualCount = elements.size
            val matchResult = matcher.match(actualCount, minCount to maxCount, ComparisonOperator.BETWEEN)

            result = if (matchResult.matched) {
                VerificationResult.success(
                    type = VerificationType.COUNT_BETWEEN,
                    target = target,
                    message = "Element count ($actualCount) is between $minCount and $maxCount",
                    expected = "$minCount-$maxCount",
                    actual = actualCount
                )
            } else {
                VerificationResult.failure(
                    type = VerificationType.COUNT_BETWEEN,
                    target = target,
                    message = "Element count ($actualCount) is not between $minCount and $maxCount",
                    expected = "$minCount-$maxCount",
                    actual = actualCount,
                    screenshot = captureScreenshot(context)
                )
            }
        }
        return result.copy(durationMs = duration)
    }

    // ═══════════════════════════════════════════════════════════════
    // COLLECTION VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verify all matching elements are displayed.
     */
    fun verifyAllDisplayed(
        target: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            val elements = findAllElements(target)

            if (elements.isEmpty()) {
                result = VerificationResult.failure(
                    type = VerificationType.ALL_DISPLAYED,
                    target = target,
                    message = "No elements found matching: $target"
                )
            } else {
                val displayedCount = elements.count { isDisplayed(it) }
                val allDisplayed = displayedCount == elements.size

                result = if (allDisplayed) {
                    VerificationResult.success(
                        type = VerificationType.ALL_DISPLAYED,
                        target = target,
                        message = "All ${elements.size} elements are displayed",
                        actual = elements.size,
                        metadata = mapOf("displayedCount" to displayedCount)
                    )
                } else {
                    VerificationResult.failure(
                        type = VerificationType.ALL_DISPLAYED,
                        target = target,
                        message = "Only $displayedCount of ${elements.size} elements are displayed",
                        expected = "all ${elements.size} displayed",
                        actual = "$displayedCount displayed",
                        screenshot = captureScreenshot(context)
                    )
                }
            }
        }
        return result.copy(durationMs = duration)
    }

    /**
     * Verify at least one matching element is displayed.
     */
    fun verifyAnyDisplayed(
        target: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            val elements = findAllElements(target)
            val anyDisplayed = elements.any { isDisplayed(it) }

            result = if (anyDisplayed) {
                VerificationResult.success(
                    type = VerificationType.ANY_DISPLAYED,
                    target = target,
                    message = "At least one element is displayed"
                )
            } else {
                VerificationResult.failure(
                    type = VerificationType.ANY_DISPLAYED,
                    target = target,
                    message = "No elements are displayed (found ${elements.size} hidden/missing)",
                    screenshot = captureScreenshot(context)
                )
            }
        }
        return result.copy(durationMs = duration)
    }

    /**
     * Verify all matching elements contain specific text.
     */
    fun verifyAllContainText(
        target: String,
        expectedText: String,
        context: VerificationContext = VerificationContext()
    ): VerificationResult {
        var result: VerificationResult
        val duration = measureTimeMillis {
            val elements = findAllElements(target)

            if (elements.isEmpty()) {
                result = VerificationResult.failure(
                    type = VerificationType.ALL_CONTAIN_TEXT,
                    target = target,
                    message = "No elements found matching: $target"
                )
            } else {
                val elementsWithText = elements.filter {
                    val text = it.text ?: ""
                    text.contains(expectedText, ignoreCase = !context.caseSensitive)
                }
                val allContain = elementsWithText.size == elements.size

                result = if (allContain) {
                    VerificationResult.success(
                        type = VerificationType.ALL_CONTAIN_TEXT,
                        target = target,
                        message = "All ${elements.size} elements contain '$expectedText'",
                        expected = expectedText
                    )
                } else {
                    VerificationResult.failure(
                        type = VerificationType.ALL_CONTAIN_TEXT,
                        target = target,
                        message = "Only ${elementsWithText.size} of ${elements.size} elements contain '$expectedText'",
                        expected = expectedText,
                        actual = "${elementsWithText.size}/${elements.size} contain text",
                        screenshot = captureScreenshot(context)
                    )
                }
            }
        }
        return result.copy(durationMs = duration)
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════

    private fun findElement(target: String, timeoutMs: Long): WebElement? {
        return try {
            elementFinder.find(target, timeoutMs)
        } catch (e: Exception) {
            logger.debug("Element find failed: ${e.message}")
            null
        }
    }

    private fun findAllElements(target: String): List<WebElement> {
        return try {
            // Try multiple locator strategies
            val elements = mutableListOf<WebElement>()

            // By text contains
            try {
                elements.addAll(driver.findElements(By.xpath("//*[contains(text(), '$target')]")))
            } catch (e: Exception) { /* ignore */ }

            // By content-desc (Android)
            try {
                elements.addAll(driver.findElements(By.xpath("//*[contains(@content-desc, '$target')]")))
            } catch (e: Exception) { /* ignore */ }

            // By accessibility ID
            try {
                elements.addAll(driver.findElements(By.xpath("//*[@accessibility-id='$target']")))
            } catch (e: Exception) { /* ignore */ }

            // By ID contains
            try {
                elements.addAll(driver.findElements(By.xpath("//*[contains(@resource-id, '$target')]")))
            } catch (e: Exception) { /* ignore */ }

            // By class name
            try {
                elements.addAll(driver.findElements(By.className(target)))
            } catch (e: Exception) { /* ignore */ }

            elements.distinctBy { it.location to it.size }
        } catch (e: Exception) {
            logger.debug("Find all elements failed: ${e.message}")
            emptyList()
        }
    }

    private fun verifyElementState(
        target: String,
        type: VerificationType,
        context: VerificationContext,
        stateCheck: (WebElement) -> Boolean,
        stateName: String
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

                val stateMatches = try {
                    stateCheck(element)
                } catch (e: Exception) {
                    logger.debug("State check failed: ${e.message}")
                    false
                }

                if (stateMatches) {
                    VerificationResult.success(
                        type = type,
                        target = target,
                        message = "Element is $stateName: $target"
                    )
                } else {
                    VerificationResult.failure(
                        type = type,
                        target = target,
                        message = "Element is not $stateName: $target",
                        expected = stateName,
                        actual = "not $stateName",
                        screenshot = captureScreenshot(context)
                    )
                }
            }
        }
        return result.copy(durationMs = duration)
    }

    private fun isDisplayed(element: WebElement): Boolean {
        return try {
            element.isDisplayed
        } catch (e: Exception) {
            false
        }
    }

    private fun isChecked(element: WebElement): Boolean {
        return try {
            element.isSelected ||
                element.getAttribute("checked") == "true" ||
                element.getAttribute("aria-checked") == "true"
        } catch (e: Exception) {
            false
        }
    }

    private fun isFocused(element: WebElement): Boolean {
        return try {
            element == driver.switchTo().activeElement()
        } catch (e: Exception) {
            false
        }
    }

    private fun <T> withRetry(context: VerificationContext, action: () -> T): T {
        var lastException: Exception? = null
        var lastResult: T? = null

        repeat(context.retryCount + 1) { attempt ->
            try {
                lastResult = action()
                // If result is a VerificationResult, check if passed
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
