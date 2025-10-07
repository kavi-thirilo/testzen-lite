#!/usr/bin/env python3
"""
Package Detector Utility
Automatically detects package name from APK file or installed app
"""

import subprocess
import os
import re
import logging
from typing import Optional, List, Dict


class PackageDetector:
    """Utility class to detect and manage app package names"""

    def __init__(self, logger: logging.Logger = None):
        self.logger = logger or logging.getLogger(__name__)

    def get_package_from_apk(self, apk_path: str) -> Optional[str]:
        """
        Extract package name from APK file using aapt

        Args:
            apk_path: Path to the APK file

        Returns:
            Package name if found, None otherwise
        """
        try:
            if not os.path.exists(apk_path):
                self.logger.error(f"APK file not found: {apk_path}")
                return None

            # Try to find aapt tool
            android_home = os.environ.get('ANDROID_HOME', os.path.expanduser('~/Library/Android/sdk'))
            build_tools_dir = os.path.join(android_home, 'build-tools')
            
            aapt_path = None
            if os.path.exists(build_tools_dir):
                # Find the latest build tools version
                versions = [d for d in os.listdir(build_tools_dir) if os.path.isdir(os.path.join(build_tools_dir, d))]
                if versions:
                    versions.sort(reverse=True)  # Get latest version
                    aapt_path = os.path.join(build_tools_dir, versions[0], 'aapt')

            if not aapt_path or not os.path.exists(aapt_path):
                self.logger.warning("aapt tool not found, trying alternative methods")
                return self._get_package_alternative(apk_path)

            # Use aapt to extract package name
            cmd = [aapt_path, 'dump', 'badging', apk_path]
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)

            if result.returncode == 0:
                # Parse output for package name
                for line in result.stdout.split('\n'):
                    if line.startswith('package:'):
                        match = re.search(r"name='([^']*)'", line)
                        if match:
                            package_name = match.group(1)
                            self.logger.info(f"[TZ] Detected package from APK: {package_name}")
                            return package_name

            return None

        except Exception as e:
            self.logger.error(f"Failed to extract package from APK: {str(e)}")
            return None

    def _get_package_alternative(self, apk_path: str) -> Optional[str]:
        """Alternative method to extract package name (fallback)"""
        try:
            # Try using unzip to extract AndroidManifest.xml
            # This is a simplified approach - in practice you'd need to parse binary XML
            self.logger.info("[TZ] Trying alternative package detection method")
            
            # Cannot determine package without proper tools
            return None

        except Exception as e:
            self.logger.error(f"Alternative package detection failed: {str(e)}")
            return None

    def get_installed_packages(self) -> List[str]:
        """
        Get list of all installed packages on the device

        Returns:
            List of package names
        """
        try:
            cmd = ['adb', 'shell', 'pm', 'list', 'packages']
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)

            if result.returncode == 0:
                packages = []
                for line in result.stdout.strip().split('\n'):
                    if line.startswith('package:'):
                        package_name = line.replace('package:', '').strip()
                        packages.append(package_name)
                return packages

            return []

        except Exception as e:
            self.logger.error(f"Failed to get installed packages: {str(e)}")
            return []

    def find_package_by_keyword(self, keyword: str) -> List[str]:
        """
        Find installed packages containing a keyword

        Args:
            keyword: Keyword to search for

        Returns:
            List of matching package names
        """
        try:
            installed_packages = self.get_installed_packages()
            matching_packages = [pkg for pkg in installed_packages if keyword.lower() in pkg.lower()]
            
            self.logger.info(f"[TZ] Found {len(matching_packages)} packages matching '{keyword}'")
            return matching_packages

        except Exception as e:
            self.logger.error(f"Failed to find packages by keyword: {str(e)}")
            return []

    def auto_detect_package(self, apk_path: str = None, keyword: str = None) -> Optional[str]:
        """
        Auto-detect package name using multiple strategies

        Args:
            apk_path: Optional path to APK file
            keyword: Optional keyword to search for

        Returns:
            Detected package name if found
        """
        try:
            # Strategy 1: Extract from APK if provided
            if apk_path and os.path.exists(apk_path):
                package = self.get_package_from_apk(apk_path)
                if package:
                    return package

            # Strategy 2: Search installed packages by keyword
            if keyword:
                matching_packages = self.find_package_by_keyword(keyword)
                if len(matching_packages) == 1:
                    self.logger.info(f"[TZ] Auto-detected unique package: {matching_packages[0]}")
                    return matching_packages[0]
                elif len(matching_packages) > 1:
                    self.logger.warning(f"[TZ] Multiple packages found for '{keyword}': {matching_packages}")
                    # Return the first one as a best guess
                    return matching_packages[0]

            self.logger.warning("[TZ] Could not auto-detect package name")
            return None

        except Exception as e:
            self.logger.error(f"Auto-detection failed: {str(e)}")
            return None

    def is_package_installed(self, package_name: str) -> bool:
        """
        Check if a specific package is installed

        Args:
            package_name: Package name to check

        Returns:
            True if package is installed
        """
        try:
            installed_packages = self.get_installed_packages()
            return package_name in installed_packages

        except Exception as e:
            self.logger.error(f"Failed to check if package is installed: {str(e)}")
            return False

    def detect_and_install(self, apk_path: str, keyword: str = None) -> Optional[str]:
        """
        Detect package name and install the APK if not already installed

        Args:
            apk_path: Path to the APK file
            keyword: Optional keyword to search for existing packages

        Returns:
            Detected package name if successful, None otherwise
        """
        try:
            self.logger.info(f"[TZ] Detecting and installing package from: {apk_path}")

            # First, try to detect the package name
            detected_package = self.auto_detect_package(apk_path=apk_path, keyword=keyword)
            
            if not detected_package:
                self.logger.error("[TZ] Could not detect package name from APK")
                return None

            self.logger.info(f"[TZ] Detected package: {detected_package}")

            # Check if package is already installed
            if self.is_package_installed(detected_package):
                self.logger.info(f"[TZ] Package {detected_package} is already installed")
                return detected_package

            # Install the APK using ADB
            if os.path.exists(apk_path):
                self.logger.info(f"[TZ] Installing APK: {apk_path}")
                result = self._install_apk_with_adb(apk_path)
                
                if result:
                    self.logger.info(f"[TZ] Successfully installed {detected_package}")
                    return detected_package
                else:
                    self.logger.error(f"[TZ] Failed to install {detected_package}")
                    return None
            else:
                self.logger.error(f"[TZ] APK file not found: {apk_path}")
                return None

        except Exception as e:
            self.logger.error(f"[TZ] detect_and_install failed: {str(e)}")
            return None

    def _install_apk_with_adb(self, apk_path: str) -> bool:
        """
        Install APK using ADB command

        Args:
            apk_path: Path to the APK file

        Returns:
            True if installation successful
        """
        try:
            # Use ADB to install the APK with test flag
            cmd = ['adb', 'install', '-r', '-t', apk_path]
            self.logger.info(f"[TZ] Running ADB command: {' '.join(cmd)}")

            result = subprocess.run(cmd, capture_output=True, text=True, timeout=60)

            if result.returncode == 0:
                self.logger.info("[TZ] APK installation successful")
                return True
            else:
                self.logger.error(f"[TZ] APK installation failed: {result.stderr}")
                return False

        except Exception as e:
            self.logger.error(f"[TZ] APK installation error: {str(e)}")
            return False

    def update_locator_file(self, locator_file_path, package_name):
        """
        Update the package name in a locator configuration file
        
        Args:
            locator_file_path: Path to the locator Python file
            package_name: New package name to set
            
        Returns:
            True if successful, False otherwise
        """
        try:
            if not os.path.exists(locator_file_path):
                self.logger.error(f"[TZ] Locator file not found: {locator_file_path}")
                return False
            
            with open(locator_file_path, 'r') as f:
                content = f.read()
            
            # Update APP_PACKAGE line
            updated_content = re.sub(
                r'APP_PACKAGE\s*=\s*["\'].*?["\']',
                f'APP_PACKAGE = "{package_name}"',
                content
            )
            
            # Write back if changed
            if updated_content != content:
                with open(locator_file_path, 'w') as f:
                    f.write(updated_content)
                
                self.logger.info(f"[TZ] Updated package name in {locator_file_path}")
                return True
            else:
                self.logger.info("[TZ] Package name already up to date")
                return True
                
        except Exception as e:
            self.logger.error(f"[TZ] Error updating locator file: {e}")
            return False