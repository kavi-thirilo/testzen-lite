package com.testzen.core.verification

import org.slf4j.LoggerFactory

/**
 * Provides matching operations for verification assertions.
 *
 * Supports multiple comparison types including:
 * - Exact and partial string matching
 * - Regex pattern matching
 * - Numeric comparisons
 * - Collection operations
 * - Null/empty checks
 *
 * Single Responsibility: Perform comparison operations for assertions.
 */
class VerificationMatcher(
    private val caseSensitive: Boolean = false,
    private val trimWhitespace: Boolean = true
) {
    private val logger = LoggerFactory.getLogger(VerificationMatcher::class.java)

    /**
     * Match two values based on comparison operator.
     *
     * @param actual The actual value found
     * @param expected The expected value
     * @param operator The comparison operator to use
     * @return MatchResult with pass/fail and details
     */
    fun match(actual: Any?, expected: Any?, operator: ComparisonOperator): MatchResult {
        return try {
            when (operator) {
                ComparisonOperator.EQUALS -> matchEquals(actual, expected)
                ComparisonOperator.NOT_EQUALS -> matchNotEquals(actual, expected)
                ComparisonOperator.CONTAINS -> matchContains(actual, expected)
                ComparisonOperator.NOT_CONTAINS -> matchNotContains(actual, expected)
                ComparisonOperator.STARTS_WITH -> matchStartsWith(actual, expected)
                ComparisonOperator.ENDS_WITH -> matchEndsWith(actual, expected)
                ComparisonOperator.MATCHES_REGEX -> matchRegex(actual, expected)
                ComparisonOperator.GREATER_THAN -> matchGreaterThan(actual, expected)
                ComparisonOperator.LESS_THAN -> matchLessThan(actual, expected)
                ComparisonOperator.GREATER_OR_EQUAL -> matchGreaterOrEqual(actual, expected)
                ComparisonOperator.LESS_OR_EQUAL -> matchLessOrEqual(actual, expected)
                ComparisonOperator.BETWEEN -> matchBetween(actual, expected)
                ComparisonOperator.IS_EMPTY -> matchIsEmpty(actual)
                ComparisonOperator.IS_NOT_EMPTY -> matchIsNotEmpty(actual)
                ComparisonOperator.IS_NULL -> matchIsNull(actual)
                ComparisonOperator.IS_NOT_NULL -> matchIsNotNull(actual)
                ComparisonOperator.IS_TRUE -> matchIsTrue(actual)
                ComparisonOperator.IS_FALSE -> matchIsFalse(actual)
            }
        } catch (e: Exception) {
            logger.error("Match operation failed: ${e.message}")
            MatchResult(
                matched = false,
                actualValue = actual?.toString(),
                expectedValue = expected?.toString(),
                message = "Match failed: ${e.message}"
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // STRING MATCHING
    // ═══════════════════════════════════════════════════════════════

    private fun matchEquals(actual: Any?, expected: Any?): MatchResult {
        val actualStr = normalizeString(actual?.toString())
        val expectedStr = normalizeString(expected?.toString())

        val matched = if (caseSensitive) {
            actualStr == expectedStr
        } else {
            actualStr.equals(expectedStr, ignoreCase = true)
        }

        return MatchResult(
            matched = matched,
            actualValue = actualStr,
            expectedValue = expectedStr,
            message = if (matched) "Values are equal" else "Expected '$expectedStr' but found '$actualStr'"
        )
    }

    private fun matchNotEquals(actual: Any?, expected: Any?): MatchResult {
        val result = matchEquals(actual, expected)
        return result.copy(
            matched = !result.matched,
            message = if (!result.matched) "Values are not equal" else "Expected different from '${result.expectedValue}' but got same value"
        )
    }

    private fun matchContains(actual: Any?, expected: Any?): MatchResult {
        val actualStr = normalizeString(actual?.toString())
        val expectedStr = normalizeString(expected?.toString())

        val matched = if (caseSensitive) {
            actualStr.contains(expectedStr)
        } else {
            actualStr.contains(expectedStr, ignoreCase = true)
        }

        return MatchResult(
            matched = matched,
            actualValue = actualStr,
            expectedValue = expectedStr,
            message = if (matched) "Text contains expected substring" else "Text '$actualStr' does not contain '$expectedStr'"
        )
    }

    private fun matchNotContains(actual: Any?, expected: Any?): MatchResult {
        val result = matchContains(actual, expected)
        return result.copy(
            matched = !result.matched,
            message = if (!result.matched) "Text does not contain substring" else "Text should not contain '${result.expectedValue}' but it does"
        )
    }

    private fun matchStartsWith(actual: Any?, expected: Any?): MatchResult {
        val actualStr = normalizeString(actual?.toString())
        val expectedStr = normalizeString(expected?.toString())

        val matched = if (caseSensitive) {
            actualStr.startsWith(expectedStr)
        } else {
            actualStr.lowercase().startsWith(expectedStr.lowercase())
        }

        return MatchResult(
            matched = matched,
            actualValue = actualStr,
            expectedValue = expectedStr,
            message = if (matched) "Text starts with expected prefix" else "Text '$actualStr' does not start with '$expectedStr'"
        )
    }

    private fun matchEndsWith(actual: Any?, expected: Any?): MatchResult {
        val actualStr = normalizeString(actual?.toString())
        val expectedStr = normalizeString(expected?.toString())

        val matched = if (caseSensitive) {
            actualStr.endsWith(expectedStr)
        } else {
            actualStr.lowercase().endsWith(expectedStr.lowercase())
        }

        return MatchResult(
            matched = matched,
            actualValue = actualStr,
            expectedValue = expectedStr,
            message = if (matched) "Text ends with expected suffix" else "Text '$actualStr' does not end with '$expectedStr'"
        )
    }

    private fun matchRegex(actual: Any?, expected: Any?): MatchResult {
        val actualStr = normalizeString(actual?.toString())
        val pattern = expected?.toString() ?: ""

        return try {
            val regex = if (caseSensitive) {
                Regex(pattern)
            } else {
                Regex(pattern, RegexOption.IGNORE_CASE)
            }

            val matched = regex.containsMatchIn(actualStr)

            MatchResult(
                matched = matched,
                actualValue = actualStr,
                expectedValue = pattern,
                message = if (matched) "Text matches regex pattern" else "Text '$actualStr' does not match pattern '$pattern'"
            )
        } catch (e: Exception) {
            MatchResult(
                matched = false,
                actualValue = actualStr,
                expectedValue = pattern,
                message = "Invalid regex pattern: ${e.message}"
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // NUMERIC MATCHING
    // ═══════════════════════════════════════════════════════════════

    private fun matchGreaterThan(actual: Any?, expected: Any?): MatchResult {
        return compareNumbers(actual, expected) { a, e -> a > e }
            .let { (matched, actualNum, expectedNum) ->
                MatchResult(
                    matched = matched,
                    actualValue = actualNum?.toString(),
                    expectedValue = expectedNum?.toString(),
                    message = if (matched) "$actualNum > $expectedNum" else "$actualNum is not greater than $expectedNum"
                )
            }
    }

    private fun matchLessThan(actual: Any?, expected: Any?): MatchResult {
        return compareNumbers(actual, expected) { a, e -> a < e }
            .let { (matched, actualNum, expectedNum) ->
                MatchResult(
                    matched = matched,
                    actualValue = actualNum?.toString(),
                    expectedValue = expectedNum?.toString(),
                    message = if (matched) "$actualNum < $expectedNum" else "$actualNum is not less than $expectedNum"
                )
            }
    }

    private fun matchGreaterOrEqual(actual: Any?, expected: Any?): MatchResult {
        return compareNumbers(actual, expected) { a, e -> a >= e }
            .let { (matched, actualNum, expectedNum) ->
                MatchResult(
                    matched = matched,
                    actualValue = actualNum?.toString(),
                    expectedValue = expectedNum?.toString(),
                    message = if (matched) "$actualNum >= $expectedNum" else "$actualNum is not >= $expectedNum"
                )
            }
    }

    private fun matchLessOrEqual(actual: Any?, expected: Any?): MatchResult {
        return compareNumbers(actual, expected) { a, e -> a <= e }
            .let { (matched, actualNum, expectedNum) ->
                MatchResult(
                    matched = matched,
                    actualValue = actualNum?.toString(),
                    expectedValue = expectedNum?.toString(),
                    message = if (matched) "$actualNum <= $expectedNum" else "$actualNum is not <= $expectedNum"
                )
            }
    }

    private fun matchBetween(actual: Any?, expected: Any?): MatchResult {
        val actualNum = toNumber(actual)
        val range = when (expected) {
            is Pair<*, *> -> {
                val min = toNumber(expected.first)
                val max = toNumber(expected.second)
                if (min != null && max != null) min to max else null
            }
            is IntRange -> expected.first.toDouble() to expected.last.toDouble()
            else -> null
        }

        if (actualNum == null || range == null) {
            return MatchResult(
                matched = false,
                actualValue = actual?.toString(),
                expectedValue = expected?.toString(),
                message = "Cannot compare: invalid number or range"
            )
        }

        val matched = actualNum >= range.first && actualNum <= range.second

        return MatchResult(
            matched = matched,
            actualValue = actualNum.toString(),
            expectedValue = "[${range.first}, ${range.second}]",
            message = if (matched) "$actualNum is between ${range.first} and ${range.second}"
            else "$actualNum is not between ${range.first} and ${range.second}"
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // NULL/EMPTY MATCHING
    // ═══════════════════════════════════════════════════════════════

    private fun matchIsEmpty(actual: Any?): MatchResult {
        val isEmpty = when (actual) {
            null -> true
            is String -> actual.trim().isEmpty()
            is Collection<*> -> actual.isEmpty()
            is Array<*> -> actual.isEmpty()
            else -> actual.toString().trim().isEmpty()
        }

        return MatchResult(
            matched = isEmpty,
            actualValue = actual?.toString() ?: "null",
            expectedValue = "empty",
            message = if (isEmpty) "Value is empty" else "Value is not empty: '$actual'"
        )
    }

    private fun matchIsNotEmpty(actual: Any?): MatchResult {
        val result = matchIsEmpty(actual)
        return result.copy(
            matched = !result.matched,
            expectedValue = "not empty",
            message = if (!result.matched) "Value is not empty" else "Value is empty"
        )
    }

    private fun matchIsNull(actual: Any?): MatchResult {
        return MatchResult(
            matched = actual == null,
            actualValue = actual?.toString() ?: "null",
            expectedValue = "null",
            message = if (actual == null) "Value is null" else "Value is not null: '$actual'"
        )
    }

    private fun matchIsNotNull(actual: Any?): MatchResult {
        return MatchResult(
            matched = actual != null,
            actualValue = actual?.toString() ?: "null",
            expectedValue = "not null",
            message = if (actual != null) "Value is not null" else "Value is null"
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // BOOLEAN MATCHING
    // ═══════════════════════════════════════════════════════════════

    private fun matchIsTrue(actual: Any?): MatchResult {
        val isTrue = when (actual) {
            is Boolean -> actual
            is String -> actual.equals("true", ignoreCase = true) || actual == "1"
            is Number -> actual.toInt() != 0
            else -> false
        }

        return MatchResult(
            matched = isTrue,
            actualValue = actual?.toString() ?: "null",
            expectedValue = "true",
            message = if (isTrue) "Value is true" else "Value is not true: '$actual'"
        )
    }

    private fun matchIsFalse(actual: Any?): MatchResult {
        val isFalse = when (actual) {
            is Boolean -> !actual
            is String -> actual.equals("false", ignoreCase = true) || actual == "0" || actual.isEmpty()
            is Number -> actual.toInt() == 0
            null -> true
            else -> false
        }

        return MatchResult(
            matched = isFalse,
            actualValue = actual?.toString() ?: "null",
            expectedValue = "false",
            message = if (isFalse) "Value is false" else "Value is not false: '$actual'"
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════

    private fun normalizeString(str: String?): String {
        if (str == null) return ""
        return if (trimWhitespace) str.trim() else str
    }

    private fun toNumber(value: Any?): Double? {
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    private fun compareNumbers(
        actual: Any?,
        expected: Any?,
        comparison: (Double, Double) -> Boolean
    ): Triple<Boolean, Double?, Double?> {
        val actualNum = toNumber(actual)
        val expectedNum = toNumber(expected)

        if (actualNum == null || expectedNum == null) {
            return Triple(false, actualNum, expectedNum)
        }

        return Triple(comparison(actualNum, expectedNum), actualNum, expectedNum)
    }

    companion object {
        /** Default matcher instance */
        val default = VerificationMatcher()

        /** Case-sensitive matcher */
        val caseSensitive = VerificationMatcher(caseSensitive = true)

        /** Matcher that preserves whitespace */
        val preserveWhitespace = VerificationMatcher(trimWhitespace = false)
    }
}

/**
 * Result of a match operation.
 */
data class MatchResult(
    /** Whether the match succeeded */
    val matched: Boolean,

    /** The actual value as string */
    val actualValue: String?,

    /** The expected value as string */
    val expectedValue: String?,

    /** Human-readable message */
    val message: String
)

/**
 * Extension functions for fluent matching.
 */
object Matchers {
    fun equalTo(expected: Any?) = MatcherSpec(ComparisonOperator.EQUALS, expected)
    fun notEqualTo(expected: Any?) = MatcherSpec(ComparisonOperator.NOT_EQUALS, expected)
    fun containing(text: String) = MatcherSpec(ComparisonOperator.CONTAINS, text)
    fun notContaining(text: String) = MatcherSpec(ComparisonOperator.NOT_CONTAINS, text)
    fun startingWith(prefix: String) = MatcherSpec(ComparisonOperator.STARTS_WITH, prefix)
    fun endingWith(suffix: String) = MatcherSpec(ComparisonOperator.ENDS_WITH, suffix)
    fun matchingRegex(pattern: String) = MatcherSpec(ComparisonOperator.MATCHES_REGEX, pattern)
    fun greaterThan(value: Number) = MatcherSpec(ComparisonOperator.GREATER_THAN, value)
    fun lessThan(value: Number) = MatcherSpec(ComparisonOperator.LESS_THAN, value)
    fun greaterOrEqualTo(value: Number) = MatcherSpec(ComparisonOperator.GREATER_OR_EQUAL, value)
    fun lessOrEqualTo(value: Number) = MatcherSpec(ComparisonOperator.LESS_OR_EQUAL, value)
    fun between(min: Number, max: Number) = MatcherSpec(ComparisonOperator.BETWEEN, min to max)
    fun empty() = MatcherSpec(ComparisonOperator.IS_EMPTY, null)
    fun notEmpty() = MatcherSpec(ComparisonOperator.IS_NOT_EMPTY, null)
    fun isNull() = MatcherSpec(ComparisonOperator.IS_NULL, null)
    fun notNull() = MatcherSpec(ComparisonOperator.IS_NOT_NULL, null)
    fun isTrue() = MatcherSpec(ComparisonOperator.IS_TRUE, null)
    fun isFalse() = MatcherSpec(ComparisonOperator.IS_FALSE, null)
}

/**
 * Specification for a matcher.
 */
data class MatcherSpec(
    val operator: ComparisonOperator,
    val expected: Any?
)
