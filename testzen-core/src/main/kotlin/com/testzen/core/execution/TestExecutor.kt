package com.testzen.core.execution

import com.testzen.core.config.TestZenConfig
import com.testzen.core.locator.CacheStats
import com.testzen.core.locator.HealingEvent
import com.testzen.core.model.*
import io.appium.java_client.AppiumDriver
import org.openqa.selenium.OutputType
import org.openqa.selenium.WebDriver
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Executes test cases against a WebDriver/AppiumDriver.
 *
 * Supports self-healing locators when enabled in configuration.
 */
class TestExecutor(
    private val driver: WebDriver,
    private val config: TestZenConfig,
    private val platform: Platform = Platform.ANDROID
) {
    private val logger = LoggerFactory.getLogger(TestExecutor::class.java)
    private val instructionParser = InstructionParser()
    private val instructionExecutor = InstructionExecutor(driver, config, platform)

    /**
     * Execute a test case and return the result.
     */
    suspend fun execute(testCase: TestCase): TestResult {
        val startTime = System.currentTimeMillis()
        val startedAt = Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
        val stepResults = mutableListOf<StepResult>()
        var overallStatus = TestStatus.PASSED
        var errorMessage: String? = null
        var screenshot: String? = null

        logger.info("Starting test: ${testCase.name} (${testCase.testId})")

        for (step in testCase.steps) {
            val stepStartTime = System.currentTimeMillis()

            try {
                // Parse the natural language instruction
                val instruction = instructionParser.parse(step.instruction)

                // Execute the instruction
                val result = instructionExecutor.execute(instruction, step.timeout ?: (config.actionTimeout * 1000))

                val stepDuration = System.currentTimeMillis() - stepStartTime

                if (result.success) {
                    stepResults.add(StepResult(
                        order = step.order,
                        instruction = step.instruction,
                        status = StepStatus.PASSED,
                        durationMs = stepDuration,
                        elementFound = result.elementFound
                    ))
                    logger.debug("Step ${step.order} passed: ${step.instruction}")
                } else {
                    // Step failed
                    val stepStatus = if (step.optional) StepStatus.SKIPPED else StepStatus.FAILED

                    stepResults.add(StepResult(
                        order = step.order,
                        instruction = step.instruction,
                        status = stepStatus,
                        durationMs = stepDuration,
                        errorMessage = result.error,
                        elementFound = result.elementFound,
                        screenshot = if (config.screenshotOnFailure) captureScreenshot() else null
                    ))

                    if (!step.optional) {
                        overallStatus = TestStatus.FAILED
                        errorMessage = "Step ${step.order} failed: ${result.error}"
                        logger.error("Step ${step.order} failed: ${step.instruction} - ${result.error}")

                        // Capture screenshot on failure
                        if (config.screenshotOnFailure) {
                            screenshot = captureScreenshot()
                        }

                        // Skip remaining steps on failure
                        break
                    } else {
                        logger.warn("Optional step ${step.order} failed, continuing: ${result.error}")
                    }
                }

            } catch (e: Exception) {
                val stepDuration = System.currentTimeMillis() - stepStartTime

                stepResults.add(StepResult(
                    order = step.order,
                    instruction = step.instruction,
                    status = if (step.optional) StepStatus.SKIPPED else StepStatus.ERROR,
                    durationMs = stepDuration,
                    errorMessage = e.message,
                    screenshot = if (config.screenshotOnFailure) captureScreenshot() else null
                ))

                if (!step.optional) {
                    overallStatus = TestStatus.ERROR
                    errorMessage = "Step ${step.order} error: ${e.message}"
                    logger.error("Step ${step.order} error: ${e.message}", e)

                    if (config.screenshotOnFailure) {
                        screenshot = captureScreenshot()
                    }
                    break
                }
            }
        }

        // Mark remaining steps as skipped
        val executedSteps = stepResults.map { it.order }.toSet()
        for (step in testCase.steps) {
            if (step.order !in executedSteps) {
                stepResults.add(StepResult(
                    order = step.order,
                    instruction = step.instruction,
                    status = StepStatus.SKIPPED,
                    durationMs = 0,
                    errorMessage = "Skipped due to previous failure"
                ))
            }
        }

        val completedAt = Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
        val totalDuration = System.currentTimeMillis() - startTime

        return TestResult(
            testId = testCase.testId,
            testName = testCase.name,
            status = overallStatus,
            durationMs = totalDuration,
            stepResults = stepResults.sortedBy { it.order },
            errorMessage = errorMessage,
            screenshot = screenshot,
            startedAt = startedAt,
            completedAt = completedAt
        )
    }

    private fun captureScreenshot(): String? {
        return try {
            when (driver) {
                is AppiumDriver -> driver.getScreenshotAs(OutputType.BASE64)
                else -> (driver as? org.openqa.selenium.TakesScreenshot)?.getScreenshotAs(OutputType.BASE64)
            }
        } catch (e: Exception) {
            logger.warn("Failed to capture screenshot: ${e.message}")
            null
        }
    }

    /**
     * Get the self-healing report for this session.
     * Returns null if self-healing is not enabled.
     */
    fun getHealingReport(): List<HealingEvent>? = instructionExecutor.getHealingReport()

    /**
     * Get cache statistics for self-healing locators.
     * Returns null if self-healing is not enabled.
     */
    fun getCacheStats(): CacheStats? = instructionExecutor.getCacheStats()
}
