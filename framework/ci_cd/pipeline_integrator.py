#!/usr/bin/env python3
"""
CI/CD Pipeline Integration for TestZen Framework
Supports Jenkins, GitHub Actions, GitLab CI, Azure DevOps, and more
"""

import os
import json
import yaml
import subprocess
from typing import Dict, Any, List, Optional
from pathlib import Path
import logging
from enum import Enum

class CIPlatform(Enum):
    JENKINS = "jenkins"
    GITHUB_ACTIONS = "github_actions"
    GITLAB_CI = "gitlab_ci"
    AZURE_DEVOPS = "azure_devops"
    CIRCLECI = "circleci"
    BAMBOO = "bamboo"
    TEAMCITY = "teamcity"

class PipelineIntegrator:
    """Main CI/CD pipeline integration class"""

    def __init__(self):
        self.logger = logging.getLogger(__name__)
        self.platform = self._detect_platform()
        self.environment_vars = {}

    def _detect_platform(self) -> Optional[CIPlatform]:
        """Detect which CI/CD platform is running"""
        if os.environ.get('JENKINS_URL'):
            return CIPlatform.JENKINS
        elif os.environ.get('GITHUB_ACTIONS'):
            return CIPlatform.GITHUB_ACTIONS
        elif os.environ.get('GITLAB_CI'):
            return CIPlatform.GITLAB_CI
        elif os.environ.get('TF_BUILD'):
            return CIPlatform.AZURE_DEVOPS
        elif os.environ.get('CIRCLECI'):
            return CIPlatform.CIRCLECI
        elif os.environ.get('bamboo_buildKey'):
            return CIPlatform.BAMBOO
        elif os.environ.get('TEAMCITY_VERSION'):
            return CIPlatform.TEAMCITY
        return None

    def get_build_info(self) -> Dict[str, Any]:
        """Get build information from CI environment"""
        info = {
            'platform': self.platform.value if self.platform else 'local',
            'build_id': 'local',
            'build_url': '',
            'branch': '',
            'commit': '',
            'triggered_by': '',
            'workspace': os.getcwd()
        }

        if self.platform == CIPlatform.JENKINS:
            info.update({
                'build_id': os.environ.get('BUILD_ID', ''),
                'build_url': os.environ.get('BUILD_URL', ''),
                'branch': os.environ.get('BRANCH_NAME', ''),
                'commit': os.environ.get('GIT_COMMIT', ''),
                'triggered_by': os.environ.get('BUILD_USER', ''),
                'workspace': os.environ.get('WORKSPACE', '')
            })
        elif self.platform == CIPlatform.GITHUB_ACTIONS:
            info.update({
                'build_id': os.environ.get('GITHUB_RUN_ID', ''),
                'build_url': f"https://github.com/{os.environ.get('GITHUB_REPOSITORY')}/actions/runs/{os.environ.get('GITHUB_RUN_ID')}",
                'branch': os.environ.get('GITHUB_REF_NAME', ''),
                'commit': os.environ.get('GITHUB_SHA', ''),
                'triggered_by': os.environ.get('GITHUB_ACTOR', ''),
                'workspace': os.environ.get('GITHUB_WORKSPACE', '')
            })
        elif self.platform == CIPlatform.GITLAB_CI:
            info.update({
                'build_id': os.environ.get('CI_PIPELINE_ID', ''),
                'build_url': os.environ.get('CI_PIPELINE_URL', ''),
                'branch': os.environ.get('CI_COMMIT_REF_NAME', ''),
                'commit': os.environ.get('CI_COMMIT_SHA', ''),
                'triggered_by': os.environ.get('GITLAB_USER_LOGIN', ''),
                'workspace': os.environ.get('CI_PROJECT_DIR', '')
            })

        return info

    def publish_test_results(self, results: Dict[str, Any], format: str = 'junit'):
        """Publish test results to CI platform"""
        if format == 'junit':
            self._publish_junit_results(results)
        elif format == 'cucumber':
            self._publish_cucumber_results(results)
        elif format == 'allure':
            self._publish_allure_results(results)

    def _publish_junit_results(self, results: Dict[str, Any]):
        """Publish JUnit format results"""
        junit_xml = self._generate_junit_xml(results)
        output_path = 'test-results.xml'

        with open(output_path, 'w') as f:
            f.write(junit_xml)

        # Platform-specific publishing
        if self.platform == CIPlatform.JENKINS:
            print(f"##teamcity[importData type='junit' path='{output_path}']")
        elif self.platform == CIPlatform.GITHUB_ACTIONS:
            print(f"::set-output name=test-results::{output_path}")

        self.logger.info(f"JUnit results published to {output_path}")

    def _generate_junit_xml(self, results: Dict[str, Any]) -> str:
        """Generate JUnit XML from test results"""
        from xml.etree.ElementTree import Element, SubElement, tostring

        testsuites = Element('testsuites')
        testsuites.set('name', 'TestZen Automation')
        testsuites.set('tests', str(results.get('total_steps', 0)))
        testsuites.set('failures', str(results.get('failed_steps', 0)))

        testsuite = SubElement(testsuites, 'testsuite')
        testsuite.set('name', results.get('test_file', 'Unknown'))
        testsuite.set('tests', str(results.get('total_steps', 0)))
        testsuite.set('failures', str(results.get('failed_steps', 0)))
        testsuite.set('time', str(results.get('duration', 0)))

        for step in results.get('steps', []):
            testcase = SubElement(testsuite, 'testcase')
            testcase.set('name', f"Step {step.get('step_no', '')} - {step.get('action', '')}")
            testcase.set('classname', 'TestZen')
            testcase.set('time', str(step.get('duration', 0)))

            if step.get('status') == 'failed':
                failure = SubElement(testcase, 'failure')
                failure.set('message', step.get('message', 'Test failed'))
                failure.text = step.get('error', '')

        return tostring(testsuites, encoding='unicode')

    def set_environment_variable(self, name: str, value: str):
        """Set environment variable for current and future steps"""
        os.environ[name] = value

        if self.platform == CIPlatform.GITHUB_ACTIONS:
            with open(os.environ.get('GITHUB_ENV', ''), 'a') as f:
                f.write(f"{name}={value}\n")
        elif self.platform == CIPlatform.AZURE_DEVOPS:
            print(f"##vso[task.setvariable variable={name}]{value}")

    def add_comment_to_pr(self, comment: str):
        """Add comment to pull request"""
        if self.platform == CIPlatform.GITHUB_ACTIONS:
            pr_number = os.environ.get('GITHUB_REF').split('/')[-2]
            repo = os.environ.get('GITHUB_REPOSITORY')

            subprocess.run([
                'gh', 'pr', 'comment', pr_number,
                '--repo', repo,
                '--body', comment
            ])

    def upload_artifacts(self, paths: List[str], artifact_name: str = 'test-artifacts'):
        """Upload artifacts to CI platform"""
        if self.platform == CIPlatform.GITHUB_ACTIONS:
            print(f"::set-output name=artifact-path::{','.join(paths)}")
            print(f"Artifacts should be uploaded using actions/upload-artifact@v2")
        elif self.platform == CIPlatform.JENKINS:
            for path in paths:
                print(f"Archiving artifact: {path}")
        elif self.platform == CIPlatform.GITLAB_CI:
            # GitLab CI artifacts are configured in .gitlab-ci.yml
            self.logger.info(f"Artifacts configured in .gitlab-ci.yml: {paths}")

class JenkinsIntegration:
    """Jenkins-specific integration"""

    def __init__(self):
        self.logger = logging.getLogger(__name__)

    def generate_jenkinsfile(self, config: Dict[str, Any]) -> str:
        """Generate Jenkinsfile for TestZen automation"""
        jenkinsfile = """pipeline {
    agent any

    environment {
        TESTZEN_HOME = '${WORKSPACE}/testzen'
        APPIUM_HOME = '${WORKSPACE}/appium'
    }

    stages {
        stage('Setup') {
            steps {
                echo 'Setting up TestZen environment...'
                sh 'pip install -r requirements.txt'
            }
        }

        stage('Start Appium') {
            steps {
                echo 'Starting Appium server...'
                sh 'appium &'
                sleep 10
            }
        }

        stage('Run Tests') {
            steps {
                echo 'Running TestZen tests...'
                sh './testzen run --all --platform ${PLATFORM}'
            }
        }

        stage('Generate Reports') {
            steps {
                echo 'Generating test reports...'
                publishHTML([
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'reports',
                    reportFiles: '**/test_report.html',
                    reportName: 'TestZen Report'
                ])
            }
        }
    }

    post {
        always {
            echo 'Cleaning up...'
            sh 'pkill -f appium || true'
            junit 'reports/**/*.xml'
        }
        success {
            echo 'Tests passed!'
            emailext(
                subject: 'TestZen Tests Passed',
                body: 'All tests passed successfully.',
                to: '${EMAIL_RECIPIENTS}'
            )
        }
        failure {
            echo 'Tests failed!'
            emailext(
                subject: 'TestZen Tests Failed',
                body: 'Some tests failed. Check the report for details.',
                to: '${EMAIL_RECIPIENTS}'
            )
        }
    }
}"""
        return jenkinsfile

class GitHubActionsIntegration:
    """GitHub Actions specific integration"""

    def __init__(self):
        self.logger = logging.getLogger(__name__)

    def generate_workflow(self, config: Dict[str, Any]) -> str:
        """Generate GitHub Actions workflow for TestZen"""
        workflow = {
            'name': 'TestZen Automation',
            'on': {
                'push': {
                    'branches': ['main', 'develop']
                },
                'pull_request': {
                    'branches': ['main']
                },
                'schedule': [
                    {'cron': '0 0 * * *'}  # Daily at midnight
                ]
            },
            'jobs': {
                'test': {
                    'runs-on': 'ubuntu-latest',
                    'strategy': {
                        'matrix': {
                            'platform': ['android', 'ios']
                        }
                    },
                    'steps': [
                        {
                            'name': 'Checkout code',
                            'uses': 'actions/checkout@v2'
                        },
                        {
                            'name': 'Setup Python',
                            'uses': 'actions/setup-python@v2',
                            'with': {
                                'python-version': '3.9'
                            }
                        },
                        {
                            'name': 'Install dependencies',
                            'run': 'pip install -r requirements.txt'
                        },
                        {
                            'name': 'Setup Appium',
                            'run': 'npm install -g appium'
                        },
                        {
                            'name': 'Start Appium',
                            'run': 'appium &'
                        },
                        {
                            'name': 'Run TestZen Tests',
                            'run': f"./testzen run --all --platform ${{{{ matrix.platform }}}}"
                        },
                        {
                            'name': 'Upload Test Results',
                            'uses': 'actions/upload-artifact@v2',
                            'if': 'always()',
                            'with': {
                                'name': 'test-results-${{ matrix.platform }}',
                                'path': 'reports/'
                            }
                        },
                        {
                            'name': 'Publish Test Report',
                            'uses': 'dorny/test-reporter@v1',
                            'if': 'always()',
                            'with': {
                                'name': 'TestZen Report - ${{ matrix.platform }}',
                                'path': 'reports/**/*.xml',
                                'reporter': 'java-junit'
                            }
                        }
                    ]
                }
            }
        }

        return yaml.dump(workflow, default_flow_style=False)

class TestManagementIntegration:
    """Integration with Test Management Systems"""

    def __init__(self):
        self.logger = logging.getLogger(__name__)

    def sync_with_jira(self, config: Dict[str, Any], results: Dict[str, Any]):
        """Sync test results with JIRA"""
        from jira import JIRA

        try:
            jira = JIRA(
                server=config['jira_url'],
                basic_auth=(config['jira_user'], config['jira_token'])
            )

            # Create or update test execution
            issue_dict = {
                'project': {'key': config['project_key']},
                'summary': f"TestZen Execution - {results['test_file']}",
                'description': self._format_results_for_jira(results),
                'issuetype': {'name': 'Test Execution'}
            }

            issue = jira.create_issue(fields=issue_dict)
            self.logger.info(f"JIRA issue created: {issue.key}")

            # Link to test cases
            for step in results.get('steps', []):
                if step.get('test_case_id'):
                    jira.create_issue_link(
                        type='Tests',
                        inwardIssue=issue.key,
                        outwardIssue=step['test_case_id']
                    )

        except Exception as e:
            self.logger.error(f"JIRA sync failed: {e}")

    def sync_with_testrail(self, config: Dict[str, Any], results: Dict[str, Any]):
        """Sync test results with TestRail"""
        import requests

        try:
            base_url = config['testrail_url']
            auth = (config['testrail_user'], config['testrail_password'])

            # Create test run
            run_data = {
                'suite_id': config['suite_id'],
                'name': f"TestZen Run - {results['test_file']}",
                'description': f"Automated run from TestZen",
                'include_all': True
            }

            response = requests.post(
                f"{base_url}/index.php?/api/v2/add_run/{config['project_id']}",
                json=run_data,
                auth=auth
            )

            run_id = response.json()['id']

            # Add results
            for step in results.get('steps', []):
                if step.get('test_case_id'):
                    result_data = {
                        'status_id': 1 if step['status'] == 'passed' else 5,
                        'comment': step.get('message', ''),
                        'elapsed': f"{step.get('duration', 0)}s"
                    }

                    requests.post(
                        f"{base_url}/index.php?/api/v2/add_result_for_case/{run_id}/{step['test_case_id']}",
                        json=result_data,
                        auth=auth
                    )

            self.logger.info(f"TestRail run created: {run_id}")

        except Exception as e:
            self.logger.error(f"TestRail sync failed: {e}")

    def _format_results_for_jira(self, results: Dict[str, Any]) -> str:
        """Format test results for JIRA description"""
        description = f"""
h2. Test Execution Summary

*Test File:* {results.get('test_file', 'Unknown')}
*Status:* {results.get('status', 'Unknown')}
*Total Steps:* {results.get('total_steps', 0)}
*Passed:* {results.get('passed_steps', 0)}
*Failed:* {results.get('failed_steps', 0)}
*Duration:* {results.get('duration', 0)}s

h3. Step Details

||Step||Action||Status||Duration||
"""
        for step in results.get('steps', []):
            description += f"|{step.get('step_no', '')}|{step.get('action', '')}|{step.get('status', '')}|{step.get('duration', 0)}s|\n"

        return description