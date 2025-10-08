#!/bin/bash
set -e

echo "=== Starting Appium Server ==="
appium &
APPIUM_PID=$!
echo "Appium started with PID: $APPIUM_PID"
sleep 5

echo "=== Waiting for Emulator ==="
adb wait-for-device
sleep 2
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
  sleep 1
done
echo "Emulator ready"

echo "=== Validating APK ==="
APK_COUNT=$(ls -1 apps/android/*.apk 2>/dev/null | wc -l)
if [ "$APK_COUNT" -eq 0 ]; then
  echo "WARNING: No APK found in apps/android/"
  echo "Skipping APK installation. Tests will run against pre-installed apps or use provided APK path."
  echo "To include APK: Add your .apk file to apps/android/"

  # Skip to running tests without APK installation
  mkdir -p reports
  echo "=== Running Tests (No APK Installation) ==="
  TEST_FAILED=0
  for module_dir in tests/android/*/; do
    if [ -d "$module_dir" ]; then
      for test_file in "$module_dir"*.xlsx; do
        if [ -f "$test_file" ]; then
          echo "Executing: $test_file"
          python testzen.py run --file "$test_file" --platform android || TEST_FAILED=1
        fi
      done
    fi
  done

  echo "=== Generating Report ==="
  python scripts/ci/generate_module_report.py android tests reports || echo "Report generation skipped"

  echo "=== Stopping Appium ==="
  kill $APPIUM_PID || true

  echo "=== Test Execution Complete (No APK) ==="
  exit $TEST_FAILED
elif [ "$APK_COUNT" -gt 1 ]; then
  echo "ERROR: Multiple APKs found. Only one APK should exist:"
  ls -lh apps/android/*.apk
  exit 1
fi

APK_PATH=$(ls apps/android/*.apk)
echo "Found APK: $APK_PATH"
ls -lh "$APK_PATH"

echo "=== Getting Package Name ==="
# Try using aapt first
PACKAGE=$(aapt dump badging "$APK_PATH" 2>/dev/null | grep "package: name=" | sed "s/.*name='\([^']*\)'.*/\1/" || echo "")

# If aapt fails, try Python script (works everywhere)
if [ -z "$PACKAGE" ]; then
  echo "aapt not available, using Python to extract package info..."
  APK_INFO=$(python scripts/ci/get_apk_info.py "$APK_PATH" 2>/dev/null || echo "")
  if [ -n "$APK_INFO" ]; then
    PACKAGE=$(echo "$APK_INFO" | grep "PACKAGE=" | cut -d= -f2)
    DETECTED_ACTIVITY=$(echo "$APK_INFO" | grep "ACTIVITY=" | cut -d= -f2)
  fi
fi

# If still empty, try apkanalyzer
if [ -z "$PACKAGE" ]; then
  echo "Python extraction failed, trying apkanalyzer..."
  PACKAGE=$(apkanalyzer manifest application-id "$APK_PATH" 2>/dev/null || echo "")
fi

if [ -z "$PACKAGE" ]; then
  echo "ERROR: Could not extract package name from APK"
  echo "Please ensure the APK is valid or manually specify package name"
  exit 1
fi

echo "Package: $PACKAGE"

echo "=== Uninstalling Existing App (if any) ==="
adb uninstall "$PACKAGE" 2>/dev/null || echo "No existing app to uninstall"

echo "=== Installing APK (Fresh Install) ==="
adb install -t "$APK_PATH"
echo "APK installed successfully"

echo "=== Verifying Package Installation and Getting Package Name ==="
# If package detection failed earlier, get it from installed packages
if [ -z "$PACKAGE" ]; then
  echo "Package name not detected from APK, getting from installed app..."
  # Get the most recently installed package (3rd party only)
  PACKAGE=$(adb shell pm list packages -3 | grep -v "io.appium" | grep -v "com.android" | tail -1 | cut -d: -f2 | tr -d '\r')
  if [ -n "$PACKAGE" ]; then
    echo "Detected installed package: $PACKAGE"
  else
    echo "ERROR: Could not detect package name from installed apps"
    exit 1
  fi
else
  adb shell pm list packages | grep "$PACKAGE" || echo "WARNING: Package not found in installed packages"
fi

echo "=== Launching App ==="
# Try to get launchable activity from aapt
ACTIVITY=$(aapt dump badging "$APK_PATH" 2>/dev/null | grep "launchable-activity: name=" | sed "s/.*name='\([^']*\)'.*/\1/" || echo "")

# If aapt failed but Python detected activity, use that
if [ -z "$ACTIVITY" ] && [ -n "$DETECTED_ACTIVITY" ]; then
  ACTIVITY="$DETECTED_ACTIVITY"
  echo "Using activity detected by Python: $ACTIVITY"
fi

if [ -n "$ACTIVITY" ]; then
  echo "Launching: $PACKAGE/$ACTIVITY"
  adb shell am start -n "$PACKAGE/$ACTIVITY"
  LAUNCH_STATUS=$?
else
  echo "Could not find launchable activity, using monkey to launch: $PACKAGE"
  adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1
  LAUNCH_STATUS=$?
fi

if [ $LAUNCH_STATUS -ne 0 ]; then
  echo "WARNING: App launch may have failed (status: $LAUNCH_STATUS)"
fi

echo "=== Waiting for App to Fully Load ==="
sleep 3


echo "=== Waiting for WebView Content to Load ==="
# Poll for UI elements with reduced timeout
MAX_WAIT=15
WAIT_COUNT=0
CONTENT_READY=false

while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
  if adb shell uiautomator dump >/dev/null 2>&1 && \
     adb pull /sdcard/window_dump.xml /tmp/ui_check.xml >/dev/null 2>&1 && \
     grep -qE "clickable=\"true\"|resource-id=" /tmp/ui_check.xml 2>/dev/null; then
    echo "UI content loaded (${WAIT_COUNT}s)"
    CONTENT_READY=true
    break
  fi

  sleep 1
  WAIT_COUNT=$((WAIT_COUNT + 1))
done

if [ "$CONTENT_READY" = false ]; then
  echo "WARNING: UI content check timed out after ${MAX_WAIT}s, proceeding anyway"
fi

mkdir -p reports
[ -f /tmp/ui_check.xml ] && cp /tmp/ui_check.xml reports/ui_hierarchy.xml || echo "No UI hierarchy available"

echo "=== Running Tests ==="
TEST_FAILED=0
for module_dir in tests/android/*/; do
  if [ -d "$module_dir" ]; then
    for test_file in "$module_dir"*.xlsx; do
      if [ -f "$test_file" ]; then
        echo "Executing: $test_file"
        python testzen.py run --file "$test_file" --platform android || TEST_FAILED=1
      fi
    done
  fi
done

echo "=== Generating Report ==="
python scripts/ci/generate_module_report.py android tests reports

echo "=== Teardown: Uninstalling App ==="
adb uninstall "$PACKAGE" || echo "Could not uninstall app"

echo "=== Stopping Appium ==="
kill $APPIUM_PID || true

echo "=== Test Execution Complete ==="

exit $TEST_FAILED
