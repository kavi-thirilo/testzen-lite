package com.testzen.core.organization

import com.testzen.core.model.Platform
import com.testzen.core.model.TestCase
import com.testzen.core.parser.DualFolderOrganizer
import com.testzen.core.parser.PlainEnglishTestParser
import com.testzen.core.parser.TestWriter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Application-level organization for test automation assets.
 *
 * Each application has its own complete folder structure:
 * ```
 * apps/
 * ├── usaa_mobile_android/                    # Application folder
 * │   ├── config/
 * │   │   └── app.json                        # App-specific configuration
 * │   │
 * │   ├── page_objects/                       # Page Object Model definitions
 * │   │   ├── common/                         # Shared page objects
 * │   │   │   └── navigation.json
 * │   │   ├── auth/                           # Module-based page objects
 * │   │   │   ├── login_page.json
 * │   │   │   └── register_page.json
 * │   │   └── dashboard/
 * │   │       └── home_page.json
 * │   │
 * │   ├── tests/                              # Test files (dual-folder structure)
 * │   │   ├── nlp/                            # Plain English tests
 * │   │   │   ├── auth/
 * │   │   │   │   └── login/
 * │   │   │   │       └── basic_login.test
 * │   │   │   └── payments/
 * │   │   │       └── checkout.test
 * │   │   │
 * │   │   └── yml/                            # Generated YAML tests
 * │   │       ├── auth/
 * │   │       │   └── login/
 * │   │       │       └── basic_login.yaml
 * │   │       └── payments/
 * │   │           └── checkout.yaml
 * │   │
 * │   ├── data/                               # Test data files
 * │   │   ├── test_data.json                  # Common test data
 * │   │   ├── users.json                      # User credentials
 * │   │   └── environments/                   # Environment-specific data
 * │   │       ├── dev.json
 * │   │       ├── staging.json
 * │   │       └── prod.json
 * │   │
 * │   ├── results/                            # Test execution results
 * │   │   └── 2024-01-21/                     # Date-based organization
 * │   │       └── execution_001/              # Execution run folder
 * │   │           ├── report.html             # HTML report
 * │   │           ├── report.json             # JSON report
 * │   │           ├── screenshots/            # Step screenshots
 * │   │           └── videos/                 # Recording videos
 * │   │
 * │   └── locators/                           # Self-healing locator cache
 * │       └── login_page_cache.json
 * │
 * ├── usaa_mobile_ios/
 * │   └── ...
 * │
 * └── usaa_web/
 *     └── ...
 * ```
 *
 * Usage:
 * ```kotlin
 * val organizer = ApplicationOrganizer(File("apps"))
 *
 * // Create a new application
 * val app = organizer.createApplication(
 *     appId = "usaa_mobile",
 *     platform = Platform.ANDROID,
 *     displayName = "USAA Mobile"
 * )
 *
 * // Access application paths
 * val testsDir = app.getTestsDirectory()
 * val pageObjectsDir = app.getPageObjectsDirectory()
 * val resultsDir = app.getResultsDirectory()
 *
 * // Create execution result folder
 * val executionDir = app.createExecutionResultFolder()
 * ```
 */
class ApplicationOrganizer(
    private val appsBaseDir: File,
    private val config: AppOrganizerConfig = AppOrganizerConfig()
) {
    private val logger = LoggerFactory.getLogger(ApplicationOrganizer::class.java)
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    init {
        if (config.autoCreate) {
            appsBaseDir.mkdirs()
        }
    }

    /**
     * Configuration for application organization.
     */
    data class AppOrganizerConfig(
        /** Auto-create directories */
        val autoCreate: Boolean = true,
        /** Use dual-folder structure for tests */
        val useDualFolderTests: Boolean = true,
        /** Date format for results folders */
        val dateFormat: String = "yyyy-MM-dd",
        /** Execution folder prefix */
        val executionPrefix: String = "execution_",
        /** Enable locator caching */
        val enableLocatorCache: Boolean = true
    )

    // ═══════════════════════════════════════════════════════════════════════════════
    // APPLICATION MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Create a new application with full folder structure.
     */
    fun createApplication(
        appId: String,
        platform: Platform,
        displayName: String? = null,
        packageName: String? = null,
        bundleId: String? = null,
        baseUrl: String? = null
    ): ApplicationContext {
        val appFolderId = generateAppFolderId(appId, platform)
        val appDir = File(appsBaseDir, appFolderId)

        // Create directory structure
        val structure = createApplicationStructure(appDir)

        // Create app config
        val appConfig = ApplicationConfig(
            appId = appId,
            displayName = displayName ?: appId,
            platform = platform.name.lowercase(),
            packageName = packageName,
            bundleId = bundleId,
            baseUrl = baseUrl,
            createdAt = LocalDateTime.now().toString()
        )

        // Write app config
        val configFile = File(structure.configDir, "app.json")
        configFile.writeText(json.encodeToString(ApplicationConfig.serializer(), appConfig))

        logger.info("Created application: $appFolderId at ${appDir.absolutePath}")

        return ApplicationContext(
            appId = appId,
            folderId = appFolderId,
            platform = platform,
            config = appConfig,
            rootDir = appDir,
            structure = structure,
            organizer = this
        )
    }

    /**
     * Get an existing application context.
     */
    fun getApplication(appId: String, platform: Platform): ApplicationContext? {
        val appFolderId = generateAppFolderId(appId, platform)
        val appDir = File(appsBaseDir, appFolderId)

        if (!appDir.exists()) {
            return null
        }

        val configFile = File(appDir, "config/app.json")
        val appConfig = if (configFile.exists()) {
            json.decodeFromString(ApplicationConfig.serializer(), configFile.readText())
        } else {
            ApplicationConfig(
                appId = appId,
                displayName = appId,
                platform = platform.name.lowercase()
            )
        }

        return ApplicationContext(
            appId = appId,
            folderId = appFolderId,
            platform = platform,
            config = appConfig,
            rootDir = appDir,
            structure = ApplicationStructure.fromExisting(appDir),
            organizer = this
        )
    }

    /**
     * Get or create an application.
     */
    fun getOrCreateApplication(
        appId: String,
        platform: Platform,
        displayName: String? = null
    ): ApplicationContext {
        return getApplication(appId, platform) ?: createApplication(
            appId = appId,
            platform = platform,
            displayName = displayName
        )
    }

    /**
     * List all applications.
     */
    fun listApplications(): List<ApplicationContext> {
        return appsBaseDir.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.mapNotNull { appDir ->
                try {
                    val configFile = File(appDir, "config/app.json")
                    if (configFile.exists()) {
                        val config = json.decodeFromString(ApplicationConfig.serializer(), configFile.readText())
                        val platform = Platform.valueOf(config.platform.uppercase())
                        ApplicationContext(
                            appId = config.appId,
                            folderId = appDir.name,
                            platform = platform,
                            config = config,
                            rootDir = appDir,
                            structure = ApplicationStructure.fromExisting(appDir),
                            organizer = this
                        )
                    } else null
                } catch (e: Exception) {
                    logger.warn("Failed to load app from ${appDir.name}: ${e.message}")
                    null
                }
            }
            ?: emptyList()
    }

    /**
     * List applications by platform.
     */
    fun listApplicationsByPlatform(platform: Platform): List<ApplicationContext> {
        return listApplications().filter { it.platform == platform }
    }

    /**
     * Delete an application and all its files.
     */
    fun deleteApplication(appId: String, platform: Platform): Boolean {
        val appFolderId = generateAppFolderId(appId, platform)
        val appDir = File(appsBaseDir, appFolderId)

        return if (appDir.exists()) {
            appDir.deleteRecursively()
            logger.info("Deleted application: $appFolderId")
            true
        } else {
            false
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // STRUCTURE CREATION
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Create the full directory structure for an application.
     */
    private fun createApplicationStructure(appDir: File): ApplicationStructure {
        val structure = ApplicationStructure(
            rootDir = appDir,
            configDir = File(appDir, "config"),
            pageObjectsDir = File(appDir, "page_objects"),
            testsDir = File(appDir, "tests"),
            dataDir = File(appDir, "data"),
            resultsDir = File(appDir, "results"),
            locatorsDir = File(appDir, "locators")
        )

        // Create all directories
        structure.configDir.mkdirs()
        structure.pageObjectsDir.mkdirs()
        structure.dataDir.mkdirs()
        structure.resultsDir.mkdirs()
        structure.locatorsDir.mkdirs()

        // Create tests directory with dual-folder structure
        if (config.useDualFolderTests) {
            File(structure.testsDir, "nlp").mkdirs()
            File(structure.testsDir, "yml").mkdirs()
        } else {
            structure.testsDir.mkdirs()
        }

        // Create common subdirectories
        File(structure.pageObjectsDir, "common").mkdirs()
        File(structure.dataDir, "environments").mkdirs()

        return structure
    }

    /**
     * Generate consistent folder ID for an application.
     */
    private fun generateAppFolderId(appId: String, platform: Platform): String {
        val sanitizedId = appId.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
        return "${sanitizedId}_${platform.name.lowercase()}"
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // SUMMARY
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Print summary of all applications.
     */
    fun printSummary() {
        val apps = listApplications()

        println("Application Organization Summary")
        println("═".repeat(60))
        println("Base directory: ${appsBaseDir.absolutePath}")
        println("Total applications: ${apps.size}")
        println()

        apps.groupBy { it.platform }.forEach { (platform, platformApps) ->
            println("📱 ${platform.name} (${platformApps.size} apps)")
            platformApps.forEach { app ->
                val testCount = app.getTestCount()
                val pageObjectCount = app.getPageObjectCount()
                println("  📦 ${app.config.displayName}")
                println("     ID: ${app.folderId}")
                println("     Tests: $testCount | Page Objects: $pageObjectCount")
            }
            println()
        }
    }

    companion object {
        /**
         * Create organizer with default apps directory.
         */
        fun default(): ApplicationOrganizer = ApplicationOrganizer(File("apps"))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// APPLICATION STRUCTURE
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Represents the directory structure of an application.
 */
data class ApplicationStructure(
    val rootDir: File,
    val configDir: File,
    val pageObjectsDir: File,
    val testsDir: File,
    val dataDir: File,
    val resultsDir: File,
    val locatorsDir: File
) {
    companion object {
        fun fromExisting(appDir: File): ApplicationStructure {
            return ApplicationStructure(
                rootDir = appDir,
                configDir = File(appDir, "config"),
                pageObjectsDir = File(appDir, "page_objects"),
                testsDir = File(appDir, "tests"),
                dataDir = File(appDir, "data"),
                resultsDir = File(appDir, "results"),
                locatorsDir = File(appDir, "locators")
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// APPLICATION CONFIG
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Application configuration stored in config/app.json.
 */
@Serializable
data class ApplicationConfig(
    @SerialName("app_id")
    val appId: String,

    @SerialName("display_name")
    val displayName: String,

    val platform: String,

    @SerialName("package_name")
    val packageName: String? = null,

    @SerialName("bundle_id")
    val bundleId: String? = null,

    @SerialName("base_url")
    val baseUrl: String? = null,

    val version: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    val description: String? = null,

    val tags: List<String> = emptyList(),

    @SerialName("default_emulator")
    val defaultEmulator: String? = null,

    @SerialName("default_simulator")
    val defaultSimulator: String? = null,

    @SerialName("default_browser")
    val defaultBrowser: String? = null,

    val capabilities: Map<String, String> = emptyMap()
)

// ═══════════════════════════════════════════════════════════════════════════════
// APPLICATION CONTEXT
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Context for working with a specific application.
 */
class ApplicationContext(
    val appId: String,
    val folderId: String,
    val platform: Platform,
    val config: ApplicationConfig,
    val rootDir: File,
    val structure: ApplicationStructure,
    private val organizer: ApplicationOrganizer
) {
    private val logger = LoggerFactory.getLogger(ApplicationContext::class.java)
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val testWriter = TestWriter()
    private val plainEnglishParser = PlainEnglishTestParser()

    // ═══════════════════════════════════════════════════════════════════════════════
    // DIRECTORY ACCESS
    // ═══════════════════════════════════════════════════════════════════════════════

    fun getConfigDirectory(): File = structure.configDir
    fun getPageObjectsDirectory(): File = structure.pageObjectsDir
    fun getTestsDirectory(): File = structure.testsDir
    fun getDataDirectory(): File = structure.dataDir
    fun getResultsDirectory(): File = structure.resultsDir
    fun getLocatorsDirectory(): File = structure.locatorsDir

    fun getNlpTestsDirectory(): File = File(structure.testsDir, "nlp")
    fun getYmlTestsDirectory(): File = File(structure.testsDir, "yml")

    // ═══════════════════════════════════════════════════════════════════════════════
    // PAGE OBJECTS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Create a page object module folder.
     */
    fun createPageObjectModule(moduleId: String): File {
        val moduleDir = File(structure.pageObjectsDir, sanitizeName(moduleId))
        moduleDir.mkdirs()
        logger.info("Created page object module: ${moduleDir.name}")
        return moduleDir
    }

    /**
     * Save a page object definition.
     */
    fun savePageObject(
        pageId: String,
        moduleId: String? = null,
        pageObject: PageObjectDefinition
    ): File {
        val targetDir = if (moduleId != null) {
            File(structure.pageObjectsDir, sanitizeName(moduleId)).also { it.mkdirs() }
        } else {
            structure.pageObjectsDir
        }

        val file = File(targetDir, "${sanitizeName(pageId)}.json")
        file.writeText(json.encodeToString(PageObjectDefinition.serializer(), pageObject))
        logger.info("Saved page object: ${file.relativeTo(rootDir)}")
        return file
    }

    /**
     * Load a page object definition.
     */
    fun loadPageObject(pageId: String, moduleId: String? = null): PageObjectDefinition? {
        val targetDir = if (moduleId != null) {
            File(structure.pageObjectsDir, sanitizeName(moduleId))
        } else {
            structure.pageObjectsDir
        }

        val file = File(targetDir, "${sanitizeName(pageId)}.json")
        return if (file.exists()) {
            json.decodeFromString(PageObjectDefinition.serializer(), file.readText())
        } else null
    }

    /**
     * List all page objects.
     */
    fun listPageObjects(): List<File> {
        return structure.pageObjectsDir.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .toList()
    }

    /**
     * Get count of page objects.
     */
    fun getPageObjectCount(): Int {
        return listPageObjects().size
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // TESTS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Get DualFolderOrganizer for this application's tests.
     */
    fun getTestOrganizer(): DualFolderOrganizer {
        return DualFolderOrganizer.standard(structure.testsDir)
    }

    /**
     * Create a test module folder.
     */
    fun createTestModule(moduleId: String, featureId: String? = null): Pair<File, File> {
        val organizer = getTestOrganizer()
        return if (featureId != null) {
            organizer.createFeature(moduleId, featureId)
        } else {
            organizer.createModule(moduleId)
        }
    }

    /**
     * Write a plain English test file.
     */
    fun writeTest(
        content: String,
        moduleId: String,
        featureId: String? = null,
        testName: String
    ): Pair<File, File> {
        val organizer = getTestOrganizer()
        return organizer.writePlainEnglishTest(
            content = content,
            moduleId = moduleId,
            featureId = featureId,
            testName = testName,
            platform = null  // Platform already determined by app
        )
    }

    /**
     * Sync all NLP tests to YAML.
     */
    fun syncTests(): DualFolderOrganizer.SyncSummary {
        return getTestOrganizer().syncAll()
    }

    /**
     * Load all tests from this application.
     */
    fun loadTests(): List<TestCase> {
        val ymlDir = getYmlTestsDirectory()
        val nlpDir = getNlpTestsDirectory()

        val sourceDir = if (ymlDir.exists() && ymlDir.listFiles()?.isNotEmpty() == true) {
            ymlDir
        } else {
            nlpDir
        }

        return if (sourceDir.exists()) {
            sourceDir.walkTopDown()
                .filter { it.isFile && it.extension in listOf("yaml", "yml", "test", "txt") }
                .flatMap { file ->
                    try {
                        if (file.extension in listOf("yaml", "yml")) {
                            // Load YAML directly
                            listOf<TestCase>() // Would need TestLoader here
                        } else {
                            plainEnglishParser.parseFile(file)
                        }
                    } catch (e: Exception) {
                        logger.warn("Failed to load test ${file.name}: ${e.message}")
                        emptyList()
                    }
                }
                .toList()
        } else {
            emptyList()
        }
    }

    /**
     * Get count of test files.
     */
    fun getTestCount(): Int {
        val nlpDir = getNlpTestsDirectory()
        val ymlDir = getYmlTestsDirectory()

        var count = 0
        if (nlpDir.exists()) {
            count += nlpDir.walkTopDown()
                .filter { it.isFile && it.extension in listOf("test", "txt", "english", "nlp") }
                .count()
        }
        if (ymlDir.exists()) {
            count += ymlDir.walkTopDown()
                .filter { it.isFile && it.extension in listOf("yaml", "yml") }
                .count()
        }
        return count
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // TEST DATA
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Save test data file.
     */
    fun saveTestData(dataId: String, data: Map<String, Any>, environment: String? = null): File {
        val targetDir = if (environment != null) {
            File(structure.dataDir, "environments").also { it.mkdirs() }
        } else {
            structure.dataDir
        }

        val fileName = if (environment != null) "$environment.json" else "${sanitizeName(dataId)}.json"
        val file = File(targetDir, fileName)

        file.writeText(json.encodeToString(data))
        logger.info("Saved test data: ${file.relativeTo(rootDir)}")
        return file
    }

    /**
     * Load test data file.
     */
    fun loadTestData(dataId: String, environment: String? = null): Map<String, Any>? {
        val targetDir = if (environment != null) {
            File(structure.dataDir, "environments")
        } else {
            structure.dataDir
        }

        val fileName = if (environment != null) "$environment.json" else "${sanitizeName(dataId)}.json"
        val file = File(targetDir, fileName)

        return if (file.exists()) {
            json.decodeFromString(file.readText())
        } else null
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // RESULTS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Create a new execution result folder.
     */
    fun createExecutionResultFolder(executionId: String? = null): ExecutionResultFolder {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val timeFormatter = DateTimeFormatter.ofPattern("HHmmss")
        val now = LocalDateTime.now()

        val dateDir = File(structure.resultsDir, now.format(dateFormatter))
        dateDir.mkdirs()

        val execId = executionId ?: "execution_${now.format(timeFormatter)}"
        val executionDir = File(dateDir, execId)
        executionDir.mkdirs()

        // Create subdirectories
        val screenshotsDir = File(executionDir, "screenshots").also { it.mkdirs() }
        val videosDir = File(executionDir, "videos").also { it.mkdirs() }
        val logsDir = File(executionDir, "logs").also { it.mkdirs() }

        logger.info("Created execution folder: ${executionDir.relativeTo(rootDir)}")

        return ExecutionResultFolder(
            executionId = execId,
            rootDir = executionDir,
            screenshotsDir = screenshotsDir,
            videosDir = videosDir,
            logsDir = logsDir,
            timestamp = now
        )
    }

    /**
     * Get latest execution result folder.
     */
    fun getLatestExecutionFolder(): ExecutionResultFolder? {
        val latestDate = structure.resultsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.maxByOrNull { it.name }

        val latestExecution = latestDate?.listFiles()
            ?.filter { it.isDirectory }
            ?.maxByOrNull { it.name }

        return latestExecution?.let {
            ExecutionResultFolder(
                executionId = it.name,
                rootDir = it,
                screenshotsDir = File(it, "screenshots"),
                videosDir = File(it, "videos"),
                logsDir = File(it, "logs"),
                timestamp = LocalDateTime.now()
            )
        }
    }

    /**
     * List all execution results.
     */
    fun listExecutionResults(): List<ExecutionResultFolder> {
        return structure.resultsDir.walkTopDown()
            .maxDepth(2)
            .filter { it.isDirectory && it.name.startsWith("execution_") }
            .map { dir ->
                ExecutionResultFolder(
                    executionId = dir.name,
                    rootDir = dir,
                    screenshotsDir = File(dir, "screenshots"),
                    videosDir = File(dir, "videos"),
                    logsDir = File(dir, "logs"),
                    timestamp = LocalDateTime.now()
                )
            }
            .toList()
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // LOCATORS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Save locator cache for a page.
     */
    fun saveLocatorCache(pageId: String, locators: Map<String, Any>): File {
        val file = File(structure.locatorsDir, "${sanitizeName(pageId)}_cache.json")
        file.writeText(json.encodeToString(locators))
        return file
    }

    /**
     * Load locator cache for a page.
     */
    fun loadLocatorCache(pageId: String): Map<String, Any>? {
        val file = File(structure.locatorsDir, "${sanitizeName(pageId)}_cache.json")
        return if (file.exists()) {
            json.decodeFromString(file.readText())
        } else null
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════════

    private fun sanitizeName(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }

    /**
     * Print application structure.
     */
    fun printStructure() {
        println("Application: ${config.displayName}")
        println("═".repeat(50))
        println("ID: $folderId")
        println("Platform: ${platform.name}")
        println("Path: ${rootDir.absolutePath}")
        println()

        println("📁 Structure:")
        println("  📂 config/")
        println("  📂 page_objects/ (${getPageObjectCount()} files)")
        println("  📂 tests/")
        println("     📁 nlp/")
        println("     📁 yml/")
        println("  📂 data/")
        println("  📂 results/ (${listExecutionResults().size} executions)")
        println("  📂 locators/")
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// EXECUTION RESULT FOLDER
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Represents an execution result folder.
 */
data class ExecutionResultFolder(
    val executionId: String,
    val rootDir: File,
    val screenshotsDir: File,
    val videosDir: File,
    val logsDir: File,
    val timestamp: LocalDateTime
) {
    /**
     * Get report file path.
     */
    fun getReportFile(format: String = "html"): File = File(rootDir, "report.$format")

    /**
     * Save screenshot.
     */
    fun saveScreenshot(stepNumber: Int, name: String, data: ByteArray): File {
        val file = File(screenshotsDir, "step_${stepNumber}_$name.png")
        file.writeBytes(data)
        return file
    }

    /**
     * Save report content.
     */
    fun saveReport(content: String, format: String = "html"): File {
        val file = getReportFile(format)
        file.writeText(content)
        return file
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PAGE OBJECT DEFINITION
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Page Object Model definition.
 */
@Serializable
data class PageObjectDefinition(
    @SerialName("page_id")
    val pageId: String,

    val name: String,

    val description: String? = null,

    @SerialName("screen_name")
    val screenName: String? = null,

    val elements: List<PageElement> = emptyList(),

    val actions: List<PageAction> = emptyList(),

    @SerialName("wait_conditions")
    val waitConditions: List<String> = emptyList(),

    val metadata: Map<String, String> = emptyMap()
)

/**
 * Element in a page object.
 */
@Serializable
data class PageElement(
    val id: String,
    val name: String,

    @SerialName("locator_type")
    val locatorType: String,

    @SerialName("locator_value")
    val locatorValue: String,

    @SerialName("fallback_locators")
    val fallbackLocators: List<FallbackLocator> = emptyList(),

    val description: String? = null,

    @SerialName("wait_timeout")
    val waitTimeout: Long? = null,

    val optional: Boolean = false
)

/**
 * Fallback locator for self-healing.
 */
@Serializable
data class FallbackLocator(
    val type: String,
    val value: String,
    val priority: Int = 0
)

/**
 * Action that can be performed on a page.
 */
@Serializable
data class PageAction(
    val id: String,
    val name: String,
    val description: String? = null,
    val steps: List<String> = emptyList()
)
