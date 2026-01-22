package com.testzen.core.locator

import com.testzen.core.model.Platform
import org.openqa.selenium.WebElement
import org.slf4j.LoggerFactory

/**
 * Extracts locator-relevant attributes from WebElements.
 *
 * Used for learning from healed elements - when a fallback locator succeeds,
 * we extract the actual element attributes to create better locators for future runs.
 *
 * Single Responsibility: Element attribute extraction for locator learning.
 */
class ElementAttributeExtractor(
    private val platform: Platform
) {
    private val logger = LoggerFactory.getLogger(ElementAttributeExtractor::class.java)

    /**
     * Extract locators from a WebElement's actual attributes.
     *
     * @param element The WebElement to extract attributes from
     * @return List of locators derived from the element's attributes
     */
    fun extract(element: WebElement): List<Locator> {
        return try {
            when (platform) {
                Platform.ANDROID -> extractAndroidAttributes(element)
                Platform.IOS -> extractIOSAttributes(element)
                Platform.WEB -> extractWebAttributes(element)
            }
        } catch (e: Exception) {
            logger.debug("Error extracting locators from element: {}", e.message)
            emptyList()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE PLATFORM-SPECIFIC EXTRACTORS
    // ═══════════════════════════════════════════════════════════════

    private fun extractAndroidAttributes(element: WebElement): List<Locator> {
        val locators = mutableListOf<Locator>()

        // Content description (accessibility ID)
        element.safeGetAttribute("content-desc")?.let {
            locators.add(Locator(LocatorType.ACCESSIBILITY_ID, it, 0.98))
        }

        // Resource ID
        element.safeGetAttribute("resource-id")?.let {
            locators.add(Locator(LocatorType.RESOURCE_ID, it, 0.95))
        }

        // Text content
        element.safeGetAttribute("text")?.let {
            locators.add(Locator(LocatorType.TEXT, it, 0.85))
        }

        return locators
    }

    private fun extractIOSAttributes(element: WebElement): List<Locator> {
        val locators = mutableListOf<Locator>()

        // Name (accessibility identifier)
        element.safeGetAttribute("name")?.let {
            locators.add(Locator(LocatorType.ACCESSIBILITY_ID, it, 0.98))
        }

        // Label
        element.safeGetAttribute("label")?.let {
            locators.add(Locator(LocatorType.NAME, it, 0.90))
        }

        // Value
        element.safeGetAttribute("value")?.let {
            locators.add(Locator(LocatorType.TEXT, it, 0.80))
        }

        return locators
    }

    private fun extractWebAttributes(element: WebElement): List<Locator> {
        val locators = mutableListOf<Locator>()

        // ID attribute - highest priority
        element.safeGetAttribute("id")?.let {
            locators.add(Locator(LocatorType.ID, it, 0.98))
        }

        // Name attribute
        element.safeGetAttribute("name")?.let {
            locators.add(Locator(LocatorType.NAME, it, 0.95))
        }

        // Data-testid (common testing convention)
        element.safeGetAttribute("data-testid")?.let {
            locators.add(Locator(LocatorType.CSS_SELECTOR, "[data-testid='$it']", 0.97))
        }

        // Data-test (alternative testing convention)
        element.safeGetAttribute("data-test")?.let {
            locators.add(Locator(LocatorType.CSS_SELECTOR, "[data-test='$it']", 0.96))
        }

        // Aria-label
        element.safeGetAttribute("aria-label")?.let {
            locators.add(Locator(LocatorType.CSS_SELECTOR, "[aria-label='$it']", 0.90))
        }

        // Placeholder (for input fields)
        element.safeGetAttribute("placeholder")?.let {
            locators.add(Locator(LocatorType.CSS_SELECTOR, "[placeholder='$it']", 0.85))
        }

        return locators
    }

    /**
     * Safely get an attribute, returning null if blank or exception occurs.
     */
    private fun WebElement.safeGetAttribute(name: String): String? {
        return try {
            getAttribute(name)?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        /**
         * Create an extractor for the specified platform.
         */
        fun forPlatform(platform: Platform): ElementAttributeExtractor = ElementAttributeExtractor(platform)
    }
}
