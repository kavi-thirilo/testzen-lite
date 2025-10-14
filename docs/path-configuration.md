# PATH Configuration Guide

Guide for configuring PATH environment variable for TestZen tools.

---

## What is PATH?

PATH is an environment variable that tells your operating system where to find commands.

When PATH is not configured correctly, you'll see errors like:
- "command not found: python"
- "command not found: adb"
- "command not found: appium"

---

## Check Your Current PATH

```bash
# macOS/Linux
echo $PATH

# Windows
echo %PATH%
```

You should see multiple directories separated by:
- **Colons (`:`)** on macOS/Linux
- **Semicolons (`;`)** on Windows

---

## Configure PATH for Each Tool

### Python

**macOS/Linux:**
```bash
echo 'export PATH=$PATH:/usr/local/bin' >> ~/.zshrc  # macOS
# OR
echo 'export PATH=$PATH:/usr/local/bin' >> ~/.bashrc  # Linux

source ~/.zshrc  # or source ~/.bashrc
```

**Windows:**
- Reinstall Python
- During installation, check **"Add Python to PATH"**

---

### Android SDK

**macOS:**
```bash
echo 'export ANDROID_HOME=$HOME/Library/Android/sdk' >> ~/.zshrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools' >> ~/.zshrc
echo 'export PATH=$PATH:$ANDROID_HOME/emulator' >> ~/.zshrc
echo 'export PATH=$PATH:$ANDROID_HOME/tools/bin' >> ~/.zshrc
source ~/.zshrc
```

**Linux:**
```bash
echo 'export ANDROID_HOME=$HOME/Android/Sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/emulator' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/tools/bin' >> ~/.bashrc
source ~/.bashrc
```

**Windows:**
1. Open **System Properties → Environment Variables**
2. Under **User variables**, click **"New"**
3. Variable name: `ANDROID_HOME`
4. Variable value: `C:\Users\YOUR_USERNAME\AppData\Local\Android\Sdk`
5. Click **"OK"**
6. Select **"Path"** variable, click **"Edit"**
7. Click **"New"** and add:
   - `%ANDROID_HOME%\platform-tools`
   - `%ANDROID_HOME%\emulator`
   - `%ANDROID_HOME%\tools\bin`
8. Click **"OK"**
9. Close and reopen terminal

---

### npm and Appium

**Find npm global prefix:**
```bash
npm config get prefix
```

**macOS/Linux:**

If prefix is `/usr/local`:
```bash
echo 'export PATH=$PATH:/usr/local/bin' >> ~/.zshrc  # macOS
# OR
echo 'export PATH=$PATH:/usr/local/bin' >> ~/.bashrc  # Linux

source ~/.zshrc  # or source ~/.bashrc
```

If prefix is `~/.npm-global` (custom):
```bash
echo 'export PATH=$PATH:~/.npm-global/bin' >> ~/.zshrc  # macOS
# OR
echo 'export PATH=$PATH:~/.npm-global/bin' >> ~/.bashrc  # Linux

source ~/.zshrc  # or source ~/.bashrc
```

**Windows:**
- npm global packages are usually added to PATH automatically
- If not, add: `%APPDATA%\npm` to PATH using System Properties

---

## Understanding Shell Configuration Files

### macOS

**Which file to use?**
```bash
echo $SHELL
```

- If output is `/bin/zsh` → Use `~/.zshrc`
- If output is `/bin/bash` → Use `~/.bash_profile` or `~/.bashrc`

### Linux

Usually `~/.bashrc` for bash shell.

Check: `echo $SHELL`

---

## Verify PATH Configuration

After configuring PATH, verify each command works:

```bash
# Close and reopen terminal first!

python3 --version     # Should show version
node --version        # Should show version
npm --version         # Should show version
adb version          # Should show version
appium --version     # Should show version
emulator -list-avds  # Should list emulators or show command help
```

---

## Important Notes

1. **Always close and reopen terminal** after changing PATH
2. **Use correct shell config file** (~/.zshrc for zsh, ~/.bashrc for bash)
3. **Don't use sudo** when editing PATH
4. **Test each command** after adding to PATH

---

## Still Not Working?

If commands still don't work after configuring PATH:

1. **Verify tool is actually installed**
   - Python: Check `/usr/local/bin/python3` exists
   - Android SDK: Check `$ANDROID_HOME/platform-tools/adb` exists
   - Appium: Check `npm list -g appium`

2. **Check for typos in PATH**
   ```bash
   echo $PATH
   # Look for the directories you added
   ```

3. **Try absolute path**
   ```bash
   /usr/local/bin/python3 --version
   ~/Library/Android/sdk/platform-tools/adb version
   ```

4. **Reload shell config manually**
   ```bash
   source ~/.zshrc  # or source ~/.bashrc
   ```

---

## Windows-Specific Issues

### Changes don't take effect

- Close **all** terminal/command prompt windows
- Reopen and try again

### Can't find Environment Variables settings

1. Press **Windows + R**
2. Type: `sysdm.cpl`
3. Press **Enter**
4. Click **"Environment Variables"** button

---

## Next Steps

After PATH is configured:

1. [Verify installation](installation.md)
2. [Set up device](device-setup.md)
3. [Run tests](../README.md#run-tests)

---

[← Back to Installation Guide](installation.md) | [← Back to Main README](../README.md)
