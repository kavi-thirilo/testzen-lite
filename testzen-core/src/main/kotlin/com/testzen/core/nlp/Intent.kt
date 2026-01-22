package com.testzen.core.nlp

/**
 * Represents the intent/action type extracted from natural language.
 *
 * Each intent represents a distinct user action that can be executed.
 */
enum class Intent {
    // Interaction intents
    CLICK,
    DOUBLE_CLICK,
    LONG_PRESS,
    ENTER_TEXT,
    CLEAR_TEXT,

    // Verification intents - presence
    VERIFY_DISPLAYED,
    VERIFY_NOT_DISPLAYED,
    VERIFY_EXISTS,
    VERIFY_NOT_EXISTS,

    // Verification intents - state
    VERIFY_ENABLED,
    VERIFY_DISABLED,
    VERIFY_CHECKED,
    VERIFY_NOT_CHECKED,
    VERIFY_SELECTED,
    VERIFY_NOT_SELECTED,
    VERIFY_FOCUSED,

    // Verification intents - text
    VERIFY_TEXT,
    VERIFY_TEXT_CONTAINS,
    VERIFY_TEXT_MATCHES,
    VERIFY_TEXT_EMPTY,
    VERIFY_TEXT_NOT_EMPTY,

    // Verification intents - numeric/comparison
    VERIFY_COUNT,
    VERIFY_VALUE_EQUALS,
    VERIFY_VALUE_GREATER,
    VERIFY_VALUE_LESS,

    // Verification intents - attribute/CSS
    VERIFY_ATTRIBUTE,
    VERIFY_CSS_PROPERTY,

    // Verification intents - page/screen
    VERIFY_PAGE_TITLE,
    VERIFY_URL,
    VERIFY_TEXT_ON_SCREEN,

    // Wait intents
    WAIT_DURATION,
    WAIT_FOR_ELEMENT,
    WAIT_FOR_ELEMENT_GONE,

    // Navigation intents
    SCROLL,
    SWIPE,
    NAVIGATE_BACK,
    NAVIGATE_FORWARD,

    // App intents
    LAUNCH_APP,
    CLOSE_APP,
    TAKE_SCREENSHOT,

    // Form intents
    SELECT_OPTION,
    CHECK_CHECKBOX,
    UNCHECK_CHECKBOX,
    TOGGLE_SWITCH,

    // Unknown/fallback
    UNKNOWN
}

/**
 * Entities extracted from the instruction.
 */
data class ExtractedEntities(
    /** Primary target element (button name, field label, etc.) */
    val target: String? = null,

    /** Secondary target (for drag-drop, select from) */
    val secondaryTarget: String? = null,

    /** Text value to enter or verify */
    val value: String? = null,

    /** Numeric value (duration, count, etc.) */
    val numericValue: Long? = null,

    /** Direction (up, down, left, right) */
    val direction: String? = null,

    /** Option to select */
    val option: String? = null,

    /** Attribute name for attribute verification */
    val attributeName: String? = null,

    /** CSS property name for CSS verification */
    val cssProperty: String? = null,

    /** Comparison operator (equals, contains, greater, less, etc.) */
    val comparisonOperator: String? = null,

    /** Regex pattern for text matching */
    val regexPattern: String? = null,

    /** Additional modifiers/flags */
    val modifiers: Map<String, Any> = emptyMap()
)

/**
 * Result of NLP parsing.
 */
data class NLPResult(
    /** The detected intent */
    val intent: Intent,

    /** Confidence score (0.0 to 1.0) */
    val confidence: Double,

    /** Extracted entities */
    val entities: ExtractedEntities,

    /** Original input text */
    val originalText: String,

    /** Normalized/cleaned text */
    val normalizedText: String,

    /** Alternative interpretations with lower confidence */
    val alternatives: List<NLPResult> = emptyList()
)
