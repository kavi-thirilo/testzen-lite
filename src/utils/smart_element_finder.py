#!/usr/bin/env python3
"""
Smart Element Finder - Enhanced UI Hierarchy Traversal
Provides intelligent element discovery with parent/child traversal and actionability recovery
"""

import time
import logging
from typing import Optional, List, Dict, Any, Tuple
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By
from appium.webdriver.common.appiumby import AppiumBy
from selenium.common.exceptions import (
    TimeoutException,
    NoSuchElementException,
    ElementNotInteractableException,
    ElementClickInterceptedException,
    StaleElementReferenceException
)


class SmartElementFinder:
    """
    Advanced element finder with smart UI hierarchy traversal and recovery mechanisms
    """

    def __init__(self, driver, logger: logging.Logger):
        self.driver = driver
        self.logger = logger
        self.recovery_hints = []  # Track recovery actions for reporting

        # Clickable container patterns (ordered by preference)
        self.clickable_containers = [
            "android.widget.Button",
            "android.widget.ImageButton",
            "android.view.ViewGroup[@clickable='true']",
            "android.widget.LinearLayout[@clickable='true']",
            "android.widget.RelativeLayout[@clickable='true']",
            "android.widget.FrameLayout[@clickable='true']",
            "android.view.View[@clickable='true']"
        ]

        # Action-specific container preferences
        self.action_containers = {
            'click': ['Button', 'ImageButton', 'ViewGroup', 'LinearLayout', 'RelativeLayout'],
            'input': ['EditText', 'AutoCompleteTextView', 'MultiAutoCompleteTextView'],
            'scroll': ['ScrollView', 'RecyclerView', 'ListView', 'ViewPager']
        }

    def find_actionable_element(self, locator: str, action: str = 'click',
                                description: str = "", timeout: int = 10) -> Optional[object]:
        """
        Find an actionable element with smart recovery mechanisms

        Args:
            locator (str): XPath locator string
            action (str): Action to be performed ('click', 'input', 'scroll', etc.)
            description (str): Human-readable description for logging
            timeout (int): Timeout in seconds

        Returns:
            WebElement or None if not found after all recovery attempts
        """
        self.logger.info(f"[TZ] Smart Element Finder: Looking for actionable element for '{action}' action")
        self.logger.info(f"[TZ] Locator: {locator}")
        self.logger.info(f"[TZ] Description: {description}")

        try:
            # Strategy 1: Direct element finding (most common success case)
            element = self._find_direct_element(locator, timeout)
            if element and self._is_actionable(element, action):
                self.logger.info("[TZ] Success: Direct element found and actionable")
                return element

            # Strategy 2: Find element and traverse to actionable parent
            element = self._find_with_parent_traversal(locator, action, timeout)
            if element:
                self._add_recovery_hint('parent_traversal', 
                                      f"Found actionable parent for: {description}")
                return element

            # Strategy 3: Find similar elements by text/content-desc
            element = self._find_similar_by_text(locator, action, description, timeout)
            if element:
                self._add_recovery_hint('text_similarity',
                                      f"Found similar element by text: {description}")
                return element

            # Strategy 4: Find by partial attributes
            element = self._find_by_partial_attributes(locator, action, timeout)
            if element:
                self._add_recovery_hint('partial_attributes',
                                      f"Found element by partial attributes: {description}")
                return element

            # Strategy 5: Area-based search (find nearby actionable elements)
            element = self._find_in_vicinity(locator, action, timeout)
            if element:
                self._add_recovery_hint('vicinity_search',
                                      f"Found nearby actionable element: {description}")
                return element

            self.logger.warning(f"[TZ] All smart recovery strategies exhausted for: {description}")
            return None

        except Exception as e:
            self.logger.error(f"[TZ] Smart element finding failed: {str(e)}")
            return None

    def _find_direct_element(self, locator: str, timeout: int) -> Optional[object]:
        """Find element directly using provided locator"""
        try:
            wait = WebDriverWait(self.driver, timeout)
            element = wait.until(EC.presence_of_element_located((AppiumBy.XPATH, locator)))
            return element
        except TimeoutException:
            self.logger.debug("[TZ] Direct element search timed out")
            return None
        except Exception as e:
            self.logger.debug(f"[TZ] Direct element search failed: {str(e)}")
            return None

    def _is_actionable(self, element, action: str) -> bool:
        """Check if element is actionable for the specified action"""
        try:
            if not element:
                return False

            # Get element properties
            is_enabled = element.is_enabled()
            is_displayed = element.is_displayed()
            tag_name = element.tag_name.lower() if element.tag_name else ""
            clickable = element.get_attribute('clickable') == 'true'
            focusable = element.get_attribute('focusable') == 'true'

            # General actionability checks
            if not (is_enabled and is_displayed):
                return False

            # Action-specific checks
            if action == 'click':
                # For clicking, prefer explicitly clickable elements
                if clickable:
                    return True
                # Also accept common clickable tags
                clickable_tags = ['button', 'imagebutton', 'textview']
                return any(tag in tag_name for tag in clickable_tags)

            elif action == 'input':
                # For input, need focusable text fields
                if focusable and 'edittext' in tag_name:
                    return True
                return 'edittext' in tag_name or 'autocomplete' in tag_name

            elif action == 'scroll':
                # For scrolling, look for scroll containers
                scroll_tags = ['scrollview', 'recyclerview', 'listview', 'viewpager']
                return any(tag in tag_name for tag in scroll_tags)

            # Default case - if enabled and displayed, consider actionable
            return True

        except Exception as e:
            self.logger.debug(f"[TZ] Actionability check failed: {str(e)}")
            return False

    def _find_with_parent_traversal(self, locator: str, action: str, timeout: int) -> Optional[object]:
        """Find element and traverse up to find actionable parent"""
        try:
            # First find any element matching the locator
            element = self._find_direct_element(locator, timeout // 2)
            if not element:
                return None

            # Traverse up the hierarchy to find actionable parent
            current = element
            max_levels = 5  # Limit traversal depth

            for level in range(max_levels):
                if self._is_actionable(current, action):
                    self.logger.info(f"[TZ] Found actionable parent at level {level}")
                    return current

                try:
                    # Get parent element
                    parent_xpath = f"({locator})/parent::*[{level + 1}]"
                    current = self.driver.find_element(AppiumBy.XPATH, parent_xpath)
                except:
                    break

            return None

        except Exception as e:
            self.logger.debug(f"[TZ] Parent traversal failed: {str(e)}")
            return None

    def _find_similar_by_text(self, locator: str, action: str, description: str, timeout: int) -> Optional[object]:
        """Find similar elements by text content or content-desc"""
        try:
            if not description:
                return None

            # Extract keywords from description
            keywords = self._extract_keywords(description)
            if not keywords:
                return None

            # Build XPath for text-based search
            text_conditions = []
            for keyword in keywords:
                text_conditions.extend([
                    f"contains(@text, '{keyword}')",
                    f"contains(@content-desc, '{keyword}')",
                    f"contains(text(), '{keyword}')"
                ])

            # Try different element types
            element_types = self.action_containers.get(action, ['*'])
            
            for element_type in element_types:
                for condition in text_conditions:
                    xpath = f"//android.widget.{element_type}[{condition}]"
                    element = self._find_direct_element(xpath, timeout // 4)
                    if element and self._is_actionable(element, action):
                        self.logger.info(f"[TZ] Found by text similarity: {xpath}")
                        return element

            return None

        except Exception as e:
            self.logger.debug(f"[TZ] Text similarity search failed: {str(e)}")
            return None

    def _find_by_partial_attributes(self, locator: str, action: str, timeout: int) -> Optional[object]:
        """Find element by partial attribute matching"""
        try:
            # Parse the original locator to extract attributes
            attributes = self._parse_locator_attributes(locator)
            if not attributes:
                return None

            # Try combinations of partial attributes
            for attr_name, attr_value in attributes.items():
                if len(attr_value) > 3:  # Only try meaningful attribute values
                    partial_value = attr_value[:len(attr_value)//2]  # Use first half
                    partial_xpath = f"//*[contains(@{attr_name}, '{partial_value}')]"
                    
                    element = self._find_direct_element(partial_xpath, timeout // 4)
                    if element and self._is_actionable(element, action):
                        self.logger.info(f"[TZ] Found by partial attribute: {partial_xpath}")
                        return element

            return None

        except Exception as e:
            self.logger.debug(f"[TZ] Partial attribute search failed: {str(e)}")
            return None

    def _find_in_vicinity(self, locator: str, action: str, timeout: int) -> Optional[object]:
        """Find actionable elements in the vicinity of the target location"""
        try:
            # This is a simplified vicinity search
            # In a real implementation, you might use coordinate-based searching
            
            # Try to find any element first to get a reference point
            reference_element = self._find_direct_element(locator, timeout // 4)
            if not reference_element:
                return None

            # Look for actionable siblings
            sibling_xpath = f"({locator})/following-sibling::*[@clickable='true'][1]"
            sibling = self._find_direct_element(sibling_xpath, timeout // 4)
            if sibling and self._is_actionable(sibling, action):
                self.logger.info("[TZ] Found actionable sibling")
                return sibling

            # Look for actionable elements in parent container
            parent_xpath = f"({locator})/parent::*//*[@clickable='true'][1]"
            nearby = self._find_direct_element(parent_xpath, timeout // 4)
            if nearby and self._is_actionable(nearby, action):
                self.logger.info("[TZ] Found actionable element in parent container")
                return nearby

            return None

        except Exception as e:
            self.logger.debug(f"[TZ] Vicinity search failed: {str(e)}")
            return None

    def _extract_keywords(self, description: str) -> List[str]:
        """Extract meaningful keywords from description"""
        try:
            # Simple keyword extraction
            import re
            
            # Remove common words and extract meaningful terms
            stop_words = {'the', 'a', 'an', 'and', 'or', 'but', 'in', 'on', 'at', 'to', 'for', 'of', 'with', 'by'}
            words = re.findall(r'\w+', description.lower())
            keywords = [word for word in words if len(word) > 2 and word not in stop_words]
            
            return keywords[:3]  # Return top 3 keywords
            
        except Exception:
            return []

    def _parse_locator_attributes(self, locator: str) -> Dict[str, str]:
        """Parse XPath locator to extract attributes"""
        try:
            import re
            
            attributes = {}
            
            # Extract @attribute='value' patterns
            attr_pattern = r'@(\w+)=[\'"](.*?)[\'"]'
            matches = re.findall(attr_pattern, locator)
            
            for attr_name, attr_value in matches:
                attributes[attr_name] = attr_value
                
            return attributes
            
        except Exception:
            return {}

    def _add_recovery_hint(self, strategy: str, description: str):
        """Add recovery hint for reporting"""
        hint = {
            'strategy': strategy,
            'description': description,
            'timestamp': time.time()
        }
        self.recovery_hints.append(hint)
        self.logger.info(f"[TZ] Smart Recovery: {strategy} - {description}")

    def get_recovery_hints(self) -> List[Dict[str, Any]]:
        """Get all recovery hints for reporting"""
        return self.recovery_hints.copy()

    def clear_recovery_hints(self):
        """Clear recovery hints"""
        self.recovery_hints.clear()

    def get_recovery_summary(self) -> str:
        """Get formatted summary of recovery actions"""
        if not self.recovery_hints:
            return "No smart recovery actions performed."

        summary_lines = ["Smart Recovery Actions:"]
        for hint in self.recovery_hints:
            summary_lines.append(f"  • {hint['strategy']}: {hint['description']}")

        return "\n".join(summary_lines)