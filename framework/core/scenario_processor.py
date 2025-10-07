"""
Scenario Processor - Handles business scenario to technical steps mapping
"""

import logging
import json
import os
from typing import Dict, List, Any
import pandas as pd

logger = logging.getLogger(__name__)


class ScenarioProcessor:
    """Converts business scenarios to technical test steps"""
    
    def __init__(self, scenario_library_path=None):
        self.scenario_library = {}
        self.step_templates = {}
        
        if scenario_library_path:
            self.load_scenario_library(scenario_library_path)
    
    def load_scenario_library(self, library_path):
        """Load scenario definitions from JSON or Excel file"""
        try:
            if library_path.endswith('.json'):
                self._load_json_library(library_path)
            elif library_path.endswith(('.xlsx', '.xls')):
                self._load_excel_library(library_path)
            else:
                logger.error(f"Unsupported library format: {library_path}")
                return False
            
            logger.info(f"Loaded {len(self.scenario_library)} scenarios from {library_path}")
            return True
            
        except Exception as e:
            logger.error(f"Error loading scenario library: {str(e)}")
            return False
    
    def _load_json_library(self, json_path):
        """Load scenarios from JSON file"""
        with open(json_path, 'r') as f:
            data = json.load(f)
            self.scenario_library = data.get('scenarios', {})
            self.step_templates = data.get('step_templates', {})
    
    def _load_excel_library(self, excel_path):
        """Load scenarios from Excel file with multiple sheets"""
        try:
            # Load scenarios sheet
            scenarios_df = pd.read_excel(excel_path, sheet_name='Scenarios')
            scenarios_df = scenarios_df.fillna('')
            
            for _, row in scenarios_df.iterrows():
                scenario_name = str(row.get('Scenario Name', '')).strip()
                if not scenario_name:
                    continue
                
                # Parse step sequence
                steps_str = str(row.get('Steps', ''))
                steps = [step.strip() for step in steps_str.split('|') if step.strip()]
                
                self.scenario_library[scenario_name] = {
                    'description': str(row.get('Description', '')),
                    'steps': steps,
                    'preconditions': str(row.get('Preconditions', '')),
                    'expected_outcome': str(row.get('Expected Outcome', '')),
                    'data_requirements': str(row.get('Data Requirements', ''))
                }
            
            # Load step templates sheet
            try:
                templates_df = pd.read_excel(excel_path, sheet_name='StepTemplates')
                templates_df = templates_df.fillna('')
                
                for _, row in templates_df.iterrows():
                    template_name = str(row.get('Template Name', '')).strip()
                    if not template_name:
                        continue
                    
                    # Parse technical steps
                    tech_steps_str = str(row.get('Technical Steps', ''))
                    tech_steps = []
                    
                    if tech_steps_str:
                        # Split by newline - handle both \\n and actual newlines  
                        raw_steps = tech_steps_str.replace('\\n', '\n').split('\n')
                        for step in raw_steps:
                            step = step.strip()
                            if step and '|' in step:
                                # Parse step format: action|locator_type|locator_value|test_data
                                parts = [p.strip() for p in step.split('|')]
                                if len(parts) >= 1:
                                    tech_steps.append({
                                        'action': parts[0],
                                        'locator_type': parts[1] if len(parts) > 1 else '',
                                        'locator_value': parts[2] if len(parts) > 2 else '',
                                        'test_data': parts[3] if len(parts) > 3 else '',
                                        'description': parts[4] if len(parts) > 4 else f"Execute {parts[0]} action",
                                        'screenshot': parts[0] == 'screenshot',
                                        'wait_time': 3 if parts[0] == 'wait' else 0
                                    })
                    
                    self.step_templates[template_name] = {
                        'description': str(row.get('Description', '')),
                        'technical_steps': tech_steps,
                        'parameters': str(row.get('Parameters', '')).split(',') if row.get('Parameters') else []
                    }
                    
            except Exception as e:
                logger.warning(f"No step templates sheet found or error reading it: {e}")
            
        except Exception as e:
            logger.error(f"Error loading Excel library: {str(e)}")
            raise
    
    def process_business_scenarios(self, scenarios_data):
        """Convert business scenario data to technical test steps"""
        technical_steps = []
        
        for scenario_row in scenarios_data:
            scenario_name = scenario_row.get('scenario', '').strip()  # Changed from 'Scenario' to 'scenario'
            
            if not scenario_name:
                continue
            
            logger.info(f"Processing scenario: {scenario_name}")
            
            # Look up scenario in library
            if scenario_name in self.scenario_library:
                scenario_def = self.scenario_library[scenario_name]
                steps = self._expand_scenario_steps(scenario_def, scenario_row)
                technical_steps.extend(steps)
            else:
                # Create a generic step for unknown scenarios
                logger.warning(f"Scenario not found in library: {scenario_name}")
                generic_step = {
                    'step_no': len(technical_steps) + 1,
                    'description': f"Manual verification: {scenario_name}",
                    'action': 'verify',
                    'locator_type': '',
                    'locator_value': '',
                    'test_data': scenario_row.get('test_data', ''),
                    'expected': scenario_row.get('expected_result', ''),
                    'screenshot': True,
                    'optional': True,
                    'on_fail': 'continue',
                    'wait_time': 2
                }
                technical_steps.append(generic_step)
        
        return technical_steps
    
    def _expand_scenario_steps(self, scenario_def, scenario_data):
        """Expand a business scenario into technical steps"""
        technical_steps = []
        
        for step_template_name in scenario_def.get('steps', []):
            if step_template_name in self.step_templates:
                template = self.step_templates[step_template_name]
                
                # Expand each technical step in the template
                for tech_step in template.get('technical_steps', []):
                    step = {
                        'step_no': len(technical_steps) + 1,
                        'description': self._substitute_parameters(tech_step.get('description', ''), scenario_data),
                        'action': tech_step.get('action', ''),
                        'locator_type': tech_step.get('locator_type', ''),
                        'locator_value': self._substitute_parameters(tech_step.get('locator_value', ''), scenario_data),
                        'test_data': self._substitute_parameters(tech_step.get('test_data', ''), scenario_data),
                        'expected': self._substitute_parameters(scenario_def.get('expected_outcome', ''), scenario_data),
                        'screenshot': tech_step.get('screenshot', False),
                        'optional': False,
                        'on_fail': 'stop',
                        'wait_time': tech_step.get('wait_time', 0)
                    }
                    technical_steps.append(step)
            else:
                # Create placeholder for unknown step template
                logger.warning(f"Step template not found: {step_template_name}")
                placeholder_step = {
                    'step_no': len(technical_steps) + 1,
                    'description': f"Manual step: {step_template_name}",
                    'action': 'wait',
                    'locator_type': '',
                    'locator_value': '',
                    'test_data': '2',
                    'expected': f"Complete {step_template_name} manually",
                    'screenshot': True,
                    'optional': True,
                    'on_fail': 'continue',
                    'wait_time': 2
                }
                technical_steps.append(placeholder_step)
        
        return technical_steps
    
    def _substitute_parameters(self, text, data):
        """Substitute parameters in text with actual data"""
        if not text or not isinstance(text, str):
            return text
        
        # Replace common placeholders
        replacements = {
            '{username}': data.get('Username', ''),
            '{password}': data.get('Password', ''),
            '{test_data}': data.get('Test Data', ''),
            '{phone}': data.get('Phone', ''),
            '{email}': data.get('Email', '')
        }
        
        result = text
        for placeholder, value in replacements.items():
            result = result.replace(placeholder, str(value))
        
        return result
    
    def create_scenario_library_template(self, output_path):
        """Create a template Excel file for scenario library"""
        try:
            with pd.ExcelWriter(output_path, engine='openpyxl') as writer:
                
                # Scenarios sheet
                scenarios_data = {
                    'Scenario Name': [
                        'Log In Mobile App',
                        'Navigate to Settings',
                        'Update User Profile',
                        'Submit Form',
                        'Verify Confirmation'
                    ],
                    'Description': [
                        'User logs into mobile application',
                        'Navigate to settings section',
                        'Update user profile information',
                        'Submit the form',
                        'Verify submission confirmation'
                    ],
                    'Steps': [
                        'Launch App|Enter Credentials|Tap Login|Verify Dashboard',
                        'Open Menu|Select Settings',
                        'Fill Profile Form|Review Details',
                        'Submit Form|Confirm Submission',
                        'Check Confirmation|Take Screenshot'
                    ],
                    'Preconditions': [
                        'App installed, valid credentials available',
                        'User logged in, settings accessible',
                        'Profile form accessible',
                        'All required fields completed',
                        'Form submitted successfully'
                    ],
                    'Expected Outcome': [
                        'User successfully logged into dashboard',
                        'Settings interface displayed',
                        'Profile details entered and validated',
                        'Form submitted with confirmation',
                        'Confirmation page displayed'
                    ],
                    'Data Requirements': [
                        'username, password',
                        'user_type',
                        'name, email, phone',
                        'verification_code',
                        'confirmation_number'
                    ]
                }
                
                df_scenarios = pd.DataFrame(scenarios_data)
                df_scenarios.to_excel(writer, sheet_name='Scenarios', index=False)
                
                # Step Templates sheet
                templates_data = {
                    'Template Name': [
                        'Launch App',
                        'Enter Credentials',
                        'Tap Login',
                        'Verify Dashboard',
                        'Navigate to Settings',
                        'Fill Form'
                    ],
                    'Description': [
                        'Launch the mobile application',
                        'Enter username and password',
                        'Tap the login button',
                        'Verify user is on dashboard',
                        'Navigate to settings',
                        'Fill out form fields'
                    ],
                    'Technical Steps': [
                        'wait|id|com.example.app:id/splash|3\nverify|id|com.example.app:id/app_logo|App Logo',
                        'type|id|com.example.app:id/username|{username}\ntype|id|com.example.app:id/password|{password}',
                        'click|id|com.example.app:id/login_button|',
                        'verify|accessibility_id|dashboard-title|Dashboard\nscreenshot|||',
                        'click|accessibility_id|settings-menu|\nwait|||2',
                        'type|id|com.example.app:id/input_field|{test_data}\nclick|xpath|//button[@text="Next"]|'
                    ],
                    'Parameters': [
                        '',
                        'username,password',
                        '',
                        '',
                        '',
                        'test_data'
                    ]
                }
                
                df_templates = pd.DataFrame(templates_data)
                df_templates.to_excel(writer, sheet_name='StepTemplates', index=False)
            
            logger.info(f"Created scenario library template: {output_path}")
            return True
            
        except Exception as e:
            logger.error(f"Error creating template: {str(e)}")
            return False