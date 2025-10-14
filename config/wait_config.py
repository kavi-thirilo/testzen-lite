"""
TestZen Wait Configuration
Configure wait times and timeouts for different scenarios to prevent flaky tests
"""

# WebView Configuration
WEBVIEW_WAIT_CONFIG = {
    # Maximum time to wait for WebView context to appear (seconds)
    'webview_context_timeout': 30,

    # Maximum time to wait for WebView content to load (seconds)
    'webview_content_timeout': 30,

    # Poll interval when checking for WebView readiness (seconds)
    'webview_poll_interval': 1,

    # Auto-detect and switch to WebView context
    'auto_detect_webview': True,
}

# Element Finding Configuration
ELEMENT_WAIT_CONFIG = {
    # Default timeout for finding elements (seconds)
    'default_timeout': 10,

    # Timeout for optional elements (seconds)
    'optional_element_timeout': 3,

    # Number of retry attempts for element finding
    'retry_attempts': 3,

    # Wait between retry attempts (seconds)
    'retry_interval': 1,
}

# App Launch Configuration
APP_LAUNCH_CONFIG = {
    # Wait after launching app for initialization (seconds)
    # Note: Device manager uses Appium query_app_state API to verify app is
    # in foreground (max 10s), so this is minimal additional stabilization
    'post_launch_wait': 2,

    # Additional wait for hybrid/WebView apps (seconds)
    'webview_app_additional_wait': 5,

    # Maximum time to wait for app to become interactive (seconds)
    'app_ready_timeout': 30,
}

# CI/CD Specific Configuration
CI_CONFIG = {
    # Detect if running in CI environment
    'auto_detect_ci': True,

    # Additional wait multiplier for CI (e.g., 1.5 = 50% longer waits)
    'ci_wait_multiplier': 1.2,

    # Extra stability wait in CI after critical operations (seconds)
    'ci_stability_wait': 2,
}

# Action-specific Configuration
ACTION_WAIT_CONFIG = {
    # Wait after click action for UI to settle (seconds)
    'post_click_wait': 2,

    # Wait after input action for UI to update (seconds)
    'post_input_wait': 1,

    # Wait before taking screenshot to ensure UI is stable (seconds)
    'pre_screenshot_wait': 0.5,

    # Wait after taking screenshot (seconds)
    'post_screenshot_wait': 0.5,
}

def is_ci_environment():
    """Detect if running in CI/CD environment"""
    import os
    ci_indicators = [
        'CI', 'CONTINUOUS_INTEGRATION',
        'GITHUB_ACTIONS', 'GITLAB_CI', 'JENKINS_HOME',
        'TRAVIS', 'CIRCLECI', 'BITBUCKET_BUILD_NUMBER'
    ]
    return any(os.getenv(key) for key in ci_indicators)

def get_wait_timeout(config_key, timeout_value):
    """Get timeout value with CI multiplier if applicable"""
    if CI_CONFIG['auto_detect_ci'] and is_ci_environment():
        return int(timeout_value * CI_CONFIG['ci_wait_multiplier'])
    return timeout_value

def get_webview_timeout():
    """Get WebView wait timeout with CI adjustment"""
    return get_wait_timeout('webview', WEBVIEW_WAIT_CONFIG['webview_content_timeout'])

def get_element_timeout(is_optional=False):
    """Get element finding timeout with CI adjustment"""
    timeout = ELEMENT_WAIT_CONFIG['optional_element_timeout'] if is_optional else ELEMENT_WAIT_CONFIG['default_timeout']
    return get_wait_timeout('element', timeout)

def get_app_launch_wait():
    """Get app launch wait time with CI adjustment"""
    return get_wait_timeout('launch', APP_LAUNCH_CONFIG['post_launch_wait'])
