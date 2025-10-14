# Installation Guide

Complete installation guide for TestZen Lite.

## Prerequisites

Before installing TestZen, ensure you have:

- **Python 3.8+** - [Setup Guide](python-setup.md)
- **Node.js 16+** - [Setup Guide](nodejs-setup.md)
- **Android SDK** - [Setup Guide](android-sdk-setup.md)
- **Appium** - [Setup Guide](appium-setup.md)

Not sure if you have these? Each guide includes a "Check if installed" section.

---

## Quick Installation

### Step 1: Clone Repository

```bash
git clone https://github.com/kavi-thirilo/testzen-lite.git
cd testzen-lite
```

### Step 2: Install Python Dependencies

```bash
# Try one of these (use whichever works):
pip3 install -r requirements.txt
# OR
python3 -m pip install -r requirements.txt
# OR (Windows)
python -m pip install -r requirements.txt
```

### Step 3: Install Appium

```bash
npm install -g appium
appium driver install uiautomator2  # For Android
appium driver install xcuitest      # For iOS (macOS only)
```

### Step 4: Verify Installation

```bash
./testzen --help
```

You should see the TestZen help menu.

---

## What If Something's Missing?

If you get errors during installation:

- **"command not found: pip"** → See [Python Setup Guide](python-setup.md)
- **"command not found: npm"** → See [Node.js Setup Guide](nodejs-setup.md)
- **"adb: command not found"** → See [Android SDK Setup Guide](android-sdk-setup.md)
- **"appium: command not found"** → See [Appium Setup Guide](appium-setup.md)
- **PATH issues** → See [PATH Configuration Guide](path-configuration.md)

---

## Next Steps

After installation:

1. [Add your mobile app](../README.md#add-your-app)
2. [Create test files](test-creation.md)
3. [Run your first test](../README.md#run-tests)

---

[← Back to Main README](../README.md)
