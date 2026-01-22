package com.testzen.core.parser

import com.testzen.core.model.TestCase
import com.testzen.core.model.TestStep
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Writes TestCase objects to various formats (YAML, JSON).
 *
 * Primary use cases:
 * 1. Convert plain English tests to YAML format
 * 2. Export test definitions for version control
 * 3. Generate test templates
 *
 * Usage:
 * ```kotlin
 * val writer = TestWriter()
 *
 * // Convert to YAML string
 * val yaml = writer.toYaml(testCase)
 *
 * // Write to file
 * writer.writeYaml(testCase, File("tests/login_test.yaml"))
 *
 * // Batch write multiple tests
 * writer.writeYamlBatch(testCases, File("tests/module/"))
 * ```
 */
class TestWriter {
    private val logger = LoggerFactory.getLogger(TestWriter::class.java)

    /**
     * Configuration for YAML output.
     */
    data class YamlConfig(
        /** Include null/empty fields */
        val includeEmptyFields: Boolean = false,
        /** Use simple step format (string list) vs detailed format */
        val simpleStepFormat: Boolean = true,
        /** Include generated comments */
        val includeComments: Boolean = true,
        /** Indent size (spaces) */
        val indentSize: Int = 2,
        /** Quote style for strings */
        val quoteStyle: QuoteStyle = QuoteStyle.DOUBLE_WHEN_NEEDED
    )

    enum class QuoteStyle {
        ALWAYS_SINGLE,
        ALWAYS_DOUBLE,
        SINGLE_WHEN_NEEDED,
        DOUBLE_WHEN_NEEDED,
        NONE
    }

    private val defaultConfig = YamlConfig()

    /**
     * Convert TestCase to YAML string.
     */
    fun toYaml(testCase: TestCase, config: YamlConfig = defaultConfig): String {
        val sb = StringBuilder()
        val indent = " ".repeat(config.indentSize)

        // Header comment
        if (config.includeComments) {
            sb.appendLine("# TestZen Test Definition")
            sb.appendLine("# Generated from plain English test")
            sb.appendLine()
        }

        // Required fields
        sb.appendLine("test_id: ${quote(testCase.testId, config)}")
        sb.appendLine("name: ${quote(testCase.name, config)}")

        // Optional metadata
        testCase.description?.let {
            sb.appendLine("description: ${quote(it, config)}")
        }

        testCase.platform?.let {
            sb.appendLine("platform: ${it.name.lowercase()}")
        }

        testCase.module?.let {
            sb.appendLine("module: ${quote(it, config)}")
        }

        testCase.appName?.let {
            sb.appendLine("app_name: ${quote(it, config)}")
        }

        testCase.packageName?.let {
            sb.appendLine("package_name: ${quote(it, config)}")
        }

        testCase.bundleId?.let {
            sb.appendLine("bundle_id: ${quote(it, config)}")
        }

        testCase.baseUrl?.let {
            sb.appendLine("base_url: ${quote(it, config)}")
        }

        // Tags
        if (testCase.tags.isNotEmpty()) {
            sb.appendLine("tags:")
            testCase.tags.forEach { tag ->
                sb.appendLine("$indent- ${quote(tag, config)}")
            }
        }

        // Steps
        sb.appendLine()
        sb.appendLine("steps:")

        if (config.simpleStepFormat) {
            // Simple format: just list of instruction strings
            testCase.steps.forEach { step ->
                sb.appendLine("$indent- ${quote(step.instruction, config)}")
            }
        } else {
            // Detailed format: step objects with metadata
            testCase.steps.forEach { step ->
                writeDetailedStep(sb, step, indent, config)
            }
        }

        return sb.toString()
    }

    /**
     * Write detailed step with all metadata.
     */
    private fun writeDetailedStep(
        sb: StringBuilder,
        step: TestStep,
        indent: String,
        config: YamlConfig
    ) {
        sb.appendLine("$indent- instruction: ${quote(step.instruction, config)}")

        step.description?.let {
            sb.appendLine("$indent${indent}description: ${quote(it, config)}")
        }

        if (step.screenshot) {
            sb.appendLine("$indent${indent}screenshot: true")
        }

        if (step.optional) {
            sb.appendLine("$indent${indent}optional: true")
        }

        step.timeout?.let {
            sb.appendLine("$indent${indent}timeout: $it")
        }
    }

    /**
     * Quote a string value for YAML.
     */
    private fun quote(value: String, config: YamlConfig): String {
        val needsQuoting = value.contains(":") ||
                value.contains("#") ||
                value.contains("'") ||
                value.contains("\"") ||
                value.contains("\n") ||
                value.startsWith(" ") ||
                value.endsWith(" ") ||
                value.matches(Regex("^[\\[{].*")) ||
                value.matches(Regex("^(true|false|yes|no|null|~)$", RegexOption.IGNORE_CASE)) ||
                value.matches(Regex("^[0-9].*"))

        return when (config.quoteStyle) {
            QuoteStyle.ALWAYS_SINGLE -> "'${value.replace("'", "''")}'"
            QuoteStyle.ALWAYS_DOUBLE -> "\"${escapeDouble(value)}\""
            QuoteStyle.SINGLE_WHEN_NEEDED -> if (needsQuoting) "'${value.replace("'", "''")}'" else value
            QuoteStyle.DOUBLE_WHEN_NEEDED -> if (needsQuoting) "\"${escapeDouble(value)}\"" else value
            QuoteStyle.NONE -> value
        }
    }

    private fun escapeDouble(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Write TestCase to YAML file.
     */
    fun writeYaml(testCase: TestCase, file: File, config: YamlConfig = defaultConfig) {
        file.parentFile?.mkdirs()
        file.writeText(toYaml(testCase, config))
        logger.info("Wrote test to ${file.absolutePath}")
    }

    /**
     * Write multiple TestCases to a single YAML file (multi-document).
     */
    fun writeYamlMultiDoc(
        testCases: List<TestCase>,
        file: File,
        config: YamlConfig = defaultConfig
    ) {
        file.parentFile?.mkdirs()

        val content = testCases.joinToString("\n---\n\n") { toYaml(it, config) }
        file.writeText(content)

        logger.info("Wrote ${testCases.size} tests to ${file.absolutePath}")
    }

    /**
     * Write TestCases to individual files in a directory.
     *
     * @param testCases Tests to write
     * @param directory Target directory
     * @param fileNameGenerator Function to generate filename for each test
     * @param config YAML configuration
     * @return List of created files
     */
    fun writeYamlBatch(
        testCases: List<TestCase>,
        directory: File,
        fileNameGenerator: (TestCase) -> String = { it.testId + ".yaml" },
        config: YamlConfig = defaultConfig
    ): List<File> {
        directory.mkdirs()

        return testCases.map { testCase ->
            val fileName = fileNameGenerator(testCase)
            val file = File(directory, fileName)
            writeYaml(testCase, file, config)
            file
        }
    }

    /**
     * Convert TestCase to JSON string.
     */
    fun toJson(testCase: TestCase, prettyPrint: Boolean = true): String {
        val sb = StringBuilder()
        val indent = if (prettyPrint) "  " else ""
        val nl = if (prettyPrint) "\n" else ""

        sb.append("{$nl")
        sb.append("$indent\"test_id\": \"${escapeJson(testCase.testId)}\",$nl")
        sb.append("$indent\"name\": \"${escapeJson(testCase.name)}\",$nl")

        testCase.description?.let {
            sb.append("$indent\"description\": \"${escapeJson(it)}\",$nl")
        }

        testCase.platform?.let {
            sb.append("$indent\"platform\": \"${it.name.lowercase()}\",$nl")
        }

        testCase.module?.let {
            sb.append("$indent\"module\": \"${escapeJson(it)}\",$nl")
        }

        testCase.appName?.let {
            sb.append("$indent\"app_name\": \"${escapeJson(it)}\",$nl")
        }

        testCase.baseUrl?.let {
            sb.append("$indent\"base_url\": \"${escapeJson(it)}\",$nl")
        }

        // Tags
        if (testCase.tags.isNotEmpty()) {
            sb.append("$indent\"tags\": [${testCase.tags.joinToString(", ") { "\"${escapeJson(it)}\"" }}],$nl")
        }

        // Steps
        sb.append("$indent\"steps\": [$nl")
        testCase.steps.forEachIndexed { index, step ->
            val comma = if (index < testCase.steps.size - 1) "," else ""
            if (prettyPrint) {
                sb.append("$indent$indent{$nl")
                sb.append("$indent$indent$indent\"order\": ${step.order},$nl")
                sb.append("$indent$indent$indent\"instruction\": \"${escapeJson(step.instruction)}\"$nl")
                sb.append("$indent$indent}$comma$nl")
            } else {
                sb.append("{\"order\":${step.order},\"instruction\":\"${escapeJson(step.instruction)}\"}$comma")
            }
        }
        sb.append("$indent]$nl")
        sb.append("}")

        return sb.toString()
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Write TestCase to JSON file.
     */
    fun writeJson(testCase: TestCase, file: File, prettyPrint: Boolean = true) {
        file.parentFile?.mkdirs()
        file.writeText(toJson(testCase, prettyPrint))
        logger.info("Wrote test to ${file.absolutePath}")
    }

    companion object {
        /**
         * Generate a safe filename from test name.
         */
        fun safeFileName(testCase: TestCase): String {
            val sanitized = testCase.name.lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
                .take(50)
            return "${sanitized}.yaml"
        }

        /**
         * Generate filename with module prefix.
         */
        fun moduleFileName(testCase: TestCase): String {
            val module = testCase.module?.lowercase()
                ?.replace(Regex("[^a-z0-9]+"), "_")
                ?.trim('_')
                ?: "general"

            val name = testCase.name.lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
                .take(40)

            return "${module}_${name}.yaml"
        }
    }
}
