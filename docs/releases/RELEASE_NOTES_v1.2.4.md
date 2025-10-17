# Release Notes - Version 1.2.4

**Release Date:** October 17, 2025

## Overview

This release focuses on documentation improvements and better troubleshooting support for emulator-related issues.

---

## What's New

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

**Moved Files:**
- All RELEASE_NOTES_*.md moved to docs/releases/
- CI_CD_SETUP.md moved to docs/cicd-setup-detailed.md
- MULTI_MODULE_GUIDE.md moved to docs/multi-module-guide.md

**Deleted Files:**
- README_old_backup.md (no longer needed)

**Updated Files:**
- README.md (updated links and added emulator troubleshooting reference)
- tests/android/README.md (updated guide links)
- tests/ios/README.md (updated guide links)
- docs/troubleshooting.md (added emulator crash section)

---

## Upgrade Instructions

No special upgrade steps required. Simply pull the latest changes:

```bash
git pull origin main
```

All existing functionality remains unchanged.

---

## Known Issues

None reported.

---

## What's Next

Future releases will focus on:
- Enhanced process cleanup mechanisms
- Signal handler improvements for graceful test interruption
- Better emulator process tracking

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
