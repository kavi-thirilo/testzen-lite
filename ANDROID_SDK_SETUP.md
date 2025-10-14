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

## Issue 2: Install Appium UiAutomator2 Driver

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

**If you get permission errors:**
```bash
# Option 1: Fix npm permissions (recommended)
mkdir ~/.npm-global
npm config set prefix '~/.npm-global'
echo 'export PATH=~/.npm-global/bin:$PATH' >> ~/.zshrc
source ~/.zshrc

# Then try again:
npm install -g appium
appium driver install uiautomator2
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
