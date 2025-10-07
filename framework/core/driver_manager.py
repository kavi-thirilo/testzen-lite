"""
Driver Manager - Handles Appium driver initialization and management
"""

from appium import webdriver
from appium.webdriver.common.appiumby import AppiumBy
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException, NoSuchElementException
import logging
import time

logger = logging.getLogger(__name__)


class DriverManager:
    """Manages Appium driver lifecycle and operations"""
    
    def __init__(self, config):
        self.config = config
        self.driver = None
        self.wait = None
        self.platform = config.get('platform', 'android').lower()
        
    def initialize_driver(self):
        """Initialize Appium driver with desired capabilities"""
        try:
            capabilities = self._build_capabilities()
            appium_server = self.config.get('appium_server', 'http://localhost:4723/wd/hub')
            
            logger.info(f"Initializing {self.platform} driver...")
            self.driver = webdriver.Remote(appium_server, capabilities)
            
            # Set implicit wait
            implicit_wait = self.config.get('implicit_wait', 10)
            self.driver.implicitly_wait(implicit_wait)
            
            # Initialize explicit wait
            explicit_wait = self.config.get('explicit_wait', 20)
            self.wait = WebDriverWait(self.driver, explicit_wait)
            
            logger.info("Driver initialized successfully")
            return self.driver
            
        except Exception as e:
            logger.error(f"Failed to initialize driver: {str(e)}")
            raise
    
    def _build_capabilities(self):
        """Build desired capabilities based on platform"""
        caps = {}
        
        if self.platform == 'android':
            caps = {
                'platformName': 'Android',
                'automationName': self.config.get('automation_name', 'UiAutomator2'),
                'deviceName': self.config.get('device_name', 'Android Device'),
                'app': self.config.get('app_path', ''),
                'appPackage': self.config.get('app_package', ''),
                'appActivity': self.config.get('app_activity', ''),
                'noReset': self.config.get('no_reset', True),
                'fullReset': self.config.get('full_reset', False),
                'autoGrantPermissions': self.config.get('auto_grant_permissions', True)
            }
        elif self.platform == 'ios':
            caps = {
                'platformName': 'iOS',
                'automationName': self.config.get('automation_name', 'XCUITest'),
                'deviceName': self.config.get('device_name', 'iPhone'),
                'platformVersion': self.config.get('platform_version', ''),
                'app': self.config.get('app_path', ''),
                'bundleId': self.config.get('bundle_id', ''),
                'noReset': self.config.get('no_reset', True),
                'fullReset': self.config.get('full_reset', False)
            }
        
        # Add any additional capabilities
        additional_caps = self.config.get('additional_capabilities', {})
        caps.update(additional_caps)
        
        # Remove empty values
        caps = {k: v for k, v in caps.items() if v != ''}
        
        return caps
    
    def quit_driver(self):
        """Quit the driver and cleanup"""
        if self.driver:
            try:
                self.driver.quit()
                logger.info("Driver quit successfully")
            except Exception as e:
                logger.error(f"Error quitting driver: {str(e)}")
            finally:
                self.driver = None
                self.wait = None
    
    def switch_to_webview(self):
        """Switch context to webview"""
        try:
            contexts = self.driver.contexts
            for context in contexts:
                if 'WEBVIEW' in context:
                    self.driver.switch_to.context(context)
                    logger.info(f"Switched to webview context: {context}")
                    return True
            logger.warning("No webview context found")
            return False
        except Exception as e:
            logger.error(f"Error switching to webview: {str(e)}")
            return False
    
    def switch_to_native(self):
        """Switch context to native app"""
        try:
            self.driver.switch_to.context('NATIVE_APP')
            logger.info("Switched to native context")
            return True
        except Exception as e:
            logger.error(f"Error switching to native: {str(e)}")
            return False
    
    def take_screenshot(self, filename):
        """Take a screenshot and save it"""
        try:
            self.driver.save_screenshot(filename)
            logger.info(f"Screenshot saved: {filename}")
            return True
        except Exception as e:
            logger.error(f"Error taking screenshot: {str(e)}")
            return False