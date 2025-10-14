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
            self.logger.debug(f"[TZ] Running: {self.adb_cmd} devices -l")
            result = subprocess.run([self.adb_cmd, 'devices', '-l'],
                                  capture_output=True, text=True, timeout=10)

            if result.returncode != 0:
                self.logger.error(f"[TZ] Failed to get devices: {result.stderr}")
                return []

            self.logger.debug(f"[TZ] ADB devices output:\n{result.stdout}")

            devices = []
            lines = result.stdout.strip().split('\n')

            # Log each line for debugging
            self.logger.debug(f"[TZ] Parsing {len(lines)} lines from adb devices")
            for i, line in enumerate(lines):
                self.logger.debug(f"[TZ] Line {i}: '{line}'")

            for line in lines[1:]:  # Skip header "List of devices attached"
                line = line.strip()
                if not line:  # Skip empty lines
                    continue

                parts = line.split()
                if len(parts) < 2:
                    self.logger.debug(f"[TZ] Skipping line with < 2 parts: '{line}'")
                    continue

                device_id = parts[0]
                device_status = parts[1]

                self.logger.debug(f"[TZ] Found device: id={device_id}, status={device_status}")

                # Only include devices with 'device' status (not 'offline', 'unauthorized', etc.)
                if device_status == 'device':
                    device_type = 'emulator' if device_id.startswith('emulator-') else 'device'
                    devices.append({
                        'id': device_id,
                        'type': device_type,
                        'status': 'online'
                    })
                    self.logger.info(f"[TZ] Detected available {device_type}: {device_id}")
                else:
                    self.logger.warning(f"[TZ] Device {device_id} has status '{device_status}' - not available")

            if not devices:
                self.logger.warning("[TZ] No devices found with 'device' status")
            else:
                self.logger.info(f"[TZ] Found {len(devices)} available device(s)")

            return devices

        except subprocess.TimeoutExpired:
            self.logger.error("[TZ] Timeout while getting device list (adb may be hung)")
            return []
        except Exception as e:
            self.logger.error(f"[TZ] Error getting connected devices: {e}")
            import traceback
            self.logger.debug(f"[TZ] Traceback: {traceback.format_exc()}")
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
                       timeout: int = 180) -> bool:
        """
        Launch an Android emulator

        Args:
            avd_name: Name of AVD to launch. If None, launches first available
            wait_for_boot: Wait for emulator to fully boot
            timeout: Maximum time to wait for boot (seconds, default: 180s)

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

    def _wait_for_emulator_boot(self, timeout: int = 180) -> bool:
        """
        Wait for emulator to fully boot

        Args:
            timeout: Maximum time to wait in seconds (default: 180s)

        Returns:
            True if emulator boots successfully, False if timeout occurs
        """
        self.logger.info(f"[TZ] Waiting for emulator to boot (timeout: {timeout}s)...")
        start_time = time.time()

        # First, wait for emulator to appear in devices list
        while time.time() - start_time < timeout:
            elapsed = int(time.time() - start_time)
            remaining = timeout - elapsed

            devices = self.get_connected_devices()
            emulators = [d for d in devices if d['type'] == 'emulator']

            if emulators:
                emulator_id = emulators[0]['id']
                self.logger.info(f"[TZ] Emulator detected: {emulator_id} (after {elapsed}s)")

                # Wait for boot completion with remaining time
                if self._wait_for_boot_complete(emulator_id, remaining):
                    total_time = int(time.time() - start_time)
                    self.logger.info(f"[TZ] Emulator fully booted and ready (total time: {total_time}s)")
                    return True
                else:
                    # Boot completion timed out
                    self.logger.error(f"[TZ] Emulator boot completion timed out after {timeout}s")
                    return False

            # Still waiting for emulator to appear
            if elapsed % 10 == 0 and elapsed > 0:  # Log every 10 seconds
                self.logger.info(f"[TZ] Still waiting for emulator... ({elapsed}/{timeout}s)")

            time.sleep(2)

        # Timeout reached without emulator appearing
        self.logger.error(f"[TZ] Emulator failed to appear after {timeout}s timeout")
        return False

    def _wait_for_boot_complete(self, device_id: str, timeout: int = 90) -> bool:
        """
        Wait for device boot to complete and system to be fully ready for automation

        This method performs comprehensive readiness checks:
        1. Basic boot completion (sys.boot_completed)
        2. Boot animation stopped (init.svc.bootanim)
        3. Package manager ready and responsive
        4. Device properties accessible

        Args:
            device_id: Device ID to monitor
            timeout: Maximum time to wait in seconds (default: 90s)

        Returns:
            True if device is fully ready, False if timeout occurs
        """
        start_time = time.time()
        boot_checks_passed = False
        system_ready = False

        self.logger.info(f"[TZ] Waiting for boot completion (timeout: {timeout}s)...")

        while time.time() - start_time < timeout:
            elapsed = int(time.time() - start_time)
            remaining = timeout - elapsed

            try:
                # Step 1: Check if boot completed
                if not boot_checks_passed:
                    result = subprocess.run([self.adb_cmd, '-s', device_id, 'shell',
                                           'getprop', 'sys.boot_completed'],
                                          capture_output=True, text=True, timeout=5)

                    if result.returncode == 0 and '1' in result.stdout.strip():
                        # Additional check for boot animation
                        anim_result = subprocess.run([self.adb_cmd, '-s', device_id, 'shell',
                                                    'getprop', 'init.svc.bootanim'],
                                                   capture_output=True, text=True, timeout=5)

                        if anim_result.returncode == 0 and 'stopped' in anim_result.stdout:
                            boot_checks_passed = True
                            self.logger.info(f"[TZ] Boot animation completed (after {elapsed}s)")

                # Step 2: Check if package manager is ready
                if boot_checks_passed and not system_ready:
                    self.logger.info("[TZ] Verifying system services...")

                    # Check package manager responsiveness
                    pm_result = subprocess.run([self.adb_cmd, '-s', device_id, 'shell',
                                               'pm', 'list', 'packages', '-e'],
                                              capture_output=True, text=True, timeout=10)

                    if pm_result.returncode == 0 and 'package:' in pm_result.stdout:
                        # Package manager is ready - sufficient for automation
                        # Additional check: verify device is responsive to getprop
                        try:
                            prop_result = subprocess.run([self.adb_cmd, '-s', device_id, 'shell',
                                                         'getprop', 'sys.boot_completed'],
                                                        capture_output=True, text=True, timeout=5)

                            if prop_result.returncode == 0 and '1' in prop_result.stdout:
                                system_ready = True
                                self.logger.info("[TZ] System services are ready")

                                # Give the system a moment to stabilize after all services are up
                                self.logger.info("[TZ] Waiting for system stabilization (3s)...")
                                time.sleep(3)

                                total_time = int(time.time() - start_time)
                                self.logger.info(f"[TZ] Device is fully ready for automation (boot time: {total_time}s)")
                                return True
                        except subprocess.TimeoutExpired:
                            self.logger.debug("[TZ] Device still initializing properties...")

            except subprocess.TimeoutExpired:
                self.logger.debug(f"[TZ] Device not yet responsive ({elapsed}/{timeout}s)")
            except Exception as e:
                self.logger.debug(f"[TZ] Boot check error (expected during boot): {e}")

            # Progress logging every 10 seconds
            if elapsed % 10 == 0 and elapsed > 0:
                status = "Waiting for boot..." if not boot_checks_passed else "Waiting for system services..."
                self.logger.info(f"[TZ] {status} ({elapsed}/{timeout}s)")

            time.sleep(3)

        # Timeout reached
        self.logger.error(f"[TZ] Device boot timeout after {timeout}s - device not ready")
        if boot_checks_passed:
            self.logger.error("[TZ] Boot completed but system services failed to initialize")
        else:
            self.logger.error("[TZ] Device failed to complete boot process")
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