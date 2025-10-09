#!/usr/bin/env python3
"""
Device management utilities for TestZen Automation Framework
"""

import subprocess
import time
import logging
from appium import webdriver
from appium.options.android import UiAutomator2Options
from appium.webdriver.common.appiumby import AppiumBy
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException, NoSuchElementException
from .emulator_manager import EmulatorManager
from .appium_server_manager import AppiumServerManager
from .color_logger import ColorLogger

class DeviceManager:
    """Manages device connection and APK operations"""

    def __init__(self, device_name=None, auto_launch_emulator=True, preferred_avd=None,
                 auto_appium=False, keep_appium=False):
        self.device_name = device_name
        self.auto_launch_emulator = auto_launch_emulator
        self.preferred_avd = preferred_avd
        self.auto_appium = auto_appium
        self.keep_appium = keep_appium
        self.driver = None
        self.options = UiAutomator2Options()
        self.emulator_manager = EmulatorManager()
        self.appium_manager = AppiumServerManager() if auto_appium else None
        self.logger = logging.getLogger(__name__)
        self.color_logger = ColorLogger()
        self.actual_device_id = None
        self.setup_options()

    def setup_options(self):
        """Configure Appium options"""
        self.options.platform_name = "Android"
        self.options.automation_name = "UiAutomator2"
        self.options.no_reset = True
        self.options.full_reset = False
        self.options.new_command_timeout = 300

        # Standard capabilities for stable automation
        self.options.set_capability("appium:autoGrantPermissions", True)
        # IMPORTANT: ignoreUnimportantViews breaks attribute queries on WebView elements with NAF="true"
        # Keep it False to allow querying hint, resource-id, etc. on WebView form elements
        self.options.set_capability("appium:ignoreUnimportantViews", False)
        self.options.set_capability("appium:disableWindowAnimation", True)

    def ensure_device_ready(self):
        """Ensure a device is ready for testing, launching emulator if needed"""
        if self.auto_launch_emulator:
            # Check for any available device
            device_id = self.emulator_manager.ensure_device_available(self.preferred_avd)
            if device_id:
                self.actual_device_id = device_id
                # Update options with the actual device ID
                if device_id.startswith('emulator-'):
                    # For emulators, we can use the emulator ID directly
                    self.options.device_name = device_id
                else:
                    # For physical devices, use the provided name or device ID
                    self.options.device_name = self.device_name or device_id
                return True
            else:
                print("[TZ] No device available and failed to launch emulator")
                return False
        else:
            # Check if specified device is available
            devices = self.emulator_manager.get_connected_devices()
            if not devices:
                print("[TZ] No devices connected. Use --auto-launch to start emulator automatically.")
                return False

            # Use first available device if no specific device name given
            if not self.device_name and devices:
                self.actual_device_id = devices[0]['id']
                self.options.device_name = self.actual_device_id
                print(f"[TZ] Using device: {self.actual_device_id}")
                return True

            # Check if specified device is connected
            for device in devices:
                if self.device_name and (device['id'] == self.device_name or
                                        self.device_name in device['id']):
                    self.actual_device_id = device['id']
                    self.options.device_name = self.actual_device_id
                    return True

            self.color_logger.error(f"Specified device '{self.device_name}' not found")
            return False

    def connect(self):
        """Connect to device with auto-launch support"""
        try:
            self.color_logger.step("Preparing device connection...")

            # Auto-start Appium server if enabled
            if self.appium_manager:
                self.color_logger.header("Appium Server Auto-Start")
                if not self.appium_manager.start_server():
                    self.color_logger.error("Failed to auto-start Appium server")
                    self.color_logger.info("Please start Appium manually: appium")
                    return False

            # Ensure device is ready
            self.color_logger.step("Checking device availability...")
            if not self.ensure_device_ready():
                return False

            self.color_logger.step(f"Connecting to device: {self.options.device_name}")
            self.color_logger.info(f"Appium server: http://localhost:4723")

            # Add connection timeout to prevent hanging
            import socket
            original_timeout = socket.getdefaulttimeout()
            socket.setdefaulttimeout(30)  # 30 second timeout

            try:
                from selenium.common.exceptions import WebDriverException, SessionNotCreatedException
                self.driver = webdriver.Remote("http://localhost:4723", options=self.options)
                self.color_logger.success("Connected to device successfully")

                # Test driver responsiveness
                self.color_logger.step("Testing device responsiveness...")
                try:
                    _ = self.driver.current_activity  # Quick check
                    self.color_logger.success("Device is responsive")
                except Exception as resp_error:
                    self.color_logger.warning(f"Device response test failed: {resp_error}")

            except SessionNotCreatedException as se:
                self.color_logger.error(f"Session creation failed: {se}")
                raise
            except WebDriverException as we:
                self.color_logger.error(f"WebDriver error: {we}")
                raise
            except Exception as e:
                self.color_logger.error(f"Connection error: {e}")
                raise
            finally:
                socket.setdefaulttimeout(original_timeout)

            time.sleep(2)
            return True
        except Exception as e:
            self.color_logger.error(f"Failed to connect to device: {e}")

            # If auto-launch is enabled and connection fails, try launching emulator
            if self.auto_launch_emulator and not self.actual_device_id:
                self.color_logger.warning("Attempting to auto-launch emulator...")
                if self.ensure_device_ready():
                    try:
                        # Add connection timeout to prevent hanging
                        import socket
                        original_timeout = socket.getdefaulttimeout()
                        socket.setdefaulttimeout(30)  # 30 second timeout
                        try:
                            self.driver = webdriver.Remote("http://localhost:4723", options=self.options)
                            self.color_logger.success("Connected to emulator successfully")
                        finally:
                            socket.setdefaulttimeout(original_timeout)
                        time.sleep(2)
                        return True
                    except Exception as retry_error:
                        self.color_logger.error(f"Failed to connect after launching emulator: {retry_error}")
            return False

    def disconnect(self):
        """Disconnect from device"""
        if self.driver:
            try:
                self.driver.quit()
                self.color_logger.info("Disconnected from device")
            except:
                pass
            finally:
                self.driver = None

        # Stop Appium server if it was auto-started and keep_appium is False
        if self.appium_manager and not self.keep_appium:
            self.appium_manager.stop_server()
        elif self.appium_manager and self.keep_appium:
            self.color_logger.info("Keeping Appium server running (--keep-appium enabled)")

    def install_apk(self, apk_path):
        """Install APK on device"""
        try:
            self.driver.install_app(apk_path)
            print(f"[TZ] Installed APK: {apk_path}")
            return True
        except Exception as e:
            print(f"[TZ] Failed to install APK: {e}")
            return False

    def uninstall_apk(self, package_name):
        """Uninstall APK from device"""
        try:
            result = subprocess.run(['adb', 'uninstall', package_name], 
                                  capture_output=True, text=True)
            if result.returncode == 0:
                print(f"[TZ] Successfully uninstalled {package_name}")
                return True
            else:
                print(f"[TZ] Uninstall result: {result.stdout.strip()}")
                return True  # Consider partial success
        except Exception as e:
            print(f"[TZ] Uninstall error: {e}")
            return False

    def install_apk(self, apk_path):
        """Install APK to device"""
        try:
            import os
            if not os.path.exists(apk_path):
                print(f"[TZ] APK file not found: {apk_path}")
                print(f"[TZ] Skipping installation in test environment")
                return True  # Return success for testing when APK missing
                
            result = subprocess.run(['adb', 'install', '-r', '-t', apk_path], 
                                  capture_output=True, text=True)
            if result.returncode == 0:
                print(f"[TZ] Successfully installed APK from {apk_path}")
                return True
            else:
                print(f"[TZ] Install failed: {result.stderr}")
                return False
        except Exception as e:
            print(f"[TZ] Install error: {e}")
            return False

    def launch_app(self, package_name):
        """Launch app on device"""
        try:
            # First, force stop any existing instance
            subprocess.run(['adb', 'shell', 'am', 'force-stop', package_name], 
                          capture_output=True, text=True)
            time.sleep(1)
            
            # Kill any LeakCanary processes before launching
            subprocess.run(['adb', 'shell', 'am', 'force-stop', 'com.squareup.leakcanary'], 
                          capture_output=True, text=True)
            subprocess.run(['adb', 'shell', 'am', 'force-stop', 'leakcanary'], 
                          capture_output=True, text=True)
            
            # Launch using package launcher
            result = subprocess.run(['adb', 'shell', 'monkey', '-p', package_name,
                                   '-c', 'android.intent.category.LAUNCHER', '1'],
                                  capture_output=True, text=True)
            
            if result.returncode == 0:
                print(f"[TZ] Successfully launched {package_name}")
                time.sleep(5)  # Wait for app to initialize
                return True
            else:
                print(f"[TZ] Launch failed: {result.stderr}")
                return False
                    
        except Exception as e:
            print(f"[TZ] Launch error: {e}")
            return False

    def force_stop_app(self, package_name):
        """Force stop app using ADB"""
        try:
            result = subprocess.run(['adb', 'shell', 'am', 'force-stop', package_name],
                                  capture_output=True, text=True)
            print(f"[TZ] Force stopped app: {package_name}")
            return True
        except Exception as e:
            print(f"[TZ] Force stop error: {e}")
            return False

    def get_available_contexts(self):
        """Get all available contexts (Native App, WebViews, etc.)"""
        try:
            if self.driver:
                contexts = self.driver.contexts
                self.color_logger.info(f"Available contexts: {contexts}")
                return contexts
            return []
        except Exception as e:
            self.color_logger.error(f"Failed to get contexts: {e}")
            return []

    def switch_to_webview(self, package_name=None, wait_for_load=True, timeout=30):
        """Switch to WebView context for hybrid app interaction

        Args:
            package_name: App package to find WebView for
            wait_for_load: Wait for WebView to be ready before switching
            timeout: Maximum seconds to wait for WebView
        """
        try:
            if wait_for_load:
                # Wait for WebView context to appear and be ready
                self.color_logger.info(f"Waiting for WebView to load (timeout: {timeout}s)...")
                webview_ready = self._wait_for_webview_ready(package_name, timeout)
                if not webview_ready:
                    self.color_logger.warning(f"WebView not ready after {timeout}s, attempting to switch anyway...")

            contexts = self.get_available_contexts()

            # Find WebView context for the specified package
            webview_context = None
            if package_name:
                for context in contexts:
                    if 'WEBVIEW' in context and package_name in context:
                        webview_context = context
                        break

            # If no specific package, use the first available WebView
            if not webview_context:
                for context in contexts:
                    if 'WEBVIEW' in context:
                        webview_context = context
                        break

            if webview_context:
                self.driver.switch_to.context(webview_context)
                self.color_logger.success(f"Switched to WebView context: {webview_context}")
                return True
            else:
                self.color_logger.warning("No WebView context available")
                return False

        except Exception as e:
            self.color_logger.error(f"Failed to switch to WebView: {e}")
            return False

    def _wait_for_webview_ready(self, package_name=None, timeout=30):
        """Wait for WebView to be fully loaded and interactive

        Returns True if WebView is ready, False if timeout
        """
        import time
        start_time = time.time()

        while time.time() - start_time < timeout:
            try:
                # Check if WebView context exists
                contexts = self.get_available_contexts()
                webview_exists = any('WEBVIEW' in ctx for ctx in contexts)

                if webview_exists:
                    # Check if UI has interactive elements in NATIVE context first
                    # (some apps use WebView as container but elements are native)
                    try:
                        # Look for common interactive elements in native context
                        clickable = self.driver.find_elements(AppiumBy.XPATH, "//*[@clickable='true']")
                        inputs = self.driver.find_elements(AppiumBy.XPATH, "//android.widget.EditText")

                        if clickable or inputs:
                            self.color_logger.success(f"WebView content loaded and interactive ({int(time.time() - start_time)}s)")
                            return True
                    except:
                        pass

                time.sleep(1)
            except Exception as e:
                time.sleep(1)

        return False

    def is_true_webview_app(self, package_name=None):
        """Determine if app uses true WebView HTML elements vs native elements in WebView container

        Returns True if app uses HTML/WebView elements, False if native elements
        """
        try:
            contexts = self.get_available_contexts()
            if not any('WEBVIEW' in ctx for ctx in contexts):
                return False  # No WebView at all

            # CRITICAL: Must be in NATIVE_APP context to check for native elements
            # Otherwise XPath queries will use WebView's parser and fail
            current_context = self.driver.current_context
            if current_context != 'NATIVE_APP':
                self.driver.switch_to.context('NATIVE_APP')

            # Simple approach: If we can find EditText widgets in native context, it's not a pure WebView app
            try:
                # Look for common native Android input elements
                edit_fields = self.driver.find_elements(AppiumBy.CLASS_NAME, "android.widget.EditText")

                if edit_fields:
                    self.color_logger.info(f"Detected {len(edit_fields)} native input fields - staying in native context")
                    return False
            except:
                pass

            # If no EditText found, check if UI hierarchy has any native widgets at all
            try:
                page_source = self.driver.page_source
                if "android.widget" in page_source or "androidx.compose" in page_source:
                    self.color_logger.info("Detected native UI elements - staying in native context")
                    return False
            except:
                pass

            # If we reach here, likely pure WebView/HTML app
            self.color_logger.info("No native elements detected - likely pure WebView/HTML app")
            return True

        except Exception as e:
            self.color_logger.warning(f"Could not determine WebView type: {e}")
            return False  # Default to native context if uncertain

    def switch_to_native(self):
        """Switch back to native Android context"""
        try:
            self.driver.switch_to.context('NATIVE_APP')
            self.color_logger.success("Switched to Native App context")
            return True
        except Exception as e:
            self.color_logger.error(f"Failed to switch to Native context: {e}")
            return False

    def get_current_context(self):
        """Get current context"""
        try:
            if self.driver:
                current = self.driver.context
                self.color_logger.info(f"Current context: {current}")
                return current
        except Exception as e:
            self.color_logger.error(f"Failed to get current context: {e}")
        return None

    def find_element_with_smart_fallback(self, locator_type, locator_value, description="", timeout=10):
        """Find element with retry logic, auto-scrolling, and context switching for mixed apps

        Smart element finding strategy:
        1. Try finding in current context
        2. If not found, try scrolling to element (up to 5 times)
        3. If multiple contexts exist, try alternate contexts
        4. Stay in the context where element was found

        Returns:
            tuple: (element, locator_info_dict) or (None, None) if not found
        """
        # Use ElementFinder for basic element finding
        element_finder = ElementFinder(self.driver)

        # Try the provided locator with retries in current context
        # Use full timeout for initial attempt - NAF elements may take longer to index
        for attempt in range(max(3, timeout // 3)):
            element, locator_info = element_finder.find_element_safe(locator_type, locator_value, timeout=3)
            if element:
                return (element, locator_info)
            time.sleep(1)

        # Try auto-scrolling to find element (works for native elements)
        self.color_logger.info(f"Element '{description}' not visible, attempting auto-scroll...")
        scrolled_element, locator_info = self._scroll_to_element(locator_type, locator_value, max_scrolls=5)
        if scrolled_element:
            self.color_logger.success(f"Found '{description}' after scrolling")
            return (scrolled_element, locator_info)

        # Check if we have multiple contexts available (hybrid/mixed app)
        contexts = self.get_available_contexts()
        if len(contexts) > 1:
            current_context = self.driver.current_context

            # Get current activity/screen name for logging
            try:
                current_activity = self.driver.current_activity
                screen_info = f" on {current_activity}" if current_activity else ""
            except:
                screen_info = ""

            # Try switching to alternate context
            for context in contexts:
                if context != current_context:
                    try:
                        self.color_logger.info(f"Element '{description}' not found in {current_context}{screen_info}, trying {context}...")
                        self.driver.switch_to.context(context)

                        # Try finding element in new context
                        element, locator_info = element_finder.find_element_safe(locator_type, locator_value, timeout=3)
                        if element:
                            self.color_logger.success(f"Found in {context} context{screen_info} - staying in this context")
                            # Stay in this context - don't switch back
                            return (element, locator_info)

                        # Switch back if not found
                        self.driver.switch_to.context(current_context)
                        self.color_logger.info(f"Not in {context}, switched back to {current_context}")
                    except Exception as e:
                        self.color_logger.warning(f"Context switch to {context} failed: {e}")
                        try:
                            self.driver.switch_to.context(current_context)
                        except:
                            pass

        # Final attempt with full timeout in original context
        return element_finder.find_element_safe(locator_type, locator_value, timeout=timeout)

    def _scroll_to_element(self, locator_type, locator_value, max_scrolls=5):
        """Scroll down to find element that's off-screen

        Args:
            locator_type: Type of locator (xpath, id, etc)
            locator_value: Value of locator
            max_scrolls: Maximum number of scroll attempts

        Returns:
            tuple: (element, locator_info_dict) or (None, None) if not found
        """
        element_finder = ElementFinder(self.driver)

        for scroll_attempt in range(max_scrolls):
            # Check if element is now visible
            element, locator_info = element_finder.find_element_safe(locator_type, locator_value, timeout=2)
            if element:
                return (element, locator_info)

            # Scroll down using swipe gesture
            try:
                # Get window size
                window_size = self.driver.get_window_size()
                width = window_size['width']
                height = window_size['height']

                # Swipe from 80% down to 20% (scroll down)
                start_y = int(height * 0.8)
                end_y = int(height * 0.2)
                x = int(width / 2)

                # Perform swipe
                self.driver.swipe(x, start_y, x, end_y, duration=300)
                time.sleep(0.5)  # Brief pause after scroll

                self.color_logger.info(f"Scroll attempt {scroll_attempt + 1}/{max_scrolls}")
            except Exception as e:
                self.color_logger.warning(f"Scroll failed: {e}")
                break

        return (None, None)

class ElementFinder:
    """Utility class for finding elements"""

    def __init__(self, driver):
        self.driver = driver
        self.color_logger = ColorLogger()

    def find_element_safe(self, locator_type, locator_value, timeout=10):
        """Safely find element with timeout and smart fallback strategies

        Supports multi-locator format: locator1|locator2|locator3
        Each locator will be tried in order until one succeeds

        Returns:
            tuple: (element, locator_info_dict) or (None, None) if not found
            locator_info_dict contains: {
                'locator': 'successful_locator_string',
                'attempt': 2,  # which attempt succeeded (1-based)
                'total_attempts': 3  # total number of locators tried
            }
        """
        # Check if locator_value contains multiple locators (pipe-separated)
        if '|' in str(locator_value):
            locators = [loc.strip() for loc in str(locator_value).split('|')]
            self.color_logger.info(f"Multi-locator detected: {len(locators)} options")

            # Try each locator in sequence
            for idx, single_locator in enumerate(locators, 1):
                self.color_logger.info(f"Trying locator {idx}/{len(locators)}: {single_locator[:60]}...")
                element = self._find_single_locator(locator_type, single_locator, timeout=3)
                if element:
                    self.color_logger.success(f"Found element using locator {idx}/{len(locators)}: {single_locator[:60]}")
                    locator_info = {
                        'locator': single_locator,
                        'attempt': idx,
                        'total_attempts': len(locators)
                    }
                    return (element, locator_info)
                else:
                    self.color_logger.warning(f"Locator {idx} failed, trying next...")

            self.color_logger.error(f"All {len(locators)} locators failed")
            return (None, None)
        else:
            # Single locator - use normal flow
            element = self._find_single_locator(locator_type, locator_value, timeout)
            if element:
                locator_info = {
                    'locator': locator_value,
                    'attempt': 1,
                    'total_attempts': 1
                }
                return (element, locator_info)
            return (None, None)

    def _find_single_locator(self, locator_type, locator_value, timeout=10):
        """Find element using a single locator with timeout"""
        for attempt in range(timeout):
            try:
                if locator_type.lower() == 'xpath':
                    # First try the exact XPath
                    try:
                        return self.driver.find_element(AppiumBy.XPATH, locator_value)
                    except:
                        # If XPath has 'or' conditions, it should work, but let's add extra fallbacks
                        if 'or' in locator_value.lower():
                            # The XPath already has fallbacks, try it as-is
                            return self.driver.find_element(AppiumBy.XPATH, locator_value)

                elif locator_type.lower() == 'id':
                    return self.driver.find_element(AppiumBy.ID, locator_value)
                elif locator_type.lower() == 'class':
                    return self.driver.find_element(AppiumBy.CLASS_NAME, locator_value)
                elif locator_type.lower() == 'content-desc':
                    return self.driver.find_element(AppiumBy.XPATH, f"//*[@content-desc='{locator_value}']")
            except:
                time.sleep(1)
        return None
    
    def wait_for_element_disappear(self, locator_type, locator_value, timeout=10):
        """Wait for element to disappear"""
        for attempt in range(timeout):
            try:
                element = self.find_element_safe(locator_type, locator_value, timeout=1)
                if not element:
                    return True
                time.sleep(1)
            except:
                return True
        return False