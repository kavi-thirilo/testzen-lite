package com.testzen.core.execution

import com.testzen.core.config.TestZenConfig
import com.testzen.core.locator.*
import com.testzen.core.locator.smart.*
import com.testzen.core.model.Platform
import com.testzen.core.verification.*
import io.appium.java_client.AppiumDriver
import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.ios.IOSDriver
import kotlinx.coroutines.delay
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.slf4j.LoggerFactory

/**
 * Executes parsed instructions against a WebDriver.
 *
 * Single Responsibility: Route instructions to appropriate handlers and coordinate execution.
 *
 * Delegates to:
 * - ElementFinder: Element location and discovery
 * - GestureHandler: Touch gestures (swipe, scroll, long press)
 * - SelfHealingLocator: Self-healing element finding (when enabled)
 *
 * Usage:
 * ```kotlin
 * val executor = InstructionExecutor(driver, config, Platform.ANDROID)
 * val result = executor.execute(Instruction.Click("Login Button"), 30000)
 * ```
 */
class InstructionExecutor(
    private val driver: WebDriver,
    private val config: TestZenConfig,
    private val platform: Platform = Platform.ANDROID
) {
    private val logger = LoggerFactory.getLogger(InstructionExecutor::class.java)

    // ═══════════════════════════════════════════════════════════════
    // COMPONENTS (Lazy initialization for clean dependency management)
    // ═══════════════════════════════════════════════════════════════

    /** Handles touch gestures and swipe actions */
    private val gestureHandler: GestureHandler by lazy {
        GestureHandler(driver)
    }

    /** Page object repository for large projects (optional) */
    private val pageObjectRepository: PageObjectRepository? by lazy {
        if (config.selfHealingEnabled && config.usePageObjectRepository) {
            val cacheMode = when (config.cacheMode.uppercase()) {
                "READ_ONLY" -> CacheMode.READ_ONLY
                "DISABLED" -> CacheMode.DISABLED
                else -> CacheMode.READ_WRITE
            }
            PageObjectRepository(
                repositoryPath = config.pageObjectsDirectory,
                autoSave = config.autoSavePageObjects,
                cacheMode = cacheMode
            )
        } else null
    }

    /** Self-healing locator for smart element finding */
    private val selfHealingLocator: SelfHealingLocator? by lazy {
        if (config.selfHealingEnabled) {
            val healingConfig = SelfHealingConfig(
                enableFallback = true,
                maxFallbackAttempts = config.maxFallbackAttempts,
                cacheEnabled = true,
                logHealingEvents = true,
                learnFromHealing = config.learnFromHealing
            )

            if (config.usePageObjectRepository) {
                SelfHealingLocator(
                    driver = driver,
                    platform = platform,
                    repository = pageObjectRepository,
                    config = healingConfig
                )
            } else {
                SelfHealingLocator(
                    driver = driver,
                    platform = platform,
                    cache = LocatorCache(config.locatorCacheDirectory),
                    config = healingConfig
                )
            }
        } else null
    }

    /** Handles element finding with optional self-healing */
    private val elementFinder: ElementFinder by lazy {
        ElementFinder(
            driver = driver,
            config = config,
            platform = platform,
            selfHealingLocator = selfHealingLocator,
            gestureHandler = gestureHandler
        )
    }

    /** Verification engine for robust assertions */
    private val verificationEngine: VerificationEngine by lazy {
        VerificationEngine(
            driver = driver,
            elementFinder = elementFinder,
            config = config,
            platform = platform
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════

    /**
     * Result of instruction execution.
     */
    data class ExecutionResult(
        val success: Boolean,
        val error: String? = null,
        val elementFound: Boolean? = null
    )

    /**
     * Execute an instruction.
     */
    suspend fun execute(instruction: Instruction, timeoutMs: Long = 30000): ExecutionResult {
        return try {
            when (instruction) {
                // Interaction instructions
                is Instruction.Click -> executeClick(instruction, timeoutMs)
                is Instruction.DoubleTap -> executeDoubleTap(instruction, timeoutMs)
                is Instruction.LongPress -> executeLongPress(instruction, timeoutMs)
                is Instruction.EnterText -> executeEnterText(instruction, timeoutMs)
                is Instruction.ClearText -> executeClearText(instruction, timeoutMs)

                // Verification instructions
                is Instruction.VerifyDisplayed -> executeVerifyDisplayed(instruction, timeoutMs)
                is Instruction.VerifyNotDisplayed -> executeVerifyNotDisplayed(instruction, timeoutMs)
                is Instruction.VerifyText -> executeVerifyText(instruction, timeoutMs)
                is Instruction.VerifyEnabled -> executeVerifyEnabled(instruction, timeoutMs)
                is Instruction.VerifyDisabled -> executeVerifyDisabled(instruction, timeoutMs)

                // Wait instructions
                is Instruction.Wait -> executeWait(instruction)
                is Instruction.WaitForElement -> executeWaitForElement(instruction, timeoutMs)

                // Navigation instructions
                is Instruction.Scroll -> executeScroll(instruction)
                is Instruction.Swipe -> executeSwipe(instruction)
                is Instruction.NavigateBack -> executeNavigateBack()
                is Instruction.NavigateForward -> executeNavigateForward()

                // App lifecycle instructions
                is Instruction.LaunchApp -> executeLaunchApp()
                is Instruction.CloseApp -> executeCloseApp()
                is Instruction.TakeScreenshot -> executeTakeScreenshot(instruction)

                // Form control instructions
                is Instruction.SelectOption -> executeSelectOption(instruction, timeoutMs)
                is Instruction.CheckCheckbox -> executeCheckCheckbox(instruction, timeoutMs)
                is Instruction.UncheckCheckbox -> executeUncheckCheckbox(instruction, timeoutMs)
                is Instruction.ToggleSwitch -> executeToggleSwitch(instruction, timeoutMs)
            }
        } catch (e: Exception) {
            logger.error("Instruction execution failed: ${e.message}")
            ExecutionResult(success = false, error = e.message)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SELF-HEALING API
    // ═══════════════════════════════════════════════════════════════

    /** Get the self-healing report for this session */
    fun getHealingReport(): List<HealingEvent>? = selfHealingLocator?.getHealingReport()

    /** Get cache statistics */
    fun getCacheStats(): CacheStats? = selfHealingLocator?.getCacheStats()

    /** Get page object repository statistics */
    fun getRepositoryStats(): RepositoryStats? = selfHealingLocator?.getRepositoryStats()

    /** Set the current page context for page object repository mode */
    fun setCurrentPage(pageName: String) {
        selfHealingLocator?.setCurrentPage(pageName)
    }

    /** Save the page object repository to disk */
    fun saveRepository() {
        selfHealingLocator?.saveRepository()
        pageObjectRepository?.save()
    }

    // ═══════════════════════════════════════════════════════════════
    // VERIFICATION API
    // ═══════════════════════════════════════════════════════════════

    /** Get the verification engine for advanced verifications */
    fun getVerificationEngine(): VerificationEngine = verificationEngine

    /** Get verification report from soft assertions */
    fun getVerificationReport(): VerificationReport = verificationEngine.getReport()

    /** Clear verification results */
    fun clearVerificationResults() = verificationEngine.clearResults()

    // ═══════════════════════════════════════════════════════════════
    // INSTRUCTION HANDLERS
    // ═══════════════════════════════════════════════════════════════

    private fun executeClick(instruction: Instruction.Click, timeoutMs: Long): ExecutionResult {
        // Use smart finding with clickable detection and parent traversal
        val element = if (config.smartFindEnabled) {
            elementFinder.findClickable(instruction.target)
        } else {
            elementFinder.find(instruction.target, timeoutMs)
        }

        if (element == null) {
            return ExecutionResult(success = false, error = "Element not found: ${instruction.target}", elementFound = false)
        }

        element.click()
        return ExecutionResult(success = true, elementFound = true)
    }

    private fun executeEnterText(instruction: Instruction.EnterText, timeoutMs: Long): ExecutionResult {
        // Use smart finding with label association for input fields
        val element = if (config.smartFindEnabled) {
            elementFinder.findInputByLabel(instruction.target)
                ?: elementFinder.findForAction(instruction.target, ActionType.ENTER_TEXT)
        } else {
            elementFinder.find(instruction.target, timeoutMs, forTextEntry = true)
        }

        if (element == null) {
            return ExecutionResult(success = false, error = "Element not found: ${instruction.target}", elementFound = false)
        }

        // Click to focus
        tryClickElement(element)

        // Clear if needed
        if (instruction.clearFirst) {
            tryClearElement(element)
        }

        element.sendKeys(instruction.value)
        return ExecutionResult(success = true, elementFound = true)
    }

    private fun executeVerifyDisplayed(instruction: Instruction.VerifyDisplayed, timeoutMs: Long): ExecutionResult {
        val context = VerificationContext(timeoutMs = timeoutMs, screenshotOnFailure = config.screenshotOnFailure)
        val result = verificationEngine.verifyDisplayed(instruction.target, context)
        return ExecutionResult(
            success = result.passed,
            error = if (result.failed) result.message else null,
            elementFound = result.passed || result.metadata["elementExists"] == true
        )
    }

    private fun executeVerifyText(instruction: Instruction.VerifyText, timeoutMs: Long): ExecutionResult {
        val context = VerificationContext(timeoutMs = timeoutMs, screenshotOnFailure = config.screenshotOnFailure)

        // First try element-based text verification
        val elementResult = verificationEngine.verifyTextContains(instruction.target, instruction.expectedText, context)
        if (elementResult.passed) {
            return ExecutionResult(success = true, elementFound = true)
        }

        // Fallback: check if text is anywhere on screen
        val screenResult = verificationEngine.verifyTextOnScreen(instruction.expectedText)
        return ExecutionResult(
            success = screenResult.passed,
            error = if (screenResult.failed) "Text '${instruction.expectedText}' not found on screen" else null,
            elementFound = elementResult.metadata["elementFound"] as? Boolean
        )
    }

    private suspend fun executeWait(instruction: Instruction.Wait): ExecutionResult {
        delay(instruction.durationMs)
        return ExecutionResult(success = true)
    }

    private fun executeWaitForElement(instruction: Instruction.WaitForElement, timeoutMs: Long): ExecutionResult {
        val found = elementFinder.waitForElement(instruction.target, timeoutMs, instruction.shouldDisappear)

        return if (found) {
            ExecutionResult(success = true, elementFound = !instruction.shouldDisappear)
        } else {
            val action = if (instruction.shouldDisappear) "disappear" else "appear"
            ExecutionResult(success = false, error = "Element did not $action: ${instruction.target}", elementFound = instruction.shouldDisappear)
        }
    }

    private fun executeScroll(instruction: Instruction.Scroll): ExecutionResult {
        val success = gestureHandler.scroll(instruction.direction)
        if (!success) {
            return ExecutionResult(success = false, error = "Scroll not supported on this driver")
        }

        // If target specified, check if visible
        instruction.target?.let { target ->
            val element = elementFinder.find(target, 2000)
            if (element == null) {
                return ExecutionResult(success = false, error = "Target not found after scroll: $target")
            }
        }

        return ExecutionResult(success = true)
    }

    private fun executeSwipe(instruction: Instruction.Swipe): ExecutionResult {
        return executeScroll(Instruction.Scroll(instruction.direction, instruction.target))
    }

    private fun executeLongPress(instruction: Instruction.LongPress, timeoutMs: Long): ExecutionResult {
        val element = elementFinder.find(instruction.target, timeoutMs)
            ?: return ExecutionResult(success = false, error = "Element not found: ${instruction.target}", elementFound = false)

        val success = gestureHandler.longPress(element, instruction.durationMs)
        return if (success) {
            ExecutionResult(success = true, elementFound = true)
        } else {
            ExecutionResult(success = false, error = "Long press not supported on this driver", elementFound = true)
        }
    }

    private fun executeDoubleTap(instruction: Instruction.DoubleTap, timeoutMs: Long): ExecutionResult {
        val element = elementFinder.find(instruction.target, timeoutMs)
            ?: return ExecutionResult(success = false, error = "Element not found: ${instruction.target}", elementFound = false)

        val success = gestureHandler.doubleTap(element)
        return if (success) {
            ExecutionResult(success = true, elementFound = true)
        } else {
            ExecutionResult(success = false, error = "Double tap not supported on this driver", elementFound = true)
        }
    }

    private fun executeNavigateBack(): ExecutionResult {
        driver.navigate().back()
        return ExecutionResult(success = true)
    }

    private fun executeLaunchApp(): ExecutionResult {
        return when (driver) {
            is AndroidDriver -> ExecutionResult(success = true)
            is IOSDriver -> ExecutionResult(success = true)
            else -> ExecutionResult(success = true)
        }
    }

    private fun executeCloseApp(): ExecutionResult {
        return try {
            when (driver) {
                is AndroidDriver -> {
                    (driver as AndroidDriver).terminateApp(driver.capabilities.getCapability("appPackage") as? String ?: "")
                    ExecutionResult(success = true)
                }
                is IOSDriver -> {
                    (driver as IOSDriver).terminateApp(driver.capabilities.getCapability("bundleId") as? String ?: "")
                    ExecutionResult(success = true)
                }
                else -> {
                    driver.quit()
                    ExecutionResult(success = true)
                }
            }
        } catch (e: Exception) {
            ExecutionResult(success = false, error = "Failed to close app: ${e.message}")
        }
    }

    private fun executeTakeScreenshot(instruction: Instruction.TakeScreenshot): ExecutionResult {
        return try {
            val screenshotDriver = driver as? TakesScreenshot
                ?: return ExecutionResult(success = false, error = "Driver does not support screenshots")

            val screenshot = screenshotDriver.getScreenshotAs(OutputType.BASE64)
            logger.info("Screenshot captured${instruction.name?.let { ": $it" } ?: ""}")
            ExecutionResult(success = true)
        } catch (e: Exception) {
            ExecutionResult(success = false, error = "Failed to take screenshot: ${e.message}")
        }
    }

    private fun executeClearText(instruction: Instruction.ClearText, timeoutMs: Long): ExecutionResult {
        val element = elementFinder.find(instruction.target, timeoutMs, forTextEntry = true)
            ?: return ExecutionResult(success = false, error = "Element not found: ${instruction.target}", elementFound = false)

        return try {
            element.clear()
            ExecutionResult(success = true, elementFound = true)
        } catch (e: Exception) {
            ExecutionResult(success = false, error = "Failed to clear text: ${e.message}", elementFound = true)
        }
    }

    private fun executeVerifyNotDisplayed(instruction: Instruction.VerifyNotDisplayed, timeoutMs: Long): ExecutionResult {
        val context = VerificationContext(timeoutMs = timeoutMs, screenshotOnFailure = config.screenshotOnFailure)
        val result = verificationEngine.verifyNotDisplayed(instruction.target, context)
        return ExecutionResult(
            success = result.passed,
            error = if (result.failed) result.message else null,
            elementFound = !result.passed
        )
    }

    private fun executeVerifyEnabled(instruction: Instruction.VerifyEnabled, timeoutMs: Long): ExecutionResult {
        val context = VerificationContext(timeoutMs = timeoutMs, screenshotOnFailure = config.screenshotOnFailure)
        val result = verificationEngine.verifyEnabled(instruction.target, context)
        return ExecutionResult(
            success = result.passed,
            error = if (result.failed) result.message else null,
            elementFound = result.passed || result.errorDetails?.contains("not enabled") == true
        )
    }

    private fun executeVerifyDisabled(instruction: Instruction.VerifyDisabled, timeoutMs: Long): ExecutionResult {
        val context = VerificationContext(timeoutMs = timeoutMs, screenshotOnFailure = config.screenshotOnFailure)
        val result = verificationEngine.verifyDisabled(instruction.target, context)
        return ExecutionResult(
            success = result.passed,
            error = if (result.failed) result.message else null,
            elementFound = result.passed || result.errorDetails?.contains("is enabled") == true
        )
    }

    private fun executeNavigateForward(): ExecutionResult {
        return try {
            driver.navigate().forward()
            ExecutionResult(success = true)
        } catch (e: Exception) {
            ExecutionResult(success = false, error = "Forward navigation not supported: ${e.message}")
        }
    }

    private fun executeSelectOption(instruction: Instruction.SelectOption, timeoutMs: Long): ExecutionResult {
        // Try compound action resolution for complex patterns (date pickers, etc.)
        if (config.smartFindEnabled) {
            val compoundResult = elementFinder.findWithCompoundSupport(
                "${instruction.target} ${instruction.option}",
                ActionType.SELECT
            )

            when (compoundResult) {
                is FindResult.CompoundActions -> {
                    // Execute all actions in sequence
                    for (resolvedAction in compoundResult.actions) {
                        val element = resolvedAction.element?.element
                            ?: if (resolvedAction.waitForOption) {
                                // Wait for option to appear after dropdown opens
                                Thread.sleep(500)
                                elementFinder.findDropdownOption(resolvedAction.value ?: "")
                            } else null

                        if (element == null && resolvedAction.value != null) {
                            // Try to find option directly
                            elementFinder.findDropdownOption(resolvedAction.value)?.let { option ->
                                option.click()
                            }
                        } else {
                            element?.let { el ->
                                when (resolvedAction.action) {
                                    ActionType.CLICK -> el.click()
                                    ActionType.SELECT -> {
                                        el.click()
                                        Thread.sleep(300)
                                    }
                                    ActionType.ENTER_TEXT -> resolvedAction.value?.let { el.sendKeys(it) }
                                    else -> el.click()
                                }
                                Thread.sleep(300) // Brief delay between actions
                            }
                        }
                    }
                    return ExecutionResult(success = true, elementFound = true)
                }
                is FindResult.SingleElement -> {
                    // Single element - click it and then find option
                    compoundResult.element.click()
                    Thread.sleep(500)

                    val option = elementFinder.findDropdownOption(instruction.option, compoundResult.element)
                        ?: elementFinder.find(instruction.option, 5000)
                        ?: return ExecutionResult(success = false, error = "Option not found: ${instruction.option}", elementFound = true)

                    option.click()
                    return ExecutionResult(success = true, elementFound = true)
                }
                is FindResult.NotFound -> {
                    // Fall through to standard approach
                }
            }
        }

        // Standard approach: Find dropdown, click, find option
        val dropdown = elementFinder.find(instruction.target, timeoutMs)
            ?: return ExecutionResult(success = false, error = "Dropdown not found: ${instruction.target}", elementFound = false)

        dropdown.click()
        Thread.sleep(500) // Wait for dropdown to expand

        // Then find and click the option
        val option = elementFinder.findDropdownOption(instruction.option, dropdown)
            ?: elementFinder.find(instruction.option, 5000)
            ?: return ExecutionResult(success = false, error = "Option not found: ${instruction.option}", elementFound = true)

        option.click()
        return ExecutionResult(success = true, elementFound = true)
    }

    private fun executeCheckCheckbox(instruction: Instruction.CheckCheckbox, timeoutMs: Long): ExecutionResult {
        // Use smart finding with checkable element detection
        val element = if (config.smartFindEnabled) {
            elementFinder.findForAction(instruction.target, ActionType.CHECK)
        } else {
            elementFinder.find(instruction.target, timeoutMs)
        }

        if (element == null) {
            return ExecutionResult(success = false, error = "Checkbox not found: ${instruction.target}", elementFound = false)
        }

        // Only click if not already checked
        val isSelected = try {
            element.isSelected || element.getAttribute("checked") == "true"
        } catch (e: Exception) {
            false
        }

        if (!isSelected) {
            element.click()
        }

        return ExecutionResult(success = true, elementFound = true)
    }

    private fun executeUncheckCheckbox(instruction: Instruction.UncheckCheckbox, timeoutMs: Long): ExecutionResult {
        val element = elementFinder.find(instruction.target, timeoutMs)
            ?: return ExecutionResult(success = false, error = "Checkbox not found: ${instruction.target}", elementFound = false)

        // Only click if currently checked
        val isSelected = try {
            element.isSelected || element.getAttribute("checked") == "true"
        } catch (e: Exception) {
            false
        }

        if (isSelected) {
            element.click()
        }

        return ExecutionResult(success = true, elementFound = true)
    }

    private fun executeToggleSwitch(instruction: Instruction.ToggleSwitch, timeoutMs: Long): ExecutionResult {
        // Use smart finding with toggle element detection
        val element = if (config.smartFindEnabled) {
            elementFinder.findForAction(instruction.target, ActionType.TOGGLE)
        } else {
            elementFinder.find(instruction.target, timeoutMs)
        }

        if (element == null) {
            return ExecutionResult(success = false, error = "Switch not found: ${instruction.target}", elementFound = false)
        }

        val currentState = try {
            element.isSelected ||
                element.getAttribute("checked") == "true" ||
                element.getAttribute("value") == "1"
        } catch (e: Exception) {
            false
        }

        val shouldClick = when (instruction.targetState) {
            true -> !currentState  // Want on, click if currently off
            false -> currentState   // Want off, click if currently on
            null -> true            // Toggle always clicks
        }

        if (shouldClick) {
            element.click()
        }

        return ExecutionResult(success = true, elementFound = true)
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════

    private fun tryClickElement(element: WebElement) {
        try {
            element.click()
            Thread.sleep(200)
        } catch (e: Exception) {
            logger.debug("Click before typing failed: ${e.message}")
        }
    }

    private fun tryClearElement(element: WebElement) {
        try {
            element.clear()
        } catch (e: Exception) {
            logger.debug("Clear failed: ${e.message}")
        }
    }
}
