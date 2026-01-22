package com.testzen.core.nlp.synonyms

import org.slf4j.LoggerFactory

/**
 * Centralized registry for all NLP synonyms.
 *
 * The SynonymRegistry provides:
 * - Single source of truth for all synonym sets
 * - Organized storage by category
 * - Easy registration and retrieval
 * - Support for custom domain-specific synonyms
 * - Utility functions for matching and normalization
 *
 * Synonym Categories:
 * - ACTION_VERBS: Click, tap, enter, scroll, etc.
 * - STATE_WORDS: Displayed, enabled, checked, etc.
 * - PREPOSITIONS: In, on, to, from, etc.
 * - DIRECTIONS: Up, down, left, right
 * - TIME_UNITS: Seconds, milliseconds, minutes
 * - ELEMENT_TYPES: Button, field, checkbox, etc.
 *
 * Usage:
 * ```kotlin
 * // Get default registry with all built-in synonyms
 * val registry = SynonymRegistry.default()
 *
 * // Get synonyms by name
 * val clickWords = registry.get("CLICK")
 *
 * // Check if word matches
 * if (registry.matches("tap", "CLICK")) { ... }
 *
 * // Register custom synonyms
 * registry.register(SynonymCategory.ACTION_VERBS, "MY_ACTION") {
 *     words("activate", "trigger", "fire")
 * }
 * ```
 */
class SynonymRegistry private constructor() {
    private val logger = LoggerFactory.getLogger(SynonymRegistry::class.java)

    // Synonyms organized by category
    private val synonymsByCategory = mutableMapOf<SynonymCategory, MutableMap<String, SynonymSet>>()

    // All synonyms in a flat map by name
    private val allSynonyms = mutableMapOf<String, SynonymSet>()

    /**
     * Register a synonym set under a category.
     */
    fun register(category: SynonymCategory, name: String, synonymSet: SynonymSet) {
        synonymsByCategory.getOrPut(category) { mutableMapOf() }[name] = synonymSet
        allSynonyms[name] = synonymSet
        logger.debug("Registered synonym set '$name' in category $category with ${synonymSet.words.size} words")
    }

    /**
     * Register a synonym set using the DSL.
     */
    fun register(category: SynonymCategory, name: String, block: SynonymSetBuilder.() -> Unit) {
        val builder = SynonymSetBuilder(name)
        builder.block()
        register(category, name, builder.build())
    }

    /**
     * Register multiple synonym sets in a category.
     */
    fun register(category: SynonymCategory, block: SynonymCategoryBuilder.() -> Unit) {
        val builder = SynonymCategoryBuilder(this, category)
        builder.block()
    }

    /**
     * Get a synonym set by name.
     */
    fun get(name: String): Set<String> {
        return allSynonyms[name]?.words ?: emptySet()
    }

    /**
     * Get a SynonymSet object by name.
     */
    fun getSynonymSet(name: String): SynonymSet? {
        return allSynonyms[name]
    }

    /**
     * Get all synonym sets in a category.
     */
    fun getByCategory(category: SynonymCategory): Map<String, SynonymSet> {
        return synonymsByCategory[category]?.toMap() ?: emptyMap()
    }

    /**
     * Check if a word matches any synonym in a named set.
     */
    fun matches(word: String, synonymSetName: String): Boolean {
        val synonymSet = allSynonyms[synonymSetName] ?: return false
        return synonymSet.matches(word)
    }

    /**
     * Check if text contains any synonym from a named set.
     */
    fun containsAny(text: String, synonymSetName: String): Boolean {
        val synonymSet = allSynonyms[synonymSetName] ?: return false
        return synonymSet.containsAnyIn(text)
    }

    /**
     * Find the first matching synonym in text.
     */
    fun findFirst(text: String, synonymSetName: String): String? {
        val synonymSet = allSynonyms[synonymSetName] ?: return null
        return synonymSet.findFirstIn(text)
    }

    /**
     * Get all registered synonym set names.
     */
    fun getAllNames(): Set<String> = allSynonyms.keys.toSet()

    /**
     * Get statistics about registered synonyms.
     */
    fun getStats(): SynonymStats {
        return SynonymStats(
            totalSets = allSynonyms.size,
            totalWords = allSynonyms.values.sumOf { it.words.size },
            byCategory = synonymsByCategory.mapValues { it.value.size },
            categories = synonymsByCategory.keys.toSet()
        )
    }

    /**
     * Clear all synonyms (useful for testing).
     */
    fun clear() {
        synonymsByCategory.clear()
        allSynonyms.clear()
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY FUNCTIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Normalize direction from various forms.
     */
    fun normalizeDirection(text: String): String? {
        val normalized = text.lowercase()
        return when {
            containsAny(normalized, "UP") -> "up"
            containsAny(normalized, "DOWN") -> "down"
            containsAny(normalized, "LEFT") -> "left"
            containsAny(normalized, "RIGHT") -> "right"
            else -> null
        }
    }

    /**
     * Parse duration from text (returns milliseconds).
     */
    fun parseDuration(text: String): Long? {
        val normalized = text.lowercase()
        val numberMatch = Regex("""(\d+(?:\.\d+)?)""").find(normalized) ?: return null
        val value = numberMatch.groupValues[1].toDoubleOrNull() ?: return null

        return when {
            containsAny(normalized, "MILLISECONDS") -> value.toLong()
            containsAny(normalized, "SECONDS") -> (value * 1000).toLong()
            containsAny(normalized, "MINUTES") -> (value * 60000).toLong()
            else -> (value * 1000).toLong() // Default to seconds
        }
    }

    companion object {
        /**
         * Create a new empty registry.
         */
        fun empty(): SynonymRegistry = SynonymRegistry()

        /**
         * Create a registry with all default built-in synonyms.
         */
        fun default(): SynonymRegistry {
            val registry = SynonymRegistry()
            DefaultSynonyms.registerAll(registry)
            return registry
        }

        /**
         * Create a registry with only specific categories.
         */
        fun withCategories(vararg categories: SynonymCategory): SynonymRegistry {
            val registry = SynonymRegistry()
            categories.forEach { category ->
                DefaultSynonyms.registerCategory(registry, category)
            }
            return registry
        }
    }
}

/**
 * Synonym categories for organization.
 */
enum class SynonymCategory {
    /** Action verbs: click, tap, enter, scroll, etc. */
    ACTION_VERBS,

    /** State words: displayed, enabled, checked, etc. */
    STATE_WORDS,

    /** Preposition words: in, on, to, from, etc. */
    PREPOSITIONS,

    /** Direction words: up, down, left, right */
    DIRECTIONS,

    /** Time unit words: seconds, milliseconds, minutes */
    TIME_UNITS,

    /** Element type words: button, field, checkbox, etc. */
    ELEMENT_TYPES,

    /** Custom domain-specific synonyms */
    CUSTOM
}

/**
 * A set of synonyms with utility methods.
 */
data class SynonymSet(
    val name: String,
    val words: Set<String>,
    val description: String = "",
    val aliases: Set<String> = emptySet()
) {
    /**
     * Check if a word matches any synonym.
     */
    fun matches(word: String): Boolean {
        val normalized = word.lowercase().trim()
        return words.any { synonym ->
            normalized == synonym ||
            normalized.startsWith(synonym) ||
            normalized.endsWith(synonym)
        }
    }

    /**
     * Check if text contains any synonym.
     */
    fun containsAnyIn(text: String): Boolean {
        val normalized = text.lowercase()
        return words.any { normalized.contains(it) }
    }

    /**
     * Find the first matching synonym in text.
     */
    fun findFirstIn(text: String): String? {
        val normalized = text.lowercase()
        return words.firstOrNull { normalized.contains(it) }
    }

    /**
     * Convert to regex pattern that matches any synonym.
     */
    fun toRegexPattern(): String {
        return "(?:${words.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }})"
    }
}

/**
 * Statistics about registered synonyms.
 */
data class SynonymStats(
    val totalSets: Int,
    val totalWords: Int,
    val byCategory: Map<SynonymCategory, Int>,
    val categories: Set<SynonymCategory>
) {
    override fun toString(): String = buildString {
        appendLine("SynonymRegistry Stats:")
        appendLine("  Total sets: $totalSets")
        appendLine("  Total words: $totalWords")
        appendLine("  Categories: ${categories.size}")
        byCategory.forEach { (category, count) ->
            appendLine("    $category: $count sets")
        }
    }
}
