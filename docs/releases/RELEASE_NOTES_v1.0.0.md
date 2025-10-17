# TestZen Lite v1.0.0 - First Production Release

## 🎉 Overview

TestZen Lite is a comprehensive no-code mobile test automation framework that enables QA teams to create and run automated tests for Android and iOS applications using Excel spreadsheets. No programming knowledge required.

**Perfect for:**
- QA Testers without programming experience
- Manual testers wanting to automate their tests
- Teams needing quick test automation setup
- Projects requiring CI/CD automated testing

---

## ✨ What's New in v1.0.0

### Robust File Validation
- **File format validation** - Only accepts `.xlsx` files with clear error messages
- **Early validation** - Checks file existence before starting Appium server (saves time)
- **Improved error messages** - Clear, actionable feedback for invalid inputs

### Enhanced User Experience
- **Updated README** - Corrected command examples to use `./testzen` wrapper script
- **Sample test included** - Ready-to-run billing test at `tests/android/billing/`
- **Fixed misleading success messages** - Accurate reporting when setup fails
- **Recursive test discovery** - Automatically finds tests in nested directories

### Auto-Appium Management
- **Automatic server startup** - No manual Appium management needed
- **Smart wrapper script** - `./testzen` handles everything automatically
- **Configurable behavior** - Keep Appium running or auto-stop after tests

---

## 🚀 Key Features

### Core Capabilities
✅ **No Coding Required** - Write all test steps in Excel spreadsheets
✅ **Multi-Platform** - Test both Android and iOS applications
✅ **Auto-Appium** - Automatic Appium server management
✅ **Multi-Locator Fallback** - Automatic fallback to alternative element locators
✅ **Professional Reports** - HTML and Excel reports with screenshots
✅ **CI/CD Ready** - Pre-configured for GitHub Actions and GitLab CI
✅ **Module Organization** - Organize tests by features for better maintenance

### Smart Features
✅ **APK/IPA Validation** - Validates app files before test execution
✅ **Element Recovery** - Handles both native and WebView applications
✅ **Comprehensive Logging** - Color-coded, timestamped logging
✅ **Screenshot Capture** - Before/after screenshots for every step
✅ **Flexible Cleanup** - Configurable post-test cleanup operations

---

## 📋 Changes History

### Latest Changes (v1.0.0)

**File Validation & Error Handling** (918b6ad)
- Add .xlsx file extension validation to reject invalid file formats
- Add early file validation in wrapper script to fail fast before Appium startup
- Fix misleading success message when no tests are executed
- Fix test listing to support recursive directory search
- Improve error messages for better user experience

**Documentation Improvements** (e8be61e)
- Update README with correct command examples using `./testzen` wrapper
- Fix outdated references from `python testzen.py` to `./testzen`
- Update all examples to reference actual sample test in `billing` module
- Clarify distinction between sample tests and user-created tests
- Add clear notes about what's included vs. what users need to create

**APK/IPA Validation** (2d6d506)
- Add validation to ensure app files exist before test execution
- Prevent wasted test time with missing app binaries
- Clear error messages for missing APK/IPA files

### Foundation Features

**Auto-Appium & Multi-Locator Support** (4afbecc)
- Automatic Appium server management
- Multi-locator fallback system (up to 3 locators per step)
- Enhanced HTML reporting with locator details
- Professional test execution framework

**Core Framework** (7b7ca6b - dacbf0a)
- Excel-based test case management
- Android and iOS platform support
- Device and emulator management
- Professional HTML/Excel reporting
- CI/CD integration (GitHub Actions, GitLab CI)
- Multi-module test organization
- Comprehensive action library

**Documentation & Setup** (cbd15e8, 7f8b689)
- Complete installation guide
- Quick start documentation
- CI/CD setup instructions
- Troubleshooting guide
- Best practices documentation

**Licensing** (889ef6c)
- MIT License for open-source distribution

---

## 📦 Installation

### Prerequisites
- Python 3.8 or higher
- Node.js (for Appium)
- Android SDK (for Android testing)
- Xcode (for iOS testing on macOS)

### Setup Steps

```bash
# Clone the repository
git clone https://github.com/kavi-thirilo/testzen-lite.git
cd testzen-lite

# Install Python dependencies
pip install -r requirements.txt

# Install Appium and drivers
npm install -g appium
appium driver install uiautomator2  # For Android
appium driver install xcuitest      # For iOS (macOS only)

# Verify installation
./testzen --help
```

---

## 🎯 Quick Start

### 1. Add Your Mobile App

```bash
# For Android
cp /path/to/your-app.apk apps/android/

# For iOS
cp /path/to/your-app.ipa apps/ios/
```

### 2. Run the Sample Test

```bash
# TestZen includes a ready-to-run sample test
./testzen run --file tests/android/billing/Payment_Form_Validation_Test.xlsx
```

### 3. View Results

After test execution:
- **HTML Report**: Open `reports/android_test_report.html` in your browser
- **Excel Report**: Check the Status column in your test Excel file

### 4. Create Your Own Tests

```bash
# Create a module for your tests
mkdir -p tests/android/your_module_name

# Create Excel test file with these columns:
# S.No | Description | Action | Locator Type | Locator Value | Locator Value 2 | Locator Value 3 | Input Data | Status | Result Message

# Run your tests
./testzen run --file tests/android/your_module_name/your_test.xlsx
```

---

## 📚 Available Commands

```bash
# Run specific test
./testzen run --file tests/android/billing/Payment_Form_Validation_Test.xlsx

# Run all tests
./testzen run --all --platform android

# List available tests
./testzen list --platform android

# Manage emulators
./testzen emulator list
./testzen emulator launch
./testzen emulator stop

# Show version
./testzen version
```

---

## 🔧 Advanced Options

```bash
# Continue on failure instead of stopping
./testzen run --file test.xlsx --skip-on-fail

# Disable screenshots
./testzen run --file test.xlsx --screenshots no

# Use specific device
./testzen run --file test.xlsx --device emulator-5554

# Launch specific AVD
./testzen run --file test.xlsx --avd Pixel_4_API_30

# Skip cleanup operations
./testzen run --file test.xlsx --no-cleanup
```

---

## 📖 Documentation

- **README**: [README.md](https://github.com/kavi-thirilo/testzen-lite/blob/main/README.md)
- **Multi-Locator Guide**: [docs/MULTI_LOCATOR_GUIDE.md](https://github.com/kavi-thirilo/testzen-lite/blob/main/docs/MULTI_LOCATOR_GUIDE.md)
- **Supported Actions**: [docs/SUPPORTED_ACTIONS.md](https://github.com/kavi-thirilo/testzen-lite/blob/main/docs/SUPPORTED_ACTIONS.md)
- **CI/CD Setup**: [CI_CD_SETUP.md](https://github.com/kavi-thirilo/testzen-lite/blob/main/CI_CD_SETUP.md)

---

## 🐛 Known Issues

None at this time. Please report any issues at: https://github.com/kavi-thirilo/testzen-lite/issues

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](https://github.com/kavi-thirilo/testzen-lite/blob/main/LICENSE) file for details.

---

## 🙏 Acknowledgments

Thank you to all contributors and users who helped make TestZen Lite possible!

---

**TestZen Lite v1.0.0** - Test Automation Made Simple

No coding required. Just Excel, your app, and automated tests.
