# Troubleshooting Guide

Common issues and solutions for TestZen.

---

## Installation Issues

### Python Installation
See [Python Setup Guide](python-setup.md) for detailed installation instructions.

### Node.js Installation
See [Node.js Setup Guide](nodejs-setup.md) for detailed installation instructions.

### Android SDK Installation
See [Android SDK Setup Guide](android-sdk-setup.md) for detailed installation instructions.

### Appium Installation
See [Appium Setup Guide](appium-setup.md) for detailed installation instructions.

### PATH Configuration
See [PATH Configuration Guide](path-configuration.md) for detailed PATH setup instructions.

---

## Device and Emulator Issues

### Device Not Detected
See [Device Setup Guide](device-setup.md) for complete device troubleshooting.

### USB Debugging
Quick fix for offline/unauthorized devices:
```bash
./scripts/enable_usb_debugging.sh
```

Detailed guide: [Device Setup - USB Debugging](device-setup.md#enable-usb-debugging-on-emulator)

---

## Test Execution Issues

### Test File Not Found

**Error:** "Test file not found"

**Solution:**
- Verify file path is correct
- Use absolute or relative path from testzen-lite root
- File must have .xlsx extension

---

### App Not Found

**Error:** "No APK/IPA file found"

**Solution:**
- Place exactly ONE APK in `apps/android/` for Android tests
- Place exactly ONE IPA in `apps/ios/` for iOS tests
- Verify file has correct extension (.apk or .ipa)

---

### Element Not Found

**Error:** "Element not found" or "No such element"

**Solution:**
1. Verify locator is correct using Appium Inspector
2. Add wait time before action:
   ```
   Action: wait
   Test Data: 3
   ```
3. Use multi-locator fallback:
   ```
   Locator Type: id|accessibility_id|xpath
   Locator Value: button_id|ButtonLabel|//android.widget.Button
   ```
4. Check if element is in WebView (requires different locator strategy)

---

### App Crashes

**Solution:**
1. Check app logs:
   ```bash
   adb logcat | grep your.app.package
   ```
2. Verify APK is compatible with device/emulator
3. Try debug APK instead of release APK
4. Check app permissions in test steps

---

## Appium Server Issues

### Port Already in Use

**Error:** "Port 4723 is already in use"

**Solution:**
```bash
# Kill existing Appium
pkill -f appium

# Or find and kill process manually
lsof -i :4723
kill -9 <PID>
```

---

### Appium Won't Start

**Solution:**
1. Check Appium log:
   ```bash
   cat /tmp/appium.log
   ```
2. Verify driver is installed:
   ```bash
   appium driver list --installed
   ```
3. Check Node.js version (needs 16+):
   ```bash
   node --version
   ```

---

## Report Generation Issues

### No Reports Generated

**Solution:**
- Check `reports/html_reports/` folder
- Verify test completed successfully
- Check for file permission issues

---

### Screenshots Missing

**Solution:**
- Screenshots are disabled by default in some configurations
- Enable with: `./testzen run --file test.xlsx --screenshots yes`
- Check device screen is on and unlocked

---

## Performance Issues

### Slow Test Execution

**Solution:**
1. Reduce wait times in test steps
2. Use faster emulator (x86_64 images)
3. Enable hardware acceleration for emulator
4. Use physical device instead of emulator

---

### Emulator Boot Slow

**Solution:**
1. Allocate more RAM to emulator (in AVD settings)
2. Use x86_64 system image (faster than ARM)
3. Enable hardware acceleration (Intel HAXM or AMD)
4. Start emulator before running tests

---

## Need More Help?

1. Check specific setup guides:
   - [Python Setup](python-setup.md)
   - [Node.js Setup](nodejs-setup.md)
   - [Android SDK Setup](android-sdk-setup.md)
   - [Appium Setup](appium-setup.md)
   - [Device Setup](device-setup.md)

2. Report issues: [GitHub Issues](https://github.com/kavi-thirilo/testzen-lite/issues)

---

[← Back to Main README](../README.md)
