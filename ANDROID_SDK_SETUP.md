# Android SDK Setup Guide for TestZen

This guide will help you fix the "adb not found" and "Appium driver missing" errors.

---

## Issue 1: Fix Android SDK PATH (adb not found)

### Step 1: Find Your Android SDK Location

The Android SDK is usually installed in one of these locations:

**macOS:**
```bash
$HOME/Library/Android/sdk
```

**Linux:**
```bash
$HOME/Android/Sdk
```

**Windows:**
```
C:\Users\YOUR_USERNAME\AppData\Local\Android\Sdk
```

### Step 2: Verify SDK Location Exists

Run this command to check if the SDK exists:

**macOS/Linux:**
```bash
ls -la $HOME/Library/Android/sdk/platform-tools
```

If you see files like `adb`, `fastboot`, etc., your SDK is installed here.

If you get "No such file or directory", try:
```bash
ls -la $HOME/Android/Sdk/platform-tools
```

**If both fail, you need to install Android Studio first** (see bottom of this guide).

### Step 3: Set ANDROID_HOME Environment Variable

Once you've confirmed your SDK location, set the ANDROID_HOME variable.

**macOS (using zsh - default on macOS Catalina+):**

```bash
# Open your shell configuration file
nano ~/.zshrc

# Add these lines at the end:
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/tools/bin

# Save the file:
# Press Ctrl+X, then Y, then Enter
```

**macOS (using bash):**

```bash
# Open your shell configuration file
nano ~/.bash_profile

# Add the same lines as above
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/tools/bin

# Save: Ctrl+X, then Y, then Enter
```

**Linux:**

```bash
# Open your shell configuration file
nano ~/.bashrc

# Add these lines:
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/tools/bin

# Save: Ctrl+X, then Y, then Enter
```

**Windows (Command Prompt):**

1. Search for "Environment Variables" in Windows search
2. Click "Edit the system environment variables"
3. Click "Environment Variables" button
4. Under "User variables", click "New"
5. Variable name: `ANDROID_HOME`
6. Variable value: `C:\Users\YOUR_USERNAME\AppData\Local\Android\Sdk`
7. Click OK
8. Find "Path" variable, click "Edit"
9. Click "New" and add: `%ANDROID_HOME%\platform-tools`
10. Click "New" and add: `%ANDROID_HOME%\emulator`
11. Click OK on all dialogs

### Step 4: Reload Your Terminal

**macOS/Linux:**
```bash
# Close and reopen your terminal, OR run:
source ~/.zshrc       # if using zsh
# OR
source ~/.bash_profile  # if using bash
# OR
source ~/.bashrc      # on Linux
```

**Windows:**
Close and reopen Command Prompt

### Step 5: Verify adb Works

```bash
# Check if adb is now found
adb version

# Should show something like:
# Android Debug Bridge version 1.0.41
# Version 34.0.4-10411341

# Check if emulator command works
emulator -version

# Should show emulator version
```

**If you still get "command not found":**
- Double-check your ANDROID_HOME path is correct
- Make sure you reloaded your terminal
- Try opening a completely new terminal window

---

## Issue 2: Enable USB Debugging on Emulator

### The Problem

When you run `adb devices`, you might see:
```
List of devices attached
emulator-5554    offline
```
or
```
List of devices attached
emulator-5554    unauthorized
```

This means USB debugging is disabled on the emulator.

### Automatic Fix (Easiest)

```bash
# Run the automated script
./scripts/enable_usb_debugging.sh
```

This script will:
- Find your running emulator
- Enable developer options
- Enable USB debugging
- Restart adb server
- Verify it worked

### Manual Fix

If the script doesn't work or you prefer manual setup:

**Step 1: Enable Developer Options**

1. Look at your emulator screen
2. Open **Settings** app
3. Scroll down to **About emulated device** (or **About phone**)
4. Tap on **Build number** 7 times
5. You'll see a message: "You are now a developer!"

**Step 2: Enable USB Debugging**

1. Go back to **Settings**
2. Find **System** → **Developer options**
3. Scroll down to **USB debugging**
4. Toggle it **ON**
5. If prompted, tap **OK** to allow USB debugging

**Step 3: Restart ADB**

```bash
adb kill-server
adb start-server
sleep 3
adb devices
```

You should now see:
```
List of devices attached
emulator-5554    device
```

Notice it says **"device"** not "offline" or "unauthorized"!

### For Physical Devices

If you're using a real Android phone:

1. Go to **Settings** → **About phone**
2. Tap **Build number** 7 times
3. Go back to **Settings** → **Developer options**
4. Enable **USB debugging**
5. Connect phone via USB cable
6. Look at phone screen - you'll see a prompt "Allow USB debugging?"
7. Check "Always allow from this computer" and tap **OK**
8. Run `adb devices` - phone should show as "device"

---

## Issue 3: Install Appium UiAutomator2 Driver

### Step 1: Verify Appium is Installed

```bash
appium --version
```

If this shows a version number (e.g., 2.0.0 or 3.1.0), Appium is installed.

If you get "command not found", install Appium first:
```bash
npm install -g appium
```

### Step 2: Check Installed Drivers

```bash
appium driver list --installed
```

If you see "No drivers have been installed", continue to Step 3.

### Step 3: Install UiAutomator2 Driver

```bash
appium driver install uiautomator2
```

**Expected output:**
```
✔ Installing 'uiautomator2'
✔ Driver 'uiautomator2' successfully installed
```

**If you get "EACCES: permission denied" or npm cache errors:**

```bash
# Solution 1: Clear npm cache and fix ownership (quick fix)
npm cache clean --force
sudo chown -R $(whoami) ~/.npm
appium driver install uiautomator2

# If that doesn't work, use Solution 2:

# Solution 2: Fix npm permissions permanently (recommended)
mkdir ~/.npm-global
npm config set prefix '~/.npm-global'
echo 'export PATH=~/.npm-global/bin:$PATH' >> ~/.zshrc
source ~/.zshrc

# Fix ownership of npm folders
sudo chown -R $(whoami) ~/.npm
sudo chown -R $(whoami) /usr/local/lib/node_modules

# Clear cache
npm cache clean --force

# Reinstall Appium to the new location
npm install -g appium

# Now install the driver
appium driver install uiautomator2

# Solution 3: Use sudo (quick but not recommended)
sudo npm cache clean --force
sudo appium driver install uiautomator2
```

### Step 4: Verify Driver Installation

```bash
appium driver list --installed
```

**Expected output:**
```
✔ Listing installed drivers
- uiautomator2@3.0.4 [installed (npm)]
```

---

## Issue 3: Verify Everything Works

### Final Verification Commands

Run these commands to make sure everything is set up correctly:

```bash
# 1. Check ANDROID_HOME is set
echo $ANDROID_HOME
# Should show: /Users/yourname/Library/Android/sdk (or similar)

# 2. Check adb works
adb version
# Should show: Android Debug Bridge version...

# 3. Check emulator command works
emulator -list-avds
# Should list your AVDs (may be empty if you haven't created any)

# 4. Check adb can see devices
adb devices
# Should show: List of devices attached

# 5. Check Appium is installed
appium --version
# Should show: 2.0.0 or 3.1.0 or similar

# 6. Check Appium driver is installed
appium driver list --installed
# Should show: uiautomator2@...
```

### All Green? Test TestZen!

If all the above commands work, you're ready to run tests:

```bash
cd /path/to/testzen-lite

# Start an emulator (if not already running)
./testzen emulator launch

# Run a test
./testzen run --file tests/android/billing/Payment_Form_Validation_Test.xlsx
```

---

## Troubleshooting

### "I don't have Android SDK installed at all"

**Install Android Studio (Easiest):**

1. Download Android Studio from: https://developer.android.com/studio
2. Install and open Android Studio
3. Go to: **Settings** → **Appearance & Behavior** → **System Settings** → **Android SDK**
4. Note the "Android SDK Location" path
5. Make sure these are checked:
   - Android SDK Platform-Tools
   - Android SDK Command-line Tools
   - Android SDK Build-Tools
6. Click "Apply" to install
7. Create an AVD: **Tools** → **Device Manager** → **Create Virtual Device**
8. Follow Step 3 above to set ANDROID_HOME to the SDK location shown

### "ANDROID_HOME is set but adb still not found"

Check if platform-tools exists:
```bash
ls -la $ANDROID_HOME/platform-tools
```

If it doesn't exist, install platform-tools in Android Studio:
1. Open Android Studio
2. Settings → Android SDK → SDK Tools tab
3. Check "Android SDK Platform-Tools"
4. Click Apply

### "appium driver install fails with permission error"

Fix npm global permissions:
```bash
mkdir ~/.npm-global
npm config set prefix '~/.npm-global'
echo 'export PATH=~/.npm-global/bin:$PATH' >> ~/.zshrc
source ~/.zshrc

# Now reinstall
npm install -g appium
appium driver install uiautomator2
```

### "Which shell am I using?"

```bash
echo $SHELL
```

- If shows `/bin/zsh` → edit `~/.zshrc`
- If shows `/bin/bash` → edit `~/.bash_profile` or `~/.bashrc`

### "Environment variables not persisting after reboot"

Make sure you:
1. Added export commands to the correct file (~/.zshrc or ~/.bash_profile)
2. Saved the file
3. Restarted terminal completely

Test by running:
```bash
echo $ANDROID_HOME
```

If empty after reboot, check your shell config file:
```bash
cat ~/.zshrc | grep ANDROID_HOME
```

---

## Quick Reference Card

**Copy-paste setup for macOS:**

```bash
# Set ANDROID_HOME (adjust path if your SDK is elsewhere)
echo 'export ANDROID_HOME=$HOME/Library/Android/sdk' >> ~/.zshrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools' >> ~/.zshrc
echo 'export PATH=$PATH:$ANDROID_HOME/emulator' >> ~/.zshrc
echo 'export PATH=$PATH:$ANDROID_HOME/tools/bin' >> ~/.zshrc
source ~/.zshrc

# Verify
adb version
emulator -list-avds

# Install Appium driver
appium driver install uiautomator2

# Verify
appium driver list --installed

# You're ready!
./testzen run --file tests/android/billing/Payment_Form_Validation_Test.xlsx
```

**Copy-paste setup for Linux:**

```bash
# Set ANDROID_HOME (adjust path if your SDK is elsewhere)
echo 'export ANDROID_HOME=$HOME/Android/Sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/emulator' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/tools/bin' >> ~/.bashrc
source ~/.bashrc

# Verify
adb version
emulator -list-avds

# Install Appium driver
appium driver install uiautomator2

# Verify
appium driver list --installed

# You're ready!
./testzen run --file tests/android/billing/Payment_Form_Validation_Test.xlsx
```

---

## Need More Help?

- **README Android Setup Section**: See README.md → Quick Start Guide → Step 1 → Android Setup
- **TestZen Issues**: https://github.com/kavi-thirilo/testzen-lite/issues
- **Android SDK Documentation**: https://developer.android.com/studio/command-line/variables
- **Appium Documentation**: https://appium.io/docs/en/latest/quickstart/install/

---

**After completing this setup, run your test again and the errors should be gone!**
