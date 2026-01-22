package com.testzen.core.reporting

import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.ZoneId

/**
 * Generates JSON reports for programmatic access and CI/CD integration.
 *
 * Features:
 * - Full hierarchical data structure
 * - Compact summary format
 * - JUnit XML compatible format
 * - CI/CD integration friendly
 */
class JsonReportGenerator(
    private val config: JsonReportConfig = JsonReportConfig()
) {
    private val dateFormatter = DateTimeFormatter.ISO_INSTANT

    /**
     * Generate full JSON report.
     */
    fun generate(report: TestExecutionReport, outputPath: String): File {
        val json = generateJson(report)
        val file = File(outputPath)
        file.parentFile?.mkdirs()
        file.writeText(json)
        return file
    }

    /**
     * Generate summary JSON (compact format).
     */
    fun generateSummary(report: TestExecutionReport, outputPath: String): File {
        val json = generateSummaryJson(report)
        val file = File(outputPath)
        file.parentFile?.mkdirs()
        file.writeText(json)
        return file
    }

    /**
     * Generate full JSON content.
     */
    fun generateJson(report: TestExecutionReport): String {
        return buildString {
            appendLine("{")
            appendLine("  \"reportId\": \"${report.id}\",")
            appendLine("  \"name\": ${report.name.toJson()},")
            report.description?.let { appendLine("  \"description\": ${it.toJson()},") }
            appendLine("  \"environment\": ${report.environment.toJson()},")
            appendLine("  \"status\": \"${report.status}\",")
            appendLine("  \"generatedAt\": \"${dateFormatter.format(report.generatedAt)}\",")
            appendLine()

            // Build info
            report.buildInfo?.let { build ->
                appendLine("  \"buildInfo\": {")
                build.buildNumber?.let { appendLine("    \"buildNumber\": ${it.toJson()},") }
                build.branch?.let { appendLine("    \"branch\": ${it.toJson()},") }
                build.commit?.let { appendLine("    \"commit\": ${it.toJson()},") }
                build.buildUrl?.let { appendLine("    \"buildUrl\": ${it.toJson()},") }
                appendLine("    \"_\": null")
                appendLine("  },")
            }

            // Timing
            appendLine("  \"timing\": {")
            appendLine("    \"startTime\": \"${dateFormatter.format(report.startTime)}\",")
            appendLine("    \"endTime\": \"${dateFormatter.format(report.endTime)}\",")
            appendLine("    \"durationMs\": ${report.durationMs},")
            appendLine("    \"formattedDuration\": ${report.formattedDuration.toJson()}")
            appendLine("  },")
            appendLine()

            // Summary statistics
            appendLine("  \"summary\": {")
            appendLine("    \"tests\": {")
            appendLine("      \"total\": ${report.totalTests},")
            appendLine("      \"passed\": ${report.passedTests},")
            appendLine("      \"failed\": ${report.failedTests},")
            appendLine("      \"skipped\": ${report.skippedTests},")
            appendLine("      \"blocked\": ${report.blockedTests},")
            appendLine("      \"error\": ${report.errorTests},")
            appendLine("      \"passRate\": ${formatDouble(report.testPassRatePercent)}")
            appendLine("    },")
            appendLine("    \"steps\": {")
            appendLine("      \"total\": ${report.totalSteps},")
            appendLine("      \"passed\": ${report.passedSteps},")
            appendLine("      \"failed\": ${report.failedSteps},")
            appendLine("      \"passRate\": ${formatDouble(report.stepPassRatePercent)}")
            appendLine("    },")
            appendLine("    \"modules\": {")
            appendLine("      \"total\": ${report.totalModules},")
            appendLine("      \"passed\": ${report.passedModules}")
            appendLine("    },")
            appendLine("    \"features\": {")
            appendLine("      \"total\": ${report.totalFeatures},")
            appendLine("      \"passed\": ${report.passedFeatures}")
            appendLine("    },")
            appendLine("    \"stories\": {")
            appendLine("      \"total\": ${report.totalStories},")
            appendLine("      \"passed\": ${report.passedStories}")
            appendLine("    }")
            appendLine("  },")
            appendLine()

            // Modules
            appendLine("  \"modules\": [")
            for ((index, module) in report.modules.withIndex()) {
                appendModule(module, index == report.modules.lastIndex)
            }
            appendLine("  ],")
            appendLine()

            // Direct test cases
            if (report.directTestCases.isNotEmpty()) {
                appendLine("  \"directTestCases\": [")
                for ((index, test) in report.directTestCases.withIndex()) {
                    appendTestCase(test, index == report.directTestCases.lastIndex, "    ")
                }
                appendLine("  ],")
            }

            // Failure analysis
            if (report.failedTests > 0) {
                appendLine("  \"failureAnalysis\": {")
                appendLine("    \"topReasons\": [")
                for ((index, reason) in report.topFailureReasons.withIndex()) {
                    appendLine("      {")
                    appendLine("        \"message\": ${reason.message.toJson()},")
                    appendLine("        \"count\": ${reason.count},")
                    appendLine("        \"affectedTests\": [${reason.affectedTests.joinToString(", ") { it.toJson() }}]")
                    appendLine("      }${if (index < report.topFailureReasons.lastIndex) "," else ""}")
                }
                appendLine("    ],")
                appendLine("    \"failuresByModule\": {")
                val failedModules = report.failuresByModule.filter { it.value > 0 }
                for ((index, entry) in failedModules.entries.withIndex()) {
                    appendLine("      ${entry.key.toJson()}: ${entry.value}${if (index < failedModules.size - 1) "," else ""}")
                }
                appendLine("    }")
                appendLine("  },")
            }

            // Metadata
            if (report.metadata.isNotEmpty()) {
                appendLine("  \"metadata\": {")
                for ((index, entry) in report.metadata.entries.withIndex()) {
                    appendLine("    ${entry.key.toJson()}: ${entry.value.toString().toJson()}${if (index < report.metadata.size - 1) "," else ""}")
                }
                appendLine("  },")
            }

            appendLine("  \"_version\": \"1.0\"")
            appendLine("}")
        }
    }

    private fun StringBuilder.appendModule(module: ModuleResult, isLast: Boolean) {
        appendLine("    {")
        appendLine("      \"moduleId\": ${module.moduleId.toJson()},")
        appendLine("      \"name\": ${module.name.toJson()},")
        module.description?.let { appendLine("      \"description\": ${it.toJson()},") }
        module.owner?.let { appendLine("      \"owner\": ${it.toJson()},") }
        appendLine("      \"status\": \"${module.status}\",")
        appendLine("      \"timing\": {")
        appendLine("        \"startTime\": \"${dateFormatter.format(module.startTime)}\",")
        appendLine("        \"endTime\": \"${dateFormatter.format(module.endTime)}\",")
        appendLine("        \"durationMs\": ${module.durationMs}")
        appendLine("      },")
        appendLine("      \"statistics\": {")
        appendLine("        \"totalTests\": ${module.totalTests},")
        appendLine("        \"passedTests\": ${module.passedTests},")
        appendLine("        \"failedTests\": ${module.failedTests},")
        appendLine("        \"skippedTests\": ${module.skippedTests},")
        appendLine("        \"passRate\": ${formatDouble(module.passRatePercent)},")
        appendLine("        \"totalFeatures\": ${module.totalFeatures},")
        appendLine("        \"totalStories\": ${module.totalStories},")
        appendLine("        \"totalSteps\": ${module.totalSteps}")
        appendLine("      },")

        // Features
        appendLine("      \"features\": [")
        for ((index, feature) in module.features.withIndex()) {
            appendFeature(feature, index == module.features.lastIndex)
        }
        appendLine("      ],")

        // Direct test cases
        if (module.directTestCases.isNotEmpty()) {
            appendLine("      \"directTestCases\": [")
            for ((index, test) in module.directTestCases.withIndex()) {
                appendTestCase(test, index == module.directTestCases.lastIndex, "        ")
            }
            appendLine("      ],")
        }

        appendLine("      \"_\": null")
        appendLine("    }${if (!isLast) "," else ""}")
    }

    private fun StringBuilder.appendFeature(feature: FeatureResult, isLast: Boolean) {
        appendLine("        {")
        appendLine("          \"featureId\": ${feature.featureId.toJson()},")
        appendLine("          \"name\": ${feature.name.toJson()},")
        feature.description?.let { appendLine("          \"description\": ${it.toJson()},") }
        appendLine("          \"status\": \"${feature.status}\",")
        appendLine("          \"statistics\": {")
        appendLine("            \"totalTests\": ${feature.totalTests},")
        appendLine("            \"passedTests\": ${feature.passedTests},")
        appendLine("            \"failedTests\": ${feature.failedTests},")
        appendLine("            \"passRate\": ${formatDouble(feature.passRatePercent)},")
        appendLine("            \"totalStories\": ${feature.totalStories}")
        appendLine("          },")

        // Stories
        appendLine("          \"stories\": [")
        for ((index, story) in feature.stories.withIndex()) {
            appendStory(story, index == feature.stories.lastIndex)
        }
        appendLine("          ],")

        // Direct test cases
        if (feature.directTestCases.isNotEmpty()) {
            appendLine("          \"directTestCases\": [")
            for ((index, test) in feature.directTestCases.withIndex()) {
                appendTestCase(test, index == feature.directTestCases.lastIndex, "            ")
            }
            appendLine("          ],")
        }

        appendLine("          \"_\": null")
        appendLine("        }${if (!isLast) "," else ""}")
    }

    private fun StringBuilder.appendStory(story: StoryResult, isLast: Boolean) {
        appendLine("            {")
        appendLine("              \"storyId\": ${story.storyId.toJson()},")
        appendLine("              \"name\": ${story.name.toJson()},")
        story.description?.let { appendLine("              \"description\": ${it.toJson()},") }
        appendLine("              \"status\": \"${story.status}\",")
        appendLine("              \"statistics\": {")
        appendLine("                \"totalTests\": ${story.totalTests},")
        appendLine("                \"passedTests\": ${story.passedTests},")
        appendLine("                \"failedTests\": ${story.failedTests},")
        appendLine("                \"passRate\": ${formatDouble(story.passRatePercent)}")
        appendLine("              },")

        // Test cases
        appendLine("              \"testCases\": [")
        for ((index, test) in story.testCases.withIndex()) {
            appendTestCase(test, index == story.testCases.lastIndex, "                ")
        }
        appendLine("              ]")
        appendLine("            }${if (!isLast) "," else ""}")
    }

    private fun StringBuilder.appendTestCase(test: TestCaseResult, isLast: Boolean, indent: String) {
        appendLine("$indent{")
        appendLine("$indent  \"id\": ${test.id.toJson()},")
        appendLine("$indent  \"testId\": ${test.testId.toJson()},")
        appendLine("$indent  \"name\": ${test.name.toJson()},")
        test.description?.let { appendLine("$indent  \"description\": ${it.toJson()},") }
        appendLine("$indent  \"status\": \"${test.status}\",")
        appendLine("$indent  \"platform\": \"${test.platform}\",")
        test.deviceInfo?.let { appendLine("$indent  \"deviceInfo\": ${it.toJson()},") }
        test.environment?.let { appendLine("$indent  \"environment\": ${it.toJson()},") }
        appendLine("$indent  \"timing\": {")
        appendLine("$indent    \"startTime\": \"${dateFormatter.format(test.startTime)}\",")
        appendLine("$indent    \"endTime\": \"${dateFormatter.format(test.endTime)}\",")
        appendLine("$indent    \"durationMs\": ${test.durationMs}")
        appendLine("$indent  },")
        appendLine("$indent  \"statistics\": {")
        appendLine("$indent    \"totalSteps\": ${test.totalSteps},")
        appendLine("$indent    \"passedSteps\": ${test.passedSteps},")
        appendLine("$indent    \"failedSteps\": ${test.failedSteps},")
        appendLine("$indent    \"stepPassRate\": ${formatDouble(test.stepPassRate * 100)}")
        appendLine("$indent  },")

        // Error info
        if (test.failed) {
            test.errorMessage?.let { appendLine("$indent  \"errorMessage\": ${it.toJson()},") }
            test.failingStep?.let { step ->
                appendLine("$indent  \"failingStep\": {")
                appendLine("$indent    \"stepNumber\": ${step.stepNumber},")
                appendLine("$indent    \"instruction\": ${step.instruction.toJson()},")
                step.errorMessage?.let { appendLine("$indent    \"errorMessage\": ${it.toJson()},") }
                appendLine("$indent    \"_\": null")
                appendLine("$indent  },")
            }
        }

        // Steps
        if (config.includeSteps) {
            appendLine("$indent  \"steps\": [")
            for ((index, step) in test.steps.withIndex()) {
                appendStep(step, index == test.steps.lastIndex, "$indent    ")
            }
            appendLine("$indent  ],")
        }

        // Tags
        if (test.tags.isNotEmpty()) {
            appendLine("$indent  \"tags\": [${test.tags.joinToString(", ") { it.toJson() }}],")
        }

        appendLine("$indent  \"_\": null")
        appendLine("$indent}${if (!isLast) "," else ""}")
    }

    private fun StringBuilder.appendStep(step: StepResult, isLast: Boolean, indent: String) {
        appendLine("$indent{")
        appendLine("$indent  \"stepNumber\": ${step.stepNumber},")
        appendLine("$indent  \"instruction\": ${step.instruction.toJson()},")
        step.intent?.let { appendLine("$indent  \"intent\": ${it.toJson()},") }
        step.target?.let { appendLine("$indent  \"target\": ${it.toJson()},") }
        step.value?.let { appendLine("$indent  \"value\": ${it.toJson()},") }
        appendLine("$indent  \"status\": \"${step.status}\",")
        appendLine("$indent  \"durationMs\": ${step.durationMs},")

        if (step.wasHealed) {
            appendLine("$indent  \"wasHealed\": true,")
            step.originalLocator?.let { appendLine("$indent  \"originalLocator\": ${it.toJson()},") }
            step.locatorUsed?.let { appendLine("$indent  \"healedLocator\": ${it.toJson()},") }
        }

        // Screenshots
        if (config.includeScreenshots) {
            step.screenshotBefore?.let {
                appendLine("$indent  \"screenshotBefore\": {")
                appendLine("$indent    \"filePath\": ${it.filePath.toJson()},")
                appendLine("$indent    \"timestamp\": \"${dateFormatter.format(it.timestamp)}\"")
                appendLine("$indent  },")
            }
            step.screenshotAfter?.let {
                appendLine("$indent  \"screenshotAfter\": {")
                appendLine("$indent    \"filePath\": ${it.filePath.toJson()},")
                appendLine("$indent    \"timestamp\": \"${dateFormatter.format(it.timestamp)}\"")
                appendLine("$indent  },")
            }
        }

        // Error info
        if (step.failed) {
            step.errorMessage?.let { appendLine("$indent  \"errorMessage\": ${it.toJson()},") }
            step.actualValue?.let { appendLine("$indent  \"actualValue\": ${it.toJson()},") }
            step.expectedValue?.let { appendLine("$indent  \"expectedValue\": ${it.toJson()},") }
        }

        appendLine("$indent  \"_\": null")
        appendLine("$indent}${if (!isLast) "," else ""}")
    }

    /**
     * Generate summary JSON (compact).
     */
    fun generateSummaryJson(report: TestExecutionReport): String {
        return buildString {
            appendLine("{")
            appendLine("  \"reportId\": \"${report.id}\",")
            appendLine("  \"name\": ${report.name.toJson()},")
            appendLine("  \"status\": \"${report.status}\",")
            appendLine("  \"environment\": ${report.environment.toJson()},")
            appendLine("  \"timestamp\": \"${dateFormatter.format(report.generatedAt)}\",")
            appendLine("  \"duration\": ${report.formattedDuration.toJson()},")
            appendLine("  \"durationMs\": ${report.durationMs},")
            appendLine()
            appendLine("  \"totals\": {")
            appendLine("    \"tests\": ${report.totalTests},")
            appendLine("    \"passed\": ${report.passedTests},")
            appendLine("    \"failed\": ${report.failedTests},")
            appendLine("    \"skipped\": ${report.skippedTests},")
            appendLine("    \"passRate\": ${formatDouble(report.testPassRatePercent)}")
            appendLine("  },")
            appendLine()
            appendLine("  \"hierarchy\": {")
            appendLine("    \"modules\": ${report.totalModules},")
            appendLine("    \"features\": ${report.totalFeatures},")
            appendLine("    \"stories\": ${report.totalStories},")
            appendLine("    \"steps\": ${report.totalSteps}")
            appendLine("  },")
            appendLine()
            appendLine("  \"modules\": [")
            for ((index, module) in report.modules.withIndex()) {
                appendLine("    {")
                appendLine("      \"name\": ${module.name.toJson()},")
                appendLine("      \"status\": \"${module.status}\",")
                appendLine("      \"total\": ${module.totalTests},")
                appendLine("      \"passed\": ${module.passedTests},")
                appendLine("      \"failed\": ${module.failedTests},")
                appendLine("      \"passRate\": ${formatDouble(module.passRatePercent)}")
                appendLine("    }${if (index < report.modules.lastIndex) "," else ""}")
            }
            appendLine("  ]")
            appendLine("}")
        }
    }

    /**
     * Generate JUnit XML format for CI/CD integration.
     */
    fun generateJunitXml(report: TestExecutionReport, outputPath: String): File {
        val xml = buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            appendLine("<testsuites name=\"${escapeXml(report.name)}\" tests=\"${report.totalTests}\" failures=\"${report.failedTests}\" errors=\"${report.errorTests}\" skipped=\"${report.skippedTests}\" time=\"${report.durationMs / 1000.0}\">")

            for (module in report.modules) {
                appendLine("  <testsuite name=\"${escapeXml(module.name)}\" tests=\"${module.totalTests}\" failures=\"${module.failedTests}\" skipped=\"${module.skippedTests}\" time=\"${module.durationMs / 1000.0}\">")

                for (testCase in module.allTestCases) {
                    append("    <testcase name=\"${escapeXml(testCase.name)}\" classname=\"${escapeXml(module.name)}.${escapeXml(testCase.testId)}\" time=\"${testCase.durationMs / 1000.0}\"")

                    when (testCase.status) {
                        TestStatus.PASSED -> appendLine("/>")
                        TestStatus.FAILED -> {
                            appendLine(">")
                            appendLine("      <failure message=\"${escapeXml(testCase.errorMessage ?: "Test failed")}\" type=\"AssertionError\">")
                            testCase.failingStep?.let { step ->
                                appendLine("Step ${step.stepNumber}: ${escapeXml(step.instruction)}")
                                step.errorMessage?.let { appendLine("Error: ${escapeXml(it)}") }
                            }
                            appendLine("      </failure>")
                            appendLine("    </testcase>")
                        }
                        TestStatus.ERROR -> {
                            appendLine(">")
                            appendLine("      <error message=\"${escapeXml(testCase.errorMessage ?: "Error occurred")}\" type=\"Error\"/>")
                            appendLine("    </testcase>")
                        }
                        TestStatus.SKIPPED -> {
                            appendLine(">")
                            appendLine("      <skipped/>")
                            appendLine("    </testcase>")
                        }
                        else -> appendLine("/>")
                    }
                }

                appendLine("  </testsuite>")
            }

            appendLine("</testsuites>")
        }

        val file = File(outputPath)
        file.parentFile?.mkdirs()
        file.writeText(xml)
        return file
    }

    private fun String.toJson(): String {
        return "\"${this
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")}\""
    }

    private fun formatDouble(value: Double): String {
        return "%.2f".format(value)
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}

/**
 * Configuration for JSON report generation.
 */
data class JsonReportConfig(
    /** Include individual steps in output */
    val includeSteps: Boolean = true,

    /** Include screenshot paths */
    val includeScreenshots: Boolean = true,

    /** Include metadata */
    val includeMetadata: Boolean = true,

    /** Pretty print JSON */
    val prettyPrint: Boolean = true
)
