"""
Locator Manager - Manages element locators from JSON/Excel configuration
"""

import json
import logging
import os
from typing import Dict, Any
import pandas as pd

logger = logging.getLogger(__name__)


class LocatorManager:
    """Manages element locators from various sources"""
    
    def __init__(self):
        self.locators = {}
        self.platform = 'android'  # default platform
        
    def load_from_json(self, json_file: str):
        """Load locators from JSON file"""
        try:
            if not os.path.exists(json_file):
                logger.warning(f"Locator file not found: {json_file}")
                return False
            
            with open(json_file, 'r') as f:
                data = json.load(f)
            
            # Support platform-specific locators
            if 'android' in data or 'ios' in data:
                self.locators = data
            else:
                # Assume flat structure
                self.locators = {'common': data}
            
            logger.info(f"Loaded {len(self._flatten_locators())} locators from {json_file}")
            return True
            
        except Exception as e:
            logger.error(f"Error loading locator file: {str(e)}")
            return False
    
    def load_from_excel(self, excel_file: str, sheet_name: str = 'Locators'):
        """Load locators from Excel file"""
        try:
            if not os.path.exists(excel_file):
                logger.warning(f"Excel file not found: {excel_file}")
                return False
            
            df = pd.read_excel(excel_file, sheet_name=sheet_name)
            df = df.fillna('')
            
            for _, row in df.iterrows():
                name = str(row.get('Name', '')).strip()
                if not name:
                    continue
                
                locator = {
                    'type': str(row.get('Type', 'id')).lower(),
                    'value': str(row.get('Value', '')),
                    'description': str(row.get('Description', '')),
                    'platform': str(row.get('Platform', 'common')).lower()
                }
                
                # Support platform-specific locators
                platform = locator['platform']
                if platform not in self.locators:
                    self.locators[platform] = {}
                
                self.locators[platform][name] = locator
            
            logger.info(f"Loaded {len(self._flatten_locators())} locators from Excel")
            return True
            
        except Exception as e:
            logger.error(f"Error loading locators from Excel: {str(e)}")
            return False
    
    def get_locator(self, name: str, platform: str = None) -> Dict[str, str]:
        """Get locator by name and platform"""
        platform = platform or self.platform
        
        # Try platform-specific first
        if platform in self.locators and name in self.locators[platform]:
            return self.locators[platform][name]
        
        # Try common locators
        if 'common' in self.locators and name in self.locators['common']:
            return self.locators['common'][name]
        
        # Search all platforms
        for plat, locs in self.locators.items():
            if name in locs:
                return locs[name]
        
        logger.warning(f"Locator not found: {name}")
        return {}
    
    def set_platform(self, platform: str):
        """Set the current platform"""
        self.platform = platform.lower()
        logger.info(f"Platform set to: {self.platform}")
    
    def add_locator(self, name: str, locator_type: str, value: str, 
                    platform: str = 'common', description: str = ''):
        """Add a new locator"""
        if platform not in self.locators:
            self.locators[platform] = {}
        
        self.locators[platform][name] = {
            'type': locator_type.lower(),
            'value': value,
            'description': description,
            'platform': platform
        }
        
        logger.info(f"Added locator: {name} for platform: {platform}")
    
    def save_to_json(self, json_file: str):
        """Save locators to JSON file"""
        try:
            with open(json_file, 'w') as f:
                json.dump(self.locators, f, indent=2)
            
            logger.info(f"Saved locators to {json_file}")
            return True
            
        except Exception as e:
            logger.error(f"Error saving locators: {str(e)}")
            return False
    
    def _flatten_locators(self) -> Dict[str, Dict]:
        """Flatten nested locator structure for counting"""
        flat = {}
        for platform, locs in self.locators.items():
            for name, loc in locs.items():
                flat[f"{platform}.{name}"] = loc
        return flat
    
    def get_all_locator_names(self) -> list:
        """Get all available locator names"""
        names = []
        for platform, locs in self.locators.items():
            names.extend(locs.keys())
        return list(set(names))
    
    def validate_locators(self) -> tuple:
        """Validate all locators"""
        errors = []
        warnings = []
        
        for platform, locs in self.locators.items():
            for name, loc in locs.items():
                if not loc.get('value'):
                    errors.append(f"{platform}.{name}: Missing locator value")
                
                if loc.get('type') not in ['id', 'xpath', 'accessibility_id', 'class', 
                                          'name', 'android_uiautomator', 'ios_predicate', 
                                          'ios_class_chain']:
                    warnings.append(f"{platform}.{name}: Unknown locator type '{loc.get('type')}'")
        
        return errors, warnings


class PageObjectModel:
    """Simple Page Object Model support"""
    
    def __init__(self, locator_manager: LocatorManager):
        self.locator_manager = locator_manager
        self.pages = {}
    
    def load_page_definitions(self, json_file: str):
        """Load page object definitions from JSON"""
        try:
            with open(json_file, 'r') as f:
                self.pages = json.load(f)
            
            logger.info(f"Loaded {len(self.pages)} page definitions")
            return True
            
        except Exception as e:
            logger.error(f"Error loading page definitions: {str(e)}")
            return False
    
    def get_page_locator(self, page_name: str, element_name: str) -> Dict[str, str]:
        """Get locator for a specific page element"""
        if page_name not in self.pages:
            logger.warning(f"Page not found: {page_name}")
            return {}
        
        page = self.pages[page_name]
        if element_name not in page.get('elements', {}):
            logger.warning(f"Element not found in page {page_name}: {element_name}")
            return {}
        
        element = page['elements'][element_name]
        
        # If it's a reference to global locator
        if 'ref' in element:
            return self.locator_manager.get_locator(element['ref'])
        
        # Direct locator definition
        return element
    
    def get_page_elements(self, page_name: str) -> Dict[str, Dict]:
        """Get all elements for a page"""
        if page_name not in self.pages:
            return {}
        
        return self.pages[page_name].get('elements', {})