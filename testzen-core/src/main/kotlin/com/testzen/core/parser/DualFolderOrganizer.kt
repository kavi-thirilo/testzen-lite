package com.testzen.core.parser

import com.testzen.core.model.Platform
import com.testzen.core.model.TestCase
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Organizes tests into dual-folder structure with automatic NLP to YAML conversion.
 *
 * Structure:
 * ```
 * tests/
 * ├── nlp/                           # Plain English test files
 * │   ├── android/
 * │   │   ├── auth/
 * │   │   │   ├── login/
 * │   │   │   │   └── basic_login.test
 * │   │   │   └── registration/
 * │   │   │       └── signup.test
 * │   │   └── payments/
 * │   │       └── checkout.test
 * │   ├── ios/
 * │   │   └── auth/
 * │   │       └── login/
 * │   │           └── basic_login.test
 * │   └── web/
 * │       └── auth/
 * │           └── login.test
 * │
 * └── yml/                           # Auto-generated YAML files (mirror of nlp/)
 *     ├── android/
 *     │   ├── auth/
 *     │   │   ├── login/
 *     │   │   │   └── basic_login.yaml
 *     │   │   └── registration/
 *     │   │       └── signup.yaml
 *     │   └── payments/
 *     │       └── checkout.yaml
 *     ├── ios/
 *     │   └── auth/
 *     │       └── login/
 *     │           └── basic_login.yaml
 *     └── web/
 *         └── auth/
 *             └── login.yaml
 * ```
 *
 * Usage:
 * ```kotlin
 * val organizer = DualFolderOrganizer(File("tests"))
 *
 * // Create module structure (creates in both nlp/ and yml/)
 * organizer.createModule("auth", Platform.ANDROID)
 * organizer.createFeature("auth", "login", Platform.ANDROID)
 *
 * // Write a plain English test (auto-converts to yml/)
 * organizer.writePlainEnglishTest(content, "auth", "login", "basic_login", Platform.ANDROID)
 *
 * // Sync all nlp files to yml
 * organizer.syncAll()
 *
 * // Start auto-sync watcher
 * organizer.startAutoSync()
 * ```
 */
class DualFolderOrganizer(
    private val baseDir: File,
    private val config: DualFolderConfig = DualFolderConfig()
) {
    private val logger = LoggerFactory.getLogger(DualFolderOrganizer::class.java)
    private val parser = PlainEnglishTestParser()
    private val writer = TestWriter()

    val nlpDir: File = File(baseDir, config.nlpFolderName)
    val ymlDir: File = File(baseDir, config.ymlFolderName)

    private var watchService: WatchService? = null
    private var watchExecutor: ScheduledExecutorService? = null
    private var isWatching = false

    /**
     * Configuration for dual-folder organization.
     */
    data class DualFolderConfig(
        /** Name of the plain English folder */
        val nlpFolderName: String = "nlp",
        /** Name of the YAML folder */
        val ymlFolderName: String = "yml",
        /** Use platform folders (android/, ios/, web/) */
        val platformFolders: Boolean = true,
        /** Use module folders */
        val moduleFolders: Boolean = true,
        /** Use feature subfolders */
        val featureFolders: Boolean = true,
        /** Use flow/page subfolders */
        val flowFolders: Boolean = false,
        /** Auto-create directories */
        val autoCreate: Boolean = true,
        /** Delete orphan YAML files when source NLP is deleted */
        val deleteOrphans: Boolean = false,
        /** YAML output configuration */
        val yamlConfig: TestWriter.YamlConfig = TestWriter.YamlConfig(),
        /** Supported NLP file extensions */
        val nlpExtensions: Set<String> = setOf("test", "txt", "english", "plain", "nlp"),
        /** File naming convention for YAML output */
        val fileNaming: ModuleOrganizer.FileNaming = ModuleOrganizer.FileNaming.SNAKE_CASE
    )

    /**
     * Sync result information.
     */
    data class SyncResult(
        val sourcePath: File,
        val targetPath: File,
        val testCount: Int,
        val success: Boolean,
        val error: String? = null
    )

    /**
     * Overall sync summary.
     */
    data class SyncSummary(
        val totalFiles: Int,
        val successCount: Int,
        val failureCount: Int,
        val totalTests: Int,
        val results: List<SyncResult>
    )

    init {
        if (config.autoCreate) {
            nlpDir.mkdirs()
            ymlDir.mkdirs()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // STRUCTURE CREATION
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Create a module folder in both nlp/ and yml/ directories.
     */
    fun createModule(moduleId: String, platform: Platform? = null): Pair<File, File> {
        val nlpPath = getModulePath(nlpDir, moduleId, platform)
        val ymlPath = getModulePath(ymlDir, moduleId, platform)

        nlpPath.mkdirs()
        ymlPath.mkdirs()

        logger.info("Created module in nlp/: ${nlpPath.relativeTo(baseDir)}")
        logger.info("Created module in yml/: ${ymlPath.relativeTo(baseDir)}")

        return nlpPath to ymlPath
    }

    /**
     * Create a feature folder under a module in both directories.
     */
    fun createFeature(
        moduleId: String,
        featureId: String,
        platform: Platform? = null
    ): Pair<File, File> {
        val nlpPath = getFeaturePath(nlpDir, moduleId, featureId, platform)
        val ymlPath = getFeaturePath(ymlDir, moduleId, featureId, platform)

        nlpPath.mkdirs()
        ymlPath.mkdirs()

        logger.info("Created feature in nlp/: ${nlpPath.relativeTo(baseDir)}")
        logger.info("Created feature in yml/: ${ymlPath.relativeTo(baseDir)}")

        return nlpPath to ymlPath
    }

    /**
     * Create a flow/page folder under a feature in both directories.
     */
    fun createFlow(
        moduleId: String,
        featureId: String,
        flowId: String,
        platform: Platform? = null
    ): Pair<File, File> {
        val nlpPath = getFlowPath(nlpDir, moduleId, featureId, flowId, platform)
        val ymlPath = getFlowPath(ymlDir, moduleId, featureId, flowId, platform)

        nlpPath.mkdirs()
        ymlPath.mkdirs()

        logger.info("Created flow in nlp/: ${nlpPath.relativeTo(baseDir)}")
        logger.info("Created flow in yml/: ${ymlPath.relativeTo(baseDir)}")

        return nlpPath to ymlPath
    }

    /**
     * Create full hierarchy: platform/module/feature/flow
     */
    fun createFullHierarchy(
        moduleId: String,
        featureId: String,
        flowId: String? = null,
        platform: Platform? = null
    ): Pair<File, File> {
        createModule(moduleId, platform)
        val (nlpFeature, ymlFeature) = createFeature(moduleId, featureId, platform)

        return if (flowId != null && config.flowFolders) {
            createFlow(moduleId, featureId, flowId, platform)
        } else {
            nlpFeature to ymlFeature
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PATH RESOLUTION
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Get module path for a specific root directory.
     */
    fun getModulePath(rootDir: File, moduleId: String, platform: Platform? = null): File {
        val sanitized = sanitizeName(moduleId)

        return if (config.platformFolders && platform != null) {
            File(rootDir, "${platform.name.lowercase()}/$sanitized")
        } else {
            File(rootDir, sanitized)
        }
    }

    /**
     * Get feature path for a specific root directory.
     */
    fun getFeaturePath(
        rootDir: File,
        moduleId: String,
        featureId: String,
        platform: Platform? = null
    ): File {
        val modulePath = getModulePath(rootDir, moduleId, platform)
        val sanitized = sanitizeName(featureId)

        return if (config.featureFolders) {
            File(modulePath, sanitized)
        } else {
            modulePath
        }
    }

    /**
     * Get flow/page path for a specific root directory.
     */
    fun getFlowPath(
        rootDir: File,
        moduleId: String,
        featureId: String,
        flowId: String,
        platform: Platform? = null
    ): File {
        val featurePath = getFeaturePath(rootDir, moduleId, featureId, platform)
        val sanitized = sanitizeName(flowId)

        return if (config.flowFolders) {
            File(featurePath, sanitized)
        } else {
            featurePath
        }
    }

    /**
     * Get the equivalent path in yml/ for a path in nlp/.
     */
    fun getYmlEquivalent(nlpPath: File): File {
        val relativePath = nlpPath.relativeTo(nlpDir)
        return File(ymlDir, relativePath.path)
    }

    /**
     * Get the equivalent path in nlp/ for a path in yml/.
     */
    fun getNlpEquivalent(ymlPath: File): File {
        val relativePath = ymlPath.relativeTo(ymlDir)
        return File(nlpDir, relativePath.path)
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // TEST FILE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Write a plain English test file and auto-convert to YAML.
     *
     * @param content Plain English test content
     * @param moduleId Module name
     * @param featureId Feature name (optional)
     * @param testName Test file name (without extension)
     * @param platform Target platform
     * @param flowId Flow/page name (optional)
     * @return Pair of (nlp file, yml file)
     */
    fun writePlainEnglishTest(
        content: String,
        moduleId: String,
        featureId: String? = null,
        testName: String,
        platform: Platform? = null,
        flowId: String? = null
    ): Pair<File, File> {
        // Determine directory paths
        val nlpTargetDir = when {
            flowId != null && featureId != null -> getFlowPath(nlpDir, moduleId, featureId, flowId, platform)
            featureId != null -> getFeaturePath(nlpDir, moduleId, featureId, platform)
            else -> getModulePath(nlpDir, moduleId, platform)
        }
        nlpTargetDir.mkdirs()

        // Write NLP file
        val nlpFile = File(nlpTargetDir, "${sanitizeName(testName)}.test")
        nlpFile.writeText(content)
        logger.info("Wrote NLP test: ${nlpFile.relativeTo(baseDir)}")

        // Convert and write YAML
        val ymlFile = convertToYaml(nlpFile)

        return nlpFile to ymlFile
    }

    /**
     * Convert a single NLP file to YAML and place in mirror location.
     */
    fun convertToYaml(nlpFile: File): File {
        val testCases = parser.parseFile(nlpFile)

        // Determine target yml path
        val relativeDir = nlpFile.parentFile.relativeTo(nlpDir)
        val ymlTargetDir = File(ymlDir, relativeDir.path)
        ymlTargetDir.mkdirs()

        // Generate yaml filename
        val baseName = nlpFile.nameWithoutExtension
        val ymlFile = File(ymlTargetDir, "${baseName}.yaml")

        // Write YAML
        if (testCases.size == 1) {
            writer.writeYaml(testCases[0], ymlFile, config.yamlConfig)
        } else {
            // Multiple tests - write as multi-document YAML
            val yamlContent = testCases.joinToString("\n---\n") {
                writer.toYaml(it, config.yamlConfig)
            }
            ymlFile.writeText(yamlContent)
        }

        logger.info("Converted to YAML: ${ymlFile.relativeTo(baseDir)} (${testCases.size} test(s))")
        return ymlFile
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // SYNC OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Sync all NLP files to YAML equivalents.
     */
    fun syncAll(): SyncSummary {
        val results = mutableListOf<SyncResult>()
        var successCount = 0
        var failureCount = 0
        var totalTests = 0

        // Find all NLP files
        val nlpFiles = nlpDir.walkTopDown()
            .filter { it.isFile && isNlpFile(it) }
            .toList()

        nlpFiles.forEach { nlpFile ->
            val result = syncFile(nlpFile)
            results.add(result)

            if (result.success) {
                successCount++
                totalTests += result.testCount
            } else {
                failureCount++
            }
        }

        // Handle orphan YAML files if configured
        if (config.deleteOrphans) {
            deleteOrphanYamlFiles()
        }

        val summary = SyncSummary(
            totalFiles = nlpFiles.size,
            successCount = successCount,
            failureCount = failureCount,
            totalTests = totalTests,
            results = results
        )

        logger.info("Sync complete: ${summary.successCount}/${summary.totalFiles} files, ${summary.totalTests} tests")
        return summary
    }

    /**
     * Sync a single NLP file to its YAML equivalent.
     */
    fun syncFile(nlpFile: File): SyncResult {
        return try {
            val testCases = parser.parseFile(nlpFile)
            val ymlFile = convertToYaml(nlpFile)

            SyncResult(
                sourcePath = nlpFile,
                targetPath = ymlFile,
                testCount = testCases.size,
                success = true
            )
        } catch (e: Exception) {
            logger.error("Failed to sync ${nlpFile.name}: ${e.message}")
            SyncResult(
                sourcePath = nlpFile,
                targetPath = getYmlEquivalent(nlpFile),
                testCount = 0,
                success = false,
                error = e.message
            )
        }
    }

    /**
     * Sync a specific module.
     */
    fun syncModule(moduleId: String, platform: Platform? = null): SyncSummary {
        val modulePath = getModulePath(nlpDir, moduleId, platform)

        if (!modulePath.exists()) {
            return SyncSummary(0, 0, 0, 0, emptyList())
        }

        val results = mutableListOf<SyncResult>()
        var successCount = 0
        var totalTests = 0

        modulePath.walkTopDown()
            .filter { it.isFile && isNlpFile(it) }
            .forEach { nlpFile ->
                val result = syncFile(nlpFile)
                results.add(result)
                if (result.success) {
                    successCount++
                    totalTests += result.testCount
                }
            }

        return SyncSummary(
            totalFiles = results.size,
            successCount = successCount,
            failureCount = results.size - successCount,
            totalTests = totalTests,
            results = results
        )
    }

    /**
     * Delete YAML files that don't have corresponding NLP source.
     */
    fun deleteOrphanYamlFiles(): List<File> {
        val deleted = mutableListOf<File>()

        ymlDir.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in setOf("yaml", "yml") }
            .forEach { ymlFile ->
                val nlpEquivalent = getNlpEquivalent(ymlFile)
                val possibleSources = config.nlpExtensions.map { ext ->
                    File(nlpEquivalent.parentFile, "${nlpEquivalent.nameWithoutExtension}.$ext")
                }

                if (possibleSources.none { it.exists() }) {
                    logger.info("Deleting orphan YAML: ${ymlFile.relativeTo(baseDir)}")
                    ymlFile.delete()
                    deleted.add(ymlFile)
                }
            }

        return deleted
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // AUTO-SYNC WATCHER
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Start watching nlp/ folder for changes and auto-sync to yml/.
     */
    fun startAutoSync() {
        if (isWatching) {
            logger.warn("Auto-sync is already running")
            return
        }

        watchService = FileSystems.getDefault().newWatchService()
        watchExecutor = Executors.newSingleThreadScheduledExecutor()
        isWatching = true

        // Register all directories under nlp/
        registerWatchDirectories(nlpDir)

        // Start watching
        watchExecutor?.scheduleWithFixedDelay({
            try {
                processWatchEvents()
            } catch (e: Exception) {
                logger.error("Watch error: ${e.message}")
            }
        }, 0, 500, TimeUnit.MILLISECONDS)

        logger.info("Started auto-sync watcher for ${nlpDir.absolutePath}")
    }

    /**
     * Stop the auto-sync watcher.
     */
    fun stopAutoSync() {
        if (!isWatching) return

        isWatching = false
        watchExecutor?.shutdown()
        watchService?.close()
        watchExecutor = null
        watchService = null

        logger.info("Stopped auto-sync watcher")
    }

    /**
     * Register directory for watching (recursive).
     */
    private fun registerWatchDirectories(dir: File) {
        dir.walkTopDown()
            .filter { it.isDirectory }
            .forEach { subDir ->
                try {
                    subDir.toPath().register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE
                    )
                } catch (e: Exception) {
                    logger.warn("Could not watch directory: ${subDir.absolutePath}")
                }
            }
    }

    /**
     * Process file system watch events.
     */
    private fun processWatchEvents() {
        val key = watchService?.poll() ?: return

        key.pollEvents().forEach { event ->
            val kind = event.kind()
            val context = event.context() as? Path ?: return@forEach
            val watchPath = (key.watchable() as Path).resolve(context)
            val file = watchPath.toFile()

            when {
                kind == StandardWatchEventKinds.ENTRY_CREATE && file.isDirectory -> {
                    // New directory - register it for watching
                    registerWatchDirectories(file)
                    // Create mirror in yml/
                    val ymlMirror = getYmlEquivalent(file)
                    ymlMirror.mkdirs()
                    logger.info("Created mirror directory: ${ymlMirror.relativeTo(baseDir)}")
                }

                kind == StandardWatchEventKinds.ENTRY_CREATE && isNlpFile(file) -> {
                    // New NLP file - convert to YAML
                    logger.info("New NLP file detected: ${file.relativeTo(baseDir)}")
                    syncFile(file)
                }

                kind == StandardWatchEventKinds.ENTRY_MODIFY && isNlpFile(file) -> {
                    // Modified NLP file - re-convert
                    logger.info("NLP file modified: ${file.relativeTo(baseDir)}")
                    syncFile(file)
                }

                kind == StandardWatchEventKinds.ENTRY_DELETE && config.deleteOrphans -> {
                    // Check if it was an NLP file and delete YAML equivalent
                    val ymlEquivalent = getYmlEquivalent(file)
                    val ymlFile = File(ymlEquivalent.parentFile, "${ymlEquivalent.nameWithoutExtension}.yaml")
                    if (ymlFile.exists()) {
                        logger.info("Deleting orphan YAML: ${ymlFile.relativeTo(baseDir)}")
                        ymlFile.delete()
                    }
                }
            }
        }

        key.reset()
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // STRUCTURE INFO
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Get structure information.
     */
    data class StructureInfo(
        val nlpModules: List<ModuleInfo>,
        val ymlModules: List<ModuleInfo>,
        val syncStatus: Map<String, Boolean>  // NLP file path -> has yml
    )

    data class ModuleInfo(
        val id: String,
        val platform: Platform?,
        val path: File,
        val features: List<String>,
        val testFiles: Int
    )

    /**
     * Get current folder structure info.
     */
    fun getStructureInfo(): StructureInfo {
        val nlpModules = scanModules(nlpDir)
        val ymlModules = scanModules(ymlDir)

        val syncStatus = mutableMapOf<String, Boolean>()

        nlpDir.walkTopDown()
            .filter { it.isFile && isNlpFile(it) }
            .forEach { nlpFile ->
                val ymlEquiv = File(
                    getYmlEquivalent(nlpFile.parentFile),
                    "${nlpFile.nameWithoutExtension}.yaml"
                )
                syncStatus[nlpFile.relativeTo(nlpDir).path] = ymlEquiv.exists()
            }

        return StructureInfo(nlpModules, ymlModules, syncStatus)
    }

    /**
     * Scan for modules in a directory.
     */
    private fun scanModules(rootDir: File): List<ModuleInfo> {
        val modules = mutableListOf<ModuleInfo>()

        if (config.platformFolders) {
            Platform.values().forEach { platform ->
                val platformDir = File(rootDir, platform.name.lowercase())
                if (platformDir.exists()) {
                    platformDir.listFiles()
                        ?.filter { it.isDirectory && !it.name.startsWith(".") }
                        ?.forEach { moduleDir ->
                            modules.add(createModuleInfo(moduleDir, platform))
                        }
                }
            }
        } else {
            rootDir.listFiles()
                ?.filter { it.isDirectory && !it.name.startsWith(".") }
                ?.forEach { moduleDir ->
                    modules.add(createModuleInfo(moduleDir, null))
                }
        }

        return modules
    }

    private fun createModuleInfo(moduleDir: File, platform: Platform?): ModuleInfo {
        val features = moduleDir.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.map { it.name }
            ?: emptyList()

        val testFiles = moduleDir.walkTopDown()
            .filter { it.isFile && (isNlpFile(it) || it.extension.lowercase() in setOf("yaml", "yml")) }
            .count()

        return ModuleInfo(
            id = moduleDir.name,
            platform = platform,
            path = moduleDir,
            features = features,
            testFiles = testFiles
        )
    }

    /**
     * Print current structure.
     */
    fun printStructure() {
        val info = getStructureInfo()

        println("Dual Folder Test Structure")
        println("═".repeat(60))
        println("Base: ${baseDir.absolutePath}")
        println()

        println("📁 NLP (Plain English): ${nlpDir.name}/")
        info.nlpModules.groupBy { it.platform }.forEach { (platform, modules) ->
            val platformName = platform?.name?.lowercase() ?: "common"
            println("  📱 $platformName/")
            modules.forEach { module ->
                println("    📂 ${module.id}/ (${module.testFiles} files)")
                module.features.forEach { feature ->
                    println("      📁 $feature/")
                }
            }
        }

        println()
        println("📁 YML (Generated YAML): ${ymlDir.name}/")
        info.ymlModules.groupBy { it.platform }.forEach { (platform, modules) ->
            val platformName = platform?.name?.lowercase() ?: "common"
            println("  📱 $platformName/")
            modules.forEach { module ->
                println("    📂 ${module.id}/ (${module.testFiles} files)")
                module.features.forEach { feature ->
                    println("      📁 $feature/")
                }
            }
        }

        println()
        val unsynced = info.syncStatus.filterValues { !it }.keys
        if (unsynced.isNotEmpty()) {
            println("⚠️  Unsynced NLP files:")
            unsynced.forEach { println("    - $it") }
        } else {
            println("✅ All NLP files are synced to YAML")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Check if file is an NLP test file.
     */
    private fun isNlpFile(file: File): Boolean {
        return file.extension.lowercase() in config.nlpExtensions
    }

    /**
     * Sanitize name for folder/file usage.
     */
    private fun sanitizeName(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }

    companion object {
        /**
         * Create organizer with standard settings.
         */
        fun standard(baseDir: File): DualFolderOrganizer {
            return DualFolderOrganizer(
                baseDir = baseDir,
                config = DualFolderConfig(
                    platformFolders = true,
                    moduleFolders = true,
                    featureFolders = true,
                    flowFolders = false
                )
            )
        }

        /**
         * Create organizer with full hierarchy (module/feature/flow).
         */
        fun fullHierarchy(baseDir: File): DualFolderOrganizer {
            return DualFolderOrganizer(
                baseDir = baseDir,
                config = DualFolderConfig(
                    platformFolders = true,
                    moduleFolders = true,
                    featureFolders = true,
                    flowFolders = true
                )
            )
        }

        /**
         * Create organizer with flat structure (platform/module only).
         */
        fun flat(baseDir: File): DualFolderOrganizer {
            return DualFolderOrganizer(
                baseDir = baseDir,
                config = DualFolderConfig(
                    platformFolders = true,
                    moduleFolders = true,
                    featureFolders = false,
                    flowFolders = false
                )
            )
        }

        /**
         * Create organizer with custom folder names.
         */
        fun withCustomFolders(
            baseDir: File,
            nlpFolderName: String,
            ymlFolderName: String
        ): DualFolderOrganizer {
            return DualFolderOrganizer(
                baseDir = baseDir,
                config = DualFolderConfig(
                    nlpFolderName = nlpFolderName,
                    ymlFolderName = ymlFolderName
                )
            )
        }
    }
}
