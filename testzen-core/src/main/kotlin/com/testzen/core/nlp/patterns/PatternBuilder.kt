package com.testzen.core.nlp.patterns

import com.testzen.core.nlp.ExtractedEntities
import com.testzen.core.nlp.Intent
import com.testzen.core.nlp.IntentPattern

/**
 * DSL builder for creating NLP patterns in a clean, readable way.
 *
 * Usage:
 * ```kotlin
 * val pattern = pattern(Intent.CLICK) {
 *     keywords("click", "tap", "press")
 *     regex { "${F.CLICK}\\s+${F.ON}${F.THE}${F.TARGET_CLEAN}${F.BUTTON}$" }
 *     priority(1.0)
 * }
 * ```
 *
 * Or using the collection builder:
 * ```kotlin
 * val patterns = patterns {
 *     pattern(Intent.CLICK) {
 *         keywords("click", "tap")
 *         regex { ... }
 *     }
 *     pattern(Intent.DOUBLE_CLICK) {
 *         keywords("double click")
 *         regex { ... }
 *     }
 * }
 * ```
 */
@DslMarker
annotation class PatternDsl

/**
 * Builder for a single IntentPattern.
 */
@PatternDsl
class PatternDefinitionBuilder(private val intent: Intent) {
    private var keywords: Set<String> = emptySet()
    private var regex: Regex? = null
    private var priority: Double = 1.0
    private var validator: ((String) -> Boolean)? = null
    private var entityExtractor: ((MatchResult) -> ExtractedEntities)? = null
    private var description: String? = null

    /**
     * Set keywords that must be present for this pattern to match.
     */
    fun keywords(vararg words: String) {
        keywords = words.toSet()
    }

    /**
     * Set keywords from an existing set.
     */
    fun keywords(words: Set<String>) {
        keywords = words
    }

    /**
     * Set the regex pattern using a lambda for readability.
     */
    fun regex(ignoreCase: Boolean = true, builder: () -> String) {
        val options = if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
        regex = Regex(builder(), options)
    }

    /**
     * Set the regex pattern directly.
     */
    fun regex(pattern: String, ignoreCase: Boolean = true) {
        val options = if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
        regex = Regex(pattern, options)
    }

    /**
     * Set the regex directly.
     */
    fun regex(pattern: Regex) {
        regex = pattern
    }

    /**
     * Set the priority (higher = more likely to be selected).
     */
    fun priority(value: Double) {
        priority = value
    }

    /**
     * Set a validator function that must return true for the pattern to match.
     */
    fun validate(predicate: (String) -> Boolean) {
        validator = predicate
    }

    /**
     * Shorthand validators for common conditions.
     */
    fun requiresNot(word: String) {
        val existing = validator
        validator = { text ->
            val current = !text.contains(word)
            if (existing != null) existing(text) && current else current
        }
    }

    fun requiresAny(vararg words: String) {
        val existing = validator
        validator = { text ->
            val current = words.any { text.contains(it) }
            if (existing != null) existing(text) && current else current
        }
    }

    /**
     * Set an entity extractor for extracting values from the match.
     */
    fun extractEntities(extractor: (MatchResult) -> ExtractedEntities) {
        entityExtractor = extractor
    }

    /**
     * Common entity extractors.
     */
    fun extractTarget() {
        entityExtractor = { match ->
            ExtractedEntities(target = match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() })
        }
    }

    fun extractTargetAndValue() {
        entityExtractor = { match ->
            ExtractedEntities(
                target = match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() },
                value = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
            )
        }
    }

    fun extractValueAndTarget() {
        entityExtractor = { match ->
            ExtractedEntities(
                target = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() },
                value = match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
            )
        }
    }

    fun extractDirection() {
        entityExtractor = { match ->
            ExtractedEntities(
                direction = normalizeDirection(match.groupValues.getOrNull(1)),
                target = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
            )
        }
    }

    fun extractOptionAndTarget() {
        entityExtractor = { match ->
            ExtractedEntities(
                option = match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() },
                target = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
            )
        }
    }

    fun extractNumericValue() {
        entityExtractor = { match ->
            ExtractedEntities(
                target = match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() },
                numericValue = match.groupValues.getOrNull(2)?.toLongOrNull()
            )
        }
    }

    fun extractAttribute() {
        entityExtractor = { match ->
            ExtractedEntities(
                target = match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() },
                attributeName = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() },
                value = match.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }
            )
        }
    }

    fun extractRegexPattern() {
        entityExtractor = { match ->
            ExtractedEntities(
                target = match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() },
                regexPattern = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
            )
        }
    }

    /**
     * Set a description for documentation purposes.
     */
    fun description(desc: String) {
        description = desc
    }

    /**
     * Build the IntentPattern.
     */
    fun build(): IntentPattern {
        requireNotNull(regex) { "Regex is required for pattern" }
        require(keywords.isNotEmpty()) { "Keywords are required for pattern" }

        return IntentPattern(
            intent = intent,
            keywords = keywords,
            regex = regex!!,
            priority = priority,
            validator = validator,
            entityExtractor = entityExtractor
        )
    }

    private fun normalizeDirection(direction: String?): String? {
        return when (direction?.lowercase()) {
            "up", "u" -> "up"
            "down", "d" -> "down"
            "left", "l" -> "left"
            "right", "r" -> "right"
            else -> direction
        }
    }
}

/**
 * Builder for a collection of patterns.
 */
@PatternDsl
class PatternCollectionBuilder {
    private val patterns = mutableListOf<IntentPattern>()

    /**
     * Add a pattern using the DSL.
     */
    fun pattern(intent: Intent, block: PatternDefinitionBuilder.() -> Unit) {
        val builder = PatternDefinitionBuilder(intent)
        builder.block()
        patterns.add(builder.build())
    }

    /**
     * Add an existing pattern.
     */
    fun add(pattern: IntentPattern) {
        patterns.add(pattern)
    }

    /**
     * Add multiple existing patterns.
     */
    fun addAll(patternsToAdd: List<IntentPattern>) {
        patterns.addAll(patternsToAdd)
    }

    /**
     * Build the list of patterns.
     */
    fun build(): List<IntentPattern> = patterns.toList()
}

/**
 * DSL entry point for creating a single pattern.
 */
fun pattern(intent: Intent, block: PatternDefinitionBuilder.() -> Unit): IntentPattern {
    val builder = PatternDefinitionBuilder(intent)
    builder.block()
    return builder.build()
}

/**
 * DSL entry point for creating multiple patterns.
 */
fun patterns(block: PatternCollectionBuilder.() -> Unit): List<IntentPattern> {
    val builder = PatternCollectionBuilder()
    builder.block()
    return builder.build()
}
