#!/usr/bin/env python3
"""
TestZen Automation Framework - Command Line Interface
Professional mobile app automation testing framework with integrated TestZen Inspector
"""

import argparse
import os
import sys
import glob
import json
from pathlib import Path

# Add src directory to Python path
current_dir = Path(__file__).parent
src_dir = current_dir / "src"
sys.path.insert(0, str(src_dir))

from src.automation.testzen_automation import TestZenAutomation
from src.utils.color_logger import ColorLogger

class TestZenCLI:
    """Command-line interface for TestZen Automation Framework"""
    
    def __init__(self):
        self.framework_dir = Path(__file__).parent
        self.tests_dir = self.framework_dir / "tests"
        self.version_config_path = self.framework_dir / "config/version.json"
    
    def list_tests(self, platform=None):
        """List all available test files"""
        pattern = "**/*.xlsx"
        # Platform filtering is optional - if no platform-specific files found, return all
        if platform:
            # First try platform-specific path
            platform_pattern = f"{platform.lower()}/**/*.xlsx"
            test_files = []
            if self.tests_dir.exists():
                test_files.extend(glob.glob(str(self.tests_dir / platform_pattern), recursive=True))

            # If no platform-specific files found, return all files with a note
            if not test_files:
                print(f"NOTE: No {platform}-specific test files found. Showing all available tests.")
                if self.tests_dir.exists():
                    test_files.extend(glob.glob(str(self.tests_dir / pattern), recursive=True))
        else:
            test_files = []
            if self.tests_dir.exists():
                test_files.extend(glob.glob(str(self.tests_dir / pattern), recursive=True))

        return sorted(test_files)
    
    def run_single_test(self, test_file, config=None):
        """Run a single test file"""
        if not os.path.exists(test_file):
            print(f"ERROR: Test file not found: {test_file}")
            return False

        # Validate file extension
        if not test_file.endswith('.xlsx'):
            print(f"ERROR: Invalid file format. Expected .xlsx file, got: {test_file}")
            print(f"       Please provide an Excel file (.xlsx)")
            return False

        print(f"RUNNING: TestZen Automation: {os.path.basename(test_file)}")
        print("=" * 60)
        
        # Display config options if any are set
        if config:
            if config.get('no_screenshots'):
                print("CONFIG: Screenshots disabled")
            if config.get('skip_on_fail'):
                print("CONFIG: Skip-on-fail enabled")
        
        automation = TestZenAutomation(test_file, config=config)
        return automation.run_test()
    
    def run_all_tests(self, platform=None, config=None):
        """Run all tests for a platform or all platforms"""
        test_files = self.list_tests(platform)

        if not test_files:
            platform_msg = f" for platform '{platform}'" if platform else ""
            print(f"ERROR: No test files found{platform_msg}")
            return False
            
        print(f"TESTZEN: Framework - Running {len(test_files)} test(s)")
        if platform:
            print(f"PLATFORM: {platform.upper()}")
        print("=" * 60)
        
        results = []
        for test_file in test_files:
            print(f"\nSTARTING: {os.path.basename(test_file)}")
            result = self.run_single_test(test_file, config)
            results.append((test_file, result))
            
        # Summary
        print("\n" + "=" * 60)
        print("TESTZEN: Execution Summary")
        print("=" * 60)
        
        passed = sum(1 for _, result in results if result)
        total = len(results)
        
        for test_file, result in results:
            status = "PASSED" if result else "FAILED"
            print(f"{status} - {os.path.basename(test_file)}")
            
        print(f"\nRESULTS: {passed}/{total} tests passed")
        return passed == total
    
    def show_inspector_info(self):
        """Show TestZen Inspector information"""
        print("TESTZEN: Inspector")
        print("=" * 60)
        print("Professional web-based element inspector for mobile apps")
        print()
        print("LOCATION: appium-web-inspector/")
        print("URL: http://localhost:3000")
        print()
        print("QUICK START:")
        print("  cd appium-web-inspector")
        print("  ./scripts/startup/START_INSPECTOR.command")
        print()
        print("FEATURES:")
        print("  • Real-time element inspection")
        print("  • Multiple locator strategies")
        print("  • Code generation (Python, JS, Java)")
        print("  • Excel-ready locator formats")
        print("  • Built-in server management")
    
    def get_version_info(self):
        """Get version information from config"""
        if self.version_config_path.exists():
            try:
                with open(self.version_config_path, 'r') as f:
                    config = json.load(f)
                    return config.get("current_version", "1.1.1")
            except:
                return "1.1.1"
        return "1.1.1"

def main():
    parser = argparse.ArgumentParser(
        description="TestZen Automation Framework - Professional Mobile App Testing",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  testzen run --file tests/login.xlsx                     # Run with auto-launch (default)
  testzen run --file tests/login.xlsx --auto-appium       # Auto-start Appium server
  testzen run --file tests/login.xlsx --auto-appium --keep-appium  # Keep Appium running after
  testzen run --file tests/login.xlsx --no-auto-launch    # Disable auto-launch
  testzen run --file tests/login.xlsx --device emulator-5554  # Use specific device
  testzen run --file tests/login.xlsx --avd Pixel_4_API_30    # Prefer specific AVD
  testzen run --file tests/login.xlsx --screenshots no    # Run without screenshots
  testzen run --file tests/login.xlsx --no-cleanup        # Skip all cleanup operations
  testzen run --file tests/login.xlsx --no-cleanup-cache  # Skip app cache cleanup
  testzen run --all                                       # Run all tests
  testzen run --all --platform android                   # Run all Android tests
  testzen run --file tests/login.xlsx --skip-on-fail     # Continue on failures
  testzen list                                           # List available tests
  testzen list --platform ios                           # List iOS tests
  testzen emulator list                                  # List AVDs and emulators
  testzen emulator launch                                # Launch default emulator
  testzen emulator launch --avd Pixel_4                  # Launch specific AVD
  testzen emulator stop                                  # Stop running emulator
  testzen inspector                                      # Show inspector info
        """
    )
    
    subparsers = parser.add_subparsers(dest='command', help='Available commands')
    
    # Run command
    run_parser = subparsers.add_parser('run', help='Run tests')
    run_group = run_parser.add_mutually_exclusive_group(required=True)
    run_group.add_argument('--all', action='store_true', help='Run all tests')
    run_group.add_argument('--file', type=str, help='Run specific test file')
    run_parser.add_argument('--platform', type=str, choices=['android', 'ios'], 
                           help='Filter by platform (android/ios)')
    run_parser.add_argument('--screenshots', type=str, choices=['yes', 'no'], default='yes',
                           help='Enable or disable screenshot capture (default: yes)')
    run_parser.add_argument('--skip-on-fail', action='store_true',
                           help='Skip remaining steps when a step fails (instead of stopping)')
    run_parser.add_argument('--no-auto-launch', action='store_true',
                           help='Disable automatic emulator launch when no device is available')
    run_parser.add_argument('--device', type=str,
                           help='Specify device ID or name to use')
    run_parser.add_argument('--avd', type=str,
                           help='Preferred AVD name to launch if no devices available')
    run_parser.add_argument('--no-cleanup', action='store_true',
                           help='Disable all cleanup operations after test completion')
    run_parser.add_argument('--no-cleanup-notifications', action='store_true',
                           help='Skip clearing device notifications during cleanup')
    run_parser.add_argument('--no-cleanup-cache', action='store_true',
                           help='Skip clearing app cache during cleanup')
    run_parser.add_argument('--no-cleanup-temp', action='store_true',
                           help='Skip removing temporary files during cleanup')
    run_parser.add_argument('--auto-appium', action='store_true',
                           help='Automatically start Appium server if not running')
    run_parser.add_argument('--keep-appium', action='store_true',
                           help='Keep Appium server running after tests (only with --auto-appium)')

    # List command
    list_parser = subparsers.add_parser('list', help='List available tests')
    list_parser.add_argument('--platform', type=str, choices=['android', 'ios'],
                            help='Filter by platform (android/ios)')
    
    # Inspector command
    subparsers.add_parser('inspector', help='Show TestZen Inspector information')

    # Version command
    subparsers.add_parser('version', help='Show version information')

    # Emulator command
    emulator_parser = subparsers.add_parser('emulator', help='Manage Android emulators')
    emulator_subparsers = emulator_parser.add_subparsers(dest='emulator_action', help='Emulator actions')
    emulator_subparsers.add_parser('list', help='List available AVDs and running emulators')
    emulator_launch = emulator_subparsers.add_parser('launch', help='Launch an emulator')
    emulator_launch.add_argument('--avd', type=str, help='AVD name to launch (auto-selects if not specified)')
    emulator_subparsers.add_parser('stop', help='Stop running emulators')
    
    if len(sys.argv) == 1:
        parser.print_help()
        return
        
    args = parser.parse_args()
    cli = TestZenCLI()
    
    try:
        if args.command == 'run':
            # Prepare config from command line arguments
            config = {}
            if hasattr(args, 'screenshots') and args.screenshots == 'no':
                config['no_screenshots'] = True
            if hasattr(args, 'skip_on_fail') and args.skip_on_fail:
                config['skip_on_fail'] = True
            if hasattr(args, 'no_auto_launch') and args.no_auto_launch:
                config['auto_launch_emulator'] = False
            if hasattr(args, 'device') and args.device:
                config['device_name'] = args.device
            if hasattr(args, 'avd') and args.avd:
                config['preferred_avd'] = args.avd

            # Appium server configuration
            if hasattr(args, 'auto_appium') and args.auto_appium:
                config['auto_appium'] = True
            if hasattr(args, 'keep_appium') and args.keep_appium:
                config['keep_appium'] = True

            # Cleanup configuration
            if hasattr(args, 'no_cleanup') and args.no_cleanup:
                config['cleanup_notifications'] = False
                config['cleanup_app_cache'] = False
                config['cleanup_temp_files'] = False
                config['reset_device_settings'] = False
            else:
                # Individual cleanup controls
                if hasattr(args, 'no_cleanup_notifications') and args.no_cleanup_notifications:
                    config['cleanup_notifications'] = False
                if hasattr(args, 'no_cleanup_cache') and args.no_cleanup_cache:
                    config['cleanup_app_cache'] = False
                if hasattr(args, 'no_cleanup_temp') and args.no_cleanup_temp:
                    config['cleanup_temp_files'] = False

            if args.all:
                success = cli.run_all_tests(args.platform, config)
                sys.exit(0 if success else 1)
            elif args.file:
                success = cli.run_single_test(args.file, config)
                sys.exit(0 if success else 1)
                
        elif args.command == 'list':
            tests = cli.list_tests(args.platform)
            if tests:
                platform_msg = f" ({args.platform.upper()})" if args.platform else ""
                print(f"AVAILABLE TESTS{platform_msg}:")
                print("-" * 40)
                for i, test in enumerate(tests, 1):
                    print(f"{i:2d}. {os.path.basename(test)}")
                print(f"\nTOTAL: {len(tests)} test file(s)")
            else:
                platform_msg = f" for platform '{args.platform}'" if args.platform else ""
                print(f"ERROR: No test files found{platform_msg}")
                
        elif args.command == 'inspector':
            cli.show_inspector_info()

        elif args.command == 'emulator':
            from src.utils.emulator_manager import EmulatorManager
            emulator_mgr = EmulatorManager()

            if args.emulator_action == 'list':
                emulator_mgr.list_emulator_status()
            elif args.emulator_action == 'launch':
                avd_name = args.avd if hasattr(args, 'avd') else None
                success = emulator_mgr.launch_emulator(avd_name)
                if success:
                    print("[TZ] Emulator launched successfully")
                else:
                    print("[TZ] Failed to launch emulator")
                    sys.exit(1)
            elif args.emulator_action == 'stop':
                success = emulator_mgr.stop_emulator()
                if success:
                    print("[TZ] Emulator stopped successfully")
                else:
                    print("[TZ] Failed to stop emulator")
                    sys.exit(1)
            else:
                print("ERROR: Please specify an emulator action (list, launch, or stop)")
                sys.exit(1)

        elif args.command == 'version':
            version = cli.get_version_info()
            print(f"[TZ] TestZen Automation Framework v{version}")
            print("White-Label Mobile App Testing Platform")
            print("=" * 50)
            print("Multi-platform support (Android/iOS)")
            print("Excel-driven test automation")
            print("TestZen Inspector included")
            print("Professional HTML/JSON reporting")
            print("App-agnostic architecture")
            print("Smart Element Recovery System")
            print("Dual distribution (Complete/Lite)")
            print("=" * 50)
            print(f"Installation: {cli.framework_dir}")
            if cli.version_config_path.exists():
                config_data = json.load(open(cli.version_config_path))
                release_notes = config_data.get("release_notes", "")
                if release_notes:
                    print(f"Release notes: {release_notes}")
            
    except KeyboardInterrupt:
        print("\n\nWARNING: TestZen execution interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"ERROR: TestZen Error: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main()