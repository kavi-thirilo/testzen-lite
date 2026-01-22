package com.testzen.core.nlp.synonyms

/**
 * DSL builders for creating synonym sets in a clean, readable way.
 *
 * Usage:
 * ```kotlin
 * // Single synonym set
 * val clickWords = synonymSet("CLICK") {
 *     words("click", "tap", "press", "touch")
 *     description("Click/tap action words")
 *     aliases("TAP", "PRESS")
 * }
 *
 * // Multiple synonym sets in a category
 * registry.register(SynonymCategory.ACTION_VERBS) {
 *     synonymSet("CLICK") {
 *         words("click", "tap", "press")
 *     }
 *     synonymSet("ENTER_TEXT") {
 *         words("enter", "type", "input")
 *     }
 * }
 * ```
 */
@DslMarker
annotation class SynonymDsl

/**
 * Builder for a single synonym set.
 */
@SynonymDsl
class SynonymSetBuilder(private val name: String) {
    private val words = mutableSetOf<String>()
    private var description: String = ""
    private val aliases = mutableSetOf<String>()

    /**
     * Add words to the synonym set.
     */
    fun words(vararg newWords: String) {
        words.addAll(newWords.map { it.lowercase() })
    }

    /**
     * Add words from an existing set.
     */
    fun words(wordSet: Set<String>) {
        words.addAll(wordSet.map { it.lowercase() })
    }

    /**
     * Add words from a collection.
     */
    fun words(wordList: Collection<String>) {
        words.addAll(wordList.map { it.lowercase() })
    }

    /**
     * Include all words from another synonym set.
     */
    fun include(other: SynonymSet) {
        words.addAll(other.words)
    }

    /**
     * Set the description.
     */
    fun description(desc: String) {
        description = desc
    }

    /**
     * Add aliases for this synonym set.
     */
    fun aliases(vararg names: String) {
        aliases.addAll(names)
    }

    /**
     * Build the SynonymSet.
     */
    fun build(): SynonymSet = SynonymSet(
        name = name,
        words = words.toSet(),
        description = description,
        aliases = aliases.toSet()
    )
}

/**
 * Builder for registering multiple synonym sets in a category.
 */
@SynonymDsl
class SynonymCategoryBuilder(
    private val registry: SynonymRegistry,
    private val category: SynonymCategory
) {
    /**
     * Define a synonym set.
     */
    fun synonymSet(name: String, block: SynonymSetBuilder.() -> Unit) {
        val builder = SynonymSetBuilder(name)
        builder.block()
        registry.register(category, name, builder.build())
    }

    /**
     * Add an existing synonym set.
     */
    fun add(synonymSet: SynonymSet) {
        registry.register(category, synonymSet.name, synonymSet)
    }
}

/**
 * DSL entry point for creating a single synonym set.
 */
fun synonymSet(name: String, block: SynonymSetBuilder.() -> Unit): SynonymSet {
    val builder = SynonymSetBuilder(name)
    builder.block()
    return builder.build()
}

/**
 * DSL entry point for creating multiple synonym sets.
 */
fun synonymSets(block: SynonymSetsBuilder.() -> Unit): List<SynonymSet> {
    val builder = SynonymSetsBuilder()
    builder.block()
    return builder.build()
}

/**
 * Builder for creating a list of synonym sets.
 */
@SynonymDsl
class SynonymSetsBuilder {
    private val sets = mutableListOf<SynonymSet>()

    fun synonymSet(name: String, block: SynonymSetBuilder.() -> Unit) {
        val builder = SynonymSetBuilder(name)
        builder.block()
        sets.add(builder.build())
    }

    fun build(): List<SynonymSet> = sets.toList()
}
