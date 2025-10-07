"""
Test Runner - Main test execution engine
"""

import logging
import time
import os
from datetime import datetime
from typing import List, Dict, Any
from .driver_manager import DriverManager
from .element_finder import ElementFinder
from .excel_parser import ExcelParser
from .scenario_processor import ScenarioProcessor
from .function_generator import FunctionGenerator
from ..handlers.action_handler import ActionHandler
from ..locators.locator_manager import LocatorManager
from ..reports.report_generator import ReportGenerator
import sys
import os
sys.path.append(os.path.join(os.path.dirname(__file__), '../..'))
from src.utils.excel_utils import ExcelManager

logger = logging.getLogger(__name__)


class TestRunner:
    """Main test execution engine"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.driver_manager = None
        self.element_finder = None
        self.action_handler = None
        self.locator_manager = None
        self.scenario_processor = None
        self.function_generator = None
        self.report_generator = None
        self.excel_manager = None
        self.test_results = []
        self.current_test = None
        
    def setup(self):
        """Setup test environment"""
        try:
            # Setup logging
            self._setup_logging()
            
            # Initialize driver
            self.driver_manager = DriverManager(self.config)
            self.driver_manager.initialize_driver()
            
            # Initialize element finder
            self.element_finder = ElementFinder(
                self.driver_manager.driver,
                self.config.get('explicit_wait', 20)
            )
            
            # Initialize action handler
            self.action_handler = ActionHandler(self.driver_manager, self.element_finder)
            
            # Initialize locator manager
            self.locator_manager = LocatorManager()
            self.locator_manager.set_platform(self.config.get('platform', 'android'))
            
            # Load locators if provided
            locator_file = self.config.get('locator_file')
            if locator_file:
                if locator_file.endswith('.json'):
                    self.locator_manager.load_from_json(locator_file)
                elif locator_file.endswith(('.xlsx', '.xls')):
                    self.locator_manager.load_from_excel(locator_file)
            
            # Initialize scenario processor
            self.scenario_processor = ScenarioProcessor()
            
            # Load scenario library if provided
            scenario_library_file = self.config.get('scenario_library_file')
            if scenario_library_file:
                self.scenario_processor.load_scenario_library(scenario_library_file)
            
            # Initialize function generator
            self.function_generator = FunctionGenerator()
            self.function_generator.set_handlers(self.action_handler, self.element_finder)
            
            # Initialize report generator
            self.report_generator = ReportGenerator(self.config.get('report_dir', 'reports'))
            
            logger.info("Test setup completed successfully")
            return True
            
        except Exception as e:
            logger.error(f"Setup failed: {str(e)}")
            return False
    
    def execute_test_file(self, excel_file: str, sheet_name: str = 'TestCases') -> Dict[str, Any]:
        """Execute test cases from Excel file"""
        start_time = datetime.now()
        test_result = {
            'test_file': excel_file,
            'start_time': start_time,
            'end_time': None,
            'status': 'pass',
            'total_steps': 0,
            'passed_steps': 0,
            'failed_steps': 0,
            'skipped_steps': 0,
            'step_results': []
        }
        
        try:
            # Initialize Excel manager for status updates
            self.excel_manager = ExcelManager(excel_file)
            self.excel_manager.load_test_data()
            
            # Parse Excel file
            parser = ExcelParser(excel_file)
            parser.load_test_file(sheet_name)
            
            # Validate test file
            errors, warnings = parser.validate_test_file()
            if errors:
                logger.error(f"Test file validation errors: {errors}")
                test_result['status'] = 'error'
                test_result['message'] = f"Validation errors: {errors}"
                return test_result
            
            if warnings:
                for warning in warnings:
                    logger.warning(warning)
            
            # Load test data sets if available
            data_sets = parser.get_test_data_sets()
            
            # Get test steps - handle both technical and business scenario formats
            if parser.is_business_scenario_format():
                logger.info("Detected business scenario format - converting to technical steps")
                business_scenarios = parser.get_business_scenarios()
                test_steps = self.scenario_processor.process_business_scenarios(business_scenarios)
            else:
                logger.info("Using technical step format")
                test_steps = parser.get_test_steps()
            
            # Auto-generate functions from test step patterns
            logger.info("Analyzing patterns and generating reusable functions...")
            generated_functions = self.function_generator.generate_functions_from_steps(test_steps)
            
            if generated_functions:
                logger.info(f"Generated {len(generated_functions)} new functions:")
                for func_name in generated_functions.keys():
                    logger.info(f"  - {func_name}()")
            
            test_result['total_steps'] = len(test_steps)
            test_result['generated_functions'] = list(generated_functions.keys())
            
            # Execute test steps
            for step_index, step in enumerate(test_steps, 1):
                logger.info(f"Executing Step {step_index}: {step.get('description', step.get('action'))}")
                
                # Update locator information from locator manager if needed
                if step.get('locator_name') and self.locator_manager:
                    locator_info = self.locator_manager.get_locator(step['locator_name'])
                    if locator_info:
                        step['locator_type'] = locator_info.get('type', step.get('locator_type'))
                        step['locator_value'] = locator_info.get('value', step.get('locator_value'))
                
                # Set test data context if available
                if data_sets:
                    # Use first data set by default or specific one if mentioned
                    data_set_name = step.get('data_set', 'default')
                    if data_set_name in data_sets and data_sets[data_set_name]:
                        self.action_handler.set_test_data_context(data_sets[data_set_name][0])
                
                # Execute action
                step_result = self.action_handler.execute_action(step)
                step_result['step_no'] = step_index
                step_result['description'] = step.get('description', '')
                step_result['action'] = step.get('action', '')
                
                # Update Excel status with colors
                if self.excel_manager:
                    excel_status = "PASSED" if step_result['status'] == 'pass' else "FAILED" if step_result['status'] == 'fail' else "SKIPPED"
                    result_message = step_result.get('message', '')
                    self.excel_manager.update_step_status(step_index - 1, excel_status, result_message)
                
                # Update counters
                if step_result['status'] == 'pass':
                    test_result['passed_steps'] += 1
                elif step_result['status'] == 'fail':
                    test_result['failed_steps'] += 1
                    
                    # Check if we should stop on failure
                    if step.get('on_fail', 'stop') == 'stop' and not step.get('optional', False):
                        test_result['status'] = 'fail'
                        logger.error(f"Test failed at step {step_index}")
                        break
                else:
                    test_result['skipped_steps'] += 1
                
                test_result['step_results'].append(step_result)
                
                # Small delay between steps
                time.sleep(0.5)
            
            # Determine overall status
            if test_result['failed_steps'] > 0:
                test_result['status'] = 'fail'
            elif test_result['passed_steps'] == test_result['total_steps']:
                test_result['status'] = 'pass'
            else:
                test_result['status'] = 'partial'
            
        except Exception as e:
            logger.error(f"Test execution error: {str(e)}")
            test_result['status'] = 'error'
            test_result['message'] = str(e)
        
        finally:
            test_result['end_time'] = datetime.now()
            test_result['duration'] = (test_result['end_time'] - start_time).total_seconds()
            
            # Generate report
            if self.report_generator:
                self.report_generator.generate_test_report(test_result)
            
            self.test_results.append(test_result)
        
        return test_result
    
    def execute_test_suite(self, test_files: List[str]) -> Dict[str, Any]:
        """Execute multiple test files as a suite"""
        suite_start = datetime.now()
        suite_result = {
            'suite_name': self.config.get('suite_name', 'Test Suite'),
            'start_time': suite_start,
            'end_time': None,
            'total_tests': len(test_files),
            'passed_tests': 0,
            'failed_tests': 0,
            'test_results': []
        }
        
        for test_file in test_files:
            logger.info(f"Executing test file: {test_file}")
            test_result = self.execute_test_file(test_file)
            
            if test_result['status'] == 'pass':
                suite_result['passed_tests'] += 1
            else:
                suite_result['failed_tests'] += 1
            
            suite_result['test_results'].append(test_result)
        
        suite_result['end_time'] = datetime.now()
        suite_result['duration'] = (suite_result['end_time'] - suite_start).total_seconds()
        
        # Generate suite report
        if self.report_generator:
            self.report_generator.generate_suite_report(suite_result)
        
        return suite_result
    
    def teardown(self):
        """Teardown test environment"""
        try:
            if self.driver_manager:
                self.driver_manager.quit_driver()
            
            logger.info("Test teardown completed")
            
        except Exception as e:
            logger.error(f"Teardown error: {str(e)}")
    
    def _setup_logging(self):
        """Setup logging configuration"""
        log_level = self.config.get('log_level', 'INFO')
        log_format = '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
        
        # Create logs directory if not exists
        os.makedirs('logs', exist_ok=True)
        
        # Configure logging
        logging.basicConfig(
            level=getattr(logging, log_level),
            format=log_format,
            handlers=[
                logging.FileHandler(f"logs/test_run_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log"),
                logging.StreamHandler()
            ]
        )
    
    def run(self, test_files: List[str]) -> bool:
        """Main run method"""
        success = True
        
        try:
            # Setup
            if not self.setup():
                return False
            
            # Execute tests
            if len(test_files) == 1:
                result = self.execute_test_file(test_files[0])
                success = result['status'] == 'pass'
            else:
                result = self.execute_test_suite(test_files)
                success = result['failed_tests'] == 0
            
            # Print summary
            self._print_summary(result)
            
        except Exception as e:
            logger.error(f"Test run error: {str(e)}")
            success = False
            
        finally:
            # Teardown
            self.teardown()
        
        return success
    
    def _print_summary(self, result: Dict[str, Any]):
        """Print test execution summary"""
        print("\n" + "="*60)
        print("TEST EXECUTION SUMMARY")
        print("="*60)
        
        if 'total_steps' in result:
            # Single test summary
            print(f"Test File: {result.get('test_file', 'Unknown')}")
            print(f"Status: {result['status'].upper()}")
            print(f"Total Steps: {result['total_steps']}")
            print(f"Passed: {result['passed_steps']}")
            print(f"Failed: {result['failed_steps']}")
            print(f"Skipped: {result['skipped_steps']}")
            print(f"Duration: {result.get('duration', 0):.2f} seconds")
        else:
            # Suite summary
            print(f"Suite Name: {result.get('suite_name', 'Test Suite')}")
            print(f"Total Tests: {result['total_tests']}")
            print(f"Passed: {result['passed_tests']}")
            print(f"Failed: {result['failed_tests']}")
            print(f"Duration: {result.get('duration', 0):.2f} seconds")
        
        print("="*60 + "\n")