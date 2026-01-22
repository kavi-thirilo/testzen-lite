package com.testzen.core.model

import kotlinx.serialization.Serializable

/**
 * Result of a test execution.
 */
@Serializable
data class TestResult(
    val testId: String,
    val testName: String,
    val status: TestStatus,
    val durationMs: Long,
    val stepResults: List<StepResult>,
    val errorMessage: String? = null,
    val screenshot: String? = null,
    val startedAt: String? = null,
    val completedAt: String? = null
) {
    val passedSteps: Int get() = stepResults.count { it.status == StepStatus.PASSED }
    val failedSteps: Int get() = stepResults.count { it.status == StepStatus.FAILED }
    val skippedSteps: Int get() = stepResults.count { it.status == StepStatus.SKIPPED }
}

/**
 * Result of a single step execution.
 */
@Serializable
data class StepResult(
    val order: Int,
    val instruction: String,
    val status: StepStatus,
    val durationMs: Long,
    val errorMessage: String? = null,
    val screenshot: String? = null,
    val elementFound: Boolean? = null
)

/**
 * Overall test status.
 */
@Serializable
enum class TestStatus {
    PASSED,
    FAILED,
    SKIPPED,
    ERROR
}

/**
 * Individual step status.
 */
@Serializable
enum class StepStatus {
    PASSED,
    FAILED,
    SKIPPED,
    TIMEOUT,
    ERROR
}
