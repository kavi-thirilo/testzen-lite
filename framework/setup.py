"""
Setup script for TestZen - No-Code Test Automation Framework
"""

from setuptools import setup, find_packages

try:
    with open("../docs/README.md", "r", encoding="utf-8") as fh:
        long_description = fh.read()
except:
    try:
        with open("../README.md", "r", encoding="utf-8") as fh:
            long_description = fh.read()
    except:
        long_description = "TestZen - No-Code Test Automation Framework"

setup(
    name="testzen",
    version="1.0.0",
    author="TestZen Team",
    author_email="support@testzen.com",
    description="No-Code Test Automation Framework for Mobile Applications",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/testzen/testzen",
    packages=find_packages(),
    classifiers=[
        "Development Status :: 4 - Beta",
        "Intended Audience :: Developers",
        "Topic :: Software Development :: Testing",
        "License :: OSI Approved :: MIT License",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
        "Programming Language :: Python :: 3.11",
    ],
    python_requires=">=3.8",
    install_requires=[
        "Appium-Python-Client>=3.1.1",
        "selenium>=4.15.0",
        "pandas>=2.1.0",
        "openpyxl>=3.1.2",
        "PyYAML>=6.0.1",
        "colorlog>=6.7.0",
        "webdriver-manager>=4.0.1",
        "requests>=2.31.0",
        "Pillow>=10.0.0"
    ],
    entry_points={
        "console_scripts": [
            "testzen=scripts.run_tests:main",
        ],
    },
    include_package_data=True,
    package_data={
        "framework": ["*.yaml", "*.json"],
        "config": ["*.yaml", "*.json"],
    },
)