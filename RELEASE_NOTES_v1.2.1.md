# TestZen Lite v1.2.1 - Critical Bug Fixes

## Overview

This patch release fixes critical bugs discovered in v1.2.0 related to Appium driver detection and HTML report generation.

---

## What's Fixed in v1.2.1

### Bug Fixes

**HTML Report Counter Bug**
- Fixed issue where HTML reports showed 0 passed count even when tests passed
- Root cause: Status value mismatch between test automation and reporter
- testzen_automation.py was setting status as "PASSED"/"FAILED"
- professional_reporter.py was checking for "PASS"/"FAIL" (without 'ED')
- This mismatch prevented counters from incrementing
- Fix: Updated professional_reporter.py to check for "PASSED"/"FAILED"

**Appium Driver Detection - ANSI Color Codes**
- Fixed driver detection failing despite having uiautomator2 installed
- Root cause: ANSI color codes in appium driver list output
- Output was showing `[33muiautomator2[39m` instead of plain text
- Added regex pattern to strip ANSI codes before checking driver presence
- Now checks both stdout and stderr for driver list output

**Appium Driver Detection - Global vs npx**
- Fixed driver detection when using global appium installation
- testzen script was using npx appium but driver was installed globally
- Updated testzen script to prefer global appium command if available
- Falls back to npx appium if global command not found
- Ensures driver checks use the same appium installation as test execution

---

## Technical Details

### Files Changed

**src/reports/professional_reporter.py:**
- Line 95: Changed from `status.upper() == 'PASS'` to `status.upper() == 'PASSED'`
- Line 97: Changed from `status.upper() == 'FAIL'` to `status.upper() == 'FAILED'`

**src/utils/device_utils.py:**
- Added ANSI color code stripping with regex: `re.sub(r'\x1b\[[0-9;]*m', '', output)`
- Now checks both stdout and stderr for driver list output
- Enhanced debug logging for driver verification

**testzen (shell script):**
- Lines 58-62: Added check for global appium command
- Prefers `appium` command if available globally
- Falls back to `npx appium` if global command not found

---

## Upgrade Instructions

### From v1.2.0 to v1.2.1

Simply pull the latest changes:

```bash
cd testzen-lite
git pull origin main
```

No configuration changes needed. All fixes are automatic.

---

## Impact

**Before v1.2.1:**
```
Issue 1: HTML report shows "Tests Passed: 0" even when all tests pass
Issue 2: "Appium uiautomator2 driver not installed" error despite having it installed
Issue 3: Driver detection fails when using global appium installation
```

**After v1.2.1:**
```
Fix 1: HTML report correctly displays "Tests Passed: 5/5"
Fix 2: Driver detection handles ANSI color codes properly
Fix 3: Works with both global and npx appium installations
```

---

## Testing

All fixes have been tested with:
- Global appium installation
- npx appium installation
- Both Appium 2.x and 3.x
- Multiple test scenarios with pass/fail combinations

---

## Breaking Changes

None. This release is fully backward compatible with v1.2.0.

---

## Known Issues

None reported in this release.

---

## Full Changelog

### Fixed
- HTML report showing zero passed count despite tests passing
- Appium driver detection failing due to ANSI color codes in output
- Driver detection failing when using global appium vs npx appium
- Status value mismatch between test execution and report generation

### Changed
- professional_reporter.py status checks now match actual status values
- device_utils.py now strips ANSI codes from driver list output
- testzen script now prefers global appium over npx appium

---

## Compatibility

- Python: 3.8+
- Appium: 2.0+ and 3.x
- Android SDK: API 21+ (Android 5.0+)
- iOS: iOS 13+ (requires Xcode on macOS)
- Operating Systems: macOS, Linux, Windows

---

## Support

- Main Documentation: README.md
- Setup Guide: ANDROID_SDK_SETUP.md
- Previous Release: RELEASE_NOTES_v1.2.0.md
- Issues: https://github.com/kavi-thirilo/testzen-lite/issues

---

Released: 2025-10-14
