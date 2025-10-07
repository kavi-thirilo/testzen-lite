#!/usr/bin/env python3
"""
TestZen CI/CD Pipeline Report Generator
Consolidates test results from multiple jobs into a single HTML report
"""

import os
import json
import glob
from pathlib import Path
from datetime import datetime
import sys

class PipelineReportGenerator:
    """Generate consolidated HTML report from pipeline test results"""

    def __init__(self, reports_dir="reports", output_dir="reports/consolidated"):
        self.reports_dir = Path(reports_dir)
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)

        # CI environment variables
        self.ci_pipeline_id = os.getenv('CI_PIPELINE_ID', 'local')
        self.ci_commit_sha = os.getenv('CI_COMMIT_SHORT_SHA', 'unknown')
        self.ci_commit_branch = os.getenv('CI_COMMIT_REF_NAME', 'unknown')
        self.ci_pipeline_url = os.getenv('CI_PIPELINE_URL', '#')
        self.ci_project_name = os.getenv('CI_PROJECT_NAME', 'TestZen')

    def collect_test_results(self):
        """Collect all test results from job artifacts"""
        results = {
            'android': [],
            'ios': [],
            'summary': {
                'total_tests': 0,
                'passed_tests': 0,
                'failed_tests': 0,
                'total_steps': 0,
                'passed_steps': 0,
                'failed_steps': 0,
                'duration': 0
            }
        }

        # Find all JSON report files
        json_files = glob.glob(str(self.reports_dir / "**" / "*.json"), recursive=True)

        for json_file in json_files:
            try:
                with open(json_file, 'r') as f:
                    data = json.load(f)

                # Determine platform from path or content
                platform = 'android' if 'android' in json_file.lower() else 'ios'

                # Extract test information
                test_info = {
                    'test_file': data.get('test_file', os.path.basename(json_file)),
                    'status': data.get('status', 'unknown'),
                    'total_steps': data.get('total_steps', 0),
                    'passed_steps': data.get('passed_steps', 0),
                    'failed_steps': data.get('failed_steps', 0),
                    'duration': data.get('duration', 0),
                    'steps': data.get('steps', []),
                    'device_info': data.get('device_info', {}),
                    'screenshots': self._find_screenshots(json_file)
                }

                results[platform].append(test_info)

                # Update summary
                results['summary']['total_tests'] += 1
                if test_info['status'] == 'passed':
                    results['summary']['passed_tests'] += 1
                else:
                    results['summary']['failed_tests'] += 1

                results['summary']['total_steps'] += test_info['total_steps']
                results['summary']['passed_steps'] += test_info['passed_steps']
                results['summary']['failed_steps'] += test_info['failed_steps']
                results['summary']['duration'] += test_info['duration']

            except Exception as e:
                print(f"[Warning] Failed to process {json_file}: {e}")
                continue

        return results

    def _find_screenshots(self, json_file):
        """Find screenshots associated with a test report"""
        screenshots = []
        base_dir = Path(json_file).parent
        screenshot_dir = base_dir / "screenshots"

        if screenshot_dir.exists():
            screenshots = [str(p.relative_to(self.reports_dir))
                          for p in screenshot_dir.glob("*.png")]

        return screenshots

    def generate_html_report(self, results):
        """Generate consolidated HTML report"""
        html = self._generate_html_header()
        html += self._generate_summary_section(results['summary'])
        html += self._generate_platform_section("Android", results['android'])
        html += self._generate_platform_section("iOS", results['ios'])
        html += self._generate_html_footer()

        # Write main index.html
        output_file = self.reports_dir / "index.html"
        with open(output_file, 'w') as f:
            f.write(html)

        print(f"[TestZen] Consolidated report generated: {output_file}")

        # Also save to consolidated directory
        consolidated_file = self.output_dir / "index.html"
        with open(consolidated_file, 'w') as f:
            f.write(html)

        # Save JSON summary
        json_file = self.output_dir / "summary.json"
        with open(json_file, 'w') as f:
            json.dump(results, f, indent=2)

        return output_file

    def _generate_html_header(self):
        """Generate HTML header with styles"""
        return f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TestZen Pipeline Report - Build #{self.ci_pipeline_id}</title>
    <style>
        * {{
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }}

        body {{
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 20px;
            color: #333;
        }}

        .container {{
            max-width: 1400px;
            margin: 0 auto;
            background: white;
            border-radius: 12px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
            overflow: hidden;
        }}

        .header {{
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            text-align: center;
        }}

        .header h1 {{
            font-size: 2.5em;
            margin-bottom: 10px;
            font-weight: 700;
        }}

        .header .subtitle {{
            font-size: 1.1em;
            opacity: 0.9;
        }}

        .ci-info {{
            background: rgba(255,255,255,0.1);
            padding: 15px;
            margin-top: 20px;
            border-radius: 8px;
            display: flex;
            justify-content: space-around;
            flex-wrap: wrap;
        }}

        .ci-info-item {{
            margin: 5px 15px;
        }}

        .ci-info-label {{
            font-size: 0.85em;
            opacity: 0.8;
            margin-bottom: 3px;
        }}

        .ci-info-value {{
            font-size: 1.1em;
            font-weight: 600;
        }}

        .summary {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            padding: 30px;
            background: #f8f9fa;
        }}

        .summary-card {{
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            text-align: center;
        }}

        .summary-card .label {{
            font-size: 0.9em;
            color: #666;
            margin-bottom: 10px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }}

        .summary-card .value {{
            font-size: 2.5em;
            font-weight: 700;
            color: #333;
        }}

        .summary-card.passed .value {{
            color: #28a745;
        }}

        .summary-card.failed .value {{
            color: #dc3545;
        }}

        .platform-section {{
            padding: 30px;
            border-top: 3px solid #e9ecef;
        }}

        .platform-title {{
            font-size: 1.8em;
            margin-bottom: 20px;
            color: #667eea;
            display: flex;
            align-items: center;
        }}

        .platform-title::before {{
            content: '';
            width: 6px;
            height: 30px;
            background: #667eea;
            margin-right: 15px;
            border-radius: 3px;
        }}

        .test-card {{
            background: white;
            border: 2px solid #e9ecef;
            border-radius: 8px;
            margin-bottom: 20px;
            overflow: hidden;
            transition: all 0.3s ease;
        }}

        .test-card:hover {{
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            transform: translateY(-2px);
        }}

        .test-header {{
            padding: 20px;
            background: #f8f9fa;
            cursor: pointer;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }}

        .test-header.passed {{
            border-left: 5px solid #28a745;
        }}

        .test-header.failed {{
            border-left: 5px solid #dc3545;
        }}

        .test-name {{
            font-size: 1.2em;
            font-weight: 600;
            color: #333;
        }}

        .test-status {{
            display: flex;
            align-items: center;
            gap: 15px;
        }}

        .badge {{
            padding: 6px 12px;
            border-radius: 20px;
            font-size: 0.85em;
            font-weight: 600;
            text-transform: uppercase;
        }}

        .badge.passed {{
            background: #28a745;
            color: white;
        }}

        .badge.failed {{
            background: #dc3545;
            color: white;
        }}

        .test-stats {{
            font-size: 0.9em;
            color: #666;
        }}

        .test-details {{
            padding: 20px;
            display: none;
            background: #fafbfc;
        }}

        .test-details.active {{
            display: block;
        }}

        .step-list {{
            margin-top: 15px;
        }}

        .step-item {{
            background: white;
            padding: 12px 15px;
            margin-bottom: 8px;
            border-radius: 6px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            border-left: 3px solid #e9ecef;
        }}

        .step-item.passed {{
            border-left-color: #28a745;
        }}

        .step-item.failed {{
            border-left-color: #dc3545;
        }}

        .step-info {{
            flex: 1;
        }}

        .step-number {{
            font-weight: 600;
            color: #667eea;
            margin-right: 10px;
        }}

        .step-action {{
            color: #333;
        }}

        .step-duration {{
            color: #666;
            font-size: 0.85em;
            margin-left: 15px;
        }}

        .no-tests {{
            text-align: center;
            padding: 40px;
            color: #999;
            font-style: italic;
        }}

        .footer {{
            background: #f8f9fa;
            padding: 20px;
            text-align: center;
            color: #666;
            font-size: 0.9em;
        }}

        .toggle-icon {{
            transition: transform 0.3s ease;
        }}

        .toggle-icon.active {{
            transform: rotate(180deg);
        }}
    </style>
    <script>
        function toggleTestDetails(testId) {{
            const details = document.getElementById('details-' + testId);
            const icon = document.getElementById('icon-' + testId);

            details.classList.toggle('active');
            icon.classList.toggle('active');
        }}
    </script>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1> TestZen Pipeline Report</h1>
            <p class="subtitle">Automated Mobile Testing - CI/CD Pipeline</p>
            <div class="ci-info">
                <div class="ci-info-item">
                    <div class="ci-info-label">Pipeline ID</div>
                    <div class="ci-info-value">#{self.ci_pipeline_id}</div>
                </div>
                <div class="ci-info-item">
                    <div class="ci-info-label">Branch</div>
                    <div class="ci-info-value">{self.ci_commit_branch}</div>
                </div>
                <div class="ci-info-item">
                    <div class="ci-info-label">Commit</div>
                    <div class="ci-info-value">{self.ci_commit_sha}</div>
                </div>
                <div class="ci-info-item">
                    <div class="ci-info-label">Generated</div>
                    <div class="ci-info-value">{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</div>
                </div>
            </div>
        </div>
"""

    def _generate_summary_section(self, summary):
        """Generate summary statistics section"""
        total = summary['total_tests']
        passed = summary['passed_tests']
        failed = summary['failed_tests']
        success_rate = (passed / total * 100) if total > 0 else 0

        return f"""
        <div class="summary">
            <div class="summary-card">
                <div class="label">Total Tests</div>
                <div class="value">{total}</div>
            </div>
            <div class="summary-card passed">
                <div class="label">Passed</div>
                <div class="value">{passed}</div>
            </div>
            <div class="summary-card failed">
                <div class="label">Failed</div>
                <div class="value">{failed}</div>
            </div>
            <div class="summary-card">
                <div class="label">Success Rate</div>
                <div class="value">{success_rate:.1f}%</div>
            </div>
            <div class="summary-card">
                <div class="label">Total Steps</div>
                <div class="value">{summary['total_steps']}</div>
            </div>
            <div class="summary-card">
                <div class="label">Duration</div>
                <div class="value">{summary['duration']:.1f}s</div>
            </div>
        </div>
"""

    def _generate_platform_section(self, platform_name, tests):
        """Generate platform-specific test results section"""
        if not tests:
            return f"""
        <div class="platform-section">
            <h2 class="platform-title">{platform_name} Tests</h2>
            <div class="no-tests">No {platform_name} tests were executed in this pipeline.</div>
        </div>
"""

        html = f"""
        <div class="platform-section">
            <h2 class="platform-title">{platform_name} Tests</h2>
"""

        for idx, test in enumerate(tests):
            status_class = 'passed' if test['status'] == 'passed' else 'failed'
            test_id = f"{platform_name.lower()}-{idx}"

            html += f"""
            <div class="test-card">
                <div class="test-header {status_class}" onclick="toggleTestDetails('{test_id}')">
                    <div class="test-name">{test['test_file']}</div>
                    <div class="test-status">
                        <span class="test-stats">{test['passed_steps']}/{test['total_steps']} steps • {test['duration']:.1f}s</span>
                        <span class="badge {status_class}">{test['status'].upper()}</span>
                        <span class="toggle-icon" id="icon-{test_id}"></span>
                    </div>
                </div>
                <div class="test-details" id="details-{test_id}">
                    <h3>Test Steps</h3>
                    <div class="step-list">
"""

            for step in test.get('steps', []):
                step_status = step.get('status', 'unknown')
                step_class = 'passed' if step_status == 'passed' else 'failed'

                html += f"""
                        <div class="step-item {step_class}">
                            <div class="step-info">
                                <span class="step-number">Step {step.get('step_no', '')}</span>
                                <span class="step-action">{step.get('action', '')} - {step.get('description', '')}</span>
                                <span class="step-duration">⏱ {step.get('duration', 0):.2f}s</span>
                            </div>
                        </div>
"""

            html += """
                    </div>
                </div>
            </div>
"""

        html += """
        </div>
"""
        return html

    def _generate_html_footer(self):
        """Generate HTML footer"""
        return f"""
        <div class="footer">
            <p>Generated by TestZen Automation Framework • Pipeline #{self.ci_pipeline_id}</p>
            <p><a href="{self.ci_pipeline_url}" target="_blank">View Pipeline in GitLab</a></p>
        </div>
    </div>
</body>
</html>
"""

def main():
    """Main execution"""
    print("[TestZen] Starting pipeline report generation...")

    generator = PipelineReportGenerator()

    # Collect all test results
    print("[TestZen] Collecting test results from artifacts...")
    results = generator.collect_test_results()

    # Generate HTML report
    print("[TestZen] Generating consolidated HTML report...")
    report_file = generator.generate_html_report(results)

    # Print summary
    summary = results['summary']
    print("\n" + "=" * 60)
    print("[TestZen] Pipeline Report Summary")
    print("=" * 60)
    print(f"Total Tests: {summary['total_tests']}")
    print(f"Passed: {summary['passed_tests']}")
    print(f"Failed: {summary['failed_tests']}")
    print(f"Success Rate: {(summary['passed_tests']/summary['total_tests']*100) if summary['total_tests'] > 0 else 0:.1f}%")
    print(f"Total Duration: {summary['duration']:.1f}s")
    print("=" * 60)
    print(f"Report: {report_file}")
    print("=" * 60)

    # Exit with appropriate code
    sys.exit(0 if summary['failed_tests'] == 0 else 1)

if __name__ == "__main__":
    main()
