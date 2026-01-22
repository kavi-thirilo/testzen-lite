package com.testzen.core.parser

import com.testzen.core.model.Platform
import com.testzen.core.model.TestCase
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Organizes tests into module-based folder structures.
 *
 * Supports creating and managing test hierarchies:
 * ```
 * tests/
 * ├── android/                    # Platform folder
 * │   ├── auth/                   # Module folder
 * │   │   ├── login/              # Feature folder
 * │   │   │   ├── basic_login.yaml
 * │   │   │   └── social_login.yaml
 * │   │   └── registration/
 * │   │       └── signup.yaml
 * │   └── payments/
 * │       └── checkout.yaml
 * ├── ios/
 * │   └── auth/
 * │       └── login/
 * └── web/
 *     └── auth/
 * ```
 *
 * Usage:
 * ```kotlin
 * val organizer = ModuleOrganizer(baseDir = File("tests"))
 *
 * // Create module structure
 * organizer.createModule("auth", Platform.ANDROID)
 * organizer.createFeature("auth", "login", Platform.ANDROID)
 *
 * // Organize tests by metadata
 * organizer.organizeTest(testCase) // Uses test's module/platform
 *
 * // Get module structure
 * val structure = organizer.getStructure()
 * ```
 */
class ModuleOrganizer(
    private val baseDir: File,
    private val config: OrganizerConfig = OrganizerConfig()
) {
    private val logger = LoggerFactory.getLogger(ModuleOrganizer::class.java)
    private val testWriter = TestWriter()

    /**
     * Configuration for test organization.
     */
    data class OrganizerConfig(
        /** Use platform folders at top level */
        val platformFolders: Boolean = true,
        /** Create feature subfolders under modules */
        val featureFolders: Boolean = true,
        /** Create story subfolders under features */
        val storyFolders: Boolean = false,
        /** File naming convention */
        val fileNaming: FileNaming = FileNaming.SNAKE_CASE,
        /** Whether to auto-create directories */
        val autoCreate: Boolean = true,
        /** YAML configuration */
        val yamlConfig: TestWriter.YamlConfig = TestWriter.YamlConfig()
    )

    enum class FileNaming {
        /** test_name.yaml */
        SNAKE_CASE,
        /** testName.yaml */
        CAMEL_CASE,
        /** test-name.yaml */
        KEBAB_CASE,
        /** Use test ID: test_123.yaml */
        TEST_ID
    }

    /**
     * Module information.
     */
    data class ModuleInfo(
        val id: String,
        val name: String,
        val path: File,
        val platform: Platform?,
        val features: List<FeatureInfo> = emptyList(),
        val testCount: Int = 0
    )

    /**
     * Feature information.
     */
    data class FeatureInfo(
        val id: String,
        val name: String,
        val path: File,
        val stories: List<StoryInfo> = emptyList(),
        val testCount: Int = 0
    )

    /**
     * Story information.
     */
    data class StoryInfo(
        val id: String,
        val name: String,
        val path: File,
        val testCount: Int = 0
    )

    /**
     * Overall test structure.
     */
    data class TestStructure(
        val baseDir: File,
        val platforms: Map<Platform, List<ModuleInfo>>,
        val totalModules: Int,
        val totalFeatures: Int,
        val totalTests: Int
    )

    init {
        if (config.autoCreate) {
            baseDir.mkdirs()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // STRUCTURE CREATION
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Create a new module folder.
     *
     * @param moduleId Module identifier (e.g., "auth", "payments")
     * @param platform Target platform (optional, creates under platform folder)
     * @return Created directory
     */
    fun createModule(moduleId: String, platform: Platform? = null): File {
        val modulePath = getModulePath(moduleId, platform)
        modulePath.mkdirs()
        logger.info("Created module: ${modulePath.relativeTo(baseDir)}")
        return modulePath
    }

    /**
     * Create a feature folder under a module.
     *
     * @param moduleId Parent module
     * @param featureId Feature identifier
     * @param platform Target platform
     * @return Created directory
     */
    fun createFeature(moduleId: String, featureId: String, platform: Platform? = null): File {
        val featurePath = getFeaturePath(moduleId, featureId, platform)
        featurePath.mkdirs()
        logger.info("Created feature: ${featurePath.relativeTo(baseDir)}")
        return featurePath
    }

    /**
     * Create a story folder under a feature.
     */
    fun createStory(
        moduleId: String,
        featureId: String,
        storyId: String,
        platform: Platform? = null
    ): File {
        val storyPath = getStoryPath(moduleId, featureId, storyId, platform)
        storyPath.mkdirs()
        logger.info("Created story: ${storyPath.relativeTo(baseDir)}")
        return storyPath
    }

    /**
     * Create full hierarchy for a test based on its metadata.
     */
    fun createHierarchy(testCase: TestCase): File {
        val module = testCase.module ?: "general"
        val platform = testCase.platform

        return createModule(module, platform)
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PATH RESOLUTION
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Get the path for a module.
     */
    fun getModulePath(moduleId: String, platform: Platform? = null): File {
        val sanitizedModule = sanitizeName(moduleId)

        return if (config.platformFolders && platform != null) {
            File(baseDir, "${platform.name.lowercase()}/$sanitizedModule")
        } else {
            File(baseDir, sanitizedModule)
        }
    }

    /**
     * Get the path for a feature.
     */
    fun getFeaturePath(moduleId: String, featureId: String, platform: Platform? = null): File {
        val modulePath = getModulePath(moduleId, platform)
        val sanitizedFeature = sanitizeName(featureId)

        return if (config.featureFolders) {
            File(modulePath, sanitizedFeature)
        } else {
            modulePath
        }
    }

    /**
     * Get the path for a story.
     */
    fun getStoryPath(
        moduleId: String,
        featureId: String,
        storyId: String,
        platform: Platform? = null
    ): File {
        val featurePath = getFeaturePath(moduleId, featureId, platform)
        val sanitizedStory = sanitizeName(storyId)

        return if (config.storyFolders) {
            File(featurePath, sanitizedStory)
        } else {
            featurePath
        }
    }

    /**
     * Get the target directory for a test based on its metadata.
     */
    fun getTestDirectory(testCase: TestCase): File {
        val module = testCase.module ?: "general"
        return getModulePath(module, testCase.platform)
    }

    /**
     * Get the full file path for a test.
     */
    fun getTestFilePath(testCase: TestCase): File {
        val directory = getTestDirectory(testCase)
        val fileName = generateFileName(testCase)
        return File(directory, fileName)
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // TEST FILE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Organize and write a test to the appropriate location.
     *
     * @param testCase Test to organize
     * @return Written file path
     */
    fun organizeTest(testCase: TestCase): File {
        val directory = getTestDirectory(testCase)
        if (config.autoCreate) {
            directory.mkdirs()
        }

        val filePath = getTestFilePath(testCase)
        testWriter.writeYaml(testCase, filePath, config.yamlConfig)

        logger.info("Organized test '${testCase.name}' to ${filePath.relativeTo(baseDir)}")
        return filePath
    }

    /**
     * Organize multiple tests.
     *
     * @param testCases Tests to organize
     * @return Map of test ID to file path
     */
    fun organizeTests(testCases: List<TestCase>): Map<String, File> {
        return testCases.associate { testCase ->
            testCase.testId to organizeTest(testCase)
        }
    }

    /**
     * Convert plain English file and organize resulting tests.
     *
     * @param plainEnglishFile Source plain English test file
     * @return List of created YAML files
     */
    fun convertAndOrganize(plainEnglishFile: File): List<File> {
        val parser = PlainEnglishTestParser()
        val testCases = parser.parseFile(plainEnglishFile)

        return testCases.map { organizeTest(it) }
    }

    /**
     * Convert plain English content and organize tests.
     *
     * @param content Plain English test content
     * @param defaultPlatform Default platform for tests
     * @return List of created YAML files
     */
    fun convertAndOrganize(content: String, defaultPlatform: Platform? = null): List<File> {
        val parser = PlainEnglishTestParser()
        val testCases = parser.parseAll(content, defaultPlatform, "converted")

        return testCases.map { organizeTest(it) }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // STRUCTURE ANALYSIS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Get the current test structure.
     */
    fun getStructure(): TestStructure {
        val platforms = mutableMapOf<Platform, MutableList<ModuleInfo>>()
        var totalModules = 0
        var totalFeatures = 0
        var totalTests = 0

        // Scan platform folders
        if (config.platformFolders) {
            Platform.values().forEach { platform ->
                val platformDir = File(baseDir, platform.name.lowercase())
                if (platformDir.exists() && platformDir.isDirectory) {
                    val modules = scanModules(platformDir, platform)
                    if (modules.isNotEmpty()) {
                        platforms[platform] = modules.toMutableList()
                        totalModules += modules.size
                        modules.forEach { module ->
                            totalFeatures += module.features.size
                            totalTests += module.testCount
                        }
                    }
                }
            }
        } else {
            // Scan root for modules directly
            val modules = scanModules(baseDir, null)
            if (modules.isNotEmpty()) {
                // Put under WEB as default
                platforms[Platform.WEB] = modules.toMutableList()
                totalModules = modules.size
                modules.forEach { module ->
                    totalFeatures += module.features.size
                    totalTests += module.testCount
                }
            }
        }

        return TestStructure(
            baseDir = baseDir,
            platforms = platforms,
            totalModules = totalModules,
            totalFeatures = totalFeatures,
            totalTests = totalTests
        )
    }

    /**
     * Scan directory for modules.
     */
    private fun scanModules(dir: File, platform: Platform?): List<ModuleInfo> {
        return dir.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.map { moduleDir ->
                val features = if (config.featureFolders) {
                    scanFeatures(moduleDir)
                } else {
                    emptyList()
                }

                val testCount = countTestFiles(moduleDir)

                ModuleInfo(
                    id = moduleDir.name,
                    name = moduleDir.name.replace("_", " ").replaceFirstChar { it.uppercase() },
                    path = moduleDir,
                    platform = platform,
                    features = features,
                    testCount = testCount
                )
            }
            ?.sortedBy { it.id }
            ?: emptyList()
    }

    /**
     * Scan directory for features.
     */
    private fun scanFeatures(moduleDir: File): List<FeatureInfo> {
        return moduleDir.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.map { featureDir ->
                val stories = if (config.storyFolders) {
                    scanStories(featureDir)
                } else {
                    emptyList()
                }

                val testCount = countTestFiles(featureDir)

                FeatureInfo(
                    id = featureDir.name,
                    name = featureDir.name.replace("_", " ").replaceFirstChar { it.uppercase() },
                    path = featureDir,
                    stories = stories,
                    testCount = testCount
                )
            }
            ?.sortedBy { it.id }
            ?: emptyList()
    }

    /**
     * Scan directory for stories.
     */
    private fun scanStories(featureDir: File): List<StoryInfo> {
        return featureDir.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.map { storyDir ->
                StoryInfo(
                    id = storyDir.name,
                    name = storyDir.name.replace("_", " ").replaceFirstChar { it.uppercase() },
                    path = storyDir,
                    testCount = countTestFiles(storyDir)
                )
            }
            ?.sortedBy { it.id }
            ?: emptyList()
    }

    /**
     * Count test files in a directory (recursive).
     */
    private fun countTestFiles(dir: File): Int {
        return dir.walkTopDown()
            .filter { it.isFile && isTestFile(it) }
            .count()
    }

    /**
     * Check if file is a test file.
     */
    private fun isTestFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in listOf("yaml", "yml", "test", "txt", "english")
    }

    /**
     * Print the current structure to console.
     */
    fun printStructure() {
        val structure = getStructure()
        println("Test Structure: ${baseDir.absolutePath}")
        println("═".repeat(60))
        println("Total: ${structure.totalModules} modules, ${structure.totalFeatures} features, ${structure.totalTests} tests")
        println()

        structure.platforms.forEach { (platform, modules) ->
            println("📱 ${platform.name}")
            modules.forEach { module ->
                println("  📁 ${module.name} (${module.testCount} tests)")
                module.features.forEach { feature ->
                    println("    📂 ${feature.name} (${feature.testCount} tests)")
                    feature.stories.forEach { story ->
                        println("      📄 ${story.name} (${story.testCount} tests)")
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Sanitize name for use as folder/file name.
     */
    private fun sanitizeName(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }

    /**
     * Generate filename for a test based on naming convention.
     */
    private fun generateFileName(testCase: TestCase): String {
        val baseName = when (config.fileNaming) {
            FileNaming.SNAKE_CASE -> testCase.name.lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')

            FileNaming.CAMEL_CASE -> testCase.name
                .split(Regex("[^a-zA-Z0-9]+"))
                .filter { it.isNotBlank() }
                .mapIndexed { i, word ->
                    if (i == 0) word.lowercase()
                    else word.replaceFirstChar { it.uppercase() }
                }
                .joinToString("")

            FileNaming.KEBAB_CASE -> testCase.name.lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')

            FileNaming.TEST_ID -> testCase.testId
        }

        return "${baseName.take(50)}.yaml"
    }

    companion object {
        /**
         * Create an organizer with standard conventions.
         */
        fun standard(baseDir: File): ModuleOrganizer {
            return ModuleOrganizer(
                baseDir = baseDir,
                config = OrganizerConfig(
                    platformFolders = true,
                    featureFolders = true,
                    storyFolders = false
                )
            )
        }

        /**
         * Create an organizer for flat structure (no nested folders).
         */
        fun flat(baseDir: File): ModuleOrganizer {
            return ModuleOrganizer(
                baseDir = baseDir,
                config = OrganizerConfig(
                    platformFolders = true,
                    featureFolders = false,
                    storyFolders = false
                )
            )
        }

        /**
         * Create an organizer with full hierarchy (module/feature/story).
         */
        fun fullHierarchy(baseDir: File): ModuleOrganizer {
            return ModuleOrganizer(
                baseDir = baseDir,
                config = OrganizerConfig(
                    platformFolders = true,
                    featureFolders = true,
                    storyFolders = true
                )
            )
        }
    }
}
