#!/usr/bin/env python3
"""
TestZen - Main Entry Point
No-Code Test Automation Framework
"""

import argparse
import sys
import os
import yaml
import json

# Add framework to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'framework'))

from core.test_runner import TestRunner


def load_config(config_file):
 """Load configuration from YAML or JSON file"""
 if config_file.endswith('.yaml') or config_file.endswith('.yml'):
 with open(config_file, 'r') as f:
 return yaml.safe_load(f)
 elif config_file.endswith('.json'):
 with open(config_file, 'r') as f:
 return json.load(f)
 else:
 raise ValueError(f"Unsupported config file format: {config_file}")


def main():
 """Main entry point"""
 parser = argparse.ArgumentParser(
 description='TestZen - No-Code Test Automation Framework',
 formatter_class=argparse.RawDescriptionHelpFormatter,
 epilog="""
    Examples:
 python scripts/run_tests.py tests/test_cases.xlsx
 python scripts/run_tests.py tests/test_cases.xlsx --config config/config.yaml
 python scripts/run_tests.py tests/test1.xlsx tests/test2.xlsx --suite "Regression Suite"
 python scripts/run_tests.py tests/test_cases.xlsx --platform ios --device "iPhone 12"
 """
 )
 
 # Required arguments
 parser.add_argument(
 'test_files',
 nargs='+',
 help='Excel test file(s) to execute'
 )
 
 # Optional arguments
 parser.add_argument(
 '-c', '--config',
 default='config/config.yaml',
 help='Configuration file (default: config/config.yaml)'
 )
 
 parser.add_argument(
 '-p', '--platform',
 choices=['android', 'ios'],
 help='Override platform from config'
 )
 
 parser.add_argument(
 '-d', '--device',
 help='Override device name from config'
 )
 
 parser.add_argument(
 '-s', '--suite',
 help='Test suite name'
 )
 
 parser.add_argument(
 '-l', '--locators',
 help='Locators file (JSON or Excel)'
 )
 
 parser.add_argument(
 '--app-path',
 help='Path to app file (APK/IPA)'
 )
 
 parser.add_argument(
 '--app-package',
 help='Android app package (Android only)'
 )
 
 parser.add_argument(
 '--app-activity',
 help='Android app activity (Android only)'
 )
 
 parser.add_argument(
 '--bundle-id',
 help='iOS bundle ID (iOS only)'
 )
 
 parser.add_argument(
 '--appium-server',
 help='Appium server URL'
 )
 
 parser.add_argument(
 '--log-level',
 choices=['DEBUG', 'INFO', 'WARNING', 'ERROR'],
 help='Logging level'
 )
 
 parser.add_argument(
 '--report-dir',
 help='Report output directory'
 )
 
 parser.add_argument(
 '--no-reset',
 action='store_true',
 help='Do not reset app state'
 )
 
 parser.add_argument(
 '--full-reset',
 action='store_true',
 help='Perform full app reset'
 )
 
 args = parser.parse_args()
 
 # Change to project root directory
 project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
 os.chdir(project_root)
 
 # Load configuration
 try:
 if os.path.exists(args.config):
 config = load_config(args.config)
 else:
 print(f"Config file not found: {args.config}")
 print("Using default configuration...")
 config = {}
 except Exception as e:
 print(f"Error loading config: {e}")
 sys.exit(1)
 
 # Override config with command line arguments
 if args.platform:
 config['platform'] = args.platform
 if args.device:
 config['device_name'] = args.device
 if args.suite:
 config['suite_name'] = args.suite
 if args.locators:
 config['locator_file'] = args.locators
 if args.app_path:
 config['app_path'] = args.app_path
 if args.app_package:
 config['app_package'] = args.app_package
 if args.app_activity:
 config['app_activity'] = args.app_activity
 if args.bundle_id:
 config['bundle_id'] = args.bundle_id
 if args.appium_server:
 config['appium_server'] = args.appium_server
 if args.log_level:
 config['log_level'] = args.log_level
 if args.report_dir:
 config['report_dir'] = args.report_dir
 if args.no_reset:
 config['no_reset'] = True
 if args.full_reset:
 config['full_reset'] = True
 
 # Validate test files exist
 for test_file in args.test_files:
 if not os.path.exists(test_file):
 print(f"Test file not found: {test_file}")
 sys.exit(1)
 
 # Print configuration
 print("\n" + "="*60)
 print("TestZen - No-Code Test Automation Framework")
 print("="*60)
 print(f"Platform: {config.get('platform', 'android')}")
 print(f"Device: {config.get('device_name', 'Not specified')}")
 print(f"Test Files: {', '.join(args.test_files)}")
 print(f"Report Directory: {config.get('report_dir', 'reports')}")
 print("="*60 + "\n")
 
 # Create and run test runner
 runner = TestRunner(config)
 
 try:
 success = runner.run(args.test_files)
 
 if success:
 print("\n All tests completed successfully!")
 sys.exit(0)
 else:
 print("\n Some tests failed. Check the reports for details.")
 sys.exit(1)
 
 except KeyboardInterrupt:
 print("\n\n Test execution interrupted by user")
 runner.teardown()
 sys.exit(1)
 
 except Exception as e:
 print(f"\n Test execution failed: {e}")
 runner.teardown()
 sys.exit(1)


    if __name__ == '__main__':
 main()