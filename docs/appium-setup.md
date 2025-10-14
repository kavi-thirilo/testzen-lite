# Appium Setup Guide

Complete guide for installing Appium and required drivers for TestZen.

---

## Prerequisites

**Node.js and npm must be installed first.**

Check if you have them:
```bash
node --version  # Should be v16+
npm --version
```

If not, see [Node.js Setup Guide](nodejs-setup.md)

---

## Check If You Have Appium

```bash
appium --version
```

**Expected:** Version 2.x or 3.x

If you see "command not found", follow the installation steps below.

---

## Installation

### Step 1: Install Appium

```bash
npm install -g appium
```

**If you get permission errors (EACCES):**
- See [Node.js Setup Guide - Fix npm Permission Errors](nodejs-setup.md#fix-npm-permission-errors)

---

### Step 2: Install Drivers

**For Android Testing:**
```bash
appium driver install uiautomator2
```

**For iOS Testing (macOS only):**
```bash
appium driver install xcuitest
```

---

### Step 3: Verify Installation

```bash
# Check Appium version
appium --version

# Check installed drivers
appium driver list --installed

# You should see:
# - uiautomator2@... (for Android)
# - xcuitest@... (for iOS, if on macOS)
```

---

### Step 4: Test Appium Server

```bash
# Start Appium manually (press Ctrl+C to stop)
appium --allow-insecure=*:chromedriver_autodownload

# You should see:
# [Appium] Welcome to Appium v...
# [Appium] Appium REST http interface listener started on 0.0.0.0:4723
```

If it starts successfully, press Ctrl+C and you're done!

---

## Troubleshooting

### "appium: command not found" after installation

npm global bin folder is not in your PATH.

**Fix:**
```bash
# Find where npm installs global packages
npm config get prefix

# Add to PATH (macOS/Linux)
echo 'export PATH=$PATH:/usr/local/bin' >> ~/.zshrc
# OR if custom prefix:
echo 'export PATH=$PATH:~/.npm-global/bin' >> ~/.zshrc

# Reload terminal
source ~/.zshrc  # or source ~/.bashrc

# Try again
appium --version
```

**For detailed PATH help:** See [PATH Configuration Guide](path-configuration.md)

---

### "Could not find a driver for automationName 'UiAutomator2'"

You forgot to install the driver.

**Fix:**
```bash
appium driver install uiautomator2
appium driver list --installed  # Verify
```

---

### npm cache or permission errors during driver install

```bash
# Clean npm cache
npm cache clean --force

# Fix npm ownership
sudo chown -R $(whoami) ~/.npm

# Try installing driver again
appium driver install uiautomator2
```

---

### "Failed to start Appium server"

Common causes:

**1. Port 4723 already in use**
```bash
# Check what's using port 4723
lsof -i :4723  # macOS/Linux
netstat -ano | findstr :4723  # Windows

# Kill existing Appium
pkill -f appium
# OR manually kill the process ID shown above
```

**2. Appium driver not installed**
```bash
appium driver list --installed
# If empty, install driver
appium driver install uiautomator2
```

**3. Check Appium log for details**
```bash
cat /tmp/appium.log
```

---

## Appium 2.x vs 3.x

TestZen supports both Appium 2.x and 3.x.

**Key Difference:**
- Appium 3.x requires `*:` prefix for insecure features
- TestZen handles this automatically

**Manual Appium startup:**
```bash
# Appium 3.x format (works with both 2.x and 3.x):
appium --allow-insecure=*:chromedriver_autodownload

# Appium 2.x format (only works with 2.x):
appium --allow-insecure=chromedriver_autodownload
```

---

## Using npx Instead of Global Install

If you prefer not to install Appium globally:

**TestZen automatically uses npx if global Appium is not found.**

No additional setup needed - just ensure Node.js and npm are installed.

---

## Next Steps

After Appium is installed:

1. [Set up Android SDK](android-sdk-setup.md)
2. [Set up device/emulator](device-setup.md)
3. [Run your first test](../README.md#run-tests)

---

[← Back to Installation Guide](installation.md) | [← Back to Main README](../README.md)
