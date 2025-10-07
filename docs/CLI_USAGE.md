# [TZ] TestZen CLI Usage Guide

## Overview
The TestZen command-line interface provides streamlined test execution with platform filtering, individual test selection capabilities, automated mobile app login functionality, and intelligent Smart Element Recovery System for robust UI interactions.

## Installation
Make sure the testzen script is executable:
```bash
chmod +x testzen
```

## New Features

### Automatic Appium Management
- **Auto-Start**: TestZen automatically detects if Appium is running and starts it if needed
- **Smart Detection**: Only checks/starts Appium for commands that require it (`run`, `inspector`)
- **Background Execution**: Appium runs quietly in the background with logs saved to `/tmp/appium.log`
- **Professional Logging**: New TestZen-branded logging format with color-coded status messages

### TestZen Logging Format
All TestZen operations now use a consistent, professional logging format:
- `[TestZen] [HH:MM:SS] [INFO]` - General information
- `[TestZen] [HH:MM:SS] [SUCCESS]` - Successful operations (Green)
- `[TestZen] [HH:MM:SS] [WARNING]` - Warnings (Yellow)
- `[TestZen] [HH:MM:SS] [ERROR]` - Errors (Red)

## Basic Commands

### Run All Tests
```bash
./testzen run --all
```

### Platform-Specific Testing
```bash
# Run all Android tests
./testzen run --all --platform android

# Run all iOS tests
./testzen run --all --platform ios
```

### Individual Test Execution
```bash
# Run a specific test file
./testzen run --file tests/login_android.xlsx
./testzen run --file tests/profile_ios.xlsx
```

### List Available Tests
```bash
# List all test files
./testzen list

# List platform-specific tests
./testzen list --platform android
./testzen list --platform ios
```

### Utility Commands
```bash
# Show TestZen Inspector information
./testzen inspector

# Show version information
./testzen version

# Quick version check (lightweight)
python3 testzen_version.py
```

## Version Management

TestZen includes comprehensive version tracking and management:

### Show Version Information
```bash
# Quick version check (lightweight, no framework dependencies)
python3 testzen_version.py

# Full version details via CLI (requires framework)
./testzen version
```

### Version Management Commands
```bash
# Show current version details
python3 scripts/version_manager.py current

# List all versions in history  
python3 scripts/version_manager.py list

# Create new release (patch/minor/major)
python3 scripts/version_manager.py release patch
python3 scripts/version_manager.py release minor  
python3 scripts/version_manager.py release major

# Generate complete changelog
python3 scripts/version_manager.py changelog

# Update version catalog documentation
python3 scripts/version_manager.py update

# Add change log entry to specific version
python3 scripts/version_manager.py add 2.0.0 new_features "Added dual distribution support"
python3 scripts/version_manager.py add 2.0.0 bug_fixes "Fixed element recovery issue"
```

### Change Log Categories
- `major_features` - Significant new capabilities that expand framework functionality
- `breaking_changes` - Changes that require user action or may break existing implementations
- `new_features` - Backwards compatible new capabilities
- `technical_improvements` - Internal enhancements, performance improvements, code quality
- `documentation` - Documentation updates, guides, examples
- `bug_fixes` - Resolved issues, stability improvements
- `security` - Security enhancements, vulnerability fixes

### Beta Development Workflow

TestZen follows a structured beta development process:

#### Current Status: Beta Phase v1.0.0-beta.1
```bash
# Check current beta version
python3 testzen_version.py

# Show comprehensive version details
python3 scripts/version_manager.py current
```

#### Beta Release Management
```bash
# Create next beta increment (e.g., 1.0.0-beta.2)
python3 scripts/version_manager.py release beta

# Graduate to stable release (1.0.0)
python3 scripts/version_manager.py release release
```

#### Example Beta Development Cycle
```bash
# 1. Test framework with complex scenarios
./testzen run --file tests/complex_mobile_app.xlsx

# 2. Document findings and improvements needed
python3 scripts/version_manager.py add 1.0.0-beta.1 bug_fixes "Fixed dynamic UI element handling"

# 3. Create next beta with improvements
python3 scripts/version_manager.py release beta
python3 scripts/version_manager.py add 1.0.0-beta.2 technical_improvements "Optimized element recovery performance"

# 4. Update documentation
python3 scripts/version_manager.py changelog
```

#### Beta Development Resources
- **[BETA_DEVELOPMENT_GUIDE.md](BETA_DEVELOPMENT_GUIDE.md)** - Complete beta testing and development guide
- **[VERSION_CATALOG.md](VERSION_CATALOG.md)** - Version history and roadmap
- **Beta Testing Areas**: Complex scenarios, edge cases, real-world applications

## Examples

### Complete Testing Workflow
```bash
# 1. List available tests (no Appium needed)
./testzen list

# 2. Run tests (Appium auto-starts if needed)
./testzen run --all --platform android

# 3. Check individual test
./testzen run --file tests/specific_test.xlsx

# Note: Appium is automatically managed by TestZen Lite!
# - Auto-starts when running tests
# - Runs in background
# - Logs saved to /tmp/appium.log
```

### Development Workflow
```bash
# List tests to see what's available
./testzen list --platform android

# Run a single test for debugging
./testzen run --file tests/login_android.xlsx

# Run all tests for final validation
./testzen run --all
```

## Command Reference

| Command | Description |
|---------|-------------|
| `./testzen run --all` | Execute all available tests |
| `./testzen run --all --platform <android\|ios>` | Execute platform-specific tests |
| `./testzen run --file <path>` | Execute a single test file |
| `./testzen list` | Show all available test files |
| `./testzen list --platform <android\|ios>` | Show platform-specific tests |
| `./testzen inspector` | Display TestZen Inspector information |
| `./testzen version` | Show framework version |

## Supported Actions in Excel Files

| Action | Description | Input Data | Locator Value |
|--------|-------------|------------|---------------|
| `click` | Click any element | Not required | XPath locator |
| `input` | Enter text in field | Text to enter | XPath locator |
| `verify` | Verify element presence | Not required | XPath locator |
| `launch` | Launch mobile app | Not required | App package name |
| `install` | Install APK file | Not required | APK file path |
| `wait` | Wait for specified time | Seconds to wait | Not required |
| `scroll` | Scroll to find element | Not required | XPath locator |
| `long_press` | Long press element | Duration (ms) | XPath locator |

## Test File Organization
- Test files should be placed in the `tests/` directory
- Use `.xlsx` extension for Excel-based test scenarios
- **Available Test Files:**
- `Mobile_App_Login_Test.xlsx` - Original 15-step login flow (legacy)
- `Mobile_App_Login_Test.xlsx` - **Current streamlined 6-step login test**
- Platform-specific naming recommended:
- `login_android.xlsx`
- `profile_ios.xlsx`
- `checkout_android.xlsx`

## Advanced Features

### Custom Login Automation
- Create custom login flows using basic actions (click, input, verify)
- Implement app-specific handlers for complex authentication if needed
- Use modular approach for reusable login sequences

### Smart Element Recovery System
- **Automatic UI Recovery**: When elements are found but not clickable, the system automatically:
- Traverses UI hierarchy to find clickable parent containers
- Searches for actionable sibling elements
- Identifies nearby clickable alternatives
- Provides fallback strategies based on element descriptions
- **Recovery Logging**: All recovery actions are logged with helpful hints
- **Report Integration**: Recovery insights included in test reports for optimization
- **Zero Breaking Changes**: Existing tests automatically benefit from smart recovery

## Prerequisites
- Android/iOS device connected and authorized
- Test files present in `tests/` directory
- Node.js and npm installed (for Appium dependencies)
- TestZen Inspector (optional, for debugging)

**Note**: Appium server is automatically managed by TestZen and will start when needed!