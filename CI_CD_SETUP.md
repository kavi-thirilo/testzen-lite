# TestZen CI/CD Setup - Complete Guide

## Overview

TestZen uses a **hybrid CI/CD approach** combining GitLab and GitHub Actions:

- **GitLab CI/CD**: Framework validation, structure checks, iOS manual testing
- **GitHub Actions**: Android automated testing with emulator (free, fast)

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Code Push to GitLab                      │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
   ┌────▼────┐              ┌─────▼──────┐
   │ GitLab  │              │   GitHub   │
   │   CI    │              │  Actions   │
   └────┬────┘              └─────┬──────┘
        │                         │
        │                         │
   ┌────▼─────────┐         ┌────▼──────────┐
   │ Validation   │         │   Android     │
   │   Jobs       │         │  Emulator     │
   │              │         │   Tests       │
   │ - Python     │         │               │
   │ - Node.js    │         │ - API 29      │
   │ - Structure  │         │ - KVM Fast    │
   │ - Linting    │         │ - Appium      │
   └────┬─────────┘         └────┬──────────┘
        │                         │
   ┌────▼────────┐          ┌────▼──────────┐
   │   Report    │          │  Test Reports │
   │ Generation  │          │   Artifacts   │
   └─────────────┘          └───────────────┘
        │                         │
   ┌────▼────────┐          ┌────▼──────────┐
   │ iOS Manual  │          │   Available   │
   │    Test     │          │  for 30 days  │
   │ (Optional)  │          └───────────────┘
   └─────────────┘
```

---

## File Structure

```
testzen/
├── .github/
│   └── workflows/
│       └── android-tests.yml          # GitHub Actions - Android emulator
├── .gitlab-ci.yml                     # GitLab CI/CD - Validation & iOS
├── build/
│   ├── README.md                      # Instructions for APK/IPA
│   └── your-app.apk                   # Your Android APK (add this)
├── tests/
│   ├── android/
│   │   ├── README.md
│   │   ├── Payment_Form_Validation_Test.xlsx
│   │   └── sample_mobile_test.xlsx
│   └── ios/
│       └── README.md                  # iOS tests go here
└── reports/                           # Generated test reports
```

---

## How It Works

### GitLab CI/CD Pipeline

**Stages:**
1. **validate** - Check Python, Node.js, dependencies
2. **lint** - YAML and Python code quality
3. **test_structure** - Framework structure validation
4. **report** - Generate validation report

**Jobs:**
- `validate:python` - Install Python dependencies
- `validate:nodejs` - Install Appium
- `validate:test_files` - Check Excel test files exist
- `lint:yaml` - Lint YAML files
- `lint:python` - Lint Python code
- `test:framework_structure` - Verify folder structure
- `test:cli_interface` - Test CLI works
- `test:import_modules` - Test Python imports
- `test:android:note` - Info about GitHub Actions
- `test:ios:manual` - Manual iOS testing (requires runner)
- `generate_reports` - Create validation report

**Triggers:**
- Every push to any branch
- Merge requests
- Scheduled (if configured)

---

### GitHub Actions Pipeline

**Workflow:** `.github/workflows/android-tests.yml`

**Steps:**
1. Checkout code
2. Set up Python 3.9
3. Install dependencies from requirements.txt
4. Set up JDK 11
5. Install Android SDK
6. Install Appium 2.11.5 with uiautomator2 driver
7. Enable KVM for hardware acceleration
8. Create Android emulator (API 29, x86_64)
9. Cache emulator for faster subsequent runs
10. Start emulator with optimizations
11. Wait for emulator to boot
12. Install APK from build/ (if exists)
13. Start Appium server
14. Run all tests from tests/android/*.xlsx
15. Upload test reports as artifacts

**Triggers:**
- Every push to main, develop, testzen_ci_cd_integration
- Pull requests to main
- Scheduled daily at 9 PM CST (3 AM UTC)
- Manual workflow dispatch

**Execution Time:**
- First run: ~8-10 minutes (creates emulator)
- Subsequent runs: ~5-7 minutes (uses cached emulator)

---

## Android Testing (GitHub Actions)

### Emulator vs Real Device

**GitHub Actions uses EMULATORS:**
- Free unlimited minutes for public repos
- Hardware acceleration (KVM)
- Fast execution (5-10 min)
- Not real devices
- Can't test device-specific features (camera, GPS sensors)

**If you need real devices:**
- Apply for BrowserStack/Sauce Labs Open Source (free)
- Or use paid plans ($99-149/month)

### Adding Your APK

**Option 1: Commit APK**
```bash
cp /path/to/your-app.apk build/
git add build/your-app.apk
git commit -m "Add Android APK for CI testing"
git push
```

**Option 2: Build in Pipeline**
Add a build job before tests in `.github/workflows/android-tests.yml`

**Option 3: No APK**
If your app is pre-installed or TestZen connects to running app, no APK needed.

### Viewing Results

1. Go to your GitHub repository
2. Click **Actions** tab
3. Find "Android Emulator Tests" workflow
4. Click on latest run
5. View logs for each step
6. Download test reports from **Artifacts** section

---

## iOS Testing (GitLab Manual)

### Requirements

- macOS runner with tags: `ios`, `macos`
- Physical iOS device or simulator
- Xcode installed
- iOS development certificates

### Running iOS Tests

1. Add test files to `tests/ios/`
2. Go to GitLab → CI/CD → Pipelines
3. Find `test:ios:manual` job
4. Click **Play** button

### Why Manual?

- iOS simulator requires macOS
- macOS runners are expensive on cloud platforms
- GitHub Actions macOS: 10x pricing vs Linux
- Best for occasional testing or when you have macOS runner

---

## Test Reports

### GitLab Reports

**Location:** Artifacts → `reports/validation_report.json`

**Contents:**
- Python dependencies status
- Node.js/Appium status
- Test files found (Android and iOS)
- Framework structure validation
- Execution summary

**Retention:** 30 days

### GitHub Actions Reports

**Location:** Actions → Workflow Run → Artifacts

**Contents:**
- Individual test results
- HTML reports (if TestZen generates them)
- Screenshots/logs
- JUnit XML (if generated)

**Retention:** 30 days

---

## Adding Tests

### Android Tests

1. Create Excel file following TestZen format
2. Save to `tests/android/your-test.xlsx`
3. Commit and push
4. GitHub Actions automatically runs it

### iOS Tests

1. Create Excel file following TestZen format
2. Save to `tests/ios/your-test.xlsx`
3. Commit and push
4. Manually trigger `test:ios:manual` in GitLab

---

## Scheduling

### GitLab Schedule (Validation)

1. GitLab → CI/CD → Schedules
2. Click "New schedule"
3. Cron: `0 3 * * *` (9 PM CST)
4. Target branch: `main`
5. Save

### GitHub Actions Schedule (Android Tests)

**Already configured** in workflow:
```yaml
schedule:
  - cron: '0 3 * * *'  # 9 PM CST daily
```

No additional setup needed.

---

## Cost Breakdown

| Component | Service | Cost |
|-----------|---------|------|
| GitLab CI/CD | GitLab.com | **Free** |
| Android Tests | GitHub Actions | **Free** (public repos) |
| iOS Tests | Manual (your hardware) | **Free** |
| **TOTAL** | | **$0/month** |

### If You Need More

| Upgrade | Cost | What You Get |
|---------|------|--------------|
| GitHub Pro | $4/month | More features (public repos still free) |
| BrowserStack Open Source | **Free** (apply) | Real devices + emulators |
| Sauce Labs Open Source | **Free** (apply) | Real devices + emulators |
| Hetzner VPS + GitLab Runner | $5/month | Android emulator in GitLab |
| BrowserStack Paid | $99/month | Real devices, no approval needed |
| Sauce Labs Paid | $149/month | Real devices, professional support |

---

## Troubleshooting

### GitHub Actions Not Running

**Check:**
1. Repository is public (or has GitHub Pro/Enterprise)
2. Workflow file is in `.github/workflows/`
3. YAML syntax is correct
4. Branch name matches trigger configuration

### Tests Failing on Emulator

**Common issues:**
1. **APK not compatible** - Check API level (currently using API 29)
2. **App not installed** - Check APK exists in `build/`
3. **Appium connection failed** - Check desired capabilities
4. **Timeout errors** - Increase wait times in test files

### GitLab iOS Tests Not Running

**Check:**
1. Runner with tags `ios`, `macos` is online
2. Job is triggered manually (not automatic)
3. Runner has macOS with Xcode
4. iOS test files exist in `tests/ios/`

---

## Next Steps

### Immediate

1. Folder structure created
2. GitHub Actions workflow configured
3. GitLab CI/CD updated
4. Add your Android APK to `build/`
5. Push to GitHub to enable Actions
6. Test the pipeline

### Future Enhancements

1. Add iOS tests when macOS runner available
2. Apply for BrowserStack/Sauce Labs Open Source
3. Add more test files
4. Configure test result parsing
5. Add Slack/email notifications
6. Integrate with test management tools (JIRA, TestRail)

---

## Support

**Questions:**
- GitLab CI/CD: Check `.gitlab-ci.yml` comments
- GitHub Actions: Check `.github/workflows/android-tests.yml` comments
- Test format: See `tests/android/README.md` and `tests/ios/README.md`

**Issues:**
- Create issue in GitLab/GitHub repository
- Check pipeline logs for errors
- Review this documentation

---

## Summary

- **GitLab** validates framework structure
- **GitHub Actions** runs Android tests on emulator (free)
- **iOS** tests available manually
- **Reports** generated and stored for 30 days
- **Cost** $0/month
- **Fast** execution with KVM acceleration

**You're all set!** Push your changes and watch the magic happen.
