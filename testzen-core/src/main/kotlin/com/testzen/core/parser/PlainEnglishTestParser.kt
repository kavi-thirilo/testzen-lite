package com.testzen.core.parser

import com.testzen.core.model.Platform
import com.testzen.core.model.TestCase
import com.testzen.core.model.TestStep
import org.slf4j.LoggerFactory
import java.io.File
import java.util.UUID

/**
 * Parser for plain English test files.
 *
 * Converts human-readable test descriptions written in natural language
 * into structured TestCase objects that can be executed or saved as YAML.
 *
 * Supported Plain English Formats:
 *
 * ## Format 1: Simple List Format
 * ```
 * Test: User Login Flow
 * Module: Authentication
 * Platform: Android
 *
 * - Launch the app
 * - Enter 'demo@example.com' in Email
 * - Click Login
 * - Verify 'Welcome' is displayed
 * ```
 *
 * ## Format 2: Numbered Steps Format
 * ```
 * Test: Checkout Process
 * Description: Complete end-to-end checkout
 * For: Web
 * Tags: smoke, critical
 *
 * Steps:
 * 1. Click "Add to Cart"
 * 2. Click "Checkout"
 * 3. Enter '4111111111111111' in Card Number
 * 4. Click "Pay Now"
 * 5. Verify "Order Confirmed" is displayed
 * ```
 *
 * ## Format 3: Minimal Format (Just Steps)
 * ```
 * Login Test (Android)
 *
 * Launch the app
 * Enter 'user@test.com' in email
 * Enter 'password' in password
 * Click login
 * Verify dashboard is displayed
 * ```
 *
 * Usage:
 * ```kotlin
 * val parser = PlainEnglishTestParser()
 *
 * // Parse from file
 * val tests = parser.parseFile(File("tests/login.test"))
 *
 * // Parse from string
 * val test = parser.parse("""
 *     Test: Login Flow
 *     Platform: Android
 *
 *     - Launch the app
 *     - Click Login
 * """.trimIndent())
 *
 * // Convert to YAML
 * val yaml = TestWriter.toYaml(test)
 * ```
 */
class PlainEnglishTestParser {
    private val logger = LoggerFactory.getLogger(PlainEnglishTestParser::class.java)

    /**
     * Parsed test structure before conversion to TestCase.
     */
    data class ParsedTest(
        val name: String,
        val description: String? = null,
        val platform: Platform? = null,
        val module: String? = null,
        val feature: String? = null,
        val story: String? = null,
        val tags: List<String> = emptyList(),
        val appName: String? = null,
        val packageName: String? = null,
        val bundleId: String? = null,
        val baseUrl: String? = null,
        val steps: List<String>
    )

    /**
     * Parse a plain English test file.
     *
     * @param file The test file (.test, .txt, or .english)
     * @return List of parsed TestCase objects
     */
    fun parseFile(file: File): List<TestCase> {
        if (!file.exists()) {
            throw IllegalArgumentException("Test file does not exist: ${file.absolutePath}")
        }

        val content = file.readText()
        val detectedPlatform = detectPlatformFromPath(file)

        return parseAll(content, detectedPlatform, file.nameWithoutExtension)
    }

    /**
     * Parse plain English test content.
     *
     * @param content The plain English test content
     * @param defaultPlatform Default platform if not specified in content
     * @param sourceName Source name for test ID generation
     * @return Single TestCase (first test if multiple found)
     */
    fun parse(
        content: String,
        defaultPlatform: Platform? = null,
        sourceName: String = "test"
    ): TestCase {
        val tests = parseAll(content, defaultPlatform, sourceName)
        if (tests.isEmpty()) {
            throw IllegalArgumentException("No valid test found in content")
        }
        return tests.first()
    }

    /**
     * Parse multiple tests from plain English content.
     *
     * Tests are separated by "---" or double blank lines.
     *
     * @param content The plain English test content
     * @param defaultPlatform Default platform if not specified
     * @param sourceName Source name for test ID generation
     * @return List of parsed TestCase objects
     */
    fun parseAll(
        content: String,
        defaultPlatform: Platform? = null,
        sourceName: String = "test"
    ): List<TestCase> {
        // Split content into individual test blocks
        val testBlocks = splitIntoTestBlocks(content)

        return testBlocks.mapIndexedNotNull { index, block ->
            try {
                val parsed = parseTestBlock(block.trim(), defaultPlatform)
                if (parsed != null) {
                    convertToTestCase(parsed, sourceName, index)
                } else null
            } catch (e: Exception) {
                logger.warn("Failed to parse test block ${index + 1}: ${e.message}")
                null
            }
        }
    }

    /**
     * Split content into individual test blocks.
     */
    private fun splitIntoTestBlocks(content: String): List<String> {
        // First try explicit separator "---"
        if (content.contains(Regex("(?m)^---\\s*$"))) {
            return content.split(Regex("(?m)^---\\s*$"))
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }

        // Then try double blank lines
        if (content.contains(Regex("\n\\s*\n\\s*\n"))) {
            val blocks = content.split(Regex("\n\\s*\n\\s*\n"))
                .map { it.trim() }
                .filter { it.isNotBlank() }

            // Only split if each block looks like a test (has steps)
            if (blocks.all { hasSteps(it) }) {
                return blocks
            }
        }

        // Single test
        return listOf(content)
    }

    /**
     * Check if content block has step-like content.
     */
    private fun hasSteps(content: String): Boolean {
        return content.lines().any { line ->
            val trimmed = line.trim()
            trimmed.startsWith("-") ||
            trimmed.matches(Regex("^\\d+[.)].*")) ||
            STEP_KEYWORDS.any { trimmed.lowercase().startsWith(it) }
        }
    }

    /**
     * Parse a single test block into ParsedTest.
     */
    private fun parseTestBlock(content: String, defaultPlatform: Platform?): ParsedTest? {
        val lines = content.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return null

        var name: String? = null
        var description: String? = null
        var platform: Platform? = defaultPlatform
        var module: String? = null
        var feature: String? = null
        var story: String? = null
        var tags: List<String> = emptyList()
        var appName: String? = null
        var packageName: String? = null
        var bundleId: String? = null
        var baseUrl: String? = null
        val steps = mutableListOf<String>()

        var inStepsSection = false

        for (line in lines) {
            // Skip comments
            if (line.startsWith("#") || line.startsWith("//")) continue

            // Check for metadata lines
            val metadataMatch = parseMetadataLine(line)
            if (metadataMatch != null) {
                val (key, value) = metadataMatch
                when (key.lowercase()) {
                    "test", "name", "test name", "testname" -> name = value
                    "description", "desc" -> description = value
                    "platform", "for", "target" -> platform = parsePlatform(value) ?: platform
                    "module", "mod" -> module = value
                    "feature" -> feature = value
                    "story", "user story", "userstory" -> story = value
                    "tags", "tag", "labels" -> tags = parseTagList(value)
                    "app", "app name", "appname", "application" -> appName = value
                    "package", "package name", "packagename" -> packageName = value
                    "bundle", "bundle id", "bundleid" -> bundleId = value
                    "url", "base url", "baseurl" -> baseUrl = value
                    "steps" -> inStepsSection = true
                }
                continue
            }

            // Check if this is the start of steps section
            if (line.lowercase() == "steps:" || line.lowercase() == "steps") {
                inStepsSection = true
                continue
            }

            // Check for step line formats
            val stepText = parseStepLine(line)
            if (stepText != null) {
                steps.add(stepText)
                inStepsSection = true
                continue
            }

            // If we're in steps section, treat any line as a step
            if (inStepsSection && line.isNotBlank()) {
                steps.add(line)
                continue
            }

            // First non-metadata line might be the test name (Format 3)
            if (name == null && !inStepsSection) {
                val (parsedName, parsedPlatform) = parseFirstLine(line)
                name = parsedName
                if (parsedPlatform != null) platform = parsedPlatform
            }
        }

        // Validate we have at least name and steps
        if (name == null || steps.isEmpty()) {
            // Try to infer name from first step if missing
            if (name == null && steps.isNotEmpty()) {
                name = "Unnamed Test"
            }
            if (steps.isEmpty()) {
                logger.warn("No steps found in test block")
                return null
            }
        }

        return ParsedTest(
            name = name!!,
            description = description,
            platform = platform,
            module = module,
            feature = feature,
            story = story,
            tags = tags,
            appName = appName,
            packageName = packageName,
            bundleId = bundleId,
            baseUrl = baseUrl,
            steps = steps
        )
    }

    /**
     * Parse metadata line like "Key: Value" or "Key = Value".
     */
    private fun parseMetadataLine(line: String): Pair<String, String>? {
        // Match "Key: Value" or "Key = Value"
        val colonMatch = Regex("^([A-Za-z][A-Za-z0-9 _-]*):\\s*(.+)$").find(line)
        if (colonMatch != null) {
            val (key, value) = colonMatch.destructured
            // Don't match if key looks like a step keyword
            if (!STEP_KEYWORDS.contains(key.lowercase())) {
                return key.trim() to value.trim()
            }
        }

        val equalsMatch = Regex("^([A-Za-z][A-Za-z0-9 _-]*)\\s*=\\s*(.+)$").find(line)
        if (equalsMatch != null) {
            val (key, value) = equalsMatch.destructured
            return key.trim() to value.trim()
        }

        return null
    }

    /**
     * Parse step line (bulleted, numbered, or keyword-prefixed).
     */
    private fun parseStepLine(line: String): String? {
        // Bullet format: "- Click Login"
        if (line.startsWith("-") || line.startsWith("*") || line.startsWith("•")) {
            return line.drop(1).trim()
        }

        // Numbered format: "1. Click Login" or "1) Click Login"
        val numberedMatch = Regex("^\\d+[.)\\s]+(.+)$").find(line)
        if (numberedMatch != null) {
            return numberedMatch.groupValues[1].trim()
        }

        // Checkbox format: "[ ] Click Login" or "[x] Click Login"
        val checkboxMatch = Regex("^\\[[ xX]?]\\s*(.+)$").find(line)
        if (checkboxMatch != null) {
            return checkboxMatch.groupValues[1].trim()
        }

        // Keyword-prefixed format: "Step: Click Login"
        val stepMatch = Regex("^(?:step|action|do|then|when|and|given)\\s*[:\\-]?\\s*(.+)$", RegexOption.IGNORE_CASE).find(line)
        if (stepMatch != null) {
            return stepMatch.groupValues[1].trim()
        }

        // Direct action keywords at start of line
        if (STEP_KEYWORDS.any { line.lowercase().startsWith(it) }) {
            return line
        }

        return null
    }

    /**
     * Parse first line for name and optional platform.
     * Handles formats like "Login Test (Android)" or "Login Test - Web"
     */
    private fun parseFirstLine(line: String): Pair<String, Platform?> {
        // Format: "Name (Platform)"
        val parenMatch = Regex("^(.+?)\\s*\\(([^)]+)\\)\\s*$").find(line)
        if (parenMatch != null) {
            val name = parenMatch.groupValues[1].trim()
            val platform = parsePlatform(parenMatch.groupValues[2])
            return name to platform
        }

        // Format: "Name - Platform" or "Name | Platform"
        val separatorMatch = Regex("^(.+?)\\s*[-|]\\s*(android|ios|web|dotcom)\\s*$", RegexOption.IGNORE_CASE).find(line)
        if (separatorMatch != null) {
            val name = separatorMatch.groupValues[1].trim()
            val platform = parsePlatform(separatorMatch.groupValues[2])
            return name to platform
        }

        return line to null
    }

    /**
     * Parse platform string to Platform enum.
     */
    private fun parsePlatform(value: String): Platform? {
        return when (value.lowercase().trim()) {
            "android", "droid" -> Platform.ANDROID
            "ios", "iphone", "ipad" -> Platform.IOS
            "web", "dotcom", "browser", "desktop" -> Platform.WEB
            else -> null
        }
    }

    /**
     * Parse comma or space separated tag list.
     */
    private fun parseTagList(value: String): List<String> {
        return value.split(Regex("[,;\\s]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    /**
     * Detect platform from file path.
     */
    private fun detectPlatformFromPath(file: File): Platform? {
        val pathParts = file.absolutePath.lowercase().split(File.separator)
        return when {
            pathParts.contains("android") -> Platform.ANDROID
            pathParts.contains("ios") -> Platform.IOS
            pathParts.any { it in listOf("web", "dotcom", "browser", "desktop") } -> Platform.WEB
            else -> null
        }
    }

    /**
     * Convert ParsedTest to TestCase.
     */
    private fun convertToTestCase(parsed: ParsedTest, sourceName: String, index: Int): TestCase {
        val testId = generateTestId(parsed.name, sourceName, index)

        val steps = parsed.steps.mapIndexed { i, instruction ->
            TestStep(
                order = i + 1,
                instruction = instruction.trim()
            )
        }

        return TestCase(
            testId = testId,
            name = parsed.name,
            description = parsed.description,
            platform = parsed.platform,
            appName = parsed.appName,
            packageName = parsed.packageName,
            bundleId = parsed.bundleId,
            baseUrl = parsed.baseUrl,
            steps = steps,
            tags = parsed.tags,
            module = parsed.module
        )
    }

    /**
     * Generate a unique test ID.
     */
    private fun generateTestId(name: String, sourceName: String, index: Int): String {
        val sanitized = name.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(30)

        val suffix = if (index > 0) "_$index" else ""
        val uuid = UUID.randomUUID().toString().take(6)

        return "${sanitized}${suffix}_$uuid"
    }

    companion object {
        /**
         * Keywords that indicate a step line.
         */
        private val STEP_KEYWORDS = setOf(
            "click", "tap", "press", "touch",
            "enter", "type", "input", "fill",
            "verify", "check", "assert", "confirm",
            "wait", "pause", "delay",
            "scroll", "swipe", "drag",
            "select", "choose", "pick",
            "clear", "erase", "delete",
            "launch", "open", "start",
            "close", "quit", "exit",
            "navigate", "go", "back", "forward",
            "long press", "double tap", "double click"
        )

        /**
         * Supported plain English file extensions.
         */
        val SUPPORTED_EXTENSIONS = setOf("test", "txt", "english", "plain")

        /**
         * Check if file is a plain English test file.
         */
        fun isPlainEnglishFile(file: File): Boolean {
            return file.extension.lowercase() in SUPPORTED_EXTENSIONS
        }
    }
}
