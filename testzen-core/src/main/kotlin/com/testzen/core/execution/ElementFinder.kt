package com.testzen.core.execution

import com.testzen.core.config.TestZenConfig
import com.testzen.core.locator.*
import com.testzen.core.locator.smart.*
import com.testzen.core.model.Platform
import io.appium.java_client.AppiumBy
import io.appium.java_client.AppiumDriver
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Handles element finding with optional self-healing and smart finding support.
 *
 * Responsible for:
 * - Finding elements using various locator strategies
 * - Coordinating with SelfHealingLocator when enabled
 * - Delegating to SmartElementFinder for intelligent element finding
 * - Building platform-specific locators
 * - Scroll-to-find functionality
 *
 * Single Responsibility: Element location and discovery orchestration.
 */
class ElementFinder(
    private val driver: WebDriver,
    private val config: TestZenConfig,
    private val platform: Platform,
    private val selfHealingLocator: SelfHealingLocator? = null,
    private val gestureHandler: GestureHandler? = null
) {
    private val logger = LoggerFactory.getLogger(ElementFinder::class.java)
    private val locatorGenerator = LocatorGenerator.forPlatform(platform)

    // Smart element finder for intelligent element finding
    private val smartFinder: SmartElementFinder by lazy {
        SmartElementFinder(
            driver = driver,
            platform = platform,
            config = SmartFinderConfig(
                minimumScore = config.smartFindMinimumScore,
                enableFallback = true,
                enableSpatialSearch = config.smartFindEnabled,
                enableCompoundResolution = config.smartFindEnabled
            )
        )
    }

    /**
     * Find an element by target name.
     *
     * @param target The element identifier (text, label, or accessibility ID)
     * @param timeoutMs Maximum time to wait for element
     * @param forTextEntry If true, include additional text entry field strategies
     * @return The found WebElement, or null if not found
     */
    fun find(target: String, timeoutMs: Long, forTextEntry: Boolean = false): WebElement? {
        // Use smart finder if enabled
        if (config.smartFindEnabled) {
            val action = if (forTextEntry) ActionType.ENTER_TEXT else ActionType.CLICK
            val result = findSmart(target, action)
            if (result != null) return result
        }

        // Use self-healing locator if enabled
        if (config.selfHealingEnabled && selfHealingLocator != null) {
            return findWithSelfHealing(target, timeoutMs, forTextEntry)
        }

        // Standard locator strategy
        return findStandard(target, timeoutMs, forTextEntry)
    }

    /**
     * Find an element for a specific action type using smart finding.
     *
     * @param target The element identifier
     * @param action The intended action type
     * @return The found WebElement, or null if not found
     */
    fun findForAction(target: String, action: ActionType): WebElement? {
        if (config.smartFindEnabled) {
            return findSmart(target, action)
        }

        // Fall back to standard find
        val forTextEntry = action == ActionType.ENTER_TEXT
        return findStandard(target, config.elementTimeoutMs, forTextEntry)
    }

    /**
     * Find an element using smart finding with spatial analysis.
     *
     * @param target The element identifier
     * @param action The intended action type
     * @return The found WebElement, or null if not found
     */
    fun findSmart(target: String, action: ActionType): WebElement? {
        return try {
            val result = smartFinder.findElement(target, action)
            when (result) {
                is FindResult.SingleElement -> {
                    logger.debug("Smart find success: target='$target', method=${result.candidate.findMethod}")
                    result.element
                }
                is FindResult.CompoundActions -> {
                    // For compound actions, return the first element
                    logger.debug("Smart find compound: ${result.actions.size} actions")
                    result.actions.firstOrNull()?.element?.element
                }
                is FindResult.NotFound -> {
                    logger.debug("Smart find not found: $target")
                    null
                }
            }
        } catch (e: Exception) {
            logger.debug("Smart find error: ${e.message}")
            null
        }
    }

    /**
     * Find element with compound action resolution.
     * Returns a FindResult which may contain multiple actions for compound UI.
     *
     * @param target The element identifier or instruction
     * @param action The intended action type
     * @return FindResult with element(s) or actions
     */
    fun findWithCompoundSupport(target: String, action: ActionType): FindResult {
        return try {
            smartFinder.findElement(target, action)
        } catch (e: Exception) {
            logger.debug("Compound find error: ${e.message}")
            FindResult.notFound(target, action, e.message ?: "Unknown error")
        }
    }

    /**
     * Find input field by label using spatial analysis.
     *
     * @param label The label text near the input field
     * @return The found input WebElement, or null if not found
     */
    fun findInputByLabel(label: String): WebElement? {
        if (config.smartFindEnabled) {
            val result = smartFinder.findInputField(label)
            if (result.success && result is FindResult.SingleElement) {
                return result.element
            }
        }
        return findStandard(label, config.elementTimeoutMs, forTextEntry = true)
    }

    /**
     * Find clickable element, traversing to parent if needed.
     *
     * @param target The element identifier
     * @return The found clickable WebElement, or null if not found
     */
    fun findClickable(target: String): WebElement? {
        if (config.smartFindEnabled) {
            val result = smartFinder.findClickable(target)
            if (result.success && result is FindResult.SingleElement) {
                return result.element
            }
        }
        return findStandard(target, config.elementTimeoutMs, forTextEntry = false)
    }

    /**
     * Find dropdown option by text.
     *
     * @param optionText The option text to find
     * @param dropdownElement Optional dropdown element to search within
     * @return The found option WebElement, or null if not found
     */
    fun findDropdownOption(optionText: String, dropdownElement: WebElement? = null): WebElement? {
        if (config.smartFindEnabled) {
            val result = smartFinder.findDropdownOption(optionText, dropdownElement)
            if (result.success && result is FindResult.SingleElement) {
                return result.element
            }
        }
        return findStandard(optionText, config.elementTimeoutMs, forTextEntry = false)
    }

    /**
     * Find element using self-healing locator with fallback strategies.
     */
    private fun findWithSelfHealing(target: String, timeoutMs: Long, forTextEntry: Boolean): WebElement? {
        val healer = selfHealingLocator ?: return findStandard(target, timeoutMs, forTextEntry)

        // Build additional locator hints for text entry fields
        val hints = if (forTextEntry) {
            locatorGenerator.generateForTextEntry(target)
        } else {
            emptyList()
        }

        return try {
            healer.findElement(target, hints)
        } catch (e: org.openqa.selenium.NoSuchElementException) {
            logger.debug("Self-healing locator failed for '$target', trying scroll strategy")

            // Try scrolling to find element
            if (config.scrollToFindElement && gestureHandler != null) {
                for (i in 1..config.maxScrollAttempts) {
                    gestureHandler.scroll(ScrollDirection.DOWN)
                    Thread.sleep(500)

                    try {
                        return healer.findElement(target, hints)
                    } catch (e2: org.openqa.selenium.NoSuchElementException) {
                        // Continue scrolling
                    }
                }
            }

            null
        }
    }

    /**
     * Standard element finding without self-healing.
     */
    private fun findStandard(target: String, timeoutMs: Long, forTextEntry: Boolean = false): WebElement? {
        val wait = WebDriverWait(driver, Duration.ofMillis(timeoutMs))
        val locators = buildSeleniumLocators(target, forTextEntry)

        for (locator in locators) {
            try {
                val element = wait.until(ExpectedConditions.presenceOfElementLocated(locator))
                if (element.isDisplayed) {
                    logger.debug("Found element with locator: $locator")
                    return element
                }
            } catch (e: Exception) {
                // Try next locator
            }
        }

        // Try scrolling to find element
        if (config.scrollToFindElement && gestureHandler != null) {
            for (i in 1..config.maxScrollAttempts) {
                gestureHandler.scroll(ScrollDirection.DOWN)
                Thread.sleep(500)

                for (locator in locators) {
                    try {
                        val element = driver.findElement(locator)
                        if (element.isDisplayed) {
                            return element
                        }
                    } catch (e: Exception) {
                        // Continue
                    }
                }
            }
        }

        return null
    }

    /**
     * Build Selenium By locators for standard element finding.
     * Note: For self-healing, use LocatorGenerator instead.
     */
    fun buildSeleniumLocators(target: String, forTextEntry: Boolean = false): List<By> {
        val locators = mutableListOf<By>()

        // Accessibility ID (highest priority)
        locators.add(AppiumBy.accessibilityId(target))

        // ID/Resource ID
        locators.add(By.id(target))

        // Platform-specific text locators
        if (driver is AppiumDriver) {
            // Android UIAutomator
            locators.add(AppiumBy.androidUIAutomator("new UiSelector().text(\"$target\")"))
            locators.add(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"$target\")"))
            locators.add(AppiumBy.androidUIAutomator("new UiSelector().description(\"$target\")"))

            // iOS NSPredicate
            locators.add(AppiumBy.iOSNsPredicateString("label == '$target'"))
            locators.add(AppiumBy.iOSNsPredicateString("label CONTAINS '$target'"))
            locators.add(AppiumBy.iOSNsPredicateString("name == '$target'"))
        }

        // XPath with text
        locators.add(By.xpath("//*[@text='$target']"))
        locators.add(By.xpath("//*[contains(@text, '$target')]"))
        locators.add(By.xpath("//*[@content-desc='$target']"))
        locators.add(By.xpath("//*[contains(@content-desc, '$target')]"))
        locators.add(By.xpath("//*[contains(text(), '$target')]"))

        // For text entry, also look for editable fields near the label
        if (forTextEntry) {
            locators.add(By.xpath("//*[@text='$target']/following-sibling::*[1][@focusable='true']"))
            locators.add(By.xpath("//*[contains(@text, '$target')]/..//*[@focusable='true']"))
        }

        // CSS selectors for web
        locators.add(By.cssSelector("[placeholder='$target']"))
        locators.add(By.cssSelector("[aria-label='$target']"))
        locators.add(By.cssSelector("[name='$target']"))

        return locators
    }

    /**
     * Wait for element to appear or disappear.
     *
     * @param target The element identifier
     * @param timeoutMs Maximum wait time
     * @param shouldDisappear If true, wait for element to disappear
     * @return True if condition met, false otherwise
     */
    fun waitForElement(target: String, timeoutMs: Long, shouldDisappear: Boolean): Boolean {
        val wait = WebDriverWait(driver, Duration.ofMillis(timeoutMs))

        return try {
            if (shouldDisappear) {
                val locators = buildSeleniumLocators(target, false)
                for (locator in locators) {
                    try {
                        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator))
                        return true
                    } catch (e: Exception) {
                        // Try next locator
                    }
                }
                true // Element not found = disappeared
            } else {
                val element = find(target, timeoutMs)
                element != null
            }
        } catch (e: Exception) {
            !shouldDisappear // Return false for appear, true for disappear
        }
    }
}
