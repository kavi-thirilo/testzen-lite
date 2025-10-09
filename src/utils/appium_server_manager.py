#!/usr/bin/env python3
"""
Appium Server Manager - Automatically starts and manages Appium server
"""

import subprocess
import time
import socket
import os
import signal
from .color_logger import ColorLogger


class AppiumServerManager:
    """Manages Appium server lifecycle - start, stop, and health checks"""

    def __init__(self, port=4723):
        self.port = port
        self.server_url = f"http://localhost:{port}"
        self.process = None
        self.color_logger = ColorLogger()
        self.log_file = None

    def is_port_in_use(self):
        """Check if Appium port is already in use"""
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                s.settimeout(1)
                result = s.connect_ex(('localhost', self.port))
                return result == 0
        except Exception:
            return False

    def is_appium_running(self):
        """Check if Appium server is running and responsive"""
        if not self.is_port_in_use():
            return False

        # Try to connect to Appium status endpoint
        try:
            import urllib.request
            import urllib.error

            status_url = f"{self.server_url}/status"
            req = urllib.request.Request(status_url, method='GET')

            with urllib.request.urlopen(req, timeout=3) as response:
                if response.status == 200:
                    self.color_logger.success(f"Appium server already running on port {self.port}")
                    return True
        except (urllib.error.URLError, urllib.error.HTTPError, Exception):
            # Port is in use but not responding to Appium API
            self.color_logger.warning(f"Port {self.port} is in use but not responding as Appium server")
            return False

        return False

    def start_server(self, wait_timeout=30):
        """Start Appium server in background

        Args:
            wait_timeout: Maximum seconds to wait for server to be ready

        Returns:
            True if server started successfully, False otherwise
        """
        try:
            # Check if already running
            if self.is_appium_running():
                self.color_logger.info("Using existing Appium server")
                return True

            # Check if Appium is installed
            try:
                result = subprocess.run(['appium', '--version'],
                                      capture_output=True, text=True, timeout=5)
                if result.returncode != 0:
                    self.color_logger.error("Appium is not installed. Install with: npm install -g appium")
                    return False

                appium_version = result.stdout.strip()
                self.color_logger.info(f"Found Appium version: {appium_version}")
            except FileNotFoundError:
                self.color_logger.error("Appium command not found. Install with: npm install -g appium")
                self.color_logger.error("Then install driver: appium driver install uiautomator2")
                return False
            except Exception as e:
                self.color_logger.error(f"Failed to check Appium installation: {e}")
                return False

            # Create log file for Appium output
            import tempfile
            log_dir = tempfile.gettempdir()
            log_file_path = os.path.join(log_dir, f"appium_testzen_{int(time.time())}.log")
            self.log_file = open(log_file_path, 'w')

            self.color_logger.step("Starting Appium server...")
            self.color_logger.info(f"Appium logs: {log_file_path}")

            # Start Appium server
            self.process = subprocess.Popen(
                ['appium', '--port', str(self.port)],
                stdout=self.log_file,
                stderr=subprocess.STDOUT,
                preexec_fn=os.setsid if os.name != 'nt' else None  # Unix: create process group
            )

            # Wait for server to be ready
            self.color_logger.step(f"Waiting for Appium server to be ready (timeout: {wait_timeout}s)...")
            start_time = time.time()

            while time.time() - start_time < wait_timeout:
                if self.is_appium_running():
                    elapsed = int(time.time() - start_time)
                    self.color_logger.success(f"Appium server started successfully on port {self.port} ({elapsed}s)")
                    return True

                # Check if process died
                if self.process.poll() is not None:
                    self.color_logger.error("Appium server process terminated unexpectedly")
                    self.color_logger.error(f"Check logs: {log_file_path}")
                    return False

                time.sleep(1)

            # Timeout
            self.color_logger.error(f"Appium server failed to start within {wait_timeout}s")
            self.color_logger.error(f"Check logs: {log_file_path}")
            self.stop_server()
            return False

        except Exception as e:
            self.color_logger.error(f"Failed to start Appium server: {e}")
            return False

    def stop_server(self, force=False):
        """Stop Appium server if it was started by this manager

        Args:
            force: Force kill the server even if it wasn't started by us
        """
        try:
            if self.process:
                self.color_logger.step("Stopping Appium server...")

                try:
                    if os.name != 'nt':
                        # Unix: kill process group
                        os.killpg(os.getpgid(self.process.pid), signal.SIGTERM)
                    else:
                        # Windows: kill process
                        self.process.terminate()

                    # Wait for process to terminate
                    try:
                        self.process.wait(timeout=10)
                        self.color_logger.success("Appium server stopped")
                    except subprocess.TimeoutExpired:
                        # Force kill if graceful shutdown failed
                        if os.name != 'nt':
                            os.killpg(os.getpgid(self.process.pid), signal.SIGKILL)
                        else:
                            self.process.kill()
                        self.color_logger.warning("Appium server force killed")

                except ProcessLookupError:
                    self.color_logger.info("Appium server already stopped")
                except Exception as e:
                    self.color_logger.warning(f"Error stopping Appium server: {e}")

                self.process = None

            # Close log file
            if self.log_file:
                try:
                    self.log_file.close()
                except:
                    pass
                self.log_file = None

        except Exception as e:
            self.color_logger.error(f"Failed to stop Appium server: {e}")

    def get_status(self):
        """Get Appium server status information"""
        if self.is_appium_running():
            return {
                'running': True,
                'url': self.server_url,
                'port': self.port,
                'managed': self.process is not None
            }
        return {
            'running': False,
            'url': self.server_url,
            'port': self.port,
            'managed': False
        }

    def __del__(self):
        """Cleanup on deletion"""
        # Don't auto-stop in destructor - let teardown handle it explicitly
        if self.log_file:
            try:
                self.log_file.close()
            except:
                pass
