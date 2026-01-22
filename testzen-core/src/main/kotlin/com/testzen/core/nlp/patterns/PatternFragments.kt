package com.testzen.core.nlp.patterns

/**
 * Centralized regex fragments for building NLP patterns.
 *
 * This object provides reusable, composable regex components that can be
 * combined to create complete patterns. Using these fragments ensures:
 * - Consistency across all patterns
 * - Single point of maintenance for common constructs
 * - Easy addition of new synonyms/variations
 * - Reduced duplication and potential for errors
 *
 * Usage:
 * ```kotlin
 * val pattern = "${F.VERIFY}\\s+${F.TARGET}\\s+${F.IS}${F.DISPLAYED}"
 * ```
 */
object PatternFragments {

    // ═══════════════════════════════════════════════════════════════
    // ACTION VERBS
    // ═══════════════════════════════════════════════════════════════

    /** Click/tap action verbs */
    const val CLICK = """(?:click|tap|press|touch|select|hit)"""

    /** Double click/tap action verbs */
    const val DOUBLE_CLICK = """(?:double[\s-]?(?:click|tap))"""

    /** Long press action verbs */
    const val LONG_PRESS = """(?:long[\s-]?press|hold|press\s+and\s+hold|touch\s+and\s+hold)"""

    /** Text entry action verbs */
    const val ENTER_TEXT = """(?:enter|type|input|fill|write)"""

    /** Clear/erase action verbs */
    const val CLEAR = """(?:clear|erase|delete|empty|wipe)"""

    /** Scroll action verbs */
    const val SCROLL = """(?:scroll|drag)"""

    /** Swipe action verbs */
    const val SWIPE = """(?:swipe|flick|fling)"""

    /** Wait action verbs */
    const val WAIT = """(?:wait|pause|delay|sleep)"""

    /** Launch action verbs */
    const val LAUNCH = """(?:launch|open|start|run)"""

    /** Close action verbs */
    const val CLOSE = """(?:close|quit|exit|terminate|end|stop|kill)"""

    /** Screenshot action verbs */
    const val SCREENSHOT = """(?:take\s+)?(?:a\s+)?(?:screenshot|screen\s*capture|screencap|snap)"""

    /** Select/choose action verbs */
    const val SELECT = """(?:select|choose|pick)"""

    /** Check checkbox action verbs */
    const val CHECK = """(?:check|tick|mark|enable)"""

    /** Uncheck checkbox action verbs */
    const val UNCHECK = """(?:uncheck|untick|unmark|disable)"""

    /** Toggle action verbs */
    const val TOGGLE = """(?:toggle|switch|flip|turn)"""

    // ═══════════════════════════════════════════════════════════════
    // VERIFICATION VERBS
    // ═══════════════════════════════════════════════════════════════

    /** Verification verbs (verify, check, assert, etc.) */
    const val VERIFY = """(?:verify|check|assert|confirm|ensure|expect|should\s+see)"""

    /** Simple verification verbs (without should see) */
    const val VERIFY_SIMPLE = """(?:verify|check|assert|confirm)"""

    /** See/observe verbs */
    const val SEE = """(?:verify|check|assert|see)"""

    // ═══════════════════════════════════════════════════════════════
    // STATE WORDS
    // ═══════════════════════════════════════════════════════════════

    /** Displayed/visible states */
    const val DISPLAYED = """(?:displayed|visible|shown|present|appears?)"""

    /** Not displayed states */
    const val NOT_DISPLAYED = """(?:not\s+displayed|not\s+visible|hidden|gone|absent|not\s+shown)"""

    /** Enabled states */
    const val ENABLED = """(?:enabled|active|clickable)"""

    /** Disabled states */
    const val DISABLED = """(?:disabled|inactive|grayed)"""

    /** Checked states */
    const val CHECKED = """(?:checked|ticked|marked|selected)"""

    /** Not checked states */
    const val NOT_CHECKED = """(?:not\s+checked|unchecked|not\s+ticked|not\s+marked)"""

    /** Selected state */
    const val SELECTED = """selected"""

    /** Not selected state */
    const val NOT_SELECTED = """not\s+selected"""

    /** Focused state */
    const val FOCUSED = """(?:focused|has\s+focus)"""

    /** Empty states */
    const val EMPTY = """(?:empty|blank)"""

    // ═══════════════════════════════════════════════════════════════
    // COMPARISON WORDS
    // ═══════════════════════════════════════════════════════════════

    /** Contains/includes comparison */
    const val CONTAINS = """(?:contains?|includes?)"""

    /** Equals comparison */
    const val EQUALS = """(?:is|equals?|=)"""

    /** Shows/displays comparison */
    const val SHOWS = """(?:shows?|contains?|has|displays?)"""

    /** Matches comparison */
    const val MATCHES = """(?:matches)"""

    /** Greater than comparison */
    const val GREATER_THAN = """(?:greater\s+than|more\s+than|above|>)"""

    /** Less than comparison */
    const val LESS_THAN = """(?:less\s+than|fewer\s+than|below|<)"""

    // ═══════════════════════════════════════════════════════════════
    // STRUCTURAL FRAGMENTS
    // ═══════════════════════════════════════════════════════════════

    /** Optional "that" word */
    const val THAT = """(?:that\s+)?"""

    /** Optional "the" word */
    const val THE = """(?:the\s+)?"""

    /** Optional "is" word */
    const val IS = """(?:is\s+)?"""

    /** Optional "be" word */
    const val BE = """(?:be\s+)?"""

    /** Optional "to" word */
    const val TO = """(?:to\s+)?"""

    /** Optional "on" word */
    const val ON = """(?:on\s+)?"""

    /** Prepositions for location (in, into, on, at) */
    const val IN = """(?:in(?:to)?|on|at)"""

    /** Prepositions for source (from, in) */
    const val FROM = """(?:from|in)"""

    // ═══════════════════════════════════════════════════════════════
    // VALUE CAPTURE GROUPS
    // ═══════════════════════════════════════════════════════════════

    /** Captures a quoted value: 'value' or "value" */
    const val QUOTED_VALUE = """['"]([^'"]+)['"]"""

    /** Captures an optional quoted or unquoted target */
    const val TARGET = """['"]?([^'"]+?)['"]?"""

    /** Captures a clean target (with optional quotes, cleaned) */
    const val TARGET_CLEAN = """['"]?(.+?)['"]?"""

    /** Captures a numeric value */
    const val NUMBER = """(\d+(?:\.\d+)?)"""

    /** Captures an integer */
    const val INTEGER = """(\d+)"""

    /** Captures a direction */
    const val DIRECTION = """(up|down|left|right)"""

    /** Duration with unit (seconds, ms, minutes) */
    const val DURATION = """(\d+(?:\.\d+)?)\s*(?:seconds?|s|milliseconds?|ms|minutes?|m)"""

    // ═══════════════════════════════════════════════════════════════
    // ELEMENT SUFFIXES
    // ═══════════════════════════════════════════════════════════════

    /** Optional button suffix */
    const val BUTTON = """(?:\s+button)?"""

    /** Optional field suffix */
    const val FIELD = """(?:\s+field)?"""

    /** Optional checkbox suffix */
    const val CHECKBOX = """(?:\s+checkbox)?"""

    /** Optional dropdown suffix */
    const val DROPDOWN = """(?:\s+dropdown)?"""

    /** Optional app suffix */
    const val APP = """(?:app(?:lication)?)?"""

    // ═══════════════════════════════════════════════════════════════
    // CONTEXT PHRASES
    // ═══════════════════════════════════════════════════════════════

    /** Page/screen context */
    const val SCREEN = """(?:the\s+)?(?:screen|page)"""

    /** Page title context */
    const val PAGE_TITLE = """(?:page\s+)?title"""

    /** URL/address context */
    const val URL = """(?:url|address)"""

    /** Text/content context */
    const val TEXT = """(?:text\s+)?"""

    /** Attribute context */
    const val ATTRIBUTE = """attribute"""

    /** Count/number context */
    const val COUNT = """(?:count|number)"""

    // ═══════════════════════════════════════════════════════════════
    // ELEMENT APPEARANCE/DISAPPEARANCE
    // ═══════════════════════════════════════════════════════════════

    /** Element appearing */
    const val APPEAR = """(?:appear|be\s+visible|be\s+displayed|show)"""

    /** Element disappearing */
    const val DISAPPEAR = """(?:disappear|be\s+gone|be\s+hidden|vanish)"""

    // ═══════════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════════

    /** Back navigation */
    const val BACK = """(?:go\s+back|press\s+back|navigate\s+back|back\s+button|return|previous)"""

    /** Forward navigation */
    const val FORWARD = """(?:go\s+forward|navigate\s+forward|forward\s+button|next)"""

    // ═══════════════════════════════════════════════════════════════
    // TOGGLE STATES
    // ═══════════════════════════════════════════════════════════════

    /** On/off state */
    const val ON_OFF = """(?:\s+(?:on|off))?"""
}

/**
 * Type alias for shorter access in pattern definitions.
 */
typealias F = PatternFragments
