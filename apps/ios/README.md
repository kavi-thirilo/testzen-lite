# iOS App Binaries

Place your iOS IPA files here for testing.

## Instructions

1. **Copy your IPA to this folder:**
   ```bash
   cp /path/to/your-app.ipa apps/ios/
   ```

2. **Only one IPA at a time:**
   - TestZen Lite expects exactly one IPA file in this folder
   - Remove old IPAs before adding new ones

3. **IPA Requirements:**
   - Valid iOS IPA file (.ipa extension)
   - Built for iOS Simulator (not device builds)
   - Minimum iOS 11.0+

## Building IPA for Simulator

To create a simulator-compatible IPA from Xcode:
```bash
# Build for simulator
xcodebuild -workspace YourApp.xcworkspace \
  -scheme YourApp \
  -sdk iphonesimulator \
  -configuration Debug \
  -derivedDataPath ./build

# Create IPA
cd build/Build/Products/Debug-iphonesimulator/
zip -r YourApp.ipa Payload/
mv YourApp.ipa ../../../../apps/ios/
```

## Git Considerations

### Option 1: Don't Commit IPAs (Recommended)
Add to your `.gitignore`:
```
apps/**/*.ipa
```

### Option 2: Commit IPAs with Git LFS
For large IPAs, use Git LFS:
```bash
git lfs install
git lfs track "apps/**/*.ipa"
git add .gitattributes
```

## Testing

Once your IPA is in place:
```bash
# Run tests locally
python testzen.py run --file tests/ios/your-test.xlsx --platform ios

# CI/CD will automatically detect and use the IPA
```

## Sample Apps for Testing

You can use sample apps for testing:
- Build your own app for simulator
- Test apps from your development team
