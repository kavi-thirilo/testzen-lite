package com.testzen.core.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Platform configuration loader and models for Android, iOS, and Web platforms.
 *
 * Configuration files location:
 * ```
 * config/
 * ├── android/
 * │   └── emulators.json       # Android emulator definitions
 * ├── ios/
 * │   └── simulators.json      # iOS simulator definitions
 * └── dotcom/
 *     └── browsers.json        # Web browser definitions
 * ```
 *
 * Usage:
 * ```kotlin
 * val configLoader = PlatformConfigLoader(File("config"))
 *
 * // Load all configurations
 * val androidConfig = configLoader.loadAndroidConfig()
 * val iosConfig = configLoader.loadIosConfig()
 * val webConfig = configLoader.loadWebConfig()
 *
 * // Get specific device/browser
 * val emulator = androidConfig.getEmulator("pixel_6_api_33")
 * val simulator = iosConfig.getSimulator("iphone_15_pro_ios_17")
 * val browser = webConfig.getBrowser("chrome_desktop")
 * ```
 */

// ═══════════════════════════════════════════════════════════════════════════════
// ANDROID CONFIGURATION
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Android emulator/device configuration.
 */
@Serializable
data class AndroidConfig(
    @SerialName("default_emulator")
    val defaultEmulator: String,

    @SerialName("emulators")
    val emulators: List<AndroidEmulator>,

    @SerialName("physical_devices")
    val physicalDevices: List<AndroidPhysicalDevice> = emptyList(),

    @SerialName("global_settings")
    val globalSettings: AndroidGlobalSettings = AndroidGlobalSettings()
) {
    /**
     * Get an emulator by ID.
     */
    fun getEmulator(id: String): AndroidEmulator? = emulators.find { it.id == id }

    /**
     * Get the default emulator.
     */
    fun getDefaultEmulator(): AndroidEmulator? = getEmulator(defaultEmulator)

    /**
     * Get all enabled emulators.
     */
    fun getEnabledEmulators(): List<AndroidEmulator> = emulators.filter { it.enabled }

    /**
     * Get emulators by tag.
     */
    fun getEmulatorsByTag(tag: String): List<AndroidEmulator> =
        emulators.filter { tag in it.tags && it.enabled }

    /**
     * Get a physical device by ID.
     */
    fun getPhysicalDevice(id: String): AndroidPhysicalDevice? = physicalDevices.find { it.id == id }

    /**
     * Get all enabled physical devices.
     */
    fun getEnabledPhysicalDevices(): List<AndroidPhysicalDevice> = physicalDevices.filter { it.enabled }
}

/**
 * Android emulator definition.
 */
@Serializable
data class AndroidEmulator(
    val id: String,
    val name: String,

    @SerialName("avd_name")
    val avdName: String,

    @SerialName("device_name")
    val deviceName: String,

    @SerialName("platform_version")
    val platformVersion: String,

    @SerialName("api_level")
    val apiLevel: Int,

    @SerialName("system_image")
    val systemImage: String = "google_apis",

    val resolution: String = "1080x1920",
    val density: Int = 420,
    val ram: Int = 2048,

    @SerialName("heap_size")
    val heapSize: Int = 256,

    val enabled: Boolean = true,
    val tags: List<String> = emptyList(),
    val capabilities: Map<String, String> = emptyMap()
) {
    /**
     * Convert to Appium capabilities map.
     */
    fun toCapabilities(): Map<String, Any> {
        val caps = mutableMapOf<String, Any>(
            "platformName" to "Android",
            "appium:deviceName" to deviceName,
            "appium:avd" to avdName,
            "appium:platformVersion" to platformVersion,
            "appium:automationName" to (capabilities["automationName"] ?: "UiAutomator2")
        )

        capabilities.forEach { (key, value) ->
            if (key != "automationName") {
                caps["appium:$key"] = when (value.lowercase()) {
                    "true" -> true
                    "false" -> false
                    else -> value
                }
            }
        }

        return caps
    }
}

/**
 * Android physical device definition.
 */
@Serializable
data class AndroidPhysicalDevice(
    val id: String,
    val name: String,
    val udid: String = "",

    @SerialName("device_name")
    val deviceName: String,

    @SerialName("platform_version")
    val platformVersion: String,

    val enabled: Boolean = false,
    val tags: List<String> = emptyList(),
    val capabilities: Map<String, String> = emptyMap()
) {
    /**
     * Convert to Appium capabilities map.
     */
    fun toCapabilities(): Map<String, Any> {
        val caps = mutableMapOf<String, Any>(
            "platformName" to "Android",
            "appium:deviceName" to deviceName,
            "appium:platformVersion" to platformVersion,
            "appium:automationName" to (capabilities["automationName"] ?: "UiAutomator2")
        )

        if (udid.isNotBlank()) {
            caps["appium:udid"] = udid
        }

        capabilities.forEach { (key, value) ->
            if (key != "automationName") {
                caps["appium:$key"] = when (value.lowercase()) {
                    "true" -> true
                    "false" -> false
                    else -> value
                }
            }
        }

        return caps
    }
}

/**
 * Android global settings.
 */
@Serializable
data class AndroidGlobalSettings(
    @SerialName("appium_host")
    val appiumHost: String = "127.0.0.1",

    @SerialName("appium_port")
    val appiumPort: Int = 4723,

    @SerialName("adb_exec_timeout")
    val adbExecTimeout: Long = 60000,

    @SerialName("startup_timeout")
    val startupTimeout: Long = 120000,

    @SerialName("new_command_timeout")
    val newCommandTimeout: Int = 300,

    @SerialName("auto_launch_emulator")
    val autoLaunchEmulator: Boolean = true,

    @SerialName("wait_for_idle_timeout")
    val waitForIdleTimeout: Long = 10000,

    @SerialName("enable_performance_logging")
    val enablePerformanceLogging: Boolean = false
)

// ═══════════════════════════════════════════════════════════════════════════════
// IOS CONFIGURATION
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * iOS simulator/device configuration.
 */
@Serializable
data class IosConfig(
    @SerialName("default_simulator")
    val defaultSimulator: String,

    @SerialName("simulators")
    val simulators: List<IosSimulator>,

    @SerialName("physical_devices")
    val physicalDevices: List<IosPhysicalDevice> = emptyList(),

    @SerialName("global_settings")
    val globalSettings: IosGlobalSettings = IosGlobalSettings()
) {
    /**
     * Get a simulator by ID.
     */
    fun getSimulator(id: String): IosSimulator? = simulators.find { it.id == id }

    /**
     * Get the default simulator.
     */
    fun getDefaultSimulator(): IosSimulator? = getSimulator(defaultSimulator)

    /**
     * Get all enabled simulators.
     */
    fun getEnabledSimulators(): List<IosSimulator> = simulators.filter { it.enabled }

    /**
     * Get simulators by tag.
     */
    fun getSimulatorsByTag(tag: String): List<IosSimulator> =
        simulators.filter { tag in it.tags && it.enabled }

    /**
     * Get a physical device by ID.
     */
    fun getPhysicalDevice(id: String): IosPhysicalDevice? = physicalDevices.find { it.id == id }

    /**
     * Get all enabled physical devices.
     */
    fun getEnabledPhysicalDevices(): List<IosPhysicalDevice> = physicalDevices.filter { it.enabled }
}

/**
 * iOS simulator definition.
 */
@Serializable
data class IosSimulator(
    val id: String,
    val name: String,

    @SerialName("device_name")
    val deviceName: String,

    @SerialName("platform_version")
    val platformVersion: String,

    val udid: String = "",
    val runtime: String = "",

    @SerialName("device_type")
    val deviceType: String = "",

    val resolution: String = "",
    val enabled: Boolean = true,
    val tags: List<String> = emptyList(),
    val capabilities: Map<String, String> = emptyMap()
) {
    /**
     * Convert to Appium capabilities map.
     */
    fun toCapabilities(): Map<String, Any> {
        val caps = mutableMapOf<String, Any>(
            "platformName" to "iOS",
            "appium:deviceName" to deviceName,
            "appium:platformVersion" to platformVersion,
            "appium:automationName" to (capabilities["automationName"] ?: "XCUITest")
        )

        if (udid.isNotBlank()) {
            caps["appium:udid"] = udid
        }

        capabilities.forEach { (key, value) ->
            if (key != "automationName") {
                val parsedValue: Any = when {
                    value.lowercase() == "true" -> true
                    value.lowercase() == "false" -> false
                    value.toLongOrNull() != null -> value.toLong()
                    else -> value
                }
                caps["appium:$key"] = parsedValue
            }
        }

        return caps
    }
}

/**
 * iOS physical device definition.
 */
@Serializable
data class IosPhysicalDevice(
    val id: String,
    val name: String,

    @SerialName("device_name")
    val deviceName: String,

    @SerialName("platform_version")
    val platformVersion: String,

    val udid: String = "",
    val enabled: Boolean = false,
    val tags: List<String> = emptyList(),
    val capabilities: Map<String, String> = emptyMap()
) {
    /**
     * Convert to Appium capabilities map.
     */
    fun toCapabilities(): Map<String, Any> {
        val caps = mutableMapOf<String, Any>(
            "platformName" to "iOS",
            "appium:deviceName" to deviceName,
            "appium:platformVersion" to platformVersion,
            "appium:automationName" to (capabilities["automationName"] ?: "XCUITest")
        )

        if (udid.isNotBlank()) {
            caps["appium:udid"] = udid
        }

        capabilities.forEach { (key, value) ->
            if (key != "automationName") {
                caps["appium:$key"] = when (value.lowercase()) {
                    "true" -> true
                    "false" -> false
                    else -> value
                }
            }
        }

        return caps
    }
}

/**
 * iOS global settings.
 */
@Serializable
data class IosGlobalSettings(
    @SerialName("appium_host")
    val appiumHost: String = "127.0.0.1",

    @SerialName("appium_port")
    val appiumPort: Int = 4723,

    @SerialName("xcode_path")
    val xcodePath: String = "/Applications/Xcode.app",

    @SerialName("derived_data_path")
    val derivedDataPath: String = "",

    @SerialName("startup_timeout")
    val startupTimeout: Long = 180000,

    @SerialName("new_command_timeout")
    val newCommandTimeout: Int = 300,

    @SerialName("auto_launch_simulator")
    val autoLaunchSimulator: Boolean = true,

    @SerialName("wda_startup_retries")
    val wdaStartupRetries: Int = 3,

    @SerialName("wda_startup_retry_interval")
    val wdaStartupRetryInterval: Long = 10000,

    @SerialName("use_new_wda")
    val useNewWda: Boolean = false,

    @SerialName("use_prebuilt_wda")
    val usePrebuiltWda: Boolean = true,

    @SerialName("screenshot_quality")
    val screenshotQuality: Int = 2,

    @SerialName("mjpeg_server_port")
    val mjpegServerPort: Int = 9100
)

// ═══════════════════════════════════════════════════════════════════════════════
// WEB/DOTCOM CONFIGURATION
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Web browser configuration.
 */
@Serializable
data class WebConfig(
    @SerialName("default_browser")
    val defaultBrowser: String,

    @SerialName("browsers")
    val browsers: List<BrowserConfig>,

    @SerialName("responsive_viewports")
    val responsiveViewports: List<ViewportConfig> = emptyList(),

    @SerialName("global_settings")
    val globalSettings: WebGlobalSettings = WebGlobalSettings()
) {
    /**
     * Get a browser by ID.
     */
    fun getBrowser(id: String): BrowserConfig? = browsers.find { it.id == id }

    /**
     * Get the default browser.
     */
    fun getDefaultBrowser(): BrowserConfig? = getBrowser(defaultBrowser)

    /**
     * Get all enabled browsers.
     */
    fun getEnabledBrowsers(): List<BrowserConfig> = browsers.filter { it.enabled }

    /**
     * Get browsers by tag.
     */
    fun getBrowsersByTag(tag: String): List<BrowserConfig> =
        browsers.filter { tag in it.tags && it.enabled }

    /**
     * Get a viewport by ID.
     */
    fun getViewport(id: String): ViewportConfig? = responsiveViewports.find { it.id == id }

    /**
     * Get viewports by tag.
     */
    fun getViewportsByTag(tag: String): List<ViewportConfig> =
        responsiveViewports.filter { tag in it.tags }
}

/**
 * Browser configuration.
 */
@Serializable
data class BrowserConfig(
    val id: String,
    val name: String,

    @SerialName("browser_name")
    val browserName: String,

    @SerialName("browser_version")
    val browserVersion: String = "latest",

    val platform: String = "ANY",
    val headless: Boolean = false,

    @SerialName("window_size")
    val windowSize: WindowSize? = null,

    @SerialName("mobile_emulation")
    val mobileEmulation: MobileEmulation? = null,

    val enabled: Boolean = true,
    val tags: List<String> = emptyList(),
    val options: BrowserOptions = BrowserOptions(),
    val capabilities: Map<String, String> = emptyMap()
) {
    /**
     * Check if this is a Chrome-based browser.
     */
    fun isChrome(): Boolean = browserName.lowercase() in listOf("chrome", "chromium")

    /**
     * Check if this is Firefox.
     */
    fun isFirefox(): Boolean = browserName.lowercase() == "firefox"

    /**
     * Check if this is Edge.
     */
    fun isEdge(): Boolean = browserName.lowercase() in listOf("edge", "microsoftedge")

    /**
     * Check if this is Safari.
     */
    fun isSafari(): Boolean = browserName.lowercase() == "safari"

    /**
     * Check if mobile emulation is enabled.
     */
    fun hasMobileEmulation(): Boolean = mobileEmulation != null
}

/**
 * Window size configuration.
 */
@Serializable
data class WindowSize(
    val width: Int,
    val height: Int
)

/**
 * Mobile emulation configuration.
 */
@Serializable
data class MobileEmulation(
    @SerialName("device_name")
    val deviceName: String? = null,

    val width: Int? = null,
    val height: Int? = null,

    @SerialName("device_scale_factor")
    val deviceScaleFactor: Double? = null,

    @SerialName("user_agent")
    val userAgent: String? = null
)

/**
 * Browser-specific options.
 */
@Serializable
data class BrowserOptions(
    val args: List<String> = emptyList(),
    val excludeSwitches: List<String> = emptyList(),
    val prefs: Map<String, String> = emptyMap(),
    val mobileEmulation: MobileEmulationOption? = null
)

/**
 * Mobile emulation option for Chrome.
 */
@Serializable
data class MobileEmulationOption(
    val deviceName: String? = null,
    val deviceMetrics: DeviceMetrics? = null,
    val userAgent: String? = null
)

/**
 * Device metrics for mobile emulation.
 */
@Serializable
data class DeviceMetrics(
    val width: Int,
    val height: Int,
    val pixelRatio: Double = 1.0,
    val touch: Boolean = true
)

/**
 * Responsive viewport configuration.
 */
@Serializable
data class ViewportConfig(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,

    @SerialName("device_scale_factor")
    val deviceScaleFactor: Int = 1,

    val tags: List<String> = emptyList()
)

/**
 * Web global settings.
 */
@Serializable
data class WebGlobalSettings(
    @SerialName("selenium_hub_url")
    val seleniumHubUrl: String = "",

    @SerialName("use_remote_webdriver")
    val useRemoteWebDriver: Boolean = false,

    @SerialName("implicit_wait")
    val implicitWait: Int = 20,

    @SerialName("page_load_timeout")
    val pageLoadTimeout: Int = 60,

    @SerialName("script_timeout")
    val scriptTimeout: Int = 30,

    @SerialName("download_directory")
    val downloadDirectory: String = "./downloads",

    @SerialName("screenshots_directory")
    val screenshotsDirectory: String = "./screenshots",

    @SerialName("enable_network_logging")
    val enableNetworkLogging: Boolean = false,

    @SerialName("enable_performance_logging")
    val enablePerformanceLogging: Boolean = false,

    @SerialName("driver_manager_enabled")
    val driverManagerEnabled: Boolean = true,

    @SerialName("browser_binary_path")
    val browserBinaryPath: String = ""
)

// ═══════════════════════════════════════════════════════════════════════════════
// PLATFORM CONFIG LOADER
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Loads and manages platform-specific configurations.
 */
class PlatformConfigLoader(
    private val configDir: File
) {
    private val logger = LoggerFactory.getLogger(PlatformConfigLoader::class.java)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // Cached configs
    private var androidConfig: AndroidConfig? = null
    private var iosConfig: IosConfig? = null
    private var webConfig: WebConfig? = null

    companion object {
        const val ANDROID_FOLDER = "android"
        const val IOS_FOLDER = "ios"
        const val DOTCOM_FOLDER = "dotcom"
        const val EMULATORS_FILE = "emulators.json"
        const val SIMULATORS_FILE = "simulators.json"
        const val BROWSERS_FILE = "browsers.json"

        /**
         * Create loader with default config directory.
         */
        fun default(): PlatformConfigLoader = PlatformConfigLoader(File("config"))
    }

    /**
     * Load Android emulator configuration.
     */
    fun loadAndroidConfig(refresh: Boolean = false): AndroidConfig {
        if (androidConfig != null && !refresh) {
            return androidConfig!!
        }

        val configFile = File(configDir, "$ANDROID_FOLDER/$EMULATORS_FILE")
        require(configFile.exists()) {
            "Android config not found: ${configFile.absolutePath}"
        }

        logger.info("Loading Android config from ${configFile.absolutePath}")
        androidConfig = json.decodeFromString(configFile.readText())
        return androidConfig!!
    }

    /**
     * Load iOS simulator configuration.
     */
    fun loadIosConfig(refresh: Boolean = false): IosConfig {
        if (iosConfig != null && !refresh) {
            return iosConfig!!
        }

        val configFile = File(configDir, "$IOS_FOLDER/$SIMULATORS_FILE")
        require(configFile.exists()) {
            "iOS config not found: ${configFile.absolutePath}"
        }

        logger.info("Loading iOS config from ${configFile.absolutePath}")
        iosConfig = json.decodeFromString(configFile.readText())
        return iosConfig!!
    }

    /**
     * Load Web browser configuration.
     */
    fun loadWebConfig(refresh: Boolean = false): WebConfig {
        if (webConfig != null && !refresh) {
            return webConfig!!
        }

        val configFile = File(configDir, "$DOTCOM_FOLDER/$BROWSERS_FILE")
        require(configFile.exists()) {
            "Web config not found: ${configFile.absolutePath}"
        }

        logger.info("Loading Web config from ${configFile.absolutePath}")
        webConfig = json.decodeFromString(configFile.readText())
        return webConfig!!
    }

    /**
     * Load all configurations.
     */
    fun loadAll(): Triple<AndroidConfig?, IosConfig?, WebConfig?> {
        val android = try { loadAndroidConfig() } catch (e: Exception) {
            logger.warn("Failed to load Android config: ${e.message}")
            null
        }

        val ios = try { loadIosConfig() } catch (e: Exception) {
            logger.warn("Failed to load iOS config: ${e.message}")
            null
        }

        val web = try { loadWebConfig() } catch (e: Exception) {
            logger.warn("Failed to load Web config: ${e.message}")
            null
        }

        return Triple(android, ios, web)
    }

    /**
     * Check which platform configs are available.
     */
    fun getAvailablePlatforms(): Set<String> {
        val available = mutableSetOf<String>()

        if (File(configDir, "$ANDROID_FOLDER/$EMULATORS_FILE").exists()) {
            available.add("android")
        }
        if (File(configDir, "$IOS_FOLDER/$SIMULATORS_FILE").exists()) {
            available.add("ios")
        }
        if (File(configDir, "$DOTCOM_FOLDER/$BROWSERS_FILE").exists()) {
            available.add("web")
        }

        return available
    }

    /**
     * Get Android emulator by ID.
     */
    fun getAndroidEmulator(id: String): AndroidEmulator? {
        return loadAndroidConfig().getEmulator(id)
    }

    /**
     * Get iOS simulator by ID.
     */
    fun getIosSimulator(id: String): IosSimulator? {
        return loadIosConfig().getSimulator(id)
    }

    /**
     * Get Web browser by ID.
     */
    fun getWebBrowser(id: String): BrowserConfig? {
        return loadWebConfig().getBrowser(id)
    }

    /**
     * Get default Android emulator.
     */
    fun getDefaultAndroidEmulator(): AndroidEmulator? {
        return loadAndroidConfig().getDefaultEmulator()
    }

    /**
     * Get default iOS simulator.
     */
    fun getDefaultIosSimulator(): IosSimulator? {
        return loadIosConfig().getDefaultSimulator()
    }

    /**
     * Get default Web browser.
     */
    fun getDefaultWebBrowser(): BrowserConfig? {
        return loadWebConfig().getDefaultBrowser()
    }

    /**
     * Clear cached configurations.
     */
    fun clearCache() {
        androidConfig = null
        iosConfig = null
        webConfig = null
    }

    /**
     * Print configuration summary.
     */
    fun printSummary() {
        println("Platform Configuration Summary")
        println("═".repeat(50))
        println("Config directory: ${configDir.absolutePath}")
        println()

        try {
            val android = loadAndroidConfig()
            println("Android:")
            println("  Default emulator: ${android.defaultEmulator}")
            println("  Emulators: ${android.emulators.size} (${android.getEnabledEmulators().size} enabled)")
            println("  Physical devices: ${android.physicalDevices.size}")
        } catch (e: Exception) {
            println("Android: Not configured")
        }

        println()

        try {
            val ios = loadIosConfig()
            println("iOS:")
            println("  Default simulator: ${ios.defaultSimulator}")
            println("  Simulators: ${ios.simulators.size} (${ios.getEnabledSimulators().size} enabled)")
            println("  Physical devices: ${ios.physicalDevices.size}")
        } catch (e: Exception) {
            println("iOS: Not configured")
        }

        println()

        try {
            val web = loadWebConfig()
            println("Web/Dotcom:")
            println("  Default browser: ${web.defaultBrowser}")
            println("  Browsers: ${web.browsers.size} (${web.getEnabledBrowsers().size} enabled)")
            println("  Viewports: ${web.responsiveViewports.size}")
        } catch (e: Exception) {
            println("Web: Not configured")
        }
    }
}
