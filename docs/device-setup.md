# Device and Emulator Setup Guide

Complete guide for setting up Android devices and emulators for TestZen.

---

## Quick Check

```bash
adb devices
```

**Expected:** At least one device showing with "device" status

If list is empty or shows "offline"/"unauthorized", follow the guide below.

---

## Option 1: Using Emulator (Recommended for Testing)

### Check Available Emulators

```bash
emulator -list-avds
```

### If No Emulators Exist - Create One

**Using Android Studio (Easiest):**

1. Open Android Studio
2. Go to: **Tools → Device Manager**
3. Click **"Create Device"**
4. Choose device (e.g., **Pixel 4** or **Pixel 5**)
5. Click **"Next"**
6. Download system image (e.g., **Android 11 / API 30**)
7. Click **"Next"**
8. Name your AVD (e.g., "Pixel_4_API_30")
9. Click **"Finish"**

**Using Command Line:**
```bash
# Download system image
sdkmanager "system-images;android-30;google_apis;x86_64"

# Create emulator
avdmanager create avd -n Pixel_4_API_30 \
  -k "system-images;android-30;google_apis;x86_64" \
  -d pixel_4

# Verify
emulator -list-avds
```

---

### Launch Emulator

**Let TestZen start it automatically:**
```bash
./testzen run --file your_test.xlsx
# TestZen will launch emulator if needed
```

**Or start manually:**
```bash
# Using TestZen command
./testzen emulator launch

# Or with specific emulator
./testzen emulator launch --avd Pixel_4_API_30

# Or using Android SDK directly
emulator @Pixel_4_API_30
```

**Wait 30-60 seconds for emulator to fully boot.**

---

### Enable USB Debugging on Emulator

If emulator shows as "offline" or "unauthorized":

**Automatic Fix:**
```bash
./scripts/enable_usb_debugging.sh
```

**Manual Fix:**
1. Look at emulator screen
2. Go to: **Settings → About emulated device**
3. Tap **"Build number"** 7 times (enables Developer Options)
4. Go back to: **Settings → System → Developer options**
5. Enable **"USB debugging"**

**Restart adb:**
```bash
adb kill-server
adb start-server
adb devices  # Should now show "device"
```

---

## Option 2: Using Physical Android Device

### Step 1: Enable Developer Options

1. On your phone, go to: **Settings → About phone**
2. Find **"Build number"**
3. Tap it **7 times**
4. You'll see: "You are now a developer!"

---

### Step 2: Enable USB Debugging

1. Go back to: **Settings → System → Developer options**
2. Enable **"USB debugging"**
3. Connect phone to computer via USB cable

---

### Step 3: Authorize Computer

```bash
adb devices
```

**If you see "unauthorized":**
- Look at your phone screen
- You should see a popup: "Allow USB debugging?"
- Check **"Always allow from this computer"**
- Tap **"Allow"**

**Run adb devices again:**
```bash
adb devices
# Should now show: <device-id>  device
```

---

## Option 3: Using iOS Device (macOS only)

iOS testing requires additional setup:

1. **Xcode** must be installed
2. **iOS device** connected via USB
3. **Developer profile** configured

For detailed iOS setup, see Apple's developer documentation.

---

## Troubleshooting

### Device shows as "offline"

```bash
# Restart adb server
adb kill-server
adb start-server
adb devices

# If still offline, enable USB debugging (see above)
```

---

### Device shows as "unauthorized"

**On emulator:**
- Use the automatic script: `./scripts/enable_usb_debugging.sh`
- Or manually enable USB debugging (see above)

**On physical device:**
- Look at phone screen for authorization popup
- If no popup appears, disable and re-enable USB debugging
- Or revoke USB authorizations: **Developer options → Revoke USB debugging authorizations**
- Then reconnect and authorize again

---

### Multiple devices connected

TestZen will use the first available device. To use a specific device:

```bash
# List all devices
adb devices

# Run test on specific device
./testzen run --file your_test.xlsx --device emulator-5554
```

---

### Emulator won't start

**Check if virtualization is enabled:**
- BIOS/UEFI must have virtualization enabled (Intel VT-x or AMD-V)

**Check available emulators:**
```bash
emulator -list-avds
```

**Try launching directly:**
```bash
emulator @Pixel_4_API_30 -verbose
# Check error messages
```

---

### No emulators available to launch

Create one first - see ["If No Emulators Exist"](#if-no-emulators-exist---create-one) above.

---

## Next Steps

After device setup:

1. [Add your mobile app](../README.md#add-your-app)
2. [Create test files](test-creation.md)
3. [Run your first test](../README.md#run-tests)

---

[← Back to Installation Guide](installation.md) | [← Back to Main README](../README.md)
