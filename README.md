# TestZen Lite - No-Code Mobile Test Automation

**Test mobile apps without writing code. Just Excel files and APK/IPA.**

Perfect for QA testers, manual testers wanting to automate, and teams needing quick test automation.

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

```bash
./testzen run --file tests/android/billing/Payment_Form_Validation_Test.xlsx
```

TestZen will:
- Install prerequisites (if needed)
- Start Appium server automatically
- Launch emulator (if no device connected)
- Install your app
- Run the test
- Generate HTML report

---

### 4. View Test Report

TestZen supports two reporting formats (configured in `config/reporting_config.json`):

**Allure Reports (Default)** - Interactive, professional reports with trends and history

After test completes, view Allure report:
```bash
# Open Allure report (starts local web server)
allure open reports/allure-report

# Or serve results directly
allure serve reports/allure-results
```

**Important:** Never double-click `index.html` - Allure reports must be served via HTTP.

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

**Switch Reporting Format:**
Edit `config/reporting_config.json` and change `default_reporter` to `"html"` or `"allure"`

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
