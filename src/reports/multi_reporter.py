#!/usr/bin/env python3
"""
Multi Reporter for TestZen Framework
Wraps multiple reporters to generate multiple report formats simultaneously
"""

from typing import List
from pathlib import Path
from src.reports.base_reporter import BaseReporter


class MultiReporter(BaseReporter):
    """Wrapper that delegates to multiple reporters simultaneously"""

    def __init__(self, reporters: List[BaseReporter]):
        """
        Initialize the multi-reporter with a list of reporters

        Args:
            reporters: List of reporter instances to use
        """
        if not reporters:
            raise ValueError("MultiReporter requires at least one reporter")

        self.reporters = reporters
        self.primary_reporter = reporters[0]  # Use first reporter for property access

        # Initialize state attributes that may be accessed externally
        self.start_time = None
        self.end_time = None
        self.test_steps = []
        self.total_planned_steps = 0
        self.passed_steps = 0
        self.failed_steps = 0
        self.skipped_steps = 0

        print(f"\n[MultiReporter] Initialized with {len(reporters)} reporters:")
        for reporter in reporters:
            print(f"  - {type(reporter).__name__}")

    @property
    def screenshot_dir(self):
        """Return the screenshot directory from primary reporter"""
        return self.primary_reporter.screenshot_dir

    def start_test_session(self):
        """Start test session on all reporters"""
        import time
        self.start_time = time.time()
        self.test_steps = []

        for reporter in self.reporters:
            try:
                reporter.start_test_session()
            except Exception as e:
                print(f"[MultiReporter] Error starting session on {type(reporter).__name__}: {e}")

    def end_test_session(self):
        """End test session on all reporters"""
        import time
        self.end_time = time.time()

        for reporter in self.reporters:
            try:
                reporter.end_test_session()
            except Exception as e:
                print(f"[MultiReporter] Error ending session on {type(reporter).__name__}: {e}")

    def set_total_planned_steps(self, total: int):
        """Set total planned steps on all reporters"""
        self.total_planned_steps = total

        for reporter in self.reporters:
            try:
                reporter.set_total_planned_steps(total)
            except Exception as e:
                print(f"[MultiReporter] Error setting planned steps on {type(reporter).__name__}: {e}")

    def add_test_step(self, step_number: int, action: str, status: str,
                     message: str = "", screenshot_before: str = None,
                     screenshot_after: str = None, locator: str = None,
                     test_data: str = None, expected_result: str = None,
                     actual_result: str = None, duration: float = 0):
        """Add test step to all reporters"""
        # Track step in MultiReporter
        step = {
            'step_number': step_number,
            'action': action,
            'status': status.upper(),
            'message': message,
            'duration': duration
        }
        self.test_steps.append(step)

        # Update metrics
        if status.upper() == 'PASSED':
            self.passed_steps += 1
        elif status.upper() == 'FAILED':
            self.failed_steps += 1
        elif status.upper() == 'SKIP':
            self.skipped_steps += 1

        # Delegate to all reporters
        for reporter in self.reporters:
            try:
                reporter.add_test_step(
                    step_number=step_number,
                    action=action,
                    status=status,
                    message=message,
                    screenshot_before=screenshot_before,
                    screenshot_after=screenshot_after,
                    locator=locator,
                    test_data=test_data,
                    expected_result=expected_result,
                    actual_result=actual_result,
                    duration=duration
                )
            except Exception as e:
                print(f"[MultiReporter] Error adding step to {type(reporter).__name__}: {e}")

    def generate_professional_html_report(self) -> str:
        """Generate reports on all reporters and return primary reporter's path"""
        paths = []
        for reporter in self.reporters:
            try:
                path = reporter.generate_professional_html_report()
                paths.append(path)
            except Exception as e:
                print(f"[MultiReporter] Error generating report on {type(reporter).__name__}: {e}")

        # Return primary reporter's path
        return paths[0] if paths else ""

    def save_summary_json(self) -> str:
        """Save summary JSON on all reporters and return primary reporter's path"""
        paths = []
        for reporter in self.reporters:
            try:
                path = reporter.save_summary_json()
                paths.append(path)
            except Exception as e:
                print(f"[MultiReporter] Error saving summary on {type(reporter).__name__}: {e}")

        # Return primary reporter's path
        return paths[0] if paths else ""
