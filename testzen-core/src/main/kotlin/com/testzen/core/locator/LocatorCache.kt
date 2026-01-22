package com.testzen.core.locator

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Simple flat cache for storing and retrieving smart locators.
 *
 * Persists successful locator strategies to disk for faster subsequent runs
 * and to support self-healing across test sessions.
 *
 * For larger projects with many pages, consider using PageObjectRepository instead.
 *
 * Single Responsibility: Flat file-based locator persistence.
 */
class LocatorCache(
    private val cacheDirectory: String = ".testzen-cache",
    private val maxEntriesPerElement: Int = 10
) : LocatorStorage {
    private val logger = LoggerFactory.getLogger(LocatorCache::class.java)
    private val cache = ConcurrentHashMap<String, SmartLocator>()
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        loadFromDisk()
    }

    // ═══════════════════════════════════════════════════════════════
    // LOCATOR STORAGE INTERFACE IMPLEMENTATION
    // ═══════════════════════════════════════════════════════════════

    override fun get(elementName: String, pageName: String?): SmartLocator? {
        // Simple cache ignores pageName (flat structure)
        return cache[normalizeKey(elementName)]
    }

    override fun put(smartLocator: SmartLocator, pageName: String?) {
        val key = normalizeKey(smartLocator.elementName)
        cache[key] = smartLocator
        saveToDisk()
    }

    override fun recordSuccess(elementName: String, successIndex: Int, pageName: String?) {
        val key = normalizeKey(elementName)
        cache[key]?.let { existing ->
            cache[key] = existing.withSuccessAt(successIndex)
            saveToDisk()
            logger.debug("Recorded success for '{}' at index {}", elementName, successIndex)
        }
    }

    override fun recordFailure(elementName: String, pageName: String?) {
        val key = normalizeKey(elementName)
        cache[key]?.let { existing ->
            cache[key] = existing.withFailure()
            saveToDisk()
            logger.debug("Recorded failure for '{}', count: {}", elementName, existing.failureCount + 1)
        }
    }

    override fun addLearnedLocators(elementName: String, locators: List<Locator>, pageName: String?) {
        if (locators.isEmpty()) return

        val key = normalizeKey(elementName)
        val existing = cache[key]

        if (existing != null) {
            // Filter out duplicate locators
            val existingValues = existing.locators.map { "${it.type}:${it.value}" }.toSet()
            val newLocators = locators.filter { "${it.type}:${it.value}" !in existingValues }

            if (newLocators.isNotEmpty()) {
                val updatedLocators = (newLocators + existing.locators)
                    .sortedByDescending { it.confidence }
                    .take(maxEntriesPerElement)

                cache[key] = existing.copy(
                    locators = updatedLocators,
                    lastSuccessfulIndex = 0 // New learned locator is primary
                )
                saveToDisk()
                logger.debug("Added {} learned locator(s) to '{}'", newLocators.size, elementName)
            }
        } else {
            // Create new entry with learned locators
            cache[key] = SmartLocator(
                elementName = elementName,
                locators = locators.sortedByDescending { it.confidence }.take(maxEntriesPerElement),
                lastSuccessfulIndex = 0
            )
            saveToDisk()
        }
    }

    override fun save() {
        saveToDisk()
    }

    override fun getStats(): StorageStats {
        var totalLocators = 0
        var healedCount = 0

        cache.values.forEach { smartLocator ->
            totalLocators += smartLocator.locators.size
            if (smartLocator.lastSuccessfulIndex > 0) {
                healedCount++
            }
        }

        return StorageStats(
            elementCount = cache.size,
            totalLocators = totalLocators,
            healedElements = healedCount,
            storageType = StorageType.SIMPLE_CACHE
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // LEGACY API (for backwards compatibility)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Add a single locator to an element's strategies.
     * @deprecated Use addLearnedLocators instead
     */
    fun addLocator(elementName: String, locator: Locator) {
        addLearnedLocators(elementName, listOf(locator))
    }

    /**
     * Get legacy CacheStats format.
     */
    fun getLegacyStats(): CacheStats {
        var totalLocators = 0
        var totalFailures = 0
        var healedCount = 0

        cache.values.forEach { smartLocator ->
            totalLocators += smartLocator.locators.size
            totalFailures += smartLocator.failureCount
            if (smartLocator.lastSuccessfulIndex > 0) {
                healedCount++
            }
        }

        return CacheStats(
            elementCount = cache.size,
            totalLocators = totalLocators,
            totalFailures = totalFailures,
            healedElements = healedCount
        )
    }

    /**
     * Clear all cached locators.
     */
    fun clear() {
        cache.clear()
        val cacheFile = getCacheFile()
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
    }

    /**
     * Get all cached element names.
     */
    fun getAllElementNames(): Set<String> = cache.keys.toSet()

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════

    private fun normalizeKey(elementName: String): String {
        return elementName.lowercase().trim()
    }

    private fun getCacheFile(): File {
        val dir = File(cacheDirectory)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "locator-cache.json")
    }

    private fun loadFromDisk() {
        try {
            val cacheFile = getCacheFile()
            if (cacheFile.exists()) {
                val content = cacheFile.readText()
                val loaded = json.decodeFromString<Map<String, SmartLocator>>(content)
                cache.putAll(loaded)
                logger.info("Loaded {} cached locators from disk", cache.size)
            }
        } catch (e: Exception) {
            logger.warn("Failed to load locator cache: ${e.message}")
        }
    }

    private fun saveToDisk() {
        try {
            val cacheFile = getCacheFile()
            val content = json.encodeToString(cache.toMap())
            cacheFile.writeText(content)
        } catch (e: Exception) {
            logger.warn("Failed to save locator cache: ${e.message}")
        }
    }
}

/**
 * Legacy statistics format for backwards compatibility.
 */
data class CacheStats(
    val elementCount: Int,
    val totalLocators: Int,
    val totalFailures: Int,
    val healedElements: Int
)
