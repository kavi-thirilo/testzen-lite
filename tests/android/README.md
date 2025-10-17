# Android Test Modules

Organize your Android tests by feature modules. Each module is a separate folder containing related test files.

## Current Modules

- **billing/** - Payment and billing tests
- **login/** - Authentication and login tests (example)
- **checkout/** - Shopping cart and checkout tests (example)

## Creating a New Module

```bash
# Create module folder
mkdir tests/android/my_module_name

# Add test files
cp your_test.xlsx tests/android/my_module_name/

# Commit
git add tests/android/my_module_name/
git commit -m "Add my_module_name tests"
git push
```

## Module Structure

```
tests/android/
├── billing/
│   └── Payment_Form_Validation_Test.xlsx
├── login/
│   ├── basic_login.xlsx
│   └── social_login.xlsx
└── checkout/
    ├── add_to_cart.xlsx
    └── payment.xlsx
```

## Test Execution

**Automatic on GitHub Actions:**
- Every push to main/develop branches
- Pull requests
- Nightly at 9 PM CST
- Manual workflow dispatch

**Execution flow:**
1. Scan all module folders in `tests/android/`
2. For each module, run all `.xlsx` files
3. Generate HTML report with module breakdown
4. Upload reports as artifacts

## Test File Format

Excel format (TestZen standard):
- Column A: Step Number
- Column B: Action
- Column C: Locator Type
- Column D: Locator Value
- Column E: Test Data
- Column F: Expected Result

## APK Location

Place your Android APK here: `apps/android/your-app.apk`

The workflow will install it on the emulator before running tests.

## Viewing Reports

1. Go to GitHub repository → **Actions** tab
2. Click on latest "Android Emulator Tests" run
3. Download artifacts
4. Open `android_test_report.html` to see module-organized results

## Best Practices

- Group related tests in same module
- Use descriptive module names (login, checkout, settings)
- Keep 2-10 tests per module
- Name test files clearly
- Don't put all tests in one module
- Avoid generic names like "module1"

For complete guide, see: [Multi-Module Testing Guide](../../docs/multi-module-guide.md)
