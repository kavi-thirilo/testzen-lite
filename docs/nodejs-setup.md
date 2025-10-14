# Node.js and npm Setup Guide

Complete guide for installing Node.js 16+ and npm for TestZen.

---

## Check If You Have Node.js and npm

```bash
node --version
npm --version
```

**Expected:** Node.js v16.0.0 or higher

If you see "command not found", follow the installation steps below.

---

## Installation by Platform

### macOS

**Option 1: Using Homebrew (Recommended)**
```bash
brew install node
```

**Option 2: Official Installer**
1. Visit https://nodejs.org/
2. Download LTS version for macOS
3. Run the installer
4. Follow installation wizard

---

### Linux

**Ubuntu/Debian:**
```bash
# Install Node.js 18.x LTS
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# Verify npm is included
npm --version
```

**Fedora/RHEL:**
```bash
# Install Node.js 18.x LTS
curl -fsSL https://rpm.nodesource.com/setup_18.x | sudo bash -
sudo dnf install -y nodejs
```

---

### Windows

1. Visit https://nodejs.org/
2. Download LTS version for Windows
3. Run the installer
4. Accept all defaults
5. Restart terminal/command prompt

---

## Verify Installation

```bash
node --version   # Should show v16.x or higher
npm --version    # Should show npm version
```

---

## Fix npm Permission Errors

If you get `EACCES` permission errors when installing packages globally:

```bash
# Create npm global directory
mkdir ~/.npm-global
npm config set prefix '~/.npm-global'

# Add to PATH (macOS/Linux)
echo 'export PATH=~/.npm-global/bin:$PATH' >> ~/.zshrc  # macOS
# OR
echo 'export PATH=~/.npm-global/bin:$PATH' >> ~/.bashrc  # Linux

# Reload terminal
source ~/.zshrc  # or source ~/.bashrc
```

**Verify fix:**
```bash
npm install -g appium  # Should work without sudo
```

---

## Troubleshooting

### "npm: command not found" after Node.js installation

Node.js includes npm, but if it's not found:

**Check if Node.js is in PATH:** See [PATH Configuration Guide](path-configuration.md)

### Permission errors continue

Try using npx instead of global installation:
- TestZen supports both global and npx Appium installations
- npx runs without needing global install

---

## Next Steps

After Node.js and npm are installed:

1. Install [Appium](appium-setup.md)
2. Continue with [Installation Guide](installation.md)

---

[← Back to Installation Guide](installation.md) | [← Back to Main README](../README.md)
