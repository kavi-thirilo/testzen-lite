#!/usr/bin/env python3
"""
TestZen Mobile Automation Framework
Main execution script for mobile app automation testing
"""

import time
import sys
import os
import logging
import pandas as pd
from src.utils.device_utils import DeviceManager, ElementFinder
from src.utils.excel_utils import ExcelManager
from src.utils.compose_helper import ComposeUIHelper
from src.reports.reporter_factory import ReporterFactory
from src.utils.color_logger import ColorLogger

class TestZenAutomation:
    """Main automation class for mobile app testing"""
    
    def __init__(self, excel_file="tests/sample_mobile_test.xlsx", config=None):
        self.excel_file = excel_file
        self.config = config or {}

        # Extract device configuration
        device_name = self.config.get('device_name', None)
        auto_launch = self.config.get('auto_launch_emulator', True)
        preferred_avd = self.config.get('preferred_avd', None)
        auto_appium = self.config.get('auto_appium', False)
        keep_appium = self.config.get('keep_appium', False)

        self.device_manager = DeviceManager(
            device_name=device_name,
            auto_launch_emulator=auto_launch,
            preferred_avd=preferred_avd,
            auto_appium=auto_appium,
            keep_appium=keep_appium
        )
        self.excel_manager = ExcelManager(excel_file)
        self.element_finder = None
        self.compose_helper = None
        # Use reporter factory to create appropriate reporter based on config
        self.professional_reporter = ReporterFactory.create_reporter(excel_file_name=excel_file)
        self.total_steps = 0
        self.passed_steps = 0
        self.failed_steps = 0
        self.current_app_package = None

        # Initialize color logger
        self.color_logger = ColorLogger()

        # Config-based settings
        self.screenshots_enabled = not self.config.get('no_screenshots', False)
        self.skip_on_fail = self.config.get('skip_on_fail', False)

        # Cleanup settings
        self.cleanup_notifications = self.config.get('cleanup_notifications', True)
        self.cleanup_app_cache = self.config.get('cleanup_app_cache', True)
        self.cleanup_temp_files = self.config.get('cleanup_temp_files', True)
        self.reset_device_settings = self.config.get('reset_device_settings', True)
        
        # Setup logging
        self.logger = logging.getLogger('TestZenAutomation')
        if not self.logger.handlers:
            handler = logging.StreamHandler()
            formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
            handler.setFormatter(formatter)
            self.logger.addHandler(handler)
            self.logger.setLevel(logging.INFO)
    
    def setup(self):
        """Initialize the automation framework"""
        self.color_logger.header("TestZen Automation Setup")

        # Load test data
        self.color_logger.step("Loading test data...")
        if not self.excel_manager.load_test_data():
            self.color_logger.error("Failed to load test data")
            return False

        # Connect to device
        self.color_logger.step("Connecting to device...")
        try:
            if not self.device_manager.connect():
                self.color_logger.error("Failed to connect to device")
                return False
        except Exception as e:
            self.color_logger.error(f"Device connection error: {e}")
            return False

        self.color_logger.step("Initializing test components...")
        self.element_finder = ElementFinder(self.device_manager.driver)
        self.compose_helper = ComposeUIHelper(self.device_manager.driver)

        # Auto-detect and set current app package
        self.color_logger.step("Detecting app package...")
        self._detect_app_package()

        # Ensure app is launched and ready
        if self.current_app_package:
            self.color_logger.step(f"Launching app: {self.current_app_package}")
            if self.device_manager.launch_app(self.current_app_package):
                self.color_logger.success("App launched successfully")

                # Import wait configuration
                try:
                    from config.wait_config import get_app_launch_wait, get_webview_timeout, WEBVIEW_WAIT_CONFIG, is_ci_environment
                    app_wait = get_app_launch_wait()
                    webview_timeout = get_webview_timeout()
                    ci_mode = is_ci_environment()

                    if ci_mode:
                        self.color_logger.info(f"CI environment detected - using adjusted wait times")

                    self.color_logger.info(f"Waiting {app_wait}s for app initialization...")
                    time.sleep(app_wait)
                except ImportError:
                    # Fallback to default values
                    self.color_logger.warning("Wait config not found, using defaults")
                    app_wait = 8
                    webview_timeout = 30
                    time.sleep(app_wait)

                # Auto-detect and handle WebView apps
                self.color_logger.step("Detecting app type (Native/WebView/Hybrid)...")
                contexts = self.device_manager.get_available_contexts()
                self.color_logger.info(f"Available contexts: {contexts}")

                if len(contexts) > 1 and any('WEBVIEW' in ctx for ctx in contexts):
                    # Wait for WebView content to load first (even if staying in native context)
                    self.color_logger.info("WebView detected - verifying content is loaded...")
                    self.device_manager._wait_for_webview_ready(self.current_app_package, webview_timeout)

                    # Determine if we need to switch to WebView context
                    if self.device_manager.is_true_webview_app(self.current_app_package):
                        self.color_logger.info("True WebView/HTML app detected - switching to WebView context")
                        if self.device_manager.switch_to_webview(self.current_app_package, wait_for_load=False, timeout=webview_timeout):
                            self.color_logger.success("WebView context ready - tests will use WebView HTML elements")
                        else:
                            self.color_logger.warning("Could not switch to WebView - will use native context")
                    else:
                        self.color_logger.success("Hybrid app with native elements detected - staying in native context")
                else:
                    self.color_logger.info("Native app detected - using native context")

            else:
                self.color_logger.error("Failed to launch app")
                return False
        else:
            self.color_logger.warning("No app package detected - tests may be unreliable")

        self.color_logger.step("Starting test session...")
        # Initialize reporting
        self.professional_reporter.start_test_session()

        self.color_logger.success("Setup completed successfully")
        return True
    
    def teardown(self):
        """Clean up automation framework with comprehensive cleanup"""
        self.color_logger.header("Test Teardown & Cleanup")

        try:
            # Step 1: Close current app if running
            if self.device_manager.driver and self.current_app_package:
                self.color_logger.step(f"Closing app: {self.current_app_package}")

                try:
                    self.device_manager.driver.terminate_app(self.current_app_package)
                    self.color_logger.success("App terminated successfully")
                except Exception as terminate_error:
                    self.color_logger.warning(f"Appium terminate failed: {terminate_error}")
                    self.color_logger.step("Attempting ADB force-stop...")

                    # Fallback to ADB force-stop
                    try:
                        import subprocess
                        result = subprocess.run(['adb', 'shell', 'am', 'force-stop', self.current_app_package],
                                              capture_output=True, text=True, timeout=10)
                        if result.returncode == 0:
                            self.color_logger.success("App force-stopped successfully")
                        else:
                            self.color_logger.error(f"Force-stop failed: {result.stderr}")
                    except Exception as adb_error:
                        self.color_logger.error(f"ADB force-stop failed: {adb_error}")

            # Step 2: Clear device notifications and temporary data (configurable)
            if self.device_manager.driver and self.cleanup_notifications:
                try:
                    self.color_logger.step("Clearing device notifications...")
                    # Clear notifications
                    self.device_manager.driver.open_notifications()
                    import time
                    time.sleep(1)
                    # Try to find and tap "Clear all" button
                    try:
                        clear_all = self.device_manager.driver.find_element("xpath",
                            "//*[@text='Clear all' or @content-desc='Clear all notifications' or contains(@text, 'Clear')]")
                        clear_all.click()
                        self.color_logger.success("Notifications cleared")
                    except:
                        self.color_logger.info("No notifications to clear or clear button not found")

                    # Return to home screen
                    self.color_logger.step("Returning to home screen...")
                    self.device_manager.driver.press_keycode(3)  # Home button
                    time.sleep(1)
                    self.color_logger.success("Returned to home screen")

                except Exception as cleanup_error:
                    self.color_logger.warning(f"Device cleanup warning: {cleanup_error}")
            elif not self.cleanup_notifications:
                self.color_logger.info("Notification cleanup skipped (disabled in config)")

            # Step 3: Reset device orientation if needed
            try:
                if self.device_manager.driver:
                    self.color_logger.step("Resetting device orientation...")
                    current_orientation = self.device_manager.driver.orientation
                    if current_orientation.lower() != 'portrait':
                        self.device_manager.driver.orientation = 'PORTRAIT'
                        self.color_logger.success("Device orientation reset to portrait")
                    else:
                        self.color_logger.info("Device orientation already correct")
            except Exception as orientation_error:
                self.color_logger.warning(f"Orientation reset warning: {orientation_error}")

        except Exception as e:
            self.color_logger.error(f"App termination error: {e}")

        try:
            # Step 4: Clear temporary files and cache (configurable)
            if self.cleanup_temp_files:
                self.color_logger.step("Cleaning up temporary files...")
            import tempfile
            import shutil
            temp_dir = tempfile.gettempdir()
            testzen_temp_files = []

            # Look for TestZen temporary files
            import glob
            temp_patterns = [
                f"{temp_dir}/testzen_*",
                f"{temp_dir}/appium_*",
                f"{temp_dir}/screenshot_*"
            ]

            for pattern in temp_patterns:
                testzen_temp_files.extend(glob.glob(pattern))

            if testzen_temp_files:
                for temp_file in testzen_temp_files:
                    try:
                        if os.path.isfile(temp_file):
                            os.remove(temp_file)
                        elif os.path.isdir(temp_file):
                            shutil.rmtree(temp_file)
                    except:
                        pass
                    self.color_logger.success(f"Cleaned {len(testzen_temp_files)} temporary files")
                else:
                    self.color_logger.info("No temporary files to clean")
            else:
                self.color_logger.info("Temporary file cleanup skipped (disabled in config)")

            # Step 5: Clear device app data/cache if needed (configurable)
            if self.device_manager.driver and self.current_app_package and self.cleanup_app_cache:
                try:
                    self.color_logger.step("Clearing app cache/data...")
                    import subprocess

                    # Clear app cache (non-destructive)
                    cache_result = subprocess.run(['adb', 'shell', 'pm', 'clear', '--cache-only', self.current_app_package],
                                                capture_output=True, text=True, timeout=10)
                    if cache_result.returncode == 0:
                        self.color_logger.success("App cache cleared")
                    else:
                        self.color_logger.info("App cache clear not needed or not supported")

                except Exception as cache_error:
                    self.color_logger.warning(f"Cache cleanup warning: {cache_error}")
            elif not self.cleanup_app_cache:
                self.color_logger.info("App cache cleanup skipped (disabled in config)")

            # Step 6: Reset device settings if modified during test (configurable)
            if self.device_manager.driver and self.reset_device_settings:
                try:
                    self.color_logger.step("Resetting device settings...")

                    # Reset animation scale if it was disabled for testing
                    subprocess.run(['adb', 'shell', 'settings', 'put', 'global', 'window_animation_scale', '1'],
                                  capture_output=True, text=True, timeout=5)
                    subprocess.run(['adb', 'shell', 'settings', 'put', 'global', 'transition_animation_scale', '1'],
                                  capture_output=True, text=True, timeout=5)
                    subprocess.run(['adb', 'shell', 'settings', 'put', 'global', 'animator_duration_scale', '1'],
                                  capture_output=True, text=True, timeout=5)

                    self.color_logger.success("Device animation settings restored")

                except Exception as settings_error:
                    self.color_logger.warning(f"Settings reset warning: {settings_error}")
            elif not self.reset_device_settings:
                self.color_logger.info("Device settings reset skipped (disabled in config)")

            # Step 7: Disconnect from device
            self.color_logger.step("Disconnecting from device...")
            self.device_manager.disconnect()

            # Step 8: Memory cleanup
            self.color_logger.step("Performing memory cleanup...")
            import gc
            gc.collect()  # Force garbage collection
            self.color_logger.success("Memory cleanup completed")
            
            # Generate reports
            if self.total_steps > 0:
                # Note: Excel file already updated with colored status during execution
                # No need for separate results file as original file contains all results
                
                # Generate professional report
                self.professional_reporter.end_test_session()
                html_path = self.professional_reporter.generate_professional_html_report()
                json_path = self.professional_reporter.save_summary_json()
                
                # Generate professional Test_Summary sheet
                test_name = os.path.basename(self.excel_file).replace('.xlsx', '')
                platform = self.config.get('platform', 'ANDROID').upper()
                execution_time = self.professional_reporter.end_time - self.professional_reporter.start_time if self.professional_reporter.end_time and self.professional_reporter.start_time else None
                additional_info = {
                    'environment': 'Test Environment',
                    'notes': f'Test executed with {"screenshots enabled" if self.screenshots_enabled else "screenshots disabled"}. Skip on fail: {"enabled" if self.skip_on_fail else "disabled"}.'
                }
                
                if self.excel_manager.generate_test_summary(
                    test_name=test_name,
                    platform=platform,
                    execution_time=str(execution_time).split('.')[0] if execution_time else "Unknown",  # Remove microseconds
                    additional_info=additional_info
                ):
                    print(f"[TZ] Test_Summary sheet generated in {self.excel_file}")
                
                if html_path:
                    print(f"[TZ] HTML Report: {html_path}")
                    print(f"[TZ]   → Open in browser to view detailed test report with screenshots")
                if json_path:
                    print(f"[TZ] JSON Summary: {json_path}")
                
                # Final summary of all generated reports
                print(f"\n[TZ] QUICK ACCESS COMMANDS:")
                print("=" * 50)
                if html_path:
                    print(f"[TZ] View HTML Report: open \"{html_path}\"")
                print(f"[TZ] View Excel Results: open \"{os.path.abspath(self.excel_file)}\"")
                print(f"[TZ] Open Reports Folder: open \"{os.path.dirname(html_path) if html_path else 'reports'}\"")
                print("=" * 50)

            # Teardown completion message
            self.color_logger.success("Test teardown and cleanup completed successfully")
            self.color_logger.info("Device and system returned to clean state")
            self.color_logger.separator()

        except Exception as e:
            self.color_logger.error(f"Cleanup error: {e}")
    
    def _take_screenshot(self, filename_prefix, description=""):
        """Take a screenshot and save it with timestamp"""
        try:
            if self.device_manager.driver:
                timestamp = time.strftime("%Y%m%d_%H%M%S")
                filename = f"{filename_prefix}_{timestamp}.png"
                screenshot_path = os.path.join(self.professional_reporter.screenshot_dir, filename)
                
                # Take screenshot
                self.device_manager.driver.save_screenshot(screenshot_path)
                
                # Verify file was created
                if os.path.exists(screenshot_path):
                    return screenshot_path
                else:
                    print(f"[TZ] Screenshot not saved: {screenshot_path}")
                    return None
        except Exception as e:
            print(f"[TZ] Screenshot error: {e}")
            return None
    
    def _should_capture_screenshot(self, action, description):
        """
        Intelligent decision on whether to capture a screenshot after an action.
        Returns True if the action produces meaningful visual results worth documenting.
        """
        
        # Check if screenshots are disabled via config
        if not self.screenshots_enabled:
            return False
        
        # Actions that never need screenshots (process/setup actions)
        no_screenshot_actions = {
            'force_stop',    # App cleanup - no visual result
            'install',       # App installation - no visual result
            'launch',        # App launch - no specific visual result needed
            'banking_demo_login'  # Login flow - internal process
        }
        
        if action in no_screenshot_actions:
            return False
        
        # Navigation actions that don't change meaningful UI state
        navigation_actions = {'scroll', 'swipe', 'navigate'}
        if action in navigation_actions:
            return False
        
        # Check description for navigation-related keywords
        navigation_keywords = [
            'scroll', 'swipe', 'navigate to', 'move to', 'go to',
            'find element', 'search for', 'look for'
        ]
        
        description_lower = description.lower()
        for keyword in navigation_keywords:
            if keyword in description_lower:
                return False
        
        # Actions that DO produce meaningful visual results
        meaningful_actions = {
            'click',         # UI interactions - show result
            'verify',        # Verification - show what was verified
            'input',         # Text entry - show filled forms
            'select',        # Selection - show selected items
            'toggle',        # State changes - show new state
            'tap',           # UI interactions - show result
            'long_press'     # UI interactions - show result
        }
        
        if action in meaningful_actions:
            return True
            
        # Default: if unsure and not explicitly navigation, capture it
        return True
    
    def _get_screenshot_wait_time(self, action, description):
        """
        Determine optimal wait time before taking screenshot based on expected UI transitions.
        Returns wait time in seconds.
        """
        description_lower = description.lower()
        
        # UI transitions that need more time (animations, bottom sheets, modals)
        long_wait_keywords = [
            'profile', 'menu', 'sheet', 'modal', 'popup', 'dialog', 
            'dropdown', 'overlay', 'panel', 'drawer', 'settings'
        ]
        
        # Form interactions that need moderate time (page loads, state changes)
        medium_wait_keywords = [
            'login', 'submit', 'confirm', 'save', 'next', 'continue',
            'verify', 'validate', 'load', 'open', 'close'
        ]
        
        # Check for transitions that typically need longer wait
        for keyword in long_wait_keywords:
            if keyword in description_lower:
                return 3.0  # 3 seconds for complex UI transitions
        
        # Check for moderate transitions
        for keyword in medium_wait_keywords:
            if keyword in description_lower:
                return 2.0  # 2 seconds for form/page transitions
        
        # Specific action-based timing
        if action == 'click':
            # Profile/menu clicks typically trigger animations
            if any(word in description_lower for word in ['profile', 'icon', 'menu']):
                return 3.0  # Extra time for bottom sheets and menus
            return 2.0  # Standard click interactions
            
        elif action == 'verify':
            return 1.5  # Verification usually has immediate visual feedback
            
        elif action in ['input', 'select', 'toggle']:
            return 2.0  # Form interactions need time to update
            
        # Default for other actions
        return 1.5
    
    def execute_step(self, step_data, step_index):
        """Execute a single test step"""
        step_num = step_data.get('S.No', step_index + 1)
        action = str(step_data.get('Action', '')).strip()
        locator_type = str(step_data.get('Locator Type', '')).strip()

        # Build locator value from multiple columns (supports fallback locators)
        locator_value = str(step_data.get('Locator Value', '')).strip()
        locator_value_2 = str(step_data.get('Locator Value 2', '')).strip()
        locator_value_3 = str(step_data.get('Locator Value 3', '')).strip()

        # Build mapping of locator to column name for reporting
        locator_column_map = {}
        if locator_value and locator_value.lower() not in ['nan', 'none', '']:
            locator_column_map[locator_value] = 'Locator Value'
        if locator_value_2 and locator_value_2.lower() not in ['nan', 'none', '']:
            locator_column_map[locator_value_2] = 'Locator Value 2'
        if locator_value_3 and locator_value_3.lower() not in ['nan', 'none', '']:
            locator_column_map[locator_value_3] = 'Locator Value 3'

        # Combine non-empty locator values with pipe separator
        locator_values = [lv for lv in [locator_value, locator_value_2, locator_value_3]
                         if lv and lv.lower() not in ['nan', 'none', '']]
        locator_value = '|'.join(locator_values) if len(locator_values) > 1 else (locator_values[0] if locator_values else '')

        description = str(step_data.get('Description', '')).strip()
        
        # Handle input data
        raw_input = step_data.get('Input Data', '')
        if pd.isna(raw_input) or str(raw_input).lower() in ['nan', 'none', '']:
            input_data = ''
        else:
            input_data = str(raw_input).strip()
        
        print(f"\n[TZ] Step {step_num}: {description}")
        print("-" * 40)

        # Note: WebView context switching removed - elements are accessible in native context

        # Screenshots will be taken BEFORE and AFTER action execution for meaningful steps
        before_screenshot_path = None
        after_screenshot_path = None
        step_start_time = time.time()
        success = False
        result_message = ""
        locator_info = None  # Track which locator actually worked (dict with attempt details)

        # Take BEFORE screenshot for actions that change UI state
        if self._should_capture_screenshot(action, description):
            before_screenshot_path = self._take_screenshot(f"step_{step_num:02d}_before", f"Before {action}: {description}")
            if before_screenshot_path:
                print(f"[TZ] Captured BEFORE screenshot for: {description}")
            time.sleep(0.5)  # Brief pause between before and action

        try:
            if action == 'click':
                # Check if this is an optional step
                is_optional = 'optional' in description.lower() or 'if visible' in description.lower() or 'if prompted' in description.lower()

                element, locator_info = self.device_manager.find_element_with_smart_fallback(locator_type, locator_value, description, timeout=3 if is_optional else 10)
                if element:
                    # Check if element is clickable
                    clickable = element.get_attribute("clickable")
                    if clickable == "false" and "content-desc" in locator_value.lower():
                        # This is likely a Compose element, use special handling
                        print(f"[TZ] Detected non-clickable Compose element, using smart click...")
                        content_desc = element.get_attribute("content-desc")
                        if self.compose_helper.click_compose_element(element):
                            success = True
                            result_message = f"Successfully clicked Compose element using smart handling"
                            print(f"[TZ] Clicked Compose element: {description}")
                        else:
                            # Try coordinate click as last resort
                            bounds = element.get_attribute("bounds")
                            if bounds:
                                import re
                                coords = re.findall(r'\d+', bounds)
                                if len(coords) == 4:
                                    x1, y1, x2, y2 = map(int, coords)
                                    center_x = (x1 + x2) // 2
                                    center_y = (y1 + y2) // 2
                                    if self.compose_helper.click_at_coordinates(center_x, center_y):
                                        success = True
                                        result_message = f"Successfully clicked element at coordinates ({center_x}, {center_y})"
                                        print(f"[TZ] Clicked via coordinates: {description}")
                    else:
                        # Regular clickable element
                        element.click()
                        time.sleep(2)
                        success = True
                        result_message = f"Successfully clicked element"
                        print(f"[TZ] Clicked element: {description}")
                else:
                    if is_optional:
                        success = True  # Don't fail on optional steps
                        result_message = f"Optional element not found (expected behavior)"
                        print(f"[TZ] Optional element not found, continuing: {description}")
                    else:
                        success = False
                        result_message = f"Expected: Clickable element with {locator_type}='{locator_value}' | Actual: Element not found or not clickable"
                        print(f"[TZ] Element not found: {locator_value}")
            
            elif action == 'input':
                element, locator_info = self.device_manager.find_element_with_smart_fallback(locator_type, locator_value, description)
                if element:
                    element.clear()
                    element.send_keys(input_data)
                    time.sleep(1)
                    success = True
                    result_message = f"Successfully entered text: '{input_data}'"
                    print(f"[TZ] Input data: {input_data}")
                else:
                    success = False
                    result_message = f"Input field not found: {locator_value}"
            
            elif action == 'verify':
                element, locator_info = self.device_manager.find_element_with_smart_fallback(locator_type, locator_value, description)
                if element:
                    success = True
                    result_message = f"Verification successful: Element found as expected"
                    print(f"[TZ] Verification successful: {description}")
                else:
                    success = False
                    result_message = f"Verification failed: Element not found"
            
            elif action == 'wait':
                wait_time = float(input_data) if input_data else 5
                time.sleep(wait_time)
                success = True
                result_message = f"Waited {wait_time} seconds"
                print(f"[TZ] Waited {wait_time} seconds")
            
            elif action == 'launch':
                success = self.device_manager.launch_app(locator_value)
                result_message = f"App launch: {locator_value}"
                
            # Removed deprecated login handler
                
                
            elif action == 'install':
                success = self.device_manager.install_apk(locator_value)
                result_message = f"Install APK: {locator_value}"
            
            elif action == 'uninstall':
                success = self.device_manager.uninstall_apk(locator_value)  
                result_message = f"Uninstall APK: {locator_value}"
                
            elif action == 'force_stop':
                # Handle force_stop action for mobile app package
                if not locator_value:
                    raise ValueError("Package name required for force_stop action. Provide package name in locator_value field.")
                package_name = locator_value
                success = self.device_manager.force_stop_app(package_name)
                result_message = f"Force stopped: {package_name}"
                
            elif action == 'long_press':
                element, locator_info = self.device_manager.find_element_with_smart_fallback(locator_type, locator_value, description)
                if element:
                    # Use mobile command for long press
                    self.device_manager.driver.execute_script('mobile: longClickGesture', {
                        'elementId': element.id,
                        'duration': 2000
                    })
                    time.sleep(2)
                    success = True
                    result_message = f"Successfully long pressed element"
                    print(f"[TZ] Long pressed element: {description}")
                else:
                    success = False
                    result_message = f"Element not found for long press: {locator_value}"
                    print(f"[TZ] Element not found for long press: {locator_value}")
                    
            elif action == 'back' or action == 'back_button':
                # System back button
                self.device_manager.driver.back()
                time.sleep(1)
                success = True
                result_message = f"Navigated back"
                print(f"[TZ] Navigated back")
                
            elif action == 'scroll':
                # Basic scroll to find element
                from appium.webdriver.common.appiumby import AppiumBy
                for attempt in range(3):
                    element = self.element_finder.find_element_safe(locator_type, locator_value, timeout=2)
                    if element:
                        if 'click' in description.lower():
                            element.click()
                        success = True
                        result_message = f"Found and acted on element after scrolling"
                        print(f"[TZ] Found and acted on: {description}")
                        break
                    else:
                        # Scroll down
                        self.device_manager.driver.swipe(500, 1500, 500, 500, 1000)
                        time.sleep(1)
                if not success:
                    success = False
                    result_message = f"Could not find element after scrolling: {locator_value}"
                    print(f"[TZ] Could not find element after scrolling: {locator_value}")
                    
                        
                # Handle Banking Demo force close
                success = self.banking_demo_login_handler.force_close_app() if self.banking_demo_login_handler else False
                result_message = f"Banking Demo app force closed" if success else f"Banking Demo force close failed"
                print(f"[TZ] {result_message}")
                
            elif action == 'scroll_and_click':
                # Enhanced scroll with specific LeakCanary selectors from UI inspector
                from appium.webdriver.common.appiumby import AppiumBy
                found = False
                leakcanary_status_before = "unknown"
                leakcanary_status_after = "unknown"
                
                print(f"[TZ] ENHANCED TEST SETTINGS SCROLL: Searching for {description}")
                
                # Special handling for LeakCanary using inspector-found selectors
                if 'leakcanary' in description.lower():
                    print(f"[TZ] Step 1: Using INSPECTOR SELECTORS for LeakCanary...")
                    
                    # Method 1: Use content-desc selector (most reliable)
                    try:
                        leakcanary_element = self.device_manager.driver.find_element(
                            AppiumBy.XPATH,
                            "//*[contains(@content-desc, 'Enable LeakCanary')]"
                        )
                        
                        if leakcanary_element:
                            print("[TZ] Found Enable LeakCanary element via content-desc")
                            found = True
                            
                            # Get current state from content-desc
                            content_desc = leakcanary_element.get_attribute("content-desc")
                            print(f"[TZ] Current state: {content_desc}")
                            
                            # Check if it's currently ON or OFF
                            is_on = "On" in content_desc or "ON" in content_desc
                            leakcanary_status_before = "ON" if is_on else "OFF"
                            
                            if is_on:
                                print("[TZ] LeakCanary is currently ON - Toggling OFF for better performance")
                                leakcanary_element.click()
                                time.sleep(1)
                                
                                # Verify the change
                                updated_desc = leakcanary_element.get_attribute("content-desc")
                                leakcanary_status_after = "OFF" if "Off" in updated_desc else "ON"
                                print(f"[TZ] New state: {updated_desc}")
                                print(f"[TZ] LEAKCANARY STATUS LOG: Before={leakcanary_status_before}, After={leakcanary_status_after}")
                            else:
                                leakcanary_status_after = "OFF"
                                print("[TZ] LeakCanary is currently OFF - No toggle needed")
                                print(f"[TZ] LEAKCANARY STATUS LOG: Before={leakcanary_status_before}, After={leakcanary_status_after}")
                            
                            success = True
                            
                    except Exception as e:
                        print(f"[TZ] Method 1 (content-desc) failed: {e}")
                        
                        # Method 2: Try coordinate tap as fallback
                        try:
                            print("[TZ] Using coordinates as fallback (720, 2055)...")
                            self.device_manager.driver.tap([(720, 2055)])
                            time.sleep(1)
                            print("[TZ] Tapped at LeakCanary switch coordinates")
                            print("[TZ] LEAKCANARY STATUS LOG: Toggled via coordinates")
                            success = True
                            found = True
                            
                        except Exception as e2:
                            print(f"[TZ] Method 2 (coordinates) failed: {e2}")
                            print("[TZ] LeakCanary switch not found - may not be available on this screen")
                            success = True  # Don't fail test for optional element
                            found = False
                
                else:
                    # For non-LeakCanary elements, use standard approach
                    print(f"[TZ] Step 1: Checking current view without scrolling...")
                    try:
                        element = self.element_finder.find_element_safe(locator_type, locator_value, timeout=3)
                        if element:
                            print(f"[TZ] Found immediately without scrolling: {description}")
                            element.click()
                            found = True
                            success = True
                    except Exception as e:
                        print(f"[TZ] Initial check error: {e}")
                        found = False
                
                # Enhanced scrolling if not found (only for non-LeakCanary elements)
                if not found and 'leakcanary' not in description.lower():
                    print(f"[TZ] Step 2: ENHANCED SCROLLING - Element not visible, starting intelligent scroll...")
                    
                    # Get screen dimensions for better scrolling
                    screen_size = self.device_manager.driver.get_window_size()
                    screen_height = screen_size['height']
                    screen_width = screen_size['width']
                    
                    # Calculate scroll parameters - more aggressive but controlled
                    start_y = int(screen_height * 0.75)  # Start at 75% of screen
                    end_y = int(screen_height * 0.25)    # End at 25% of screen  
                    center_x = int(screen_width / 2)     # Center of screen
                    
                    print(f"[TZ] SCROLL PARAMETERS: Screen {screen_width}x{screen_height}, scroll from {start_y} to {end_y}")
                    
                    max_scrolls = 3  # Reduced scrolls to avoid getting stuck
                    for scroll_attempt in range(max_scrolls):
                        print(f"[TZ] Scroll attempt {scroll_attempt + 1}/{max_scrolls} - FASTER scroll...")
                        
                        # Faster, more meaningful scroll
                        self.device_manager.driver.swipe(center_x, start_y, center_x, end_y, 800)  # Faster scroll
                        time.sleep(0.5)  # Short wait between scrolls
                        
                        try:
                            # Check for element after scroll
                            element = self.element_finder.find_element_safe(locator_type, locator_value, timeout=2)
                            
                            if element:
                                print(f"[TZ] SUCCESS: Found after scroll attempt {scroll_attempt + 1}")
                                element.click()
                                found = True
                                success = True
                                break
                        except Exception as e:
                            print(f"[TZ] Scroll attempt {scroll_attempt + 1} check error: {e}")
                
                # Final status
                if not found and 'leakcanary' not in description.lower():
                    print(f"[TZ] RESULT: Element not found after scrolling - may not be present on current screen")
                    success = False  # Fail for non-optional elements
                elif not found and 'leakcanary' in description.lower():
                    print(f"[TZ] RESULT: LeakCanary element not found - this is optional for test execution")
                    success = True  # Don't fail on LeakCanary - it's optional
                else:
                    print(f"[TZ] RESULT: Successfully processed {description}")
                    if 'leakcanary' in description.lower():
                        print(f"[TZ] LEAKCANARY TRACKING: Status changed from '{leakcanary_status_before}' to '{leakcanary_status_after}'")
                        
                result_message = f"Scroll and click: {description}"
            
            else:
                print(f"[TZ] Unknown action: {action}")
                success = False
                result_message = f"Unknown action: {action}"
            
            # Take AFTER screenshot after successful meaningful actions
            if success and self._should_capture_screenshot(action, description):
                wait_time = self._get_screenshot_wait_time(action, description)
                time.sleep(wait_time)
                after_screenshot_path = self._take_screenshot(f"step_{step_num:02d}_after", f"After {action}: {description}")
                if after_screenshot_path:
                    print(f"[TZ] Captured AFTER screenshot showing result of: {description} (waited {wait_time}s for UI to settle)")
            
            # Update status
            status = "PASSED" if success else "FAILED"
            self.excel_manager.update_step_status(step_index, status, result_message)

            # Add to professional report
            step_duration = time.time() - step_start_time

            # Build locator display string with attempt info
            if locator_info:
                successful_locator = locator_info['locator']
                attempt_num = locator_info['attempt']
                total_attempts = locator_info['total_attempts']

                # Get column name from the mapping
                column_name = locator_column_map.get(successful_locator, 'Locator Value')

                # Build display string
                if total_attempts > 1:
                    locator_str = f"{locator_type}: {successful_locator} (Found on attempt {attempt_num}/{total_attempts} from {column_name})"
                else:
                    locator_str = f"{locator_type}: {successful_locator}"
            else:
                # Fallback if no locator_info available
                locator_str = f"{locator_type}: {locator_value}" if locator_type and locator_value else None

            self.professional_reporter.add_test_step(
                step_number=step_num,
                action=f"{action} - {description}",
                status=status,
                message=result_message if not success else "",
                screenshot_before=before_screenshot_path,
                screenshot_after=after_screenshot_path,
                locator=locator_str,
                test_data=test_data if 'test_data' in locals() else None,
                expected_result=expected_result if 'expected_result' in locals() else None,
                duration=round(step_duration, 2)
            )
            
            if success:
                self.passed_steps += 1
            else:
                self.failed_steps += 1
                if self.skip_on_fail:
                    print(f"[TZ] Step {step_num} FAILED - Continuing with next step (skip-on-fail enabled)")
                    return True  # Skip this failure and continue
                else:
                    print(f"[TZ] Step {step_num} FAILED - Stopping execution")
                    return False  # Stop execution on failure (default behavior)
            
            return True
            
        except Exception as e:
            print(f"[TZ] Step {step_num} ERROR: {e}")
            self.excel_manager.update_step_status(step_index, "FAILED", str(e))
            self.failed_steps += 1

            # Add failed step to report
            step_duration = time.time() - step_start_time

            # Build locator display string with attempt info
            if locator_info:
                successful_locator = locator_info['locator']
                attempt_num = locator_info['attempt']
                total_attempts = locator_info['total_attempts']

                # Get column name from the mapping
                column_name = locator_column_map.get(successful_locator, 'Locator Value')

                # Build display string
                if total_attempts > 1:
                    locator_str = f"{locator_type}: {successful_locator} (Found on attempt {attempt_num}/{total_attempts} from {column_name})"
                else:
                    locator_str = f"{locator_type}: {successful_locator}"
            else:
                # Fallback if no locator_info available
                locator_str = f"{locator_type}: {locator_value}" if locator_type and locator_value else None

            self.professional_reporter.add_test_step(
                step_number=step_num,
                action=f"{action} - {description}",
                status="FAILED",
                message=str(e),
                screenshot_before=before_screenshot_path,
                screenshot_after=after_screenshot_path,
                locator=locator_str,
                duration=round(step_duration, 2)
            )
            
            # Apply skip-on-fail logic for exceptions too
            if self.skip_on_fail:
                print(f"[TZ] Step {step_num} ERROR - Continuing with next step (skip-on-fail enabled)")
                return True  # Skip this error and continue
            else:
                print(f"[TZ] Step {step_num} ERROR - Stopping execution")
                return False  # Stop execution on error (default behavior)
    
    def run_test(self):
        """Execute the complete test suite"""
        try:
            # Setup phase
            if not self.setup():
                print("[TZ] Setup failed")
                return False
            
            test_steps = self.excel_manager.get_test_steps()
            self.total_steps = len(test_steps)
            
            # Set total planned steps in reporter for better reporting
            self.professional_reporter.set_total_planned_steps(self.total_steps)
            
            print(f"\n[TZ] Starting test execution with {self.total_steps} steps")
            print("=" * 50)
            
            # Execute test steps
            executed_steps = 0
            for index, step_data in enumerate(test_steps):
                executed_steps = index + 1
                if not self.execute_step(step_data, index):
                    print(f"[TZ] Test stopped at step {step_data.get('Step', index + 1)} - proceeding with cleanup and reporting")
                    break
            
            # Add unexecuted steps to the report
            if executed_steps < len(test_steps):
                for index in range(executed_steps, len(test_steps)):
                    step_data = test_steps[index]
                    step_num = step_data.get('S.No', index + 1)
                    description = str(step_data.get('Description', '')).strip()
                    action = str(step_data.get('Action', '')).strip()
                    locator_type = str(step_data.get('Locator Type', '')).strip()
                    locator_value = str(step_data.get('Locator Value', '')).strip()

                    # Add unexecuted step to report
                    locator_str = f"{locator_type}: {locator_value}" if locator_type and locator_value else None
                    self.professional_reporter.add_test_step(
                        step_number=step_num,
                        action=f"{action} - {description}",
                        status="SKIP",
                        message="Test execution stopped before reaching this step",
                        screenshot_before=None,
                        screenshot_after=None,
                        locator=locator_str,
                        duration=0
                    )

            # Return True only if no steps failed
            return self.failed_steps == 0
            
        except KeyboardInterrupt:
            print(f"\n[TZ] TEST INTERRUPTED BY USER")
            print("[TZ] Proceeding with cleanup and report generation...")
            # Add interruption info to report
            if hasattr(self, 'professional_reporter'):
                self.professional_reporter.add_test_step(
                    step_number=999,
                    action="Test Interrupted",
                    status="FAIL",
                    message="Test was manually interrupted (Ctrl+C)",
                    duration=0
                )
            self.failed_steps += 1
            return False
            
        except Exception as e:
            print(f"\n[TZ] UNEXPECTED ERROR DURING TEST EXECUTION: {e}")
            print("[TZ] Proceeding with cleanup and report generation...")
            # Add exception info to report
            if hasattr(self, 'professional_reporter'):
                self.professional_reporter.add_test_step(
                    step_number=998,
                    action="Unexpected Error",
                    status="FAIL",
                    message=str(e),
                    duration=0
                )
            self.failed_steps += 1
            return False
            
        finally:
            # Always perform cleanup
            try:
                self.color_logger.header("TEST EXECUTION SUMMARY")
                self.color_logger.print_summary(self.passed_steps, self.failed_steps, total=self.total_steps)

                # Only show success message if tests actually ran
                if self.total_steps == 0:
                    print("\n[TZ] NO TESTS EXECUTED - Setup or validation failed")
                elif self.failed_steps == 0:
                    print("\n[TZ] ALL TESTS PASSED!")
                else:
                    print(f"\n[TZ] {self.failed_steps} TEST(S) FAILED")
                
                # Show file paths for easy access
                print(f"\n[TZ] RESULTS AVAILABLE:")
                print("=" * 50)
                print(f"[TZ] Excel Results: {os.path.abspath(self.excel_file)}")
                print(f"[TZ]   → Open to view Test_Summary sheet with detailed results")
                
                self.teardown()
                
            except Exception as cleanup_error:
                print(f"[TZ] Error during cleanup: {cleanup_error}")

    def _detect_app_package(self):
        """Detect and set the current app package from APK/IPA"""
        try:
            from src.utils.package_detector import PackageDetector
            detector = PackageDetector()
            import glob
            import os

            # Determine platform and expected app folder
            platform = self.config.get('platform', 'android').lower()

            if platform == 'android':
                app_folder = 'apps/android'
                app_extension = '*.apk'
                app_type = 'APK'
            elif platform == 'ios':
                app_folder = 'apps/ios'
                app_extension = '*.ipa'
                app_type = 'IPA'
            else:
                self.color_logger.error(f"Unsupported platform: {platform}")
                return False

            # Search for app file in the correct platform folder
            search_pattern = os.path.join(app_folder, app_extension)
            app_files = glob.glob(search_pattern)

            if not app_files:
                # No app file found - fail fast with clear error message
                self.color_logger.error(f"No {app_type} file found in {app_folder}/ folder")
                self.color_logger.error("")
                self.color_logger.error(f"Please place your {app_type} file in the {app_folder}/ folder before running tests.")
                self.color_logger.error("")
                self.color_logger.error("Instructions:")
                if platform == 'android':
                    self.color_logger.error("  1. Copy your APK file:")
                    self.color_logger.error(f"     cp /path/to/your-app.apk {app_folder}/")
                    self.color_logger.error("")
                    self.color_logger.error("  2. Ensure only ONE APK file exists in the folder")
                    self.color_logger.error("")
                    self.color_logger.error("  3. Run tests again:")
                    self.color_logger.error("     python testzen.py run --file tests/android/your-test.xlsx --platform android")
                else:  # iOS
                    self.color_logger.error("  1. Copy your IPA file (built for simulator):")
                    self.color_logger.error(f"     cp /path/to/your-app.ipa {app_folder}/")
                    self.color_logger.error("")
                    self.color_logger.error("  2. Ensure only ONE IPA file exists in the folder")
                    self.color_logger.error("")
                    self.color_logger.error("  3. Run tests again:")
                    self.color_logger.error("     python testzen.py run --file tests/ios/your-test.xlsx --platform ios")

                return False

            if len(app_files) > 1:
                # Multiple app files found - warn user
                self.color_logger.warning(f"Multiple {app_type} files found in {app_folder}/ folder:")
                for app_file in app_files:
                    self.color_logger.warning(f"  - {app_file}")
                self.color_logger.warning(f"Using: {app_files[0]}")
                self.color_logger.warning("")
                self.color_logger.warning(f"Recommendation: Keep only ONE {app_type} file in {app_folder}/ folder")

            # Use the first app file found
            app_path = app_files[0]
            self.color_logger.success(f"Found {app_type}: {os.path.basename(app_path)}")

            # Extract package name from app file
            if platform == 'android':
                package = detector.get_package_from_apk(app_path)
                if package:
                    self.current_app_package = package
                    self.color_logger.success(f"Detected app package: {package}")
                    return True
                else:
                    self.color_logger.error(f"Failed to extract package name from APK: {app_path}")
                    return False
            else:  # iOS
                # For iOS, we'll need to extract the bundle ID from IPA
                # This requires additional implementation in package_detector.py
                # For now, we'll set a placeholder and return True
                self.color_logger.warning("iOS IPA detection not fully implemented yet")
                self.current_app_package = "com.example.app"  # Placeholder
                return True

        except Exception as e:
            self.color_logger.error(f"Package detection failed: {e}")
            import traceback
            self.color_logger.error(traceback.format_exc())
            return False

    def _switch_context_if_needed(self, step_number):
        """Switch context based on app type and step requirements"""
        try:
            # For WebView-based apps, switch to WebView context for input/click operations
            if step_number == 1 and self.current_app_package:  # First step, likely needs WebView
                if self.device_manager.switch_to_webview(self.current_app_package):
                    self.color_logger.info("Switched to WebView context for hybrid app interaction")
                    return True
            return False
        except Exception as e:
            self.color_logger.warning(f"Context switching failed: {e}")
            return False

def main():
    """Main execution function"""
    excel_file = "tests/sample_mobile_test.xlsx"
    
    if len(sys.argv) > 1:
        excel_file = sys.argv[1]
    
    if not os.path.exists(excel_file):
        print(f"[TZ] Excel file not found: {excel_file}")
        return False
    
    automation = TestZenAutomation(excel_file)
    return automation.run_test()

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)