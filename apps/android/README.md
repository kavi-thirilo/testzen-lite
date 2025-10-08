# Android App Binaries

Place your Android APK files here for testing.

## Instructions

1. **Copy your APK to this folder:**
   ```bash
   cp /path/to/your-app.apk apps/android/
   ```

2. **Only one APK at a time:**
   - TestZen Lite expects exactly one APK file in this folder
   - Remove old APKs before adding new ones

3. **APK Requirements:**
   - Valid Android APK file (.apk extension)
   - Built for the target Android API level (21+)
   - Debug or release build (both supported)

## Git Considerations

### Option 1: Don't Commit APKs (Recommended)
Add to your `.gitignore`:
```
apps/**/*.apk
```

### Option 2: Commit APKs with Git LFS
For large APKs, use Git LFS to avoid bloating the repository:
```bash
git lfs install
git lfs track "apps/**/*.apk"
git add .gitattributes
```

## Testing

Once your APK is in place:
```bash
# Run tests locally
python testzen.py run --file tests/android/your-test.xlsx --platform android

# CI/CD will automatically detect and use the APK
```

## Example APKs for Testing

You can download sample apps for testing:
- [Android API Demos](https://github.com/appium/android-apidemos/releases)
- Your own app's debug builds
