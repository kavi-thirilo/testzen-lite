"""
Excel Parser - Reads test cases and steps from Excel files
"""

import pandas as pd
import logging
import os
from typing import List, Dict, Any

logger = logging.getLogger(__name__)


class ExcelParser:
    """Parse test cases from Excel files"""
    
    def __init__(self, excel_file_path):
        self.excel_file = excel_file_path
        self.test_data = None
        self.locators_data = None
        
    def load_test_file(self, sheet_name='TestCases'):
        """Load test cases from Excel file"""
        try:
            if not os.path.exists(self.excel_file):
                raise FileNotFoundError(f"Excel file not found: {self.excel_file}")
            
            # Read the test cases sheet
            self.test_data = pd.read_excel(self.excel_file, sheet_name=sheet_name)
            
            # Clean column names
            self.test_data.columns = [col.strip() for col in self.test_data.columns]
            
            # Fill NaN values with empty string
            self.test_data = self.test_data.fillna('')
            
            logger.info(f"Loaded {len(self.test_data)} test steps from {sheet_name}")
            
            # Try to load locators sheet if exists
            try:
                self.locators_data = pd.read_excel(self.excel_file, sheet_name='Locators')
                self.locators_data = self.locators_data.fillna('')
                logger.info("Loaded locators sheet")
            except:
                logger.info("No locators sheet found")
            
            return True
            
        except Exception as e:
            logger.error(f"Error loading Excel file: {str(e)}")
            raise
    
    def is_business_scenario_format(self) -> bool:
        """Detect if the Excel file uses business scenario format vs technical steps"""
        if self.test_data is None:
            return False
        
        # Check for business scenario columns
        scenario_indicators = ['Scenario', 'Business Process', 'Test Case', 'User Story']
        technical_indicators = ['Action', 'Locator Type', 'Locator Value']
        
        columns = [col.lower() for col in self.test_data.columns]
        
        has_scenario_cols = any(indicator.lower() in ' '.join(columns) for indicator in scenario_indicators)
        has_technical_cols = any(indicator.lower().replace(' ', '_') in ' '.join(columns) for indicator in technical_indicators)
        
        # If has scenario-like columns but lacks technical columns, it's likely business format
        return has_scenario_cols and not has_technical_cols

    def get_test_steps(self) -> List[Dict[str, Any]]:
        """Get all test steps as a list of dictionaries"""
        if self.test_data is None:
            raise ValueError("Test data not loaded. Call load_test_file() first")
        
        # Check if this is business scenario format
        if self.is_business_scenario_format():
            return self.get_business_scenarios()
        
        # Original technical steps format
        test_steps = []
        
        for index, row in self.test_data.iterrows():
            step = {
                'step_no': index + 1,
                'description': str(row.get('Description', '')),
                'action': str(row.get('Action', '')).lower(),
                'locator_name': str(row.get('Element', '')),
                'locator_type': str(row.get('Locator Type', '')),
                'locator_value': str(row.get('Locator Value', '')),
                'test_data': str(row.get('Test Data', '')),
                'expected': str(row.get('Expected Result', '')),
                'wait_time': int(row.get('Wait Time', 0)) if row.get('Wait Time', '') else 0,
                'screenshot': str(row.get('Screenshot', 'no')).lower() == 'yes',
                'optional': str(row.get('Optional', 'no')).lower() == 'yes',
                'condition': str(row.get('Condition', '')),
                'on_fail': str(row.get('On Fail', 'stop')).lower()
            }
            
            # If locator name is provided, try to get from locators sheet
            if step['locator_name'] and self.locators_data is not None:
                locator_info = self._get_locator_info(step['locator_name'])
                if locator_info:
                    step.update(locator_info)
            
            test_steps.append(step)
        
        return test_steps
    
    def get_business_scenarios(self) -> List[Dict[str, Any]]:
        """Get business scenarios for processing with scenario processor"""
        if self.test_data is None:
            raise ValueError("Test data not loaded. Call load_test_file() first")
        
        scenarios = []
        
        for index, row in self.test_data.iterrows():
            # Map various possible column names to standard format
            scenario = {
                'step_no': index + 1,
                'scenario': self._get_column_value(row, ['Scenario', 'Test Case', 'Business Process', 'User Story', 'Description']),
                'description': self._get_column_value(row, ['Description', 'Details', 'Notes']),
                'test_data': self._get_column_value(row, ['Test Data', 'Data', 'Input', 'Parameters']),
                'expected_result': self._get_column_value(row, ['Expected Result', 'Expected', 'Outcome', 'Result']),
                'preconditions': self._get_column_value(row, ['Preconditions', 'Prerequisites', 'Setup']),
                'username': self._get_column_value(row, ['Username', 'User', 'Login']),
                'password': self._get_column_value(row, ['Password', 'Pass', 'Pwd']),
                'optional': str(self._get_column_value(row, ['Optional', 'Skip'])).lower() == 'yes',
                'on_fail': str(self._get_column_value(row, ['On Fail', 'Failure Action'])).lower() or 'stop'
            }
            
            # Only add if we have a scenario
            if scenario['scenario']:
                scenarios.append(scenario)
        
        logger.info(f"Loaded {len(scenarios)} business scenarios")
        return scenarios
    
    def _get_column_value(self, row, possible_columns):
        """Get value from row using multiple possible column names"""
        for col in possible_columns:
            if col in row:
                value = row[col]
                if pd.notna(value):
                    return str(value).strip()
            
            # Try case-insensitive match
            for actual_col in row.index:
                if actual_col.lower() == col.lower():
                    value = row[actual_col]
                    if pd.notna(value):
                        return str(value).strip()
        
        return ''
    
    def _get_locator_info(self, locator_name) -> Dict[str, str]:
        """Get locator information from locators sheet"""
        try:
            locator_row = self.locators_data[
                self.locators_data['Name'].str.lower() == locator_name.lower()
            ]
            
            if not locator_row.empty:
                return {
                    'locator_type': str(locator_row.iloc[0].get('Type', '')),
                    'locator_value': str(locator_row.iloc[0].get('Value', ''))
                }
        except Exception as e:
            logger.warning(f"Could not find locator {locator_name}: {str(e)}")
        
        return {}
    
    def get_test_scenarios(self) -> List[Dict[str, Any]]:
        """Get test scenarios if sheet exists"""
        try:
            scenarios_df = pd.read_excel(self.excel_file, sheet_name='Scenarios')
            scenarios = []
            
            for index, row in scenarios_df.iterrows():
                scenario = {
                    'name': str(row.get('Scenario Name', f'Scenario_{index+1}')),
                    'description': str(row.get('Description', '')),
                    'test_steps': str(row.get('Test Steps', '')).split(','),
                    'data_set': str(row.get('Data Set', '')),
                    'enabled': str(row.get('Enabled', 'yes')).lower() == 'yes'
                }
                scenarios.append(scenario)
            
            return scenarios
            
        except Exception as e:
            logger.info("No scenarios sheet found")
            return []
    
    def get_test_data_sets(self) -> Dict[str, List[Dict[str, Any]]]:
        """Get test data sets if sheet exists"""
        try:
            data_df = pd.read_excel(self.excel_file, sheet_name='TestData')
            data_sets = {}
            
            # Group by data set name
            if 'DataSet' in data_df.columns:
                for data_set_name, group in data_df.groupby('DataSet'):
                    data_sets[data_set_name] = group.to_dict('records')
            else:
                # If no DataSet column, use all as default
                data_sets['default'] = data_df.to_dict('records')
            
            return data_sets
            
        except Exception as e:
            logger.info("No test data sheet found")
            return {}
    
    def validate_test_file(self) -> tuple:
        """Validate the test file structure"""
        errors = []
        warnings = []
        
        # Check required columns
        required_columns = ['Action']
        for col in required_columns:
            if col not in self.test_data.columns:
                errors.append(f"Required column '{col}' not found")
        
        # Check for at least one locator method
        has_locator = False
        if 'Element' in self.test_data.columns or \
           ('Locator Type' in self.test_data.columns and 'Locator Value' in self.test_data.columns):
            has_locator = True
        
        if not has_locator:
            warnings.append("No locator columns found. Make sure to provide either 'Element' or 'Locator Type' and 'Locator Value'")
        
        # Validate actions
        valid_actions = [
            'click', 'tap', 'enter', 'type', 'clear', 'swipe', 'scroll',
            'verify', 'assert', 'wait', 'screenshot', 'switch_context',
            'navigate', 'back', 'forward', 'refresh', 'close', 'quit',
            'if', 'else', 'endif', 'loop', 'endloop', 'call', 'return'
        ]
        
        for index, row in self.test_data.iterrows():
            action = str(row.get('Action', '')).lower()
            if action and action not in valid_actions:
                warnings.append(f"Row {index+1}: Unknown action '{action}'")
        
        return errors, warnings