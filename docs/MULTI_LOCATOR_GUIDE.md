# Multi-Locator Support Guide

## Overview

TestZen now supports **multiple fallback locators** per test step. This feature dramatically improves test reliability across different environments (local vs CI/CD, different Android versions, etc.).

## Why Multi-Locators?

Different environments may handle element attributes differently:

- **Local Environment**: `@hint` attribute works perfectly
- **CI/CD Environment**: `@hint` not indexed, but `@resource-id` works
- **Older Android**: Some attributes not available at all

Instead of maintaining separate test files, use multi-locators for automatic fallback!

## Format - Separate Columns (Recommended)

Your Excel file now has **three separate locator columns** for easy editing:

| Column Name | Purpose | When to Use |
|-------------|---------|-------------|
| **Locator Value** | Primary locator | Your most reliable locator (tried first) |
| **Locator Value 2** | Fallback option 1 | Alternative if primary fails |
| **Locator Value 3** | Fallback option 2 | Last resort if both fail |

**Simply leave columns empty** if you don't need multiple locators for a step!

## How It Works

1. TestZen tries the **first locator**
2. If it fails, tries the **second locator**
3. Continues until a locator succeeds
4. If all fail, the step fails

**Timeout:** Each locator gets 3 seconds before trying the next one.

## Examples

### Example 1: Username Field

**Problem**: `@hint` works locally but fails in CI/CD

**Excel Format**:

| S.No | Description | Locator Type | Locator Value | Locator Value 2 | Locator Value 3 |
|------|-------------|--------------|---------------|-----------------|-----------------|
| 1 | Enter username | xpath | `//android.widget.EditText[@hint="Username"]` | `//android.widget.EditText[@resource-id="username"]` | `//android.widget.EditText[1]` |

**Result**:
- Local: Uses `@hint` (fast) ✓
- CI/CD: Falls back to `@resource-id` (reliable) ✓
- Worst case: Uses position `[1]` (last resort) ✓

### Example 2: Login Button

**Multiple strategies to find the same button**:

| S.No | Description | Locator Type | Locator Value | Locator Value 2 | Locator Value 3 |
|------|-------------|--------------|---------------|-----------------|-----------------|
| 2 | Click Login | xpath | `//android.widget.Button[@text="Log In"]` | `//android.widget.Button[@content-desc="login_button"]` | `//android.widget.Button[@resource-id="btn_login"]` |

### Example 3: Simple Step (No Fallback Needed)

**When your locator is 100% reliable, just use the first column**:

| S.No | Description | Locator Type | Locator Value | Locator Value 2 | Locator Value 3 |
|------|-------------|--------------|---------------|-----------------|-----------------|
| 3 | Click Submit | id | `submit_button` | *(empty)* | *(empty)* |

## Best Practices

### 1. Order Matters - Most Specific First

```
✓ Good:
@hint="Username"|@resource-id="username"|//android.widget.EditText[1]

✗ Bad:
//android.widget.EditText[1]|@hint="Username"|@resource-id="username"
```

**Why**: The first locator that succeeds wins. Position-based `[1]` might match the wrong element.

### 2. Use 2-3 Locators (Not Too Many)

```
✓ Good: 2-3 well-chosen locators
@hint="Search"|@content-desc="search_input"|@resource-id="search"

✗ Bad: Too many similar locators
@hint="Search"|@text="Search"|@label="Search"|@name="Search"
```

**Why**: Each locator adds 3 seconds if it fails. Too many = slow tests.

### 3. Recommended Fallback Strategy

For **input fields**:
```
@hint="FieldName"|@resource-id="field_id"|//android.widget.EditText[position]
```

For **buttons**:
```
@text="Button Text"|@content-desc="button_desc"|@resource-id="button_id"
```

For **general elements**:
```
@content-desc="element"|@resource-id="element_id"|xpath_with_class_and_text
```

## Console Output

When using multi-locators, you'll see detailed logs:

```
[TestZen] [INFO] Multi-locator detected: 3 options
[TestZen] [INFO] Trying locator 1/3: //android.widget.EditText[@hint="Username"]...
[TestZen] [WARNING] Locator 1 failed, trying next...
[TestZen] [INFO] Trying locator 2/3: //android.widget.EditText[@resource-id="username"]...
[TestZen] [SUCCESS] Found element using locator 2: //android.widget.EditText[@resource-id="username"]
```

This helps you understand **which locator worked** and optimize your test file.

## Excel File Format

Your Excel file's **TestCases** sheet should look like this:

| S.No | Description | Action | Locator Type | Locator Value | Locator Value 2 | Locator Value 3 | Input Data | Status | Result Message |
|------|-------------|--------|--------------|---------------|-----------------|-----------------|------------|--------|----------------|
| 1 | Enter username | input | xpath | `//android.widget.EditText[@hint="Username"]` | `//android.widget.EditText[@resource-id="username"]` | `//android.widget.EditText[1]` | testuser | | |
| 2 | Click Login | click | xpath | `//android.widget.Button[@text="Log In"]` | `//android.widget.Button[@content-desc="login_btn"]` | | | | |
| 3 | Verify welcome | verify | id | `welcome_msg` | | | | | |

**Note**: Leave `Locator Value 2` and `Locator Value 3` empty for steps that don't need fallback locators.

## Common Use Cases

### Case 1: Environment Differences

**Scenario**: Hint works locally, resource-id works in CI/CD

```
Locator Value: //android.widget.EditText[@hint="Email"]|//android.widget.EditText[@resource-id="email_input"]
```

### Case 2: App Version Differences

**Scenario**: Button text changed between versions

```
Locator Value: //android.widget.Button[@text="Submit"]|//android.widget.Button[@text="Send"]|//android.widget.Button[@resource-id="submit_btn"]
```

### Case 3: Flaky Element IDs

**Scenario**: Resource IDs are dynamic or unstable

```
Locator Value: //android.widget.TextView[@text="Welcome"]|//android.widget.TextView[contains(@text, "Welcome")]|//android.widget.TextView[1]
```

## Troubleshooting

### All Locators Failing?

Check the console output to see which locators were tried:

```
[TestZen] [ERROR] All 3 locators failed
```

**Solutions**:
1. Use Appium Inspector to find current element attributes
2. Add a new working locator to the list
3. Check if element is in a different context (WebView vs Native)

### Test Too Slow?

If you have many multi-locator steps and all first locators are failing:

**Solution**: Reorder your locators - put the working one first!

```
Before: @hint="Text"|@resource-id="field_id"  # hint fails, wastes 3s
After:  @resource-id="field_id"|@hint="Text"  # resource-id works immediately
```

## How to Add Fallback Locators to Existing Tests

### Method 1: Edit in Excel (Easiest)

1. Open your test file in Excel
2. Find the "Locator Value 2" and "Locator Value 3" columns
3. Add fallback locators in those columns for steps that need them
4. Leave empty for steps that don't need fallbacks
5. Save and run!

### Method 2: Python Script

To programmatically add fallback locators:

```python
import pandas as pd

# Read test file
df = pd.read_excel('your_test.xlsx', sheet_name='TestCases')

# Add columns if they don't exist
if 'Locator Value 2' not in df.columns:
    df['Locator Value 2'] = ''
if 'Locator Value 3' not in df.columns:
    df['Locator Value 3'] = ''

# Add fallback locator for username field
mask = df['Description'].str.contains('username', case=False, na=False)
df.loc[mask, 'Locator Value 2'] = '//android.widget.EditText[@resource-id="username"]'
df.loc[mask, 'Locator Value 3'] = '//android.widget.EditText[1]'

# Save
with pd.ExcelWriter('your_test.xlsx', engine='openpyxl', mode='a', if_sheet_exists='replace') as writer:
    df.to_excel(writer, sheet_name='TestCases', index=False)
```

## Summary

✅ **Use multi-locators for**:
- Environment-specific differences
- Flaky element attributes
- App version compatibility
- Robust CI/CD tests

❌ **Don't use multi-locators for**:
- Simple, stable elements with 100% reliable locators
- When test speed is critical and locators never fail

**Result**: More reliable tests that work across all environments! 🎯
