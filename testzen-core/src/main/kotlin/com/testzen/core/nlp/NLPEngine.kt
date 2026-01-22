package com.testzen.core.nlp

import com.testzen.core.nlp.synonyms.SynonymRegistry
import org.slf4j.LoggerFactory

/**
 * Main NLP engine for parsing natural language test instructions.
 *
 * Orchestrates intent classification and entity extraction to produce
 * structured NLPResult objects from free-form text.
 *
 * Features:
 * - Robust intent classification with synonym support
 * - Flexible entity extraction for various patterns
 * - Confidence scoring for ambiguous inputs
 * - Extensible pattern registry
 *
 * Usage:
 * ```kotlin
 * val engine = NLPEngine()
 * val result = engine.parse("click the login button")
 * println(result.intent) // CLICK
 * println(result.entities.target) // "login"
 * ```
 */
class NLPEngine {
    private val logger = LoggerFactory.getLogger(NLPEngine::class.java)

    private val intentMatcher = IntentMatcher()
    private val entityExtractor = EntityExtractor()
    private val synonyms by lazy { SynonymRegistry.default() }

    // Minimum confidence threshold for accepting a match
    private var confidenceThreshold = 0.3

    /**
     * Parse a natural language instruction into structured NLPResult.
     */
    fun parse(text: String): NLPResult {
        val trimmed = text.trim()
        val normalized = normalizeText(trimmed)

        logger.debug("NLP parsing: '{}'", trimmed)

        // Step 1: Match intent
        val intentResult = intentMatcher.match(normalized)

        // Step 2: Extract entities
        val entities = if (intentResult.intent != Intent.UNKNOWN && intentResult.matchedPattern != null) {
            // Try pattern-specific extraction first
            intentResult.matchedPattern.extractEntities(trimmed)
                ?: entityExtractor.extract(trimmed, intentResult.intent, intentResult)
        } else {
            entityExtractor.extract(trimmed, intentResult.intent, intentResult)
        }

        // Step 3: Build result
        val result = NLPResult(
            intent = intentResult.intent,
            confidence = intentResult.confidence,
            entities = entities,
            originalText = trimmed,
            normalizedText = normalized,
            alternatives = intentResult.alternatives.map { alt ->
                NLPResult(
                    intent = alt.intent,
                    confidence = alt.confidence,
                    entities = entityExtractor.extract(trimmed, alt.intent, alt),
                    originalText = trimmed,
                    normalizedText = normalized
                )
            }
        )

        logger.debug("NLP result: intent={}, confidence={}, target={}",
            result.intent, result.confidence, result.entities.target)

        return result
    }

    /**
     * Parse with fallback behavior for unknown intents.
     *
     * If the confidence is below threshold and a fallback intent is provided,
     * use the fallback instead.
     */
    fun parseWithFallback(text: String, fallbackIntent: Intent = Intent.CLICK): NLPResult {
        val result = parse(text)

        return if (result.intent == Intent.UNKNOWN || result.confidence < confidenceThreshold) {
            // Use fallback, treating the text as target
            NLPResult(
                intent = fallbackIntent,
                confidence = 0.1,
                entities = ExtractedEntities(target = cleanFallbackTarget(text)),
                originalText = result.originalText,
                normalizedText = result.normalizedText
            )
        } else {
            result
        }
    }

    /**
     * Set the confidence threshold for accepting matches.
     */
    fun setConfidenceThreshold(threshold: Double) {
        this.confidenceThreshold = threshold.coerceIn(0.0, 1.0)
    }

    /**
     * Register a custom intent pattern.
     */
    fun registerPattern(pattern: IntentPattern) {
        intentMatcher.registerPattern(pattern)
    }

    /**
     * Batch parse multiple instructions.
     */
    fun parseBatch(texts: List<String>): List<NLPResult> {
        return texts.map { parse(it) }
    }

    /**
     * Check if text likely contains a valid instruction.
     */
    fun isValidInstruction(text: String): Boolean {
        val result = parse(text)
        return result.intent != Intent.UNKNOWN && result.confidence >= confidenceThreshold
    }

    /**
     * Get intent suggestions for a partial input.
     * Useful for autocomplete/suggestions.
     */
    fun suggestIntents(partialText: String): List<Intent> {
        val normalized = normalizeText(partialText)
        val suggestions = mutableSetOf<Intent>()

        // Check which action keywords match the partial text
        if (synonyms.containsAny(normalized, "CLICK")) suggestions.add(Intent.CLICK)
        if (synonyms.containsAny(normalized, "ENTER_TEXT")) suggestions.add(Intent.ENTER_TEXT)
        if (synonyms.containsAny(normalized, "VERIFY")) {
            suggestions.add(Intent.VERIFY_DISPLAYED)
            suggestions.add(Intent.VERIFY_TEXT)
        }
        if (synonyms.containsAny(normalized, "WAIT")) {
            suggestions.add(Intent.WAIT_DURATION)
            suggestions.add(Intent.WAIT_FOR_ELEMENT)
        }
        if (synonyms.containsAny(normalized, "SCROLL")) suggestions.add(Intent.SCROLL)
        if (synonyms.containsAny(normalized, "SWIPE")) suggestions.add(Intent.SWIPE)

        return suggestions.toList()
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════

    private fun normalizeText(text: String): String {
        return text.lowercase()
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun cleanFallbackTarget(text: String): String {
        return text
            .replace(Regex("""^(?:click|tap|press)\s+(?:on\s+)?(?:the\s+)?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+button$""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    companion object {
        /** Singleton instance for convenience */
        val default: NLPEngine by lazy { NLPEngine() }

        /**
         * Quick parse helper.
         */
        fun quickParse(text: String): NLPResult = default.parse(text)
    }
}
