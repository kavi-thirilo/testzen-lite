# iOS Test Modules

Organize your iOS tests by feature modules. Each module is a separate folder containing related test files.

## Current Modules

- **login/** - Authentication and login tests (example)
- **dashboard/** - Dashboard and home screen tests (example)

## Creating a New Module

```bash
# Create module folder
mkdir tests/ios/my_module_name

# Add test files
cp your_test.xlsx tests/ios/my_module_name/

# Commit
git add tests/ios/my_module_name/
git commit -m "Add my_module_name iOS tests"
git push
```

## Module Structure

```
tests/ios/
├── login/
│   ├── basic_login.xlsx
│   └── biometric_login.xlsx
├── dashboard/
│   └── dashboard_load.xlsx
└── settings/
    ├── change_password.xlsx
    └── notifications.xlsx
```

## Test Execution

**Manual trigger in GitLab:**
1. Go to GitLab → CI/CD → Pipelines
2. Find latest pipeline
3. Click **Play** on `test:ios:manual` job

**Execution flow:**
1. Check for IPA in `build/ios/ipa/`
2. Install IPA on iOS device/simulator (if available)
3. Scan all module folders in `tests/ios/`
4. For each module, run all `.xlsx` files
5. Generate HTML report with module breakdown
6. Upload reports as artifacts

## Requirements

iOS testing requires:
- macOS runner with tags: `ios`, `macos`
- Physical iOS device or simulator
- Xcode and iOS development tools
- iOS development certificates

## Test File Format

Excel format (TestZen standard):
- Column A: Step Number
- Column B: Action
- Column C: Locator Type
- Column D: Locator Value
- Column E: Test Data
- Column F: Expected Result

## IPA Location

Place your iOS IPA here: `build/ios/ipa/your-app.ipa`

The workflow will install it on the simulator/device before running tests.

## Viewing Reports

1. Go to GitLab → CI/CD → Pipelines
2. Click on pipeline with manual iOS job
3. Click **Browse** under Artifacts
4. Open `reports/ios_test_report.html`

## Best Practices

- Group related tests in same module
- Use descriptive module names (login, settings, profile)
- Keep 2-10 tests per module
- Name test files clearly
- Test on both simulator and real device when possible
- Don't put all tests in one module
- Avoid generic names like "module1"

For complete guide, see: `MULTI_MODULE_GUIDE.md`
