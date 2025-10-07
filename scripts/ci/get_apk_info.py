#!/usr/bin/env python3
"""
Extract package name and main activity from APK
Works without aapt by parsing AndroidManifest.xml
"""

import sys
import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path

def parse_android_manifest(apk_path):
    """Extract package name and launcher activity from APK"""
    try:
        with zipfile.ZipFile(apk_path, 'r') as apk:
            # Read AndroidManifest.xml (it's in binary XML format)
            manifest_data = apk.read('AndroidManifest.xml')

            # Try to parse it (might be binary AXML format)
            # For binary format, we need to use axmlparser or similar
            # For now, let's use a simpler approach with strings
            manifest_str = manifest_data.decode('utf-8', errors='ignore')

            # Extract package name
            package = None
            for line in manifest_str.split('\x00'):
                if '.' in line and len(line) > 5 and line.count('.') >= 2:
                    # Likely a package name format: com.example.app
                    if not any(x in line for x in ['android.', 'http', 'https', '/']):
                        parts = line.split('.')
                        if len(parts) >= 3 and all(p.replace('_', '').isalnum() for p in parts[:3]):
                            package = line
                            break

            # Extract launcher activity
            activity = None
            if 'MainActivity' in manifest_str:
                activity = 'MainActivity'
            elif 'LauncherActivity' in manifest_str:
                activity = 'LauncherActivity'
            elif 'SplashActivity' in manifest_str:
                activity = 'SplashActivity'

            return package, activity

    except Exception as e:
        print(f"Error parsing APK: {e}", file=sys.stderr)
        return None, None

def main():
    if len(sys.argv) != 2:
        print("Usage: get_apk_info.py <apk_path>", file=sys.stderr)
        sys.exit(1)

    apk_path = sys.argv[1]

    if not Path(apk_path).exists():
        print(f"Error: APK not found: {apk_path}", file=sys.stderr)
        sys.exit(1)

    package, activity = parse_android_manifest(apk_path)

    if package:
        print(f"PACKAGE={package}")
        if activity:
            print(f"ACTIVITY={package}.{activity}")
    else:
        print("ERROR: Could not extract package name", file=sys.stderr)
        sys.exit(1)

if __name__ == '__main__':
    main()
