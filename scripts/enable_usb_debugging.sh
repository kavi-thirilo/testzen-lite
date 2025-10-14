#!/bin/bash
# Enable USB Debugging on Android Emulator
# This script automatically enables developer options and USB debugging

echo "=========================================="
echo "Enable USB Debugging on Emulator"
echo "=========================================="

# Check if emulator is running
DEVICE_ID=$(adb devices | grep emulator | head -1 | awk '{print $1}')

if [ -z "$DEVICE_ID" ]; then
    echo "ERROR: No emulator detected"
    echo "Please start an emulator first with: ./testzen emulator launch"
    exit 1
fi

echo "Found device: $DEVICE_ID"
echo ""
echo "Enabling USB debugging..."

# Enable developer options
adb -s $DEVICE_ID shell settings put global development_settings_enabled 1

# Enable USB debugging
adb -s $DEVICE_ID shell settings put global adb_enabled 1

# Verify settings
echo ""
echo "Verifying settings..."
DEV_ENABLED=$(adb -s $DEVICE_ID shell settings get global development_settings_enabled)
ADB_ENABLED=$(adb -s $DEVICE_ID shell settings get global adb_enabled)

echo "Developer options enabled: $DEV_ENABLED"
echo "USB debugging enabled: $ADB_ENABLED"

if [ "$ADB_ENABLED" = "1" ]; then
    echo ""
    echo "✓ SUCCESS: USB debugging is now enabled!"
    echo ""
    echo "Restarting adb to ensure clean connection..."
    adb kill-server
    sleep 2
    adb start-server
    sleep 2
    adb devices
    echo ""
    echo "You can now run your tests!"
else
    echo ""
    echo "WARNING: USB debugging could not be verified"
    echo "You may need to enable it manually"
fi

echo "=========================================="
