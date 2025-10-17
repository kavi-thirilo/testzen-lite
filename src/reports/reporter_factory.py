#!/usr/bin/env python3
"""
Reporter Factory for TestZen Framework
Dynamically creates reporters based on configuration
"""

import json
import os
from pathlib import Path
from typing import Optional
from src.reports.base_reporter import BaseReporter
from src.reports.professional_reporter import ProfessionalReporter
from src.reports.allure_reporter import AllureReporter


class ReporterFactory:
    """Factory class for creating reporter instances based on configuration"""

    CONFIG_FILE = "config/reporting_config.json"
    DEFAULT_REPORTER = "allure"

    @staticmethod
    def load_config() -> dict:
        """Load reporting configuration from JSON file"""
        config_path = Path(ReporterFactory.CONFIG_FILE)

        if not config_path.exists():
            print(f"\n[ReporterFactory] Warning: Config file not found: {config_path}")
            print(f"[ReporterFactory] Using default reporter: {ReporterFactory.DEFAULT_REPORTER}")
            return {
                "default_reporter": ReporterFactory.DEFAULT_REPORTER,
                "reporters": {
                    "allure": {"enabled": True},
                    "html": {"enabled": True}
                }
            }

        try:
            with open(config_path, 'r') as f:
                config = json.load(f)
            return config
        except Exception as e:
            print(f"\n[ReporterFactory] Error loading config: {e}")
            print(f"[ReporterFactory] Using default reporter: {ReporterFactory.DEFAULT_REPORTER}")
            return {
                "default_reporter": ReporterFactory.DEFAULT_REPORTER,
                "reporters": {
                    "allure": {"enabled": True},
                    "html": {"enabled": True}
                }
            }

    @staticmethod
    def create_reporter(excel_file_name: str = "test", reporter_type: Optional[str] = None) -> BaseReporter:
        """
        Create a reporter instance based on configuration or specified type

        Args:
            excel_file_name: Name of the Excel test file
            reporter_type: Optional override for reporter type ('allure' or 'html')
                          If None, uses the default from config

        Returns:
            BaseReporter: An instance of the appropriate reporter
        """
        config = ReporterFactory.load_config()

        # Determine which reporter to use
        if reporter_type is None:
            reporter_type = config.get("default_reporter", ReporterFactory.DEFAULT_REPORTER)

        # Validate reporter type
        reporters_config = config.get("reporters", {})
        if reporter_type not in reporters_config:
            print(f"\n[ReporterFactory] Warning: Unknown reporter type '{reporter_type}'")
            print(f"[ReporterFactory] Falling back to: {ReporterFactory.DEFAULT_REPORTER}")
            reporter_type = ReporterFactory.DEFAULT_REPORTER

        # Check if reporter is enabled
        reporter_config = reporters_config.get(reporter_type, {})
        if not reporter_config.get("enabled", True):
            print(f"\n[ReporterFactory] Warning: Reporter '{reporter_type}' is disabled")
            print(f"[ReporterFactory] Falling back to: html")
            reporter_type = "html"

        # Create the appropriate reporter
        print(f"\n[ReporterFactory] Creating reporter: {reporter_type}")

        if reporter_type == "allure":
            return AllureReporter(excel_file_name=excel_file_name)
        elif reporter_type == "html":
            return ProfessionalReporter(excel_file_name=excel_file_name)
        else:
            # Fallback to HTML reporter
            print(f"\n[ReporterFactory] Unknown reporter type: {reporter_type}")
            print(f"[ReporterFactory] Using HTML reporter as fallback")
            return ProfessionalReporter(excel_file_name=excel_file_name)

    @staticmethod
    def get_available_reporters() -> list:
        """Get list of available reporter types"""
        config = ReporterFactory.load_config()
        reporters = config.get("reporters", {})
        return [name for name, cfg in reporters.items() if cfg.get("enabled", True)]

    @staticmethod
    def get_default_reporter() -> str:
        """Get the default reporter type from configuration"""
        config = ReporterFactory.load_config()
        return config.get("default_reporter", ReporterFactory.DEFAULT_REPORTER)

    @staticmethod
    def print_config_info():
        """Print current reporter configuration"""
        config = ReporterFactory.load_config()
        print("\n" + "=" * 60)
        print("TestZen Reporter Configuration")
        print("=" * 60)
        print(f"Default Reporter: {config.get('default_reporter', 'N/A')}")
        print(f"Configuration File: {ReporterFactory.CONFIG_FILE}")
        print("\nAvailable Reporters:")

        reporters = config.get("reporters", {})
        for name, cfg in reporters.items():
            status = "Enabled" if cfg.get("enabled", True) else "Disabled"
            print(f"  - {name}: {status}")

        print("\nTo change the default reporter:")
        print(f"  Edit {ReporterFactory.CONFIG_FILE} and set 'default_reporter' to 'allure' or 'html'")
        print("=" * 60 + "\n")
