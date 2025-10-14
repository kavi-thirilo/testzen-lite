# TestZen Lite v1.1.1 - Performance & Stability Improvements

## Overview

This patch release addresses critical stability issues and significantly improves test execution speed. Focus areas include emulator boot detection, app installation reliability, and performance optimization through native Appium API usage.

---

## What's Fixed in v1.1.1

### Critical Bug Fixes

**Emulator Boot Detection Timeout**
- Fixed timeout issues on slow systems and freshly launched emulators
- Increased boot detection timeout from 60s to 90s
- Increased total emulator launch timeout from 120s to 180s
- Removed problematic pipe command that caused system check failures
- More reliable boot completion verification

**APK Installation for Test Packages**
- Fixed INSTALL_FAILED_TEST_ONLY error when installing debug APKs
- Corrected Appium API parameter from androidInstallOptions to allowTestPackages
- Added automatic permission granting during installation
- Proper support for test-only packages without manual ADB commands

### Performance Improvements

**60% Faster Test Execution**
- Reduced wait times from approximately 30s to 12s per test
- Optimized app launch verification with 500ms polling instead of 1s
- Removed unnecessary stabilization waits
- Tests complete significantly faster on all systems

**Appium Native API Migration**
- Replaced all ADB shell commands with native Appium driver APIs
- Using driver.is_app_installed() instead of adb pm list
- Using driver.install_app() instead of adb install
- Using driver.activate_app() instead of adb am start
- Using driver.terminate_app() instead of adb am force-stop
- Using driver.query_app_state() for reliable app state verification

---

## Technical Details

### Wait Time Optimization

| Operation | v1.1.0 | v1.1.1 | Time Saved |
|-----------|--------|--------|------------|
| Post-install wait | 5s | 0s | 5s |
| Foreground check | 15s max | 10s max | 5s |
| Stabilization | 2s | 0s | 2s |
| Post-launch wait | 8s | 2s | 6s |
| Total per test | ~30s | ~12s | ~18s |

### Boot Detection Improvements

**v1.1.0 Process:**
- Wait for emulator appearance (120s max)
- Check boot_completed and bootanim (60s max)
- Attempt complex system UI verification (often failed)

**v1.1.1 Process:**
- Wait for emulator appearance (180s max)
- Check boot_completed and bootanim (90s max)
- Simple getprop verification (reliable)
- Total: More time but more reliable

### App State Values (Appium API)

```
0 = App not installed
1 = App not running
3 = App running in background
4 = App running in foreground
```

Tests only proceed when state equals 4, ensuring app is visible and ready.

---

## Breaking Changes

None. This release is fully backward compatible with v1.1.0.

---

## Bug Fixes

- Fixed emulator boot timeout on slow systems or cold starts
- Fixed APK installation failure for test-only packages
- Fixed system UI check causing boot verification failures
- Fixed race condition where tests started before app was fully visible
- Consolidated duplicate install_apk methods
- Fixed uninstall_apk to use Appium remove_app API

---

## Performance Improvements

- 60% reduction in test execution time
- Faster app state verification with 500ms polling
- Removed unnecessary wait periods
- More efficient foreground state detection
- Earlier exit when conditions are met

---

## Code Quality Improvements

- Removed 130+ lines of complex ADB command code
- Eliminated brittle shell command parsing
- Consistent use of Appium driver APIs throughout
- Better error handling with proper API responses
- More maintainable codebase
- Cross-platform compatibility (Android/iOS)

---

## Upgrade Instructions

### From v1.1.0 to v1.1.1

Simply pull the latest changes:

```bash
cd testzen-lite
git pull origin main
```

No configuration changes needed. All improvements are automatic.

---

## Compatibility

- Python: 3.8+
- Appium: 2.0+
- Android SDK: API 21+ (Android 5.0+)
- iOS: iOS 13+ (requires Xcode on macOS)
- Operating Systems: macOS, Linux, Windows

---

## Known Issues

None reported in this release.

---

## Installation Parameters

The following Appium install_app parameters are now used:

```python
driver.install_app(apk_path,
    replace=True,              # Reinstall if exists
    allowTestPackages=True,    # Allow debug/test APKs
    grantPermissions=True      # Auto-grant permissions
)
```

---

## Contributors

This release includes bug fixes and performance optimizations based on user feedback and testing.

---

## Full Changelog

### Added
- Appium native API support for all app management operations
- App state verification using query_app_state API
- Automatic permission granting during installation
- Better timeout handling for slow emulator boots

### Changed
- Increased emulator boot timeout from 120s to 180s
- Increased boot completion timeout from 60s to 90s
- Reduced post-launch wait from 8s to 2s in configuration
- Optimized foreground check from 15s to 10s with faster polling
- Simplified boot completion verification process

### Fixed
- Test-only APK installation using correct allowTestPackages parameter
- Emulator boot detection timeout on slow systems
- System UI check failures with pipe commands
- Duplicate install_apk method definitions
- Race conditions during app launch

### Removed
- Complex launcher activity detection logic (100+ lines)
- Monkey command fallback methods
- ADB shell command dependencies for app management
- Unnecessary stabilization waits
- Problematic dumpsys activity parsing

---

## Next Steps

After upgrading, verify your setup:

```bash
# Check version
./testzen version

# List available tests
./testzen list

# Run a test
./testzen run --file tests/android/billing/Payment_Form_Validation_Test.xlsx
```

---

## Support

- Documentation: README.md
- Issues: https://github.com/kavi-thirilo/testzen-lite/issues
- Getting Started: Follow the README installation guide

---

Released: 2025-10-14
