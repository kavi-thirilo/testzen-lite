#!/usr/bin/env python3
"""
Color Logger for TestZen Framework
Provides Linux-style color-coded logging for better terminal visibility
"""

import sys
import os
from datetime import datetime
from enum import Enum
from typing import Optional


class Colors:
    """ANSI color codes for terminal output"""
    # Reset
    # Reset
    RESET = '\033[0m'

    # Regular Colors
    BLACK = '\033[0;30m'
    RED = '\033[0;31m'
    GREEN = '\033[0;32m'
    YELLOW = '\033[0;33m'
    BLUE = '\033[0;34m'
    PURPLE = '\033[0;35m'
    CYAN = '\033[0;36m'
    WHITE = '\033[0;37m'

    # Bold Colors
    BOLD_BLACK = '\033[1;30m'
    BOLD_RED = '\033[1;31m'
    BOLD_GREEN = '\033[1;32m'
    BOLD_YELLOW = '\033[1;33m'
    BOLD_BLUE = '\033[1;34m'
    BOLD_PURPLE = '\033[1;35m'
    BOLD_CYAN = '\033[1;36m'
    BOLD_WHITE = '\033[1;37m'

    # Background Colors
    BG_BLACK = '\033[40m'
    BG_RED = '\033[41m'
    BG_GREEN = '\033[42m'
    BG_YELLOW = '\033[43m'
    BG_BLUE = '\033[44m'
    BG_PURPLE = '\033[45m'
    BG_CYAN = '\033[46m'
    BG_WHITE = '\033[47m'

    # High Intensity Colors
    HI_BLACK = '\033[0;90m'
    HI_RED = '\033[0;91m'
    HI_GREEN = '\033[0;92m'
    HI_YELLOW = '\033[0;93m'
    HI_BLUE = '\033[0;94m'
    HI_PURPLE = '\033[0;95m'
    HI_CYAN = '\033[0;96m'
    HI_WHITE = '\033[0;97m'

    # Bold High Intensity
    BOLD_HI_BLACK = '\033[1;90m'
    BOLD_HI_RED = '\033[1;91m'
    BOLD_HI_GREEN = '\033[1;92m'
    BOLD_HI_YELLOW = '\033[1;93m'
    BOLD_HI_BLUE = '\033[1;94m'
    BOLD_HI_PURPLE = '\033[1;95m'
    BOLD_HI_CYAN = '\033[1;96m'
    BOLD_HI_WHITE = '\033[1;97m'


class LogLevel(Enum):
    """Log levels with associated colors and symbols"""
    DEBUG = ("DEBUG", Colors.HI_BLACK, "")
    INFO = ("INFO", Colors.CYAN, "")
    SUCCESS = ("SUCCESS", Colors.GREEN, "")
    WARNING = ("WARNING", Colors.YELLOW, "")
    ERROR = ("ERROR", Colors.RED, "")
    CRITICAL = ("CRITICAL", Colors.BOLD_RED, "")
    STEP = ("STEP", Colors.BLUE, "")
    PASS = ("PASS", Colors.BOLD_GREEN, "")
    FAIL = ("FAIL", Colors.BOLD_RED, "")
    SKIP = ("SKIP", Colors.HI_YELLOW, "")


class ColorLogger:
    """Color-coded logger for TestZen Framework"""

    def __init__(self, name: str = "TestZen", show_timestamp: bool = True,
                 use_colors: bool = True, use_symbols: bool = False):
        """
        Initialize the color logger

        Args:
            name: Logger name prefix (default: "TestZen")
            show_timestamp: Whether to show timestamps
            use_colors: Whether to use colors (auto-disabled for non-terminal)
            use_symbols: Whether to use emoji symbols
        """
        self.name = name
        self.show_timestamp = show_timestamp
        self.use_symbols = use_symbols

        # Auto-detect if output supports colors
        self.use_colors = use_colors and self._supports_colors()

    def _supports_colors(self) -> bool:
        """Check if terminal supports colors"""
        # Check if output is to a terminal
        if not hasattr(sys.stdout, 'isatty') or not sys.stdout.isatty():
            return False

        # Check for common environment variables
        if os.environ.get('NO_COLOR'):
            return False

        if os.environ.get('TERM') == 'dumb':
            return False

        # Windows color support
        if sys.platform == 'win32':
            try:
                import ctypes
                kernel32 = ctypes.windll.kernel32
                kernel32.SetConsoleMode(kernel32.GetStdHandle(-11), 7)
            except:
                pass

        return True

    def _get_timestamp(self) -> str:
        """Get formatted timestamp"""
        return datetime.now().strftime('%H:%M:%S')

    def _format_message(self, level: LogLevel, message: str, prefix: str = None) -> str:
        """Format message with colors and metadata"""
        parts = []

        # Add name prefix
        if self.use_colors:
            parts.append(f"{Colors.CYAN}[{self.name}]{Colors.RESET}")
        else:
            parts.append(f"[{self.name}]")

        # Add timestamp
        if self.show_timestamp:
            timestamp = self._get_timestamp()
            if self.use_colors:
                parts.append(f"{Colors.BLUE}[{timestamp}]{Colors.RESET}")
            else:
                parts.append(f"[{timestamp}]")

        # Add custom prefix if provided
        if prefix:
            if self.use_colors:
                parts.append(f"{Colors.PURPLE}[{prefix}]{Colors.RESET}")
            else:
                parts.append(f"[{prefix}]")

        # Add level indicator
        level_name, color, symbol = level.value
        if self.use_colors:
            if self.use_symbols and symbol:
                parts.append(f"{color}{symbol} {level_name}{Colors.RESET}")
            else:
                parts.append(f"{color}[{level_name}]{Colors.RESET}")
        else:
            parts.append(f"[{level_name}]")

        # Add message
        if self.use_colors and level in [LogLevel.ERROR, LogLevel.CRITICAL, LogLevel.FAIL]:
            parts.append(f"{color}{message}{Colors.RESET}")
        elif self.use_colors and level in [LogLevel.SUCCESS, LogLevel.PASS]:
            parts.append(f"{Colors.GREEN}{message}{Colors.RESET}")
        elif self.use_colors and level in [LogLevel.WARNING, LogLevel.SKIP]:
            parts.append(f"{Colors.YELLOW}{message}{Colors.RESET}")
        else:
            parts.append(message)

        return " ".join(parts)

    def debug(self, message: str, prefix: str = None):
        """Log debug message"""
        print(self._format_message(LogLevel.DEBUG, message, prefix))

    def info(self, message: str, prefix: str = None):
        """Log info message"""
        print(self._format_message(LogLevel.INFO, message, prefix))

    def success(self, message: str, prefix: str = None):
        """Log success message"""
        print(self._format_message(LogLevel.SUCCESS, message, prefix))

    def warning(self, message: str, prefix: str = None):
        """Log warning message"""
        print(self._format_message(LogLevel.WARNING, message, prefix), file=sys.stderr)

    def error(self, message: str, prefix: str = None):
        """Log error message"""
        print(self._format_message(LogLevel.ERROR, message, prefix), file=sys.stderr)

    def critical(self, message: str, prefix: str = None):
        """Log critical message"""
        print(self._format_message(LogLevel.CRITICAL, message, prefix), file=sys.stderr)

    def step(self, message: str, prefix: str = None):
        """Log test step"""
        print(self._format_message(LogLevel.STEP, message, prefix))

    def pass_step(self, message: str, prefix: str = None):
        """Log passed test step"""
        print(self._format_message(LogLevel.PASS, message, prefix))

    def fail_step(self, message: str, prefix: str = None):
        """Log failed test step"""
        print(self._format_message(LogLevel.FAIL, message, prefix))

    def skip_step(self, message: str, prefix: str = None):
        """Log skipped test step"""
        print(self._format_message(LogLevel.SKIP, message, prefix))

    def header(self, title: str, width: int = 60, char: str = "="):
        """Print a formatted header"""
        if self.use_colors:
            line = f"{Colors.CYAN}{char * width}{Colors.RESET}"
            title_line = f"{Colors.BOLD_CYAN}{title.center(width)}{Colors.RESET}"
        else:
            line = char * width
            title_line = title.center(width)

        print(line)
        print(title_line)
        print(line)

    def separator(self, width: int = 60, char: str = "-"):
        """Print a separator line"""
        if self.use_colors:
            print(f"{Colors.HI_BLACK}{char * width}{Colors.RESET}")
        else:
            print(char * width)

    def progress(self, current: int, total: int, message: str = "", prefix: str = None):
        """Log progress indicator"""
        percentage = (current / total * 100) if total > 0 else 0
        bar_length = 30
        filled = int(bar_length * current / total) if total > 0 else 0

        if self.use_colors:
            bar = f"{Colors.GREEN}{'' * filled}{Colors.HI_BLACK}{'' * (bar_length - filled)}{Colors.RESET}"
            progress_text = f"{Colors.CYAN}[{current}/{total}]{Colors.RESET} {bar} {Colors.YELLOW}{percentage:.1f}%{Colors.RESET}"
        else:
            bar = f"{'#' * filled}{'-' * (bar_length - filled)}"
            progress_text = f"[{current}/{total}] [{bar}] {percentage:.1f}%"

        if message:
            progress_text += f" - {message}"

        if prefix:
            if self.use_colors:
                progress_text = f"{Colors.PURPLE}[{prefix}]{Colors.RESET} {progress_text}"
            else:
                progress_text = f"[{prefix}] {progress_text}"

        print(progress_text)

    def table_row(self, columns: list, widths: list = None, colors: list = None):
        """Print a formatted table row"""
        if not widths:
            widths = [20] * len(columns)

        if not colors and self.use_colors:
            colors = [Colors.RESET] * len(columns)
        elif not self.use_colors:
            colors = [""] * len(columns)

        row = []
        for col, width, color in zip(columns, widths, colors):
            if self.use_colors:
                row.append(f"{color}{str(col)[:width].ljust(width)}{Colors.RESET}")
            else:
                row.append(str(col)[:width].ljust(width))

        print(" | ".join(row))

    def print_summary(self, passed: int, failed: int, skipped: int = 0, total: int = None):
        """Print test summary with colors"""
        if total is None:
            total = passed + failed + skipped

        self.separator()
        self.info("Test Execution Summary", prefix="SUMMARY")
        self.separator()

        # Calculate success rate
        success_rate = (passed / total * 100) if total > 0 else 0

        # Print statistics
        if self.use_colors:
            print(f"  {Colors.GREEN} Passed:{Colors.RESET}  {passed:3d} ({passed/total*100:.1f}%)")
            print(f"  {Colors.RED} Failed:{Colors.RESET}  {failed:3d} ({failed/total*100:.1f}%)")
            if skipped > 0:
                print(f"  {Colors.YELLOW} Skipped:{Colors.RESET} {skipped:3d} ({skipped/total*100:.1f}%)")
            print(f"  {Colors.CYAN}Total:{Colors.RESET}   {total:3d}")

            # Success rate with color coding
            if success_rate >= 90:
                rate_color = Colors.BOLD_GREEN
            elif success_rate >= 70:
                rate_color = Colors.BOLD_YELLOW
            else:
                rate_color = Colors.BOLD_RED

            print(f"\n  {Colors.BOLD_CYAN}Success Rate:{Colors.RESET} {rate_color}{success_rate:.1f}%{Colors.RESET}")
        else:
            print(f"  Passed:  {passed:3d} ({passed/total*100:.1f}%)")
            print(f"  Failed:  {failed:3d} ({failed/total*100:.1f}%)")
            if skipped > 0:
                print(f"  Skipped: {skipped:3d} ({skipped/total*100:.1f}%)")
            print(f"  Total:   {total:3d}")
            print(f"\n  Success Rate: {success_rate:.1f}%")

        self.separator()


# Global logger instance for easy import
logger = ColorLogger()


# Convenience functions for quick logging
def log_info(message: str, prefix: str = None):
    """Quick info log"""
    logger.info(message, prefix)


def log_success(message: str, prefix: str = None):
    """Quick success log"""
    logger.success(message, prefix)


def log_warning(message: str, prefix: str = None):
    """Quick warning log"""
    logger.warning(message, prefix)


def log_error(message: str, prefix: str = None):
    """Quick error log"""
    logger.error(message, prefix)


def log_step(message: str, prefix: str = None):
    """Quick step log"""
    logger.step(message, prefix)


if __name__ == "__main__":
    # Demo of color logger capabilities
    demo_logger = ColorLogger(use_symbols=True)

    demo_logger.header("TestZen Color Logger Demo")

    demo_logger.info("Starting test execution...")
    demo_logger.step("Connecting to device")
    demo_logger.success("Device connected successfully")

    demo_logger.separator()

    demo_logger.step("Running test cases")
    demo_logger.pass_step("Login test passed")
    demo_logger.fail_step("Navigation test failed")
    demo_logger.skip_step("Payment test skipped")
    demo_logger.warning("Low memory detected")
    demo_logger.error("Network timeout occurred")

    demo_logger.separator()

    # Progress demo
    for i in range(1, 6):
        demo_logger.progress(i, 5, f"Processing test {i}", prefix="TESTS")

    demo_logger.separator()

    # Table demo
    demo_logger.info("Test Results Table:")
    demo_logger.table_row(["Test Name", "Status", "Time (s)"], [20, 10, 10])
    demo_logger.table_row(["-" * 20, "-" * 10, "-" * 10], [20, 10, 10])
    demo_logger.table_row(["Login Test", "PASS", "2.5"], [20, 10, 10],
                          [Colors.RESET, Colors.GREEN, Colors.RESET])
    demo_logger.table_row(["Navigation Test", "FAIL", "5.2"], [20, 10, 10],
                          [Colors.RESET, Colors.RED, Colors.RESET])
    demo_logger.table_row(["Payment Test", "SKIP", "0.0"], [20, 10, 10],
                          [Colors.RESET, Colors.YELLOW, Colors.RESET])

    # Summary
    demo_logger.print_summary(passed=8, failed=2, skipped=1)
