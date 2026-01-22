package com.testzen.core.locator.smart

import com.testzen.core.model.Platform
import com.testzen.core.stability.ElementStabilityWaiter
import com.testzen.core.stability.PageLoadIntelligence
import com.testzen.core.stability.RetryWithBackoff
import com.testzen.core.stability.SmartScrollStrategy
import com.testzen.core.stability.StabilityConfig
import org.openqa.selenium.By
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.slf4j.LoggerFactory

/**
 * Intelligent element finder that uses multiple strategies to locate elements.
 *
 * Features:
 * - Action-aware element finding
 * - Spatial relationship analysis for finding related elements
 * - Parent/child traversal when target isn't directly actionable
 * - Compound UI resolution (date pickers, multi-part forms)
 * - Cross-platform support (Android, iOS, Web)
 * - Scoring system for best match selection
 * - **Element stability verification** (waits for elements to stop moving)
 * - **Smart scrolling** with end-of-content detection
 * - **Page load intelligence** for network latency handling
 * - **Stale element recovery** with automatic re-finding
 *
 * Single Responsibility: Orchestrate intelligent element finding.
 *
 * Usage:
 * ```kotlin
 * val finder = SmartElementFinder(driver, Platform.ANDROID)
 * val result = finder.findElement("Login Button", ActionType.CLICK)
 * if (result.success) {
 *     result.element?.click()
 * }
 * ```
 */
class SmartElementFinder(
    private val driver: WebDriver,
    private val platform: Platform,
    private val config: SmartFinderConfig = SmartFinderConfig(),
    private val stabilityConfig: StabilityConfig = StabilityConfig.default()
) {
    private val logger = LoggerFactory.getLogger(SmartElementFinder::class.java)

    // Core components
    private val adapter = PlatformElementAdapter.forPlatform(platform)
    private val scorer = ElementScorer()
    private val spatialAnalyzer = SpatialAnalyzer(adapter)
    private val compoundResolver = CompoundElementResolver(spatialAnalyzer, scorer)

    // Stability components
    private val stabilityWaiter = ElementStabilityWaiter(stabilityConfig)
    private val scrollStrategy = SmartScrollStrategy(stabilityConfig)
    private val pageLoadIntelligence = PageLoadIntelligence(stabilityConfig)
    private val retryMechanism = RetryWithBackoff(stabilityConfig)

    /**
     * Find an element for the given target and action.
     *
     * @param target Element identifier (text, label, accessibility ID, etc.)
     * @param action The intended action type
     * @param context Optional context for spatial search
     * @return FindResult with the best matching element
     */
    fun findElement(
        target: String,
        action: ActionType,
        context: FindContext = FindContext()
    ): FindResult {
        logger.debug("Smart find: target='$target', action=$action")

        // Phase 0: Wait for page to be ready (handles network latency)
        if (stabilityConfig.pageLoadIntelligenceEnabled && context.waitForPageReady) {
            val pageResult = pageLoadIntelligence.waitForPageReady(driver, stabilityConfig.pageLoadTimeoutMs / 4)
            if (pageResult is PageLoadIntelligence.WaitResult.Timeout) {
                logger.debug("Page not fully ready, continuing: ${pageResult.reason}")
            }
        }

        // Phase 1: Direct search with multiple locator strategies
        val directCandidates = findDirectCandidatesWithRetry(target)
        if (directCandidates.isNotEmpty()) {
            val scored = scorer.rankCandidates(directCandidates, target, action)
            val compatible = scored.filter { it.supportsAction(action) }

            if (compatible.isNotEmpty()) {
                val best = compatible.first()
                if (best.overallScore >= config.minimumScore) {
                    logger.debug("Direct match found: score=${best.overallScore}")
                    // Wait for element stability before returning
                    val stableResult = waitForElementStability(best)
                    if (stableResult != null) {
                        return FindResult.success(stableResult)
                    }
                    return FindResult.success(best)
                }
            }

            // Phase 2: Try parent traversal if direct element isn't actionable
            if (action.allowParentTraversal) {
                val parentResult = tryParentTraversalWithStability(scored.firstOrNull(), action)
                if (parentResult != null) {
                    logger.debug("Parent traversal succeeded")
                    return FindResult.success(parentResult)
                }
            }

            // Phase 3: Try child traversal for input fields
            if (action.allowChildTraversal) {
                val childResult = tryChildTraversalWithStability(scored.firstOrNull(), action, target)
                if (childResult != null) {
                    logger.debug("Child traversal succeeded")
                    return FindResult.success(childResult)
                }
            }
        }

        // Phase 4: Spatial search - find by label association
        if (action.useSpatialSearch) {
            val spatialResult = trySpatialSearchWithStability(target, action, directCandidates)
            if (spatialResult != null) {
                logger.debug("Spatial search succeeded")
                return FindResult.success(spatialResult)
            }
        }

        // Phase 5: Compound element resolution
        if (compoundResolver.isCompoundScenario(target)) {
            val compoundActions = compoundResolver.resolveCompoundAction(
                target,
                getAllVisibleCandidates(),
                action
            )
            if (compoundActions.isNotEmpty()) {
                logger.debug("Compound resolution: ${compoundActions.size} actions")
                return FindResult.compound(compoundActions)
            }
        }

        // Phase 6: Smart scroll to find element (if enabled)
        if (context.scrollToFind && stabilityConfig.smartScrollEnabled) {
            val scrollResult = scrollToFindElement(target, action)
            if (scrollResult != null) {
                logger.debug("Element found after scrolling")
                return FindResult.success(scrollResult)
            }
        }

        // Phase 7: Fallback - best effort with relaxed scoring
        if (config.enableFallback && directCandidates.isNotEmpty()) {
            val fallback = directCandidates
                .map { scorer.calculateScore(it, target, action) }
                .maxByOrNull { it.overallScore }

            if (fallback != null && fallback.overallScore >= config.fallbackMinimumScore) {
                logger.debug("Fallback match: score=${fallback.overallScore}")
                return FindResult.success(
                    fallback.withRelationship(FindMethod.FALLBACK, ElementRelationship.SELF)
                )
            }
        }

        logger.debug("Element not found: $target")
        return FindResult.notFound(target, action)
    }

    /**
     * Find element with full stability handling (recommended for most use cases).
     *
     * This method combines all stability features:
     * - Page load waiting
     * - Element stability verification
     * - Smart scrolling
     * - Stale element recovery
     *
     * @param target Element identifier
     * @param action Intended action
     * @param timeoutMs Maximum time for entire operation
     * @return FindResult with stable element
     */
    fun findElementStable(
        target: String,
        action: ActionType,
        timeoutMs: Long = stabilityConfig.getActionTimeout(action.name, platform)
    ): FindResult {
        val startTime = System.currentTimeMillis()

        logger.debug("Stable find: target='$target', action=$action, timeout=${timeoutMs}ms")

        // Use retry mechanism for the entire find operation
        val result = retryMechanism.execute(
            operation = { ctx ->
                val remainingTime = timeoutMs - ctx.elapsedTimeMs
                if (remainingTime <= 0) {
                    throw RuntimeException("Timeout exceeded")
                }

                val context = FindContext(
                    waitForPageReady = ctx.attemptNumber == 1,  // Only wait on first attempt
                    scrollToFind = ctx.attemptNumber >= 2       // Enable scroll on retry
                )

                val findResult = findElement(target, action, context)

                if (!findResult.success) {
                    throw RuntimeException("Element not found: $target")
                }

                findResult
            },
            maxAttempts = stabilityConfig.maxTransientRetries + 1,
            timeoutMs = timeoutMs
        )

        return when (result) {
            is RetryWithBackoff.RetryResult.Success -> result.value
            is RetryWithBackoff.RetryResult.Failure -> FindResult.notFound(target, action,
                "Element not found after ${result.attempts} attempts: ${result.lastException.message}")
        }
    }

    /**
     * Find element specifically for text entry.
     */
    fun findInputField(
        target: String,
        context: FindContext = FindContext()
    ): FindResult {
        // First try direct find
        var result = findElement(target, ActionType.ENTER_TEXT, context)
        if (result.success) return result

        // Try finding by label and getting associated input
        val labelCandidates = findDirectCandidates(target)
            .filter { ElementTrait.HAS_TEXT in it.traits }

        for (labelCandidate in labelCandidates) {
            val allCandidates = getAllVisibleCandidates()
            val inputField = spatialAnalyzer.findInputForLabel(labelCandidate, allCandidates)

            if (inputField != null) {
                val scored = scorer.calculateScore(inputField, target, ActionType.ENTER_TEXT)
                    .withRelationship(FindMethod.LABEL_ASSOCIATION, ElementRelationship.LABEL_FOR)
                return FindResult.success(scored)
            }
        }

        return FindResult.notFound(target, ActionType.ENTER_TEXT)
    }

    /**
     * Find clickable element, considering parent traversal.
     */
    fun findClickable(
        target: String,
        context: FindContext = FindContext()
    ): FindResult {
        val result = findElement(target, ActionType.CLICK, context)
        if (result.success) return result

        // Additional strategy: find any element with text and traverse to clickable parent
        val textElements = findByText(target)
        for (textElement in textElements) {
            val candidate = createCandidate(textElement)
            val clickableParent = findClickableParent(candidate)

            if (clickableParent != null) {
                return FindResult.success(
                    clickableParent.withRelationship(
                        FindMethod.PARENT_TRAVERSAL,
                        ElementRelationship.PARENT
                    )
                )
            }
        }

        return FindResult.notFound(target, ActionType.CLICK)
    }

    /**
     * Find element in a dropdown/picker by option text.
     */
    fun findDropdownOption(
        optionText: String,
        dropdownElement: WebElement? = null
    ): FindResult {
        // If dropdown provided, search within it
        val searchContext = dropdownElement ?: driver

        val options = try {
            when (platform) {
                Platform.ANDROID -> {
                    searchContext.findElements(By.className("android.widget.TextView")) +
                            searchContext.findElements(By.className("android.widget.CheckedTextView"))
                }

                Platform.IOS -> {
                    searchContext.findElements(By.className("XCUIElementTypeStaticText")) +
                            searchContext.findElements(By.className("XCUIElementTypeCell"))
                }

                Platform.WEB -> {
                    searchContext.findElements(By.tagName("option")) +
                            searchContext.findElements(By.cssSelector("[role='option']")) +
                            searchContext.findElements(By.cssSelector("li"))
                }
            }
        } catch (e: Exception) {
            emptyList()
        }

        val candidates = options.mapNotNull { createCandidate(it) }
        val scored = scorer.rankCandidates(candidates, optionText, ActionType.SELECT)

        return if (scored.isNotEmpty() && scored.first().textMatchScore > 0.5) {
            FindResult.success(
                scored.first().withRelationship(FindMethod.DROPDOWN_OPTION, ElementRelationship.CHILD)
            )
        } else {
            FindResult.notFound(optionText, ActionType.SELECT)
        }
    }

    /**
     * Get all visible elements as candidates.
     */
    fun getAllVisibleCandidates(): List<ElementCandidate> {
        val elements = try {
            driver.findElements(By.xpath("//*"))
        } catch (e: Exception) {
            emptyList()
        }

        return elements.mapNotNull { element ->
            try {
                if (element.isDisplayed) createCandidate(element) else null
            } catch (e: Exception) {
                null
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE SEARCH METHODS
    // ═══════════════════════════════════════════════════════════════

    private fun findDirectCandidates(target: String): List<ElementCandidate> {
        val candidates = mutableListOf<ElementCandidate>()
        val normalizedTarget = target.trim()

        // Strategy 1: Accessibility ID
        tryFind { findByAccessibilityId(normalizedTarget) }?.let { candidates.addAll(it) }

        // Strategy 2: Resource/Element ID
        tryFind { findByResourceId(normalizedTarget) }?.let { candidates.addAll(it) }

        // Strategy 3: Text content
        tryFind { findByText(normalizedTarget) }?.let { candidates.addAll(it.map { createCandidate(it) }) }

        // Strategy 4: Content description
        tryFind { findByContentDescription(normalizedTarget) }?.let { candidates.addAll(it) }

        // Strategy 5: XPath with text contains
        tryFind { findByXPathText(normalizedTarget) }?.let { candidates.addAll(it) }

        // Strategy 6: Platform-specific strategies
        tryFind { findByPlatformSpecific(normalizedTarget) }?.let { candidates.addAll(it) }

        return candidates.distinctBy { it.element }
    }

    private fun findByAccessibilityId(target: String): List<ElementCandidate> {
        val elements = when (platform) {
            Platform.ANDROID -> {
                driver.findElements(By.xpath("//*[@content-desc='$target']")) +
                        driver.findElements(By.xpath("//*[contains(@content-desc, '$target')]"))
            }

            Platform.IOS -> {
                driver.findElements(By.xpath("//*[@name='$target']")) +
                        driver.findElements(By.xpath("//*[@label='$target']"))
            }

            Platform.WEB -> {
                driver.findElements(By.cssSelector("[aria-label='$target']")) +
                        driver.findElements(By.cssSelector("[data-testid='$target']"))
            }
        }
        return elements.map { createCandidate(it) }
    }

    private fun findByResourceId(target: String): List<ElementCandidate> {
        val normalizedId = target.lowercase().replace(" ", "_").replace("-", "_")

        val elements = when (platform) {
            Platform.ANDROID -> {
                driver.findElements(By.xpath("//*[contains(@resource-id, '$normalizedId')]"))
            }

            Platform.IOS -> {
                driver.findElements(By.xpath("//*[contains(@name, '$normalizedId')]"))
            }

            Platform.WEB -> {
                driver.findElements(By.id(target)) +
                        driver.findElements(By.id(normalizedId)) +
                        driver.findElements(By.name(target))
            }
        }
        return elements.map { createCandidate(it) }
    }

    private fun findByText(target: String): List<WebElement> {
        return when (platform) {
            Platform.ANDROID -> {
                driver.findElements(By.xpath("//*[@text='$target']")) +
                        driver.findElements(By.xpath("//*[contains(@text, '$target')]"))
            }

            Platform.IOS -> {
                driver.findElements(By.xpath("//*[@label='$target']")) +
                        driver.findElements(By.xpath("//*[contains(@label, '$target')]")) +
                        driver.findElements(By.xpath("//*[@value='$target']"))
            }

            Platform.WEB -> {
                driver.findElements(By.xpath("//*[text()='$target']")) +
                        driver.findElements(By.xpath("//*[contains(text(), '$target')]")) +
                        driver.findElements(By.xpath("//*[@placeholder='$target']"))
            }
        }
    }

    private fun findByContentDescription(target: String): List<ElementCandidate> {
        val elements = when (platform) {
            Platform.ANDROID -> {
                driver.findElements(By.xpath("//*[@content-desc='$target']")) +
                        driver.findElements(By.xpath("//*[contains(@content-desc, '$target')]"))
            }

            Platform.IOS -> {
                driver.findElements(By.xpath("//*[@accessibility-id='$target']"))
            }

            Platform.WEB -> {
                driver.findElements(By.cssSelector("[title='$target']")) +
                        driver.findElements(By.cssSelector("[alt='$target']"))
            }
        }
        return elements.map { createCandidate(it) }
    }

    private fun findByXPathText(target: String): List<ElementCandidate> {
        val xpaths = listOf(
            "//*[normalize-space(.)='$target']",
            "//*[contains(normalize-space(.), '$target')]"
        )

        val elements = xpaths.flatMap {
            try {
                driver.findElements(By.xpath(it))
            } catch (e: Exception) {
                emptyList()
            }
        }

        return elements.map { createCandidate(it) }
    }

    private fun findByPlatformSpecific(target: String): List<ElementCandidate> {
        val elements = when (platform) {
            Platform.ANDROID -> {
                // UIAutomator selector
                try {
                    driver.findElements(
                        By.xpath("//*[contains(@text, '$target') or contains(@content-desc, '$target')]")
                    )
                } catch (e: Exception) {
                    emptyList()
                }
            }

            Platform.IOS -> {
                // iOS predicate
                try {
                    driver.findElements(
                        By.xpath("//*[contains(@label, '$target') or contains(@name, '$target') or contains(@value, '$target')]")
                    )
                } catch (e: Exception) {
                    emptyList()
                }
            }

            Platform.WEB -> {
                // CSS selectors for common patterns
                val selectors = listOf(
                    "button:contains('$target')",
                    "a:contains('$target')",
                    "[class*='$target' i]",
                    "[data-test*='$target' i]"
                )
                selectors.flatMap {
                    try {
                        driver.findElements(By.cssSelector(it))
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }
        }
        return elements.map { createCandidate(it) }
    }

    // ═══════════════════════════════════════════════════════════════
    // TRAVERSAL METHODS
    // ═══════════════════════════════════════════════════════════════

    private fun tryParentTraversal(
        candidate: ElementCandidate?,
        action: ActionType
    ): ElementCandidate? {
        if (candidate == null) return null

        var current = candidate.element
        var depth = 0
        val maxDepth = config.maxParentTraversalDepth

        while (depth < maxDepth) {
            val parent = try {
                current.findElement(By.xpath(".."))
            } catch (e: Exception) {
                return null
            }

            val parentCandidate = createCandidate(parent)

            if (parentCandidate.supportsAction(action)) {
                return scorer.calculateScore(parentCandidate, candidate.text, action)
                    .withRelationship(FindMethod.PARENT_TRAVERSAL, ElementRelationship.PARENT)
            }

            current = parent
            depth++
        }

        return null
    }

    private fun tryChildTraversal(
        candidate: ElementCandidate?,
        action: ActionType,
        target: String
    ): ElementCandidate? {
        if (candidate == null) return null

        val children = try {
            candidate.element.findElements(By.xpath(".//*"))
        } catch (e: Exception) {
            return null
        }

        val childCandidates = children.mapNotNull {
            try {
                if (it.isDisplayed) createCandidate(it) else null
            } catch (e: Exception) {
                null
            }
        }

        val actionable = childCandidates.filter { it.supportsAction(action) }

        return if (actionable.isNotEmpty()) {
            val scored = scorer.rankCandidates(actionable, target, action)
            scored.firstOrNull()?.withRelationship(FindMethod.CHILD_TRAVERSAL, ElementRelationship.CHILD)
        } else {
            null
        }
    }

    private fun trySpatialSearch(
        target: String,
        action: ActionType,
        existingCandidates: List<ElementCandidate>
    ): ElementCandidate? {
        // Find potential labels matching the target
        val labels = existingCandidates.filter {
            ElementTrait.HAS_TEXT in it.traits &&
                    it.text.contains(target, ignoreCase = true)
        }

        if (labels.isEmpty()) return null

        // Get all visible candidates
        val allCandidates = getAllVisibleCandidates()

        for (label in labels) {
            // Find elements near the label that support the action
            val nearby = spatialAnalyzer.findNearbyElements(
                reference = label.bounds,
                candidates = allCandidates.filter { it.supportsAction(action) },
                direction = when (action) {
                    ActionType.ENTER_TEXT -> SpatialDirection.RIGHT
                    ActionType.TOGGLE, ActionType.CHECK -> SpatialDirection.LEFT
                    else -> SpatialDirection.ANY
                }
            )

            if (nearby.isNotEmpty()) {
                val best = nearby.first()
                return best.candidate
                    .withScores(spatial = best.spatialScore)
                    .withRelationship(FindMethod.SPATIAL_SEARCH, best.relationship)
            }
        }

        return null
    }

    private fun findClickableParent(candidate: ElementCandidate): ElementCandidate? {
        return tryParentTraversal(candidate, ActionType.CLICK)
    }

    // ═══════════════════════════════════════════════════════════════
    // STABILITY METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Find candidates with retry for transient failures.
     */
    private fun findDirectCandidatesWithRetry(target: String): List<ElementCandidate> {
        if (!stabilityConfig.staleElementRecoveryEnabled) {
            return findDirectCandidates(target)
        }

        val result = retryMechanism.execute(
            operation = { findDirectCandidates(target) },
            maxAttempts = 2,
            timeoutMs = config.searchTimeoutMs
        )

        return when (result) {
            is RetryWithBackoff.RetryResult.Success -> result.value
            is RetryWithBackoff.RetryResult.Failure -> emptyList()
        }
    }

    /**
     * Wait for element to stabilize (position/size stop changing).
     */
    private fun waitForElementStability(candidate: ElementCandidate): ElementCandidate? {
        if (!stabilityConfig.elementStabilityEnabled) {
            return candidate
        }

        val result = stabilityWaiter.waitForStability(
            candidate.element,
            stabilityConfig.stabilityTimeoutMs
        )

        return when (result) {
            is ElementStabilityWaiter.StabilityResult.Stable -> candidate
            is ElementStabilityWaiter.StabilityResult.Unstable -> {
                logger.debug("Element unstable but continuing: ${result.reason}")
                candidate  // Return anyway, let caller decide
            }
            is ElementStabilityWaiter.StabilityResult.ElementGone -> {
                logger.debug("Element disappeared during stability wait")
                null
            }
        }
    }

    /**
     * Parent traversal with stability checks.
     */
    private fun tryParentTraversalWithStability(
        candidate: ElementCandidate?,
        action: ActionType
    ): ElementCandidate? {
        val parentResult = tryParentTraversal(candidate, action) ?: return null
        return waitForElementStability(parentResult)
    }

    /**
     * Child traversal with stability checks.
     */
    private fun tryChildTraversalWithStability(
        candidate: ElementCandidate?,
        action: ActionType,
        target: String
    ): ElementCandidate? {
        val childResult = tryChildTraversal(candidate, action, target) ?: return null
        return waitForElementStability(childResult)
    }

    /**
     * Spatial search with stability checks.
     */
    private fun trySpatialSearchWithStability(
        target: String,
        action: ActionType,
        existingCandidates: List<ElementCandidate>
    ): ElementCandidate? {
        val spatialResult = trySpatialSearch(target, action, existingCandidates) ?: return null
        return waitForElementStability(spatialResult)
    }

    /**
     * Scroll to find element using smart scroll strategy.
     */
    private fun scrollToFindElement(target: String, action: ActionType): ElementCandidate? {
        logger.debug("Attempting scroll to find: '$target'")

        val scrollResult = scrollStrategy.scrollAllDirectionsToFind(
            driver = driver,
            findElement = {
                val candidates = findDirectCandidates(target)
                val scored = scorer.rankCandidates(candidates, target, action)
                val best = scored.filter { it.supportsAction(action) }.firstOrNull()

                if (best != null && best.overallScore >= config.minimumScore) {
                    try {
                        if (best.element.isDisplayed) best.element else null
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            },
            primaryDirection = SmartScrollStrategy.ScrollDirection.DOWN,
            maxAttempts = stabilityConfig.maxScrollAttempts
        )

        return when (scrollResult) {
            is SmartScrollStrategy.ScrollResult.ElementFound -> {
                // Found the element, now wait for it to stabilize after scroll
                val candidate = createCandidate(scrollResult.element)
                val scored = scorer.calculateScore(candidate, target, action)
                waitForElementStability(scored)
            }
            is SmartScrollStrategy.ScrollResult.EndOfContent -> {
                logger.debug("Reached end of content without finding element")
                null
            }
            else -> null
        }
    }

    /**
     * Execute action on element with stale element recovery.
     */
    fun <T> executeOnElementWithRecovery(
        target: String,
        action: ActionType,
        operation: (WebElement) -> T
    ): T? {
        val refind: () -> WebElement? = {
            val result = findElement(target, action, FindContext())
            if (result is FindResult.SingleElement) {
                result.element
            } else {
                null
            }
        }

        val result = retryMechanism.executeWithStaleRecovery(refind, operation)

        return when (result) {
            is RetryWithBackoff.RetryResult.Success -> result.value
            is RetryWithBackoff.RetryResult.Failure -> {
                logger.error("Operation failed: ${result.lastException.message}")
                null
            }
        }
    }

    /**
     * Wait for element to disappear (useful for loading indicators).
     */
    fun waitForElementGone(target: String, timeoutMs: Long = stabilityConfig.stabilityTimeoutMs): Boolean {
        return stabilityWaiter.waitForDisappearance(
            findElement = {
                val candidates = findDirectCandidates(target)
                candidates.firstOrNull()?.element
            },
            timeoutMs = timeoutMs
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════

    private fun createCandidate(element: WebElement): ElementCandidate {
        return ElementCandidateBuilder(element, adapter).build()
    }

    private fun <T> tryFind(finder: () -> T): T? {
        return try {
            finder()
        } catch (e: Exception) {
            logger.debug("Find failed: ${e.message}")
            null
        }
    }
}

/**
 * Configuration for SmartElementFinder.
 */
data class SmartFinderConfig(
    /** Minimum score to accept a match */
    val minimumScore: Double = 0.4,

    /** Minimum score for fallback matches */
    val fallbackMinimumScore: Double = 0.2,

    /** Maximum depth to traverse parent elements */
    val maxParentTraversalDepth: Int = 5,

    /** Enable fallback matching with relaxed criteria */
    val enableFallback: Boolean = true,

    /** Enable spatial search for label associations */
    val enableSpatialSearch: Boolean = true,

    /** Enable compound element resolution */
    val enableCompoundResolution: Boolean = true,

    /** Timeout for element searches in milliseconds */
    val searchTimeoutMs: Long = 5000,

    /** Enable element stability verification */
    val enableStabilityCheck: Boolean = true,

    /** Enable smart scrolling to find elements */
    val enableSmartScroll: Boolean = true,

    /** Enable page load intelligence */
    val enablePageLoadWait: Boolean = true
)

/**
 * Context for element finding.
 */
data class FindContext(
    /** Current page/screen name */
    val pageName: String? = null,

    /** Reference element for spatial search */
    val referenceElement: WebElement? = null,

    /** Preferred search direction */
    val preferredDirection: SpatialDirection? = null,

    /** Additional hints for finding */
    val hints: Map<String, String> = emptyMap(),

    /** Wait for page to be ready before searching */
    val waitForPageReady: Boolean = true,

    /** Enable scrolling to find the element */
    val scrollToFind: Boolean = false,

    /** Wait for element stability before returning */
    val waitForStability: Boolean = true
)

/**
 * Result of element finding.
 */
sealed class FindResult {
    abstract val success: Boolean

    /** Successful find with single element */
    data class SingleElement(
        val candidate: ElementCandidate
    ) : FindResult() {
        override val success = true
        val element: WebElement get() = candidate.element
    }

    /** Successful find requiring multiple actions (compound UI) */
    data class CompoundActions(
        val actions: List<ResolvedAction>
    ) : FindResult() {
        override val success = true
    }

    /** Element not found */
    data class NotFound(
        val target: String,
        val action: ActionType,
        val reason: String = "No matching element found"
    ) : FindResult() {
        override val success = false
    }

    companion object {
        fun success(candidate: ElementCandidate) = SingleElement(candidate)
        fun compound(actions: List<ResolvedAction>) = CompoundActions(actions)
        fun notFound(target: String, action: ActionType, reason: String = "No matching element found") =
            NotFound(target, action, reason)
    }
}
