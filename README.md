# TestZen Lite - No-Code Test Automation

**Mobile test automation without writing a single line of code. Just Excel files and APK/IPA.**

> **Note:** This is the lite version of TestZen, focused on core mobile testing capabilities. Features removed: Appium Web Inspector, BDD Runner, API Testing, and demo examples.

## What is TestZen Lite?

TestZen Lite is a streamlined no-code mobile test automation framework that runs tests defined in Excel spreadsheets. Perfect for QA teams, testers, and anyone who wants automated mobile testing without programming.

### Key Features
- **No coding required** - Write tests in Excel
- **Multi-platform** - Supports Android and iOS
- **CI/CD ready** - Automated testing on GitHub Actions & GitLab CI
- **Modular testing** - Organize tests by feature modules
- **Professional reports** - HTML reports with screenshots
- **Smart element finding** - Handles WebView and native apps

---

## Installation

### Step 1: Clone the Repository

```bash
git clone https://github.com/kavi-thirilo/testzen-lite.git
cd testzen-lite
```

### Step 2: Install Python Dependencies

```bash
pip install -r requirements.txt
```

### Step 3: Install Appium

```bash
npm install -g appium
appium driver install uiautomator2  # For Android
appium driver install xcuitest      # For iOS (macOS only)
```

### Step 4: Verify Installation

```bash
python testzen.py --help
```

**What you get:**
```
testzen-lite/
├── apps/
│   ├── android/          # Place your APK here
│   └── ios/              # Place your IPA here
├── tests/
│   ├── android/          # Your Android test Excel files
│   └── ios/              # Your iOS test Excel files
├── reports/              # Generated test reports
├── testzen.py            # Main executable
├── .github/workflows/    # CI/CD ready to use
└── .gitlab-ci.yml        # GitLab CI configuration
```

---

## Getting Started

### 1. Remove Example Tests (Optional)

```bash
# Remove example test files
rm -rf tests/android/billing
rm -rf tests/android/login
rm -rf tests/android/checkout
```

### 2. Create Your Test Module

```bash
# Create module for your feature
mkdir -p tests/android/my_feature
```

### 3. Add Your Test Files

- Create Excel files following the format in [Quick Start](#quick-start)
- Copy to `tests/android/my_feature/my_test.xlsx`

### 4. Add Your Mobile App

```bash
# Copy your APK
cp /path/to/your-app.apk apps/android/

# Or copy your IPA
cp /path/to/your-app.ipa apps/ios/
```

### 5. Run Your First Test

```bash
python testzen.py run --file tests/android/my_feature/my_test.xlsx --platform android
```

### 6. Enable CI/CD (Optional)

Push to your GitHub/GitLab repository - tests will run automatically!

```bash
git remote set-url origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
git add .
git commit -m "Add my mobile tests"
git push -u origin main
```

---

## Quick Start

### 1. Test Organization

Place your test files in module folders:

```
tests/
├── android/
│   ├── login/                    # Login feature tests
│   │   └── login_test.xlsx
│   ├── checkout/                 # Checkout feature tests
│   │   └── checkout_test.xlsx
│   └── settings/                 # Settings tests
│       └── preferences_test.xlsx
├── ios/
│   ├── login/
│   │   └── login_test.xlsx
│   └── dashboard/
│       └── dashboard_test.xlsx
```

**Why modules?** Organize tests by features (login, checkout, profile) for better maintainability and reporting.

### 2. App Binaries

Place your mobile app files here:

```
apps/
├── android/
│   └── your-app.apk         # Android app to test
└── ios/
    └── your-app.ipa         # iOS app to test
```

**Important:** Only one APK or IPA per platform.

### 3. Excel Test Format

Your test Excel files should have these columns:

| Column | Name | Description | Example |
|--------|------|-------------|---------|
| **A** | Step Number | Sequential step number | 1, 2, 3... |
| **B** | Action | What to do | `click`, `input`, `verify` |
| **C** | Locator Type | How to find element | `xpath`, `id`, `accessibility id` |
| **D** | Locator Value | Element identifier | `//android.widget.Button[@text='Login']` |
| **E** | Input Data | Data to enter (for `input` action) | `testuser@example.com` |
| **F** | Expected Result | What should happen | `Login successful` |

**Example Test:**

| Step | Action | Locator Type | Locator Value | Input Data | Expected Result |
|------|--------|--------------|---------------|------------|-----------------|
| 1 | input | id | username_field | john@example.com | Username entered |
| 2 | input | id | password_field | password123 | Password entered |
| 3 | click | xpath | //android.widget.Button[@text='Login'] | | Button clicked |
| 4 | verify | id | welcome_message | | Login successful |

**Common Actions:**
- `input` - Type text into a field
- `click` - Tap an element
- `verify` - Check if element exists
- `wait` - Pause for seconds
- `scroll` - Scroll up/down

See [docs/SUPPORTED_ACTIONS.md](docs/SUPPORTED_ACTIONS.md) for complete list.

---

## Running Tests

### Local Execution

```bash
# Install dependencies
pip install -r requirements.txt

# Run a specific test
python testzen.py run --file tests/android/login/login_test.xlsx --platform android

# Run all Android tests
python testzen.py run --platform android

# Run all iOS tests
python testzen.py run --platform ios
```

**Prerequisites:**
- Android: Connected device/emulator with USB debugging enabled
- iOS: macOS with Xcode and connected device/simulator
- Appium installed (`npm install -g appium`)

### CI/CD Automation

Tests run automatically in your CI/CD pipeline:

#### GitHub Actions (Android)

**Triggers:**
- Every push to `main`, `develop` branches
- Pull requests to `main`
- Nightly at 9 PM CST
- Manual workflow dispatch

**Configuration:** `.github/workflows/android-tests.yml`

**What happens:**
1. Spins up Android emulator (API 29)
2. Installs your APK from `apps/android/`
3. Runs all tests in `tests/android/*/`
4. Generates HTML report
5. Uploads reports as artifacts

**View results:**
1. Go to **Actions** tab in GitHub
2. Click latest workflow run
3. Download **android-test-reports** artifact
4. Open `android_test_report.html`

#### GitLab CI (Android & iOS)

**Android - Automated:**
- Triggers on every push to `main`
- Uses GitLab runner with `android` tag
- Auto-runs if runner available

**iOS - Manual Trigger:**
- Go to **CI/CD → Pipelines**
- Click Play on `test:ios:manual` job
- Requires macOS runner with `ios` tag

**Why manual iOS?**
- iOS testing requires macOS runners with Xcode
- Not all GitLab instances have macOS runners
- Manual trigger prevents pipeline blocking

**Configuration:** `.gitlab-ci.yml`

**View results:**
1. Go to **CI/CD → Pipelines**
2. Click pipeline number
3. Click **Browse** under job artifacts
4. Open `reports/android_test_report.html`

---

## Multi-Module Testing

**Benefits:**
- Organize tests by feature (login, checkout, profile)
- Parallel test execution (future)
- Better reporting - see results per module
- Easier maintenance

**Example structure:**

```
tests/android/
├── login/              # All login-related tests
│   ├── basic_login.xlsx
│   └── social_login.xlsx
├── checkout/           # Shopping cart tests
│   ├── add_to_cart.xlsx
│   └── payment.xlsx
└── profile/            # User profile tests
    └── edit_profile.xlsx
```

**Report shows:**
- Total tests per module
- Pass/fail breakdown by module
- Execution time per module

---

## Understanding Test Reports

After test execution, find reports in:

**Local:** `reports/` directory
**CI/CD:** Download from artifacts

**Report includes:**
- Test summary (passed/failed/total)
- Step-by-step execution details
- Screenshots of each step
- Execution time and timestamps
- Device and app information

---

## Troubleshooting

### Tests failing locally?

1. **Check device connection:**
   ```bash
   adb devices  # Should show your device
   ```

2. **Check Appium server:**
   ```bash
   appium  # Should start on port 4723
   ```

3. **Check APK exists:**
   ```bash
   ls apps/android/*.apk  # Should show your APK
   ```

### CI/CD tests failing?

1. **Check runner availability:**
   - Android: Needs runner with `android` tag
   - iOS: Needs macOS runner with `ios`, `macos` tags

2. **Check APK/IPA committed:**
   ```bash
   git ls-files apps/android/
   git ls-files apps/ios/
   ```

3. **Check workflow logs:**
   - GitHub: Actions → Click workflow → View logs
   - GitLab: CI/CD → Pipelines → Click job → View logs

### Element not found errors?

1. **Use correct locator type:**
   - Android: `xpath`, `id`, `accessibility id`
   - iOS: `xpath`, `accessibility id`, `class name`

2. **Verify locator in app:**
   - Use Appium Inspector to find correct locators
   - Test locators before adding to Excel

3. **Add wait time:**
   - Use `wait` action before finding elements
   - Default: Framework waits 10s automatically

---

## Advanced Configuration

### Custom Wait Times

Edit `config/wait_config.py`:

```python
DEFAULT_WAIT = 10        # Element find timeout
WEBVIEW_WAIT = 30        # WebView load timeout
CI_MULTIPLIER = 1.5      # Extra time in CI/CD
```

### Test Execution Control

**Skip tests:** Rename folder to start with underscore:
```
tests/android/_disabled_module/  # Won't run
```

**Run specific module:**
```bash
python testzen.py run --file tests/android/login/*.xlsx --platform android
```

---

## Complete Documentation

- **[Supported Actions](docs/SUPPORTED_ACTIONS.md)** - All available test actions
- **[Multi-Module Guide](MULTI_MODULE_GUIDE.md)** - Detailed module organization
- **[CI/CD Setup](CI_CD_SETUP.md)** - Complete CI/CD configuration guide
- **[CLI Usage](docs/CLI_USAGE.md)** - Command line interface guide

---

## Example Test Scenarios

**Login Test** (`tests/android/login/login_test.xlsx`):

| Step | Action | Locator Type | Locator Value | Input Data | Expected |
|------|--------|--------------|---------------|------------|----------|
| 1 | input | xpath | //android.widget.EditText[@resource-id="username"] | testuser@example.com | Username entered |
| 2 | input | xpath | //android.widget.EditText[@resource-id="password"] | Test@123 | Password entered |
| 3 | click | xpath | //android.widget.Button[@text='Sign In'] | | Button clicked |
| 4 | wait | | | 3 | Wait for login |
| 5 | verify | id | welcome_message | | Login success |

**Form Validation** (`tests/android/checkout/checkout_test.xlsx`):

| Step | Action | Locator Type | Locator Value | Input Data | Expected |
|------|--------|--------------|---------------|------------|----------|
| 1 | click | id | checkout_button | | Navigate to checkout |
| 2 | input | xpath | //android.widget.EditText[@hint='Card Number'] | 4111111111111111 | Card entered |
| 3 | input | xpath | //android.widget.EditText[@hint='CVV'] | 123 | CVV entered |
| 4 | click | id | submit_payment | | Payment submitted |
| 5 | verify | xpath | //*[@text='Payment Successful'] | | Payment confirmed |

---

## Need Help?

- **Issues:** [GitHub Issues](https://github.com/kavi-thirilo/testzen/issues)
- **Documentation:** Check `docs/` folder
- **CI/CD Guide:** See [CI_CD_SETUP.md](CI_CD_SETUP.md)

---

**TestZen - Test Automation Made Simple**
