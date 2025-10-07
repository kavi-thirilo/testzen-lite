#!/usr/bin/env python3
"""
Main entry point for TestZen Mobile Automation
"""

import sys
import os
# Add parent directory to path to find src module
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from src.automation.testzen_automation import main

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)