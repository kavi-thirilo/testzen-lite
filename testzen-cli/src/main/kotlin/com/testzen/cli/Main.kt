package com.testzen.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int
import com.testzen.core.TestLoader
import com.testzen.core.TestZenRunner
import com.testzen.core.config.TestZenConfig
import com.testzen.core.model.Platform
import com.testzen.core.model.TestResult
import com.testzen.core.model.TestStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * TestZen CLI - Command-line interface for no-code test automation.
 *
 * Supports folder-based platform convention:
 * ```
 * tests/
 * ├── android/     # Android tests (auto-detected)
 * ├── ios/         # iOS tests (auto-detected)
 * ├── web/         # Web tests (auto-detected)
 * └── dotcom/      # Web tests (alias)
 * ```
 *
 * Usage:
 *   # Auto-detect platform from folder structure
 *   java -jar testzen-cli.jar run --tests ./tests/android
 *   java -jar testzen-cli.jar run --tests ./tests/ios
 *   java -jar testzen-cli.jar run --tests ./tests/dotcom
 *
 *   # Explicit platform (overrides folder detection)
 *   java -jar testzen-cli.jar run --tests ./tests --platform android
 */
fun main(args: Array<String>) = TestZenCli()
    .subcommands(RunCommand(), VersionCommand(), ValidateCommand())
    .main(args)

class TestZenCli : CliktCommand(
    name = "testzen",
    help = """
        TestZen - Lightweight No-Code Test Automation Framework

        Run automated tests defined in YAML files against mobile and web applications.

        Platform Detection:
          Tests are auto-detected based on folder structure:
          • tests/android/  → Android platform
          • tests/ios/      → iOS platform
          • tests/web/      → Web platform
          • tests/dotcom/   → Web platform (alias)

        Examples:
          testzen run --tests ./tests/android
          testzen run --tests ./tests/ios --device iPhone-14
          testzen run --tests ./tests/dotcom --headless
          testzen run --tests ./tests --platform android  (explicit)
          testzen validate --tests ./tests
    """.trimIndent()
) {
    override fun run() = Unit
}

class RunCommand : CliktCommand(
    name = "run",
    help = "Execute test cases"
) {
    private val testFile by option("--test", "-t", help = "Single test file to run")
    private val testsDir by option("--tests", "-T", help = "Directory containing test files")
    private val platform by option("--platform", "-p", help = "Target platform (auto-detected from folder if not specified)")
        .enum<Platform>(ignoreCase = true)
    private val deviceId by option("--device", "-d", help = "Device/emulator ID")
    private val appiumUrl by option("--appium", help = "Appium server URL")
        .default("http://127.0.0.1:4723")
    private val configFile by option("--config", "-c", help = "Configuration file (YAML/JSON)")
    private val outputDir by option("--output", "-o", help = "Output directory for results")
        .default("./results")
    private val outputFormat by option("--format", "-f", help = "Output format (json, text)")
        .default("json")
    private val headless by option("--headless", help = "Run browser in headless mode").flag()
    private val browser by option("--browser", "-b", help = "Browser type for web testing")
        .default("chrome")
    private val verbose by option("--verbose", "-v", help = "Verbose output").flag()
    private val retries by option("--retries", "-r", help = "Number of retries for failed steps")
        .int()
        .default(2)

    override fun run() {
        // Validate input
        if (testFile == null && testsDir == null) {
            echo("Error: Either --test or --tests must be specified", err = true)
            return
        }

        printBanner()

        val testsPath = testFile ?: testsDir!!

        // Auto-detect platform from path if not explicitly specified
        val detectedPlatform = platform ?: TestLoader.detectPlatformFromPath(testsPath)

        if (detectedPlatform == null) {
            echo("Error: Could not detect platform. Either:", err = true)
            echo("  1. Use folder convention: tests/android/, tests/ios/, tests/web/, tests/dotcom/", err = true)
            echo("  2. Specify platform explicitly: --platform android", err = true)
            echo("", err = true)
            echo("Supported platform folders: ${TestLoader.PLATFORM_FOLDERS.keys.joinToString(", ")}", err = true)
            return
        }

        if (platform == null) {
            echo("Auto-detected platform: $detectedPlatform (from path: $testsPath)")
        }

        // Load configuration
        val config = loadConfig()

        // Create runner
        val runner = TestZenRunner(config)

        // Load tests
        echo("Loading tests from: $testsPath")

        val testCount = try {
            runner.loadTests(testsPath)
        } catch (e: Exception) {
            echo("Error loading tests: ${e.message}", err = true)
            return
        }

        if (testCount == 0) {
            echo("No tests found in $testsPath", err = true)
            return
        }

        echo("Loaded $testCount test(s)")
        echo("")

        // Execute tests
        echo("═══════════════════════════════════════════════════════════════")
        echo("Executing on: $detectedPlatform" + (deviceId?.let { " ($it)" } ?: ""))
        echo("═══════════════════════════════════════════════════════════════")
        echo("")

        val results = try {
            runner.execute(detectedPlatform, deviceId, appiumUrl)
        } catch (e: Exception) {
            echo("Execution failed: ${e.message}", err = true)
            if (verbose) {
                e.printStackTrace()
            }
            return
        }

        // Print results
        printResults(results)

        // Save results
        saveResults(results)

        // Exit with appropriate code
        val failedCount = results.count { it.status != TestStatus.PASSED }
        if (failedCount > 0) {
            throw RuntimeException("$failedCount test(s) failed")
        }
    }

    private fun loadConfig(): TestZenConfig {
        val baseConfig = configFile?.let {
            try {
                TestZenConfig.fromFile(it)
            } catch (e: Exception) {
                echo("Warning: Failed to load config file: ${e.message}")
                TestZenConfig()
            }
        } ?: TestZenConfig()

        // Override with CLI options
        return baseConfig.copy(
            headless = headless || baseConfig.headless,
            browserType = browser,
            retryFailedSteps = retries,
            outputDirectory = outputDir,
            outputFormat = outputFormat
        )
    }

    private fun printResults(results: List<TestResult>) {
        echo("")
        echo("═══════════════════════════════════════════════════════════════")
        echo("RESULTS")
        echo("═══════════════════════════════════════════════════════════════")

        for (result in results) {
            val statusIcon = when (result.status) {
                TestStatus.PASSED -> "✓"
                TestStatus.FAILED -> "✗"
                TestStatus.SKIPPED -> "○"
                TestStatus.ERROR -> "⚠"
            }
            val statusColor = when (result.status) {
                TestStatus.PASSED -> "\u001B[32m"  // Green
                TestStatus.FAILED -> "\u001B[31m"  // Red
                TestStatus.SKIPPED -> "\u001B[33m" // Yellow
                TestStatus.ERROR -> "\u001B[31m"   // Red
            }
            val reset = "\u001B[0m"

            echo("$statusColor$statusIcon ${result.status}$reset: ${result.testName} (${result.durationMs}ms)")

            if (result.status != TestStatus.PASSED && result.errorMessage != null) {
                echo("  └─ ${result.errorMessage}")
            }

            if (verbose) {
                echo("  Steps: ${result.passedSteps} passed, ${result.failedSteps} failed, ${result.skippedSteps} skipped")
            }
        }

        echo("")
        echo("───────────────────────────────────────────────────────────────")

        val passed = results.count { it.status == TestStatus.PASSED }
        val failed = results.count { it.status == TestStatus.FAILED }
        val skipped = results.count { it.status == TestStatus.SKIPPED }
        val errors = results.count { it.status == TestStatus.ERROR }
        val totalTime = results.sumOf { it.durationMs }

        echo("Total: ${results.size} | Passed: $passed | Failed: $failed | Skipped: $skipped | Errors: $errors")
        echo("Duration: ${totalTime}ms")
        echo("")
    }

    private fun saveResults(results: List<TestResult>) {
        val outputDirFile = File(outputDir)
        if (!outputDirFile.exists()) {
            outputDirFile.mkdirs()
        }

        when (outputFormat.lowercase()) {
            "json" -> {
                val json = Json {
                    prettyPrint = true
                    encodeDefaults = true
                }
                val jsonContent = json.encodeToString(results)
                val outputFile = File(outputDirFile, "results.json")
                outputFile.writeText(jsonContent)
                echo("Results saved to: ${outputFile.absolutePath}")
            }
            "text" -> {
                val outputFile = File(outputDirFile, "results.txt")
                outputFile.writeText(buildString {
                    appendLine("TestZen Execution Results")
                    appendLine("═".repeat(60))
                    appendLine()
                    for (result in results) {
                        appendLine("Test: ${result.testName}")
                        appendLine("Status: ${result.status}")
                        appendLine("Duration: ${result.durationMs}ms")
                        result.errorMessage?.let { appendLine("Error: $it") }
                        appendLine()
                    }
                })
                echo("Results saved to: ${outputFile.absolutePath}")
            }
        }
    }

    private fun printBanner() {
        echo("""

  ╔════════════════════════════════════════════════════════════╗
  ║                                                            ║
  ║   ████████╗███████╗███████╗████████╗███████╗███████╗███╗   ║
  ║   ╚══██╔══╝██╔════╝██╔════╝╚══██╔══╝╚══███╔╝██╔════╝████╗  ║
  ║      ██║   █████╗  ███████╗   ██║     ███╔╝ █████╗  ██╔██╗ ║
  ║      ██║   ██╔══╝  ╚════██║   ██║    ███╔╝  ██╔══╝  ██║╚██╗║
  ║      ██║   ███████╗███████║   ██║   ███████╗███████╗██║ ╚██║
  ║      ╚═╝   ╚══════╝╚══════╝   ╚═╝   ╚══════╝╚══════╝╚═╝  ╚═╝
  ║                                                            ║
  ║   Lightweight No-Code Test Automation Framework            ║
  ║   Version 1.0.0                                            ║
  ║                                                            ║
  ╚════════════════════════════════════════════════════════════╝
        """.trimIndent())
        echo("")
    }
}

class VersionCommand : CliktCommand(
    name = "version",
    help = "Show version information"
) {
    override fun run() {
        echo("TestZen CLI v1.0.0")
        echo("Core Library v1.0.0")
        echo("")
        echo("Kotlin: ${KotlinVersion.CURRENT}")
        echo("Java: ${System.getProperty("java.version")}")
        echo("")
        echo("Supported platform folders:")
        TestLoader.PLATFORM_FOLDERS.forEach { (folder, platform) ->
            echo("  $folder/ → $platform")
        }
    }
}

class ValidateCommand : CliktCommand(
    name = "validate",
    help = "Validate test files without executing"
) {
    private val testFile by option("--test", "-t", help = "Single test file to validate")
    private val testsDir by option("--tests", "-T", help = "Directory containing test files")

    override fun run() {
        if (testFile == null && testsDir == null) {
            echo("Error: Either --test or --tests must be specified", err = true)
            return
        }

        val runner = TestZenRunner()
        val testsPath = testFile ?: testsDir!!

        echo("Validating tests in: $testsPath")
        echo("")

        // Show detected platform
        val detectedPlatform = TestLoader.detectPlatformFromPath(testsPath)
        if (detectedPlatform != null) {
            echo("Detected platform from path: $detectedPlatform")
            echo("")
        }

        try {
            val testCount = runner.loadTests(testsPath)
            val tests = runner.getLoadedTests()

            echo("✓ Successfully validated $testCount test(s)")
            echo("")

            // Group tests by platform
            val byPlatform = tests.groupBy { it.platform }

            for ((platform, platformTests) in byPlatform) {
                echo("Platform: ${platform ?: "unspecified"} (${platformTests.size} tests)")
                for (test in platformTests) {
                    echo("  • ${test.testId}: ${test.name}")
                    echo("    Steps: ${test.steps.size}")
                }
                echo("")
            }
        } catch (e: Exception) {
            echo("✗ Validation failed: ${e.message}", err = true)
        }
    }
}
