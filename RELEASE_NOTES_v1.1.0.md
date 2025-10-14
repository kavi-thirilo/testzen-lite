# TestZen Lite v1.1.0 - Enhanced Reliability & Device Management

## Overview

This release focuses on improving device connection reliability, emulator boot detection, and app launching mechanisms. Major improvements to timeout handling and intelligent polling make the framework more robust for both local development and CI/CD environments.

---

## What's New in v1.1.0

### Intelligent Device Boot Detection
- **Active polling** - Replaces fixed wait times with smart status checking
- **Multi-stage boot verification** - Checks boot_completed, bootanim, package manager, and system UI readiness
- **Progress logging** - Real-time feedback every 10 seconds during boot process
- **Timeout safety** - All polling loops have clear exit conditions (no infinite loops)

### Improved App Launch Mechanism
- **Proper activity manager** - Uses `am start` instead of unreliable monkey command
- **Auto-install APK** - Automatically installs app if not found on device
- **Dynamic activity detection** - Finds launcher activity via dumpsys package
- **Fallback strategy** - Multiple launch methods for maximum compatibility
- **Test-only APK support** - Handles debug APKs with `-t` flag

### Enhanced Connection Stability
- **Device readiness verification** - Checks package manager responsiveness before Appium connection
- **Optimized timeouts** - Increased session timeout from 30s to 60s for UiAutomator2 initialization
- **Better error messages** - Clear diagnostic output when boot or connection fails
- **Works for all devices** - Both fresh emulators and already-running devices

### Script Fixes
- **Fixed Appium startup** - Removed reference to non-existent appium-web-inspector directory
- **Cleaner execution** - No more "directory not found" errors during Appium launch

---

## Technical Details

### Device Boot Process (New)
```
1. Launch/Detect Emulator (max 120s)
   ↓
2. Wait for boot_completed = "1" (active polling)
   ↓
3. Wait for bootanim = "stopped" (active polling)
   ↓
4. Verify Package Manager responsive (active polling)
   ↓
5. Verify System UI running (active polling)
   ↓
6. System stabilization (5s)
   ↓
7. Device marked READY
   ↓
8. Create Appium session (60s timeout)
```

### App Launch Process (New)
```
1. Check if app is installed
   ↓ (if not installed)
2. Find APK in apps/android/
   ↓
3. Install APK with -t flag
   ↓
4. Get launcher activity via dumpsys
   ↓
5. Launch with am start -n package/activity
   ↓ (if failed)
6. Fallback to monkey command
```

---

## Breaking Changes

None. This release is fully backward compatible with v1.0.0.

---

## Bug Fixes

- Fixed testzen script trying to cd into non-existent appium-web-inspector directory
- Fixed app launch failures due to monkey command issues
- Fixed connection timeouts with freshly booted emulators
- Fixed missing timeout exit conditions in polling loops

---

## Performance Improvements

- Faster device ready detection (proceeds immediately when ready)
- Reduced unnecessary waits (no more arbitrary sleep times)
- Better resource utilization (intelligent polling vs constant checking)

---

## Upgrade Instructions

### From v1.0.0 to v1.1.0

Simply pull the latest changes:

```bash
cd testzen-lite
git pull origin main
```

No configuration changes needed. All improvements are automatic.

---

## Compatibility

- **Python:** 3.8+
- **Appium:** 2.0+
- **Android SDK:** API 21+ (Android 5.0+)
- **iOS:** iOS 13+ (requires Xcode on macOS)
- **Operating Systems:** macOS, Linux, Windows

---

## Known Issues

None reported in this release.

---

## Contributors

This release includes contributions focused on reliability and robustness.

---

## Full Changelog

### Added
- Device automation readiness verification method
- Multi-stage boot completion checking
- Progress logging for all polling operations
- Automatic APK installation when app not found
- Dynamic launcher activity detection
- Fallback app launch strategies

### Changed
- Increased Appium session timeout from 30s to 60s
- Improved boot detection with package manager and system UI checks
- Enhanced app launch to use activity manager instead of monkey command
- Better error messages with diagnostic information

### Fixed
- testzen script directory change error
- App launch failures with monkey command
- Device connection timeouts with slow emulators
- Missing timeout exit conditions in polling loops

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

- **Documentation:** README.md
- **Issues:** https://github.com/kavi-thirilo/testzen-lite/issues
- **Getting Started:** Follow the README installation guide

---

*Released: 2025-01-14*
