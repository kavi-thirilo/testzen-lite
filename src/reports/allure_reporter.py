#!/usr/bin/env python3
"""
Allure Reporter for TestZen Framework
Generates Allure reports by creating Allure result JSON files directly
"""

import os
import json
import time
import uuid
import subprocess
import shutil
from datetime import datetime
from pathlib import Path
from typing import Optional, List, Dict, Any
from src.reports.base_reporter import BaseReporter


class AllureReporter(BaseReporter):
    """Generate Allure reports by creating Allure result files directly"""

    def __init__(self, excel_file_name: str = "test"):
        """Initialize the Allure reporter"""
        self.excel_file_name = excel_file_name
        self.test_name = Path(excel_file_name).stem

        # Create reports directory
        self.reports_dir = Path("reports")
        self.reports_dir.mkdir(exist_ok=True)

        # Create Allure results directory
        self.allure_results_dir = self.reports_dir / "allure-results"
        self.allure_results_dir.mkdir(exist_ok=True)

        # Create Allure report directory
        self.allure_report_dir = self.reports_dir / "allure-report"
        self.allure_report_dir.mkdir(exist_ok=True)

        # Create screenshots directory
        self._screenshot_dir = self.reports_dir / "screenshots"
        self._screenshot_dir.mkdir(exist_ok=True)

        # Test execution tracking
        self.start_time = None
        self.end_time = None
        self.test_steps = []
        self.total_planned_steps = 0

        # Metrics
        self.passed_steps = 0
        self.failed_steps = 0
        self.skipped_steps = 0

        # Allure specific
        self.test_uuid = str(uuid.uuid4())
        self.container_uuid = str(uuid.uuid4())
        self.allure_steps = []
        self.allure_attachments = []

    @property
    def screenshot_dir(self):
        """Return the screenshot directory path"""
        return self._screenshot_dir

    def start_test_session(self):
        """Mark the start of test execution"""
        # Clean up old results
        self._cleanup_old_results()

        self.start_time = int(time.time() * 1000)  # milliseconds
        self.test_steps = []
        self.allure_steps = []
        self.allure_attachments = []

    def _cleanup_old_results(self):
        """Clean up old Allure result files before starting new test"""
        try:
            # Clean allure-results directory
            if self.allure_results_dir.exists():
                for file in self.allure_results_dir.glob('*'):
                    if file.is_file():
                        file.unlink()
                print(f"[Allure] Cleaned up old result files from: {self.allure_results_dir}")

            # Clean allure-report directory
            if self.allure_report_dir.exists():
                for item in self.allure_report_dir.glob('*'):
                    if item.is_file():
                        item.unlink()
                    elif item.is_dir():
                        shutil.rmtree(item)
                print(f"[Allure] Cleaned up old report files from: {self.allure_report_dir}")

            # Clean old summary JSON for this test
            old_summary = self.reports_dir / f"{self.test_name}_allure_summary.json"
            if old_summary.exists():
                old_summary.unlink()
                print(f"[Allure] Removed old summary: {old_summary.name}")

        except Exception as e:
            print(f"[Allure] Warning: Failed to clean up old files: {e}")

    def end_test_session(self):
        """Mark the end of test execution"""
        self.end_time = int(time.time() * 1000)  # milliseconds
        self._write_allure_results()

    def set_total_planned_steps(self, total: int):
        """Set total number of planned test steps"""
        self.total_planned_steps = total

    def add_test_step(self, step_number: int, action: str, status: str,
                     message: str = "", screenshot_before: str = None,
                     screenshot_after: str = None, locator: str = None,
                     test_data: str = None, expected_result: str = None,
                     actual_result: str = None, duration: float = 0):
        """Add a test step to the report"""
        step_start = int(time.time() * 1000)
        step_stop = step_start + int(duration * 1000)

        step = {
            'step_number': step_number,
            'action': action,
            'status': status.upper(),
            'message': message,
            'screenshot_before': screenshot_before,
            'screenshot_after': screenshot_after,
            'locator': locator,
            'test_data': test_data,
            'expected_result': expected_result,
            'actual_result': actual_result,
            'duration': duration,
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        }

        self.test_steps.append(step)

        # Update metrics
        if status.upper() == 'PASSED':
            self.passed_steps += 1
            allure_status = 'passed'
        elif status.upper() == 'FAILED':
            self.failed_steps += 1
            allure_status = 'failed'
        elif status.upper() == 'SKIP':
            self.skipped_steps += 1
            allure_status = 'skipped'
        else:
            allure_status = 'unknown'

        # Create Allure step
        step_uuid = str(uuid.uuid4())
        allure_step = {
            'name': f'Step {step_number}: {action}',
            'status': allure_status,
            'statusDetails': {
                'message': message
            } if message else {},
            'stage': 'finished',
            'start': step_start,
            'stop': step_stop,
            'steps': [],
            'attachments': [],
            'parameters': []
        }

        # Add parameters if available
        if locator:
            allure_step['parameters'].append({
                'name': 'Locator',
                'value': locator
            })
        if test_data:
            allure_step['parameters'].append({
                'name': 'Test Data',
                'value': str(test_data)
            })
        if expected_result:
            allure_step['parameters'].append({
                'name': 'Expected',
                'value': str(expected_result)
            })
        if actual_result:
            allure_step['parameters'].append({
                'name': 'Actual',
                'value': str(actual_result)
            })

        # Attach screenshots
        if screenshot_before and os.path.exists(screenshot_before):
            attachment_name = self._create_attachment(screenshot_before, f'Step {step_number} - Before')
            if attachment_name:
                allure_step['attachments'].append({
                    'name': f'Step {step_number} - Before',
                    'source': attachment_name,
                    'type': 'image/png'
                })

        if screenshot_after and os.path.exists(screenshot_after):
            attachment_name = self._create_attachment(screenshot_after, f'Step {step_number} - After')
            if attachment_name:
                allure_step['attachments'].append({
                    'name': f'Step {step_number} - After',
                    'source': attachment_name,
                    'type': 'image/png'
                })

        self.allure_steps.append(allure_step)

    def _create_attachment(self, source_path: str, name: str) -> Optional[str]:
        """Copy attachment to allure-results and return the filename"""
        try:
            source = Path(source_path)
            if not source.exists():
                return None

            # Generate unique filename
            ext = source.suffix
            attachment_filename = f'{uuid.uuid4()}-attachment{ext}'
            dest = self.allure_results_dir / attachment_filename

            # Copy file
            shutil.copy2(source, dest)
            return attachment_filename
        except Exception as e:
            print(f"[Allure] Warning: Failed to copy attachment {source_path}: {e}")
            return None

    def _write_allure_results(self):
        """Write Allure result JSON files"""
        # Determine overall test status
        if self.failed_steps > 0:
            test_status = 'failed'
        elif self.skipped_steps > 0 and self.passed_steps == 0:
            test_status = 'skipped'
        elif len(self.test_steps) > 0:
            test_status = 'passed'
        else:
            test_status = 'unknown'

        # Create test result
        test_result = {
            'uuid': self.test_uuid,
            'historyId': self.test_name,
            'testCaseId': self.test_name,
            'fullName': f'{self.test_name}',
            'labels': [
                {'name': 'suite', 'value': 'TestZen'},
                {'name': 'feature', 'value': self.test_name},
                {'name': 'framework', 'value': 'TestZen'},
                {'name': 'language', 'value': 'python'}
            ],
            'links': [],
            'name': self.test_name,
            'status': test_status,
            'statusDetails': {
                'message': f'Test completed with {self.passed_steps} passed, {self.failed_steps} failed, {self.skipped_steps} skipped',
                'trace': ''
            },
            'stage': 'finished',
            'description': f'Automated test from Excel: {self.excel_file_name}',
            'steps': self.allure_steps,
            'attachments': [],
            'parameters': [
                {'name': 'Excel File', 'value': self.excel_file_name},
                {'name': 'Total Steps', 'value': str(len(self.test_steps))},
                {'name': 'Passed', 'value': str(self.passed_steps)},
                {'name': 'Failed', 'value': str(self.failed_steps)},
                {'name': 'Skipped', 'value': str(self.skipped_steps)}
            ],
            'start': self.start_time,
            'stop': self.end_time
        }

        # Write test result file
        result_file = self.allure_results_dir / f'{self.test_uuid}-result.json'
        with open(result_file, 'w') as f:
            json.dump(test_result, f, indent=2)

        # Create test container
        container = {
            'uuid': self.container_uuid,
            'name': f'TestZen Suite: {self.test_name}',
            'children': [self.test_uuid],
            'befores': [],
            'afters': [],
            'start': self.start_time,
            'stop': self.end_time
        }

        # Write container file
        container_file = self.allure_results_dir / f'{self.container_uuid}-container.json'
        with open(container_file, 'w') as f:
            json.dump(container, f, indent=2)

        print(f"\n[Allure] Results written to: {self.allure_results_dir}")
        print(f"[Allure] Test UUID: {self.test_uuid}")
        print(f"[Allure] Total files created: {len(list(self.allure_results_dir.glob('*')))} files")

    def generate_professional_html_report(self) -> str:
        """Generate Allure HTML report"""
        try:
            # Check if there are result files
            result_files = list(self.allure_results_dir.glob('*.json'))
            if not result_files:
                print(f"\n[Allure] Warning: No result files found in {self.allure_results_dir}")
                return str(self.allure_results_dir)

            print(f"\n[Allure] Found {len(result_files)} result files")
            print(f"[Allure] Generating HTML report...")

            # Generate Allure report using allure command
            cmd = [
                'allure', 'generate',
                str(self.allure_results_dir),
                '-o', str(self.allure_report_dir),
                '--clean'
            ]

            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=60
            )

            if result.returncode == 0:
                report_path = self.allure_report_dir / "index.html"
                print(f"\n[Allure] Report generated successfully!")
                print(f"[Allure] Report location: {report_path}")
                print(f"[Allure] To view the report:")
                print(f"[Allure]   Option 1: open {report_path}")
                print(f"[Allure]   Option 2: allure open {self.allure_report_dir}")
                return str(report_path)
            else:
                print(f"\n[Allure] Warning: Failed to generate Allure report")
                print(f"[Allure] Error: {result.stderr}")
                print(f"[Allure] Make sure Allure CLI is installed:")
                print(f"[Allure]   - macOS: brew install allure")
                print(f"[Allure]   - npm: npm install -g allure-commandline")
                print(f"[Allure] Results saved to: {self.allure_results_dir}")
                return str(self.allure_results_dir)

        except FileNotFoundError:
            print(f"\n[Allure] Warning: Allure CLI not found")
            print(f"[Allure] Install Allure CLI to generate HTML reports:")
            print(f"[Allure]   - macOS: brew install allure")
            print(f"[Allure]   - npm: npm install -g allure-commandline")
            print(f"[Allure] Raw results saved to: {self.allure_results_dir}")
            print(f"[Allure] You can generate the report later with:")
            print(f"[Allure]   allure generate {self.allure_results_dir} -o {self.allure_report_dir} --clean")
            return str(self.allure_results_dir)
        except Exception as e:
            print(f"\n[Allure] Error generating report: {e}")
            print(f"[Allure] Results saved to: {self.allure_results_dir}")
            return str(self.allure_results_dir)

    def save_summary_json(self) -> str:
        """Save test execution summary as JSON"""
        execution_time = (self.end_time - self.start_time) / 1000 if self.end_time and self.start_time else 0
        total_steps = len(self.test_steps)
        pass_rate = (self.passed_steps / total_steps * 100) if total_steps > 0 else 0

        summary = {
            "test_name": self.test_name,
            "excel_file": self.excel_file_name,
            "start_time": datetime.fromtimestamp(self.start_time / 1000).strftime('%Y-%m-%d %H:%M:%S') if self.start_time else None,
            "end_time": datetime.fromtimestamp(self.end_time / 1000).strftime('%Y-%m-%d %H:%M:%S') if self.end_time else None,
            "execution_time_seconds": round(execution_time, 2),
            "total_steps": total_steps,
            "planned_steps": self.total_planned_steps,
            "passed_steps": self.passed_steps,
            "failed_steps": self.failed_steps,
            "skipped_steps": self.skipped_steps,
            "pass_rate": round(pass_rate, 2),
            "overall_status": "PASSED" if self.failed_steps == 0 and total_steps > 0 else "FAILED",
            "reporter_type": "allure",
            "allure_results_dir": str(self.allure_results_dir),
            "allure_report_dir": str(self.allure_report_dir),
            "test_uuid": self.test_uuid
        }

        # Save summary JSON
        summary_path = self.reports_dir / f"{self.test_name}_allure_summary.json"
        with open(summary_path, 'w') as f:
            json.dump(summary, f, indent=2)

        print(f"[Allure] Summary JSON saved to: {summary_path}")
        return str(summary_path)
