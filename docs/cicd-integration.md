# CI/CD Integration Guide

Guide for integrating TestZen with CI/CD pipelines.

---

## GitHub Actions

### Basic Setup

Create `.github/workflows/testzen.yml`:

```yaml
name: TestZen Mobile Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  android-tests:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout code
      uses: actions/checkout@v3

    - name: Set up Python
      uses: actions/setup-python@v4
      with:
        python-version: '3.8'

    - name: Set up Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '16'

    - name: Set up Android SDK
      uses: android-actions/setup-android@v2

    - name: Install dependencies
      run: |
        pip install -r requirements.txt
        npm install -g appium
        appium driver install uiautomator2

    - name: Start Appium
      run: |
        appium --allow-insecure=*:chromedriver_autodownload &
        sleep 5

    - name: Start Android Emulator
      uses: reactivecircus/android-emulator-runner@v2
      with:
        api-level: 29
        target: default
        arch: x86_64
        profile: Nexus 6
        script: |
          ./testzen run --all --platform android

    - name: Upload Test Reports
      if: always()
      uses: actions/upload-artifact@v3
      with:
        name: test-reports
        path: reports/
```

---

## GitLab CI

### Basic Setup

Create `.gitlab-ci.yml`:

```yaml
image: ubuntu:latest

variables:
  ANDROID_SDK_ROOT: "/opt/android-sdk"

stages:
  - test

android_tests:
  stage: test
  before_script:
    # Install dependencies
    - apt-get update
    - apt-get install -y python3 python3-pip nodejs npm openjdk-11-jdk wget unzip

    # Install Android SDK
    - wget https://dl.google.com/android/repository/commandlinetools-linux-latest.zip
    - mkdir -p $ANDROID_SDK_ROOT/cmdline-tools
    - unzip commandlinetools-linux-latest.zip -d $ANDROID_SDK_ROOT/cmdline-tools

    # Install TestZen dependencies
    - pip3 install -r requirements.txt
    - npm install -g appium
    - appium driver install uiautomator2

  script:
    # Start Appium
    - appium --allow-insecure=*:chromedriver_autodownload &
    - sleep 5

    # Run tests
    - ./testzen run --all --platform android

  artifacts:
    when: always
    paths:
      - reports/
    expire_in: 1 week
```

---

## Jenkins

### Basic Pipeline

Create `Jenkinsfile`:

```groovy
pipeline {
    agent any

    environment {
        ANDROID_HOME = '/opt/android-sdk'
    }

    stages {
        stage('Setup') {
            steps {
                sh 'pip3 install -r requirements.txt'
                sh 'npm install -g appium'
                sh 'appium driver install uiautomator2'
            }
        }

        stage('Start Appium') {
            steps {
                sh 'appium --allow-insecure=*:chromedriver_autodownload &'
                sh 'sleep 5'
            }
        }

        stage('Run Tests') {
            steps {
                sh './testzen run --all --platform android'
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'reports/**/*', fingerprint: true
            publishHTML([
                allowMissing: false,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'reports/html_reports',
                reportFiles: 'TestZen_Report_*.html',
                reportName: 'TestZen Report'
            ])
        }
    }
}
```

---

## Best Practices

### 1. Use Specific Emulator Images

```yaml
# GitHub Actions
with:
  api-level: 29
  target: default
  arch: x86_64
```

### 2. Cache Dependencies

```yaml
# GitHub Actions - Cache pip
- name: Cache pip packages
  uses: actions/cache@v3
  with:
    path: ~/.cache/pip
    key: ${{ runner.os }}-pip-${{ hashFiles('requirements.txt') }}

# Cache npm packages
- name: Cache npm packages
  uses: actions/cache@v3
  with:
    path: ~/.npm
    key: ${{ runner.os }}-npm-${{ hashFiles('package*.json') }}
```

### 3. Parallel Test Execution

Run different test modules in parallel:

```yaml
strategy:
  matrix:
    test-module: [login, checkout, profile]

steps:
  - run: ./testzen run --all --platform android
    # Filter by module in test runner
```

### 4. Artifact Retention

```yaml
artifacts:
  when: always
  paths:
    - reports/
  expire_in: 30 days
```

### 5. Failure Notifications

```yaml
# Slack notification on failure
- name: Notify Slack
  if: failure()
  uses: rtCamp/action-slack-notify@v2
  env:
    SLACK_WEBHOOK: ${{ secrets.SLACK_WEBHOOK }}
```

---

## Environment Variables

Set these in your CI/CD platform:

```bash
ANDROID_HOME=/path/to/android/sdk
PATH=$PATH:$ANDROID_HOME/platform-tools
```

---

## Running Specific Tests

```bash
# In CI/CD pipeline
./testzen run --file tests/android/login/valid_login.xlsx

# Run all tests for module
./testzen run --all --platform android
```

---

## Handling Test Failures

### Continue on Failure

```bash
./testzen run --file test.xlsx --skip-on-fail
```

### Exit Codes

TestZen returns:
- `0` - All tests passed
- `1` - One or more tests failed

Use in CI/CD:
```bash
./testzen run --all || exit 1
```

---

## Viewing Reports in CI/CD

### HTML Reports

Most CI/CD platforms support HTML artifact viewing:

- **GitHub Actions:** Download artifacts or use Pages
- **GitLab CI:** View in job artifacts browser
- **Jenkins:** Use HTML Publisher plugin

### Example: GitHub Pages

```yaml
- name: Deploy to GitHub Pages
  uses: peaceiris/actions-gh-pages@v3
  with:
    github_token: ${{ secrets.GITHUB_TOKEN }}
    publish_dir: ./reports/html_reports
```

---

## Docker Support

### Dockerfile for TestZen

```dockerfile
FROM ubuntu:20.04

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/platform-tools

RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    nodejs \
    npm \
    openjdk-11-jdk \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /testzen
COPY . .

RUN pip3 install -r requirements.txt
RUN npm install -g appium
RUN appium driver install uiautomator2

CMD ["./testzen", "run", "--all", "--platform", "android"]
```

### Docker Compose

```yaml
version: '3'
services:
  testzen:
    build: .
    volumes:
      - ./reports:/testzen/reports
    environment:
      - ANDROID_HOME=/opt/android-sdk
```

---

## Troubleshooting CI/CD

### Emulator Won't Start

Enable hardware acceleration:
```yaml
# GitHub Actions
with:
  emulator-options: -no-window -gpu swiftshader_indirect -no-snapshot -noaudio -no-boot-anim
```

### Tests Timeout

Increase timeout:
```yaml
timeout-minutes: 30
```

### Appium Connection Issues

Ensure Appium starts before tests:
```bash
appium &
sleep 10  # Wait for Appium to start
```

---

## Need Help?

- [Installation Guide](installation.md) - Setup instructions
- [Troubleshooting](troubleshooting.md) - Common issues
- [GitHub Issues](https://github.com/kavi-thirilo/testzen-lite/issues) - Report problems

---

[← Back to Main README](../README.md)
