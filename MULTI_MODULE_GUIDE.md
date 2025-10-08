# TestZen Multi-Module Testing Guide

## Overview

TestZen now supports **multi-module testing**, allowing you to organize tests by feature modules within your Android and iOS projects. Each module can have its own set of test files, and reports are generated per platform with all modules included.

---

## Folder Structure

```
testzen/
├── tests/
│   ├── android/
│   │   ├── login/              # Login module
│   │   │   ├── login_test.xlsx
│   │   │   └── oauth_login_test.xlsx
│   │   ├── checkout/           # Checkout module
│   │   │   ├── cart_test.xlsx
│   │   │   └── payment_test.xlsx
│   │   ├── billing/            # Billing module
│   │   │   └── Payment_Form_Validation_Test.xlsx
│   │   └── dashboard/          # Dashboard module
│   │       └── dashboard_test.xlsx
│   └── ios/
│       ├── login/              # Login module (iOS)
│       │   └── login_test.xlsx
│       ├── dashboard/          # Dashboard module (iOS)
│       │   └── dashboard_test.xlsx
│       └── settings/           # Settings module (iOS)
│           └── settings_test.xlsx
├── build/
│   ├── android/
│   │   └── apk/
│   │       └── app.apk         # Your Android APK
│   └── ios/
│       └── ipa/
│           └── app.ipa         # Your iOS IPA
└── reports/
    ├── android_test_report.html     # Generated Android report
    ├── android_test_report.json
    ├── ios_test_report.html          # Generated iOS report
    └── ios_test_report.json
```

---

## Creating Modules

### Android Modules

1. Create a new folder under `tests/android/`:
   ```bash
   mkdir tests/android/my_new_module
   ```

2. Add Excel test files to the module:
   ```bash
   cp my_test.xlsx tests/android/my_new_module/
   ```

3. Commit and push:
   ```bash
   git add tests/android/my_new_module/
   git commit -m "Add my_new_module tests for Android"
   git push
   ```

4. GitHub Actions will automatically detect and run tests in the new module

### iOS Modules

1. Create a new folder under `tests/ios/`:
   ```bash
   mkdir tests/ios/my_new_module
   ```

2. Add Excel test files:
   ```bash
   cp my_test.xlsx tests/ios/my_new_module/
   ```

3. Commit and push:
   ```bash
   git add tests/ios/my_new_module/
   git commit -m "Add my_new_module tests for iOS"
   git push
   ```

4. Manually trigger the iOS test job in GitLab CI/CD

---

## Module Naming Best Practices

### Good Module Names
- `login` - Login and authentication
- `checkout` - Shopping cart and checkout flow
- `billing` - Payment and billing
- `dashboard` - Main dashboard screens
- `profile` - User profile management
- `settings` - App settings and preferences
- `notifications` - Push notifications and alerts
- `search` - Search functionality
- `onboarding` - User onboarding flow

### Avoid
- `tests` - Too generic
- `module1`, `module2` - Not descriptive
- `test_cases` - Redundant
- Special characters or spaces

---

## How Test Execution Works

### Android (GitHub Actions)

**Automatic execution on:**
- Every push to main/develop branches
- Pull requests
- Nightly at 9 PM CST
- Manual workflow dispatch

**Execution flow:**
1. GitHub Actions starts Android emulator (API 29)
2. Installs APK from `apps/android/*.apk` (if exists)
3. Starts Appium server
4. **Scans** `tests/android/*/` for all modules
5. **For each module:**
   - Lists all `.xlsx` files
   - Runs each test sequentially
   - Logs results
6. **Generates HTML report** with all modules organized by name
7. Uploads reports as artifacts (30-day retention)

**Example log output:**
```
=========================================
Scanning for test modules...
=========================================

MODULE: login
-----------------------------------------
  Test: login_test.xlsx
  Running test...
  ✓ Test passed

  Test: oauth_login_test.xlsx
  Running test...
  ✓ Test passed

MODULE: checkout
-----------------------------------------
  Test: cart_test.xlsx
  Running test...
  ✓ Test passed

  Test: payment_test.xlsx
  Running test...
  ✗ Test failed

=========================================
TEST EXECUTION SUMMARY
=========================================
Modules scanned: 2
Tests executed: 4
=========================================

Generating module-based test report...
✓ HTML report saved: reports/android_test_report.html
✓ JSON report saved: reports/android_test_report.json
```

### iOS (GitLab Manual)

**Manual execution:**
1. Go to GitLab → CI/CD → Pipelines
2. Find latest pipeline
3. Click **Play** on `test:ios:manual` job

**Execution flow:**
1. Checks for IPA in `apps/ios/*.ipa`
2. Installs IPA on iOS device/simulator (if available)
3. **Scans** `tests/ios/*/` for all modules
4. **For each module:**
   - Lists all `.xlsx` files
   - Runs each test sequentially
   - Logs results
5. **Generates HTML report** with all modules
6. Uploads reports as GitLab artifacts (30-day retention)

---

## Generated Reports

### HTML Report Features

The generated HTML report includes:

- **Platform badge** (Android/iOS)
- **Summary cards**:
  - Total modules
  - Total test files
  - Platform name
- **Module sections**:
  - Each module has its own card
  - Shows test count per module
  - Lists all test files
  - Test status (if available)
- **Responsive design** - works on mobile/desktop
- **Modern UI** - gradient header, cards, badges

### Example Report Structure

```
╔════════════════════════════════════════╗
║     TestZen Test Report - ANDROID       ║
║     Generated: 2025-10-05 05:00:00     ║
╚════════════════════════════════════════╝

┌─────────┐  ┌─────────┐  ┌─────────┐
│    3    │  │    12   │  │ ANDROID │
│ Modules │  │  Tests  │  │Platform │
└─────────┘  └─────────┘  └─────────┘

┌─ login ──────────────── 2 test(s) ─┐
│ ✓ login_test.xlsx                  │
│ ✓ oauth_login_test.xlsx            │
└────────────────────────────────────┘

┌─ checkout ──────────────── 2 test(s) ─┐
│ ✓ cart_test.xlsx                      │
│ ✗ payment_test.xlsx                   │
└────────────────────────────────────────┘

┌─ billing ──────────────── 1 test(s) ─┐
│ ✓ Payment_Form_Validation_Test.xlsx  │
└───────────────────────────────────────┘
```

### Accessing Reports

**GitHub Actions (Android):**
1. Go to repository → Actions tab
2. Click on latest workflow run
3. Scroll to **Artifacts** section
4. Download `android-test-reports-api-29`
5. Extract and open `android_test_report.html`

**GitLab CI/CD (iOS):**
1. Go to GitLab → CI/CD → Pipelines
2. Click on pipeline
3. Find `test:ios:manual` job
4. Click **Browse** under Artifacts
5. Open `reports/ios_test_report.html`

---

## Adding APK/IPA Files

### Android APK

**Option 1: Commit APK** (for small APKs < 50MB)
```bash
cp path/to/app-debug.apk apps/android/
git add apps/android/app-debug.apk
git commit -m "Add Android APK for automated testing"
git push
```

**Option 2: Use Git LFS** (for large APKs)
```bash
# Install Git LFS
git lfs install

# Track APK files
git lfs track "apps/android/*.apk"
git add .gitattributes

# Add APK
cp path/to/app.apk apps/android/
git add apps/android/app.apk
git commit -m "Add Android APK via Git LFS"
git push
```

**Option 3: Build in Pipeline** (recommended)
Add a build step in `.github/workflows/android-tests.yml` before tests:
```yaml
- name: Build Android APK
  run: |
    cd android
    ./gradlew assembleDebug
    cp app/build/outputs/apk/debug/app-debug.apk ../apps/android/
```

### iOS IPA

**Option 1: Commit IPA**
```bash
cp path/to/app.ipa apps/ios/
git add apps/ios/app.ipa
git commit -m "Add iOS IPA for manual testing"
git push
```

**Option 2: Use Git LFS**
```bash
git lfs track "apps/ios/*.ipa"
git add .gitattributes apps/ios/app.ipa
git commit -m "Add iOS IPA via Git LFS"
git push
```

---

## Test File Organization

### Module Guidelines

**Group tests by feature/functionality:**

**Good organization:**
```
tests/android/
├── login/
│   ├── basic_login.xlsx
│   ├── social_login.xlsx
│   └── password_reset.xlsx
├── checkout/
│   ├── add_to_cart.xlsx
│   ├── apply_coupon.xlsx
│   └── complete_purchase.xlsx
└── profile/
    ├── edit_profile.xlsx
    └── change_password.xlsx
```

**Poor organization:**
```
tests/android/
├── all_tests/
│   ├── login.xlsx
│   ├── checkout.xlsx
│   ├── profile.xlsx
│   └── (100 more files...)
```

### Test File Naming

**Use descriptive names:**
- `login_with_email.xlsx`
- `checkout_with_credit_card.xlsx`
- `add_product_to_wishlist.xlsx`

**Avoid:**
- `test1.xlsx`
- `my_test.xlsx`
- `final_test_v2_updated.xlsx`

---

## Continuous Integration Flow

### Complete Pipeline Flow

```
┌─────────────────────┐
│   Push to GitLab    │
└──────────┬──────────┘
           │
           ├─────────────────────┐
           │                     │
    ┌──────▼──────┐       ┌─────▼──────┐
    │   GitLab    │       │   GitHub   │
    │   CI/CD     │       │  Actions   │
    └──────┬──────┘       └─────┬──────┘
           │                     │
    ┌──────▼──────┐       ┌─────▼──────────┐
    │ Validation  │       │    Android     │
    │    Jobs     │       │   Emulator     │
    │             │       │                │
    │ ✓ Python    │       │ 1. Start emu   │
    │ ✓ Node.js   │       │ 2. Install APK │
    │ ✓ Structure │       │ 3. Scan modules│
    │ ✓ Linting   │       │ 4. Run tests   │
    └──────┬──────┘       │ 5. Generate    │
           │              │    report      │
    ┌──────▼──────┐       └─────┬──────────┘
    │   Reports   │             │
    │ Generation  │       ┌─────▼──────────┐
    │             │       │Upload Artifacts│
    │ Module info │       │                │
    │ Test files  │       │ - HTML report  │
    └──────┬──────┘       │ - JSON data    │
           │              │ - Screenshots  │
    ┌──────▼──────┐       └────────────────┘
    │  iOS Manual │
    │   (Optional)│
    │             │
    │ 1. Trigger  │
    │ 2. Scan     │
    │ 3. Run      │
    │ 4. Report   │
    └─────────────┘
```

---

## Module Report Generator

### Usage

The report generator is automatically called by CI/CD, but you can also run it locally:

```bash
# Generate Android report
python scripts/ci/generate_module_report.py android

# Generate iOS report
python scripts/ci/generate_module_report.py ios

# Specify custom paths
python scripts/ci/generate_module_report.py android tests reports
```

### Output

**HTML Report:**
- `reports/android_test_report.html`
- `reports/ios_test_report.html`

**JSON Data:**
- `reports/android_test_report.json`
- `reports/ios_test_report.json`

### JSON Structure

```json
{
  "platform": "android",
  "generated_at": "2025-10-05T05:00:00.123456",
  "summary": {
    "total_modules": 3,
    "total_tests": 12
  },
  "modules": {
    "login": {
      "test_count": 2,
      "test_files": [
        "tests/android/login/login_test.xlsx",
        "tests/android/login/oauth_login_test.xlsx"
      ]
    },
    "checkout": {
      "test_count": 2,
      "test_files": [
        "tests/android/checkout/cart_test.xlsx",
        "tests/android/checkout/payment_test.xlsx"
      ]
    }
  }
}
```

---

## Troubleshooting

### No modules found

**Problem:** Report shows "No test files found"

**Solution:**
1. Check folder structure: `tests/android/[module_name]/`
2. Ensure `.xlsx` files are in module folders, not in `tests/android/` root
3. Verify file extensions are `.xlsx` not `.xls`

### Tests not running for a module

**Problem:** Module appears in report but tests don't run

**Solution:**
1. Check test file format (must be valid TestZen Excel format)
2. Verify file permissions (should be readable)
3. Check CI/CD logs for error messages
4. Ensure APK/IPA is installed before tests run

### APK/IPA not found

**Problem:** "No APK/IPA found" warning

**Solutions:**
1. Add APK/IPA to correct location: `apps/android/` or `apps/ios/`
2. Check .gitignore doesn't exclude build files
3. Consider using Git LFS for large files
4. Or build app in CI/CD pipeline

### Report not generated

**Problem:** No HTML report in artifacts

**Solution:**
1. Check if `scripts/ci/generate_module_report.py` exists
2. Verify Python is installed in CI environment
3. Check CI logs for Python errors
4. Ensure `reports/` directory is created

---

## Best Practices

### 1. Module Organization
- Keep modules focused on a single feature
- Don't create too many small modules (minimum 2-3 tests per module)
- Use consistent naming across Android and iOS

### 2. Test File Management
- Name tests clearly and descriptively
- Keep test files under 50 steps when possible
- Use comments in Excel files to document complex flows

### 3. APK/IPA Management
- Use Git LFS for files > 50MB
- Or build in pipeline to avoid committing binaries
- Keep separate debug and release builds if needed

### 4. Report Review
- Check HTML reports after each CI run
- Monitor test execution time per module
- Track failure patterns by module

### 5. Continuous Improvement
- Add new modules as features grow
- Refactor tests when modules become too large
- Archive old/deprecated modules

---

## Example: Adding a New Module

Let's add a "Notifications" module for Android:

```bash
# 1. Create module folder
mkdir tests/android/notifications

# 2. Create test files
cat > tests/android/notifications/push_notification_test.xlsx
# (Create your Excel test file using TestZen format)

cat > tests/android/notifications/in_app_notification_test.xlsx
# (Create another test file)

# 3. Verify structure
ls tests/android/notifications/
# Output:
# push_notification_test.xlsx
# in_app_notification_test.xlsx

# 4. Commit and push
git add tests/android/notifications/
git commit -m "Add notifications module with 2 tests"
git push

# 5. Check GitHub Actions
# Go to Actions tab and watch tests run automatically

# 6. Download report
# Check Artifacts in GitHub Actions for the HTML report
```

Result: Your HTML report will now include a "notifications" section with both tests listed.

---

## Summary

- **Multi-module support enabled**
- **Organized tests by feature modules**
- **Platform-specific module organization (Android/iOS)**
- **Automatic module scanning and execution**
- **Beautiful HTML reports with module breakdown**
- **APK/IPA per platform support**
- **JSON data export for custom reporting**

**Your test organization is now scalable for large projects!**
