package com.testzen.core.locator.smart

import com.testzen.core.model.Platform
import org.openqa.selenium.Dimension
import org.openqa.selenium.Point
import org.openqa.selenium.Rectangle
import org.openqa.selenium.WebElement
import org.slf4j.LoggerFactory

/**
 * Platform-specific adapter for extracting element traits and attributes.
 *
 * Handles differences between Android, iOS, and Web element attributes
 * to provide a unified interface for element analysis.
 *
 * Single Responsibility: Platform-specific element attribute extraction.
 */
class PlatformElementAdapter(
    private val platform: Platform
) {
    private val logger = LoggerFactory.getLogger(PlatformElementAdapter::class.java)

    /**
     * Extract all traits from an element.
     */
    fun extractTraits(element: WebElement): Set<ElementTrait> {
        return when (platform) {
            Platform.ANDROID -> extractAndroidTraits(element)
            Platform.IOS -> extractIOSTraits(element)
            Platform.WEB -> extractWebTraits(element)
        }
    }

    /**
     * Get element's text content.
     */
    fun getText(element: WebElement): String {
        return try {
            when (platform) {
                Platform.ANDROID -> {
                    element.getAttribute("text")
                        ?: element.getAttribute("content-desc")
                        ?: element.text
                        ?: ""
                }
                Platform.IOS -> {
                    element.getAttribute("label")
                        ?: element.getAttribute("value")
                        ?: element.getAttribute("name")
                        ?: element.text
                        ?: ""
                }
                Platform.WEB -> {
                    element.text
                        ?: element.getAttribute("innerText")
                        ?: element.getAttribute("value")
                        ?: element.getAttribute("placeholder")
                        ?: ""
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Get element's accessibility identifier.
     */
    fun getAccessibilityId(element: WebElement): String? {
        return try {
            when (platform) {
                Platform.ANDROID -> element.getAttribute("content-desc")
                Platform.IOS -> element.getAttribute("name") ?: element.getAttribute("accessibility-id")
                Platform.WEB -> element.getAttribute("aria-label") ?: element.getAttribute("data-testid")
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get element's resource/element ID.
     */
    fun getResourceId(element: WebElement): String? {
        return try {
            when (platform) {
                Platform.ANDROID -> element.getAttribute("resource-id")
                Platform.IOS -> element.getAttribute("name")
                Platform.WEB -> element.getAttribute("id")
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get element class/type name.
     */
    fun getClassName(element: WebElement): String {
        return try {
            when (platform) {
                Platform.ANDROID -> element.getAttribute("class") ?: element.tagName ?: ""
                Platform.IOS -> element.getAttribute("type") ?: element.tagName ?: ""
                Platform.WEB -> element.tagName ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Get element bounds as Rectangle.
     */
    fun getBounds(element: WebElement): Rectangle {
        return try {
            element.rect
        } catch (e: Exception) {
            try {
                val location = element.location
                val size = element.size
                Rectangle(location.x, location.y, size.height, size.width)
            } catch (e2: Exception) {
                Rectangle(0, 0, 0, 0)
            }
        }
    }

    /**
     * Get element center point.
     */
    fun getCenter(element: WebElement): Point {
        val bounds = getBounds(element)
        return Point(
            bounds.x + bounds.width / 2,
            bounds.y + bounds.height / 2
        )
    }

    /**
     * Check if element is an input/text field.
     */
    fun isInputField(element: WebElement): Boolean {
        val className = getClassName(element).lowercase()
        return when (platform) {
            Platform.ANDROID -> className.contains("edittext") ||
                    className.contains("textinputlayout") ||
                    element.getAttribute("focusable") == "true" &&
                    element.getAttribute("editable") == "true"

            Platform.IOS -> className.contains("textfield") ||
                    className.contains("textview") ||
                    className.contains("securetextfield")

            Platform.WEB -> {
                val tagName = element.tagName?.lowercase() ?: ""
                val type = element.getAttribute("type")?.lowercase() ?: ""
                tagName == "input" && type !in listOf("button", "submit", "reset", "checkbox", "radio") ||
                        tagName == "textarea"
            }
        }
    }

    /**
     * Check if element is a button.
     */
    fun isButton(element: WebElement): Boolean {
        val className = getClassName(element).lowercase()
        return when (platform) {
            Platform.ANDROID -> className.contains("button") ||
                    className.contains("imagebutton") ||
                    className.contains("floatingactionbutton")

            Platform.IOS -> className.contains("button") ||
                    className.contains("xcuielementtypebutton")

            Platform.WEB -> {
                val tagName = element.tagName?.lowercase() ?: ""
                val type = element.getAttribute("type")?.lowercase() ?: ""
                val role = element.getAttribute("role")?.lowercase() ?: ""
                tagName == "button" ||
                        (tagName == "input" && type in listOf("button", "submit", "reset")) ||
                        role == "button"
            }
        }
    }

    /**
     * Check if element is a checkbox or radio button.
     */
    fun isCheckable(element: WebElement): Boolean {
        val className = getClassName(element).lowercase()
        return when (platform) {
            Platform.ANDROID -> {
                className.contains("checkbox") ||
                        className.contains("radiobutton") ||
                        className.contains("switch") ||
                        className.contains("togglebutton") ||
                        element.getAttribute("checkable") == "true"
            }

            Platform.IOS -> {
                className.contains("switch") ||
                        className.contains("checkbox") ||
                        className.contains("xcuielementtypeswitch")
            }

            Platform.WEB -> {
                val tagName = element.tagName?.lowercase() ?: ""
                val type = element.getAttribute("type")?.lowercase() ?: ""
                val role = element.getAttribute("role")?.lowercase() ?: ""
                (tagName == "input" && type in listOf("checkbox", "radio")) ||
                        role in listOf("checkbox", "radio", "switch")
            }
        }
    }

    /**
     * Check if element is a dropdown/picker.
     */
    fun isDropdown(element: WebElement): Boolean {
        val className = getClassName(element).lowercase()
        return when (platform) {
            Platform.ANDROID -> className.contains("spinner") ||
                    className.contains("dropdown") ||
                    className.contains("picker")

            Platform.IOS -> className.contains("picker") ||
                    className.contains("popupbutton")

            Platform.WEB -> {
                val tagName = element.tagName?.lowercase() ?: ""
                val role = element.getAttribute("role")?.lowercase() ?: ""
                tagName == "select" || role in listOf("listbox", "combobox", "menu")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE PLATFORM-SPECIFIC EXTRACTORS
    // ═══════════════════════════════════════════════════════════════

    private fun extractAndroidTraits(element: WebElement): Set<ElementTrait> {
        val traits = mutableSetOf<ElementTrait>()

        try {
            // Visibility
            if (element.isDisplayed) traits.add(ElementTrait.VISIBLE)

            // Enabled state
            if (element.isEnabled) traits.add(ElementTrait.ENABLED)

            // Clickable
            if (element.getAttribute("clickable") == "true") {
                traits.add(ElementTrait.CLICKABLE)
            }

            // Long clickable
            if (element.getAttribute("long-clickable") == "true") {
                traits.add(ElementTrait.LONG_CLICKABLE)
            }

            // Focusable
            if (element.getAttribute("focusable") == "true") {
                traits.add(ElementTrait.FOCUSABLE)
            }

            // Scrollable
            if (element.getAttribute("scrollable") == "true") {
                traits.add(ElementTrait.SCROLLABLE)
            }

            // Checkable
            if (element.getAttribute("checkable") == "true") {
                traits.add(ElementTrait.CHECKABLE)
            }
            if (element.getAttribute("checked") == "true") {
                traits.add(ElementTrait.CHECKED)
            }

            // Editable (for text fields)
            val className = getClassName(element).lowercase()
            if (className.contains("edittext") ||
                element.getAttribute("editable") == "true" ||
                (element.getAttribute("focusable") == "true" && className.contains("text"))
            ) {
                traits.add(ElementTrait.EDITABLE)
            }

            // Password field
            if (element.getAttribute("password") == "true") {
                traits.add(ElementTrait.PASSWORD)
            }

            // Has text
            if (!getText(element).isNullOrBlank()) {
                traits.add(ElementTrait.HAS_TEXT)
            }

            // Element type classification
            if (isButton(element)) traits.add(ElementTrait.BUTTON)
            if (isInputField(element)) traits.add(ElementTrait.INPUT_FIELD)
            if (isDropdown(element)) traits.add(ElementTrait.DROPDOWN)
            if (className.contains("image")) traits.add(ElementTrait.IMAGE)
            if (className.contains("layout") || className.contains("view")) {
                traits.add(ElementTrait.CONTAINER)
            }
            if (className.contains("listview") || className.contains("recyclerview")) {
                traits.add(ElementTrait.LIST_ITEM)
            }

        } catch (e: Exception) {
            logger.debug("Error extracting Android traits: ${e.message}")
        }

        return traits
    }

    private fun extractIOSTraits(element: WebElement): Set<ElementTrait> {
        val traits = mutableSetOf<ElementTrait>()

        try {
            // Visibility
            if (element.isDisplayed) traits.add(ElementTrait.VISIBLE)

            // Enabled state
            if (element.isEnabled) traits.add(ElementTrait.ENABLED)
            if (element.getAttribute("enabled") == "true") traits.add(ElementTrait.ENABLED)

            // Accessibility
            if (element.getAttribute("accessible") == "true") {
                traits.add(ElementTrait.CLICKABLE) // Accessible elements are typically clickable
            }

            val className = getClassName(element).lowercase()

            // Button
            if (className.contains("button")) {
                traits.add(ElementTrait.CLICKABLE)
                traits.add(ElementTrait.BUTTON)
            }

            // Text fields
            if (className.contains("textfield") || className.contains("textview")) {
                traits.add(ElementTrait.EDITABLE)
                traits.add(ElementTrait.FOCUSABLE)
                traits.add(ElementTrait.INPUT_FIELD)
            }
            if (className.contains("securetextfield")) {
                traits.add(ElementTrait.PASSWORD)
                traits.add(ElementTrait.EDITABLE)
            }

            // Switch
            if (className.contains("switch")) {
                traits.add(ElementTrait.CHECKABLE)
                traits.add(ElementTrait.CLICKABLE)
                if (element.getAttribute("value") == "1") {
                    traits.add(ElementTrait.CHECKED)
                }
            }

            // Scrollable
            if (className.contains("scrollview") || className.contains("tableview") ||
                className.contains("collectionview")
            ) {
                traits.add(ElementTrait.SCROLLABLE)
                traits.add(ElementTrait.CONTAINER)
            }

            // Picker
            if (className.contains("picker")) {
                traits.add(ElementTrait.DROPDOWN)
                traits.add(ElementTrait.SCROLLABLE)
            }

            // Image
            if (className.contains("image")) {
                traits.add(ElementTrait.IMAGE)
            }

            // Has text
            if (!getText(element).isNullOrBlank()) {
                traits.add(ElementTrait.HAS_TEXT)
            }

            // Cell/list item
            if (className.contains("cell")) {
                traits.add(ElementTrait.LIST_ITEM)
                traits.add(ElementTrait.CLICKABLE)
            }

        } catch (e: Exception) {
            logger.debug("Error extracting iOS traits: ${e.message}")
        }

        return traits
    }

    private fun extractWebTraits(element: WebElement): Set<ElementTrait> {
        val traits = mutableSetOf<ElementTrait>()

        try {
            // Visibility
            if (element.isDisplayed) traits.add(ElementTrait.VISIBLE)

            // Enabled state
            if (element.isEnabled) traits.add(ElementTrait.ENABLED)

            val tagName = element.tagName?.lowercase() ?: ""
            val type = element.getAttribute("type")?.lowercase() ?: ""
            val role = element.getAttribute("role")?.lowercase() ?: ""

            // Clickable elements
            if (tagName in listOf("a", "button") ||
                role == "button" ||
                element.getAttribute("onclick") != null ||
                (tagName == "input" && type in listOf("button", "submit", "reset"))
            ) {
                traits.add(ElementTrait.CLICKABLE)
            }

            // Links
            if (tagName == "a") {
                traits.add(ElementTrait.LINK)
                traits.add(ElementTrait.CLICKABLE)
            }

            // Buttons
            if (tagName == "button" || (tagName == "input" && type in listOf("button", "submit", "reset"))) {
                traits.add(ElementTrait.BUTTON)
                traits.add(ElementTrait.CLICKABLE)
            }

            // Input fields
            if (tagName == "input" && type !in listOf("button", "submit", "reset", "checkbox", "radio", "hidden")) {
                traits.add(ElementTrait.EDITABLE)
                traits.add(ElementTrait.FOCUSABLE)
                traits.add(ElementTrait.INPUT_FIELD)
            }
            if (tagName == "textarea") {
                traits.add(ElementTrait.EDITABLE)
                traits.add(ElementTrait.FOCUSABLE)
                traits.add(ElementTrait.INPUT_FIELD)
            }
            if (type == "password") {
                traits.add(ElementTrait.PASSWORD)
            }

            // Checkable
            if ((tagName == "input" && type in listOf("checkbox", "radio")) ||
                role in listOf("checkbox", "radio", "switch")
            ) {
                traits.add(ElementTrait.CHECKABLE)
                traits.add(ElementTrait.CLICKABLE)
                if (element.isSelected || element.getAttribute("checked") == "true") {
                    traits.add(ElementTrait.CHECKED)
                }
            }

            // Dropdown
            if (tagName == "select" || role in listOf("listbox", "combobox")) {
                traits.add(ElementTrait.DROPDOWN)
                traits.add(ElementTrait.CLICKABLE)
            }

            // Scrollable containers
            val overflow = element.getCssValue("overflow") ?: ""
            if (overflow in listOf("auto", "scroll") ||
                element.getAttribute("scrollable") == "true"
            ) {
                traits.add(ElementTrait.SCROLLABLE)
            }

            // Container elements
            if (tagName in listOf("div", "section", "article", "main", "form", "ul", "ol")) {
                traits.add(ElementTrait.CONTAINER)
            }

            // List items
            if (tagName == "li" || role == "listitem") {
                traits.add(ElementTrait.LIST_ITEM)
            }

            // Images
            if (tagName == "img" || tagName == "svg") {
                traits.add(ElementTrait.IMAGE)
            }

            // Labels
            if (tagName == "label") {
                traits.add(ElementTrait.LABEL)
            }

            // Has text
            if (!getText(element).isNullOrBlank()) {
                traits.add(ElementTrait.HAS_TEXT)
            }

            // Focusable
            val tabIndex = element.getAttribute("tabindex")
            if (tabIndex != null && tabIndex != "-1") {
                traits.add(ElementTrait.FOCUSABLE)
            }

        } catch (e: Exception) {
            logger.debug("Error extracting Web traits: ${e.message}")
        }

        return traits
    }

    companion object {
        fun forPlatform(platform: Platform): PlatformElementAdapter = PlatformElementAdapter(platform)
    }
}
