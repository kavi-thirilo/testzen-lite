package com.testzen.core.locator

/**
 * Interface for locator storage backends.
 *
 * Provides abstraction over different storage mechanisms:
 * - Simple flat cache (LocatorCache)
 * - Page-based repository (PageObjectRepository)
 *
 * Single Responsibility: Define contract for locator persistence.
 */
interface LocatorStorage {
    /**
     * Get a SmartLocator by element name.
     *
     * @param elementName The element identifier
     * @param pageName Optional page context (for page-based storage)
     * @return The SmartLocator if found, null otherwise
     */
    fun get(elementName: String, pageName: String? = null): SmartLocator?

    /**
     * Store or update a SmartLocator.
     *
     * @param smartLocator The locator to store
     * @param pageName Optional page context (for page-based storage)
     */
    fun put(smartLocator: SmartLocator, pageName: String? = null)

    /**
     * Record a successful locator usage at a specific index.
     *
     * @param elementName The element identifier
     * @param successIndex The index of the successful locator
     * @param pageName Optional page context
     */
    fun recordSuccess(elementName: String, successIndex: Int, pageName: String? = null)

    /**
     * Record a locator failure.
     *
     * @param elementName The element identifier
     * @param pageName Optional page context
     */
    fun recordFailure(elementName: String, pageName: String? = null)

    /**
     * Add a learned locator to an existing element's strategies.
     *
     * @param elementName The element identifier
     * @param locators The learned locators to add
     * @param pageName Optional page context
     */
    fun addLearnedLocators(elementName: String, locators: List<Locator>, pageName: String? = null)

    /**
     * Persist any pending changes to disk.
     */
    fun save()

    /**
     * Get statistics about the storage.
     */
    fun getStats(): StorageStats
}

/**
 * Statistics about locator storage.
 */
data class StorageStats(
    val elementCount: Int,
    val totalLocators: Int,
    val healedElements: Int,
    val storageType: StorageType
)

/**
 * Type of storage backend.
 */
enum class StorageType {
    SIMPLE_CACHE,
    PAGE_REPOSITORY
}
