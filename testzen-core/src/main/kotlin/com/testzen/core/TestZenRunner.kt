package com.testzen.core

import com.testzen.core.config.TestZenConfig
import com.testzen.core.execution.TestExecutor
import com.testzen.core.locator.CacheStats
import com.testzen.core.locator.HealingEvent
import com.testzen.core.model.*
import com.testzen.core.platform.PlatformDriverFactory
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.File

/**
 * TestZen Runner - Main entry point for the lightweight test automation framework.
 *
 * Usage:
 * ```kotlin
 * val runner = TestZenRunner()
 * runner.loadTests("./tests")
 * val results = runner.execute(Platform.ANDROID, deviceId = "emulator-5554")
 * ```
 *
 * Or with configuration:
 * ```kotlin
 * val config = TestZenConfig(
 *     implicitWait = 20,
 *     explicitWait = 30,
 *     retryFailedSteps = 2
 * )
 * val runner = TestZenRunner(config)
 * ```
 */
class TestZenRunner(
    private val config: TestZenConfig = TestZenConfig()
) {
    private val logger = LoggerFactory.getLogger(TestZenRunner::class.java)
    private val testLoader = TestLoader()
    private val loadedTests = mutableListOf<TestCase>()

    // Store last execution's healing report
    private var lastHealingReport: List<HealingEvent>? = null
    private var lastCacheStats: CacheStats? = null

    /**
     * Load tests from a directory or single file.
     *
     * @param path Path to a YAML test file or directory containing test files
     * @return Number of tests loaded
     */
    fun loadTests(path: String): Int {
        val file = File(path)
        require(file.exists()) { "Test path does not exist: $path" }

        loadedTests.clear()

        if (file.isDirectory) {
            file.walkTopDown()
                .filter { it.extension in listOf("yaml", "yml") }
                .forEach { testFile ->
                    try {
                        val tests = testLoader.loadFromFile(testFile)
                        loadedTests.addAll(tests)
                        logger.info("Loaded ${tests.size} test(s) from ${testFile.name}")
                    } catch (e: Exception) {
                        logger.warn("Failed to load tests from ${testFile.name}: ${e.message}")
                    }
                }
        } else {
            val tests = testLoader.loadFromFile(file)
            loadedTests.addAll(tests)
            logger.info("Loaded ${tests.size} test(s) from ${file.name}")
        }

        logger.info("Total tests loaded: ${loadedTests.size}")
        return loadedTests.size
    }

    /**
     * Load a single test from YAML content.
     *
     * @param yamlContent YAML test definition as a string
     * @return The loaded TestCase
     */
    fun loadTestFromYaml(yamlContent: String): TestCase {
        val test = testLoader.loadFromYaml(yamlContent)
        loadedTests.add(test)
        return test
    }

    /**
     * Execute all loaded tests.
     *
     * @param platform Target platform (ANDROID, IOS, WEB)
     * @param deviceId Optional device/emulator ID
     * @param appiumUrl Appium server URL (default: http://127.0.0.1:4723)
     * @return List of test results
     */
    fun execute(
        platform: Platform,
        deviceId: String? = null,
        appiumUrl: String = "http://127.0.0.1:4723"
    ): List<TestResult> = runBlocking {
        executeAsync(platform, deviceId, appiumUrl)
    }

    /**
     * Execute all loaded tests asynchronously.
     */
    suspend fun executeAsync(
        platform: Platform,
        deviceId: String? = null,
        appiumUrl: String = "http://127.0.0.1:4723"
    ): List<TestResult> {
        require(loadedTests.isNotEmpty()) { "No tests loaded. Call loadTests() first." }

        logger.info("Executing ${loadedTests.size} test(s) on $platform")

        val results = mutableListOf<TestResult>()
        val driverFactory = PlatformDriverFactory(appiumUrl, config)

        try {
            val driver = driverFactory.createDriver(platform, deviceId)
            val executor = TestExecutor(driver, config, platform)

            for (test in loadedTests) {
                logger.info("Running test: ${test.name}")
                val result = executor.execute(test)
                results.add(result)

                when (result.status) {
                    TestStatus.PASSED -> logger.info("✓ PASSED: ${test.name}")
                    TestStatus.FAILED -> logger.error("✗ FAILED: ${test.name} - ${result.errorMessage}")
                    TestStatus.SKIPPED -> logger.warn("○ SKIPPED: ${test.name}")
                    TestStatus.ERROR -> logger.error("⚠ ERROR: ${test.name} - ${result.errorMessage}")
                }
            }

            // Capture healing report before closing driver
            lastHealingReport = executor.getHealingReport()
            lastCacheStats = executor.getCacheStats()

            // Log healing summary if any healing occurred
            lastHealingReport?.let { report ->
                val healed = report.count { it.success && it.healedLocator != null }
                if (healed > 0) {
                    logger.info("Self-healing: $healed element(s) were healed during execution")
                }
            }

            driver.quit()
        } catch (e: Exception) {
            logger.error("Execution failed: ${e.message}", e)
            // Mark remaining tests as error
            loadedTests.filter { test -> results.none { it.testId == test.testId } }
                .forEach { test ->
                    results.add(TestResult(
                        testId = test.testId,
                        testName = test.name,
                        status = TestStatus.ERROR,
                        errorMessage = "Execution aborted: ${e.message}",
                        durationMs = 0,
                        stepResults = emptyList()
                    ))
                }
        }

        // Log summary
        val passed = results.count { it.status == TestStatus.PASSED }
        val failed = results.count { it.status == TestStatus.FAILED }
        val skipped = results.count { it.status == TestStatus.SKIPPED }
        val errors = results.count { it.status == TestStatus.ERROR }

        logger.info("═══════════════════════════════════════════════════")
        logger.info("Execution Summary: $passed passed, $failed failed, $skipped skipped, $errors errors")
        logger.info("═══════════════════════════════════════════════════")

        return results
    }

    /**
     * Execute a single test by ID.
     */
    fun executeTest(
        testId: String,
        platform: Platform,
        deviceId: String? = null,
        appiumUrl: String = "http://127.0.0.1:4723"
    ): TestResult = runBlocking {
        val test = loadedTests.find { it.testId == testId }
            ?: throw IllegalArgumentException("Test not found: $testId")

        val driverFactory = PlatformDriverFactory(appiumUrl, config)
        val driver = driverFactory.createDriver(platform, deviceId)
        val executor = TestExecutor(driver, config, platform)

        try {
            val result = executor.execute(test)
            lastHealingReport = executor.getHealingReport()
            lastCacheStats = executor.getCacheStats()
            result
        } finally {
            driver.quit()
        }
    }

    /**
     * Get list of loaded tests.
     */
    fun getLoadedTests(): List<TestCase> = loadedTests.toList()

    /**
     * Clear all loaded tests.
     */
    fun clearTests() {
        loadedTests.clear()
    }

    /**
     * Get framework version.
     */
    fun getVersion(): String = "1.0.0"

    /**
     * Get the self-healing report from the last execution.
     * Returns null if self-healing was not enabled or no execution occurred.
     */
    fun getLastHealingReport(): List<HealingEvent>? = lastHealingReport

    /**
     * Get cache statistics from the last execution.
     * Returns null if self-healing was not enabled or no execution occurred.
     */
    fun getLastCacheStats(): CacheStats? = lastCacheStats

    companion object {
        /**
         * Quick execution helper for running tests from a path.
         */
        fun run(
            testsPath: String,
            platform: Platform,
            deviceId: String? = null,
            config: TestZenConfig = TestZenConfig()
        ): List<TestResult> {
            val runner = TestZenRunner(config)
            runner.loadTests(testsPath)
            return runner.execute(platform, deviceId)
        }
    }
}
