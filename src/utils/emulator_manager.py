#!/usr/bin/env python3
"""
Emulator Manager for TestZen Framework
Automatically detects and launches Android emulators when no devices are available
"""

import subprocess
import time
import os
import logging
import platform
from typing import List, Optional, Dict


class EmulatorManager:
    """Manages Android emulator detection and launching"""

    def __init__(self, logger: logging.Logger = None):
        self.logger = logger or logging.getLogger(__name__)
        self.android_home = os.environ.get('ANDROID_HOME', os.path.expanduser('~/Library/Android/sdk'))
        self.emulator_cmd = self._get_emulator_command()
        self.adb_cmd = self._get_adb_command()

    def _get_emulator_command(self) -> str:
        """Get the emulator command path based on OS"""
        emulator_path = os.path.join(self.android_home, 'emulator', 'emulator')
        if platform.system() == 'Windows':
            emulator_path += '.exe'
        return emulator_path if os.path.exists(emulator_path) else 'emulator'

    def _get_adb_command(self) -> str:
        """Get the adb command path based on OS"""
        adb_path = os.path.join(self.android_home, 'platform-tools', 'adb')
        if platform.system() == 'Windows':
            adb_path += '.exe'
        return adb_path if os.path.exists(adb_path) else 'adb'

    def get_connected_devices(self) -> List[Dict[str, str]]:
        """Get list of connected devices and emulators"""
        try:
            result = subprocess.run([self.adb_cmd, 'devices', '-l'],
                                  capture_output=True, text=True, timeout=10)

            if result.returncode != 0:
                self.logger.error(f"Failed to get devices: {result.stderr}")
                return []

            devices = []
            lines = result.stdout.strip().split('\n')

            for line in lines[1:]:  # Skip header
                if 'device' in line and line.strip():  # Look for 'device' status in non-empty lines
                    parts = line.split()
                    if len(parts) >= 2 and parts[1] == 'device':  # Ensure second column is 'device'
                        device_id = parts[0]
                        device_type = 'emulator' if device_id.startswith('emulator-') else 'device'
                        devices.append({
                            'id': device_id,
                            'type': device_type,
                            'status': 'online'
                        })

            return devices

        except Exception as e:
            self.logger.error(f"Error getting connected devices: {e}")
            return []

    def get_available_avds(self) -> List[str]:
        """Get list of available Android Virtual Devices (AVDs)"""
        try:
            result = subprocess.run([self.emulator_cmd, '-list-avds'],
                                  capture_output=True, text=True, timeout=10)

            if result.returncode != 0:
                self.logger.error(f"Failed to list AVDs: {result.stderr}")
                return []

            avds = [avd.strip() for avd in result.stdout.strip().split('\n') if avd.strip()]
            return avds

        except Exception as e:
            self.logger.error(f"Error listing AVDs: {e}")
            return []

    def is_emulator_running(self, avd_name: str = None) -> bool:
        """Check if any emulator is running, or specific AVD if name provided"""
        devices = self.get_connected_devices()
        emulators = [d for d in devices if d['type'] == 'emulator']

        if not avd_name:
            return len(emulators) > 0

        # Check for specific AVD
        for emulator in emulators:
            try:
                # Get emulator AVD name
                result = subprocess.run([self.adb_cmd, '-s', emulator['id'], 'emu', 'avd', 'name'],
                                      capture_output=True, text=True, timeout=5)
                if result.returncode == 0 and avd_name in result.stdout:
                    return True
            except:
                continue

        return False

    def launch_emulator(self, avd_name: str = None, wait_for_boot: bool = True,
                       timeout: int = 120) -> bool:
        """
        Launch an Android emulator

        Args:
            avd_name: Name of AVD to launch. If None, launches first available
            wait_for_boot: Wait for emulator to fully boot
            timeout: Maximum time to wait for boot (seconds)

        Returns:
            True if emulator launched successfully
        """
        try:
            # Get available AVDs if no specific one requested
            if not avd_name:
                avds = self.get_available_avds()
                if not avds:
                    self.logger.error("[TZ] No AVDs found. Please create an AVD using Android Studio.")
                    return False
                avd_name = avds[0]
                self.logger.info(f"[TZ] Auto-selecting AVD: {avd_name}")

            # Check if already running
            if self.is_emulator_running(avd_name):
                self.logger.info(f"[TZ] Emulator '{avd_name}' is already running")
                return True

            self.logger.info(f"[TZ] Launching emulator: {avd_name}")

            # Launch emulator in background
            emulator_args = [
                self.emulator_cmd,
                '-avd', avd_name,
                '-no-snapshot-save',
                '-no-audio',
                '-no-boot-anim'
            ]

            # Start emulator process
            if platform.system() == 'Windows':
                subprocess.Popen(emulator_args,
                               creationflags=subprocess.CREATE_NEW_CONSOLE)
            else:
                subprocess.Popen(emulator_args,
                               stdout=subprocess.DEVNULL,
                               stderr=subprocess.DEVNULL)

            if wait_for_boot:
                return self._wait_for_emulator_boot(timeout)

            return True

        except Exception as e:
            self.logger.error(f"[TZ] Failed to launch emulator: {e}")
            return False

    def _wait_for_emulator_boot(self, timeout: int = 120) -> bool:
        """Wait for emulator to fully boot"""
        self.logger.info("[TZ] Waiting for emulator to boot...")
        start_time = time.time()

        # First, wait for emulator to appear in devices list
        while time.time() - start_time < timeout:
            devices = self.get_connected_devices()
            emulators = [d for d in devices if d['type'] == 'emulator']

            if emulators:
                emulator_id = emulators[0]['id']
                self.logger.info(f"[TZ] Emulator detected: {emulator_id}")

                # Wait for boot completion
                if self._wait_for_boot_complete(emulator_id,
                                               timeout - int(time.time() - start_time)):
                    self.logger.info("[TZ] Emulator fully booted and ready")
                    return True

            time.sleep(2)

        self.logger.error("[TZ] Emulator boot timeout")
        return False

    def _wait_for_boot_complete(self, device_id: str, timeout: int = 60) -> bool:
        """Wait for device boot to complete"""
        start_time = time.time()

        while time.time() - start_time < timeout:
            try:
                # Check if boot completed
                result = subprocess.run([self.adb_cmd, '-s', device_id, 'shell',
                                       'getprop', 'sys.boot_completed'],
                                      capture_output=True, text=True, timeout=5)

                if result.returncode == 0 and '1' in result.stdout.strip():
                    # Additional check for boot animation
                    anim_result = subprocess.run([self.adb_cmd, '-s', device_id, 'shell',
                                                'getprop', 'init.svc.bootanim'],
                                               capture_output=True, text=True, timeout=5)

                    if anim_result.returncode == 0 and 'stopped' in anim_result.stdout:
                        return True
            except:
                pass

            time.sleep(2)

        return False

    def stop_emulator(self, device_id: str = None) -> bool:
        """Stop a running emulator"""
        try:
            if not device_id:
                # Stop all emulators
                devices = self.get_connected_devices()
                emulators = [d for d in devices if d['type'] == 'emulator']

                if not emulators:
                    self.logger.info("[TZ] No emulators to stop")
                    return True

                device_id = emulators[0]['id']

            self.logger.info(f"[TZ] Stopping emulator: {device_id}")

            # Send power button event to shutdown gracefully
            subprocess.run([self.adb_cmd, '-s', device_id, 'emu', 'kill'],
                          capture_output=True, text=True, timeout=10)

            self.logger.info("[TZ] Emulator stopped")
            return True

        except Exception as e:
            self.logger.error(f"[TZ] Failed to stop emulator: {e}")
            return False

    def ensure_device_available(self, preferred_avd: str = None) -> Optional[str]:
        """
        Ensure a device is available for testing

        Args:
            preferred_avd: Preferred AVD name to launch if no devices available

        Returns:
            Device ID if available, None otherwise
        """
        # Check for existing devices
        devices = self.get_connected_devices()

        if devices:
            device_id = devices[0]['id']
            device_type = devices[0]['type']
            self.logger.info(f"[TZ] Using existing {device_type}: {device_id}")
            return device_id

        # No devices available, try to launch emulator
        self.logger.info("[TZ] No devices found. Attempting to launch emulator...")

        if self.launch_emulator(preferred_avd):
            # Get the launched emulator ID
            devices = self.get_connected_devices()
            if devices:
                device_id = devices[0]['id']
                self.logger.info(f"[TZ] Successfully launched emulator: {device_id}")
                return device_id

        self.logger.error("[TZ] Failed to ensure device availability")
        return None

    def list_emulator_status(self) -> None:
        """Print status of all emulators and devices"""
        print("\n[TZ] Device & Emulator Status")

        # List connected devices
        devices = self.get_connected_devices()
        if devices:
            print("\nConnected Devices:")
            for device in devices:
                print(f"  - {device['id']} ({device['type']})")
        else:
            print("\nNo connected devices or emulators")

        # List available AVDs
        avds = self.get_available_avds()
        if avds:
            print("\nAvailable AVDs:")
            for avd in avds:
                status = "Running" if self.is_emulator_running(avd) else "Stopped"
                print(f"  - {avd} [{status}]")
        else:
            print("\nNo AVDs found")

        print("=" * 50)