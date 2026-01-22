package com.testzen.core.reporting

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Complete test execution report with enterprise-grade aggregations.
 *
 * Provides:
 * - Overall execution summary
 * - Module-level breakdown
 * - Feature-level breakdown
 * - Story-level breakdown
 * - Individual test case details
 * - Step-by-step execution with screenshots
 * - Failure analysis
 * - Trend data
 */
data class TestExecutionReport(
    /** Unique report identifier */
    val id: String = UUID.randomUUID().toString(),

    /** Report name/title */
    val name: String,

    /** Report description */
    val description: String? = null,

    /** Execution environment */
    val environment: String = "default",

    /** Build/version information */
    val buildInfo: BuildInfo? = null,

    /** Execution configuration */
    val executionConfig: ExecutionConfig? = null,

    /** Modules with results */
    val modules: List<ModuleResult> = emptyList(),

    /** Direct test cases (not in any module) */
    val directTestCases: List<TestCaseResult> = emptyList(),

    /** Start time of execution */
    val startTime: Instant = calculateStartTime(modules, directTestCases),

    /** End time of execution */
    val endTime: Instant = calculateEndTime(modules, directTestCases),

    /** Total duration in milliseconds */
    val durationMs: Long = Duration.between(startTime, endTime).toMillis(),

    /** Report generation timestamp */
    val generatedAt: Instant = Instant.now(),

    /** Overall status */
    val status: TestStatus = calculateStatus(modules, directTestCases),

    /** Tags/labels for filtering */
    val tags: Set<String> = emptySet(),

    /** Additional metadata */
    val metadata: Map<String, Any> = emptyMap()
) {
    // ═══════════════════════════════════════════════════════════════
    // AGGREGATED STATISTICS
    // ═══════════════════════════════════════════════════════════════

    /** All test cases from all modules + direct */
    val allTestCases: List<TestCaseResult>
        get() = modules.flatMap { it.allTestCases } + directTestCases

    /** All stories from all modules */
    val allStories: List<StoryResult>
        get() = modules.flatMap { it.allStories }

    /** All features from all modules */
    val allFeatures: List<FeatureResult>
        get() = modules.flatMap { it.features }

    /** All steps from all test cases */
    val allSteps: List<StepResult>
        get() = allTestCases.flatMap { it.steps }

    // Test counts
    val totalTests: Int get() = allTestCases.size
    val passedTests: Int get() = allTestCases.count { it.passed }
    val failedTests: Int get() = allTestCases.count { it.failed }
    val skippedTests: Int get() = allTestCases.count { it.status == TestStatus.SKIPPED }
    val blockedTests: Int get() = allTestCases.count { it.status == TestStatus.BLOCKED }
    val errorTests: Int get() = allTestCases.count { it.status == TestStatus.ERROR }

    // Step counts
    val totalSteps: Int get() = allSteps.size
    val passedSteps: Int get() = allSteps.count { it.passed }
    val failedSteps: Int get() = allSteps.count { it.failed }

    // Module counts
    val totalModules: Int get() = modules.size
    val passedModules: Int get() = modules.count { it.status == TestStatus.PASSED }
    val failedModules: Int get() = modules.count { it.failed }

    // Feature counts
    val totalFeatures: Int get() = allFeatures.size
    val passedFeatures: Int get() = allFeatures.count { it.status == TestStatus.PASSED }

    // Story counts
    val totalStories: Int get() = allStories.size
    val passedStories: Int get() = allStories.count { it.status == TestStatus.PASSED }

    // Pass rates
    val testPassRate: Double
        get() = if (totalTests > 0) passedTests.toDouble() / totalTests else 0.0

    val testPassRatePercent: Double get() = testPassRate * 100

    val stepPassRate: Double
        get() = if (totalSteps > 0) passedSteps.toDouble() / totalSteps else 0.0

    val stepPassRatePercent: Double get() = stepPassRate * 100

    val modulePassRate: Double
        get() = if (totalModules > 0) passedModules.toDouble() / totalModules else 0.0

    val featurePassRate: Double
        get() = if (totalFeatures > 0) passedFeatures.toDouble() / totalFeatures else 0.0

    val storyPassRate: Double
        get() = if (totalStories > 0) passedStories.toDouble() / totalStories else 0.0

    // Duration formatting
    val formattedDuration: String
        get() {
            val duration = Duration.ofMillis(durationMs)
            val hours = duration.toHours()
            val minutes = duration.toMinutesPart()
            val seconds = duration.toSecondsPart()
            return when {
                hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
                minutes > 0 -> "${minutes}m ${seconds}s"
                else -> "${seconds}s"
            }
        }

    // Date/time formatting
    val formattedStartTime: String
        get() = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
            .format(startTime)

    val formattedEndTime: String
        get() = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
            .format(endTime)

    // ═══════════════════════════════════════════════════════════════
    // FAILURE ANALYSIS
    // ═══════════════════════════════════════════════════════════════

    /** All failed test cases */
    val failedTestCases: List<TestCaseResult>
        get() = allTestCases.filter { it.failed }

    /** All failed steps */
    val failedStepsList: List<StepResult>
        get() = allSteps.filter { it.failed }

    /** Failure summary by module */
    val failuresByModule: Map<String, Int>
        get() = modules.associate { it.name to it.failedTests }

    /** Failure summary by feature */
    val failuresByFeature: Map<String, Int>
        get() = allFeatures.associate { it.name to it.failedTests }

    /** Top failure reasons */
    val topFailureReasons: List<FailureReason>
        get() {
            return failedStepsList
                .groupBy { it.errorMessage ?: "Unknown error" }
                .map { (message, steps) ->
                    FailureReason(
                        message = message.take(200),
                        count = steps.size,
                        affectedTests = steps.mapNotNull { step ->
                            allTestCases.find { it.steps.contains(step) }?.testId
                        }.distinct()
                    )
                }
                .sortedByDescending { it.count }
                .take(10)
        }

    // ═══════════════════════════════════════════════════════════════
    // PLATFORM BREAKDOWN
    // ═══════════════════════════════════════════════════════════════

    /** Tests by platform */
    val testsByPlatform: Map<TestPlatform, List<TestCaseResult>>
        get() = allTestCases.groupBy { it.platform }

    /** Pass rate by platform */
    val passRateByPlatform: Map<TestPlatform, Double>
        get() = testsByPlatform.mapValues { (_, tests) ->
            if (tests.isNotEmpty()) tests.count { it.passed }.toDouble() / tests.size else 0.0
        }

    // ═══════════════════════════════════════════════════════════════
    // SUMMARY DATA
    // ═══════════════════════════════════════════════════════════════

    /** Get executive summary */
    fun getExecutiveSummary(): ExecutiveSummary {
        return ExecutiveSummary(
            totalTests = totalTests,
            passedTests = passedTests,
            failedTests = failedTests,
            skippedTests = skippedTests,
            passRate = testPassRatePercent,
            totalModules = totalModules,
            passedModules = passedModules,
            totalFeatures = totalFeatures,
            passedFeatures = passedFeatures,
            totalStories = totalStories,
            passedStories = passedStories,
            duration = formattedDuration,
            startTime = formattedStartTime,
            endTime = formattedEndTime,
            environment = environment,
            status = status
        )
    }

    /** Get module summary */
    fun getModuleSummary(): List<ModuleSummary> {
        return modules.map { module ->
            ModuleSummary(
                moduleId = module.moduleId,
                name = module.name,
                totalTests = module.totalTests,
                passedTests = module.passedTests,
                failedTests = module.failedTests,
                skippedTests = module.skippedTests,
                passRate = module.passRatePercent,
                totalFeatures = module.totalFeatures,
                passedFeatures = module.passedFeatures,
                totalStories = module.totalStories,
                passedStories = module.passedStories,
                duration = formatDuration(module.durationMs),
                status = module.status
            )
        }
    }

    private fun formatDuration(ms: Long): String {
        val duration = Duration.ofMillis(ms)
        val minutes = duration.toMinutes()
        val seconds = duration.toSecondsPart()
        return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
    }

    companion object {
        private fun calculateStartTime(modules: List<ModuleResult>, direct: List<TestCaseResult>): Instant {
            val moduleStart = modules.minOfOrNull { it.startTime }
            val directStart = direct.minOfOrNull { it.startTime }
            return listOfNotNull(moduleStart, directStart).minOrNull() ?: Instant.now()
        }

        private fun calculateEndTime(modules: List<ModuleResult>, direct: List<TestCaseResult>): Instant {
            val moduleEnd = modules.maxOfOrNull { it.endTime }
            val directEnd = direct.maxOfOrNull { it.endTime }
            return listOfNotNull(moduleEnd, directEnd).maxOrNull() ?: Instant.now()
        }

        private fun calculateStatus(modules: List<ModuleResult>, direct: List<TestCaseResult>): TestStatus {
            val allTests = modules.flatMap { it.allTestCases } + direct
            return when {
                allTests.isEmpty() -> TestStatus.PENDING
                allTests.all { it.passed } -> TestStatus.PASSED
                allTests.any { it.status == TestStatus.ERROR } -> TestStatus.ERROR
                allTests.any { it.failed } -> TestStatus.FAILED
                allTests.any { it.status == TestStatus.BLOCKED } -> TestStatus.BLOCKED
                allTests.all { it.status == TestStatus.SKIPPED } -> TestStatus.SKIPPED
                else -> TestStatus.PASSED
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SUPPORTING MODELS
// ═══════════════════════════════════════════════════════════════

/**
 * Build information.
 */
data class BuildInfo(
    val buildNumber: String? = null,
    val buildUrl: String? = null,
    val branch: String? = null,
    val commit: String? = null,
    val commitMessage: String? = null,
    val author: String? = null,
    val triggeredBy: String? = null,
    val pipelineId: String? = null,
    val jobName: String? = null
)

/**
 * Execution configuration.
 */
data class ExecutionConfig(
    val parallel: Boolean = false,
    val maxParallel: Int = 1,
    val retryCount: Int = 0,
    val timeout: Long = 0,
    val browser: String? = null,
    val device: String? = null,
    val platform: String? = null,
    val headless: Boolean = false,
    val tags: Set<String> = emptySet(),
    val excludeTags: Set<String> = emptySet()
)

/**
 * Executive summary for quick overview.
 */
data class ExecutiveSummary(
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val skippedTests: Int,
    val passRate: Double,
    val totalModules: Int,
    val passedModules: Int,
    val totalFeatures: Int,
    val passedFeatures: Int,
    val totalStories: Int,
    val passedStories: Int,
    val duration: String,
    val startTime: String,
    val endTime: String,
    val environment: String,
    val status: TestStatus
)

/**
 * Module summary for dashboard.
 */
data class ModuleSummary(
    val moduleId: String,
    val name: String,
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val skippedTests: Int,
    val passRate: Double,
    val totalFeatures: Int,
    val passedFeatures: Int,
    val totalStories: Int,
    val passedStories: Int,
    val duration: String,
    val status: TestStatus
)

/**
 * Failure reason analysis.
 */
data class FailureReason(
    val message: String,
    val count: Int,
    val affectedTests: List<String>
)

/**
 * Extension to check if module failed.
 */
val ModuleResult.failed: Boolean
    get() = status == TestStatus.FAILED || status == TestStatus.ERROR
