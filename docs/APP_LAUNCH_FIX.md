# Fixing Element Not Found and App Launch Issues

## Problem
1. App not launching automatically before tests run
2. Elements not found (hint attribute doesn't exist)

## Solution 1: Add App Launch Capabilities to Workflow

Update the GitHub Actions workflow to launch the app after installation:

```bash
# After APK installation, launch the app
adb shell am start -n com.yourapp.package/.MainActivity

# Wait for app to start
sleep 3

# Verify app is running
adb shell dumpsys window | grep -E 'mCurrentFocus|mFocusedApp'
```

## Solution 2: Add App Package Info to Test

The test framework needs to know:
- **App Package**: `com.example.bankingdemo` (or your actual package name)
- **App Activity**: `.MainActivity` (or your actual main activity)

### Get Package and Activity Name

```bash
# Method 1: Using adb on installed app
adb shell pm list packages | grep banking
adb shell dumpsys package com.example.bankingdemo | grep -A 1 MAIN

# Method 2: Using aapt (if available)
aapt dump badging build/android/apk/banking_demo.apk | grep package
aapt dump badging build/android/apk/banking_demo.apk | grep launchable-activity
```

## Solution 3: Fix Element Locators

The current test uses `hint` attribute which may not exist. Use better locators:

### Instead of:
```
Locator Type: xpath
Locator Value: //*[@hint='Username input field']
```

### Use one of these:
```
# Option 1: By resource-id
Locator Type: id
Locator Value: com.example.app:id/username

# Option 2: By text
Locator Type: xpath
Locator Value: //*[@text='Username']

# Option 3: By content-desc
Locator Type: accessibility id
Locator Value: username_field

# Option 4: By class and index
Locator Type: xpath
Locator Value: //android.widget.EditText[1]
```

## Solution 4: Inspect App Elements

To find correct locators, use Appium Inspector:

### During GitHub Actions Run:
The workflow currently installs the app. To inspect elements:

1. **Option A**: Run locally with emulator
   ```bash
   # Start emulator
   emulator -avd test_avd &

   # Install APK
   adb install -r -t build/android/apk/banking_demo.apk

   # Launch app
   adb shell am start -n <package>/<activity>

   # Start Appium
   appium

   # Use Appium Inspector to connect and inspect elements
   ```

2. **Option B**: Add debug step to workflow
   ```yaml
   - name: Inspect app elements
     run: |
       adb shell uiautomator dump /sdcard/ui.xml
       adb pull /sdcard/ui.xml
       cat ui.xml
   ```

## Solution 5: Add Launch Step to Test

Add this as first step in your Excel test file:

| S.No | Description | Action | Locator Type | Locator Value | Input Data | Expected Result |
|------|-------------|--------|--------------|---------------|------------|-----------------|
| 1 | Launch Banking App | launch_app | | | | App should launch successfully |
| 2 | Wait for app to load | wait | | | 3 | App elements should be visible |

## Solution 6: Update Workflow to Auto-Launch App

Add to `.github/workflows/android-tests.yml` after APK install:

```bash
# Get package name from APK
PACKAGE=$(aapt dump badging build/android/apk/*.apk | grep package: | awk '{print $2}' | sed "s/name='//g" | sed "s/'//g")

# Get launchable activity
ACTIVITY=$(aapt dump badging build/android/apk/*.apk | grep launchable-activity | awk '{print $2}' | sed "s/name='//g" | sed "s/'//g")

# Launch app
adb shell am start -n $PACKAGE/$ACTIVITY

# Wait for launch
sleep 3

# Verify app launched
adb shell "dumpsys window | grep mCurrentFocus"
```

## Quick Fix for Current Issue

The easiest fix right now:

1. **Find the actual package name** of banking_demo.apk
2. **Add app launch** to the GitHub Actions workflow
3. **Update test locators** to use resource-id or text instead of hint

### Example Updated Workflow Script:

```bash
adb install -r -t build/android/apk/*.apk && \
adb shell am start -n com.example.bankingdemo/.MainActivity && \
sleep 3 && \
echo "Running tests..." && \
python testzen.py run --file tests/android/billing/Payment_Form_Validation_Test.xlsx --platform android
```

## Recommended Next Steps

1. Extract package name from APK
2. Add app launch command to workflow
3. Run `adb shell uiautomator dump` to get UI hierarchy
4. Update test Excel file with correct locators
5. Re-run tests
