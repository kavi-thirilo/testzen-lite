# Test Creation Guide

Complete guide for creating Excel test files for TestZen.

---

## Quick Start

1. Download sample: `tests/android/billing/Payment_Form_Validation_Test.xlsx`
2. Open in Excel/LibreOffice
3. Duplicate and modify for your app
4. Save in `tests/android/` or `tests/ios/`

---

## Excel File Structure

Each test file has these columns:

| Column | Description | Required |
|--------|-------------|----------|
| **Step** | Step number (1, 2, 3...) | Yes |
| **Action** | What to do (click, enter_text, etc.) | Yes |
| **Locator Type** | How to find element (id, xpath, accessibility_id) | For element actions |
| **Locator Value** | Element identifier | For element actions |
| **Test Data** | Text to enter or value to verify | For input/verification |
| **Expected Result** | What should happen | Optional but recommended |

---

## Available Actions

### Basic Actions

- **`click`** - Tap an element
- **`enter_text`** - Type text into a field
- **`clear_text`** - Clear a text field
- **`wait`** - Wait for specified seconds

### Verification Actions

- **`verify_text`** - Check if text exists on screen
- **`verify_element`** - Check if element exists
- **`verify_not_visible`** - Check element is NOT visible

### Navigation Actions

- **`swipe_up`** - Swipe up (scroll down)
- **`swipe_down`** - Swipe down (scroll up)
- **`swipe_left`** - Swipe left
- **`swipe_right`** - Swipe right
- **`go_back`** - Press device back button

### Advanced Actions

- **`take_screenshot`** - Capture screen
- **`close_app`** - Close the app
- **`launch_app`** - Reopen the app

---

## Locator Types

### Most Common (Recommended)

1. **`id`** - Android resource ID
   ```
   Example: com.example.app:id/username
   ```

2. **`accessibility_id`** - Accessibility identifier
   ```
   Example: username_field
   ```

3. **`xpath`** - XML path (use as last resort)
   ```
   Example: //android.widget.EditText[@text='Username']
   ```

### Also Available

- **`class_name`** - Element class
- **`text`** - Exact text match
- **`contains_text`** - Partial text match

---

## Multi-Locator Fallback System

TestZen supports multiple locators for one element. If first locator fails, it tries the next.

**Format:** Separate with pipe `|`

```
Locator Type: id|accessibility_id|xpath
Locator Value: login_button|LoginBtn|//android.widget.Button[@text='Login']
```

**Why use this?**
- App updates might change IDs
- Different Android versions use different structures
- Increases test reliability

---

## Example Test Cases

### Example 1: Login Test

| Step | Action | Locator Type | Locator Value | Test Data | Expected Result |
|------|--------|--------------|---------------|-----------|-----------------|
| 1 | wait | - | - | 3 | App loads |
| 2 | enter_text | id | username_field | testuser@example.com | Username entered |
| 3 | enter_text | id | password_field | Test1234! | Password entered |
| 4 | click | id | login_button | - | Login button clicked |
| 5 | verify_text | - | - | Welcome | Login successful |

---

### Example 2: Form Validation

| Step | Action | Locator Type | Locator Value | Test Data | Expected Result |
|------|--------|--------------|---------------|-----------|-----------------|
| 1 | click | id | email_field | - | Focus on email field |
| 2 | enter_text | id | email_field | invalid-email | Invalid email entered |
| 3 | click | id | submit_button | - | Submit clicked |
| 4 | verify_text | - | - | Invalid email format | Error message shown |
| 5 | clear_text | id | email_field | - | Field cleared |
| 6 | enter_text | id | email_field | valid@email.com | Valid email entered |
| 7 | click | id | submit_button | - | Submit clicked |
| 8 | verify_text | - | - | Success | Form submitted |

---

## Finding Element Locators

### Method 1: Using TestZen Inspector (Recommended)

1. Start inspector:
   ```bash
   cd appium-web-inspector
   ./scripts/startup/START_INSPECTOR.command
   ```
2. Open browser: http://localhost:3000
3. Connect to device
4. Click elements to see locators
5. Copy locator values to Excel

---

### Method 2: Using Appium Inspector

1. Download from: https://github.com/appium/appium-inspector/releases
2. Start Appium server
3. Launch Appium Inspector
4. Connect to `localhost:4723`
5. Click elements to see properties
6. Use resource-id, accessibility id, or xpath

---

### Method 3: Using Android Studio Layout Inspector

1. Open Android Studio
2. **Tools → Layout Inspector**
3. Select running app
4. Click elements to see IDs and properties

---

## Test Organization

Organize tests by feature modules:

```
tests/
├── android/
│   ├── login/
│   │   ├── valid_login.xlsx
│   │   ├── invalid_login.xlsx
│   │   └── forgot_password.xlsx
│   ├── checkout/
│   │   ├── add_to_cart.xlsx
│   │   ├── payment_flow.xlsx
│   │   └── coupon_code.xlsx
│   └── profile/
│       ├── edit_profile.xlsx
│       └── change_password.xlsx
└── ios/
    └── login/
        └── login_test.xlsx
```

**Benefits:**
- Easy to find specific tests
- Better reporting (grouped by module)
- Team collaboration
- Easier maintenance

---

## Best Practices

### 1. Use Descriptive Step Numbers
Don't skip numbers - use sequential: 1, 2, 3, 4...

### 2. Add Expected Results
Always document what should happen at each step

### 3. Use Wait Steps
Add `wait` actions after clicks/taps to let UI load:
```
Action: wait
Test Data: 2
```

### 4. Use Multi-Locator Fallback
Provide backup locators for critical elements

### 5. Keep Tests Focused
One test file = One user journey or feature

### 6. Use Clear Names
File names should describe what test does:
- ✅ `valid_login_test.xlsx`
- ❌ `test1.xlsx`

### 7. Test Data in Separate Column
Don't embed test data in locators - use Test Data column

---

## Common Mistakes to Avoid

### ❌ Missing Locator Type
```
Action: click
Locator Value: login_button
```
**Fix:** Add Locator Type (id, xpath, etc.)

---

### ❌ Wrong Test Data Column
```
Action: verify_text
Locator Value: Welcome
```
**Fix:** Put "Welcome" in Test Data column, not Locator Value

---

### ❌ No Wait After Actions
```
Step 1: click | login_button
Step 2: verify_text | Dashboard
```
**Fix:** Add wait between steps:
```
Step 1: click | login_button
Step 2: wait | 2
Step 3: verify_text | Dashboard
```

---

### ❌ Overly Complex XPath
```
//android.widget.FrameLayout[1]/android.widget.LinearLayout[1]/android.widget.FrameLayout[1]/android.widget.Button[2]
```
**Fix:** Use simpler locators (id, accessibility_id) or text-based xpath:
```
//android.widget.Button[@text='Login']
```

---

## Next Steps

After creating tests:

1. [Run your tests](../README.md#run-tests)
2. [View test reports](../README.md#view-reports)
3. Learn [advanced features](advanced-usage.md)

---

[← Back to Main README](../README.md)
