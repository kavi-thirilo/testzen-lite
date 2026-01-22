package com.testzen.core.verification

import org.openqa.selenium.WebElement
import java.time.Instant

/**
 * Types of verification operations supported.
 */
enum class VerificationType {
    // Element presence/visibility
    DISPLAYED,
    NOT_DISPLAYED,
    EXISTS,
    NOT_EXISTS,

    // Element state
    ENABLED,
    DISABLED,
    SELECTED,
    NOT_SELECTED,
    CHECKED,
    NOT_CHECKED,
    FOCUSED,
    NOT_FOCUSED,

    // Text verification
    TEXT_EQUALS,
    TEXT_CONTAINS,
    TEXT_STARTS_WITH,
    TEXT_ENDS_WITH,
    TEXT_MATCHES_REGEX,
    TEXT_NOT_CONTAINS,
    TEXT_IS_EMPTY,
    TEXT_IS_NOT_EMPTY,

    // Attribute verification
    ATTRIBUTE_EQUALS,
    ATTRIBUTE_CONTAINS,
    ATTRIBUTE_EXISTS,
    ATTRIBUTE_NOT_EXISTS,

    // CSS property verification
    CSS_PROPERTY_EQUALS,
    CSS_PROPERTY_CONTAINS,

    // Count/numeric verification
    COUNT_EQUALS,
    COUNT_GREATER_THAN,
    COUNT_LESS_THAN,
    COUNT_GREATER_OR_EQUAL,
    COUNT_LESS_OR_EQUAL,
    COUNT_BETWEEN,

    // Collection verification
    ALL_DISPLAYED,
    ANY_DISPLAYED,
    NONE_DISPLAYED,
    ALL_CONTAIN_TEXT,
    ANY_CONTAINS_TEXT,

    // Comparison verification
    VALUE_EQUALS,
    VALUE_NOT_EQUALS,
    VALUE_GREATER_THAN,
    VALUE_LESS_THAN,

    // Page/screen verification
    PAGE_TITLE_EQUALS,
    PAGE_TITLE_CONTAINS,
    URL_EQUALS,
    URL_CONTAINS,
    PAGE_SOURCE_CONTAINS,

    // Custom
    CUSTOM
}

/**
 * Comparison operators for assertions.
 */
enum class ComparisonOperator {
    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    NOT_CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    MATCHES_REGEX,
    GREATER_THAN,
    LESS_THAN,
    GREATER_OR_EQUAL,
    LESS_OR_EQUAL,
    BETWEEN,
    IS_EMPTY,
    IS_NOT_EMPTY,
    IS_NULL,
    IS_NOT_NULL,
    IS_TRUE,
    IS_FALSE
}

/**
 * Result of a verification operation.
 *
 * Provides detailed information about what was verified, expected vs actual values,
 * and context for debugging failures.
 */
data class VerificationResult(
    /** Whether the verification passed */
    val passed: Boolean,

    /** Type of verification performed */
    val verificationType: VerificationType,

    /** Target element/field that was verified */
    val target: String,

    /** Expected value (for comparison verifications) */
    val expected: Any? = null,

    /** Actual value found */
    val actual: Any? = null,

    /** Human-readable message describing the result */
    val message: String,

    /** Detailed error message for failures */
    val errorDetails: String? = null,

    /** Whether this was a soft assertion (non-blocking) */
    val isSoftAssertion: Boolean = false,

    /** Time taken for this verification in milliseconds */
    val durationMs: Long = 0,

    /** Timestamp when verification was performed */
    val timestamp: Instant = Instant.now(),

    /** Screenshot captured during verification (base64) */
    val screenshot: String? = null,

    /** Additional metadata */
    val metadata: Map<String, Any> = emptyMap()
) {
    val failed: Boolean get() = !passed

    companion object {
        fun success(
            type: VerificationType,
            target: String,
            message: String,
            expected: Any? = null,
            actual: Any? = null,
            durationMs: Long = 0,
            metadata: Map<String, Any> = emptyMap()
        ) = VerificationResult(
            passed = true,
            verificationType = type,
            target = target,
            expected = expected,
            actual = actual,
            message = message,
            durationMs = durationMs,
            metadata = metadata
        )

        fun failure(
            type: VerificationType,
            target: String,
            message: String,
            expected: Any? = null,
            actual: Any? = null,
            errorDetails: String? = null,
            durationMs: Long = 0,
            screenshot: String? = null,
            metadata: Map<String, Any> = emptyMap()
        ) = VerificationResult(
            passed = false,
            verificationType = type,
            target = target,
            expected = expected,
            actual = actual,
            message = message,
            errorDetails = errorDetails,
            durationMs = durationMs,
            screenshot = screenshot,
            metadata = metadata
        )

        fun softFailure(
            type: VerificationType,
            target: String,
            message: String,
            expected: Any? = null,
            actual: Any? = null,
            errorDetails: String? = null
        ) = VerificationResult(
            passed = false,
            verificationType = type,
            target = target,
            expected = expected,
            actual = actual,
            message = message,
            errorDetails = errorDetails,
            isSoftAssertion = true
        )
    }

    override fun toString(): String {
        val status = if (passed) "PASS" else "FAIL"
        val soft = if (isSoftAssertion) " (soft)" else ""
        return "[$status$soft] $verificationType: $message" +
            (if (!passed && errorDetails != null) " - $errorDetails" else "")
    }
}

/**
 * Context for verification operations.
 */
data class VerificationContext(
    /** Current page/screen name */
    val pageName: String? = null,

    /** Step number in test */
    val stepNumber: Int? = null,

    /** Test case ID */
    val testCaseId: String? = null,

    /** Timeout for verification in milliseconds */
    val timeoutMs: Long = 10000,

    /** Retry count for flaky verifications */
    val retryCount: Int = 0,

    /** Delay between retries in milliseconds */
    val retryDelayMs: Long = 500,

    /** Capture screenshot on failure */
    val screenshotOnFailure: Boolean = true,

    /** Use soft assertions (collect all failures instead of stopping) */
    val softAssertions: Boolean = false,

    /** Case-sensitive text comparisons */
    val caseSensitive: Boolean = false,

    /** Trim whitespace before comparison */
    val trimWhitespace: Boolean = true,

    /** Additional context data */
    val data: Map<String, Any> = emptyMap(),

    // ═══════════════════════════════════════════════════════════════
    // STABILITY SETTINGS
    // ═══════════════════════════════════════════════════════════════

    /** Wait for element position/size to stabilize before verification */
    val waitForStability: Boolean = true,

    /** Use exponential backoff for retries instead of fixed delay */
    val useExponentialBackoff: Boolean = true,

    /** Wait for page to be ready (network idle, DOM stable) before verification */
    val waitForPageReady: Boolean = false,

    /** Maximum time to wait for element stability in milliseconds */
    val stabilityTimeoutMs: Long = 3000,

    /** Scroll to find element if not visible */
    val scrollToFind: Boolean = false
)

/**
 * Aggregated results from multiple verifications.
 */
data class VerificationReport(
    /** All verification results */
    val results: List<VerificationResult>,

    /** Total verifications performed */
    val totalCount: Int = results.size,

    /** Number of passed verifications */
    val passedCount: Int = results.count { it.passed },

    /** Number of failed verifications */
    val failedCount: Int = results.count { it.failed },

    /** Number of soft assertion failures */
    val softFailureCount: Int = results.count { it.failed && it.isSoftAssertion },

    /** Total duration in milliseconds */
    val totalDurationMs: Long = results.sumOf { it.durationMs },

    /** Overall pass/fail status */
    val allPassed: Boolean = results.all { it.passed || it.isSoftAssertion }
) {
    val passRate: Double get() = if (totalCount > 0) passedCount.toDouble() / totalCount else 1.0

    fun getFailures(): List<VerificationResult> = results.filter { it.failed }

    fun getHardFailures(): List<VerificationResult> = results.filter { it.failed && !it.isSoftAssertion }

    fun getSoftFailures(): List<VerificationResult> = results.filter { it.failed && it.isSoftAssertion }

    fun getByType(type: VerificationType): List<VerificationResult> =
        results.filter { it.verificationType == type }

    fun summary(): String {
        return "Verification Report: $passedCount/$totalCount passed (${(passRate * 100).toInt()}%), " +
            "$failedCount failed, ${totalDurationMs}ms total"
    }
}

/**
 * Specification for a verification to be performed.
 */
data class VerificationSpec(
    /** Type of verification */
    val type: VerificationType,

    /** Target element or field */
    val target: String,

    /** Expected value */
    val expected: Any? = null,

    /** Comparison operator */
    val operator: ComparisonOperator = ComparisonOperator.EQUALS,

    /** Attribute name (for attribute verifications) */
    val attributeName: String? = null,

    /** CSS property name (for CSS verifications) */
    val cssProperty: String? = null,

    /** Whether this is a soft assertion */
    val soft: Boolean = false,

    /** Custom error message */
    val customMessage: String? = null,

    /** Context overrides */
    val contextOverrides: VerificationContext? = null
)

/**
 * Builder for creating verification specifications fluently.
 */
class VerificationBuilder(private val target: String) {
    private var type: VerificationType = VerificationType.DISPLAYED
    private var expected: Any? = null
    private var operator: ComparisonOperator = ComparisonOperator.EQUALS
    private var attributeName: String? = null
    private var cssProperty: String? = null
    private var soft: Boolean = false
    private var customMessage: String? = null

    // Element presence
    fun isDisplayed() = apply { type = VerificationType.DISPLAYED }
    fun isNotDisplayed() = apply { type = VerificationType.NOT_DISPLAYED }
    fun exists() = apply { type = VerificationType.EXISTS }
    fun notExists() = apply { type = VerificationType.NOT_EXISTS }

    // Element state
    fun isEnabled() = apply { type = VerificationType.ENABLED }
    fun isDisabled() = apply { type = VerificationType.DISABLED }
    fun isSelected() = apply { type = VerificationType.SELECTED }
    fun isNotSelected() = apply { type = VerificationType.NOT_SELECTED }
    fun isChecked() = apply { type = VerificationType.CHECKED }
    fun isNotChecked() = apply { type = VerificationType.NOT_CHECKED }
    fun isFocused() = apply { type = VerificationType.FOCUSED }
    fun isNotFocused() = apply { type = VerificationType.NOT_FOCUSED }

    // Text verification
    fun hasText(text: String) = apply {
        type = VerificationType.TEXT_EQUALS
        expected = text
    }
    fun containsText(text: String) = apply {
        type = VerificationType.TEXT_CONTAINS
        expected = text
    }
    fun textStartsWith(text: String) = apply {
        type = VerificationType.TEXT_STARTS_WITH
        expected = text
    }
    fun textEndsWith(text: String) = apply {
        type = VerificationType.TEXT_ENDS_WITH
        expected = text
    }
    fun textMatches(regex: String) = apply {
        type = VerificationType.TEXT_MATCHES_REGEX
        expected = regex
    }
    fun textDoesNotContain(text: String) = apply {
        type = VerificationType.TEXT_NOT_CONTAINS
        expected = text
    }
    fun textIsEmpty() = apply { type = VerificationType.TEXT_IS_EMPTY }
    fun textIsNotEmpty() = apply { type = VerificationType.TEXT_IS_NOT_EMPTY }

    // Attribute verification
    fun hasAttribute(name: String, value: String) = apply {
        type = VerificationType.ATTRIBUTE_EQUALS
        attributeName = name
        expected = value
    }
    fun attributeContains(name: String, value: String) = apply {
        type = VerificationType.ATTRIBUTE_CONTAINS
        attributeName = name
        expected = value
    }
    fun hasAttributePresent(name: String) = apply {
        type = VerificationType.ATTRIBUTE_EXISTS
        attributeName = name
    }
    fun attributeNotPresent(name: String) = apply {
        type = VerificationType.ATTRIBUTE_NOT_EXISTS
        attributeName = name
    }

    // CSS verification
    fun hasCssProperty(property: String, value: String) = apply {
        type = VerificationType.CSS_PROPERTY_EQUALS
        cssProperty = property
        expected = value
    }
    fun cssPropertyContains(property: String, value: String) = apply {
        type = VerificationType.CSS_PROPERTY_CONTAINS
        cssProperty = property
        expected = value
    }

    // Count verification
    fun countEquals(count: Int) = apply {
        type = VerificationType.COUNT_EQUALS
        expected = count
    }
    fun countGreaterThan(count: Int) = apply {
        type = VerificationType.COUNT_GREATER_THAN
        expected = count
    }
    fun countLessThan(count: Int) = apply {
        type = VerificationType.COUNT_LESS_THAN
        expected = count
    }
    fun countBetween(min: Int, max: Int) = apply {
        type = VerificationType.COUNT_BETWEEN
        expected = min to max
    }

    // Soft assertion
    fun asSoft() = apply { soft = true }

    // Custom message
    fun withMessage(message: String) = apply { customMessage = message }

    fun build(): VerificationSpec = VerificationSpec(
        type = type,
        target = target,
        expected = expected,
        operator = operator,
        attributeName = attributeName,
        cssProperty = cssProperty,
        soft = soft,
        customMessage = customMessage
    )

    companion object {
        fun verify(target: String) = VerificationBuilder(target)
    }
}
