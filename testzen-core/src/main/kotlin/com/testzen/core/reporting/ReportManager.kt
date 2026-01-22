package com.testzen.core.reporting

import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Central manager for test execution reporting.
 *
 * Responsibilities:
 * - Collect test results during execution
 * - Capture screenshots (before/after steps, on failure)
 * - Build hierarchical result structure
 * - Generate reports in multiple formats
 *
 * Usage:
 * ```kotlin
 * val reportManager = ReportManager(config)
 *
 * // Start execution
 * reportManager.startExecution("Regression Suite", "staging")
 *
 * // Start a test case
 * val testContext = reportManager.startTestCase(
 *     testId = "TC001",
 *     name = "Login Test",
 *     moduleId = "auth",
 *     featureId = "login",
 *     storyId = "US-123"
 * )
 *
 * // Record steps
 * testContext.startStep(1, "Click Login button")
 * testContext.captureScreenshot(driver, ScreenshotType.BEFORE_STEP)
 * // ... execute step ...
 * testContext.captureScreenshot(driver, ScreenshotType.AFTER_STEP)
 * testContext.endStep(TestStatus.PASSED)
 *
 * // End test case
 * testContext.end(TestStatus.PASSED)
 *
 * // Generate reports
 * val report = reportManager.endExecution()
 * reportManager.generateHtmlReport("./reports/report.html")
 * reportManager.generateJsonReport("./reports/report.json")
 * ```
 */
class ReportManager(
    private val config: ReportConfig = ReportConfig()
) {
    private val logger = LoggerFactory.getLogger(ReportManager::class.java)

    // Current execution state
    private var executionId: String? = null
    private var executionName: String = ""
    private var environment: String = "default"
    private var executionStartTime: Instant? = null
    private var buildInfo: BuildInfo? = null
    private var executionConfig: ExecutionConfig? = null

    // Result collection
    private val moduleResults = ConcurrentHashMap<String, ModuleResultBuilder>()
    private val directTestCases = mutableListOf<TestCaseResult>()

    // Current report
    private var currentReport: TestExecutionReport? = null

    // Generators
    private val htmlGenerator = HtmlReportGenerator(config.htmlConfig)
    private val jsonGenerator = JsonReportGenerator(config.jsonConfig)

    // ═══════════════════════════════════════════════════════════════
    // EXECUTION LIFECYCLE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Start a new test execution.
     */
    fun startExecution(
        name: String,
        environment: String = "default",
        buildInfo: BuildInfo? = null,
        executionConfig: ExecutionConfig? = null
    ): String {
        this.executionId = UUID.randomUUID().toString()
        this.executionName = name
        this.environment = environment
        this.executionStartTime = Instant.now()
        this.buildInfo = buildInfo
        this.executionConfig = executionConfig

        // Clear previous results
        moduleResults.clear()
        directTestCases.clear()
        currentReport = null

        logger.info("Started test execution: $name (ID: $executionId)")
        return executionId!!
    }

    /**
     * End test execution and build report.
     */
    fun endExecution(): TestExecutionReport {
        val endTime = Instant.now()

        // Build module results
        val modules = moduleResults.values.map { it.build() }

        currentReport = TestExecutionReport(
            id = executionId ?: UUID.randomUUID().toString(),
            name = executionName,
            environment = environment,
            buildInfo = buildInfo,
            executionConfig = executionConfig,
            modules = modules,
            directTestCases = directTestCases.toList(),
            startTime = executionStartTime ?: endTime,
            endTime = endTime,
            generatedAt = Instant.now()
        )

        logger.info("Test execution completed: ${currentReport!!.totalTests} tests, ${currentReport!!.passedTests} passed, ${currentReport!!.failedTests} failed")
        return currentReport!!
    }

    /**
     * Get current report (or build if not yet built).
     */
    fun getReport(): TestExecutionReport {
        return currentReport ?: endExecution()
    }

    // ═══════════════════════════════════════════════════════════════
    // TEST CASE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Start a new test case and return its context.
     */
    fun startTestCase(
        testId: String,
        name: String,
        description: String? = null,
        moduleId: String? = null,
        moduleName: String? = null,
        featureId: String? = null,
        featureName: String? = null,
        storyId: String? = null,
        storyName: String? = null,
        tags: Set<String> = emptySet(),
        platform: TestPlatform = TestPlatform.UNKNOWN,
        deviceInfo: String? = null
    ): TestCaseContext {
        val context = TestCaseContext(
            testId = testId,
            name = name,
            description = description,
            tags = tags,
            platform = platform,
            deviceInfo = deviceInfo,
            screenshotDir = config.screenshotDirectory,
            captureScreenshots = config.captureScreenshots,
            moduleId = moduleId,
            featureId = featureId,
            storyId = storyId
        )

        // Register with hierarchy
        if (moduleId != null) {
            val moduleBuilder = moduleResults.getOrPut(moduleId) {
                ModuleResultBuilder(moduleId, moduleName ?: moduleId)
            }

            if (featureId != null) {
                val featureBuilder = moduleBuilder.getOrCreateFeature(featureId, featureName ?: featureId)

                if (storyId != null) {
                    featureBuilder.getOrCreateStory(storyId, storyName ?: storyId)
                        .addTestContext(context)
                } else {
                    featureBuilder.addDirectTestContext(context)
                }
            } else {
                moduleBuilder.addDirectTestContext(context)
            }
        } else {
            // Direct test case (no module)
            context.onComplete = { result -> directTestCases.add(result) }
        }

        logger.debug("Started test case: $name ($testId)")
        return context
    }

    // ═══════════════════════════════════════════════════════════════
    // REPORT GENERATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Generate HTML report.
     */
    fun generateHtmlReport(outputPath: String = "${config.outputDirectory}/report.html"): File {
        val report = getReport()
        val file = htmlGenerator.generate(report, outputPath)
        logger.info("Generated HTML report: ${file.absolutePath}")
        return file
    }

    /**
     * Generate full JSON report.
     */
    fun generateJsonReport(outputPath: String = "${config.outputDirectory}/report.json"): File {
        val report = getReport()
        val file = jsonGenerator.generate(report, outputPath)
        logger.info("Generated JSON report: ${file.absolutePath}")
        return file
    }

    /**
     * Generate summary JSON report.
     */
    fun generateSummaryReport(outputPath: String = "${config.outputDirectory}/summary.json"): File {
        val report = getReport()
        val file = jsonGenerator.generateSummary(report, outputPath)
        logger.info("Generated summary report: ${file.absolutePath}")
        return file
    }

    /**
     * Generate JUnit XML report (for CI/CD).
     */
    fun generateJunitReport(outputPath: String = "${config.outputDirectory}/junit.xml"): File {
        val report = getReport()
        val file = jsonGenerator.generateJunitXml(report, outputPath)
        logger.info("Generated JUnit XML report: ${file.absolutePath}")
        return file
    }

    /**
     * Generate all report formats.
     */
    fun generateAllReports(outputDir: String = config.outputDirectory): Map<String, File> {
        return mapOf(
            "html" to generateHtmlReport("$outputDir/report.html"),
            "json" to generateJsonReport("$outputDir/report.json"),
            "summary" to generateSummaryReport("$outputDir/summary.json"),
            "junit" to generateJunitReport("$outputDir/junit.xml")
        )
    }
}

/**
 * Context for building a test case result during execution.
 */
class TestCaseContext(
    val testId: String,
    val name: String,
    val description: String?,
    val tags: Set<String>,
    val platform: TestPlatform,
    val deviceInfo: String?,
    private val screenshotDir: String,
    private val captureScreenshots: Boolean,
    val moduleId: String?,
    val featureId: String?,
    val storyId: String?
) {
    private val id = UUID.randomUUID().toString()
    private val startTime = Instant.now()
    private val steps = mutableListOf<StepResult>()
    private var currentStep: StepResultBuilder? = null
    private var status: TestStatus = TestStatus.RUNNING
    private var errorMessage: String? = null

    internal var onComplete: ((TestCaseResult) -> Unit)? = null

    /**
     * Start a new step.
     */
    fun startStep(stepNumber: Int, instruction: String, intent: String? = null, target: String? = null, value: String? = null) {
        // End previous step if not ended
        currentStep?.let { endStep(TestStatus.PASSED) }

        currentStep = StepResultBuilder(
            stepNumber = stepNumber,
            instruction = instruction,
            intent = intent,
            target = target,
            value = value
        )
    }

    /**
     * Capture screenshot for current step.
     */
    fun captureScreenshot(driver: WebDriver, type: ScreenshotType): Screenshot? {
        if (!captureScreenshots) return null
        if (driver !is TakesScreenshot) return null

        return try {
            val screenshot = driver.getScreenshotAs(OutputType.BYTES)
            val fileName = "${testId}_step${currentStep?.stepNumber ?: 0}_${type.name.lowercase()}_${System.currentTimeMillis()}.png"
            val filePath = "$screenshotDir/$fileName"

            File(screenshotDir).mkdirs()
            File(filePath).writeBytes(screenshot)

            val screenshotObj = Screenshot(
                type = type,
                filePath = filePath,
                fileName = fileName,
                fileSize = screenshot.size.toLong()
            )

            // Attach to current step
            currentStep?.let { step ->
                when (type) {
                    ScreenshotType.BEFORE_STEP -> step.screenshotBefore = screenshotObj
                    ScreenshotType.AFTER_STEP -> step.screenshotAfter = screenshotObj
                    else -> step.additionalScreenshots.add(screenshotObj)
                }
            }

            screenshotObj
        } catch (e: Exception) {
            null
        }
    }

    /**
     * End current step with status.
     */
    fun endStep(
        status: TestStatus,
        errorMessage: String? = null,
        actualValue: String? = null,
        expectedValue: String? = null,
        locatorUsed: String? = null,
        wasHealed: Boolean = false,
        originalLocator: String? = null
    ) {
        currentStep?.let { step ->
            step.status = status
            step.errorMessage = errorMessage
            step.actualValue = actualValue
            step.expectedValue = expectedValue
            step.locatorUsed = locatorUsed
            step.wasHealed = wasHealed
            step.originalLocator = originalLocator
            steps.add(step.build())
            currentStep = null
        }
    }

    /**
     * Record a step failure.
     */
    fun failStep(errorMessage: String, actualValue: String? = null, expectedValue: String? = null) {
        endStep(TestStatus.FAILED, errorMessage, actualValue, expectedValue)
        if (this.errorMessage == null) {
            this.errorMessage = errorMessage
        }
    }

    /**
     * End the test case.
     */
    fun end(status: TestStatus = TestStatus.PASSED, errorMessage: String? = null): TestCaseResult {
        // End any current step
        currentStep?.let { endStep(if (status == TestStatus.PASSED) TestStatus.PASSED else TestStatus.SKIPPED) }

        this.status = status
        if (errorMessage != null) {
            this.errorMessage = errorMessage
        }

        val result = build()
        onComplete?.invoke(result)
        return result
    }

    /**
     * Build the test case result.
     */
    internal fun build(): TestCaseResult {
        val endTime = Instant.now()
        val failingStep = steps.firstOrNull { it.failed }

        // Determine final status
        val finalStatus = when {
            status == TestStatus.FAILED || status == TestStatus.ERROR -> status
            steps.any { it.status == TestStatus.ERROR } -> TestStatus.ERROR
            steps.any { it.failed } -> TestStatus.FAILED
            steps.all { it.passed } -> TestStatus.PASSED
            else -> status
        }

        return TestCaseResult(
            id = id,
            testId = testId,
            name = name,
            description = description,
            tags = tags,
            status = finalStatus,
            startTime = startTime,
            endTime = endTime,
            steps = steps.toList(),
            platform = platform,
            deviceInfo = deviceInfo,
            errorMessage = errorMessage ?: failingStep?.errorMessage,
            failingStep = failingStep,
            moduleId = moduleId,
            featureId = featureId,
            storyId = storyId
        )
    }
}

/**
 * Builder for step results.
 */
internal class StepResultBuilder(
    val stepNumber: Int,
    val instruction: String,
    val intent: String?,
    val target: String?,
    val value: String?
) {
    private val startTime = Instant.now()
    var status: TestStatus = TestStatus.RUNNING
    var errorMessage: String? = null
    var actualValue: String? = null
    var expectedValue: String? = null
    var locatorUsed: String? = null
    var wasHealed: Boolean = false
    var originalLocator: String? = null
    var screenshotBefore: Screenshot? = null
    var screenshotAfter: Screenshot? = null
    val additionalScreenshots = mutableListOf<Screenshot>()

    fun build(): StepResult {
        val endTime = Instant.now()
        return StepResult(
            stepNumber = stepNumber,
            instruction = instruction,
            intent = intent,
            target = target,
            value = value,
            status = status,
            startTime = startTime,
            endTime = endTime,
            screenshotBefore = screenshotBefore,
            screenshotAfter = screenshotAfter,
            additionalScreenshots = additionalScreenshots.toList(),
            errorMessage = errorMessage,
            actualValue = actualValue,
            expectedValue = expectedValue,
            locatorUsed = locatorUsed,
            wasHealed = wasHealed,
            originalLocator = originalLocator
        )
    }
}

/**
 * Builder for module results.
 */
internal class ModuleResultBuilder(
    private val moduleId: String,
    private val name: String
) {
    private val features = mutableMapOf<String, FeatureResultBuilder>()
    private val directTestContexts = mutableListOf<TestCaseContext>()

    fun getOrCreateFeature(featureId: String, name: String): FeatureResultBuilder {
        return features.getOrPut(featureId) { FeatureResultBuilder(featureId, name) }
    }

    fun addDirectTestContext(context: TestCaseContext) {
        context.onComplete = { result ->
            // Store result when test completes
        }
        directTestContexts.add(context)
    }

    fun build(): ModuleResult {
        return ModuleResult(
            moduleId = moduleId,
            name = name,
            features = features.values.map { it.build() },
            directTestCases = directTestContexts.map { it.build() }
        )
    }
}

/**
 * Builder for feature results.
 */
internal class FeatureResultBuilder(
    private val featureId: String,
    private val name: String
) {
    private val stories = mutableMapOf<String, StoryResultBuilder>()
    private val directTestContexts = mutableListOf<TestCaseContext>()

    fun getOrCreateStory(storyId: String, name: String): StoryResultBuilder {
        return stories.getOrPut(storyId) { StoryResultBuilder(storyId, name) }
    }

    fun addDirectTestContext(context: TestCaseContext) {
        directTestContexts.add(context)
    }

    fun build(): FeatureResult {
        return FeatureResult(
            featureId = featureId,
            name = name,
            stories = stories.values.map { it.build() },
            directTestCases = directTestContexts.map { it.build() }
        )
    }
}

/**
 * Builder for story results.
 */
internal class StoryResultBuilder(
    private val storyId: String,
    private val name: String
) {
    private val testContexts = mutableListOf<TestCaseContext>()

    fun addTestContext(context: TestCaseContext) {
        testContexts.add(context)
    }

    fun build(): StoryResult {
        return StoryResult(
            storyId = storyId,
            name = name,
            testCases = testContexts.map { it.build() }
        )
    }
}

/**
 * Configuration for report manager.
 */
data class ReportConfig(
    /** Output directory for reports */
    val outputDirectory: String = "./test-reports",

    /** Screenshot directory */
    val screenshotDirectory: String = "./test-reports/screenshots",

    /** Capture screenshots */
    val captureScreenshots: Boolean = true,

    /** Capture screenshot before each step */
    val screenshotBeforeStep: Boolean = true,

    /** Capture screenshot after each step */
    val screenshotAfterStep: Boolean = true,

    /** Capture screenshot on failure */
    val screenshotOnFailure: Boolean = true,

    /** HTML report configuration */
    val htmlConfig: HtmlReportConfig = HtmlReportConfig(),

    /** JSON report configuration */
    val jsonConfig: JsonReportConfig = JsonReportConfig()
)
