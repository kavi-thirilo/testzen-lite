# TestZen Lite - No-Code Test Automation

**Mobile test automation without writing a single line of code. Just Excel files and APK/IPA.**

---

## Table of Contents

- [What is TestZen Lite?](#what-is-testzen-lite)
- [Key Features](#key-features)
- [Quick Start Guide](#quick-start-guide)
  - [Step 1: Installation](#step-1-installation)
    - [Android Setup](#android-setup-required-for-android-testing)
  - [Step 2: Add Your Mobile App](#step-2-add-your-mobile-app)
  - [Step 3: Create Test Files](#step-3-create-test-files)
  - [Step 4: Run Tests](#step-4-run-tests)
- [Creating Excel Test Files](#creating-excel-test-files)
  - [Excel File Structure](#excel-file-structure)
  - [Available Test Actions](#available-test-actions)
  - [Multi-Locator Fallback System](#multi-locator-fallback-system)
- [Running Tests](#running-tests)
  - [Local Test Execution](#local-test-execution)
  - [Auto-Appium Mode](#auto-appium-mode)
  - [Command Line Options](#command-line-options)
  - [Emulator Management](#emulator-management)
- [Viewing Test Reports](#viewing-test-reports)
  - [HTML Report](#html-report)
  - [Excel Report](#excel-report)
  - [Understanding Results](#understanding-results)
- [CI/CD Integration](#cicd-integration)
  - [GitHub Actions](#github-actions)
  - [GitLab CI](#gitlab-ci)
- [Test Organization](#test-organization)
- [Troubleshooting](#troubleshooting)
  - [Installation Issues](#installation-issues)
  - [Android SDK Issues](#android-sdk-issues)
  - [Tests Not Running](#tests-not-running)
  - [Element Not Found Errors](#element-not-found-errors)
  - [Slow Test Execution](#slow-test-execution)
  - [CI/CD Failures](#cicd-failures)
- [Advanced Features](#advanced-features)
- [Complete Documentation](#complete-documentation)

---

## What is TestZen Lite?

TestZen Lite is a no-code mobile test automation framework that allows you to create and run automated tests for Android and iOS apps using Excel spreadsheets. No programming knowledge required.

**Perfect for:**
- QA Testers without programming experience
- Manual testers wanting to automate their tests
- Teams needing quick test automation setup
- Projects requiring CI/CD automated testing

---

## Key Features

- **No Coding Required** - Write all test steps in Excel spreadsheets
- **Multi-Platform** - Test both Android and iOS applications
- **Auto-Appium** - Automatic Appium server management (no manual setup needed)
- **Multi-Locator Fallback** - Automatic fallback to alternative element locators
- **Professional Reports** - HTML and Excel reports with screenshots
- **CI/CD Ready** - Pre-configured for GitHub Actions and GitLab CI
- **Smart Element Finding** - Handles both native and WebView applications
- **Module Organization** - Organize tests by features for better maintenance

---

## Quick Start Guide

### Step 1: Installation

**Prerequisites:**
- Python 3.8 or higher installed on your computer
- Node.js and npm installed (for Appium)
- Android SDK and platform tools (see Android Setup below)
- Android device or emulator - TestZen starts emulators automatically if needed

**Android Setup (Required for Android Testing):**

If you don't have Android development tools set up, you'll need:

**Option 1: Install Android Studio (Easiest - Recommended for beginners)**
1. Download Android Studio from https://developer.android.com/studio
2. Install and open Android Studio
3. Go to Settings → Appearance & Behavior → System Settings → Android SDK
4. Install Android SDK Platform-Tools and Android SDK Command-line Tools
5. Create an emulator: Tools → Device Manager → Create Virtual Device
6. Add to your PATH (add to ~/.zshrc or ~/.bashrc):
   ```bash
   export ANDROID_HOME=$HOME/Library/Android/sdk  # macOS
   # OR export ANDROID_HOME=$HOME/Android/Sdk      # Linux
   # OR set ANDROID_HOME=C:\Users\YOUR_USERNAME\AppData\Local\Android\Sdk  # Windows
   export PATH=$PATH:$ANDROID_HOME/platform-tools
   export PATH=$PATH:$ANDROID_HOME/emulator
   export PATH=$PATH:$ANDROID_HOME/tools/bin
   ```
7. Reload terminal: `source ~/.zshrc` or `source ~/.bashrc`
8. Verify: `adb version` and `emulator -list-avds`

**Option 2: Install Android Command Line Tools Only (Lighter weight)**
1. Download command line tools from https://developer.android.com/studio#command-tools
2. Extract to a folder (e.g., ~/android-sdk)
3. Set ANDROID_HOME and PATH as shown above
4. Install platform-tools: `sdkmanager "platform-tools" "emulator"`
5. Create emulator using avdmanager command

**Verify Android Setup:**
```bash
adb version              # Should show Android Debug Bridge version
emulator -list-avds      # Should list available emulators (may be empty if none created)
echo $ANDROID_HOME       # Should show path to Android SDK
```

**1.1 Clone or Download the Repository**

```bash
git clone https://github.com/kavi-thirilo/testzen-lite.git
cd testzen-lite
```

**1.2 Install Python Dependencies**

```bash
# Try one of these commands (use whichever works on your system):
pip3 install -r requirements.txt

# OR if pip3 doesn't work:
python3 -m pip install -r requirements.txt

# OR on Windows:
python -m pip install -r requirements.txt
```

**1.3 Install Appium**

```bash
npm install -g appium
appium driver install uiautomator2  # For Android testing
appium driver install xcuitest      # For iOS testing (macOS only)
```

**1.4 Verify Installation**

```bash
./testzen --help
```

You should see the TestZen help menu with available commands.

---

### Step 2: Add Your Mobile App

**For Android (APK):**

1. Locate your Android APK file (usually in `app/build/outputs/apk/` folder of your Android project)
2. Copy the APK file to the `apps/android/` folder in TestZen

```bash
cp /path/to/your-app.apk apps/android/
```

**For iOS (IPA):**

1. Locate your iOS IPA file
2. Copy the IPA file to the `apps/ios/` folder in TestZen

```bash
cp /path/to/your-app.ipa apps/ios/
```

**Important Notes:**
- Place only ONE APK file in the `apps/android/` folder
- Place only ONE IPA file in the `apps/ios/` folder
- TestZen will automatically detect and use your app file
- Debug APKs work automatically (common when building from Android Studio)

---

### Step 3: Create Test Files

**3.1 Use the Sample Test (Recommended for First-Time Users)**

TestZen includes a sample test file you can run immediately:

```bash
# The sample test is already included at:
# tests/android/billing/Payment_Form_Validation_Test.xlsx
```

**3.2 Create a New Test Module**

To create your own tests, organize them by feature (e.g., billing, login, checkout):

```bash
# Create a module folder for your feature
mkdir -p tests/android/your_module_name
```

**3.3 Create an Excel Test File**

1. Open Excel or Google Sheets
2. Create a new spreadsheet with these columns:

| S.No | Description | Action | Locator Type | Locator Value | Locator Value 2 | Locator Value 3 | Input Data | Status | Result Message |
|------|-------------|--------|--------------|---------------|-----------------|-----------------|------------|--------|----------------|

3. Add your test steps (see [Creating Excel Test Files](#creating-excel-test-files) for details)
4. Save as `your_test.xlsx` in `tests/android/your_module_name/` folder

**Example Test File Structure:**

| S.No | Description | Action | Locator Type | Locator Value | Locator Value 2 | Locator Value 3 | Input Data | Status | Result Message |
|------|-------------|--------|--------------|---------------|-----------------|-----------------|------------|--------|----------------|
| 1 | Enter email | input | xpath | //android.widget.EditText[@hint="Email"] | //android.widget.EditText[@resource-id="email"] | //android.widget.EditText[1] | test@example.com | | |
| 2 | Enter card number | input | xpath | //android.widget.EditText[@hint="Card Number"] | //android.widget.EditText[@resource-id="cardNumber"] | //android.widget.EditText[2] | 4111111111111111 | | |
| 3 | Click Submit | click | xpath | //android.widget.Button[@text='Submit Payment'] | //android.widget.Button[@resource-id="submit"] | | | | |
| 4 | Verify success | verify | id | success_message | xpath | //android.widget.TextView[@text='Payment Successful'] | | | |

**Note:** See the actual sample test file at `tests/android/billing/Payment_Form_Validation_Test.xlsx` for a working example.

---

### Step 4: Run Tests

**No device or emulator running? No problem!** TestZen can auto-launch emulators for you.

**Option A: Using ./testzen Script (Recommended - Auto-Appium)**

The `./testzen` script automatically manages the Appium server for you:

```bash
# Run the sample billing test
./testzen run --file tests/android/billing/Payment_Form_Validation_Test.xlsx

# Or run your own test
./testzen run --file tests/android/your_module/your_test.xlsx
```

**Option B: Manual Python Command**

If you prefer to manage Appium yourself:

```bash
# Start Appium server manually in one terminal
appium

# Run tests in another terminal
python testzen.py run --file tests/android/billing/Payment_Form_Validation_Test.xlsx
```

**View Results:**

After test execution completes:
- HTML Report: Open `reports/android_test_report.html` in your browser
- Excel Report: Check the Status column in your test Excel file

---

## Creating Excel Test Files

### Excel File Structure

Your Excel test file must have two sheets:

**Sheet 1: Test_Summary** (auto-generated, you can leave empty)
**Sheet 2: TestCases** (your test steps go here)

### TestCases Sheet Columns

| Column | Name | Required | Description | Example |
|--------|------|----------|-------------|---------|
| A | S.No | Yes | Step number (1, 2, 3...) | 1 |
| B | Description | Yes | What this step does | Enter username |
| C | Action | Yes | Type of action to perform | input |
| D | Locator Type | Yes | How to find the element | xpath |
| E | Locator Value | Yes | Primary locator | //android.widget.EditText[@hint="Username"] |
| F | Locator Value 2 | No | Fallback locator 1 | //android.widget.EditText[@resource-id="username"] |
| G | Locator Value 3 | No | Fallback locator 2 | //android.widget.EditText[1] |
| H | Input Data | No | Data to enter (for input action) | john@example.com |
| I | Status | No | Auto-filled after test run | PASSED/FAILED |
| J | Result Message | No | Auto-filled after test run | Error details |

### Available Test Actions

| Action | Description | Requires Locator | Requires Input Data | Example |
|--------|-------------|------------------|---------------------|---------|
| **input** | Type text into a field | Yes | Yes | Enter email address |
| **click** | Tap/click an element | Yes | No | Click login button |
| **verify** | Check if element exists | Yes | No | Verify welcome message |
| **wait** | Pause for X seconds | No | Yes (seconds) | Wait 3 seconds |
| **scroll** | Scroll up or down | No | Yes (up/down) | Scroll down |
| **swipe** | Swipe in a direction | No | Yes (left/right/up/down) | Swipe left |
| **long_press** | Long press an element | Yes | No | Long press menu item |

**See [docs/SUPPORTED_ACTIONS.md](docs/SUPPORTED_ACTIONS.md) for complete list of actions.**

### Multi-Locator Fallback System

TestZen supports up to 3 fallback locators per step. This ensures tests work across different environments (local, CI/CD, different Android versions).

**Why use multiple locators?**
- Local environment might find elements using `@hint` attribute
- CI/CD environment might need `@resource-id` instead
- Provides automatic fallback if primary locator fails

**How it works:**
1. TestZen tries **Locator Value** first
2. If that fails, tries **Locator Value 2**
3. If that fails, tries **Locator Value 3**
4. Reports show which locator succeeded

**Example:**

| Locator Value | Locator Value 2 | Locator Value 3 |
|---------------|-----------------|-----------------|
| //android.widget.EditText[@hint="Password"] | //android.widget.EditText[@resource-id="password"] | //android.widget.EditText[2] |

**See [docs/MULTI_LOCATOR_GUIDE.md](docs/MULTI_LOCATOR_GUIDE.md) for detailed guide.**

---

## Running Tests

### Local Test Execution

**Prerequisites for Local Testing:**

**For Android:**
- Android device connected via USB with USB debugging enabled, OR
- Android emulator configured (doesn't need to be running - TestZen starts it automatically)
- Optional: Run `adb devices` to verify device status

**For iOS:**
- macOS computer (required for iOS testing)
- Xcode installed
- iOS simulator or iOS device connected
- Optional: Run `xcrun simctl list` to see available simulators

### Auto-Appium Mode

The easiest way to run tests - The `./testzen` script automatically handles the Appium server:

**Run a specific test:**
```bash
./testzen run --file tests/android/billing/Payment_Form_Validation_Test.xlsx
```

**Run all tests in a module:**
```bash
./testzen run --file tests/android/billing/*.xlsx
```

**Run using Python directly with auto-appium flag:**
```bash
python testzen.py run --file tests/android/billing/Payment_Form_Validation_Test.xlsx --auto-appium
```

### Command Line Options

**Basic Commands:**

```bash
# Run specific test file (recommended - auto-manages Appium)
./testzen run --file tests/android/billing/Payment_Form_Validation_Test.xlsx

# Run all Android tests
./testzen run --all --platform android

# Run all iOS tests
./testzen run --all --platform ios

# List available tests
./testzen list --platform android
```

**Advanced Options:**

```bash
# Auto-start Appium server
--auto-appium

# Keep Appium running after tests
--keep-appium

# Disable screenshots
--screenshots no

# Continue on failure instead of stopping
--skip-on-fail

# Use specific device
--device emulator-5554

# Launch specific emulator
--avd Pixel_4_API_30

# Disable auto-launch emulator
--no-auto-launch
```

**Example with Multiple Options:**

```bash
./testzen run --file tests/android/billing/Payment_Form_Validation_Test.xlsx --skip-on-fail --device emulator-5554
```

### Emulator Management

TestZen provides simple commands to manage Android emulators:

```bash
# See what emulators are available and which are running
./testzen emulator list

# Start an emulator (picks the first available)
./testzen emulator launch

# Start a specific emulator
./testzen emulator launch --avd Pixel_4_API_30

# Stop the running emulator
./testzen emulator stop
```

**Note:** You don't need to manually launch emulators before running tests - TestZen does this automatically when needed.

---

## Viewing Test Reports

After running tests, TestZen generates two types of reports:

### HTML Report

**Location:** `reports/android_test_report.html` or `reports/ios_test_report.html`

**How to view:**
1. Navigate to the `reports/` folder
2. Double-click the HTML file to open in your browser
3. View detailed test results with screenshots

**What's included:**
- Test execution summary (passed/failed/total)
- Step-by-step execution details
- Screenshots for each step (before and after)
- Exact locator used for each element
- Execution time for each step
- Error messages for failed steps
- Device and app information

**Understanding the Report:**

- **Green rows** = Passed steps
- **Red rows** = Failed steps
- **Yellow rows** = Skipped steps
- **Locator info** shows which fallback locator was used (if any)
  - Example: `xpath: //EditText[@resource-id="password"] (Found on attempt 2/3 from Locator Value 2)`

### Excel Report

**Location:** Your original test Excel file

**How to view:**
1. Open your test Excel file (e.g., `tests/android/login/login_test.xlsx`)
2. Look at the TestCases sheet
3. Check the **Status** and **Result Message** columns

**Status Column Colors:**
- **Light Green** = PASSED (dark green text)
- **Light Red** = FAILED (dark red text)
- **Light Yellow** = SKIP (dark yellow text)

**Test_Summary Sheet:**
- Automatically generated after test run
- Shows overall test statistics
- Execution date and time
- Pass/fail breakdown
- Success rate percentage

### Understanding Results

**Test Passed:**
- Status shows PASSED in green
- HTML report shows green rows
- All test steps completed successfully

**Test Failed:**
- Status shows FAILED in red
- HTML report shows red row for failed step
- Result Message shows error details
- Screenshot shows app state when failure occurred

**Common Failure Reasons:**
- Element not found (locator incorrect or element not visible)
- Timeout (element took too long to appear)
- App crash or unexpected behavior
- Network issues

---

## CI/CD Integration

TestZen comes pre-configured for automated testing in CI/CD pipelines.

### GitHub Actions

**Automatic Setup:**
- Already configured in `.github/workflows/android-tests.yml`
- Tests run automatically on every push to `main` or `develop`
- Tests run on all pull requests to `main`
- Nightly tests at 9 PM CST

**How to Enable:**

1. Push your code to GitHub:
```bash
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
git add .
git commit -m "Add automated tests"
git push -u origin main
```

2. GitHub Actions will automatically run your tests

**View Results:**

1. Go to your GitHub repository
2. Click the **Actions** tab
3. Click on the latest workflow run
4. Download the **android-test-reports** artifact (ZIP file)
5. Extract and open `android_test_report.html`

**What Happens in GitHub Actions:**
1. Spins up Ubuntu runner with Android emulator
2. Installs your APK from `apps/android/` folder
3. Runs all test files in `tests/android/` folders
4. Generates HTML report
5. Uploads report as downloadable artifact
6. Marks build as passed/failed based on test results

### GitLab CI

**Automatic Setup:**
- Already configured in `.gitlab-ci.yml`
- Android tests run automatically on push to `main`
- iOS tests are manual trigger (requires macOS runner)

**How to Enable:**

1. Push your code to GitLab:
```bash
git remote add origin https://gitlab.com/YOUR_USERNAME/YOUR_REPO.git
git add .
git commit -m "Add automated tests"
git push -u origin main
```

2. GitLab CI will automatically run Android tests if runner is available

**View Results:**

1. Go to your GitLab repository
2. Click **CI/CD** → **Pipelines**
3. Click on the latest pipeline
4. Click **Browse** next to job artifacts
5. Navigate to `reports/` folder
6. Download and open `android_test_report.html`

**iOS Testing (Manual Trigger):**

1. Go to **CI/CD** → **Pipelines**
2. Click the play button on `test:ios:manual` job
3. Wait for macOS runner to execute tests
4. Download artifacts same as Android

**Why is iOS manual?**
- iOS testing requires macOS runners with Xcode
- Not all GitLab instances have macOS runners
- Manual trigger prevents blocking the pipeline

---

## Test Organization

Organize your tests by features/modules for better maintainability:

**Current Structure (with sample):**

```
tests/
├── android/
│   ├── billing/                 # Payment and billing tests (SAMPLE INCLUDED)
│   │   └── Payment_Form_Validation_Test.xlsx
│   ├── login/                   # Authentication tests (create your own)
│   │   ├── basic_login.xlsx
│   │   ├── social_login.xlsx
│   │   └── forgot_password.xlsx
│   ├── checkout/                # Shopping cart tests (create your own)
│   │   ├── add_to_cart.xlsx
│   │   ├── payment.xlsx
│   │   └── coupon_code.xlsx
│   └── profile/                 # User profile tests (create your own)
│       ├── edit_profile.xlsx
│       └── change_password.xlsx
└── ios/
    ├── login/
    │   └── login_test.xlsx
    └── dashboard/
        └── dashboard_test.xlsx
```

**Note:** The `billing` module contains a working sample test. Other modules shown are examples for you to create.

**Benefits:**
- Easy to find specific tests
- Better reporting (results grouped by module)
- Team collaboration (different people work on different modules)
- Easier maintenance (update related tests together)

**Module Reports:**
When you run all tests, the report shows results per module:
- Total tests in each module
- Pass/fail breakdown by module
- Execution time per module

---

## Troubleshooting

### Installation Issues

**Problem: "command not found: pip" error**

Solution:
```bash
# Try pip3 instead:
pip3 install -r requirements.txt

# If pip3 doesn't work, use Python module:
python3 -m pip install -r requirements.txt

# On Windows:
python -m pip install -r requirements.txt

# Still not working? Check if Python is installed:
python3 --version  # Should show Python 3.8 or higher
```

**Problem: npm permission error (EACCES) when installing Appium**

Error message: `npm error code: 'EACCES'` or `The operation was rejected by your operating system`

Solution (Choose one):

```bash
# Option 1 (Recommended): Fix npm permissions permanently
# This allows npm to install global packages without sudo
mkdir ~/.npm-global
npm config set prefix '~/.npm-global'
echo 'export PATH=~/.npm-global/bin:$PATH' >> ~/.zshrc  # or ~/.bashrc
source ~/.zshrc  # or source ~/.bashrc

# Now install Appium without sudo:
npm install -g appium
appium driver install uiautomator2

# Option 2: Use npx (no global install needed)
# No installation required - npx downloads and runs Appium temporarily
# TestZen will use npx appium automatically if global appium not found

# Option 3: Use sudo (NOT recommended - security risk)
sudo npm install -g appium
```

**After fixing npm permissions, verify installation:**
```bash
appium --version  # Should show version number
which appium      # Should show path to appium
```

**Problem: "[Errno 2] No such file or directory: 'adb'" error**

Error message: `[Errno 2] No such file or directory: 'adb'`

**This means**: Android SDK is not installed or not in your PATH.

Solution:

```bash
# Step 1: Check if ANDROID_HOME is set
echo $ANDROID_HOME

# If empty, you need to install Android SDK and set ANDROID_HOME
# See "Android Setup" section in Step 1: Installation for complete instructions

# Quick fix if SDK is already installed but not in PATH:
export ANDROID_HOME=$HOME/Library/Android/sdk  # macOS
# OR export ANDROID_HOME=$HOME/Android/Sdk      # Linux

export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/emulator

# Make it permanent:
echo 'export ANDROID_HOME=$HOME/Library/Android/sdk' >> ~/.zshrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools' >> ~/.zshrc
source ~/.zshrc

# Verify:
adb version  # Should show Android Debug Bridge version
```

**Problem: "Could not find a driver for automationName 'UiAutomator2'" error**

Error message: `Could not find a driver for automationName 'UiAutomator2' and platformName 'Android'`

**This means**: The Appium uiautomator2 driver is not installed.

Solution:

```bash
# Install the driver
appium driver install uiautomator2

# Verify it's installed
appium driver list --installed

# Should show:
# - uiautomator2@...

# Now run your test again
./testzen run --file your_test.xlsx
```

**Problem: "Failed to start Appium server" error**

Error message: `[ERROR] Failed to start Appium server` or `Could not start Appium`

Solution:

```bash
# Step 1: Check if Appium is installed
appium --version

# If not installed, install it:
npm install -g appium
appium driver install uiautomator2

# Step 2: Check if port 4723 is already in use
lsof -i :4723  # macOS/Linux
netstat -ano | findstr :4723  # Windows

# If port is in use, kill the process:
kill -9 <PID>  # Replace <PID> with the process ID from above

# Step 3: Check detailed error in Appium log
cat /tmp/appium.log

# Step 4: Try starting Appium manually to see full error
appium --allow-insecure=*:chromedriver_autodownload

# Step 5: Verify Node.js version (needs 16+)
node --version  # Should be v16 or higher
```

**Common causes:**
- Appium not installed (fix npm permissions first)
- Port 4723 already in use by another Appium instance
- Node.js version too old (upgrade to v16+)
- Missing Appium drivers (run `appium driver install uiautomator2`)

**Problem: Appium 3.x error - "The full feature name must include both the destination automation name"**

Error message: `Fatal Error: The full feature name must include both the destination automation name or the '*' wildcard`

This happens with Appium 3.x because the `--allow-insecure` flag format changed.

Solution:

```bash
# Wrong (Appium 2.x format - will fail in Appium 3.x):
appium --allow-insecure=chromedriver_autodownload

# Correct (Appium 3.x format - works with both 2.x and 3.x):
appium --allow-insecure=*:chromedriver_autodownload

# Or specify driver explicitly:
appium --allow-insecure=uiautomator2:chromedriver_autodownload
```

**Note:** The `./testzen` script has been updated to work with both Appium 2.x and 3.x automatically. This issue only affects manual Appium startup.

### Android SDK Issues

**Problem: "adb: command not found" or "emulator: command not found"**

This means Android SDK is not installed or not in your PATH.

Solution:

```bash
# Check if ANDROID_HOME is set
echo $ANDROID_HOME

# If empty, Android SDK is not configured. You need to:
# 1. Install Android Studio (recommended) OR command line tools
# 2. Set ANDROID_HOME environment variable
# 3. Add SDK tools to PATH

# See "Android Setup" section in Step 1: Installation for detailed instructions
```

**Problem: "ANDROID_HOME environment variable not set"**

Solution:
```bash
# Find where Android SDK is installed:
# Common locations:
# macOS: ~/Library/Android/sdk
# Linux: ~/Android/Sdk
# Windows: C:\Users\YOUR_USERNAME\AppData\Local\Android\Sdk

# Set ANDROID_HOME (add to ~/.zshrc or ~/.bashrc):
export ANDROID_HOME=$HOME/Library/Android/sdk  # macOS
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/emulator

# Reload your terminal
source ~/.zshrc  # or source ~/.bashrc

# Verify
adb version
```

**Problem: No AVDs (emulators) available**

Error: TestZen can't launch emulator automatically because none exist.

Solution:
```bash
# Check available emulators
emulator -list-avds

# If empty, create one using Android Studio:
# 1. Open Android Studio
# 2. Tools → Device Manager
# 3. Click "Create Virtual Device"
# 4. Choose a device (e.g., Pixel 4)
# 5. Choose a system image (e.g., Android 11/API 30)
# 6. Finish setup

# Verify again
emulator -list-avds  # Should now show your emulator name

# Or use TestZen to list them
./testzen emulator list
```

### Tests Not Running

**Problem: "No device connected" error**

Solution:
```bash
# For Android
adb devices  # Should show at least one device

# If no devices shown:
# 1. Enable USB debugging on your Android device
# 2. Connect via USB
# 3. Accept USB debugging prompt on device

# If still no devices and no emulator:
# - You need Android SDK installed (see Android SDK Issues above)
# - Create an AVD emulator (see above)
# - OR connect a physical Android device with USB debugging enabled
```

**Problem: Hangs at "Checking device availability" even though emulator is running**

Error: TestZen hangs or shows no progress after emulator launches and appears available.

**Why this happens:**
- ADB server has stale connection to device
- Device is in "offline" or "unauthorized" state
- Device is booted but system services not fully ready

Solution:

```bash
# Option 1: Restart ADB server manually
adb kill-server
adb start-server
adb devices  # Check if device shows as "device" (not "offline" or "unauthorized")

# Option 2: If device shows as "unauthorized"
# - Look at emulator screen
# - Accept "Allow USB debugging" prompt
# - If no prompt, run: adb kill-server && adb start-server

# Option 3: If device shows as "offline"
# - Close the emulator completely
# - Wait 10 seconds
# - Start emulator again: ./testzen emulator launch

# Option 4: If issue persists, run test with enhanced logging
# The updated TestZen now automatically restarts adb and shows detailed progress
./testzen run --file your_test.xlsx

# You should now see:
# [TZ] Restarting adb server to ensure clean connection...
# [TZ] Waiting for device emulator-5554 to be recognized by adb...
# [TZ] Device emulator-5554 detected by adb
# [TZ] Check #1: Testing device responsiveness...
```

**TestZen v1.1.1+ includes automatic fixes:**
- Automatically restarts adb server before checking device
- Shows check count and progress every 5 seconds
- Waits for device to appear in `adb devices` list
- More detailed logging to identify where hang occurs

**Problem: "Appium server not running" error**

Solution:
```bash
# Option 1: Use ./testzen script (auto-manages Appium)
./testzen run --file your_test.xlsx

# Option 2: Use Python with --auto-appium flag
python testzen.py run --file your_test.xlsx --auto-appium

# Option 3: Start Appium manually
appium  # In a separate terminal window
```

**Problem: "APK not found" error**

Solution:
```bash
# Check if APK exists
ls apps/android/*.apk

# If no APK found, copy your APK:
cp /path/to/your-app.apk apps/android/
```

### Element Not Found Errors

**Problem: Test fails with "Element not found"**

Solution:
1. Use Appium Inspector to find correct locator
2. Add multiple fallback locators (Locator Value 2, Locator Value 3)
3. Add a `wait` step before trying to find the element
4. Verify the element is visible on screen (not scrolled off-screen)

**Example Fix:**

| S.No | Description | Action | Locator Type | Locator Value | Locator Value 2 | Locator Value 3 |
|------|-------------|--------|--------------|---------------|-----------------|-----------------|
| 1 | Wait for login screen | wait | | | | 3 |
| 2 | Enter username | input | xpath | //EditText[@hint="Username"] | //EditText[@resource-id="username"] | //EditText[1] |

### Slow Test Execution

**Problem: Tests run very slowly**

Solutions:
1. Reduce wait times in test steps
2. Use `--no-cleanup` to skip cleanup operations
3. Disable screenshots: `--screenshots no`
4. Use a faster emulator or real device

### CI/CD Failures

**Problem: Tests pass locally but fail in CI/CD**

Common causes:
1. **Different locators work in different environments**
   - Solution: Use multi-locator fallback system
   - Add resource-id based locators as Locator Value 2

2. **Timing issues (elements appear slower in CI)**
   - Solution: Add wait steps before critical elements
   - Increase timeout values

3. **APK not committed to repository**
   - Solution: Ensure APK is committed:
   ```bash
   git add apps/android/your-app.apk
   git commit -m "Add APK for testing"
   git push
   ```

---

## Advanced Features

### Auto-Appium Server Management

TestZen can automatically start and stop the Appium server:

```bash
# Recommended: Use ./testzen script (auto-manages Appium)
./testzen run --file test.xlsx

# Alternative: Use Python with --auto-appium flag
python testzen.py run --file test.xlsx --auto-appium

# Keep Appium running after tests (for multiple test runs)
python testzen.py run --file test.xlsx --auto-appium --keep-appium
```

**Benefits:**
- No need to manually start Appium server
- Server automatically stopped after tests
- Saves time and reduces setup complexity

### Emulator Management

See the [Emulator Management](#emulator-management) section for details on managing Android emulators with TestZen commands

### Skip on Failure Mode

Continue running tests even if a step fails:

```bash
./testzen run --file test.xlsx --skip-on-fail
```

**When to use:**
- Running multiple independent test steps
- Want to see all failures, not just the first one
- Testing in development/debugging mode

### Custom Wait Times

Edit `config/wait_config.py` to adjust wait times:

```python
DEFAULT_WAIT = 10        # Element find timeout (seconds)
WEBVIEW_WAIT = 30        # WebView load timeout (seconds)
CI_MULTIPLIER = 1.5      # Extra time multiplier for CI/CD
```

---

## Complete Documentation

**Guides:**
- [Multi-Locator Guide](docs/MULTI_LOCATOR_GUIDE.md) - Complete guide for fallback locators
- [Supported Actions](docs/SUPPORTED_ACTIONS.md) - All available test actions
- [CLI Usage](docs/CLI_USAGE.md) - Command line reference
- [CI/CD Setup](CI_CD_SETUP.md) - Detailed CI/CD configuration

**Support:**
- [GitHub Issues](https://github.com/kavi-thirilo/testzen-lite/issues) - Report bugs or request features
- [Documentation](docs/) - Additional guides and references

---

**TestZen - Test Automation Made Simple**

No coding required. Just Excel, your app, and automated tests.
