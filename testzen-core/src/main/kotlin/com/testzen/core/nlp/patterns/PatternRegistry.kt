package com.testzen.core.nlp.patterns

import com.testzen.core.nlp.Intent
import com.testzen.core.nlp.IntentPattern
import org.slf4j.LoggerFactory

/**
 * Centralized registry for all NLP intent patterns.
 *
 * The PatternRegistry provides:
 * - Single source of truth for all patterns
 * - Organized pattern storage by category
 * - Easy registration and retrieval of patterns
 * - Support for custom/third-party patterns
 * - Pattern statistics and debugging
 *
 * Pattern Categories:
 * - INTERACTION: Click, tap, enter text, clear, etc.
 * - VERIFICATION: Verify displayed, verify text, verify state, etc.
 * - NAVIGATION: Scroll, swipe, back, forward, etc.
 * - WAIT: Wait duration, wait for element, etc.
 * - APP_LIFECYCLE: Launch, close, screenshot, etc.
 * - FORM_CONTROL: Select, check, uncheck, toggle, etc.
 *
 * Usage:
 * ```kotlin
 * // Get default registry with all built-in patterns
 * val registry = PatternRegistry.default()
 *
 * // Get patterns by category
 * val verificationPatterns = registry.getByCategory(PatternCategory.VERIFICATION)
 *
 * // Get patterns for a specific intent
 * val clickPatterns = registry.getByIntent(Intent.CLICK)
 *
 * // Register custom patterns
 * registry.register(PatternCategory.CUSTOM, myCustomPattern)
 * ```
 */
class PatternRegistry private constructor() {
    private val logger = LoggerFactory.getLogger(PatternRegistry::class.java)

    // Patterns organized by category
    private val patternsByCategory = mutableMapOf<PatternCategory, MutableList<IntentPattern>>()

    // All patterns in a flat list (for matching)
    private val allPatterns = mutableListOf<IntentPattern>()

    // Index by intent for fast lookup
    private val patternsByIntent = mutableMapOf<Intent, MutableList<IntentPattern>>()

    /**
     * Register a single pattern under a category.
     */
    fun register(category: PatternCategory, pattern: IntentPattern) {
        patternsByCategory.getOrPut(category) { mutableListOf() }.add(pattern)
        patternsByIntent.getOrPut(pattern.intent) { mutableListOf() }.add(pattern)
        allPatterns.add(pattern)
        logger.debug("Registered pattern for ${pattern.intent} in category $category")
    }

    /**
     * Register multiple patterns under a category.
     */
    fun register(category: PatternCategory, patterns: List<IntentPattern>) {
        patterns.forEach { register(category, it) }
    }

    /**
     * Register patterns using the DSL.
     */
    fun register(category: PatternCategory, block: PatternCollectionBuilder.() -> Unit) {
        val patterns = patterns(block)
        register(category, patterns)
    }

    /**
     * Get all patterns in a specific category.
     */
    fun getByCategory(category: PatternCategory): List<IntentPattern> {
        return patternsByCategory[category]?.toList() ?: emptyList()
    }

    /**
     * Get all patterns for a specific intent.
     */
    fun getByIntent(intent: Intent): List<IntentPattern> {
        return patternsByIntent[intent]?.toList() ?: emptyList()
    }

    /**
     * Get all registered patterns.
     */
    fun getAllPatterns(): List<IntentPattern> {
        return allPatterns.toList()
    }

    /**
     * Get pattern count statistics.
     */
    fun getStats(): PatternStats {
        return PatternStats(
            totalPatterns = allPatterns.size,
            byCategory = patternsByCategory.mapValues { it.value.size },
            byIntent = patternsByIntent.mapValues { it.value.size },
            categories = patternsByCategory.keys.toSet()
        )
    }

    /**
     * Clear all patterns (useful for testing).
     */
    fun clear() {
        patternsByCategory.clear()
        patternsByIntent.clear()
        allPatterns.clear()
    }

    /**
     * Check if registry has patterns for an intent.
     */
    fun hasPatternFor(intent: Intent): Boolean {
        return patternsByIntent.containsKey(intent)
    }

    companion object {
        /**
         * Create a new empty registry.
         */
        fun empty(): PatternRegistry = PatternRegistry()

        /**
         * Create a registry with all default built-in patterns.
         */
        fun default(): PatternRegistry {
            val registry = PatternRegistry()
            DefaultPatterns.registerAll(registry)
            return registry
        }

        /**
         * Create a registry with only specific categories.
         */
        fun withCategories(vararg categories: PatternCategory): PatternRegistry {
            val registry = PatternRegistry()
            categories.forEach { category ->
                DefaultPatterns.registerCategory(registry, category)
            }
            return registry
        }
    }
}

/**
 * Pattern categories for organization.
 */
enum class PatternCategory {
    /** Click, tap, enter text, clear, double tap, long press */
    INTERACTION,

    /** All verification/assertion patterns */
    VERIFICATION,

    /** Scroll, swipe, back, forward */
    NAVIGATION,

    /** Wait duration, wait for element */
    WAIT,

    /** Launch, close, screenshot */
    APP_LIFECYCLE,

    /** Select, check, uncheck, toggle */
    FORM_CONTROL,

    /** Custom patterns added by users */
    CUSTOM
}

/**
 * Statistics about registered patterns.
 */
data class PatternStats(
    val totalPatterns: Int,
    val byCategory: Map<PatternCategory, Int>,
    val byIntent: Map<Intent, Int>,
    val categories: Set<PatternCategory>
) {
    override fun toString(): String = buildString {
        appendLine("PatternRegistry Stats:")
        appendLine("  Total patterns: $totalPatterns")
        appendLine("  Categories: ${categories.size}")
        byCategory.forEach { (category, count) ->
            appendLine("    $category: $count patterns")
        }
    }
}
