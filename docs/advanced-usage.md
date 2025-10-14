# Advanced Usage Guide

Advanced features and configuration options for TestZen.

---

## Command Line Options

### Test Execution Options

```bash
# Disable screenshots
./testzen run --file test.xlsx --screenshots no

# Skip remaining steps on failure (instead of stopping)
./testzen run --file test.xlsx --skip-on-fail

# Disable automatic emulator launch
./testzen run --file test.xlsx --no-auto-launch

# Use specific device
./testzen run --file test.xlsx --device emulator-5554

# Prefer specific AVD
./testzen run --file test.xlsx --avd Pixel_4_API_30
```

### Cleanup Options

```bash
# Disable all cleanup operations
./testzen run --file test.xlsx --no-cleanup

# Skip clearing notifications
./testzen run --file test.xlsx --no-cleanup-notifications

# Skip clearing app cache
./testzen run --file test.xlsx --no-cleanup-cache

# Skip removing temp files
./testzen run --file test.xlsx --no-cleanup-temp
```

### Appium Server Options

```bash
# Auto-start Appium if not running
./testzen run --file test.xlsx --auto-appium

# Keep Appium running after tests
./testzen run --file test.xlsx --auto-appium --keep-appium
```

---

## Multi-Locator Fallback System

Use multiple locators for reliable element finding:

```
Locator Type: id|accessibility_id|xpath|text
Locator Value: login_btn|LoginButton|//android.widget.Button[@text='Login']|Login
```

TestZen will try each locator in order until one succeeds.

**Benefits:**
- Handles app updates that change IDs
- Works across different Android versions
- Increases test stability

---

## Test Organization by Modules

Organize tests by feature for better reporting:

```
tests/android/
├── login/
│   ├── valid_login.xlsx
│   ├── invalid_login.xlsx
│   └── forgot_password.xlsx
├── checkout/
│   ├── add_to_cart.xlsx
│   └── payment.xlsx
└── profile/
    └── edit_profile.xlsx
```

Reports will group results by module.

---

## Custom Wait Strategies

### Fixed Wait
```
Action: wait
Test Data: 5
```

### Wait for Element (coming soon)
Custom wait implementations can be added to framework.

---

## WebView Testing

For apps with web content:

1. Enable WebView debugging in app
2. Use xpath or text locators
3. Switch context if needed

---

## Environment-Specific Configuration

### Development vs Production

Use different APKs for different environments:

```bash
# Development
cp app-debug.apk apps/android/

# Production
cp app-release.apk apps/android/
```

### Test Data Management

Keep test data in Excel Test Data column:
- Easy to update
- No code changes needed
- Version control friendly

---

## Parallel Test Execution

For running multiple tests simultaneously:

```bash
# Run all Android tests
./testzen run --all --platform android

# Run all iOS tests
./testzen run --all --platform ios
```

Each test runs sequentially within the command, but you can launch multiple terminal windows for true parallelism.

---

## Custom Reporting

Reports are generated in:
- HTML: `reports/html_reports/`
- Excel: `reports/excel_reports/`

HTML reports include:
- Pass/fail summary
- Screenshots at each step
- Execution timeline
- Detailed error messages

---

## Debugging Tests

### View Appium Logs

```bash
cat /tmp/appium.log
```

### View Device Logs

```bash
# Android
adb logcat | grep your.app.package

# Filter by priority
adb logcat *:E  # Errors only
```

### Manual Appium Testing

```bash
# Start Appium manually
appium --allow-insecure=*:chromedriver_autodownload

# Run test in another terminal
./testzen run --file test.xlsx
```

---

## Performance Optimization

### Use x86_64 Emulators
Faster than ARM emulators on Intel/AMD processors.

### Enable Hardware Acceleration
- Intel: Install HAXM
- AMD: Enable AMD-V in BIOS

### Pre-launch Emulator
```bash
./testzen emulator launch
# Wait for boot, then run tests
```

### Reduce Wait Times
Minimize wait steps in tests for faster execution.

---

## CI/CD Integration

See [CI/CD Integration Guide](cicd-integration.md) for detailed setup.

---

## Advanced Test Actions

### Available Actions

- `click` - Tap element
- `enter_text` - Input text
- `clear_text` - Clear field
- `verify_text` - Check text exists
- `verify_element` - Check element exists
- `swipe_up`, `swipe_down`, `swipe_left`, `swipe_right` - Swipe gestures
- `go_back` - Press back button
- `take_screenshot` - Capture screen
- `wait` - Pause execution
- `close_app` - Close app
- `launch_app` - Reopen app

---

## Error Handling

### Skip on Fail Mode

```bash
./testzen run --file test.xlsx --skip-on-fail
```

Continues to next step instead of stopping on failure.

### Screenshot on Error

Screenshots are automatically captured on errors (unless disabled).

---

## Need Help?

- [Test Creation Guide](test-creation.md) - Basic test writing
- [Troubleshooting](troubleshooting.md) - Common issues
- [GitHub Issues](https://github.com/kavi-thirilo/testzen-lite/issues) - Report bugs

---

[← Back to Main README](../README.md)
