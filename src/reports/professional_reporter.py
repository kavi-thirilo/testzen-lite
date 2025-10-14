#!/usr/bin/env python3
"""
Professional HTML Reporter for TestZen Framework
Generates beautiful, detailed HTML reports with screenshots and metrics
"""

import os
import json
import time
from datetime import datetime, timezone, timedelta
from pathlib import Path
from typing import List, Dict, Any, Optional


class ProfessionalReporter:
    """Generate professional HTML test reports with screenshots and detailed metrics"""

    def __init__(self, excel_file_name: str = "test"):
        """Initialize the reporter"""
        self.excel_file_name = excel_file_name
        self.test_name = Path(excel_file_name).stem

        # Use system local timezone
        self.timezone = None  # None means use local timezone

        # Create reports directory
        self.reports_dir = Path("reports")
        self.reports_dir.mkdir(exist_ok=True)

        # Create screenshots directory
        self.screenshot_dir = self.reports_dir / "screenshots"
        self.screenshot_dir.mkdir(exist_ok=True)

        # Test execution tracking
        self.start_time = None
        self.end_time = None
        self.test_steps = []
        self.total_planned_steps = 0

        # Metrics
        self.passed_steps = 0
        self.failed_steps = 0
        self.skipped_steps = 0

    def get_local_time(self, timestamp=None):
        """Get local time in system timezone"""
        if timestamp is None:
            dt = datetime.now()
        else:
            dt = datetime.fromtimestamp(timestamp)

        # Get timezone name
        import time as time_module
        tz_name = time_module.strftime('%Z')

        return dt.strftime(f'%Y-%m-%d %H:%M:%S {tz_name}')

    def start_test_session(self):
        """Mark the start of test execution"""
        self.start_time = time.time()
        self.test_steps = []

    def end_test_session(self):
        """Mark the end of test execution"""
        self.end_time = time.time()

    def set_total_planned_steps(self, total: int):
        """Set total number of planned test steps"""
        self.total_planned_steps = total

    def add_test_step(self, step_number: int, action: str, status: str,
                     message: str = "", screenshot_before: str = None,
                     screenshot_after: str = None, locator: str = None,
                     test_data: str = None, expected_result: str = None,
                     actual_result: str = None, duration: float = 0):
        """Add a test step to the report"""
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
            'timestamp': self.get_local_time()
        }

        self.test_steps.append(step)

        # Update metrics
        if status.upper() == 'PASSED':
            self.passed_steps += 1
        elif status.upper() == 'FAILED':
            self.failed_steps += 1
        elif status.upper() == 'SKIP':
            self.skipped_steps += 1

    def generate_professional_html_report(self) -> str:
        """Generate a beautiful HTML report"""
        execution_time = self.end_time - self.start_time if self.end_time and self.start_time else 0
        total_steps = len(self.test_steps)
        pass_rate = (self.passed_steps / total_steps * 100) if total_steps > 0 else 0

        # Determine overall status
        overall_status = "PASSED" if self.failed_steps == 0 and total_steps > 0 else "FAILED"
        status_color = "#28a745" if overall_status == "PASSED" else "#dc3545"

        html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TestZen Report - {self.test_name}</title>
    <style>
        * {{
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }}

        body {{
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 20px;
            color: #333;
        }}

        .container {{
            max-width: 1400px;
            margin: 0 auto;
            background: white;
            border-radius: 15px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
            overflow: hidden;
        }}

        .header {{
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px 40px;
            text-align: center;
        }}

        .header h1 {{
            font-size: 2.5em;
            margin-bottom: 10px;
            text-shadow: 2px 2px 4px rgba(0,0,0,0.3);
        }}

        .header .test-name {{
            font-size: 1.2em;
            opacity: 0.9;
            margin-top: 10px;
        }}

        .summary {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            padding: 30px 40px;
            background: #f8f9fa;
        }}

        .summary-card {{
            background: white;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            text-align: center;
            transition: transform 0.2s;
        }}

        .summary-card:hover {{
            transform: translateY(-5px);
            box-shadow: 0 5px 20px rgba(0,0,0,0.15);
        }}

        .summary-card .value {{
            font-size: 2.5em;
            font-weight: bold;
            margin: 10px 0;
        }}

        .summary-card .label {{
            color: #666;
            text-transform: uppercase;
            font-size: 0.85em;
            letter-spacing: 1px;
        }}

        .status-badge {{
            display: inline-block;
            padding: 10px 30px;
            border-radius: 25px;
            font-size: 1.2em;
            font-weight: bold;
            background: {status_color};
            color: white;
            margin: 20px 0;
        }}

        .progress-bar {{
            background: #e9ecef;
            height: 30px;
            border-radius: 15px;
            overflow: hidden;
            margin: 20px 40px;
            box-shadow: inset 0 2px 4px rgba(0,0,0,0.1);
        }}

        .progress-fill {{
            height: 100%;
            background: linear-gradient(90deg, #28a745 0%, #20c997 100%);
            width: {pass_rate}%;
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-weight: bold;
            transition: width 1s ease;
        }}

        .steps-container {{
            padding: 20px 40px 40px 40px;
        }}

        .step-card {{
            background: white;
            border-left: 5px solid #ddd;
            margin: 15px 0;
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            transition: all 0.3s;
        }}

        .step-card:hover {{
            box-shadow: 0 5px 15px rgba(0,0,0,0.2);
        }}

        .step-card.pass {{
            border-left-color: #28a745;
        }}

        .step-card.fail {{
            border-left-color: #dc3545;
        }}

        .step-card.skip {{
            border-left-color: #ffc107;
        }}

        .step-header {{
            padding: 20px;
            cursor: pointer;
            background: #f8f9fa;
            display: flex;
            align-items: center;
            gap: 15px;
        }}

        .step-number {{
            background: #667eea;
            color: white;
            width: 40px;
            height: 40px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: bold;
            flex-shrink: 0;
        }}

        .step-info {{
            flex-grow: 1;
        }}

        .step-action {{
            font-weight: bold;
            font-size: 1.1em;
            margin-bottom: 5px;
        }}

        .step-details {{
            color: #666;
            font-size: 0.9em;
        }}

        .step-status {{
            padding: 8px 20px;
            border-radius: 20px;
            font-weight: bold;
            font-size: 0.9em;
            flex-shrink: 0;
        }}

        .step-status.pass {{
            background: #d4edda;
            color: #155724;
        }}

        .step-status.fail {{
            background: #f8d7da;
            color: #721c24;
        }}

        .step-status.skip {{
            background: #fff3cd;
            color: #856404;
        }}

        .step-body {{
            padding: 20px;
            border-top: 1px solid #e9ecef;
            display: none;
        }}

        .step-card.expanded .step-body {{
            display: block;
        }}

        .detail-grid {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 15px;
            margin-bottom: 20px;
        }}

        .detail-item {{
            background: #f8f9fa;
            padding: 15px;
            border-radius: 8px;
        }}

        .detail-label {{
            font-weight: bold;
            color: #667eea;
            margin-bottom: 5px;
            font-size: 0.9em;
        }}

        .detail-value {{
            color: #333;
            word-break: break-word;
        }}

        .screenshots {{
            display: flex;
            gap: 15px;
            margin-top: 20px;
            flex-wrap: wrap;
        }}

        .screenshot-box {{
            text-align: center;
            flex: 0 0 auto;
        }}

        .screenshot-thumbnail {{
            width: 150px;
            height: 200px;
            object-fit: cover;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            cursor: pointer;
            transition: all 0.2s;
            border: 2px solid #e0e0e0;
        }}

        .screenshot-thumbnail:hover {{
            transform: scale(1.05);
            box-shadow: 0 4px 15px rgba(0,0,0,0.2);
            border-color: #667eea;
        }}

        .screenshot-label {{
            margin-top: 8px;
            font-weight: bold;
            color: #667eea;
            font-size: 0.9em;
        }}

        /* Modal for full-size screenshot */
        .modal {{
            display: none;
            position: fixed;
            z-index: 1000;
            left: 0;
            top: 0;
            width: 100%;
            height: 100%;
            background-color: rgba(0,0,0,0.9);
            overflow: auto;
        }}

        .modal-content {{
            margin: auto;
            display: block;
            max-width: 90%;
            max-height: 90%;
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
        }}

        .modal-close {{
            position: absolute;
            top: 20px;
            right: 35px;
            color: #f1f1f1;
            font-size: 40px;
            font-weight: bold;
            cursor: pointer;
        }}

        .modal-close:hover,
        .modal-close:focus {{
            color: #bbb;
        }}

        .footer {{
            background: #f8f9fa;
            padding: 20px;
            text-align: center;
            color: #666;
            border-top: 1px solid #e9ecef;
        }}

        @media (max-width: 768px) {{
            .summary {{
                grid-template-columns: 1fr;
            }}

            .detail-grid {{
                grid-template-columns: 1fr;
            }}

            .screenshots {{
                grid-template-columns: 1fr;
            }}
        }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>TestZen Execution Report</h1>
            <div class="test-name">{self.test_name}</div>
            <div class="status-badge">{overall_status}</div>
        </div>

        <div class="summary">
            <div class="summary-card">
                <div class="label">Total Steps</div>
                <div class="value" style="color: #667eea;">{total_steps}</div>
            </div>
            <div class="summary-card">
                <div class="label">Passed</div>
                <div class="value" style="color: #28a745;">{self.passed_steps}</div>
            </div>
            <div class="summary-card">
                <div class="label">Failed</div>
                <div class="value" style="color: #dc3545;">{self.failed_steps}</div>
            </div>
            <div class="summary-card">
                <div class="label">Skipped</div>
                <div class="value" style="color: #ffc107;">{self.skipped_steps}</div>
            </div>
            <div class="summary-card">
                <div class="label">Pass Rate</div>
                <div class="value" style="color: #20c997;">{pass_rate:.1f}%</div>
            </div>
            <div class="summary-card">
                <div class="label">Duration</div>
                <div class="value" style="color: #6610f2;">{execution_time:.1f}s</div>
            </div>
        </div>

        <div class="progress-bar">
            <div class="progress-fill">{pass_rate:.1f}%</div>
        </div>

        <div class="steps-container">
            <h2 style="margin-bottom: 20px; color: #667eea;">Test Steps Execution</h2>
"""

        # Add each test step
        for step in self.test_steps:
            status_class = step['status'].lower()
            step_html = f"""
            <div class="step-card {status_class}" onclick="this.classList.toggle('expanded')">
                <div class="step-header">
                    <div class="step-number">{step['step_number']}</div>
                    <div class="step-info">
                        <div class="step-action">{step['action']}</div>
                        <div class="step-details">
                            {step['timestamp']} • Duration: {step['duration']:.2f}s
                        </div>
                    </div>
                    <div class="step-status {status_class}">{step['status']}</div>
                </div>
                <div class="step-body">
                    <div class="detail-grid">
"""

            if step.get('locator'):
                step_html += f"""
                        <div class="detail-item">
                            <div class="detail-label">Locator</div>
                            <div class="detail-value">{step['locator']}</div>
                        </div>
"""

            if step.get('test_data'):
                step_html += f"""
                        <div class="detail-item">
                            <div class="detail-label">Test Data</div>
                            <div class="detail-value">{step['test_data']}</div>
                        </div>
"""

            if step.get('expected_result'):
                step_html += f"""
                        <div class="detail-item">
                            <div class="detail-label">Expected Result</div>
                            <div class="detail-value">{step['expected_result']}</div>
                        </div>
"""

            if step.get('actual_result'):
                step_html += f"""
                        <div class="detail-item">
                            <div class="detail-label">Actual Result</div>
                            <div class="detail-value">{step['actual_result']}</div>
                        </div>
"""

            if step.get('message'):
                step_html += f"""
                        <div class="detail-item">
                            <div class="detail-label">Message</div>
                            <div class="detail-value">{step['message']}</div>
                        </div>
"""

            step_html += """
                    </div>
"""

            # Add screenshots if available
            if step.get('screenshot_before') or step.get('screenshot_after'):
                step_html += """
                    <div class="screenshots">
"""

                if step.get('screenshot_before'):
                    # Make path relative by removing reports/ prefix
                    screenshot_path = step['screenshot_before']
                    if screenshot_path and screenshot_path.startswith('reports/'):
                        screenshot_path = screenshot_path.replace('reports/', '', 1)
                    step_html += f"""
                        <div class="screenshot-box">
                            <img class="screenshot-thumbnail" src="{screenshot_path}" alt="Screenshot Before" onclick="openModal(this.src)" title="Click to enlarge">
                            <div class="screenshot-label">Before</div>
                        </div>
"""

                if step.get('screenshot_after'):
                    # Make path relative by removing reports/ prefix
                    screenshot_path = step['screenshot_after']
                    if screenshot_path and screenshot_path.startswith('reports/'):
                        screenshot_path = screenshot_path.replace('reports/', '', 1)
                    step_html += f"""
                        <div class="screenshot-box">
                            <img class="screenshot-thumbnail" src="{screenshot_path}" alt="Screenshot After" onclick="openModal(this.src)" title="Click to enlarge">
                            <div class="screenshot-label">After</div>
                        </div>
"""

                step_html += """
                    </div>
"""

            step_html += """
                </div>
            </div>
"""

            html_content += step_html

        # Close HTML
        html_content += f"""
        </div>

        <div class="footer">
            <p>Generated by TestZen Framework • {self.get_local_time()}</p>
            <p>Execution Time: {execution_time:.2f} seconds</p>
        </div>
    </div>

    <!-- Modal for full-size screenshot -->
    <div id="screenshotModal" class="modal" onclick="closeModal()">
        <span class="modal-close">&times;</span>
        <img class="modal-content" id="modalImage">
    </div>

    <script>
        // Auto-expand failed steps
        document.querySelectorAll('.step-card.fail').forEach(card => {{
            card.classList.add('expanded');
        }});

        // Modal functions for screenshot viewing
        function openModal(src) {{
            event.stopPropagation();
            const modal = document.getElementById('screenshotModal');
            const modalImg = document.getElementById('modalImage');
            modal.style.display = 'block';
            modalImg.src = src;
        }}

        function closeModal() {{
            document.getElementById('screenshotModal').style.display = 'none';
        }}

        // Close modal on ESC key
        document.addEventListener('keydown', function(e) {{
            if (e.key === 'Escape') {{
                closeModal();
            }}
        }});
    </script>
</body>
</html>
"""

        # Save HTML report
        report_path = self.reports_dir / f"{self.test_name}_report.html"
        with open(report_path, 'w', encoding='utf-8') as f:
            f.write(html_content)

        print(f"Professional HTML report generated: {report_path}")
        return str(report_path)

    def save_summary_json(self) -> str:
        """Save test summary as JSON"""
        execution_time = self.end_time - self.start_time if self.end_time and self.start_time else 0
        total_steps = len(self.test_steps)
        pass_rate = (self.passed_steps / total_steps * 100) if total_steps > 0 else 0

        summary = {
            'test_name': self.test_name,
            'excel_file': self.excel_file_name,
            'execution_time': execution_time,
            'start_time': self.get_local_time(self.start_time) if self.start_time else None,
            'end_time': self.get_local_time(self.end_time) if self.end_time else None,
            'total_steps': total_steps,
            'passed_steps': self.passed_steps,
            'failed_steps': self.failed_steps,
            'skipped_steps': self.skipped_steps,
            'pass_rate': pass_rate,
            'overall_status': 'PASSED' if self.failed_steps == 0 and total_steps > 0 else 'FAILED',
            'timezone': 'CST (UTC-6)',
            'steps': self.test_steps
        }

        json_path = self.reports_dir / f"{self.test_name}_summary.json"
        with open(json_path, 'w', encoding='utf-8') as f:
            json.dump(summary, f, indent=2)

        print(f"JSON summary saved: {json_path}")
        return str(json_path)
