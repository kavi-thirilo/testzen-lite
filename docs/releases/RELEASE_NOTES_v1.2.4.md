# Release Notes - Version 1.2.4

**Release Date:** October 17, 2025

## Overview

This release introduces Allure reporting integration, enhanced documentation, and better troubleshooting support for emulator-related issues.

---

## What's New

### Allure Reporting Integration

**Professional Interactive Reports**
- Added Allure reporting framework as default reporter
- Generates interactive HTML reports with rich visualizations
- Includes test history, trends, and flaky test detection
- Supports screenshot attachments for each test step
- Provides timeline views and execution analytics

**Configurable Reporter System**
- New `config/reporting_config.json` for easy reporter switching
- Support for both Allure and custom HTML reports
- Automatic cleanup of old report files before each test run
- Reporter factory pattern for extensibility

**New Files Added:**
- `src/reports/allure_reporter.py` - Allure report generator
- `src/reports/base_reporter.py` - Reporter interface
- `src/reports/reporter_factory.py` - Reporter factory
- `config/reporting_config.json` - Reporter configuration

### Documentation Improvements

**Better Organization**
- Reorganized all documentation into docs/ folder
- Created dedicated docs/releases/ folder for release notes
- Moved CI/CD and multi-module guides to docs/ folder
- Cleaned up root directory (only README.md remains)
- Updated all internal documentation links

**Enhanced Troubleshooting**
- Added comprehensive emulator crash troubleshooting guide
- Documented solutions for port 5037 conflicts
- Added instructions for handling zombie ADB processes
- Provided OS-specific cleanup commands for macOS/Linux and Windows
- Added quick reference in main README for common issues

---

## Improvements

### Reporting Enhancements

**Allure Integration:**
- Direct JSON file generation (no pytest dependency required)
- Automatic Allure CLI detection and report generation
- Screenshot attachments copied to allure-results
- Step-by-step execution tracking with parameters
- Test metadata including locators, test data, expected/actual results

**Report Management:**
- Automatic cleanup of old reports before test execution
- Separate cleanup for Allure results, HTML reports, and screenshots
- Prevents confusion with stale test results

**Updated Dependencies:**
- Added `allure-pytest>=2.13.0` to requirements.txt

### Documentation Structure

**New File Locations:**
- `docs/releases/` - All release notes organized in subfolder
- `docs/cicd-setup-detailed.md` - Detailed CI/CD setup guide
- `docs/multi-module-guide.md` - Multi-module testing guide
- `docs/troubleshooting.md` - Enhanced with emulator crash section

**Cleanup:**
- Removed outdated backup files
- Consolidated documentation in proper folders
- Fixed all internal documentation links

### Troubleshooting Enhancements

**New Sections:**
- Emulator crashes on launch
- Port 5037 already in use errors
- Zombie process cleanup procedures
- Prevention tips for clean test execution

---

## Bug Fixes

None in this release - documentation focused.

---

## Breaking Changes

None - all changes are backward compatible.

---

## File Changes Summary

**New Files:**
- `src/reports/allure_reporter.py` - Allure report generator implementation
- `src/reports/base_reporter.py` - Abstract base class for reporters
- `src/reports/reporter_factory.py` - Factory for creating reporter instances
- `config/reporting_config.json` - Reporter configuration file

**Moved Files:**
- All RELEASE_NOTES_*.md moved to docs/releases/
- CI_CD_SETUP.md moved to docs/cicd-setup-detailed.md
- MULTI_MODULE_GUIDE.md moved to docs/multi-module-guide.md

**Deleted Files:**
- README_old_backup.md (no longer needed)

**Updated Files:**
- `README.md` - Added Allure reporting instructions and troubleshooting
- `requirements.txt` - Added allure-pytest dependency
- `config/version.json` - Updated to v1.2.4
- `src/automation/testzen_automation.py` - Integrated reporter factory
- `src/reports/professional_reporter.py` - Added base class inheritance and cleanup
- `tests/android/README.md` - Updated guide links
- `tests/ios/README.md` - Updated guide links
- `docs/troubleshooting.md` - Added emulator crash section
- `docs/releases/RELEASE_NOTES_v1.2.4.md` - Added Allure reporting details

---

## Upgrade Instructions

1. Pull the latest changes:
```bash
git pull origin main
```

2. Install new dependencies:
```bash
pip install -r requirements.txt
```

3. (Optional) Install Allure CLI for viewing reports:
```bash
# macOS:
brew install allure

# Or using npm (all platforms):
npm install -g allure-commandline
```

4. View Allure reports after test execution:
```bash
allure open reports/allure-report
```

All existing functionality remains unchanged. By default, Allure reporting is enabled, but you can switch to HTML reports by editing `config/reporting_config.json`.

---

## Known Issues

None reported.

---

## What's Next

Future releases will focus on:
- Multi-reporter support (generate both Allure and HTML simultaneously)
- Enhanced process cleanup mechanisms
- Signal handler improvements for graceful test interruption
- Better emulator process tracking
- Allure categories and custom labels
- Test retry and flaky test management

---

## Contributors

This release includes contributions focused on documentation quality and user experience improvements.

---

## Support

- Documentation: [docs/](../README.md)
- Troubleshooting: [docs/troubleshooting.md](../troubleshooting.md)
- Issues: [GitHub Issues](https://github.com/kavi-thirilo/testzen-lite/issues)

---

**Full Changelog:** https://github.com/kavi-thirilo/testzen-lite/compare/v1.2.3...v1.2.4
