"""
Helper utilities for TestZen framework
"""

import os
import time
from datetime import datetime
import logging

logger = logging.getLogger(__name__)


def ensure_directory_exists(directory_path):
    """Create directory if it doesn't exist"""
    try:
        os.makedirs(directory_path, exist_ok=True)
        return True
    except Exception as e:
        logger.error(f"Failed to create directory {directory_path}: {e}")
        return False


def get_timestamp(format_str="%Y%m%d_%H%M%S"):
    """Get current timestamp string"""
    return datetime.now().strftime(format_str)


def safe_filename(filename):
    """Create a safe filename by removing/replacing invalid characters"""
    invalid_chars = '<>:"/\\|?*'
    safe_name = filename
    for char in invalid_chars:
        safe_name = safe_name.replace(char, '_')
    return safe_name


def wait_with_timeout(condition_func, timeout_seconds=30, poll_interval=1):
    """Wait for a condition with timeout"""
    start_time = time.time()
    while time.time() - start_time < timeout_seconds:
        if condition_func():
            return True
        time.sleep(poll_interval)
    return False


def format_duration(seconds):
    """Format duration in seconds to human readable format"""
    if seconds < 60:
        return f"{seconds:.1f}s"
    elif seconds < 3600:
        minutes = seconds // 60
        remaining_seconds = seconds % 60
        return f"{int(minutes)}m {remaining_seconds:.0f}s"
    else:
        hours = seconds // 3600
        minutes = (seconds % 3600) // 60
        return f"{int(hours)}h {int(minutes)}m"


def cleanup_old_files(directory, max_age_days=7, file_pattern="*"):
    """Cleanup old files in directory"""
    try:
        import glob
        cutoff_time = time.time() - (max_age_days * 24 * 60 * 60)
        
        pattern = os.path.join(directory, file_pattern)
        files = glob.glob(pattern)
        
        deleted_count = 0
        for file_path in files:
            if os.path.getmtime(file_path) < cutoff_time:
                try:
                    os.remove(file_path)
                    deleted_count += 1
                except Exception as e:
                    logger.warning(f"Could not delete {file_path}: {e}")
        
        if deleted_count > 0:
            logger.info(f"Cleaned up {deleted_count} old files from {directory}")
        
        return deleted_count
        
    except Exception as e:
        logger.error(f"Error during cleanup: {e}")
        return 0


def validate_file_exists(file_path, file_type="file"):
    """Validate that file exists and is accessible"""
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"{file_type.title()} not found: {file_path}")
    
    if not os.path.isfile(file_path):
        raise ValueError(f"Path is not a file: {file_path}")
    
    return True


def load_json_safely(file_path):
    """Load JSON file safely with error handling"""
    import json
    try:
        with open(file_path, 'r') as f:
            return json.load(f)
    except FileNotFoundError:
        logger.error(f"JSON file not found: {file_path}")
        return {}
    except json.JSONDecodeError as e:
        logger.error(f"Invalid JSON in {file_path}: {e}")
        return {}
    except Exception as e:
        logger.error(f"Error loading JSON {file_path}: {e}")
        return {}


def save_json_safely(data, file_path):
    """Save JSON file safely with error handling"""
    import json
    try:
        # Ensure directory exists
        directory = os.path.dirname(file_path)
        if directory:
            ensure_directory_exists(directory)
        
        with open(file_path, 'w') as f:
            json.dump(data, f, indent=2, default=str)
        return True
    except Exception as e:
        logger.error(f"Error saving JSON to {file_path}: {e}")
        return False