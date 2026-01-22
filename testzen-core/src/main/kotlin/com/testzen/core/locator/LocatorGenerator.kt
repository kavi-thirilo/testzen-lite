package com.testzen.core.locator

import com.testzen.core.model.Platform

/**
 * Generates locator strategies for elements based on platform.
 *
 * This class is responsible for creating multiple locator strategies
 * for a given element name, ordered by confidence/priority.
 *
 * Single Responsibility: Platform-specific locator generation only.
 */
class LocatorGenerator(
    private val platform: Platform
) {
    /**
     * Generate locators for an element based on its name and the configured platform.
     *
     * @param elementName The human-readable element name
     * @return List of locators ordered by confidence (highest first)
     */
    fun generate(elementName: String): List<Locator> {
        val normalizedName = elementName.trim()

        val locators = when (platform) {
            Platform.ANDROID -> generateAndroidLocators(normalizedName)
            Platform.IOS -> generateIOSLocators(normalizedName)
            Platform.WEB -> generateWebLocators(normalizedName)
        }

        return locators.sortedByDescending { it.confidence }
    }

    /**
     * Generate locators specifically for text entry fields.
     * These include additional strategies for finding input elements near labels.
     */
    fun generateForTextEntry(elementName: String): List<Locator> {
        val baseLocators = generate(elementName)
        val normalizedName = elementName.trim()

        val textEntryLocators = when (platform) {
            Platform.ANDROID -> listOf(
                Locator(LocatorType.XPATH,
                    "//*[@text='$normalizedName']/following-sibling::*[1][@focusable='true']", 0.5),
                Locator(LocatorType.XPATH,
                    "//*[contains(@text, '$normalizedName')]/..//*[@focusable='true']", 0.4)
            )
            Platform.IOS -> listOf(
                Locator(LocatorType.XPATH,
                    "//*[@label='$normalizedName']/following-sibling::*[1]", 0.5),
                Locator(LocatorType.XPATH,
                    "//*[contains(@label, '$normalizedName')]/..//XCUIElementTypeTextField", 0.4)
            )
            Platform.WEB -> listOf(
                Locator(LocatorType.CSS_SELECTOR, "[placeholder='$normalizedName']", 0.6),
                Locator(LocatorType.CSS_SELECTOR, "input[aria-label='$normalizedName']", 0.55),
                Locator(LocatorType.XPATH,
                    "//label[contains(text(),'$normalizedName')]/following-sibling::input", 0.5)
            )
        }

        return (baseLocators + textEntryLocators).sortedByDescending { it.confidence }
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE PLATFORM-SPECIFIC GENERATORS
    // ═══════════════════════════════════════════════════════════════

    private fun generateAndroidLocators(elementName: String): List<Locator> {
        val locators = mutableListOf<Locator>()

        // Accessibility ID (content-description) - highest priority
        locators.add(Locator(LocatorType.ACCESSIBILITY_ID, elementName, 0.9))

        // Resource ID variations
        val resourceId = toResourceId(elementName)
        locators.add(Locator(LocatorType.RESOURCE_ID, "*:id/$resourceId", 0.8))

        // Text matching
        locators.add(Locator(LocatorType.TEXT, elementName, 0.85))

        // XPath with exact match
        locators.add(Locator(LocatorType.XPATH,
            "//*[@text='$elementName' or @content-desc='$elementName']", 0.7))

        // XPath with contains
        locators.add(Locator(LocatorType.XPATH,
            "//*[contains(@text, '$elementName') or contains(@content-desc, '$elementName')]", 0.6))

        return locators
    }

    private fun generateIOSLocators(elementName: String): List<Locator> {
        val locators = mutableListOf<Locator>()

        // Accessibility ID - highest priority
        locators.add(Locator(LocatorType.ACCESSIBILITY_ID, elementName, 0.9))

        // Name attribute
        locators.add(Locator(LocatorType.NAME, elementName, 0.85))

        // XPath with label/name exact match
        locators.add(Locator(LocatorType.XPATH,
            "//*[@label='$elementName' or @name='$elementName']", 0.7))

        // XPath with contains
        locators.add(Locator(LocatorType.XPATH,
            "//*[contains(@label, '$elementName') or contains(@name, '$elementName')]", 0.6))

        return locators
    }

    private fun generateWebLocators(elementName: String): List<Locator> {
        val locators = mutableListOf<Locator>()
        val elementId = toHtmlId(elementName)

        // ID attribute - highest priority
        locators.add(Locator(LocatorType.ID, elementId, 0.9))

        // Name attribute
        locators.add(Locator(LocatorType.NAME, elementId, 0.85))

        // Data test attributes
        locators.add(Locator(LocatorType.CSS_SELECTOR,
            "[data-testid='$elementId'], [data-test='$elementId']", 0.85))

        // Aria label
        locators.add(Locator(LocatorType.CSS_SELECTOR,
            "[aria-label='$elementName']", 0.8))

        // Text content via XPath
        locators.add(Locator(LocatorType.XPATH,
            "//*[text()='$elementName' or @placeholder='$elementName']", 0.7))

        // Link text
        locators.add(Locator(LocatorType.LINK_TEXT, elementName, 0.75))

        // Partial link text
        locators.add(Locator(LocatorType.PARTIAL_LINK_TEXT, elementName, 0.6))

        return locators
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Convert element name to Android resource ID format (snake_case).
     */
    private fun toResourceId(name: String): String {
        return name.lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
    }

    /**
     * Convert element name to HTML ID format (kebab-case).
     */
    private fun toHtmlId(name: String): String {
        return name.lowercase()
            .replace(" ", "-")
            .replace(Regex("[^a-z0-9-]"), "")
    }

    companion object {
        /**
         * Create a generator for the specified platform.
         */
        fun forPlatform(platform: Platform): LocatorGenerator = LocatorGenerator(platform)
    }
}
