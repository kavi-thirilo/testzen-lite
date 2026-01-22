package com.testzen.core.nlp

import com.testzen.core.nlp.patterns.PatternCategory
import com.testzen.core.nlp.patterns.PatternRegistry
import org.slf4j.LoggerFactory

/**
 * Pattern-based intent classification with confidence scoring.
 *
 * Uses the centralized PatternRegistry for all pattern management.
 * Supports both built-in patterns and custom user-defined patterns.
 *
 * Usage:
 * ```kotlin
 * // Default with all built-in patterns
 * val matcher = IntentMatcher()
 *
 * // Custom registry
 * val matcher = IntentMatcher(PatternRegistry.withCategories(
 *     PatternCategory.INTERACTION,
 *     PatternCategory.VERIFICATION
 * ))
 *
 * // Match text
 * val result = matcher.match("click the login button")
 * println("Intent: ${result.intent}, Confidence: ${result.confidence}")
 * ```
 */
class IntentMatcher(
    private val registry: PatternRegistry = PatternRegistry.default()
) {
    private val logger = LoggerFactory.getLogger(IntentMatcher::class.java)

    /**
     * Match text against all patterns and return the best intent.
     */
    fun match(text: String): IntentMatchResult {
        val normalized = normalizeText(text)
        val matches = mutableListOf<IntentMatchResult>()

        for (pattern in registry.getAllPatterns()) {
            val confidence = pattern.match(normalized, text)
            if (confidence > 0.0) {
                matches.add(IntentMatchResult(
                    intent = pattern.intent,
                    confidence = confidence,
                    matchedPattern = pattern
                ))
            }
        }

        // Sort by confidence (highest first)
        matches.sortByDescending { it.confidence }

        return if (matches.isNotEmpty()) {
            val best = matches.first()
            best.copy(alternatives = matches.drop(1).take(3))
        } else {
            IntentMatchResult(Intent.UNKNOWN, 0.0, null)
        }
    }

    /**
     * Register a custom pattern.
     */
    fun registerPattern(pattern: IntentPattern) {
        registry.register(PatternCategory.CUSTOM, pattern)
    }

    /**
     * Register multiple custom patterns.
     */
    fun registerPatterns(patterns: List<IntentPattern>) {
        registry.register(PatternCategory.CUSTOM, patterns)
    }

    /**
     * Get the underlying registry for advanced operations.
     */
    fun getRegistry(): PatternRegistry = registry

    /**
     * Get statistics about registered patterns.
     */
    fun getStats() = registry.getStats()

    /**
     * Normalize text for matching.
     */
    private fun normalizeText(text: String): String {
        return text.lowercase()
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    companion object {
        /**
         * Create a matcher with only specific pattern categories.
         */
        fun withCategories(vararg categories: PatternCategory): IntentMatcher {
            return IntentMatcher(PatternRegistry.withCategories(*categories))
        }

        /**
         * Create a matcher with an empty registry (for testing or custom patterns only).
         */
        fun empty(): IntentMatcher {
            return IntentMatcher(PatternRegistry.empty())
        }
    }
}

/**
 * Result of intent matching.
 */
data class IntentMatchResult(
    val intent: Intent,
    val confidence: Double,
    val matchedPattern: IntentPattern?,
    val alternatives: List<IntentMatchResult> = emptyList()
)

/**
 * Defines a pattern for matching an intent.
 */
data class IntentPattern(
    val intent: Intent,
    val keywords: Set<String>,
    val regex: Regex,
    val priority: Double = 1.0,
    val validator: ((String) -> Boolean)? = null,
    val entityExtractor: ((MatchResult) -> ExtractedEntities)? = null
) {
    /**
     * Match text against this pattern.
     * @return Confidence score (0.0 to 1.0), or 0.0 if no match
     */
    fun match(normalizedText: String, originalText: String): Double {
        // First check keywords (fast path)
        val hasKeyword = keywords.any { normalizedText.contains(it) }
        if (!hasKeyword) return 0.0

        // Validate if validator provided
        if (validator != null && !validator.invoke(normalizedText)) {
            return 0.0
        }

        // Try regex match
        val regexMatch = regex.find(normalizedText)
        if (regexMatch != null) {
            // Base confidence from regex match
            val matchCoverage = regexMatch.value.length.toDouble() / normalizedText.length
            return (0.5 + matchCoverage * 0.5) * priority
        }

        // Partial match based on keywords
        return 0.3 * priority
    }

    /**
     * Extract entities from a match result.
     */
    fun extractEntities(text: String): ExtractedEntities? {
        val match = regex.find(text.lowercase()) ?: return null

        return if (entityExtractor != null) {
            entityExtractor.invoke(match)
        } else {
            // Default: first group is target
            val target = match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
            ExtractedEntities(target = target)
        }
    }
}
