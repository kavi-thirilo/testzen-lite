package com.testzen.core.locator

/**
 * Adapter that wraps PageObjectRepository to implement LocatorStorage interface.
 *
 * This allows the SelfHealingLocator to work with either simple cache
 * or page-based repository through a common interface.
 *
 * Single Responsibility: Adapt PageObjectRepository to LocatorStorage contract.
 */
class PageObjectRepositoryAdapter(
    private val repository: PageObjectRepository,
    private val defaultPageName: String = "default"
) : LocatorStorage {

    override fun get(elementName: String, pageName: String?): SmartLocator? {
        val page = pageName ?: defaultPageName
        return repository.findElement(page, elementName)
    }

    override fun put(smartLocator: SmartLocator, pageName: String?) {
        val page = pageName ?: defaultPageName
        repository.getPage(page).addElement(smartLocator)
    }

    override fun recordSuccess(elementName: String, successIndex: Int, pageName: String?) {
        val page = pageName ?: defaultPageName
        repository.recordSuccess(page, elementName, successIndex)
    }

    override fun recordFailure(elementName: String, pageName: String?) {
        val page = pageName ?: defaultPageName
        repository.recordFailure(page, elementName)
    }

    override fun addLearnedLocators(elementName: String, locators: List<Locator>, pageName: String?) {
        if (locators.isEmpty()) return

        val page = pageName ?: defaultPageName
        val pageObject = repository.getPage(page)
        val existing = pageObject.getElement(elementName)

        if (existing != null) {
            // Add new locators to existing element (avoid duplicates)
            val existingValues = existing.locators.map { "${it.type}:${it.value}" }.toSet()
            val newLocators = locators.filter { "${it.type}:${it.value}" !in existingValues }

            if (newLocators.isNotEmpty()) {
                val updatedLocators = (newLocators + existing.locators)
                    .sortedByDescending { it.confidence }
                    .take(10) // Keep max 10 locators per element

                pageObject.addElement(SmartLocator(
                    elementName = elementName,
                    locators = updatedLocators,
                    lastSuccessfulIndex = 0, // New learned locator is now primary
                    failureCount = 0
                ))
            }
        } else {
            // Create new element with learned locators
            pageObject.addElement(SmartLocator(
                elementName = elementName,
                locators = locators.sortedByDescending { it.confidence },
                lastSuccessfulIndex = 0,
                failureCount = 0
            ))
        }
    }

    override fun save() {
        repository.save()
    }

    override fun getStats(): StorageStats {
        val repoStats = repository.getStats()
        return StorageStats(
            elementCount = repoStats.totalElements,
            totalLocators = repoStats.totalLocators,
            healedElements = repoStats.healedElements,
            storageType = StorageType.PAGE_REPOSITORY
        )
    }

    /**
     * Get the underlying repository for advanced operations.
     */
    fun getRepository(): PageObjectRepository = repository
}
