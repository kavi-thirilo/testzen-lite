package com.testzen.core.verification.registry

import com.testzen.core.verification.ComparisonOperator
import com.testzen.core.verification.VerificationType

/**
 * DSL builders for creating verification definitions in a clean, readable way.
 *
 * Usage:
 * ```kotlin
 * // Single verification definition
 * val def = verification(VerificationType.TEXT_CONTAINS) {
 *     displayName("Text Contains")
 *     description("Verify element text contains expected value")
 *     keywords("contains", "includes", "has text")
 *     patterns(
 *         "verify .* contains .*",
 *         "check .* includes .*"
 *     )
 *     operators(ComparisonOperator.CONTAINS)
 *     requiresTarget()
 *     requiresExpectedValue()
 *     examples(
 *         "Verify message contains 'success'",
 *         "Check title includes 'welcome'"
 *     )
 * }
 *
 * // Multiple definitions in a category
 * registry.register(VerificationCategory.TEXT) {
 *     verification(VerificationType.TEXT_EQUALS) {
 *         displayName("Text Equals")
 *         keywords("equals", "is", "matches")
 *     }
 *     verification(VerificationType.TEXT_CONTAINS) {
 *         displayName("Text Contains")
 *         keywords("contains", "includes")
 *     }
 * }
 * ```
 */
@DslMarker
annotation class VerificationDsl

/**
 * Builder for a single verification definition.
 */
@VerificationDsl
class VerificationDefinitionBuilder(private val type: VerificationType) {
    private var displayName: String = type.name.replace("_", " ").lowercase()
        .replaceFirstChar { it.uppercase() }
    private var description: String = ""
    private val nlpKeywords = mutableSetOf<String>()
    private val nlpPatterns = mutableListOf<String>()
    private val supportedOperators = mutableSetOf<ComparisonOperator>()
    private var requiresTarget: Boolean = true
    private var requiresExpectedValue: Boolean = false
    private var supportsSoftAssert: Boolean = true
    private var supportsRetry: Boolean = true
    private val examples = mutableListOf<String>()
    private val metadata = mutableMapOf<String, Any>()

    /**
     * Set the display name.
     */
    fun displayName(name: String) {
        displayName = name
    }

    /**
     * Set the description.
     */
    fun description(desc: String) {
        description = desc
    }

    /**
     * Add NLP keywords that trigger this verification.
     */
    fun keywords(vararg words: String) {
        nlpKeywords.addAll(words.map { it.lowercase() })
    }

    /**
     * Add keywords from a set.
     */
    fun keywords(words: Set<String>) {
        nlpKeywords.addAll(words.map { it.lowercase() })
    }

    /**
     * Add NLP patterns for matching.
     */
    fun patterns(vararg patternList: String) {
        nlpPatterns.addAll(patternList)
    }

    /**
     * Set supported comparison operators.
     */
    fun operators(vararg ops: ComparisonOperator) {
        supportedOperators.addAll(ops)
    }

    /**
     * Mark that this verification requires a target element.
     */
    fun requiresTarget(required: Boolean = true) {
        requiresTarget = required
    }

    /**
     * Mark that this verification requires an expected value.
     */
    fun requiresExpectedValue(required: Boolean = true) {
        requiresExpectedValue = required
    }

    /**
     * Mark that this verification does not require a target.
     */
    fun noTarget() {
        requiresTarget = false
    }

    /**
     * Mark that this verification supports soft assertions.
     */
    fun supportsSoftAssert(supported: Boolean = true) {
        supportsSoftAssert = supported
    }

    /**
     * Mark that this verification supports retry.
     */
    fun supportsRetry(supported: Boolean = true) {
        supportsRetry = supported
    }

    /**
     * Add usage examples.
     */
    fun examples(vararg exampleList: String) {
        examples.addAll(exampleList)
    }

    /**
     * Add metadata.
     */
    fun metadata(key: String, value: Any) {
        metadata[key] = value
    }

    /**
     * Build the VerificationDefinition.
     */
    fun build(): VerificationDefinition = VerificationDefinition(
        type = type,
        displayName = displayName,
        description = description,
        nlpKeywords = nlpKeywords.toSet(),
        nlpPatterns = nlpPatterns.toList(),
        supportedOperators = if (supportedOperators.isEmpty()) setOf(ComparisonOperator.EQUALS) else supportedOperators.toSet(),
        requiresTarget = requiresTarget,
        requiresExpectedValue = requiresExpectedValue,
        supportsSoftAssert = supportsSoftAssert,
        supportsRetry = supportsRetry,
        examples = examples.toList(),
        metadata = metadata.toMap()
    )
}

/**
 * Builder for registering multiple verifications in a category.
 */
@VerificationDsl
class VerificationCategoryBuilder(
    private val registry: VerificationRegistry,
    private val category: VerificationCategory
) {
    /**
     * Define a verification.
     */
    fun verification(type: VerificationType, block: VerificationDefinitionBuilder.() -> Unit) {
        val builder = VerificationDefinitionBuilder(type)
        builder.block()
        registry.register(category, builder.build())
    }

    /**
     * Add an existing definition.
     */
    fun add(definition: VerificationDefinition) {
        registry.register(category, definition)
    }
}

/**
 * DSL entry point for creating a single verification definition.
 */
fun verification(type: VerificationType, block: VerificationDefinitionBuilder.() -> Unit): VerificationDefinition {
    val builder = VerificationDefinitionBuilder(type)
    builder.block()
    return builder.build()
}

/**
 * DSL entry point for creating multiple verification definitions.
 */
fun verifications(block: VerificationsBuilder.() -> Unit): List<VerificationDefinition> {
    val builder = VerificationsBuilder()
    builder.block()
    return builder.build()
}

/**
 * Builder for creating a list of verification definitions.
 */
@VerificationDsl
class VerificationsBuilder {
    private val definitions = mutableListOf<VerificationDefinition>()

    fun verification(type: VerificationType, block: VerificationDefinitionBuilder.() -> Unit) {
        val builder = VerificationDefinitionBuilder(type)
        builder.block()
        definitions.add(builder.build())
    }

    fun build(): List<VerificationDefinition> = definitions.toList()
}
