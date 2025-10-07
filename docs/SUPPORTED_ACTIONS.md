# [TZ] TestZen Framework - Supported Actions Reference

## Complete List of Supported Actions

This document provides the definitive reference for all actions supported by the TestZen Mobile Automation Framework. Use this reference when creating Excel test files to avoid action-related errors.

## Core Actions (Excel-Compatible)

These are the primary actions that should be used in Excel test files:

### **Basic Interaction Actions**

#### **`click`**
- **Purpose**: Click on any UI element
- **Locator Required**: Yes (xpath, id, content-desc, etc.)
- **Input Data**: Not required
- **Example**: Click on "Login" button
```excel
Action: click
Locator Type: xpath
Locator Value: //*[@text='Login']
```

#### **`input`**
- **Purpose**: Enter text into input fields
- **Locator Required**: Yes (targets EditText or input fields)
- **Input Data**: Required (the text to enter)
- **Example**: Enter username
```excel
Action: input
Locator Type: xpath
Locator Value: //android.widget.EditText[@content-desc='Username']
Input Data: user123
```

#### **`verify`**
- **Purpose**: Verify element exists or contains specific text
- **Locator Required**: Yes
- **Input Data**: Optional (expected text to verify)
- **Example**: Verify success message
```excel
Action: verify
Locator Type: xpath
Locator Value: //*[contains(@text,'Success')]
```

#### **`wait`**
- **Purpose**: Wait for specified duration or element
- **Locator Required**: Optional
- **Input Data**: Duration in seconds (optional)
- **Example**: Wait 3 seconds
```excel
Action: wait
Input Data: 3
```

#### **`scroll`**
- **Purpose**: Scroll within scrollable containers
- **Locator Required**: Optional (scroll container)
- **Input Data**: Optional (direction: up, down, left, right)
- **Example**: Scroll down
```excel
Action: scroll
Input Data: down
```

### **App Management Actions**

#### **`force_stop`**
- **Purpose**: Force stop an application
- **Locator Required**: No
- **Input Data**: App package name
- **Example**: Stop target mobile app
```excel
Action: force_stop
Input Data: com.example.mobile.app
```

### **Advanced Actions (Framework-Specific)**


## Deprecated/Unsupported Actions

**DO NOT USE** these actions in Excel files - they will cause test failures:

- `coordinate_tap` - Use `click` instead
- `send_keys` - Use `input` instead
- `tap` - Use `click` instead
- `wait_for_element` - Framework handles automatically
- `verify_text` - Use `verify` instead
- `long_press` - Not commonly needed for Excel tests
- `back` / `back_button` - Use hardware back or specific navigation
- `scroll_and_click` - Use separate `scroll` and `click` actions

## Framework-Internal Actions (Code-Only)

These actions are available in the automation engine but not intended for Excel use:

- `install` - App installation (handled by framework)
- `uninstall` - App removal (handled by framework)
- `launch` - App launching (handled by framework)
- `long_press` - Long press gestures
- `back_button` - Hardware back button
- `scroll_and_click` - Combined scroll and click operation

## Excel Test File Structure

When creating Excel test files, use this column structure:

| Column | Name | Required | Description |
|--------|------|----------|-------------|
| A | S.No | Yes | Sequential step number |
| B | Description | Yes | Human-readable step description |
| C | Action | Yes | Action type from supported list above |
| D | Locator Type | Conditional | Required for actions targeting elements |
| E | Locator Value | Conditional | XPath, ID, content-desc, etc. |
| F | Input Data | Conditional | Required for `input` actions, optional for others |
| G | Status | No | Framework updates during execution |
| H | Result Message | No | Framework updates during execution |

## Action Selection Guidelines

### **For UI Interactions:**
- **Clicking elements**: Always use `click`
- **Text input**: Always use `input`
- **Element verification**: Always use `verify`
- **Waiting**: Use `wait` sparingly (framework handles most waits)

### **For App Authentication:**
- **Custom login flows**: Use individual `click` and `input` actions to create login sequences

### **For Form Handling:**
- **Text fields**: Use `input` with appropriate XPath locators
- **Dropdowns**: Use `click` to open, then `click` to select
- **Checkboxes/Switches**: Use `click` to toggle
- **Buttons**: Always use `click`

## Locator Types

Supported locator types for element targeting:

- **`xpath`** - Most flexible, recommended for complex scenarios
- **`id`** - Android resource ID
- **`content-desc`** - Accessibility content description
- **`text`** - Visible text content (use with care)
- **`class`** - UI element class name

## Examples of Complete Test Steps

### Example 1: Simple Login Form
```excel
S.No | Description | Action | Locator Type | Locator Value | Input Data
1 | Enter username | input | xpath | //android.widget.EditText[@content-desc='Username'] | testuser
2 | Enter password | input | xpath | //android.widget.EditText[@content-desc='Password'] | password123
3 | Click login button | click | xpath | //*[@text='Login'] |
4 | Verify success message | verify | xpath | //*[contains(@text,'Welcome')] | Welcome
```

### Example 2: Payment Flow
```excel
S.No | Description | Action | Locator Type | Locator Value | Input Data
1 | Launch app | launch | | com.example.app |
2 | Click Billing section | click | content-desc | Billing |
3 | Enter payment amount | input | xpath | //android.widget.EditText[1] | 100.00
4 | Click Submit button | click | xpath | //*[@text='Submit Payment'] |
5 | Verify payment successful | verify | xpath | //*[contains(@text,'successful')] |
```

## Best Practices

### **Action Selection:**
1. **Always** use `click` instead of `tap` or `coordinate_tap`
2. **Always** use `input` instead of `send_keys`
3. **Create** custom login sequences using basic actions for authentication flows
4. **Avoid** unnecessary `wait` actions - framework handles element waiting

### **Locator Strategy:**
1. **Prefer** `content-desc` for accessibility-labeled elements
2. **Use** `xpath` for complex element targeting
3. **Avoid** index-based locators like `[1]`, `[2]` when possible
4. **Target** specific attributes like `@hint`, `@text`, `@content-desc`

### **Test Design:**
1. **Break down** complex flows into individual actions
2. **Use** descriptive step descriptions
3. **Include** verification steps after important actions
4. **Group** related actions logically

## Common Mistakes to Avoid

1. **Using `coordinate_tap`** - Framework doesn't support this action
2. **Using `send_keys`** - Use `input` instead
3. **Manual wait actions** - Framework handles element waiting automatically
4. **Generic EditText indices** - Use specific locators when possible
5. **Missing input data** - `input` actions require data in Input Data column
6. **Wrong locator types** - Stick to supported types listed above

---

** Remember**: When in doubt, refer to the `templates/Mobile_Test_Steps_Template.xlsx` file for examples of properly formatted test steps using supported actions.