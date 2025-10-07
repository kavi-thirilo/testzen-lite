"""
Element Finder - Handles element location and interaction with smart wait strategies
"""

from appium.webdriver.common.appiumby import AppiumBy
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException, NoSuchElementException
import logging
import time

logger = logging.getLogger(__name__)


class ElementFinder:
    """Handles element finding with various strategies"""
    
    def __init__(self, driver, wait_timeout=20):
        self.driver = driver
        self.wait_timeout = wait_timeout
        self.wait = WebDriverWait(driver, wait_timeout)
        
    def find_element(self, locator_type, locator_value, timeout=None):
        """Find element with specified locator strategy"""
        timeout = timeout or self.wait_timeout
        wait = WebDriverWait(self.driver, timeout)
        
        try:
            by = self._get_by_type(locator_type)
            
            # First check if element exists
            element = wait.until(
                EC.presence_of_element_located((by, locator_value))
            )
            
            # Try to scroll to element if needed
            if not self._is_element_visible(element):
                self._scroll_to_element(element)
            
            # Wait for element to be clickable
            element = wait.until(
                EC.element_to_be_clickable((by, locator_value))
            )
            
            logger.info(f"Found element: {locator_type}={locator_value}")
            return element
            
        except TimeoutException:
            logger.error(f"Element not found: {locator_type}={locator_value}")
            # Try scrolling and searching
            if self._scroll_and_find(locator_type, locator_value):
                return self.find_element(locator_type, locator_value, timeout=5)
            raise
        except Exception as e:
            logger.error(f"Error finding element: {str(e)}")
            raise
    
    def find_elements(self, locator_type, locator_value, timeout=None):
        """Find multiple elements with specified locator strategy"""
        timeout = timeout or self.wait_timeout
        wait = WebDriverWait(self.driver, timeout)
        
        try:
            by = self._get_by_type(locator_type)
            elements = wait.until(
                EC.presence_of_all_elements_located((by, locator_value))
            )
            logger.info(f"Found {len(elements)} elements: {locator_type}={locator_value}")
            return elements
        except TimeoutException:
            logger.warning(f"No elements found: {locator_type}={locator_value}")
            return []
        except Exception as e:
            logger.error(f"Error finding elements: {str(e)}")
            return []
    
    def wait_for_element(self, locator_type, locator_value, timeout=None, condition='visible'):
        """Wait for element with specific condition"""
        timeout = timeout or self.wait_timeout
        wait = WebDriverWait(self.driver, timeout)
        by = self._get_by_type(locator_type)
        
        try:
            if condition == 'visible':
                element = wait.until(
                    EC.visibility_of_element_located((by, locator_value))
                )
            elif condition == 'clickable':
                element = wait.until(
                    EC.element_to_be_clickable((by, locator_value))
                )
            elif condition == 'present':
                element = wait.until(
                    EC.presence_of_element_located((by, locator_value))
                )
            else:
                raise ValueError(f"Unknown condition: {condition}")
            
            logger.info(f"Element is {condition}: {locator_type}={locator_value}")
            return element
        except TimeoutException:
            logger.error(f"Timeout waiting for element to be {condition}: {locator_type}={locator_value}")
            return None
        except Exception as e:
            logger.error(f"Error waiting for element: {str(e)}")
            return None
    
    def is_element_present(self, locator_type, locator_value, timeout=3):
        """Check if element is present without throwing exception"""
        try:
            self.wait_for_element(locator_type, locator_value, timeout=timeout, condition='present')
            return True
        except:
            return False
    
    def _get_by_type(self, locator_type):
        """Convert string locator type to By object"""
        locator_map = {
            'id': AppiumBy.ID,
            'accessibility_id': AppiumBy.ACCESSIBILITY_ID,
            'xpath': AppiumBy.XPATH,
            'class': AppiumBy.CLASS_NAME,
            'name': AppiumBy.NAME,
            'android_uiautomator': AppiumBy.ANDROID_UIAUTOMATOR,
            'ios_predicate': AppiumBy.IOS_PREDICATE_STRING,
            'ios_class_chain': AppiumBy.IOS_CLASS_CHAIN,
            'image': AppiumBy.IMAGE
        }
        
        locator_type = locator_type.lower()
        if locator_type not in locator_map:
            raise ValueError(f"Unknown locator type: {locator_type}")
        
        return locator_map[locator_type]
    
    def _is_element_visible(self, element):
        """Check if element is visible in viewport"""
        try:
            return element.is_displayed()
        except:
            return False
    
    def _scroll_to_element(self, element):
        """Scroll to make element visible"""
        try:
            if self.driver.capabilities.get('platformName', '').lower() == 'android':
                self.driver.execute_script('mobile: scrollToElement', {
                    'element': element.id,
                    'strategy': 'accessibility id',
                    'maxSwipes': 10
                })
            else:
                # iOS scroll
                self.driver.execute_script('mobile: scroll', {
                    'element': element.id,
                    'toVisible': True
                })
            logger.info("Scrolled to element")
            return True
        except Exception as e:
            logger.warning(f"Could not scroll to element: {str(e)}")
            return False
    
    def _scroll_and_find(self, locator_type, locator_value, max_scrolls=5):
        """Scroll and search for element"""
        platform = self.driver.capabilities.get('platformName', '').lower()
        
        for i in range(max_scrolls):
            if self.is_element_present(locator_type, locator_value, timeout=1):
                return True
            
            # Perform scroll
            if platform == 'android':
                self._scroll_down_android()
            else:
                self._scroll_down_ios()
            
            time.sleep(0.5)
        
        return False
    
    def _scroll_down_android(self):
        """Scroll down on Android"""
        try:
            size = self.driver.get_window_size()
            start_x = size['width'] // 2
            start_y = size['height'] * 0.8
            end_y = size['height'] * 0.2
            
            self.driver.swipe(start_x, start_y, start_x, end_y, duration=800)
        except Exception as e:
            logger.warning(f"Error scrolling: {str(e)}")
    
    def _scroll_down_ios(self):
        """Scroll down on iOS"""
        try:
            self.driver.execute_script('mobile: scroll', {'direction': 'down'})
        except Exception as e:
            logger.warning(f"Error scrolling: {str(e)}")
    
    def scroll_up(self):
        """Scroll up"""
        platform = self.driver.capabilities.get('platformName', '').lower()
        
        if platform == 'android':
            size = self.driver.get_window_size()
            start_x = size['width'] // 2
            start_y = size['height'] * 0.2
            end_y = size['height'] * 0.8
            self.driver.swipe(start_x, start_y, start_x, end_y, duration=800)
        else:
            self.driver.execute_script('mobile: scroll', {'direction': 'up'})