# TestZen Lite - No-Code Mobile Test Automation

**Test mobile apps without writing code. Just Excel files and APK/IPA.**

Perfect for QA testers, manual testers wanting to automate, and teams needing quick test automation.

---

## Table of Contents

- [Quick Start](#quick-start)
  - [1. Clone Repository](#1-clone-repository)
  - [2. Add Your Mobile App](#2-add-your-mobile-app)
  - [3. Run Sample Test](#3-run-sample-test)
  - [4. View Test Report](#4-view-test-report)
- [Configure Reporting (Allure vs HTML)](#configure-reporting-allure-vs-html)
- [Create Your Own Tests](#create-your-own-tests)
- [Where to Keep Files](#where-to-keep-files)
- [Command Reference](#command-reference)
- [Setup Issues](#setup-issues)
- [Documentation](#documentation)
- [Key Features](#key-features)
- [System Requirements](#system-requirements)
- [Quick Links](#quick-links)
- [Example Test Report](#example-test-report)
- [Need Help](#need-help)
- [Version](#version)

---

## Quick Start

### 1. Clone Repository

```bash
git clone https://github.com/kavi-thirilo/testzen-lite.git
cd testzen-lite
```

---

### 2. Add Your Mobile App

**For Android:**
```bash
# Copy your APK file here:
cp /path/to/your-app.apk apps/android/
```

**For iOS:**
```bash
# Copy your IPA file here:
cp /path/to/your-app.ipa apps/ios/
```

**Note:** Place only ONE APK or IPA file per platform folder.

---

### 3. Run Sample Test

Run the included sample test to verify everything works:

```bash
./testzen run --file tests/android/billing/Payment_Form_Validation_Test.xlsx
```

**What TestZen does automatically:**
- Checks and installs prerequisites (if needed)
- Starts Appium server automatically
- Launches emulator (if no device connected)
- Installs your app on the device
- Executes test steps from Excel file
- Takes screenshots at each step
- Generates interactive Allure report

**Expected output:**
```
[TZ] Starting test execution...
[TZ] Device: emulator-5554 (connected)
[TZ] Installing app...
[TZ] Running 15 test steps...
[TZ] Test completed successfully!
[Allure] Report generated: reports/allure-report/index.html
```

---

### 4. View Test Report

TestZen supports two reporting formats (configured in `config/reporting_config.json`):

**Allure Reports (Default)** - Interactive, professional reports with trends and history

After test completes, view Allure report:
```bash
# IMPORTANT: Run this command from the testzen-lite directory
# (where you ran the test)

# Open Allure report (starts local web server)
allure open reports/allure-report

# Or serve results directly
allure serve reports/allure-results
```

**Important Notes:**
- Never double-click `index.html` - Allure reports must be served via HTTP
- Always run `allure open` from the framework directory (testzen-lite)
- The `reports/` path is relative to your project directory

Report shows:
- Interactive timeline and trends
- Test history and flaky test detection
- Screenshots attached to each step
- Detailed step execution with parameters
- Execution time and status

**HTML Reports** - Standalone HTML reports (if configured)

Report location:
```
reports/<test_name>_report.html
```

Open directly in browser to see:
- Pass/fail status for each step
- Screenshots at each step
- Execution time
- Detailed error messages

---

## Configure Reporting (Allure vs HTML)

TestZen supports two reporting formats. You can easily switch between them by editing the configuration file.

### Current Configuration

The reporter is configured in `config/reporting_config.json`:

```json
{
  "default_reporter": "allure",
  "reporters": {
    "allure": {
      "enabled": true,
      "output_dir": "reports/allure-results",
      "report_dir": "reports/allure-report"
    },
    "html": {
      "enabled": true,
      "output_dir": "reports",
      "template": "professional"
    }
  }
}
```

### Switch to HTML Reports

If you prefer standalone HTML reports instead of Allure:

1. Open `config/reporting_config.json`
2. Change `"default_reporter"` from `"allure"` to `"html"`
3. Save the file

```json
{
  "default_reporter": "html",
  ...
}
```

After changing, run your tests normally. Reports will be generated at `reports/<test_name>_report.html`

### Switch to Allure Reports (Default)

To use Allure reports:

1. Open `config/reporting_config.json`
2. Change `"default_reporter"` to `"allure"`
3. Save the file
4. Make sure Allure CLI is installed:

```bash
# macOS:
brew install allure

# Or npm (all platforms):
npm install -g allure-commandline
```

After changing, run your tests normally. View reports with `allure open reports/allure-report`

### Enable Multi-Reporter Mode (Both Reports)

To generate both Allure and HTML reports simultaneously:

1. Open `config/reporting_config.json`
2. Set `"multi_reporter"` to `true`
3. Ensure both reporters are enabled
4. Save the file

```json
{
  "default_reporter": "allure",
  "multi_reporter": true,
  "reporters": {
    "allure": {
      "enabled": true
    },
    "html": {
      "enabled": true
    }
  }
}
```

After changing, run your tests normally. Both reports will be generated:
- Allure: `reports/allure-report/index.html` (view with `allure open reports/allure-report` from framework directory)
- HTML: `reports/<test_name>_report.html` (open directly in browser)

**Note:** Multi-reporter mode is useful for:
- Generating reports for different audiences (technical team vs stakeholders)
- Having backup reports in different formats
- Local HTML for quick review + Allure for CI/CD

### Comparison

| Feature | Allure Reports | HTML Reports | Multi-Reporter |
|---------|---------------|--------------|----------------|
| Interactive Dashboard | Yes | No | Both |
| Test History & Trends | Yes | No | Both |
| Flaky Test Detection | Yes | No | Both |
| Viewing Method | Requires web server | Direct browser open | Both |
| Installation | Requires Allure CLI | No dependencies | Requires Allure CLI |
| CI/CD Integration | Excellent | Good | Excellent |
| Report Size | Larger | Smaller | Both (larger total) |
| Best For | Teams, CI/CD, Analytics | Quick local testing | Maximum flexibility |

---

## Create Your Own Tests

### Option 1: Duplicate Sample Test

```bash
# Sample test is at:
tests/android/billing/Payment_Form_Validation_Test.xlsx

# Duplicate it, open in Excel, and modify for your app
```

### Option 2: Create New Test

1. Create Excel file with these columns:
   - **Step** - Step number
   - **Action** - click, enter_text, verify_text, etc.
   - **Locator Type** - id, xpath, accessibility_id
   - **Locator Value** - Element identifier
   - **Test Data** - Text to enter or verify
   - **Expected Result** - What should happen

2. Save in `tests/android/` or `tests/ios/`

3. Run your test:
   ```bash
   ./testzen run --file tests/android/your_test.xlsx
   ```

**Need help creating tests?** See [Test Creation Guide](docs/test-creation.md)

---

## Where to Keep Files

```
testzen-lite/
├── apps/
│   ├── android/          # Place your APK here
│   └── ios/              # Place your IPA here
├── tests/
│   ├── android/          # Place Android test .xlsx files here
│   │   └── billing/      # Organize by feature
│   └── ios/              # Place iOS test .xlsx files here
│       └── login/        # Organize by feature
├── config/
│   └── reporting_config.json  # Configure Allure vs HTML reporting
└── reports/
    ├── allure-results/   # Allure test results (JSON)
    ├── allure-report/    # Allure HTML report
    ├── screenshots/      # Test screenshots
    └── *_report.html     # Custom HTML reports (if configured)
```

---

## Command Reference

### Run Tests

```bash
# Run specific test
./testzen run --file tests/android/your_test.xlsx

# Run all tests for a platform
./testzen run --all --platform android

# Run all tests (both Android and iOS)
./testzen run --all
```

### List Tests

```bash
# List all available tests
./testzen list

# List tests for specific platform
./testzen list --platform android
```

### Manage Emulators

```bash
# List available emulators
./testzen emulator list

# Launch emulator
./testzen emulator launch

# Stop emulator
./testzen emulator stop
```

### Other Commands

```bash
# Show version
./testzen version

# Show help
./testzen --help
```

---

## Setup Issues?

If you encounter errors, check what's missing:

### "command not found: pip"
**What's missing:** Python not installed
**Fix:** [Python Setup Guide](docs/python-setup.md)

---

### "command not found: npm"
**What's missing:** Node.js not installed
**Fix:** [Node.js Setup Guide](docs/nodejs-setup.md)

---

### "adb: command not found"
**What's missing:** Android SDK not installed
**Fix:** [Android SDK Setup Guide](docs/android-sdk-setup.md)

---

### "appium: command not found"
**What's missing:** Appium not installed
**Fix:** [Appium Setup Guide](docs/appium-setup.md)

---

### "No device connected"
**What's missing:** Device or emulator not available
**Fix:** [Device Setup Guide](docs/device-setup.md)

---

### Device shows "offline" or "unauthorized"
**What's missing:** USB debugging not enabled
**Quick Fix:**
```bash
./scripts/enable_usb_debugging.sh
```
**Detailed Fix:** [Device Setup Guide - Enable USB Debugging](docs/device-setup.md#enable-usb-debugging-on-emulator)

---

### Emulator crashes or won't launch
**Problem:** Emulator crashes immediately, port 5037 in use, or zombie processes
**Quick Fix:**
```bash
adb kill-server && adb start-server
```
**Detailed Fix:** [Troubleshooting Guide - Emulator Crashes](docs/troubleshooting.md#emulator-crashes-on-launch)

---

### Commands don't work after installation
**What's missing:** PATH not configured
**Fix:** [PATH Configuration Guide](docs/path-configuration.md)

---

### Allure report shows "loading" forever
**Problem:** Allure reports opened by double-clicking index.html
**Quick Fix:**
```bash
# Never double-click index.html
# Always use Allure CLI to view reports:
allure open reports/allure-report
```
**If Allure CLI not installed:**
```bash
# macOS:
brew install allure

# npm (all platforms):
npm install -g allure-commandline
```

---

### Other installation issues
**See:** [Complete Installation Guide](docs/installation.md)

---

## Documentation

### Getting Started
- [Installation Guide](docs/installation.md) - Complete setup instructions
- [Device Setup](docs/device-setup.md) - Emulator and physical device setup
- [Test Creation](docs/test-creation.md) - How to write tests in Excel

### Prerequisites Setup
- [Python Setup](docs/python-setup.md) - Install Python 3.8+
- [Node.js Setup](docs/nodejs-setup.md) - Install Node.js and npm
- [Android SDK Setup](docs/android-sdk-setup.md) - Install Android development tools
- [Appium Setup](docs/appium-setup.md) - Install Appium and drivers
- [PATH Configuration](docs/path-configuration.md) - Fix PATH issues

### Advanced
- [Advanced Usage](docs/advanced-usage.md) - Advanced features and options
- [Reporting Configuration](config/reporting_config.json) - Switch between Allure and HTML reports
- [CI/CD Integration](docs/cicd-integration.md) - GitHub Actions, GitLab CI
- [Troubleshooting](docs/troubleshooting.md) - Common issues and solutions

---

## Key Features

- **No Coding Required** - Write tests in Excel
- **Auto-Appium** - Server starts automatically
- **Auto-Emulator** - Launches emulator if needed
- **Multi-Locator Fallback** - Automatic retry with alternative locators
- **Allure Reports** - Interactive reports with trends and history (default)
- **Custom HTML Reports** - Standalone HTML reports option
- **CI/CD Ready** - Pre-configured for GitHub Actions & GitLab CI
- **Debug APK Support** - Works with development builds
- **Multi-Platform** - Android and iOS support

---

## System Requirements

- **Python:** 3.8 or higher
- **Node.js:** 16 or higher
- **Android SDK:** For Android testing
- **Xcode:** For iOS testing (macOS only)
- **Allure CLI:** For viewing Allure reports (optional)
  - Install: `brew install allure` (macOS) or `npm install -g allure-commandline`
- **Operating Systems:** macOS, Linux, Windows

---

## Quick Links

- **GitHub:** https://github.com/kavi-thirilo/testzen-lite
- **Issues:** https://github.com/kavi-thirilo/testzen-lite/issues
- **Releases:** https://github.com/kavi-thirilo/testzen-lite/releases
- **Sample Test:** [Payment_Form_Validation_Test.xlsx](tests/android/billing/Payment_Form_Validation_Test.xlsx)

---

## Example Test Report

After running tests, Allure report shows:

- Interactive dashboard with test overview
- Test execution timeline and trends
- Historical data and flaky test detection
- Screenshots attached to each test step
- Step-by-step execution with parameters
- Detailed error messages and stack traces
- Pass/fail statistics and duration
- Categories and severity levels

**View with:** `allure open reports/allure-report`

For custom HTML reports, see `reports/<test_name>_report.html`

---

## Need Help?

1. **Start with:** [Installation Guide](docs/installation.md)
2. **Having issues?** Check [Troubleshooting](docs/troubleshooting.md)
3. **Report bugs:** [GitHub Issues](https://github.com/kavi-thirilo/testzen-lite/issues)

---

## Version

Current version: **1.2.4**

See [Release Notes](docs/releases/RELEASE_NOTES_v1.2.4.md) for changelog.

---

**Happy Testing!**
