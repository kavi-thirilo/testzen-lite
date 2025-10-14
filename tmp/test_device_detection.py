#!/usr/bin/env python3
"""
Quick diagnostic script to test device detection logic
Run this to see detailed logs of device detection
"""

import sys
from pathlib import Path

# Add src to path
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root / "src"))

from src.utils.emulator_manager import EmulatorManager
from src.utils.color_logger import ColorLogger

# Enable debug logging
import logging
logging.basicConfig(level=logging.DEBUG, format='%(message)s')

logger = ColorLogger()

print("=" * 70)
print("TESTZEN DEVICE DETECTION DIAGNOSTIC")
print("=" * 70)

emulator_mgr = EmulatorManager()

print("\n[STEP 1] Running: adb devices")
print("-" * 70)

devices = emulator_mgr.get_connected_devices()

print(f"\n[RESULT] Found {len(devices)} device(s)")
if devices:
    for device in devices:
        print(f"  - {device['id']} ({device['type']}) - {device['status']}")
else:
    print("  No devices detected!")

print("\n[STEP 2] Checking AVDs")
print("-" * 70)

avds = emulator_mgr.get_available_avds()
print(f"\n[RESULT] Found {len(avds)} AVD(s)")
if avds:
    for avd in avds:
        running = emulator_mgr.is_emulator_running(avd)
        status = "RUNNING" if running else "STOPPED"
        print(f"  - {avd} [{status}]")
else:
    print("  No AVDs found!")

print("\n[STEP 3] Testing ensure_device_available()")
print("-" * 70)

device_id = emulator_mgr.ensure_device_available()
if device_id:
    print(f"\n[SUCCESS] Device ready: {device_id}")
else:
    print(f"\n[FAILED] Could not ensure device availability")

print("\n" + "=" * 70)
print("DIAGNOSTIC COMPLETE")
print("=" * 70)
