package com.testzen.core.locator.smart

/**
 * Defines action types and their element requirements.
 *
 * Each action type specifies what traits an element needs to support that action,
 * enabling intelligent element selection based on intended interaction.
 */
enum class ActionType(
    /** Traits required for this action */
    val requiredTraits: Set<ElementTrait>,
    /** Traits preferred but not required */
    val preferredTraits: Set<ElementTrait>,
    /** Whether to search parent elements if target doesn't support action */
    val allowParentTraversal: Boolean,
    /** Whether to search child elements */
    val allowChildTraversal: Boolean,
    /** Whether spatial search should be used for finding related elements */
    val useSpatialSearch: Boolean
) {
    /** Click/tap action - needs clickable element */
    CLICK(
        requiredTraits = setOf(ElementTrait.CLICKABLE),
        preferredTraits = setOf(ElementTrait.VISIBLE, ElementTrait.ENABLED),
        allowParentTraversal = true,
        allowChildTraversal = false,
        useSpatialSearch = true
    ),

    /** Double tap action */
    DOUBLE_TAP(
        requiredTraits = setOf(ElementTrait.CLICKABLE),
        preferredTraits = setOf(ElementTrait.VISIBLE, ElementTrait.ENABLED),
        allowParentTraversal = true,
        allowChildTraversal = false,
        useSpatialSearch = true
    ),

    /** Long press/hold action */
    LONG_PRESS(
        requiredTraits = setOf(ElementTrait.LONG_CLICKABLE, ElementTrait.CLICKABLE),
        preferredTraits = setOf(ElementTrait.VISIBLE, ElementTrait.ENABLED),
        allowParentTraversal = true,
        allowChildTraversal = false,
        useSpatialSearch = true
    ),

    /** Text entry action - needs editable element */
    ENTER_TEXT(
        requiredTraits = setOf(ElementTrait.EDITABLE),
        preferredTraits = setOf(ElementTrait.VISIBLE, ElementTrait.ENABLED, ElementTrait.FOCUSABLE),
        allowParentTraversal = false,
        allowChildTraversal = true,
        useSpatialSearch = true
    ),

    /** Clear text action */
    CLEAR_TEXT(
        requiredTraits = setOf(ElementTrait.EDITABLE),
        preferredTraits = setOf(ElementTrait.VISIBLE, ElementTrait.ENABLED),
        allowParentTraversal = false,
        allowChildTraversal = true,
        useSpatialSearch = true
    ),

    /** Scroll action - needs scrollable container */
    SCROLL(
        requiredTraits = setOf(ElementTrait.SCROLLABLE),
        preferredTraits = setOf(ElementTrait.VISIBLE),
        allowParentTraversal = true,
        allowChildTraversal = false,
        useSpatialSearch = false
    ),

    /** Swipe action */
    SWIPE(
        requiredTraits = setOf(),
        preferredTraits = setOf(ElementTrait.VISIBLE),
        allowParentTraversal = false,
        allowChildTraversal = false,
        useSpatialSearch = false
    ),

    /** Verification - just needs to exist */
    VERIFY(
        requiredTraits = setOf(),
        preferredTraits = setOf(ElementTrait.VISIBLE),
        allowParentTraversal = false,
        allowChildTraversal = false,
        useSpatialSearch = false
    ),

    /** Select from dropdown/picker */
    SELECT(
        requiredTraits = setOf(ElementTrait.CLICKABLE),
        preferredTraits = setOf(ElementTrait.VISIBLE, ElementTrait.ENABLED),
        allowParentTraversal = true,
        allowChildTraversal = true,
        useSpatialSearch = true
    ),

    /** Toggle switch/checkbox */
    TOGGLE(
        requiredTraits = setOf(ElementTrait.CHECKABLE, ElementTrait.CLICKABLE),
        preferredTraits = setOf(ElementTrait.VISIBLE, ElementTrait.ENABLED),
        allowParentTraversal = true,
        allowChildTraversal = false,
        useSpatialSearch = true
    ),

    /** Check checkbox/radio */
    CHECK(
        requiredTraits = setOf(ElementTrait.CHECKABLE),
        preferredTraits = setOf(ElementTrait.VISIBLE, ElementTrait.ENABLED, ElementTrait.CLICKABLE),
        allowParentTraversal = true,
        allowChildTraversal = false,
        useSpatialSearch = true
    ),

    /** Generic read/find action */
    READ(
        requiredTraits = setOf(),
        preferredTraits = setOf(ElementTrait.VISIBLE),
        allowParentTraversal = false,
        allowChildTraversal = false,
        useSpatialSearch = false
    );

    /**
     * Check if an element with given traits can support this action.
     */
    fun isCompatible(traits: Set<ElementTrait>): Boolean {
        // For actions with no required traits (like VERIFY), any element works
        if (requiredTraits.isEmpty()) return true

        // Check if element has at least one of the required traits
        return requiredTraits.any { it in traits }
    }

    /**
     * Calculate compatibility score (0.0 to 1.0).
     */
    fun compatibilityScore(traits: Set<ElementTrait>): Double {
        if (requiredTraits.isEmpty()) {
            // Score based on preferred traits only
            val preferredCount = preferredTraits.count { it in traits }
            return if (preferredTraits.isEmpty()) 1.0 else preferredCount.toDouble() / preferredTraits.size
        }

        // Required trait match
        val requiredMatch = requiredTraits.count { it in traits }
        val requiredScore = requiredMatch.toDouble() / requiredTraits.size

        // Preferred trait bonus
        val preferredMatch = preferredTraits.count { it in traits }
        val preferredScore = if (preferredTraits.isEmpty()) 0.0 else preferredMatch.toDouble() / preferredTraits.size

        // Combined score: 70% required, 30% preferred
        return (requiredScore * 0.7) + (preferredScore * 0.3)
    }

    companion object {
        /**
         * Map instruction type to action type.
         */
        fun fromInstructionName(name: String): ActionType {
            return when (name.uppercase()) {
                "CLICK", "TAP", "PRESS" -> CLICK
                "DOUBLE_TAP", "DOUBLE_CLICK" -> DOUBLE_TAP
                "LONG_PRESS", "HOLD" -> LONG_PRESS
                "ENTER_TEXT", "TYPE", "INPUT" -> ENTER_TEXT
                "CLEAR_TEXT", "CLEAR" -> CLEAR_TEXT
                "SCROLL" -> SCROLL
                "SWIPE" -> SWIPE
                "VERIFY", "VERIFY_DISPLAYED", "VERIFY_TEXT" -> VERIFY
                "SELECT", "SELECT_OPTION" -> SELECT
                "TOGGLE", "TOGGLE_SWITCH" -> TOGGLE
                "CHECK", "CHECK_CHECKBOX", "UNCHECK" -> CHECK
                else -> READ
            }
        }
    }
}

/**
 * Element traits that describe element capabilities.
 */
enum class ElementTrait {
    /** Element is visible on screen */
    VISIBLE,

    /** Element is enabled for interaction */
    ENABLED,

    /** Element can be clicked/tapped */
    CLICKABLE,

    /** Element supports long click/press */
    LONG_CLICKABLE,

    /** Element can receive text input */
    EDITABLE,

    /** Element can receive focus */
    FOCUSABLE,

    /** Element can be scrolled */
    SCROLLABLE,

    /** Element can be checked/unchecked (checkbox, radio, switch) */
    CHECKABLE,

    /** Element is currently checked/selected */
    CHECKED,

    /** Element is a container that holds other elements */
    CONTAINER,

    /** Element contains text content */
    HAS_TEXT,

    /** Element is password field */
    PASSWORD,

    /** Element is a label for another element */
    LABEL,

    /** Element is an image/icon */
    IMAGE,

    /** Element is a button */
    BUTTON,

    /** Element is an input field */
    INPUT_FIELD,

    /** Element is a dropdown/picker */
    DROPDOWN,

    /** Element is a list item */
    LIST_ITEM,

    /** Element is a web link */
    LINK
}
