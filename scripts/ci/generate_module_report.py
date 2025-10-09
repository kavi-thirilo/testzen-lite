#!/usr/bin/env python3
"""
TestZen Multi-Module Report Generator
Generates HTML reports organized by platform and module
"""

import json
import os
import sys
from pathlib import Path
from datetime import datetime
from typing import Dict, List

class ModuleReportGenerator:
    """Generate comprehensive test reports for multi-module projects"""

    def __init__(self, platform: str, reports_dir: Path):
        self.platform = platform.lower()
        self.reports_dir = Path(reports_dir)
        self.reports_dir.mkdir(parents=True, exist_ok=True)
        self.test_results = {}

    def scan_modules(self, tests_base_dir: Path) -> Dict[str, List[Path]]:
        """Scan for modules and their test files"""
        platform_dir = tests_base_dir / self.platform
        modules = {}

        if not platform_dir.exists():
            print(f"WARNING: {platform_dir} does not exist")
            return modules

        # Find all module directories
        for module_dir in sorted(platform_dir.iterdir()):
            if module_dir.is_dir() and not module_dir.name.startswith('.'):
                module_name = module_dir.name
                test_files = list(module_dir.glob("*.xlsx"))
                if test_files:
                    modules[module_name] = test_files
                    print(f"Found module '{module_name}' with {len(test_files)} test file(s)")

        return modules

    def load_test_results(self, results_file: Path) -> Dict:
        """Load test results from JSON file"""
        if results_file.exists():
            with open(results_file, 'r') as f:
                return json.load(f)
        return {}

    def generate_html_report(self, modules: Dict[str, List[Path]],
                            test_results: Dict = None) -> str:
        """Generate HTML report for all modules"""

        total_modules = len(modules)
        total_tests = sum(len(tests) for tests in modules.values())

        html = f"""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TestZen - {self.platform.upper()} Test Report</title>
    <style>
        * {{
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }}

        body {{
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
            background: #f5f7fa;
            color: #2d3748;
            line-height: 1.6;
        }}

        .header {{
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 2rem;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }}

        .header h1 {{
            font-size: 2.5rem;
            margin-bottom: 0.5rem;
        }}

        .header .platform-badge {{
            display: inline-block;
            background: rgba(255,255,255,0.2);
            padding: 0.25rem 1rem;
            border-radius: 20px;
            font-size: 0.9rem;
            text-transform: uppercase;
            letter-spacing: 1px;
        }}

        .header .meta {{
            margin-top: 1rem;
            opacity: 0.9;
        }}

        .container {{
            max-width: 1200px;
            margin: 2rem auto;
            padding: 0 1rem;
        }}

        .summary {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 1.5rem;
            margin-bottom: 2rem;
        }}

        .summary-card {{
            background: white;
            padding: 1.5rem;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            text-align: center;
        }}

        .summary-card .number {{
            font-size: 3rem;
            font-weight: bold;
            color: #667eea;
        }}

        .summary-card .label {{
            color: #718096;
            text-transform: uppercase;
            font-size: 0.875rem;
            letter-spacing: 0.5px;
            margin-top: 0.5rem;
        }}

        .module {{
            background: white;
            border-radius: 8px;
            padding: 1.5rem;
            margin-bottom: 1.5rem;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }}

        .module-header {{
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 1rem;
            padding-bottom: 1rem;
            border-bottom: 2px solid #e2e8f0;
        }}

        .module-title {{
            font-size: 1.5rem;
            color: #2d3748;
            font-weight: 600;
        }}

        .module-badge {{
            background: #667eea;
            color: white;
            padding: 0.25rem 0.75rem;
            border-radius: 12px;
            font-size: 0.875rem;
        }}

        .test-list {{
            list-style: none;
        }}

        .test-item {{
            padding: 1rem;
            margin-bottom: 0.75rem;
            background: #f7fafc;
            border-left: 4px solid #667eea;
            border-radius: 4px;
            transition: all 0.3s ease;
        }}

        .test-item:hover {{
            background: #edf2f7;
            transform: translateX(4px);
        }}

        .test-name {{
            font-weight: 500;
            color: #2d3748;
            margin-bottom: 0.25rem;
        }}

        .test-path {{
            font-size: 0.875rem;
            color: #718096;
            font-family: 'Courier New', monospace;
        }}

        .status {{
            display: inline-block;
            padding: 0.25rem 0.75rem;
            border-radius: 12px;
            font-size: 0.75rem;
            font-weight: 600;
            text-transform: uppercase;
        }}

        .status.pass {{
            background: #c6f6d5;
            color: #22543d;
        }}

        .status.fail {{
            background: #fed7d7;
            color: #742a2a;
        }}

        .status.pending {{
            background: #feebc8;
            color: #7c2d12;
        }}

        .footer {{
            text-align: center;
            padding: 2rem;
            color: #718096;
            font-size: 0.875rem;
        }}

        .empty-state {{
            text-align: center;
            padding: 3rem;
            color: #a0aec0;
        }}

        .empty-state svg {{
            width: 64px;
            height: 64px;
            margin-bottom: 1rem;
            opacity: 0.5;
        }}
    </style>
</head>
<body>
    <div class="header">
        <h1>TestZen Test Report</h1>
        <span class="platform-badge">{self.platform.upper()}</span>
        <div class="meta">
            <strong>Generated:</strong> {datetime.now().strftime('%Y-%m-%d %H:%M:%S UTC')}<br>
            <strong>CI Platform:</strong> {'GitHub Actions' if self.platform == 'android' else 'GitLab CI/CD'}
        </div>
    </div>

    <div class="container">
        <div class="summary">
            <div class="summary-card">
                <div class="number">{total_modules}</div>
                <div class="label">Modules</div>
            </div>
            <div class="summary-card">
                <div class="number">{total_tests}</div>
                <div class="label">Test Files</div>
            </div>
            <div class="summary-card">
                <div class="number">{self.platform.upper()}</div>
                <div class="label">Platform</div>
            </div>
        </div>
"""

        if not modules:
            html += """
        <div class="empty-state">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <h2>No test files found</h2>
            <p>Add .xlsx test files to tests/{}/[module_name]/ to get started</p>
        </div>
""".format(self.platform)
        else:
            for module_name, test_files in sorted(modules.items()):
                html += f"""
        <div class="module">
            <div class="module-header">
                <h2 class="module-title">{module_name}</h2>
                <span class="module-badge">{len(test_files)} test(s)</span>
            </div>
            <ul class="test-list">
"""
                for test_file in sorted(test_files):
                    test_name = test_file.stem
                    test_rel_path = test_file.relative_to(Path.cwd()) if Path.cwd() in test_file.parents else test_file

                    # Check if we have results for this test
                    status_html = '<span class="status pending">Pending</span>'
                    if test_results and module_name in test_results:
                        module_results = test_results[module_name]
                        if test_name in module_results:
                            result = module_results[test_name]
                            status = 'pass' if result.get('passed') else 'fail'
                            status_html = f'<span class="status {status}">{status}</span>'

                    html += f"""
                <li class="test-item">
                    <div class="test-name">
                        {test_name}
                        {status_html}
                    </div>
                    <div class="test-path">{test_rel_path}</div>
                </li>
"""
                html += """
            </ul>
        </div>
"""

        html += """
    </div>

    <div class="footer">
        Generated by TestZen Framework<br>
        Multi-Module Test Automation Platform
    </div>
</body>
</html>
"""
        return html

    def generate_json_report(self, modules: Dict[str, List[Path]]) -> Dict:
        """Generate JSON report data"""
        report = {
            "platform": self.platform,
            "generated_at": datetime.now().isoformat(),
            "summary": {
                "total_modules": len(modules),
                "total_tests": sum(len(tests) for tests in modules.values())
            },
            "modules": {}
        }

        for module_name, test_files in modules.items():
            report["modules"][module_name] = {
                "test_count": len(test_files),
                "test_files": [str(f) for f in test_files]
            }

        return report

    def load_test_results_from_reports(self, modules: Dict[str, List[Path]]) -> Dict:
        """Load test results from individual test JSON reports"""
        results = {}

        for module_name, test_files in modules.items():
            results[module_name] = {}
            for test_file in test_files:
                test_name = test_file.stem
                # Look for JSON summary file
                json_file = self.reports_dir / f"{test_name}_summary.json"

                if json_file.exists():
                    try:
                        with open(json_file, 'r') as f:
                            test_data = json.load(f)
                            # Determine if test passed
                            passed = test_data.get('overall_status') == 'PASSED'
                            results[module_name][test_name] = {
                                'passed': passed,
                                'total_steps': test_data.get('total_steps', 0),
                                'passed_steps': test_data.get('passed_steps', 0),
                                'failed_steps': test_data.get('failed_steps', 0)
                            }
                    except Exception as e:
                        print(f"Warning: Could not load results for {test_name}: {e}")

        return results

    def save_reports(self, modules: Dict[str, List[Path]],
                    test_results: Dict = None):
        """Save both HTML and JSON reports"""

        # Load test results from JSON files if not provided
        if test_results is None:
            test_results = self.load_test_results_from_reports(modules)

        # Generate HTML report
        html_content = self.generate_html_report(modules, test_results)
        html_file = self.reports_dir / f"{self.platform}_test_report.html"
        with open(html_file, 'w') as f:
            f.write(html_content)
        print(f"HTML report saved: {html_file}")

        # Generate JSON report
        json_data = self.generate_json_report(modules)
        json_file = self.reports_dir / f"{self.platform}_test_report.json"
        with open(json_file, 'w') as f:
            json.dump(json_data, f, indent=2)
        print(f"JSON report saved: {json_file}")

        return html_file, json_file


def main():
    """Main entry point"""
    if len(sys.argv) < 2:
        print("Usage: python generate_module_report.py <platform> [tests_dir] [reports_dir]")
        print("Example: python generate_module_report.py android tests reports")
        sys.exit(1)

    platform = sys.argv[1].lower()
    tests_dir = Path(sys.argv[2]) if len(sys.argv) > 2 else Path("tests")
    reports_dir = Path(sys.argv[3]) if len(sys.argv) > 3 else Path("reports")

    if platform not in ['android', 'ios']:
        print(f"ERROR: Platform must be 'android' or 'ios', got '{platform}'")
        sys.exit(1)

    print(f"=== TestZen Multi-Module Report Generator ===")
    print(f"Platform: {platform.upper()}")
    print(f"Tests directory: {tests_dir}")
    print(f"Reports directory: {reports_dir}")
    print()

    generator = ModuleReportGenerator(platform, reports_dir)

    # Scan for modules and test files
    modules = generator.scan_modules(tests_dir)

    if not modules:
        print(f"WARNING: No modules found in {tests_dir}/{platform}/")
        print("Create module folders and add .xlsx test files to generate reports")

    # Generate and save reports
    html_file, json_file = generator.save_reports(modules)

    print()
    print("=== Report Generation Complete ===")
    print(f"View HTML report: {html_file}")
    print(f"View JSON data: {json_file}")

    return 0 if modules else 1


if __name__ == "__main__":
    sys.exit(main())
