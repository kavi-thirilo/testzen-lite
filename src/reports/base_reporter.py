#!/usr/bin/env python3
"""
Base Reporter Interface for TestZen Framework
Defines the contract that all reporters must follow
"""

from abc import ABC, abstractmethod
from typing import Optional


class BaseReporter(ABC):
    """Abstract base class for all TestZen reporters"""

    @abstractmethod
    def __init__(self, excel_file_name: str = "test"):
        """Initialize the reporter"""
        pass

    @abstractmethod
    def start_test_session(self):
        """Mark the start of test execution"""
        pass

    @abstractmethod
    def end_test_session(self):
        """Mark the end of test execution"""
        pass

    @abstractmethod
    def set_total_planned_steps(self, total: int):
        """Set total number of planned test steps"""
        pass

    @abstractmethod
    def add_test_step(self, step_number: int, action: str, status: str,
                     message: str = "", screenshot_before: str = None,
                     screenshot_after: str = None, locator: str = None,
                     test_data: str = None, expected_result: str = None,
                     actual_result: str = None, duration: float = 0):
        """Add a test step to the report"""
        pass

    @abstractmethod
    def generate_professional_html_report(self) -> str:
        """Generate the report and return the path to the report file"""
        pass

    @abstractmethod
    def save_summary_json(self) -> str:
        """Save summary as JSON and return the path to the JSON file"""
        pass
