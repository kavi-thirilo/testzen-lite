#!/usr/bin/env python3
"""
Compose UI helper utilities for TestZen automation framework
"""

import time


class ComposeUIHelper:
    """Helper class for handling Compose UI elements"""

    def __init__(self, driver):
        self.driver = driver

    def click_compose_element(self, element):
        """Click on a Compose element using smart handling"""
        try:
            # Try regular click first
            element.click()
            return True
        except Exception as e:
            print(f"[TZ] Regular click failed: {e}")
            # Try coordinate click as fallback
            return self.click_element_by_coordinates(element)

    def click_element_by_coordinates(self, element):
        """Click element using its bounds coordinates"""
        try:
            bounds = element.get_attribute("bounds")
            if bounds:
                import re
                coords = re.findall(r'\d+', bounds)
                if len(coords) == 4:
                    x1, y1, x2, y2 = map(int, coords)
                    center_x = (x1 + x2) // 2
                    center_y = (y1 + y2) // 2
                    return self.click_at_coordinates(center_x, center_y)
            return False
        except Exception as e:
            print(f"[TZ] Coordinate click failed: {e}")
            return False

    def click_at_coordinates(self, x, y):
        """Click at specific coordinates"""
        try:
            self.driver.tap([(x, y)])
            time.sleep(1)
            return True
        except Exception as e:
            print(f"[TZ] Tap at coordinates failed: {e}")
            return False