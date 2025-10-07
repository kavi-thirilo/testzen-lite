"""
Function Generator - Auto-generates reusable functions from Excel patterns
Converts Excel test steps into callable functions like verifySettingsScreenDisplayed()
"""

import logging
import re
import json
from typing import Dict, List, Any, Callable
import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(__file__)))
from utils.helpers import save_json_safely, load_json_safely

logger = logging.getLogger(__name__)


class FunctionGenerator:
    """Auto-generates and manages reusable test functions from Excel patterns"""
    
    def __init__(self, function_cache_path='config/generated_functions.json'):
        self.function_cache_path = function_cache_path
        self.generated_functions = {}
        self.function_cache = load_json_safely(function_cache_path)
        self.action_handler = None
        self.element_finder = None
        
    def set_handlers(self, action_handler, element_finder):
        """Set the action handler and element finder for function execution"""
        self.action_handler = action_handler
        self.element_finder = element_finder
    
    def generate_functions_from_steps(self, test_steps: List[Dict[str, Any]]):
        """Analyze test steps and auto-generate reusable functions"""
        patterns_found = {}
        
        for step in test_steps:
            # Analyze step patterns and generate function names
            function_name = self._generate_function_name(step)
            
            if function_name:
                # Check if we already have this function
                if function_name not in self.function_cache:
                    function_def = self._create_function_definition(step, function_name)
                    self.function_cache[function_name] = function_def
                    patterns_found[function_name] = function_def
                
                # Replace step with function call
                step['generated_function'] = function_name
        
        # Save updated function cache
        if patterns_found:
            save_json_safely(self.function_cache, self.function_cache_path)
            logger.info(f"Generated {len(patterns_found)} new functions")
        
        return patterns_found
    
    def _generate_function_name(self, step: Dict[str, Any]) -> str:
        """Generate function name based on step pattern"""
        action = step.get('action', '').lower()
        description = step.get('description', '')
        locator_value = step.get('locator_value', '')
        expected = step.get('expected', '')
        
        # Pattern 1: Verify actions -> verify[ScreenName]Displayed()
        if action in ['verify', 'assert', 'validate']:
            if 'screen' in description.lower() or 'page' in description.lower() or 'displayed' in description.lower():
                screen_name = self._extract_screen_name(description, locator_value, expected)
                if screen_name:
                    return f"verify{screen_name}Displayed"
            
            elif 'button' in description.lower() or 'element' in description.lower():
                element_name = self._extract_element_name(description, locator_value)
                if element_name:
                    return f"verify{element_name}Visible"
            
            elif expected:
                element_name = self._extract_element_name(description, expected)
                if element_name:
                    return f"verify{element_name}Text"
        
        # Pattern 2: Click actions -> click[ElementName]()
        elif action in ['click', 'tap']:
            element_name = self._extract_element_name(description, locator_value)
            if element_name:
                if 'button' in description.lower():
                    return f"click{element_name}Button"
                elif 'tab' in description.lower():
                    return f"click{element_name}Tab"
                else:
                    return f"click{element_name}"
        
        # Pattern 3: Input actions -> enter[FieldName]()
        elif action in ['type', 'enter', 'input']:
            field_name = self._extract_element_name(description, locator_value)
            if field_name:
                return f"enter{field_name}"
        
        # Pattern 4: Navigation -> navigateTo[ScreenName]()
        elif action in ['navigate', 'goto']:
            screen_name = self._extract_screen_name(description, locator_value, expected)
            if screen_name:
                return f"navigateTo{screen_name}"
        
        # Pattern 5: Wait actions -> waitFor[ScreenName]()
        elif action == 'wait' and ('screen' in description.lower() or 'page' in description.lower()):
            screen_name = self._extract_screen_name(description, locator_value, expected)
            if screen_name:
                return f"waitFor{screen_name}"
        
        return ""
    
    def _extract_screen_name(self, description: str, locator_value: str, expected: str) -> str:
        """Extract screen name from various sources"""
        # Priority: expected result > description > locator
        text_sources = [expected, description, locator_value]
        
        for text in text_sources:
            if not text:
                continue
            
            # Look for screen/page patterns
            patterns = [
                r'(\w+)\s*screen',
                r'(\w+)\s*page',
                r'(\w+)\s*dashboard',
                r'(\w+)\s*menu',
                r'manage\s*(\w+)',
                r'(\w+)\s*billing',
                r'(\w+)\s*payment',
                r'(\w+)\s*confirmation'
            ]

            for pattern in patterns:
                match = re.search(pattern, text.lower())
                if match:
                    screen_name = match.group(1).title()
                    return screen_name
        
        return ""
    
    def _extract_element_name(self, description: str, locator_value: str) -> str:
        """Extract element name from description or locator"""
        text_sources = [description, locator_value]
        
        for text in text_sources:
            if not text:
                continue
            
            # Look for element patterns
            patterns = [
                r'(\w+)\s*button',
                r'(\w+)\s*tab',
                r'(\w+)\s*field',
                r'(\w+)\s*input',
                r'submit',
                r'continue',
                r'next',
                r'back',
                r'edit',
                r'save',
                r'settings',
                r'alerts'
            ]
            
            for pattern in patterns:
                if pattern in ['submit', 'continue', 'next', 'back', 'edit', 'save', 'settings', 'alerts']:
                    if pattern in text.lower():
                        return pattern.title()
                else:
                    match = re.search(pattern, text.lower())
                    if match:
                        return match.group(1).title()
        
        # Extract from locator ID
        if 'id/' in locator_value:
            id_part = locator_value.split('id/')[-1]
            # Convert snake_case or camelCase to TitleCase
            if '_' in id_part:
                return ''.join(word.title() for word in id_part.split('_'))
            elif any(c.isupper() for c in id_part[1:]):
                # camelCase - capitalize first letter
                return id_part[0].upper() + id_part[1:]
        
        return ""
    
    def _create_function_definition(self, step: Dict[str, Any], function_name: str) -> Dict[str, Any]:
        """Create function definition from step"""
        return {
            'name': function_name,
            'action': step.get('action'),
            'locator_type': step.get('locator_type'),
            'locator_value': step.get('locator_value'),
            'test_data': step.get('test_data'),
            'expected': step.get('expected'),
            'wait_time': step.get('wait_time', 0),
            'description': step.get('description'),
            'generated_from': 'excel_pattern',
            'usage_count': 0,
            'parameters': self._extract_parameters(step)
        }
    
    def _extract_parameters(self, step: Dict[str, Any]) -> List[str]:
        """Extract parameterizable values from step"""
        parameters = []
        
        test_data = step.get('test_data', '')
        expected = step.get('expected', '')
        
        # Look for variable patterns
        if '{' in test_data or '$' in test_data:
            parameters.append('test_data')
        
        if '{' in expected or '$' in expected:
            parameters.append('expected_value')
        
        return parameters
    
    def execute_function(self, function_name: str, **kwargs) -> Dict[str, Any]:
        """Execute a generated function"""
        if function_name not in self.function_cache:
            return {
                'status': 'fail',
                'message': f'Function not found: {function_name}'
            }
        
        func_def = self.function_cache[function_name]
        
        # Update usage count
        func_def['usage_count'] = func_def.get('usage_count', 0) + 1
        
        # Build step from function definition
        step = {
            'description': kwargs.get('description', f"Execute {function_name}()"),
            'action': func_def['action'],
            'locator_type': func_def['locator_type'],
            'locator_value': func_def['locator_value'],
            'test_data': kwargs.get('test_data', func_def['test_data']),
            'expected': kwargs.get('expected', func_def['expected']),
            'wait_time': func_def.get('wait_time', 0),
            'screenshot': kwargs.get('screenshot', False)
        }
        
        # Substitute parameters
        for param in func_def.get('parameters', []):
            if param in kwargs:
                value = kwargs[param]
                step['test_data'] = step['test_data'].replace(f'{{{param}}}', str(value))
                step['expected'] = step['expected'].replace(f'{{{param}}}', str(value))
        
        logger.info(f"Executing generated function: {function_name}()")
        
        # Execute using action handler
        if self.action_handler:
            return self.action_handler.execute_action(step)
        else:
            return {
                'status': 'skip',
                'message': 'Action handler not available'
            }
    
    def get_function_library(self) -> Dict[str, Any]:
        """Get all generated functions as a library"""
        return {
            'functions': self.function_cache,
            'total_functions': len(self.function_cache),
            'most_used': sorted(
                self.function_cache.items(), 
                key=lambda x: x[1].get('usage_count', 0), 
                reverse=True
            )[:10]
        }
    
    def generate_function_documentation(self) -> str:
        """Generate documentation for all functions"""
        doc = "# Auto-Generated Function Library\\n\\n"
        
        categories = {
            'verify': 'Verification Functions',
            'click': 'Click/Tap Functions',
            'enter': 'Input Functions', 
            'navigate': 'Navigation Functions',
            'wait': 'Wait Functions'
        }
        
        for category, title in categories.items():
            functions = [
                (name, func) for name, func in self.function_cache.items()
                if name.lower().startswith(category)
            ]
            
            if functions:
                doc += f"## {title}\\n\\n"
                for name, func in functions:
                    doc += f"### `{name}()`\\n"
                    doc += f"- **Action**: {func['action']}\\n"
                    doc += f"- **Description**: {func['description']}\\n"
                    doc += f"- **Usage Count**: {func.get('usage_count', 0)}\\n"
                    if func.get('parameters'):
                        doc += f"- **Parameters**: {', '.join(func['parameters'])}\\n"
                    doc += "\\n"
        
        return doc