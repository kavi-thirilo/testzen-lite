package com.testzen.core

import com.testzen.core.model.Platform
import com.testzen.core.model.TestCase
import com.testzen.core.model.TestStep
import com.testzen.core.parser.PlainEnglishTestParser
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileReader
import java.util.UUID

/**
 * Loads test definitions from YAML and Plain English files.
 *
 * Supports dual-folder structure:
 * ```
 * tests/
 * ├── nlp/                 # Plain English source files
 * │   ├── android/
 * │   │   └── auth/
 * │   │       └── login.test
 * │   ├── ios/
 * │   └── web/
 * │
 * └── yml/                 # Auto-generated YAML files
 *     ├── android/
 *     │   └── auth/
 *     │       └── login.yaml
 *     ├── ios/
 *     └── web/
 * ```
 *
 * Also supports single-folder convention:
 * ```
 * tests/
 * ├── android/     # Android tests (auto-detected)
 * ├── ios/         # iOS tests (auto-detected)
 * ├── web/         # Web tests (auto-detected)
 * └── dotcom/      # Web tests (alias for web)
 * ```
 *
 * Supported file formats:
 * - YAML (.yaml, .yml): Standard TestZen format
 * - Plain English (.test, .txt, .english, .plain, .nlp): Human-readable format
 *
 * Supported YAML format:
 * ```yaml
 * test_id: login_test_001
 * name: "Login Test"
 * description: "Verify user can log in"
 * # platform: android  # Optional if using folder convention
 * app_name: my_app
 *
 * steps:
 *   - "Launch the app"
 *   - "Enter 'demo' in 'Username'"
 *   - "Enter 'password' in 'Password'"
 *   - "Click 'Log In' button"
 *   - "Verify 'Welcome' displayed"
 * ```
 *
 * Supported Plain English format:
 * ```
 * Test: Login Test
 * Platform: Android
 *
 * - Launch the app
 * - Enter 'demo' in Username
 * - Enter 'password' in Password
 * - Click Log In button
 * - Verify 'Welcome' is displayed
 * ```
 */
class TestLoader {
    private val logger = LoggerFactory.getLogger(TestLoader::class.java)
    private val yaml = Yaml()
    private val plainEnglishParser = PlainEnglishTestParser()

    companion object {
        /**
         * Platform folder names (case-insensitive).
         */
        val PLATFORM_FOLDERS = mapOf(
            "android" to Platform.ANDROID,
            "ios" to Platform.IOS,
            "web" to Platform.WEB,
            "dotcom" to Platform.WEB,
            "desktop" to Platform.WEB,
            "browser" to Platform.WEB
        )

        /**
         * Supported YAML extensions.
         */
        val YAML_EXTENSIONS = setOf("yaml", "yml")

        /**
         * Supported plain English extensions.
         */
        val PLAIN_ENGLISH_EXTENSIONS = setOf("test", "txt", "english", "plain", "nlp")

        /**
         * Default NLP folder name in dual-folder structure.
         */
        const val NLP_FOLDER = "nlp"

        /**
         * Default YAML folder name in dual-folder structure.
         */
        const val YML_FOLDER = "yml"

        /**
         * All supported test file extensions.
         */
        val ALL_EXTENSIONS = YAML_EXTENSIONS + PLAIN_ENGLISH_EXTENSIONS

        /**
         * Detect platform from file path based on folder convention.
         *
         * @param file The test file
         * @return Detected platform or null if not determinable
         */
        fun detectPlatformFromPath(file: File): Platform? {
            val pathParts = file.absolutePath.lowercase().split(File.separator)

            // Check each path segment for platform folder names
            for (part in pathParts) {
                PLATFORM_FOLDERS[part]?.let { return it }
            }

            return null
        }

        /**
         * Detect platform from a directory path.
         *
         * @param path The directory path
         * @return Detected platform or null if not determinable
         */
        fun detectPlatformFromPath(path: String): Platform? {
            val normalizedPath = path.lowercase().replace('\\', '/')
            val parts = normalizedPath.split('/')

            for (part in parts) {
                PLATFORM_FOLDERS[part]?.let { return it }
            }

            return null
        }

        /**
         * Check if file is a supported test file.
         */
        fun isTestFile(file: File): Boolean {
            return file.isFile && file.extension.lowercase() in ALL_EXTENSIONS
        }

        /**
         * Check if file is a YAML test file.
         */
        fun isYamlFile(file: File): Boolean {
            return file.isFile && file.extension.lowercase() in YAML_EXTENSIONS
        }

        /**
         * Check if file is a plain English test file.
         */
        fun isPlainEnglishFile(file: File): Boolean {
            return file.isFile && file.extension.lowercase() in PLAIN_ENGLISH_EXTENSIONS
        }
    }

    /**
     * Load tests from any supported file format (YAML or Plain English).
     *
     * @param file The test file to load
     * @return List of TestCase objects
     */
    fun loadFromFile(file: File): List<TestCase> {
        require(file.exists()) { "File does not exist: ${file.absolutePath}" }

        val extension = file.extension.lowercase()
        require(extension in ALL_EXTENSIONS) {
            "Unsupported file format: ${file.name}. Supported: ${ALL_EXTENSIONS.joinToString(", ")}"
        }

        return when {
            extension in YAML_EXTENSIONS -> loadFromYamlFile(file)
            extension in PLAIN_ENGLISH_EXTENSIONS -> loadFromPlainEnglishFile(file)
            else -> throw IllegalArgumentException("Unsupported file: ${file.name}")
        }
    }

    /**
     * Load tests from a YAML file with automatic platform detection.
     *
     * @param file The YAML file to load
     * @return List of TestCase objects
     */
    fun loadFromYamlFile(file: File): List<TestCase> {
        require(file.exists()) { "File does not exist: ${file.absolutePath}" }
        require(file.extension.lowercase() in YAML_EXTENSIONS) {
            "File must be YAML: ${file.name}"
        }

        // Detect platform from folder structure
        val detectedPlatform = detectPlatformFromPath(file)

        return FileReader(file).use { reader ->
            val documents = yaml.loadAll(reader)
            documents.mapNotNull { doc ->
                try {
                    parseTestCase(doc, file.nameWithoutExtension, detectedPlatform)
                } catch (e: Exception) {
                    logger.warn("Failed to parse test in ${file.name}: ${e.message}")
                    null
                }
            }.toList()
        }
    }

    /**
     * Load tests from a plain English file.
     *
     * @param file The plain English test file
     * @return List of TestCase objects
     */
    fun loadFromPlainEnglishFile(file: File): List<TestCase> {
        require(file.exists()) { "File does not exist: ${file.absolutePath}" }

        logger.debug("Loading plain English tests from ${file.name}")
        return plainEnglishParser.parseFile(file)
    }

    /**
     * Load all tests from a directory (recursive).
     *
     * @param directory The directory to scan
     * @param recursive Whether to scan subdirectories
     * @return List of all TestCase objects found
     */
    fun loadFromDirectory(directory: File, recursive: Boolean = true): List<TestCase> {
        require(directory.exists() && directory.isDirectory) {
            "Directory does not exist: ${directory.absolutePath}"
        }

        val testFiles = if (recursive) {
            directory.walkTopDown()
                .filter { isTestFile(it) }
                .toList()
        } else {
            directory.listFiles()
                ?.filter { isTestFile(it) }
                ?: emptyList()
        }

        logger.info("Found ${testFiles.size} test files in ${directory.absolutePath}")

        return testFiles.flatMap { file ->
            try {
                loadFromFile(file)
            } catch (e: Exception) {
                logger.warn("Failed to load tests from ${file.name}: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * Load tests from dual-folder structure (nlp/ and yml/).
     *
     * Prioritizes yml/ folder for faster loading. Falls back to nlp/ if yml not present.
     *
     * @param baseDir Base directory containing nlp/ and yml/ folders
     * @param preferYml Load from yml/ first (default true), or from nlp/ if false
     * @param recursive Whether to scan subdirectories
     * @return List of all TestCase objects found
     */
    fun loadFromDualFolder(
        baseDir: File,
        preferYml: Boolean = true,
        recursive: Boolean = true
    ): List<TestCase> {
        val nlpDir = File(baseDir, NLP_FOLDER)
        val ymlDir = File(baseDir, YML_FOLDER)

        val sourceDir = when {
            preferYml && ymlDir.exists() && ymlDir.isDirectory -> ymlDir
            nlpDir.exists() && nlpDir.isDirectory -> nlpDir
            ymlDir.exists() && ymlDir.isDirectory -> ymlDir
            else -> {
                logger.warn("Neither nlp/ nor yml/ folder found in ${baseDir.absolutePath}")
                return emptyList()
            }
        }

        logger.info("Loading tests from dual-folder structure: ${sourceDir.name}/")
        return loadFromDirectory(sourceDir, recursive)
    }

    /**
     * Load tests for a specific platform from dual-folder structure.
     *
     * @param baseDir Base directory containing nlp/ and yml/ folders
     * @param platform Target platform
     * @param preferYml Load from yml/ first (default true)
     * @return List of TestCase objects for the platform
     */
    fun loadFromDualFolderByPlatform(
        baseDir: File,
        platform: Platform,
        preferYml: Boolean = true
    ): List<TestCase> {
        val nlpDir = File(baseDir, NLP_FOLDER)
        val ymlDir = File(baseDir, YML_FOLDER)

        val sourceDir = if (preferYml && ymlDir.exists()) ymlDir else nlpDir
        val platformDir = File(sourceDir, platform.name.lowercase())

        if (!platformDir.exists() || !platformDir.isDirectory) {
            logger.warn("Platform folder not found: ${platformDir.absolutePath}")
            return emptyList()
        }

        return loadFromDirectory(platformDir, recursive = true)
    }

    /**
     * Load tests for a specific module from dual-folder structure.
     *
     * @param baseDir Base directory containing nlp/ and yml/ folders
     * @param moduleId Module identifier
     * @param platform Target platform (optional)
     * @param preferYml Load from yml/ first (default true)
     * @return List of TestCase objects for the module
     */
    fun loadFromDualFolderByModule(
        baseDir: File,
        moduleId: String,
        platform: Platform? = null,
        preferYml: Boolean = true
    ): List<TestCase> {
        val nlpDir = File(baseDir, NLP_FOLDER)
        val ymlDir = File(baseDir, YML_FOLDER)

        val sourceDir = if (preferYml && ymlDir.exists()) ymlDir else nlpDir

        val moduleDir = if (platform != null) {
            File(sourceDir, "${platform.name.lowercase()}/$moduleId")
        } else {
            // Search for module across all platforms
            Platform.values()
                .map { File(sourceDir, "${it.name.lowercase()}/$moduleId") }
                .firstOrNull { it.exists() && it.isDirectory }
                ?: File(sourceDir, moduleId)
        }

        if (!moduleDir.exists() || !moduleDir.isDirectory) {
            logger.warn("Module folder not found: ${moduleDir.absolutePath}")
            return emptyList()
        }

        return loadFromDirectory(moduleDir, recursive = true)
    }

    /**
     * Check if a directory uses dual-folder structure.
     *
     * @param baseDir Directory to check
     * @return true if both nlp/ and yml/ exist (or just nlp/)
     */
    fun isDualFolderStructure(baseDir: File): Boolean {
        val nlpDir = File(baseDir, NLP_FOLDER)
        val ymlDir = File(baseDir, YML_FOLDER)
        return nlpDir.exists() || ymlDir.exists()
    }

    /**
     * Load tests intelligently - auto-detects single or dual folder structure.
     *
     * @param testsDir The tests directory
     * @param recursive Whether to scan subdirectories
     * @return List of all TestCase objects found
     */
    fun loadAuto(testsDir: File, recursive: Boolean = true): List<TestCase> {
        return if (isDualFolderStructure(testsDir)) {
            logger.info("Detected dual-folder structure in ${testsDir.absolutePath}")
            loadFromDualFolder(testsDir, preferYml = true, recursive = recursive)
        } else {
            logger.info("Using standard folder structure in ${testsDir.absolutePath}")
            loadFromDirectory(testsDir, recursive = recursive)
        }
    }

    /**
     * Load a test from YAML content string.
     *
     * @param yamlContent YAML content as a string
     * @param platform Optional platform override
     * @return TestCase object
     */
    fun loadFromYaml(yamlContent: String, platform: Platform? = null): TestCase {
        val doc = yaml.load<Any>(yamlContent)
        return parseTestCase(doc, "inline", platform)
            ?: throw IllegalArgumentException("Failed to parse YAML content")
    }

    /**
     * Load multiple tests from YAML content string (multi-document).
     */
    fun loadAllFromYaml(yamlContent: String, platform: Platform? = null): List<TestCase> {
        val documents = yaml.loadAll(yamlContent)
        return documents.mapNotNull { doc ->
            try {
                parseTestCase(doc, "inline", platform)
            } catch (e: Exception) {
                logger.warn("Failed to parse test: ${e.message}")
                null
            }
        }.toList()
    }

    /**
     * Load tests from plain English content string.
     *
     * @param content Plain English test content
     * @param platform Optional platform override
     * @return List of TestCase objects
     */
    fun loadFromPlainEnglish(content: String, platform: Platform? = null): List<TestCase> {
        return plainEnglishParser.parseAll(content, platform, "inline")
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseTestCase(doc: Any?, sourceName: String, detectedPlatform: Platform? = null): TestCase? {
        if (doc !is Map<*, *>) return null

        val data = doc as Map<String, Any?>

        // Parse test ID (required or generate)
        val testId = data["test_id"]?.toString()
            ?: data["id"]?.toString()
            ?: "${sourceName}_${UUID.randomUUID().toString().take(8)}"

        // Parse name (required)
        val name = data["name"]?.toString()
            ?: throw IllegalArgumentException("Test name is required")

        // Parse description (optional)
        val description = data["description"]?.toString()

        // Parse platform: YAML definition takes priority, then folder detection
        val platformStr = data["platform"]?.toString()
        val platform = platformStr?.let {
            try {
                Platform.valueOf(it.uppercase())
            } catch (e: Exception) {
                logger.warn("Unknown platform: $it, using detected platform or null")
                detectedPlatform
            }
        } ?: detectedPlatform

        // Parse app info
        val appName = data["app_name"]?.toString()
        val packageName = data["package_name"]?.toString()
        val bundleId = data["bundle_id"]?.toString()
        val baseUrl = data["base_url"]?.toString()

        // Parse steps (required)
        val stepsData = data["steps"]
            ?: throw IllegalArgumentException("Test steps are required")

        val steps = when (stepsData) {
            is List<*> -> stepsData.mapIndexedNotNull { index, step ->
                parseStep(step, index + 1)
            }
            else -> throw IllegalArgumentException("Steps must be a list")
        }

        if (steps.isEmpty()) {
            throw IllegalArgumentException("Test must have at least one step")
        }

        // Parse tags (optional)
        val tags = (data["tags"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

        // Parse module (optional)
        val module = data["module"]?.toString()

        return TestCase(
            testId = testId,
            name = name,
            description = description,
            platform = platform,
            appName = appName,
            packageName = packageName,
            bundleId = bundleId,
            baseUrl = baseUrl,
            steps = steps,
            tags = tags,
            module = module
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseStep(stepData: Any?, order: Int): TestStep? {
        return when (stepData) {
            // Simple string step: "Click the login button"
            is String -> TestStep(
                order = order,
                instruction = stepData,
                description = null
            )

            // Detailed step with metadata
            is Map<*, *> -> {
                val data = stepData as Map<String, Any?>
                val instruction = data["instruction"]?.toString()
                    ?: data["step"]?.toString()
                    ?: data["action"]?.toString()
                    ?: return null

                TestStep(
                    order = order,
                    instruction = instruction,
                    description = data["description"]?.toString(),
                    screenshot = data["screenshot"] as? Boolean ?: false,
                    optional = data["optional"] as? Boolean ?: false,
                    timeout = (data["timeout"] as? Number)?.toLong()
                )
            }

            else -> null
        }
    }
}
