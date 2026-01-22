package com.testzen.core.locator

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Page Object Repository - Scalable page-based locator storage.
 *
 * Organizes locators by page/screen for large projects:
 * ```
 * page-objects/
 * ├── _index.json           # Page registry and metadata
 * ├── login_page.json       # Login page elements
 * ├── home_page.json        # Home page elements
 * ├── checkout/
 * │   ├── cart_page.json
 * │   └── payment_page.json
 * └── settings/
 *     └── profile_page.json
 * ```
 *
 * Usage:
 * ```kotlin
 * val repo = PageObjectRepository("./page-objects")
 *
 * // Load or create a page
 * val loginPage = repo.getPage("login_page")
 *
 * // Add element to page
 * loginPage.addElement("email_field", listOf(
 *     Locator(LocatorType.ACCESSIBILITY_ID, "email_input"),
 *     Locator(LocatorType.RESOURCE_ID, "com.app:id/email")
 * ))
 *
 * // Get element locators
 * val locators = loginPage.getElement("email_field")
 *
 * // Save changes
 * repo.save()
 * ```
 */
class PageObjectRepository(
    private val repositoryPath: String = "./page-objects",
    private val autoSave: Boolean = false,
    private val cacheMode: CacheMode = CacheMode.READ_WRITE
) {
    private val logger = LoggerFactory.getLogger(PageObjectRepository::class.java)
    private val pages = ConcurrentHashMap<String, PageObject>()
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private var isDirty = false

    init {
        if (cacheMode != CacheMode.DISABLED) {
            loadIndex()
        }
    }

    /**
     * Get or create a page object.
     */
    fun getPage(pageName: String): PageObject {
        if (cacheMode == CacheMode.DISABLED) {
            return PageObject(pageName)
        }

        return pages.getOrPut(normalizePageName(pageName)) {
            loadPage(pageName) ?: PageObject(pageName)
        }
    }

    /**
     * Check if a page exists.
     */
    fun hasPage(pageName: String): Boolean {
        return pages.containsKey(normalizePageName(pageName)) ||
               getPageFile(pageName).exists()
    }

    /**
     * Get all page names.
     */
    fun getPageNames(): Set<String> = pages.keys.toSet()

    /**
     * Find element across all pages.
     */
    fun findElement(elementName: String): PageElementResult? {
        for ((pageName, page) in pages) {
            page.getElement(elementName)?.let { smartLocator ->
                return PageElementResult(pageName, smartLocator)
            }
        }
        return null
    }

    /**
     * Find element in specific page, with fallback to global search.
     */
    fun findElement(pageName: String?, elementName: String): SmartLocator? {
        // Try specific page first
        if (pageName != null) {
            getPage(pageName).getElement(elementName)?.let { return it }
        }

        // Fallback to global search
        return findElement(elementName)?.smartLocator
    }

    /**
     * Record successful locator for learning.
     */
    fun recordSuccess(pageName: String, elementName: String, successIndex: Int) {
        if (cacheMode == CacheMode.READ_ONLY || cacheMode == CacheMode.DISABLED) return

        getPage(pageName).recordSuccess(elementName, successIndex)
        isDirty = true
        if (autoSave) savePage(pageName)
    }

    /**
     * Record failure for an element.
     */
    fun recordFailure(pageName: String, elementName: String) {
        if (cacheMode == CacheMode.READ_ONLY || cacheMode == CacheMode.DISABLED) return

        getPage(pageName).recordFailure(elementName)
        isDirty = true
        if (autoSave) savePage(pageName)
    }

    /**
     * Save all modified pages to disk.
     */
    fun save() {
        if (cacheMode == CacheMode.READ_ONLY || cacheMode == CacheMode.DISABLED) return
        if (!isDirty) return

        ensureDirectoryExists()
        saveIndex()

        for ((pageName, page) in pages) {
            if (page.isDirty) {
                savePage(pageName)
            }
        }

        isDirty = false
        logger.info("Saved {} page(s) to repository", pages.size)
    }

    /**
     * Save a specific page.
     */
    fun savePage(pageName: String) {
        if (cacheMode == CacheMode.READ_ONLY || cacheMode == CacheMode.DISABLED) return

        val page = pages[normalizePageName(pageName)] ?: return
        val file = getPageFile(pageName)

        try {
            file.parentFile?.mkdirs()
            val content = json.encodeToString(page.toSerializable())
            file.writeText(content)
            page.markClean()
            logger.debug("Saved page: {}", pageName)
        } catch (e: Exception) {
            logger.error("Failed to save page {}: {}", pageName, e.message)
        }
    }

    /**
     * Import page objects from another repository or exported file.
     */
    fun import(sourcePath: String, overwrite: Boolean = false) {
        val sourceDir = File(sourcePath)
        if (!sourceDir.exists()) {
            throw IllegalArgumentException("Source path does not exist: $sourcePath")
        }

        if (sourceDir.isFile && sourcePath.endsWith(".json")) {
            // Import single page file
            importPageFile(sourceDir, overwrite)
        } else {
            // Import directory
            sourceDir.walkTopDown()
                .filter { it.isFile && it.extension == "json" && it.name != "_index.json" }
                .forEach { importPageFile(it, overwrite) }
        }

        save()
    }

    /**
     * Export page objects for sharing.
     */
    fun export(targetPath: String, pageNames: List<String>? = null) {
        val targetDir = File(targetPath)
        targetDir.mkdirs()

        val pagesToExport = pageNames?.map { normalizePageName(it) }?.toSet()
            ?: pages.keys

        for (pageName in pagesToExport) {
            val page = pages[pageName] ?: continue
            val targetFile = File(targetDir, "${pageName}.json")
            val content = json.encodeToString(page.toSerializable())
            targetFile.writeText(content)
        }

        logger.info("Exported {} page(s) to {}", pagesToExport.size, targetPath)
    }

    /**
     * Clear all cached pages (respects cache mode).
     */
    fun clear() {
        pages.clear()
        isDirty = false
    }

    /**
     * Get repository statistics.
     */
    fun getStats(): RepositoryStats {
        var totalElements = 0
        var totalLocators = 0
        var healedElements = 0

        pages.values.forEach { page ->
            totalElements += page.elementCount
            page.elements.values.forEach { smartLocator ->
                totalLocators += smartLocator.locators.size
                if (smartLocator.lastSuccessfulIndex > 0) {
                    healedElements++
                }
            }
        }

        return RepositoryStats(
            pageCount = pages.size,
            totalElements = totalElements,
            totalLocators = totalLocators,
            healedElements = healedElements,
            cacheMode = cacheMode
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE METHODS
    // ═══════════════════════════════════════════════════════════════

    private fun normalizePageName(name: String): String {
        return name.lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_/]"), "")
    }

    private fun ensureDirectoryExists() {
        val dir = File(repositoryPath)
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    private fun getPageFile(pageName: String): File {
        val normalized = normalizePageName(pageName)
        return File(repositoryPath, "$normalized.json")
    }

    private fun loadIndex() {
        val indexFile = File(repositoryPath, "_index.json")
        if (!indexFile.exists()) return

        try {
            val content = indexFile.readText()
            val index = json.decodeFromString<RepositoryIndex>(content)

            // Load each page
            for (pageName in index.pages) {
                loadPage(pageName)
            }

            logger.info("Loaded {} page(s) from repository", pages.size)
        } catch (e: Exception) {
            logger.warn("Failed to load repository index: {}", e.message)
        }
    }

    private fun saveIndex() {
        val indexFile = File(repositoryPath, "_index.json")
        val index = RepositoryIndex(
            pages = pages.keys.toList(),
            version = "1.0",
            lastModified = System.currentTimeMillis()
        )

        try {
            val content = json.encodeToString(index)
            indexFile.writeText(content)
        } catch (e: Exception) {
            logger.error("Failed to save repository index: {}", e.message)
        }
    }

    private fun loadPage(pageName: String): PageObject? {
        val file = getPageFile(pageName)
        if (!file.exists()) return null

        return try {
            val content = file.readText()
            val data = json.decodeFromString<SerializablePageObject>(content)
            val page = PageObject.fromSerializable(data)
            pages[normalizePageName(pageName)] = page
            page
        } catch (e: Exception) {
            logger.warn("Failed to load page {}: {}", pageName, e.message)
            null
        }
    }

    private fun importPageFile(file: File, overwrite: Boolean) {
        try {
            val content = file.readText()
            val data = json.decodeFromString<SerializablePageObject>(content)
            val pageName = normalizePageName(data.name)

            if (!overwrite && pages.containsKey(pageName)) {
                logger.debug("Skipping existing page: {}", pageName)
                return
            }

            pages[pageName] = PageObject.fromSerializable(data)
            isDirty = true
            logger.debug("Imported page: {}", pageName)
        } catch (e: Exception) {
            logger.warn("Failed to import {}: {}", file.name, e.message)
        }
    }
}

/**
 * Cache mode for the repository.
 */
enum class CacheMode {
    /** Read and write to cache (default) */
    READ_WRITE,

    /** Read from cache but don't write updates */
    READ_ONLY,

    /** Disable cache entirely - generate locators fresh each time */
    DISABLED
}

/**
 * Represents a page/screen with its elements.
 */
class PageObject(
    val name: String,
    internal val elements: ConcurrentHashMap<String, SmartLocator> = ConcurrentHashMap()
) {
    internal var isDirty = false

    val elementCount: Int get() = elements.size

    /**
     * Get element locators by name.
     */
    fun getElement(elementName: String): SmartLocator? {
        return elements[normalizeElementName(elementName)]
    }

    /**
     * Add or update an element.
     */
    fun addElement(elementName: String, locators: List<Locator>) {
        val key = normalizeElementName(elementName)
        elements[key] = SmartLocator(
            elementName = elementName,
            locators = locators,
            lastSuccessfulIndex = 0,
            failureCount = 0
        )
        isDirty = true
    }

    /**
     * Add element with SmartLocator.
     */
    fun addElement(smartLocator: SmartLocator) {
        val key = normalizeElementName(smartLocator.elementName)
        elements[key] = smartLocator
        isDirty = true
    }

    /**
     * Remove an element.
     */
    fun removeElement(elementName: String) {
        elements.remove(normalizeElementName(elementName))
        isDirty = true
    }

    /**
     * Record successful locator.
     */
    fun recordSuccess(elementName: String, successIndex: Int) {
        val key = normalizeElementName(elementName)
        elements[key]?.let { existing ->
            elements[key] = existing.withSuccessAt(successIndex)
            isDirty = true
        }
    }

    /**
     * Record failure.
     */
    fun recordFailure(elementName: String) {
        val key = normalizeElementName(elementName)
        elements[key]?.let { existing ->
            elements[key] = existing.withFailure()
            isDirty = true
        }
    }

    /**
     * Get all element names.
     */
    fun getElementNames(): Set<String> = elements.keys.toSet()

    internal fun markClean() {
        isDirty = false
    }

    internal fun toSerializable(): SerializablePageObject {
        return SerializablePageObject(
            name = name,
            elements = elements.toMap(),
            version = "1.0",
            lastModified = System.currentTimeMillis()
        )
    }

    private fun normalizeElementName(name: String): String {
        return name.lowercase().trim()
    }

    companion object {
        fun fromSerializable(data: SerializablePageObject): PageObject {
            val page = PageObject(data.name)
            page.elements.putAll(data.elements)
            return page
        }
    }
}

/**
 * Result when finding an element across pages.
 */
data class PageElementResult(
    val pageName: String,
    val smartLocator: SmartLocator
)

/**
 * Repository statistics.
 */
data class RepositoryStats(
    val pageCount: Int,
    val totalElements: Int,
    val totalLocators: Int,
    val healedElements: Int,
    val cacheMode: CacheMode
)

// ═══════════════════════════════════════════════════════════════
// SERIALIZATION MODELS
// ═══════════════════════════════════════════════════════════════

@Serializable
internal data class RepositoryIndex(
    val pages: List<String>,
    val version: String,
    val lastModified: Long
)

@Serializable
internal data class SerializablePageObject(
    val name: String,
    val elements: Map<String, SmartLocator>,
    val version: String,
    val lastModified: Long
)
