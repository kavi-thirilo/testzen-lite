package com.testzen.core.nlp

import com.testzen.core.nlp.synonyms.SynonymRegistry
import org.slf4j.LoggerFactory

/**
 * Extracts entities (targets, values, etc.) from natural language text.
 *
 * Handles various quoting styles and complex entity references.
 */
class EntityExtractor {
    private val logger = LoggerFactory.getLogger(EntityExtractor::class.java)

    // Synonym registry for utility functions
    private val synonyms by lazy { SynonymRegistry.default() }

    // ═══════════════════════════════════════════════════════════════
    // PATTERNS FOR ENTITY EXTRACTION
    // ═══════════════════════════════════════════════════════════════

    /** Matches quoted strings (single, double, or smart quotes) */
    private val quotedStringPattern = Regex("""['"""]([^'"""]+)['"""]""")

    /** Matches "the X" or "a X" patterns */
    private val articleTargetPattern = Regex("""(?:the|a|an)\s+(['"]?[^'"]+['"]?)(?:\s+button|\s+field|\s+link|\s+element)?$""", RegexOption.IGNORE_CASE)

    /** Matches numeric values */
    private val numericPattern = Regex("""(\d+(?:\.\d+)?)""")

    /** Matches direction words */
    private val directionPattern = Regex("""(up|down|left|right|upward|downward|top|bottom)""", RegexOption.IGNORE_CASE)

    /**
     * Extract entities for a specific intent.
     */
    fun extract(text: String, intent: Intent, matchResult: IntentMatchResult?): ExtractedEntities {
        val normalized = text.lowercase().trim()

        return when (intent) {
            Intent.CLICK, Intent.DOUBLE_CLICK, Intent.LONG_PRESS -> extractClickEntities(normalized, text)
            Intent.ENTER_TEXT -> extractEnterTextEntities(normalized, text)
            Intent.CLEAR_TEXT -> extractClearEntities(normalized, text)
            Intent.VERIFY_DISPLAYED, Intent.VERIFY_NOT_DISPLAYED -> extractVerifyDisplayedEntities(normalized, text)
            Intent.VERIFY_TEXT -> extractVerifyTextEntities(normalized, text)
            Intent.VERIFY_ENABLED, Intent.VERIFY_DISABLED -> extractVerifyStateEntities(normalized, text)
            Intent.WAIT_DURATION -> extractWaitDurationEntities(normalized)
            Intent.WAIT_FOR_ELEMENT, Intent.WAIT_FOR_ELEMENT_GONE -> extractWaitForElementEntities(normalized, text)
            Intent.SCROLL, Intent.SWIPE -> extractScrollEntities(normalized, text)
            Intent.SELECT_OPTION -> extractSelectEntities(normalized, text)
            Intent.CHECK_CHECKBOX, Intent.UNCHECK_CHECKBOX -> extractCheckboxEntities(normalized, text)
            Intent.TOGGLE_SWITCH -> extractToggleEntities(normalized, text)
            else -> {
                // Try pattern extraction if available
                matchResult?.matchedPattern?.extractEntities(text) ?: ExtractedEntities()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE EXTRACTION METHODS
    // ═══════════════════════════════════════════════════════════════

    private fun extractClickEntities(normalized: String, original: String): ExtractedEntities {
        // Try quoted target first
        val quotedTarget = extractQuotedString(original)
        if (quotedTarget != null) {
            return ExtractedEntities(target = quotedTarget)
        }

        // Extract target after action words
        val actionWords = listOf("click", "tap", "press", "touch", "select", "hit",
            "double click", "double tap", "double-click", "long press", "hold")

        for (action in actionWords) {
            if (normalized.contains(action)) {
                val afterAction = normalized.substringAfter(action).trim()
                val target = cleanTarget(afterAction)
                if (target.isNotBlank()) {
                    return ExtractedEntities(target = target)
                }
            }
        }

        // Fallback: use everything after common prepositions
        val target = extractAfterPreposition(normalized, listOf("on", "the"))
        return ExtractedEntities(target = target ?: normalized)
    }

    private fun extractEnterTextEntities(normalized: String, original: String): ExtractedEntities {
        // Look for quoted value
        val quotedStrings = quotedStringPattern.findAll(original).map { it.groupValues[1] }.toList()

        return when {
            quotedStrings.size >= 2 -> {
                // First quoted is value, second is target (most common pattern)
                ExtractedEntities(value = quotedStrings[0], target = quotedStrings[1])
            }
            quotedStrings.size == 1 -> {
                // One quoted string - determine if value or target
                val quoted = quotedStrings[0]
                val afterQuote = original.substringAfter(quoted).lowercase()

                if (afterQuote.contains(Regex("""(?:in|into|on|at)\s+"""))) {
                    // Value followed by target
                    val target = extractAfterPreposition(afterQuote, listOf("in", "into", "on", "at"))
                    ExtractedEntities(value = quoted, target = cleanTarget(target ?: ""))
                } else {
                    // Assume it's the value, extract target before
                    val beforeQuote = original.substringBefore(quoted).lowercase()
                    val target = extractAfterPreposition(beforeQuote, listOf("in", "into", "on", "at"))
                    if (target != null) {
                        ExtractedEntities(target = cleanTarget(target), value = quoted)
                    } else {
                        ExtractedEntities(value = quoted)
                    }
                }
            }
            else -> ExtractedEntities()
        }
    }

    private fun extractClearEntities(normalized: String, original: String): ExtractedEntities {
        val quotedTarget = extractQuotedString(original)
        if (quotedTarget != null) {
            return ExtractedEntities(target = quotedTarget)
        }

        // After "clear", "erase", etc.
        val target = extractAfterWords(normalized, listOf("clear", "erase", "delete", "empty", "wipe"))
        return ExtractedEntities(target = cleanTarget(target ?: ""))
    }

    private fun extractVerifyDisplayedEntities(normalized: String, original: String): ExtractedEntities {
        val quotedTarget = extractQuotedString(original)
        if (quotedTarget != null) {
            return ExtractedEntities(target = quotedTarget)
        }

        // Extract element name before "is displayed", "is visible", etc.
        val stateWords = listOf("is displayed", "is visible", "is shown", "displayed", "visible", "shown", "appears", "present")
        for (state in stateWords) {
            if (normalized.contains(state)) {
                val beforeState = normalized.substringBefore(state).trim()
                val target = cleanTarget(beforeState.substringAfterLast(" that ").substringAfterLast(" "))
                if (target.isNotBlank()) {
                    return ExtractedEntities(target = target)
                }
            }
        }

        return ExtractedEntities()
    }

    private fun extractVerifyTextEntities(normalized: String, original: String): ExtractedEntities {
        val quotedStrings = quotedStringPattern.findAll(original).map { it.groupValues[1] }.toList()

        return when {
            quotedStrings.size >= 2 -> {
                // Target and expected text
                ExtractedEntities(target = quotedStrings[0], value = quotedStrings[1])
            }
            quotedStrings.size == 1 -> {
                // Likely the expected text
                val beforeQuote = normalized.substringBefore(quotedStrings[0].lowercase())
                val target = extractAfterWords(beforeQuote, listOf("verify", "check", "assert"))
                    ?.replace(Regex("""(shows?|contains?|has|displays?)\s*$"""), "")
                    ?.trim()

                ExtractedEntities(target = cleanTarget(target ?: "screen"), value = quotedStrings[0])
            }
            else -> ExtractedEntities()
        }
    }

    private fun extractVerifyStateEntities(normalized: String, original: String): ExtractedEntities {
        val quotedTarget = extractQuotedString(original)
        if (quotedTarget != null) {
            return ExtractedEntities(target = quotedTarget)
        }

        // Extract before "is enabled", "is disabled", etc.
        val stateWords = listOf("is enabled", "is disabled", "is active", "is inactive", "enabled", "disabled")
        for (state in stateWords) {
            if (normalized.contains(state)) {
                val beforeState = normalized.substringBefore(state).trim()
                val target = cleanTarget(beforeState.substringAfterLast(" that ").substringAfterLast(" "))
                if (target.isNotBlank()) {
                    return ExtractedEntities(target = target)
                }
            }
        }

        return ExtractedEntities()
    }

    private fun extractWaitDurationEntities(normalized: String): ExtractedEntities {
        val duration = synonyms.parseDuration(normalized)
        return ExtractedEntities(numericValue = duration ?: 1000)
    }

    private fun extractWaitForElementEntities(normalized: String, original: String): ExtractedEntities {
        val quotedTarget = extractQuotedString(original)
        if (quotedTarget != null) {
            return ExtractedEntities(target = quotedTarget)
        }

        // Extract element before "to appear", "to disappear", etc.
        val actionWords = listOf("to appear", "to disappear", "to be visible", "to be gone", "to vanish", "to show")
        for (action in actionWords) {
            if (normalized.contains(action)) {
                val beforeAction = normalized.substringBefore(action).trim()
                val target = extractAfterWords(beforeAction, listOf("wait for", "wait"))
                if (target != null && target.isNotBlank()) {
                    return ExtractedEntities(target = cleanTarget(target))
                }
            }
        }

        return ExtractedEntities()
    }

    private fun extractScrollEntities(normalized: String, original: String): ExtractedEntities {
        // Extract direction
        val directionMatch = directionPattern.find(normalized)
        val direction = directionMatch?.let { synonyms.normalizeDirection(it.value) }

        // Extract target (if any)
        val quotedTarget = extractQuotedString(original)
        val target = quotedTarget ?: extractAfterWords(normalized, listOf("to find", "to", "until"))

        return ExtractedEntities(
            direction = direction,
            target = target?.let { cleanTarget(it) }
        )
    }

    private fun extractSelectEntities(normalized: String, original: String): ExtractedEntities {
        val quotedStrings = quotedStringPattern.findAll(original).map { it.groupValues[1] }.toList()

        return when {
            quotedStrings.size >= 2 -> {
                ExtractedEntities(option = quotedStrings[0], target = quotedStrings[1])
            }
            quotedStrings.size == 1 -> {
                val afterQuote = normalized.substringAfter(quotedStrings[0].lowercase())
                val target = extractAfterPreposition(afterQuote, listOf("from", "in"))
                ExtractedEntities(option = quotedStrings[0], target = cleanTarget(target ?: ""))
            }
            else -> ExtractedEntities()
        }
    }

    private fun extractCheckboxEntities(normalized: String, original: String): ExtractedEntities {
        val quotedTarget = extractQuotedString(original)
        if (quotedTarget != null) {
            return ExtractedEntities(target = quotedTarget)
        }

        val target = extractAfterWords(normalized, listOf("check", "uncheck", "tick", "untick", "mark", "unmark"))
        return ExtractedEntities(target = cleanTarget(target ?: ""))
    }

    private fun extractToggleEntities(normalized: String, original: String): ExtractedEntities {
        val quotedTarget = extractQuotedString(original)
        val target = quotedTarget ?: extractAfterWords(normalized, listOf("toggle", "switch", "flip", "turn"))

        val state = when {
            normalized.endsWith(" on") -> "on"
            normalized.endsWith(" off") -> "off"
            else -> "toggle"
        }

        return ExtractedEntities(
            target = cleanTarget(target ?: ""),
            modifiers = mapOf("state" to state)
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════

    private fun extractQuotedString(text: String): String? {
        return quotedStringPattern.find(text)?.groupValues?.get(1)
    }

    private fun extractAfterPreposition(text: String, prepositions: List<String>): String? {
        for (prep in prepositions) {
            val pattern = Regex("""$prep\s+(?:the\s+)?(['"]?[^'"]+['"]?)""", RegexOption.IGNORE_CASE)
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    private fun extractAfterWords(text: String, words: List<String>): String? {
        for (word in words) {
            if (text.contains(word)) {
                val after = text.substringAfter(word).trim()
                if (after.isNotBlank()) {
                    return after
                }
            }
        }
        return null
    }

    private fun cleanTarget(target: String): String {
        return target
            .replace(Regex("""^(?:the|a|an)\s+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+(?:button|field|link|element|checkbox|switch|dropdown)$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^['"]|['"]$"""), "")
            .trim()
    }
}
