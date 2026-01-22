package com.testzen.core.verification.registry

import com.testzen.core.verification.ComparisonOperator
import com.testzen.core.verification.VerificationType
import org.slf4j.LoggerFactory

/**
 * Centralized registry for all verification type definitions.
 *
 * The VerificationRegistry provides:
 * - Single source of truth for all verification definitions
 * - Organized storage by category
 * - Metadata about each verification type
 * - Support for custom verification types
 * - NLP pattern hints for natural language matching
 *
 * Verification Categories:
 * - PRESENCE: Displayed, exists, hidden, gone
 * - STATE: Enabled, disabled, checked, selected, focused
 * - TEXT: Equals, contains, starts with, matches regex
 * - ATTRIBUTE: Attribute equals, contains, exists
 * - CSS: CSS property verification
 * - COUNT: Count equals, greater than, less than, between
 * - COLLECTION: All displayed, any displayed, none displayed
 * - PAGE: Page title, URL, page source
 *
 * Usage:
 * ```kotlin
 * // Get default registry with all built-in verifications
 * val registry = VerificationRegistry.default()
 *
 * // Get verifications by category
 * val textVerifications = registry.getByCategory(VerificationCategory.TEXT)
 *
 * // Get definition for a specific type
 * val def = registry.getDefinition(VerificationType.TEXT_CONTAINS)
 *
 * // Register custom verification
 * registry.register(VerificationCategory.CUSTOM, customDefinition)
 * ```
 */
class VerificationRegistry private constructor() {
    private val logger = LoggerFactory.getLogger(VerificationRegistry::class.java)

    // Verifications organized by category
    private val verificationsByCategory = mutableMapOf<VerificationCategory, MutableList<VerificationDefinition>>()

    // Index by type for fast lookup
    private val verificationsByType = mutableMapOf<VerificationType, VerificationDefinition>()

    /**
     * Register a verification definition under a category.
     */
    fun register(category: VerificationCategory, definition: VerificationDefinition) {
        verificationsByCategory.getOrPut(category) { mutableListOf() }.add(definition)
        verificationsByType[definition.type] = definition
        logger.debug("Registered verification ${definition.type} in category $category")
    }

    /**
     * Register multiple definitions under a category.
     */
    fun register(category: VerificationCategory, definitions: List<VerificationDefinition>) {
        definitions.forEach { register(category, it) }
    }

    /**
     * Register verifications using the DSL.
     */
    fun register(category: VerificationCategory, block: VerificationCategoryBuilder.() -> Unit) {
        val builder = VerificationCategoryBuilder(this, category)
        builder.block()
    }

    /**
     * Get a verification definition by type.
     */
    fun getDefinition(type: VerificationType): VerificationDefinition? {
        return verificationsByType[type]
    }

    /**
     * Get all verifications in a specific category.
     */
    fun getByCategory(category: VerificationCategory): List<VerificationDefinition> {
        return verificationsByCategory[category]?.toList() ?: emptyList()
    }

    /**
     * Get all registered definitions.
     */
    fun getAllDefinitions(): List<VerificationDefinition> {
        return verificationsByType.values.toList()
    }

    /**
     * Get all verification types.
     */
    fun getAllTypes(): Set<VerificationType> {
        return verificationsByType.keys.toSet()
    }

    /**
     * Get NLP keywords for a verification type.
     */
    fun getKeywords(type: VerificationType): Set<String> {
        return verificationsByType[type]?.nlpKeywords ?: emptySet()
    }

    /**
     * Find verification types matching keywords.
     */
    fun findByKeywords(keywords: Set<String>): List<VerificationDefinition> {
        return verificationsByType.values.filter { def ->
            def.nlpKeywords.any { kw -> keywords.any { it.contains(kw) || kw.contains(it) } }
        }
    }

    /**
     * Get statistics about registered verifications.
     */
    fun getStats(): VerificationStats {
        return VerificationStats(
            totalVerifications = verificationsByType.size,
            byCategory = verificationsByCategory.mapValues { it.value.size },
            categories = verificationsByCategory.keys.toSet()
        )
    }

    /**
     * Clear all verifications (useful for testing).
     */
    fun clear() {
        verificationsByCategory.clear()
        verificationsByType.clear()
    }

    companion object {
        /**
         * Create a new empty registry.
         */
        fun empty(): VerificationRegistry = VerificationRegistry()

        /**
         * Create a registry with all default built-in verifications.
         */
        fun default(): VerificationRegistry {
            val registry = VerificationRegistry()
            DefaultVerifications.registerAll(registry)
            return registry
        }

        /**
         * Create a registry with only specific categories.
         */
        fun withCategories(vararg categories: VerificationCategory): VerificationRegistry {
            val registry = VerificationRegistry()
            categories.forEach { category ->
                DefaultVerifications.registerCategory(registry, category)
            }
            return registry
        }
    }
}

/**
 * Verification categories for organization.
 */
enum class VerificationCategory {
    /** Element presence: displayed, exists, hidden, gone */
    PRESENCE,

    /** Element state: enabled, disabled, checked, selected, focused */
    STATE,

    /** Text verification: equals, contains, starts with, regex */
    TEXT,

    /** Attribute verification: equals, contains, exists */
    ATTRIBUTE,

    /** CSS property verification */
    CSS,

    /** Count/numeric verification: equals, greater than, less than */
    COUNT,

    /** Collection verification: all, any, none */
    COLLECTION,

    /** Page-level verification: title, URL, source */
    PAGE,

    /** Custom verifications */
    CUSTOM
}

/**
 * Definition of a verification type with metadata.
 */
data class VerificationDefinition(
    /** The verification type */
    val type: VerificationType,

    /** Display name */
    val displayName: String,

    /** Description of what this verification does */
    val description: String,

    /** NLP keywords that trigger this verification */
    val nlpKeywords: Set<String>,

    /** NLP patterns for natural language matching */
    val nlpPatterns: List<String> = emptyList(),

    /** Supported comparison operators */
    val supportedOperators: Set<ComparisonOperator> = setOf(ComparisonOperator.EQUALS),

    /** Whether this verification requires a target element */
    val requiresTarget: Boolean = true,

    /** Whether this verification requires an expected value */
    val requiresExpectedValue: Boolean = false,

    /** Whether this verification supports soft assertions */
    val supportsSoftAssert: Boolean = true,

    /** Whether this verification supports retry */
    val supportsRetry: Boolean = true,

    /** Example usage */
    val examples: List<String> = emptyList(),

    /** Additional metadata */
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * Check if this verification matches the given keywords.
     */
    fun matchesKeywords(text: String): Boolean {
        val normalized = text.lowercase()
        return nlpKeywords.any { normalized.contains(it) }
    }

    /**
     * Get a human-readable description for reports.
     */
    fun getReportDescription(target: String?, expected: Any?): String {
        return when {
            requiresExpectedValue && expected != null -> "$displayName: '$target' = '$expected'"
            requiresTarget && target != null -> "$displayName: '$target'"
            else -> displayName
        }
    }
}

/**
 * Statistics about registered verifications.
 */
data class VerificationStats(
    val totalVerifications: Int,
    val byCategory: Map<VerificationCategory, Int>,
    val categories: Set<VerificationCategory>
) {
    override fun toString(): String = buildString {
        appendLine("VerificationRegistry Stats:")
        appendLine("  Total verifications: $totalVerifications")
        appendLine("  Categories: ${categories.size}")
        byCategory.forEach { (category, count) ->
            appendLine("    $category: $count verifications")
        }
    }
}
