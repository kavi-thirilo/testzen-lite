package com.testzen.core.reporting

import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Comprehensive test result models for enterprise-grade reporting.
 *
 * Hierarchy:
 * TestExecutionReport
 * └── ModuleResult (e.g., "Payments", "Authentication", "Checkout")
 *     └── FeatureResult (e.g., "User Login", "Password Reset")
 *         └── StoryResult (e.g., "US-123: User can login with email")
 *             └── TestCaseResult (e.g., "test_valid_login")
 *                 └── StepResult (e.g., "Click Login button")
 *                     └── Screenshots (before/after)
 */

// ═══════════════════════════════════════════════════════════════
// ENUMS
// ═══════════════════════════════════════════════════════════════

/**
 * Test execution status.
 */
enum class TestStatus {
    PASSED,
    FAILED,
    SKIPPED,
    BLOCKED,
    ERROR,
    RUNNING,
    PENDING
}

/**
 * Severity level for failures.
 */
enum class Severity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFO
}

/**
 * Platform on which test was executed.
 */
enum class TestPlatform {
    ANDROID,
    IOS,
    WEB,
    API,
    UNKNOWN
}

// ═══════════════════════════════════════════════════════════════
// SCREENSHOT MODEL
// ═══════════════════════════════════════════════════════════════

/**
 * Screenshot captured during test execution.
 */
data class Screenshot(
    /** Unique identifier */
    val id: String = UUID.randomUUID().toString(),

    /** When screenshot was captured */
    val timestamp: Instant = Instant.now(),

    /** Type of screenshot */
    val type: ScreenshotType,

    /** File path to screenshot */
    val filePath: String,

    /** File name */
    val fileName: String,

    /** Base64 encoded image (optional, for embedded reports) */
    val base64Data: String? = null,

    /** Width in pixels */
    val width: Int? = null,

    /** Height in pixels */
    val height: Int? = null,

    /** File size in bytes */
    val fileSize: Long? = null,

    /** Additional metadata */
    val metadata: Map<String, String> = emptyMap()
)

enum class ScreenshotType {
    BEFORE_STEP,
    AFTER_STEP,
    ON_FAILURE,
    ON_SUCCESS,
    CUSTOM
}

// ═══════════════════════════════════════════════════════════════
// STEP RESULT
// ═══════════════════════════════════════════════════════════════

/**
 * Result of a single test step execution.
 */
data class StepResult(
    /** Unique identifier */
    val id: String = UUID.randomUUID().toString(),

    /** Step number (1-based) */
    val stepNumber: Int,

    /** Original instruction text */
    val instruction: String,

    /** Parsed intent */
    val intent: String? = null,

    /** Target element */
    val target: String? = null,

    /** Value used (for enter text, etc.) */
    val value: String? = null,

    /** Execution status */
    val status: TestStatus,

    /** Start time */
    val startTime: Instant,

    /** End time */
    val endTime: Instant,

    /** Duration in milliseconds */
    val durationMs: Long = Duration.between(startTime, endTime).toMillis(),

    /** Screenshot before step execution */
    val screenshotBefore: Screenshot? = null,

    /** Screenshot after step execution */
    val screenshotAfter: Screenshot? = null,

    /** Additional screenshots (e.g., on failure) */
    val additionalScreenshots: List<Screenshot> = emptyList(),

    /** Error message if failed */
    val errorMessage: String? = null,

    /** Stack trace if error */
    val stackTrace: String? = null,

    /** Actual value found (for verifications) */
    val actualValue: String? = null,

    /** Expected value (for verifications) */
    val expectedValue: String? = null,

    /** Element locator used */
    val locatorUsed: String? = null,

    /** Was element healed? */
    val wasHealed: Boolean = false,

    /** Original locator if healed */
    val originalLocator: String? = null,

    /** Retry count */
    val retryCount: Int = 0,

    /** Additional metadata */
    val metadata: Map<String, Any> = emptyMap()
) {
    val passed: Boolean get() = status == TestStatus.PASSED
    val failed: Boolean get() = status == TestStatus.FAILED || status == TestStatus.ERROR

    /** Get all screenshots for this step */
    val allScreenshots: List<Screenshot>
        get() = listOfNotNull(screenshotBefore, screenshotAfter) + additionalScreenshots
}

// ═══════════════════════════════════════════════════════════════
// TEST CASE RESULT
// ═══════════════════════════════════════════════════════════════

/**
 * Result of a single test case execution.
 */
data class TestCaseResult(
    /** Unique identifier */
    val id: String = UUID.randomUUID().toString(),

    /** Test case ID from YAML */
    val testId: String,

    /** Test case name */
    val name: String,

    /** Test description */
    val description: String? = null,

    /** Tags/labels */
    val tags: Set<String> = emptySet(),

    /** Priority */
    val priority: Int = 0,

    /** Execution status */
    val status: TestStatus,

    /** Start time */
    val startTime: Instant,

    /** End time */
    val endTime: Instant,

    /** Duration in milliseconds */
    val durationMs: Long = Duration.between(startTime, endTime).toMillis(),

    /** Individual step results */
    val steps: List<StepResult> = emptyList(),

    /** Platform executed on */
    val platform: TestPlatform = TestPlatform.UNKNOWN,

    /** Device/browser info */
    val deviceInfo: String? = null,

    /** App version */
    val appVersion: String? = null,

    /** Environment (dev, staging, prod) */
    val environment: String? = null,

    /** Error message if failed */
    val errorMessage: String? = null,

    /** First failing step */
    val failingStep: StepResult? = null,

    /** Retry attempt number */
    val retryAttempt: Int = 0,

    /** Was this a retry of a failed test */
    val isRetry: Boolean = false,

    /** Linked story ID */
    val storyId: String? = null,

    /** Linked feature ID */
    val featureId: String? = null,

    /** Linked module ID */
    val moduleId: String? = null,

    /** Additional metadata */
    val metadata: Map<String, Any> = emptyMap()
) {
    val passed: Boolean get() = status == TestStatus.PASSED
    val failed: Boolean get() = status == TestStatus.FAILED || status == TestStatus.ERROR

    /** Total steps */
    val totalSteps: Int get() = steps.size

    /** Passed steps */
    val passedSteps: Int get() = steps.count { it.passed }

    /** Failed steps */
    val failedSteps: Int get() = steps.count { it.failed }

    /** Skipped steps */
    val skippedSteps: Int get() = steps.count { it.status == TestStatus.SKIPPED }

    /** Step pass rate */
    val stepPassRate: Double
        get() = if (totalSteps > 0) passedSteps.toDouble() / totalSteps else 0.0

    /** All screenshots from all steps */
    val allScreenshots: List<Screenshot>
        get() = steps.flatMap { it.allScreenshots }

    /** Get failure screenshot */
    val failureScreenshot: Screenshot?
        get() = failingStep?.screenshotAfter
            ?: failingStep?.additionalScreenshots?.firstOrNull { it.type == ScreenshotType.ON_FAILURE }
}

// ═══════════════════════════════════════════════════════════════
// STORY RESULT (User Story Level)
// ═══════════════════════════════════════════════════════════════

/**
 * Result aggregated at user story level.
 */
data class StoryResult(
    /** Unique identifier */
    val id: String = UUID.randomUUID().toString(),

    /** Story ID (e.g., "US-123", "JIRA-456") */
    val storyId: String,

    /** Story name/title */
    val name: String,

    /** Story description */
    val description: String? = null,

    /** Tags */
    val tags: Set<String> = emptySet(),

    /** Test cases in this story */
    val testCases: List<TestCaseResult> = emptyList(),

    /** Parent feature ID */
    val featureId: String? = null,

    /** Execution status (aggregated) */
    val status: TestStatus = calculateStatus(testCases),

    /** Start time (earliest test) */
    val startTime: Instant = testCases.minOfOrNull { it.startTime } ?: Instant.now(),

    /** End time (latest test) */
    val endTime: Instant = testCases.maxOfOrNull { it.endTime } ?: Instant.now(),

    /** Total duration */
    val durationMs: Long = Duration.between(startTime, endTime).toMillis(),

    /** Additional metadata */
    val metadata: Map<String, Any> = emptyMap()
) {
    /** Total test cases */
    val totalTests: Int get() = testCases.size

    /** Passed test cases */
    val passedTests: Int get() = testCases.count { it.passed }

    /** Failed test cases */
    val failedTests: Int get() = testCases.count { it.failed }

    /** Skipped test cases */
    val skippedTests: Int get() = testCases.count { it.status == TestStatus.SKIPPED }

    /** Pass rate (0.0 to 1.0) */
    val passRate: Double
        get() = if (totalTests > 0) passedTests.toDouble() / totalTests else 0.0

    /** Pass rate percentage */
    val passRatePercent: Double get() = passRate * 100

    /** Total steps across all tests */
    val totalSteps: Int get() = testCases.sumOf { it.totalSteps }

    /** Passed steps across all tests */
    val passedSteps: Int get() = testCases.sumOf { it.passedSteps }

    companion object {
        private fun calculateStatus(tests: List<TestCaseResult>): TestStatus {
            return when {
                tests.isEmpty() -> TestStatus.PENDING
                tests.all { it.passed } -> TestStatus.PASSED
                tests.any { it.status == TestStatus.ERROR } -> TestStatus.ERROR
                tests.any { it.failed } -> TestStatus.FAILED
                tests.any { it.status == TestStatus.BLOCKED } -> TestStatus.BLOCKED
                tests.all { it.status == TestStatus.SKIPPED } -> TestStatus.SKIPPED
                else -> TestStatus.PASSED
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// FEATURE RESULT
// ═══════════════════════════════════════════════════════════════

/**
 * Result aggregated at feature level.
 */
data class FeatureResult(
    /** Unique identifier */
    val id: String = UUID.randomUUID().toString(),

    /** Feature ID */
    val featureId: String,

    /** Feature name */
    val name: String,

    /** Feature description */
    val description: String? = null,

    /** Tags */
    val tags: Set<String> = emptySet(),

    /** Stories in this feature */
    val stories: List<StoryResult> = emptyList(),

    /** Direct test cases (not in a story) */
    val directTestCases: List<TestCaseResult> = emptyList(),

    /** Parent module ID */
    val moduleId: String? = null,

    /** Execution status (aggregated) */
    val status: TestStatus = calculateStatus(stories, directTestCases),

    /** Start time */
    val startTime: Instant = calculateStartTime(stories, directTestCases),

    /** End time */
    val endTime: Instant = calculateEndTime(stories, directTestCases),

    /** Total duration */
    val durationMs: Long = Duration.between(startTime, endTime).toMillis(),

    /** Additional metadata */
    val metadata: Map<String, Any> = emptyMap()
) {
    /** All test cases (from stories + direct) */
    val allTestCases: List<TestCaseResult>
        get() = stories.flatMap { it.testCases } + directTestCases

    /** Total test cases */
    val totalTests: Int get() = allTestCases.size

    /** Passed test cases */
    val passedTests: Int get() = allTestCases.count { it.passed }

    /** Failed test cases */
    val failedTests: Int get() = allTestCases.count { it.failed }

    /** Skipped test cases */
    val skippedTests: Int get() = allTestCases.count { it.status == TestStatus.SKIPPED }

    /** Pass rate */
    val passRate: Double
        get() = if (totalTests > 0) passedTests.toDouble() / totalTests else 0.0

    /** Pass rate percentage */
    val passRatePercent: Double get() = passRate * 100

    /** Total stories */
    val totalStories: Int get() = stories.size

    /** Passed stories */
    val passedStories: Int get() = stories.count { it.status == TestStatus.PASSED }

    /** Total steps */
    val totalSteps: Int get() = allTestCases.sumOf { it.totalSteps }

    /** Passed steps */
    val passedSteps: Int get() = allTestCases.sumOf { it.passedSteps }

    companion object {
        private fun calculateStatus(stories: List<StoryResult>, direct: List<TestCaseResult>): TestStatus {
            val allTests = stories.flatMap { it.testCases } + direct
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

        private fun calculateStartTime(stories: List<StoryResult>, direct: List<TestCaseResult>): Instant {
            val storyStart = stories.minOfOrNull { it.startTime }
            val directStart = direct.minOfOrNull { it.startTime }
            return listOfNotNull(storyStart, directStart).minOrNull() ?: Instant.now()
        }

        private fun calculateEndTime(stories: List<StoryResult>, direct: List<TestCaseResult>): Instant {
            val storyEnd = stories.maxOfOrNull { it.endTime }
            val directEnd = direct.maxOfOrNull { it.endTime }
            return listOfNotNull(storyEnd, directEnd).maxOrNull() ?: Instant.now()
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// MODULE RESULT (LOB Level)
// ═══════════════════════════════════════════════════════════════

/**
 * Result aggregated at module/LOB level.
 */
data class ModuleResult(
    /** Unique identifier */
    val id: String = UUID.randomUUID().toString(),

    /** Module ID */
    val moduleId: String,

    /** Module name (e.g., "Payments", "Authentication") */
    val name: String,

    /** Module description */
    val description: String? = null,

    /** Owner/team */
    val owner: String? = null,

    /** Tags */
    val tags: Set<String> = emptySet(),

    /** Features in this module */
    val features: List<FeatureResult> = emptyList(),

    /** Direct test cases (not in a feature) */
    val directTestCases: List<TestCaseResult> = emptyList(),

    /** Execution status (aggregated) */
    val status: TestStatus = calculateStatus(features, directTestCases),

    /** Start time */
    val startTime: Instant = calculateStartTime(features, directTestCases),

    /** End time */
    val endTime: Instant = calculateEndTime(features, directTestCases),

    /** Total duration */
    val durationMs: Long = Duration.between(startTime, endTime).toMillis(),

    /** Additional metadata */
    val metadata: Map<String, Any> = emptyMap()
) {
    /** All test cases */
    val allTestCases: List<TestCaseResult>
        get() = features.flatMap { it.allTestCases } + directTestCases

    /** All stories */
    val allStories: List<StoryResult>
        get() = features.flatMap { it.stories }

    /** Total test cases */
    val totalTests: Int get() = allTestCases.size

    /** Passed test cases */
    val passedTests: Int get() = allTestCases.count { it.passed }

    /** Failed test cases */
    val failedTests: Int get() = allTestCases.count { it.failed }

    /** Skipped test cases */
    val skippedTests: Int get() = allTestCases.count { it.status == TestStatus.SKIPPED }

    /** Pass rate */
    val passRate: Double
        get() = if (totalTests > 0) passedTests.toDouble() / totalTests else 0.0

    /** Pass rate percentage */
    val passRatePercent: Double get() = passRate * 100

    /** Total features */
    val totalFeatures: Int get() = features.size

    /** Passed features */
    val passedFeatures: Int get() = features.count { it.status == TestStatus.PASSED }

    /** Total stories */
    val totalStories: Int get() = allStories.size

    /** Passed stories */
    val passedStories: Int get() = allStories.count { it.status == TestStatus.PASSED }

    /** Total steps */
    val totalSteps: Int get() = allTestCases.sumOf { it.totalSteps }

    /** Passed steps */
    val passedSteps: Int get() = allTestCases.sumOf { it.passedSteps }

    companion object {
        private fun calculateStatus(features: List<FeatureResult>, direct: List<TestCaseResult>): TestStatus {
            val allTests = features.flatMap { it.allTestCases } + direct
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

        private fun calculateStartTime(features: List<FeatureResult>, direct: List<TestCaseResult>): Instant {
            val featureStart = features.minOfOrNull { it.startTime }
            val directStart = direct.minOfOrNull { it.startTime }
            return listOfNotNull(featureStart, directStart).minOrNull() ?: Instant.now()
        }

        private fun calculateEndTime(features: List<FeatureResult>, direct: List<TestCaseResult>): Instant {
            val featureEnd = features.maxOfOrNull { it.endTime }
            val directEnd = direct.maxOfOrNull { it.endTime }
            return listOfNotNull(featureEnd, directEnd).maxOrNull() ?: Instant.now()
        }
    }
}
