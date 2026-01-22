package com.testzen.core.locator

import kotlinx.serialization.Serializable

/**
 * Represents different strategies for locating elements.
 */
enum class LocatorType {
    ACCESSIBILITY_ID,
    RESOURCE_ID,
    TEXT,
    CONTENT_DESC,
    XPATH,
    CSS_SELECTOR,
    ID,
    NAME,
    CLASS_NAME,
    TAG_NAME,
    LINK_TEXT,
    PARTIAL_LINK_TEXT
}

/**
 * A single locator with type and value.
 */
@Serializable
data class Locator(
    val type: LocatorType,
    val value: String,
    val confidence: Double = 1.0
)

/**
 * Smart locator with multiple fallback strategies.
 *
 * The locators are ordered by priority - the first one that works will be used.
 * If it fails in subsequent runs, the system will try the next locator.
 */
@Serializable
data class SmartLocator(
    val elementName: String,
    val locators: List<Locator>,
    val lastSuccessfulIndex: Int = 0,
    val failureCount: Int = 0
) {
    /**
     * Get the primary locator (last successful or first).
     */
    fun primaryLocator(): Locator = locators.getOrElse(lastSuccessfulIndex) { locators.first() }

    /**
     * Get fallback locators (excluding primary).
     */
    fun fallbackLocators(): List<Locator> = locators.filterIndexed { index, _ -> index != lastSuccessfulIndex }

    /**
     * Create a copy with updated success index.
     */
    fun withSuccessAt(index: Int): SmartLocator = copy(lastSuccessfulIndex = index, failureCount = 0)

    /**
     * Create a copy with incremented failure count.
     */
    fun withFailure(): SmartLocator = copy(failureCount = failureCount + 1)
}

/**
 * Result of a locator resolution attempt.
 */
sealed class LocatorResult {
    data class Found(
        val locator: Locator,
        val locatorIndex: Int,
        val wasHealed: Boolean = false
    ) : LocatorResult()

    data class NotFound(
        val attemptedLocators: List<Locator>,
        val errors: List<String>
    ) : LocatorResult()
}
