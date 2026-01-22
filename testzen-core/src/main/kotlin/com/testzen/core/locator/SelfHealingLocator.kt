package com.testzen.core.locator

import com.testzen.core.model.Platform
import org.openqa.selenium.By
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.slf4j.LoggerFactory

/**
 * Self-healing element locator that automatically tries fallback strategies
 * when the primary locator fails.
 *
 * Features:
 * - Multiple locator strategies per element
 * - Automatic fallback when primary fails
 * - Learning from successful locators
 * - Persistent storage across test sessions
 * - Detailed healing reports
 *
 * Single Responsibility: Orchestrate self-healing element location.
 *
 * Usage with simple cache:
 * ```kotlin
 * val healer = SelfHealingLocator(driver, Platform.ANDROID)
 * val element = healer.findElement("Login Button")
 * ```
 *
 * Usage with Page Object Repository:
 * ```kotlin
 * val repo = PageObjectRepository("./page-objects")
 * val storage = PageObjectRepositoryAdapter(repo)
 * val healer = SelfHealingLocator(driver, Platform.ANDROID, storage = storage)
 * val element = healer.findElement("email_field", pageName = "login_page")
 * ```
 */
class SelfHealingLocator(
    private val driver: WebDriver,
    private val platform: Platform,
    private val storage: LocatorStorage? = null,
    private val cache: LocatorCache? = null,
    private val repository: PageObjectRepository? = null,
    private val config: SelfHealingConfig = SelfHealingConfig()
) {
    private val logger = LoggerFactory.getLogger(SelfHealingLocator::class.java)
    private val healingReport = mutableListOf<HealingEvent>()

    // Extracted components for single responsibility
    private val locatorGenerator = LocatorGenerator.forPlatform(platform)
    private val attributeExtractor = ElementAttributeExtractor.forPlatform(platform)

    // Resolve the actual storage to use
    private val resolvedStorage: LocatorStorage by lazy {
        storage
            ?: repository?.let { PageObjectRepositoryAdapter(it) }
            ?: cache
            ?: LocatorCache()
    }

    // Track current page context for repository mode
    private var currentPageName: String? = null

    /**
     * Set the current page context for page object repository mode.
     */
    fun setCurrentPage(pageName: String) {
        currentPageName = pageName
    }

    /**
     * Get the current page context.
     */
    fun getCurrentPage(): String? = currentPageName

    /**
     * Find an element by name, using cached locators and fallback strategies.
     *
     * @param elementName The human-readable element name
     * @param locatorHints Optional explicit locators to try
     * @param pageName Optional page name for page object repository mode
     * @return The found WebElement
     * @throws NoSuchElementException if element cannot be found with any strategy
     */
    fun findElement(
        elementName: String,
        locatorHints: List<Locator> = emptyList(),
        pageName: String? = currentPageName
    ): WebElement {
        // Get cached smart locator or create from hints
        val smartLocator = getSmartLocator(elementName, pageName)
            ?: createSmartLocator(elementName, locatorHints, pageName)

        // Try to find element with self-healing
        val result = tryLocators(smartLocator)

        return when (result) {
            is LocatorResult.Found -> handleFoundElement(result, smartLocator, elementName, pageName)
            is LocatorResult.NotFound -> handleNotFoundElement(result, smartLocator, elementName, pageName)
        }
    }

    /**
     * Find element without throwing exception.
     */
    fun findElementOrNull(elementName: String, locatorHints: List<Locator> = emptyList()): WebElement? {
        return try {
            findElement(elementName, locatorHints)
        } catch (e: NoSuchElementException) {
            null
        }
    }

    /**
     * Check if an element exists.
     */
    fun elementExists(elementName: String, locatorHints: List<Locator> = emptyList()): Boolean {
        return findElementOrNull(elementName, locatorHints) != null
    }

    /**
     * Generate locators for an element based on its name and platform.
     * Delegates to LocatorGenerator.
     */
    fun generateLocators(elementName: String): List<Locator> {
        return locatorGenerator.generate(elementName)
    }

    /**
     * Get the healing report for this session.
     */
    fun getHealingReport(): List<HealingEvent> = healingReport.toList()

    /**
     * Clear the healing report.
     */
    fun clearHealingReport() {
        healingReport.clear()
    }

    /**
     * Get cache statistics.
     */
    fun getCacheStats(): CacheStats {
        val stats = resolvedStorage.getStats()
        return CacheStats(
            elementCount = stats.elementCount,
            totalLocators = stats.totalLocators,
            totalFailures = 0,
            healedElements = stats.healedElements
        )
    }

    /**
     * Get repository statistics (if using page object repository).
     */
    fun getRepositoryStats(): RepositoryStats? {
        return when (val s = resolvedStorage) {
            is PageObjectRepositoryAdapter -> s.getRepository().getStats()
            else -> repository?.getStats()
        }
    }

    /**
     * Save the storage to disk.
     */
    fun saveRepository() {
        resolvedStorage.save()
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    private fun handleFoundElement(
        result: LocatorResult.Found,
        smartLocator: SmartLocator,
        elementName: String,
        pageName: String?
    ): WebElement {
        if (result.wasHealed) {
            logger.info("Self-healed element '{}' using locator at index {}", elementName, result.locatorIndex)

            // Learn from healed element
            if (config.learnFromHealing) {
                learnFromHealedElement(result.locator, elementName, pageName)
            }

            healingReport.add(HealingEvent(
                elementName = elementName,
                pageName = pageName,
                originalLocator = smartLocator.primaryLocator(),
                healedLocator = result.locator,
                success = true
            ))
        }

        resolvedStorage.recordSuccess(elementName, result.locatorIndex, pageName)
        return findWithLocator(result.locator)
    }

    private fun handleNotFoundElement(
        result: LocatorResult.NotFound,
        smartLocator: SmartLocator,
        elementName: String,
        pageName: String?
    ): Nothing {
        resolvedStorage.recordFailure(elementName, pageName)
        healingReport.add(HealingEvent(
            elementName = elementName,
            pageName = pageName,
            originalLocator = smartLocator.primaryLocator(),
            healedLocator = null,
            success = false,
            errors = result.errors
        ))
        throw NoSuchElementException(
            "Cannot find element '$elementName' after trying ${result.attemptedLocators.size} strategies. " +
            "Errors: ${result.errors.joinToString("; ")}"
        )
    }

    private fun learnFromHealedElement(successfulLocator: Locator, elementName: String, pageName: String?) {
        try {
            val element = findWithLocator(successfulLocator)
            val learnedLocators = attributeExtractor.extract(element)

            if (learnedLocators.isNotEmpty()) {
                resolvedStorage.addLearnedLocators(elementName, learnedLocators, pageName)
                logger.info("Learned {} new locator(s) from healed element '{}'", learnedLocators.size, elementName)
            }
        } catch (e: Exception) {
            logger.debug("Failed to learn from healed element: {}", e.message)
        }
    }

    private fun getSmartLocator(elementName: String, pageName: String?): SmartLocator? {
        return resolvedStorage.get(elementName, pageName)
    }

    private fun createSmartLocator(elementName: String, hints: List<Locator>, pageName: String?): SmartLocator {
        val locators = if (hints.isNotEmpty()) {
            hints + locatorGenerator.generate(elementName)
        } else {
            locatorGenerator.generate(elementName)
        }

        val smartLocator = SmartLocator(
            elementName = elementName,
            locators = locators.distinctBy { "${it.type}:${it.value}" }
        )

        // Cache for future use
        resolvedStorage.put(smartLocator, pageName)
        return smartLocator
    }

    private fun tryLocators(smartLocator: SmartLocator): LocatorResult {
        val errors = mutableListOf<String>()
        val attemptedLocators = mutableListOf<Locator>()

        // Try primary locator first
        val primary = smartLocator.primaryLocator()
        attemptedLocators.add(primary)

        try {
            findWithLocator(primary)
            return LocatorResult.Found(primary, smartLocator.lastSuccessfulIndex, wasHealed = false)
        } catch (e: Exception) {
            errors.add("${primary.type}(${primary.value}): ${e.message}")
            logger.debug("Primary locator failed for '{}': {}", smartLocator.elementName, e.message)
        }

        // Try fallback locators if enabled
        if (config.enableFallback) {
            for ((index, locator) in smartLocator.locators.withIndex()) {
                if (index == smartLocator.lastSuccessfulIndex) continue // Skip primary

                attemptedLocators.add(locator)
                try {
                    findWithLocator(locator)
                    return LocatorResult.Found(locator, index, wasHealed = true)
                } catch (e: Exception) {
                    errors.add("${locator.type}(${locator.value}): ${e.message}")
                    logger.debug("Fallback locator {} failed: {}", index, e.message)
                }

                // Stop if we've tried enough fallbacks
                if (attemptedLocators.size >= config.maxFallbackAttempts) {
                    break
                }
            }
        }

        return LocatorResult.NotFound(attemptedLocators, errors)
    }

    private fun findWithLocator(locator: Locator): WebElement {
        val by = when (locator.type) {
            LocatorType.ACCESSIBILITY_ID -> {
                By.xpath("//*[@content-desc='${locator.value}' or @accessibility-id='${locator.value}']")
            }
            LocatorType.RESOURCE_ID -> {
                if (locator.value.contains("*:id/")) {
                    By.xpath("//*[contains(@resource-id, '${locator.value.substringAfter("*:id/")}')]")
                } else {
                    By.id(locator.value)
                }
            }
            LocatorType.TEXT -> By.xpath("//*[@text='${locator.value}']")
            LocatorType.CONTENT_DESC -> By.xpath("//*[@content-desc='${locator.value}']")
            LocatorType.XPATH -> By.xpath(locator.value)
            LocatorType.CSS_SELECTOR -> By.cssSelector(locator.value)
            LocatorType.ID -> By.id(locator.value)
            LocatorType.NAME -> By.name(locator.value)
            LocatorType.CLASS_NAME -> By.className(locator.value)
            LocatorType.TAG_NAME -> By.tagName(locator.value)
            LocatorType.LINK_TEXT -> By.linkText(locator.value)
            LocatorType.PARTIAL_LINK_TEXT -> By.partialLinkText(locator.value)
        }

        return driver.findElement(by)
    }
}

/**
 * Configuration for self-healing behavior.
 */
data class SelfHealingConfig(
    val enableFallback: Boolean = true,
    val maxFallbackAttempts: Int = 5,
    val cacheEnabled: Boolean = true,
    val logHealingEvents: Boolean = true,
    /** When true, extract and learn new locators from healed elements */
    val learnFromHealing: Boolean = true
)

/**
 * Event recorded when self-healing is attempted.
 */
data class HealingEvent(
    val elementName: String,
    val pageName: String? = null,
    val originalLocator: Locator,
    val healedLocator: Locator?,
    val success: Boolean,
    val errors: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
