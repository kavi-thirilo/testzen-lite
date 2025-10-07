#!/usr/bin/env python3
"""
Mobile Utility Functions
Helper functions for mobile automation tasks
"""

import time
import logging
from typing import Optional, Union, List, Dict, Any
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By
from appium.webdriver.common.appiumby import AppiumBy
from selenium.common.exceptions import TimeoutException, NoSuchElementException
from .smart_element_finder import SmartElementFinder


class MobileUtils:
    """Utility class for common mobile automation operations"""

    def __init__(self, driver, logger: logging.Logger):
        self.driver = driver
        self.logger = logger
        self.default_timeout = 10
        self.smart_finder = SmartElementFinder(driver, logger)

    def find_element_safe(self, locator: str, timeout: Optional[int] = None) -> Optional[object]:
        """
        Safely find element with timeout

        Args:
            locator (str): XPath locator string
            timeout (int, optional): Timeout in seconds

        Returns:
            WebElement or None if not found
        """
        try:
            wait_time = timeout if timeout is not None else self.default_timeout
            wait = WebDriverWait(self.driver, wait_time)
            element = wait.until(EC.presence_of_element_located((AppiumBy.XPATH, locator)))
            return element
        except TimeoutException:
            return None
        except Exception as e:
            self.logger.warning(f"Element search failed for '{locator}': {str(e)}")
            return None

    def find_elements_safe(self, locator: str, timeout: Optional[int] = None) -> list:
        """
        Safely find multiple elements with timeout

        Args:
            locator (str): XPath locator string
            timeout (int, optional): Timeout in seconds

        Returns:
            List of WebElements (empty list if none found)
        """
        try:
            wait_time = timeout if timeout is not None else self.default_timeout
            wait = WebDriverWait(self.driver, wait_time)
            elements = wait.until(EC.presence_of_all_elements_located((AppiumBy.XPATH, locator)))
            return elements
        except TimeoutException:
            return []
        except Exception as e:
            self.logger.warning(f"Elements search failed for '{locator}': {str(e)}")
            return []

    def click_element(self, locator: str, timeout: Optional[int] = None, description: str = "") -> bool:
        """
        Click element safely with smart recovery

        Args:
            locator (str): XPath locator string
            timeout (int, optional): Timeout in seconds
            description (str): Human-readable description for smart recovery

        Returns:
            bool: True if clicked successfully, False otherwise
        """
        try:
            # First try simple approach
            element = self.find_element_safe(locator, timeout or 3)
            if element:
                try:
                    element.click()
                    time.sleep(1)  # Brief pause after click
                    return True
                except Exception as click_error:
                    self.logger.warning(f"Direct click failed: {str(click_error)}, attempting smart recovery...")

            # If direct approach fails, use smart recovery
            wait_time = timeout if timeout is not None else self.default_timeout
            smart_element = self.smart_finder.find_actionable_element(
                locator, 'click', description, wait_time
            )

            if smart_element:
                smart_element.click()
                time.sleep(1)  # Brief pause after click
                return True

            return False

        except Exception as e:
            self.logger.error(f"Smart click failed for '{locator}': {str(e)}")
            return False

    def long_press_element(self, locator: str, duration: int = 2000, timeout: Optional[int] = None) -> bool:
        """
        Long press element

        Args:
            locator (str): XPath locator string
            duration (int): Duration in milliseconds
            timeout (int, optional): Timeout in seconds

        Returns:
            bool: True if long press successful, False otherwise
        """
        try:
            element = self.find_element_safe(locator, timeout)
            if element:
                self.driver.execute_script('mobile: longClickGesture', {
                    'elementId': element.id,
                    'duration': duration
                })
                time.sleep(2)  # Brief pause after long press
                return True
            return False
        except Exception as e:
            self.logger.error(f"Long press failed for '{locator}': {str(e)}")
            return False

    def enter_text(self, locator: str, text: str, timeout: Optional[int] = None, clear_first: bool = True, description: str = "") -> bool:
        """
        Enter text into element with smart recovery

        Args:
            locator (str): XPath locator string
            text (str): Text to enter
            timeout (int, optional): Timeout in seconds
            clear_first (bool): Clear field before entering text
            description (str): Human-readable description for smart recovery

        Returns:
            bool: True if text entered successfully, False otherwise
        """
        try:
            # First try simple approach
            element = self.find_element_safe(locator, timeout or 3)
            if element:
                try:
                    if clear_first:
                        element.clear()
                    element.send_keys(text)
                    time.sleep(1)  # Brief pause after text entry
                    return True
                except Exception as input_error:
                    self.logger.warning(f"Direct text input failed: {str(input_error)}, attempting smart recovery...")

            # If direct approach fails, use smart recovery
            wait_time = timeout if timeout is not None else self.default_timeout
            smart_element = self.smart_finder.find_actionable_element(
                locator, 'input', description, wait_time
            )

            if smart_element:
                if clear_first:
                    smart_element.clear()
                smart_element.send_keys(text)
                time.sleep(1)  # Brief pause after text entry
                return True

            return False

        except Exception as e:
            self.logger.error(f"Smart text entry failed for '{locator}': {str(e)}")
            return False

    def get_text(self, locator: str, timeout: Optional[int] = None) -> Optional[str]:
        """
        Get text from element

        Args:
            locator (str): XPath locator string
            timeout (int, optional): Timeout in seconds

        Returns:
            str or None: Element text or None if not found
        """
        try:
            element = self.find_element_safe(locator, timeout)
            if element:
                return element.text
            return None
        except Exception as e:
            self.logger.error(f"Get text failed for '{locator}': {str(e)}")
            return None

    def get_attribute(self, locator: str, attribute: str, timeout: Optional[int] = None) -> Optional[str]:
        """
        Get attribute from element

        Args:
            locator (str): XPath locator string
            attribute (str): Attribute name
            timeout (int, optional): Timeout in seconds

        Returns:
            str or None: Attribute value or None if not found
        """
        try:
            element = self.find_element_safe(locator, timeout)
            if element:
                return element.get_attribute(attribute)
            return None
        except Exception as e:
            self.logger.error(f"Get attribute failed for '{locator}': {str(e)}")
            return None

    def scroll_and_click(self, locator: str, max_scrolls: int = 5, timeout: Optional[int] = None) -> bool:
        """
        Scroll to find and click element

        Args:
            locator (str): XPath locator string
            max_scrolls (int): Maximum number of scroll attempts
            timeout (int, optional): Timeout in seconds per attempt

        Returns:
            bool: True if element found and clicked, False otherwise
        """
        try:
            # First try to find element without scrolling
            if self.click_element(locator, timeout or 3):
                return True

            # Scroll and search
            for i in range(max_scrolls):
                self.logger.info(f"Scroll attempt {i+1} of {max_scrolls}")

                # Perform scroll down
                self.driver.execute_script('mobile: scrollGesture', {
                    'left': 100,
                    'top': 100,
                    'width': 200,
                    'height': 400,
                    'direction': 'down',
                    'percent': 3.0
                })

                time.sleep(1)

                # Try to find and click element
                if self.click_element(locator, timeout or 3):
                    self.logger.info(f"Element found and clicked after scroll attempt {i+1}")
                    return True

            self.logger.warning(f"Element not found after {max_scrolls} scroll attempts")
            return False

        except Exception as e:
            self.logger.error(f"Scroll and click failed for '{locator}': {str(e)}")
            return False

    def wait_for_element_visible(self, locator: str, timeout: Optional[int] = None) -> bool:
        """
        Wait for element to be visible

        Args:
            locator (str): XPath locator string
            timeout (int, optional): Timeout in seconds

        Returns:
            bool: True if element becomes visible, False otherwise
        """
        try:
            wait_time = timeout if timeout is not None else self.default_timeout
            wait = WebDriverWait(self.driver, wait_time)
            wait.until(EC.visibility_of_element_located((AppiumBy.XPATH, locator)))
            return True
        except TimeoutException:
            return False
        except Exception as e:
            self.logger.error(f"Wait for visibility failed for '{locator}': {str(e)}")
            return False

    def wait_for_element_clickable(self, locator: str, timeout: Optional[int] = None) -> bool:
        """
        Wait for element to be clickable

        Args:
            locator (str): XPath locator string
            timeout (int, optional): Timeout in seconds

        Returns:
            bool: True if element becomes clickable, False otherwise
        """
        try:
            wait_time = timeout if timeout is not None else self.default_timeout
            wait = WebDriverWait(self.driver, wait_time)
            wait.until(EC.element_to_be_clickable((AppiumBy.XPATH, locator)))
            return True
        except TimeoutException:
            return False
        except Exception as e:
            self.logger.error(f"Wait for clickable failed for '{locator}': {str(e)}")
            return False

    def is_element_present(self, locator: str) -> bool:
        """
        Check if element is present (without waiting)

        Args:
            locator (str): XPath locator string

        Returns:
            bool: True if element present, False otherwise
        """
        try:
            self.driver.find_element(AppiumBy.XPATH, locator)
            return True
        except NoSuchElementException:
            return False
        except Exception as e:
            self.logger.warning(f"Element presence check failed for '{locator}': {str(e)}")
            return False

    def swipe_screen(self, direction: str = 'up', duration: int = 1000) -> bool:
        """
        Swipe screen in specified direction

        Args:
            direction (str): Direction to swipe ('up', 'down', 'left', 'right')
            duration (int): Duration in milliseconds

        Returns:
            bool: True if swipe successful, False otherwise
        """
        try:
            size = self.driver.get_window_size()
            width = size['width']
            height = size['height']

            start_x = width // 2
            start_y = height // 2
            end_x = start_x
            end_y = start_y

            if direction == 'up':
                start_y = height * 0.8
                end_y = height * 0.2
            elif direction == 'down':
                start_y = height * 0.2
                end_y = height * 0.8
            elif direction == 'left':
                start_x = width * 0.8
                end_x = width * 0.2
            elif direction == 'right':
                start_x = width * 0.2
                end_x = width * 0.8

            self.driver.swipe(int(start_x), int(start_y), int(end_x), int(end_y), duration)
            time.sleep(1)
            return True

        except Exception as e:
            self.logger.error(f"Swipe failed: {str(e)}")
            return False

    def get_recovery_hints(self) -> List[Dict[str, Any]]:
        """Get recovery hints from smart finder for reporting"""
        return self.smart_finder.get_recovery_hints()

    def clear_recovery_hints(self):
        """Clear recovery hints"""
        self.smart_finder.clear_recovery_hints()

    def get_recovery_summary(self) -> str:
        """Get formatted summary of smart recovery actions"""
        return self.smart_finder.get_recovery_summary()

    def uninstall_app(self, package_name: str) -> bool:
        """
        Uninstall app using ADB command

        Args:
            package_name (str): Package name of the app to uninstall

        Returns:
            bool: True if successful, False otherwise
        """
        try:
            self.logger.info(f"[TZ] Uninstalling app: {package_name}")
            self.driver.remove_app(package_name)
            self.logger.info(f"[TZ] Successfully uninstalled {package_name}")
            return True
        except Exception as e:
            self.logger.warning(f"[TZ] App uninstall failed (may not be installed): {str(e)}")
            # This is not necessarily an error if the app wasn't installed
            return True

    def install_app(self, apk_path: str) -> bool:
        """
        Install app from APK file using direct ADB command for test APKs

        Args:
            apk_path (str): Path to APK file

        Returns:
            bool: True if successful, False otherwise
        """
        try:
            self.logger.info(f"[TZ] Installing app from: {apk_path}")

            # Use direct ADB command with -t flag for test APKs
            import subprocess
            import os

            # Get ADB path
            android_home = os.environ.get('ANDROID_HOME', os.path.expanduser('~/Library/Android/sdk'))
            adb_path = os.path.join(android_home, 'platform-tools', 'adb')

            # Install with test flag
            cmd = [adb_path, 'install', '-r', '-t', apk_path]
            self.logger.info(f"Running ADB command: {' '.join(cmd)}")

            result = subprocess.run(cmd, capture_output=True, text=True)

            if result.returncode == 0:
                self.logger.info("[TZ] App installation completed via ADB")
                return True
            else:
                self.logger.error(f"[TZ] ADB install failed: {result.stderr}")
                # Fallback to Appium method
                self.driver.install_app(apk_path, replace=True)
                self.logger.info("[TZ] App installation completed via Appium fallback")
                return True

        except Exception as e:
            self.logger.error(f"[TZ] App installation failed: {str(e)}")
            return False

    def launch_app(self, package_name: str) -> bool:
        """
        Launch app by package name

        Args:
            package_name (str): Package name of the app to launch

        Returns:
            bool: True if successful, False otherwise
        """
        try:
            self.logger.info(f"[TZ] Launching app: {package_name}")
            self.driver.activate_app(package_name)
            self.logger.info("[TZ] App launched successfully")
            return True
        except Exception as e:
            self.logger.error(f"[TZ] App launch failed: {str(e)}")
            return False