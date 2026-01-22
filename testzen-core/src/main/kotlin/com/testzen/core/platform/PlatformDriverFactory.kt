package com.testzen.core.platform

import com.testzen.core.config.TestZenConfig
import com.testzen.core.model.Platform
import io.appium.java_client.AppiumDriver
import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.options.UiAutomator2Options
import io.appium.java_client.ios.IOSDriver
import io.appium.java_client.ios.options.XCUITestOptions
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.Duration

/**
 * Factory for creating platform-specific WebDriver instances.
 */
class PlatformDriverFactory(
    private val appiumUrl: String = "http://127.0.0.1:4723",
    private val config: TestZenConfig = TestZenConfig()
) {
    private val logger = LoggerFactory.getLogger(PlatformDriverFactory::class.java)

    /**
     * Create a driver for the specified platform.
     *
     * @param platform Target platform
     * @param deviceId Device/emulator ID (optional)
     * @param appPath Path to app file (optional, for mobile)
     * @param packageName Android package name (optional)
     * @param bundleId iOS bundle ID (optional)
     * @param baseUrl Base URL for web testing (optional)
     */
    fun createDriver(
        platform: Platform,
        deviceId: String? = null,
        appPath: String? = null,
        packageName: String? = null,
        bundleId: String? = null,
        baseUrl: String? = null
    ): WebDriver {
        logger.info("Creating driver for platform: $platform")

        return when (platform) {
            Platform.ANDROID -> createAndroidDriver(deviceId, appPath, packageName)
            Platform.IOS -> createIOSDriver(deviceId, appPath, bundleId)
            Platform.WEB -> createWebDriver(baseUrl)
        }
    }

    /**
     * Create Android driver using Appium UiAutomator2.
     */
    private fun createAndroidDriver(
        deviceId: String?,
        appPath: String?,
        packageName: String?
    ): AndroidDriver {
        val options = UiAutomator2Options().apply {
            // Device selection
            deviceId?.let { setUdid(it) }

            // App configuration
            appPath?.let { setApp(it) }
            packageName?.let {
                setAppPackage(it)
                // Try to get main activity
                setAppActivity("$it.MainActivity")
            }

            // Automation settings
            setAutomationName("UiAutomator2")
            setNewCommandTimeout(Duration.ofSeconds(config.actionTimeout))

            // Don't reset app state between sessions
            setNoReset(true)

            // Allow insecure features for testing
            setCapability("appium:allowInsecureFeatures", listOf("adb_shell"))
        }

        logger.info("Connecting to Appium at $appiumUrl for Android")
        return AndroidDriver(URL(appiumUrl), options).also {
            configureTimeouts(it)
        }
    }

    /**
     * Create iOS driver using Appium XCUITest.
     */
    private fun createIOSDriver(
        deviceId: String?,
        appPath: String?,
        bundleId: String?
    ): IOSDriver {
        val options = XCUITestOptions().apply {
            // Device selection
            deviceId?.let { setUdid(it) }

            // App configuration
            appPath?.let { setApp(it) }
            bundleId?.let { setBundleId(it) }

            // Automation settings
            setAutomationName("XCUITest")
            setNewCommandTimeout(Duration.ofSeconds(config.actionTimeout))

            // Don't reset app state
            setNoReset(true)
        }

        logger.info("Connecting to Appium at $appiumUrl for iOS")
        return IOSDriver(URL(appiumUrl), options).also {
            configureTimeouts(it)
        }
    }

    /**
     * Create web driver using Selenium.
     */
    private fun createWebDriver(baseUrl: String?): WebDriver {
        val driver = when (config.browserType.lowercase()) {
            "firefox" -> createFirefoxDriver()
            else -> createChromeDriver()
        }

        configureTimeouts(driver)

        // Navigate to base URL if provided
        baseUrl?.let {
            logger.info("Navigating to base URL: $it")
            driver.get(it)
        }

        return driver
    }

    private fun createChromeDriver(): ChromeDriver {
        val options = ChromeOptions().apply {
            if (config.headless) {
                addArguments("--headless=new")
            }
            addArguments("--no-sandbox")
            addArguments("--disable-dev-shm-usage")
            addArguments("--disable-gpu")
            addArguments("--window-size=1920,1080")

            // Enable network logging
            setCapability("goog:loggingPrefs", mapOf("performance" to "ALL"))
        }

        logger.info("Creating Chrome driver (headless: ${config.headless})")
        return ChromeDriver(options)
    }

    private fun createFirefoxDriver(): FirefoxDriver {
        val options = FirefoxOptions().apply {
            if (config.headless) {
                addArguments("--headless")
            }
        }

        logger.info("Creating Firefox driver (headless: ${config.headless})")
        return FirefoxDriver(options)
    }

    private fun configureTimeouts(driver: WebDriver) {
        driver.manage().timeouts().apply {
            implicitlyWait(Duration.ofSeconds(config.implicitWait))
            pageLoadTimeout(Duration.ofSeconds(config.pageLoadTimeout))
        }
    }

    companion object {
        /**
         * Quick helper to create an Android driver.
         */
        fun android(
            appiumUrl: String = "http://127.0.0.1:4723",
            deviceId: String? = null,
            packageName: String? = null
        ): AndroidDriver {
            return PlatformDriverFactory(appiumUrl).createDriver(
                Platform.ANDROID,
                deviceId = deviceId,
                packageName = packageName
            ) as AndroidDriver
        }

        /**
         * Quick helper to create an iOS driver.
         */
        fun ios(
            appiumUrl: String = "http://127.0.0.1:4723",
            deviceId: String? = null,
            bundleId: String? = null
        ): IOSDriver {
            return PlatformDriverFactory(appiumUrl).createDriver(
                Platform.IOS,
                deviceId = deviceId,
                bundleId = bundleId
            ) as IOSDriver
        }

        /**
         * Quick helper to create a web driver.
         */
        fun web(
            baseUrl: String? = null,
            headless: Boolean = false
        ): WebDriver {
            val config = TestZenConfig(headless = headless)
            return PlatformDriverFactory(config = config).createDriver(
                Platform.WEB,
                baseUrl = baseUrl
            )
        }
    }
}
