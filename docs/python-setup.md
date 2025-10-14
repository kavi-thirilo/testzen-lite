# Python Setup Guide

Complete guide for installing Python 3.8+ for TestZen.

---

## Check If You Have Python

```bash
python3 --version
```

**Expected:** `Python 3.8.0` or higher

If you see "command not found" or version is below 3.8, follow the installation steps below.

---

## Installation by Platform

### macOS

**Option 1: Using Homebrew (Recommended)**
```bash
brew install python3
```

**Option 2: Official Installer**
1. Visit https://www.python.org/downloads/
2. Download Python 3.8+ for macOS
3. Run the installer
4. Follow installation wizard

---

### Linux

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install python3 python3-pip
```

**Fedora/RHEL:**
```bash
sudo dnf install python3 python3-pip
```

---

### Windows

1. Visit https://www.python.org/downloads/
2. Download Python 3.8+ for Windows
3. Run the installer
4. **IMPORTANT:** Check "Add Python to PATH"
5. Click "Install Now"

---

## Verify Installation

```bash
python3 --version  # Should show Python 3.8+
pip3 --version     # Should show pip version
```

**On Windows,** you might use `python` instead of `python3`:
```bash
python --version
pip --version
```

---

## Troubleshooting

### "command not found: pip"

Try these alternatives:
```bash
pip3 --version          # Try pip3
python3 -m pip --version  # Use Python module
python -m pip --version   # Windows
```

### Python installed but command doesn't work

**Check PATH configuration:** See [PATH Configuration Guide](path-configuration.md)

---

## Next Steps

After Python is installed:

1. Continue with [Installation Guide](installation.md)
2. Or [check other prerequisites](installation.md#prerequisites)

---

[← Back to Installation Guide](installation.md) | [← Back to Main README](../README.md)
