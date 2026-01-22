# TestZen Lite - Lightweight No-Code Test Automation Framework

A minimal, security-compliant distribution of the TestZen no-code test automation framework designed for organizations with formal open-source approval processes.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Folder-Based Platform Convention](#folder-based-platform-convention)
- [Test Format (YAML)](#test-format-yaml)
- [Supported Instructions](#supported-instructions)
- [NLP Engine](#nlp-engine)
- [Pattern Management System](#pattern-management-system)
- [Synonym Registry](#synonym-registry)
- [Verification Engine](#verification-engine)
- [Smart Element Finder](#smart-element-finder)
- [Self-Healing Locators](#self-healing-locators)
- [CLI Usage](#cli-usage)
- [Configuration Reference](#configuration-reference)
- [Enterprise Test Reporting](#enterprise-test-reporting)
- [Intelligent Stability System](#intelligent-stability-system)
- [API Reference](#api-reference)
- [Project Structure](#project-structure)
- [Building](#building)
- [Security & Compliance](#security--compliance)
- [License](#license)

---

## Overview

TestZen Lite provides core test automation functionality without the full enterprise stack:

| Component | Full TestZen | TestZen Lite |
|-----------|-------------|--------------|
| Core Execution Engine | ✓ | ✓ |
| YAML Test Definitions | ✓ | ✓ |
| Android Support (Appium) | ✓ | ✓ |
| iOS Support (Appium) | ✓ | ✓ |
| Web Support (Selenium) | ✓ | ✓ |
| Smart Locators (Self-healing) | ✓ | ✓ |
| Intelligent Element Finder | ✓ | ✓ |
| Spatial Analysis | ✓ | ✓ |
| Compound UI Resolution | ✓ | ✓ |
| NLP Engine (37 intents) | ✓ | ✓ |
| Verification Engine (42+ types) | ✓ | ✓ |
| Centralized Registries | ✓ | ✓ |
| Soft Assertions & Reporting | ✓ | ✓ |
| Intelligent Stability System | ✓ | ✓ |
| REST API Server | ✓ | ✗ |
| React Web UI | ✓ | ✗ |
| Video Recording | ✓ | ✗ |
| Network Capture | ✓ | ✗ |
| Enterprise Reporting | ✓ | ✓ (HTML, JSON, JUnit XML) |

### Key Statistics

| Component | Count |
|-----------|-------|
| Intent Types | 37 |
| Verification Types | 42+ |
| Pattern Definitions | 43 |
| Synonym Sets | 30+ |
| Configuration Options | 27 |
| Report Formats | 4 (HTML, JSON, Summary, JUnit) |
| Report Hierarchy Levels | 5 (Module→Feature→Story→Test→Step) |
| Stability Components | 12 (6 core + 6 registry/adapters) |
| Stability Presets | 4 (FAST, DEFAULT, ROBUST, CI) |
| Platform Adapters | 3 (Web, Android, iOS) |
| Network Profiles | 5 |
| Kotlin Source Files | 59 |

---

## Architecture

TestZen Lite follows a clean, layered architecture with centralized registries:

```
┌─────────────────────────────────────────────────────────────────────┐
│                           CLI / API Layer                           │
│  testzen-cli (Main.kt) ─── TestZenRunner ─── TestLoader            │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
┌────────────────────────────────▼────────────────────────────────────┐
│                          Core Engine Layer                          │
│  ┌──────────────────────┐  ┌──────────────────────┐                │
│  │     NLP Engine       │  │  Execution Engine    │                │
│  │  ┌────────────────┐  │  │  ┌────────────────┐  │                │
│  │  │ IntentMatcher  │  │  │  │ TestExecutor   │  │                │
│  │  │ EntityExtractor│  │  │  │ InstructionExec│  │                │
│  │  └────────────────┘  │  │  │ GestureHandler │  │                │
│  └──────────────────────┘  │  └────────────────┘  │                │
│                            └──────────────────────┘                │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
┌────────────────────────────────▼────────────────────────────────────┐
│                      Centralized Registries                         │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐       │
│  │ PatternRegistry │ │ SynonymRegistry │ │VerificationReg. │       │
│  │ ┌─────────────┐ │ │ ┌─────────────┐ │ │ ┌─────────────┐ │       │
│  │ │PatternBuild.│ │ │ │SynonymBuild.│ │ │ │VerifyBuilder│ │       │
│  │ │PatternFrags.│ │ │ │DefaultSyns. │ │ │ │DefaultVerif.│ │       │
│  │ │DefaultPats. │ │ │ └─────────────┘ │ │ └─────────────┘ │       │
│  │ └─────────────┘ │ └─────────────────┘ └─────────────────┘       │
│  └─────────────────┘                                                │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
┌────────────────────────────────▼────────────────────────────────────┐
│                      Element Finding Layer                          │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │              SmartElementFinder (Orchestrator)               │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌───────────────────────┐  │   │
│  │  │ElementScorer│ │SpatialAnalyz│ │CompoundElementResolver│  │   │
│  │  │             │ │             │ │                       │  │   │
│  │  │ FuzzyMatch  │ │ Label→Field │ │ DatePickers, Radios   │  │   │
│  │  │ ActionScore │ │ Proximity   │ │ Multi-Select, etc.    │  │   │
│  │  └─────────────┘ └─────────────┘ └───────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │              SelfHealingLocator (Fallback)                   │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐    │   │
│  │  │LocatorCache │ │LocatorGen. │ │PageObjectRepository │    │   │
│  │  │             │ │             │ │                     │    │   │
│  │  │ Learning    │ │ Multi-strat.│ │ Large Project Scale │    │   │
│  │  └─────────────┘ └─────────────┘ └─────────────────────┘    │   │
│  └─────────────────────────────────────────────────────────────┘   │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
┌────────────────────────────────▼────────────────────────────────────┐
│                      Enterprise Reporting Layer                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                     ReportManager                             │  │
│  │  ┌─────────────────────────────────────────────────────────┐ │  │
│  │  │ TestExecutionReport → ModuleResult → FeatureResult →    │ │  │
│  │  │ StoryResult → TestCaseResult → StepResult (screenshots) │ │  │
│  │  └─────────────────────────────────────────────────────────┘ │  │
│  │  ┌─────────────────────┐  ┌────────────────────────────────┐ │  │
│  │  │ HtmlReportGenerator │  │    JsonReportGenerator         │ │  │
│  │  │ ┌─────────────────┐ │  │ ┌────────────┐ ┌─────────────┐ │ │  │
│  │  │ │ Interactive UI  │ │  │ │ Full JSON  │ │ JUnit XML   │ │ │  │
│  │  │ │ Bootstrap 5     │ │  │ │ Summary    │ │ CI/CD       │ │ │  │
│  │  │ │ Drill-down      │ │  │ └────────────┘ └─────────────┘ │ │  │
│  │  │ └─────────────────┘ │  └────────────────────────────────┘ │  │
│  │  └─────────────────────┘                                     │  │
│  └──────────────────────────────────────────────────────────────┘  │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
┌────────────────────────────────▼────────────────────────────────────┐
│                        Platform Layer                               │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │              PlatformDriverFactory                            │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │  │
│  │  │Android Driver│  │  iOS Driver  │  │  Web Driver  │        │  │
│  │  │(UiAutomator2)│  │ (XCUITest)   │  │ (Selenium)   │        │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘        │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

### Design Principles

1. **Centralized Registries**: All patterns, synonyms, and verification types are stored in central registries with DSL builders
2. **No Backward Compatibility Facades**: Direct usage of registries without wrapper objects
3. **Lazy Initialization**: Registries are initialized on first use
4. **Extensible via DSL**: All components can be extended using clean Kotlin DSL

---

## Quick Start

### Option 1: CLI with Folder Convention (Simplest)

```bash
# Run Android tests - platform auto-detected from folder
java -jar testzen-cli.jar run --tests ./tests/android

# Run iOS tests - platform auto-detected
java -jar testzen-cli.jar run --tests ./tests/ios --device iPhone-14

# Run Web tests - platform auto-detected
java -jar testzen-cli.jar run --tests ./tests/dotcom --headless
```

### Option 2: CLI with Explicit Platform

```bash
# Override or specify platform explicitly
java -jar testzen-cli.jar run --tests ./tests --platform android
java -jar testzen-cli.jar run --test ./my_test.yaml --platform web
```

### Option 3: Library Integration (Recommended for Production)

```kotlin
import com.testzen.core.TestZenRunner
import com.testzen.core.config.TestZenConfig
import com.testzen.core.model.Platform

fun main() {
    // Configure
    val config = TestZenConfig(
        implicitWait = 20,
        retryFailedSteps = 2,
        screenshotOnFailure = true
    )

    // Create runner
    val runner = TestZenRunner(config)

    // Load tests - platform auto-detected from folder
    runner.loadTests("./tests/android")

    // Execute - platform inferred from loaded tests
    val results = runner.execute(
        platform = Platform.ANDROID,
        deviceId = "emulator-5554"
    )

    // Process results
    results.forEach { result ->
        println("${result.testName}: ${result.status}")
    }
}
```

---

## Folder-Based Platform Convention

**No need to specify `--platform` flag!** Simply organize tests into platform folders:

```
tests/
├── android/           # Android tests (auto-detected)
│   ├── login_test.yaml
│   └── checkout_test.yaml
├── ios/               # iOS tests (auto-detected)
│   ├── login_test.yaml
│   └── checkout_test.yaml
├── web/               # Web tests (auto-detected)
│   └── signup_test.yaml
└── dotcom/            # Web tests (alias for web)
    └── homepage_test.yaml
```

### Supported Platform Folders

| Folder Name | Platform |
|-------------|----------|
| `android/` | Android (Appium UiAutomator2) |
| `ios/` | iOS (Appium XCUITest) |
| `web/` | Web (Selenium) |
| `dotcom/` | Web (alias) |
| `desktop/` | Web (alias) |
| `browser/` | Web (alias) |

---

## Test Format (YAML)

Tests are defined as simple YAML files with natural language steps:

```yaml
# tests/android/login_test.yaml
# Platform is auto-detected from folder - no need to specify!

test_id: login_test_001
name: "User Login Test"
description: "Verify user can log in successfully"
app_name: my_app

steps:
  - "Launch the app"
  - "Enter 'demo@example.com' in 'Email'"
  - "Enter 'password123' in 'Password'"
  - "Click 'Log In' button"
  - "Wait for 2 seconds"
  - "Verify 'Welcome' is displayed"
```

---

## Supported Instructions

TestZen Lite uses a **robust NLP engine** that understands natural language instructions with high flexibility.

### Interaction Instructions (6 types)

| Action | Examples |
|--------|----------|
| **Click/Tap** | `"Click the login button"`, `"Tap on Submit"`, `"Press continue"` |
| **Double Tap** | `"Double tap on the image"`, `"Double click the item"` |
| **Long Press** | `"Long press the item"`, `"Hold the delete button"` |
| **Enter Text** | `"Enter 'hello' in the search field"`, `"Type 'user@email.com' into Email"` |
| **Enter Text (Reversed)** | `"In the email field, type 'test@example.com'"` |
| **Clear Text** | `"Clear the search field"`, `"Erase text from email"` |

### Verification Instructions (22 types)

| Category | Actions |
|----------|---------|
| **Presence** | `VERIFY_DISPLAYED`, `VERIFY_NOT_DISPLAYED`, `VERIFY_EXISTS`, `VERIFY_NOT_EXISTS` |
| **State** | `VERIFY_ENABLED`, `VERIFY_DISABLED`, `VERIFY_CHECKED`, `VERIFY_NOT_CHECKED`, `VERIFY_SELECTED`, `VERIFY_FOCUSED` |
| **Text** | `VERIFY_TEXT`, `VERIFY_TEXT_CONTAINS`, `VERIFY_TEXT_MATCHES`, `VERIFY_TEXT_EMPTY`, `VERIFY_TEXT_NOT_EMPTY` |
| **Numeric** | `VERIFY_COUNT`, `VERIFY_VALUE_EQUALS`, `VERIFY_VALUE_GREATER`, `VERIFY_VALUE_LESS` |
| **Attribute** | `VERIFY_ATTRIBUTE`, `VERIFY_CSS_PROPERTY` |
| **Page** | `VERIFY_PAGE_TITLE`, `VERIFY_URL`, `VERIFY_TEXT_ON_SCREEN` |

### Wait Instructions (3 types)

| Action | Examples |
|--------|----------|
| **Wait Duration** | `"Wait for 3 seconds"`, `"Wait 5s"`, `"Pause for 2 seconds"` |
| **Wait for Element** | `"Wait for the dashboard to appear"` |
| **Wait for Element Gone** | `"Wait for the loading spinner to disappear"` |

### Navigation Instructions (4 types)

| Action | Examples |
|--------|----------|
| **Scroll** | `"Scroll down"`, `"Scroll up to find 'Settings'"` |
| **Swipe** | `"Swipe left"`, `"Swipe right on the card"` |
| **Navigate Back** | `"Go back"`, `"Press back button"` |
| **Navigate Forward** | `"Go forward"`, `"Navigate forward"` |

### App Lifecycle Instructions (3 types)

| Action | Examples |
|--------|----------|
| **Launch App** | `"Launch the app"`, `"Open the application"` |
| **Close App** | `"Close the app"`, `"Quit the application"` |
| **Take Screenshot** | `"Take a screenshot"`, `"Capture screen"` |

### Form Control Instructions (4 types)

| Action | Examples |
|--------|----------|
| **Select Option** | `"Select 'Option 1' from the dropdown"` |
| **Check Checkbox** | `"Check the Terms checkbox"` |
| **Uncheck Checkbox** | `"Uncheck notifications"` |
| **Toggle Switch** | `"Toggle dark mode on"`, `"Turn on WiFi"` |

---

## NLP Engine

The NLP engine parses natural language instructions into structured intents and entities.

### Features

| Feature | Description |
|---------|-------------|
| **37 Intent Types** | Comprehensive action coverage |
| **50+ Synonyms per action** | "click", "tap", "press", "touch", "select", "hit" all work |
| **Flexible Word Order** | `"Enter 'text' in field"` and `"In field, enter 'text'"` both work |
| **Smart Quotes** | Handles `'single'`, `"double"`, and `"smart"` quotes |
| **Confidence Scoring** | Returns confidence level (0.0-1.0) |
| **Alternative Suggestions** | Provides alternative interpretations |
| **Centralized Pattern Registry** | All patterns in one place |
| **Centralized Synonym Registry** | All synonyms in one place |

### Programmatic Usage

```kotlin
import com.testzen.core.nlp.NLPEngine
import com.testzen.core.nlp.Intent

val engine = NLPEngine()

// Parse instruction
val result = engine.parse("click the login button")
println("Intent: ${result.intent}")         // CLICK
println("Confidence: ${result.confidence}") // 0.95
println("Target: ${result.entities.target}") // "login"

// Check validity
if (engine.isValidInstruction("tap submit")) {
    // Process
}

// Get suggestions for autocomplete
val suggestions = engine.suggestIntents("ent")  // Returns [ENTER_TEXT]
```

### All 37 Intent Types

```kotlin
enum class Intent {
    // Interactions (5)
    CLICK, DOUBLE_CLICK, LONG_PRESS, ENTER_TEXT, CLEAR_TEXT,

    // Verification - Presence (4)
    VERIFY_DISPLAYED, VERIFY_NOT_DISPLAYED, VERIFY_EXISTS, VERIFY_NOT_EXISTS,

    // Verification - State (7)
    VERIFY_ENABLED, VERIFY_DISABLED, VERIFY_CHECKED, VERIFY_NOT_CHECKED,
    VERIFY_SELECTED, VERIFY_NOT_SELECTED, VERIFY_FOCUSED,

    // Verification - Text (5)
    VERIFY_TEXT, VERIFY_TEXT_CONTAINS, VERIFY_TEXT_MATCHES,
    VERIFY_TEXT_EMPTY, VERIFY_TEXT_NOT_EMPTY,

    // Verification - Numeric (4)
    VERIFY_COUNT, VERIFY_VALUE_EQUALS, VERIFY_VALUE_GREATER, VERIFY_VALUE_LESS,

    // Verification - Attribute/CSS (2)
    VERIFY_ATTRIBUTE, VERIFY_CSS_PROPERTY,

    // Verification - Page (3)
    VERIFY_PAGE_TITLE, VERIFY_URL, VERIFY_TEXT_ON_SCREEN,

    // Wait (3)
    WAIT_DURATION, WAIT_FOR_ELEMENT, WAIT_FOR_ELEMENT_GONE,

    // Navigation (4)
    SCROLL, SWIPE, NAVIGATE_BACK, NAVIGATE_FORWARD,

    // App Lifecycle (3)
    LAUNCH_APP, CLOSE_APP, TAKE_SCREENSHOT,

    // Form Controls (4)
    SELECT_OPTION, CHECK_CHECKBOX, UNCHECK_CHECKBOX, TOGGLE_SWITCH,

    // Fallback (1)
    UNKNOWN
}
```

### Extracted Entities

```kotlin
data class ExtractedEntities(
    val target: String? = null,           // Primary target element
    val secondaryTarget: String? = null,  // For drag-drop, select from
    val value: String? = null,            // Text value to enter/verify
    val numericValue: Long? = null,       // Duration, count, etc.
    val direction: String? = null,        // up, down, left, right
    val option: String? = null,           // Option to select
    val attributeName: String? = null,    // For attribute verification
    val cssProperty: String? = null,      // For CSS verification
    val comparisonOperator: String? = null, // equals, contains, etc.
    val regexPattern: String? = null,     // For text matching
    val modifiers: Map<String, Any> = emptyMap()
)
```

---

## Pattern Management System

TestZen Lite uses a **centralized pattern management system** for all NLP patterns.

### Architecture

```
PatternRegistry          ← Central storage (43 patterns)
├── PatternCategory      ← Organized by category
├── PatternFragments     ← Reusable regex components
├── PatternBuilder       ← DSL for clean definition
└── DefaultPatterns      ← Built-in patterns
```

### Pattern Categories

| Category | Description | Count |
|----------|-------------|-------|
| `INTERACTION` | Click, tap, enter text, clear, etc. | 8 |
| `VERIFICATION` | All verify/assert patterns | 20 |
| `NAVIGATION` | Scroll, swipe, back, forward | 4 |
| `WAIT` | Wait duration, wait for element | 3 |
| `APP_LIFECYCLE` | Launch, close, screenshot | 3 |
| `FORM_CONTROL` | Select, check, uncheck, toggle | 5 |
| `CUSTOM` | User-defined patterns | - |

### Using Pattern Fragments

```kotlin
import com.testzen.core.nlp.patterns.PatternFragments as F

// Instead of raw regex:
// Regex("""(?:verify|check|assert)\s+(?:that\s+)?['"]?([^'"]+?)['"]?\s+(?:is\s+)?(?:displayed|visible)""")

// Use composable fragments:
Regex("${F.VERIFY_SIMPLE}\\s+${F.THAT}${F.TARGET}\\s+${F.IS}${F.DISPLAYED}")
```

### Available Fragments

| Fragment | Pattern | Example Matches |
|----------|---------|-----------------|
| `F.CLICK` | `(?:click\|tap\|press\|touch\|select\|hit)` | click, tap, press |
| `F.VERIFY` | `(?:verify\|check\|assert\|confirm\|...)` | verify, check, assert |
| `F.DISPLAYED` | `(?:displayed\|visible\|shown\|present)` | displayed, visible |
| `F.TARGET` | `['"]?([^'"]+?)['"]?` | 'Login', Login, "Submit" |
| `F.QUOTED_VALUE` | `['"]([^'"]+)['"]` | 'hello', "world" |
| `F.DIRECTION` | `(up\|down\|left\|right)` | up, down, left, right |
| `F.DURATION` | `(\d+)\s*(?:seconds?\|s\|ms\|...)` | 5 seconds, 500ms |

### Defining Custom Patterns

```kotlin
import com.testzen.core.nlp.patterns.*
import com.testzen.core.nlp.Intent

// Define pattern using DSL
val myPattern = pattern(Intent.CLICK) {
    keywords("click", "tap", "press")
    regex { "${F.CLICK}\\s+${F.ON}${F.THE}${F.TARGET_CLEAN}$" }
    priority(1.0)
    extractTarget()
}

// Register with PatternRegistry
val registry = PatternRegistry.default()
registry.register(PatternCategory.CUSTOM, myPattern)

// Create matcher with custom registry
val matcher = IntentMatcher(registry)
```

### Pattern Statistics

```kotlin
val matcher = IntentMatcher()
println(matcher.getStats())
// Output:
// PatternRegistry Stats:
//   Total patterns: 43
//   Categories: 6
//     INTERACTION: 8 patterns
//     VERIFICATION: 20 patterns
//     NAVIGATION: 4 patterns
//     WAIT: 3 patterns
//     APP_LIFECYCLE: 3 patterns
//     FORM_CONTROL: 5 patterns
```

---

## Synonym Registry

TestZen Lite includes a **centralized Synonym Registry** for all NLP synonyms.

### Architecture

```
SynonymRegistry          ← Central storage (30+ sets, 200+ words)
├── SynonymCategory      ← Organized by category
├── SynonymBuilder       ← DSL for definition
└── DefaultSynonyms      ← Built-in synonyms
```

### Synonym Categories

| Category | Description | Examples |
|----------|-------------|----------|
| `ACTION_VERBS` | Interaction action words | click, tap, enter, scroll, swipe |
| `STATE_WORDS` | Element state words | displayed, enabled, checked |
| `PREPOSITIONS` | Connector words | in, on, to, from |
| `DIRECTIONS` | Direction words | up, down, left, right |
| `TIME_UNITS` | Time unit words | seconds, milliseconds, minutes |
| `ELEMENT_TYPES` | Element type words | button, field, checkbox |
| `CUSTOM` | User-defined synonyms | domain-specific terms |

### Using the Synonym Registry

```kotlin
import com.testzen.core.nlp.synonyms.*

// Get default registry with all built-in synonyms
val registry = SynonymRegistry.default()

// Get synonyms by name
val clickWords = registry.get("CLICK")  // ["click", "tap", "press", ...]

// Check if word matches
if (registry.matches("tap", "CLICK")) {
    // Handle click action
}

// Check if text contains any synonym
if (registry.containsAny("tap on the button", "CLICK")) {
    // Handle click intent
}

// Utility functions
val duration = registry.parseDuration("wait 5 seconds")  // 5000 (ms)
val direction = registry.normalizeDirection("scroll upwards")  // "up"

// Get by category
val actionVerbs = registry.getByCategory(SynonymCategory.ACTION_VERBS)
```

### Defining Custom Synonyms

```kotlin
import com.testzen.core.nlp.synonyms.*

// Register custom synonyms
val registry = SynonymRegistry.default()

registry.register(SynonymCategory.CUSTOM) {
    synonymSet("DOMAIN_TERMS") {
        words("checkout", "purchase", "buy", "order")
        description("E-commerce action terms")
    }
    synonymSet("LOGIN_TERMS") {
        words("signin", "sign-in", "authenticate", "log in")
        description("Authentication terms")
    }
}
```

### Synonym Statistics

```kotlin
val registry = SynonymRegistry.default()
println(registry.getStats())
// Output:
// SynonymRegistry Stats:
//   Total sets: 30+
//   Total words: 200+
//   Categories: 6
```

---

## Verification Engine

TestZen Lite includes a **comprehensive verification engine** with 42+ verification types.

### Architecture

```
VerificationEngine       ← Main orchestrator & fluent API
├── ElementVerifier      ← Element state assertions
├── TextVerifier         ← Text-based assertions
├── VerificationMatcher  ← Comparison operations
└── registry/
    ├── VerificationRegistry   ← Central storage
    ├── VerificationBuilder    ← DSL for definition
    └── DefaultVerifications   ← Built-in definitions
```

### All 42+ Verification Types

```kotlin
enum class VerificationType {
    // Presence/Visibility (4)
    DISPLAYED, NOT_DISPLAYED, EXISTS, NOT_EXISTS,

    // Element State (8)
    ENABLED, DISABLED, SELECTED, NOT_SELECTED,
    CHECKED, NOT_CHECKED, FOCUSED, NOT_FOCUSED,

    // Text (8)
    TEXT_EQUALS, TEXT_CONTAINS, TEXT_STARTS_WITH, TEXT_ENDS_WITH,
    TEXT_MATCHES_REGEX, TEXT_NOT_CONTAINS, TEXT_IS_EMPTY, TEXT_IS_NOT_EMPTY,

    // Attribute (4)
    ATTRIBUTE_EQUALS, ATTRIBUTE_CONTAINS, ATTRIBUTE_EXISTS, ATTRIBUTE_NOT_EXISTS,

    // CSS Property (2)
    CSS_PROPERTY_EQUALS, CSS_PROPERTY_CONTAINS,

    // Count/Numeric (6)
    COUNT_EQUALS, COUNT_GREATER_THAN, COUNT_LESS_THAN,
    COUNT_GREATER_OR_EQUAL, COUNT_LESS_OR_EQUAL, COUNT_BETWEEN,

    // Collection (5)
    ALL_DISPLAYED, ANY_DISPLAYED, NONE_DISPLAYED,
    ALL_CONTAIN_TEXT, ANY_CONTAINS_TEXT,

    // Value Comparison (4)
    VALUE_EQUALS, VALUE_NOT_EQUALS, VALUE_GREATER_THAN, VALUE_LESS_THAN,

    // Page/Screen (5)
    PAGE_TITLE_EQUALS, PAGE_TITLE_CONTAINS, URL_EQUALS, URL_CONTAINS,
    PAGE_SOURCE_CONTAINS,

    // Custom (1)
    CUSTOM
}
```

### Comparison Operators

```kotlin
enum class ComparisonOperator {
    EQUALS, NOT_EQUALS, CONTAINS, NOT_CONTAINS,
    STARTS_WITH, ENDS_WITH, MATCHES_REGEX,
    GREATER_THAN, LESS_THAN, GREATER_OR_EQUAL, LESS_OR_EQUAL, BETWEEN,
    IS_EMPTY, IS_NOT_EMPTY, IS_NULL, IS_NOT_NULL, IS_TRUE, IS_FALSE
}
```

### Fluent API Usage

```kotlin
import com.testzen.core.verification.*

val verifier = executor.getVerificationEngine()

// Element verifications
verifier.verify("Login button").isDisplayed()
verifier.verify("Submit").isEnabled()
verifier.verify("Remember me").isChecked()

// Text verifications
verifier.verify("Welcome header").hasText("Welcome, John!")
verifier.verify("Error message").textContains("invalid")
verifier.verify("Email field").textMatches("[a-z]+@[a-z]+\\.[a-z]+")

// Count verifications
verifier.verify("search-results").hasCount(10)
verifier.verify("cart-items").hasCountGreaterThan(0)

// Page-level verifications
verifier.verifyPageTitle("Dashboard")
verifier.verifyUrl("https://example.com/dashboard")
```

### Soft Assertions

```kotlin
// Create soft assertion builder
val softAssert = verifier.softAssert()

// Add multiple verifications (continues even if some fail)
softAssert.verify("Username field").isDisplayed()
softAssert.verify("Password field").isDisplayed()
softAssert.verify("Login button").isEnabled()
softAssert.verify("Error message").isNotDisplayed()

// Assert all at the end
softAssert.assertAll()

// Or get detailed report
val report = softAssert.getReport()
println("Passed: ${report.passedCount}, Failed: ${report.failedCount}")
```

### Verification Results

```kotlin
data class VerificationResult(
    val passed: Boolean,
    val verificationType: VerificationType,
    val target: String,
    val expected: Any?,
    val actual: Any?,
    val message: String,
    val errorDetails: String?,
    val isSoftAssertion: Boolean,
    val durationMs: Long,
    val timestamp: Instant,
    val screenshot: String?,
    val metadata: Map<String, Any>
)
```

### Using the Verification Registry

```kotlin
import com.testzen.core.verification.registry.*

val registry = VerificationRegistry.default()

// Get verification definition
val def = registry.getDefinition(VerificationType.TEXT_CONTAINS)
println("Name: ${def.displayName}")        // "Text Contains"
println("Keywords: ${def.nlpKeywords}")    // ["contains", "includes", ...]

// Get by category
val textVerifications = registry.getByCategory(VerificationCategory.TEXT)

// Find by keywords
val matches = registry.findByKeywords(setOf("contains", "text"))
```

---

## Smart Element Finder

Intelligent element finding system that understands relationships between actions and elements.

### Features

| Feature | Description |
|---------|-------------|
| **Action-Aware Finding** | Finds elements appropriate for the intended action |
| **Parent/Child Traversal** | Traverses DOM to find clickable parents |
| **Spatial Analysis** | Finds input fields by nearby labels |
| **Compound UI Resolution** | Handles date pickers, radio groups, etc. |
| **Scoring System** | Ranks candidates using fuzzy matching |

### Multi-Phase Search Strategy

1. **Direct Search** - Find elements matching target text
2. **Parent Traversal** - Find clickable parent if element isn't actionable
3. **Child Traversal** - Search containers for actionable children
4. **Spatial Search** - Find elements near labels
5. **Compound Resolution** - Handle multi-element UI patterns
6. **Fallback** - Relaxed matching as last resort

### Action Types

| Action | Required Traits | Search Strategy |
|--------|-----------------|-----------------|
| `CLICK` | Clickable | Parent traversal |
| `ENTER_TEXT` | Editable | Child + Spatial |
| `SCROLL` | Scrollable | Direct |
| `CHECK` | Checkable | Spatial search |
| `SELECT` | Dropdown | Child + Spatial |

### Programmatic Usage

```kotlin
import com.testzen.core.locator.smart.*

val smartFinder = SmartElementFinder(
    driver = driver,
    platform = Platform.ANDROID,
    config = SmartFinderConfig(
        minimumScore = 0.4,
        enableSpatialSearch = true,
        enableCompoundResolution = true
    )
)

// Find clickable element
val result = smartFinder.findClickable("Submit")
if (result.success && result is FindResult.SingleElement) {
    result.element.click()
}

// Find input field by label
val inputResult = smartFinder.findInputField("Email")

// Handle compound actions (e.g., date picker)
val dateResult = smartFinder.findElement("date jan 21 2026", ActionType.SELECT)
```

### Element Scoring

| Factor | Weight | Description |
|--------|--------|-------------|
| Text Match | 40% | Fuzzy text similarity |
| Action Compatibility | 30% | Does element support the action? |
| Spatial Relevance | 20% | Proximity to reference elements |
| Visibility | 10% | Is element visible and enabled? |

---

## Self-Healing Locators

Smart locators that automatically try fallback strategies when primary locator fails.

### How It Works

1. **Multiple Strategies**: Generate multiple locator strategies per element
2. **Automatic Fallback**: Try alternatives when primary fails
3. **Learning**: Cache successful locators for future runs
4. **Persistent Cache**: Cache persists across sessions

### Configuration

```yaml
elements:
  self_healing_enabled: true
  max_fallback_attempts: 5
  locator_cache_directory: .testzen-cache
  learn_from_healing: true
```

### Supported Locator Types

| Platform | Locator Types |
|----------|---------------|
| Android | Accessibility ID, Resource ID, Text, Content Description, XPath |
| iOS | Accessibility ID, Name, Label, XPath |
| Web | ID, CSS Selector, XPath, Link Text, Aria Label, Data Attributes |

### Page Object Repository (Large Projects)

```
page-objects/
├── _index.json           # Page registry
├── login_page.json       # Login page elements
├── home_page.json        # Home page elements
└── checkout/
    ├── cart_page.json
    └── payment_page.json
```

```kotlin
val repo = PageObjectRepository("./page-objects")
val loginPage = repo.getPage("login_page")

loginPage.addElement("email_field", listOf(
    Locator(LocatorType.ACCESSIBILITY_ID, "email_input", 0.95),
    Locator(LocatorType.RESOURCE_ID, "com.myapp:id/email", 0.9)
))
```

---

## CLI Usage

### Run Command

```bash
# Auto-detect platform from folder
java -jar testzen-cli.jar run --tests ./tests/android
java -jar testzen-cli.jar run --tests ./tests/ios --device iPhone-14-Pro

# Web with options
java -jar testzen-cli.jar run --tests ./tests/web --headless --browser chrome

# With configuration file
java -jar testzen-cli.jar run --tests ./tests --config ./config.yaml --output ./results
```

### Validate Command

```bash
java -jar testzen-cli.jar validate --tests ./tests/android
```

### Version Command

```bash
java -jar testzen-cli.jar version
```

---

## Configuration Reference

### All 27 Configuration Options

#### Timeout Settings

| Option | Default | Description |
|--------|---------|-------------|
| `implicitWait` | 20 | Implicit wait in seconds |
| `explicitWait` | 30 | Explicit wait in seconds |
| `pageLoadTimeout` | 120 | Page load timeout in seconds |
| `actionTimeout` | 60 | Action timeout in seconds |

#### Retry Settings

| Option | Default | Description |
|--------|---------|-------------|
| `retryFailedSteps` | 2 | Number of retries for failed steps |
| `retryDelayMs` | 1000 | Delay between retries in ms |

#### Screenshot Settings

| Option | Default | Description |
|--------|---------|-------------|
| `screenshotOnFailure` | true | Capture on failure |
| `screenshotOnSuccess` | false | Capture on success |
| `screenshotDirectory` | ./screenshots | Screenshot output directory |

#### Element Finding Settings

| Option | Default | Description |
|--------|---------|-------------|
| `fuzzyMatchEnabled` | true | Enable fuzzy text matching |
| `fuzzyMatchThreshold` | 0.8 | Minimum fuzzy match score |
| `scrollToFindElement` | true | Scroll to find elements |
| `maxScrollAttempts` | 5 | Maximum scroll attempts |

#### Self-Healing Settings

| Option | Default | Description |
|--------|---------|-------------|
| `selfHealingEnabled` | true | Enable self-healing locators |
| `maxFallbackAttempts` | 5 | Maximum fallback strategies |
| `locatorCacheDirectory` | .testzen-cache | Cache directory |
| `learnFromHealing` | true | Learn from healed elements |

#### Page Object Repository Settings

| Option | Default | Description |
|--------|---------|-------------|
| `usePageObjectRepository` | false | Enable page object mode |
| `pageObjectsDirectory` | ./page-objects | Page objects directory |
| `cacheMode` | READ_WRITE | READ_WRITE, READ_ONLY, DISABLED |
| `autoSavePageObjects` | false | Auto-save discovered elements |

#### Smart Find Settings

| Option | Default | Description |
|--------|---------|-------------|
| `smartFindEnabled` | true | Enable smart element finding |
| `smartFindMinimumScore` | 0.4 | Minimum score for matches |
| `elementTimeoutMs` | 10000 | Element search timeout |

#### Logging Settings

| Option | Default | Description |
|--------|---------|-------------|
| `logLevel` | INFO | Log level |
| `logToFile` | false | Write logs to file |
| `logDirectory` | ./logs | Log output directory |

#### Output Settings

| Option | Default | Description |
|--------|---------|-------------|
| `outputFormat` | json | Output format |
| `outputDirectory` | ./results | Results directory |

#### Appium/Selenium Settings

| Option | Default | Description |
|--------|---------|-------------|
| `appiumHost` | 127.0.0.1 | Appium server host |
| `appiumPort` | 4723 | Appium server port |
| `browserType` | chrome | Browser type |
| `headless` | false | Run browser headless |

### YAML Configuration File

```yaml
execution:
  implicit_wait: 20
  explicit_wait: 30
  page_load_timeout: 120
  action_timeout: 60
  retry_failed_steps: 2
  retry_delay_ms: 1000
  screenshot_on_failure: true
  screenshot_on_success: false
  screenshot_directory: ./screenshots

elements:
  fuzzy_match_enabled: true
  fuzzy_match_threshold: 0.8
  scroll_to_find: true
  max_scroll_attempts: 5
  self_healing_enabled: true
  max_fallback_attempts: 5
  locator_cache_directory: .testzen-cache
  learn_from_healing: true
  use_page_object_repository: false
  page_objects_directory: ./page-objects
  cache_mode: READ_WRITE
  auto_save_page_objects: false
  smart_find_enabled: true
  smart_find_minimum_score: 0.4
  element_timeout_ms: 10000

logging:
  level: INFO
  log_to_file: false
  directory: ./logs

output:
  format: json
  directory: ./results

appium:
  host: 127.0.0.1
  port: 4723
  browser: chrome
  headless: false
```

---

## Enterprise Test Reporting

TestZen Lite includes a **comprehensive enterprise-grade reporting system** designed to handle 100s or 1000s of tests organized by modules, features, and stories.

### Report Hierarchy

```
TestExecutionReport              ← Overall execution summary
└── ModuleResult                 ← LOB/Module level (e.g., "Payments", "Auth")
    └── FeatureResult            ← Feature level (e.g., "User Login")
        └── StoryResult          ← User story level (e.g., "US-123")
            └── TestCaseResult   ← Individual test
                └── StepResult   ← Each step with screenshots
                    ├── screenshotBefore
                    └── screenshotAfter
```

### Key Features

| Feature | Description |
|---------|-------------|
| **Hierarchical Organization** | Module → Feature → Story → Test → Step |
| **Before/After Screenshots** | Automatic screenshot capture per step |
| **Failure Analysis** | Top failure reasons, affected tests |
| **Multiple Formats** | HTML, JSON, JUnit XML |
| **Drill-Down Reports** | From summary to individual step details |
| **CI/CD Integration** | JUnit XML for Jenkins, GitHub Actions, etc. |
| **Pass Rate Aggregation** | At every level of hierarchy |
| **Duration Tracking** | Per step, test, story, feature, module |

### Report Formats

| Format | Use Case | File |
|--------|----------|------|
| **HTML** | Interactive browser viewing | `report.html` |
| **JSON (Full)** | Programmatic access, archival | `report.json` |
| **JSON (Summary)** | Dashboard integration | `summary.json` |
| **JUnit XML** | CI/CD pipelines | `junit.xml` |

### Test Organization in YAML

Organize tests with module/feature/story metadata:

```yaml
# tests/android/payments/checkout_test.yaml
test_id: checkout_001
name: "Complete checkout flow"
description: "Verify user can complete purchase"

# Hierarchy metadata
module: payments
module_name: "Payment Processing"
feature: checkout
feature_name: "Checkout Flow"
story: US-456
story_name: "User can complete checkout"

tags:
  - smoke
  - critical
  - payments

steps:
  - "Launch the app"
  - "Click 'Add to Cart'"
  - "Click 'Checkout'"
  - "Enter '4111111111111111' in 'Card Number'"
  - "Click 'Pay Now'"
  - "Verify 'Order Confirmed' is displayed"
```

### Using ReportManager

```kotlin
import com.testzen.core.reporting.*

// Create report manager
val reportManager = ReportManager(ReportConfig(
    outputDirectory = "./test-reports",
    screenshotDirectory = "./test-reports/screenshots",
    captureScreenshots = true
))

// Start execution
reportManager.startExecution(
    name = "Regression Suite - Sprint 42",
    environment = "staging",
    buildInfo = BuildInfo(
        buildNumber = "1234",
        branch = "main",
        commit = "abc123"
    )
)

// Start a test case with hierarchy
val testContext = reportManager.startTestCase(
    testId = "TC001",
    name = "Login Test",
    moduleId = "auth",
    moduleName = "Authentication",
    featureId = "login",
    featureName = "User Login",
    storyId = "US-123",
    storyName = "User can login with email",
    platform = TestPlatform.ANDROID,
    deviceInfo = "Pixel 6 - Android 13"
)

// Record steps with screenshots
testContext.startStep(1, "Click Login button", intent = "CLICK", target = "Login")
testContext.captureScreenshot(driver, ScreenshotType.BEFORE_STEP)
// ... execute click ...
testContext.captureScreenshot(driver, ScreenshotType.AFTER_STEP)
testContext.endStep(TestStatus.PASSED)

testContext.startStep(2, "Enter email", intent = "ENTER_TEXT", target = "Email")
testContext.captureScreenshot(driver, ScreenshotType.BEFORE_STEP)
// ... execute enter text ...
testContext.captureScreenshot(driver, ScreenshotType.AFTER_STEP)
testContext.endStep(TestStatus.PASSED)

// Handle failures
testContext.startStep(3, "Verify welcome message")
testContext.captureScreenshot(driver, ScreenshotType.BEFORE_STEP)
// ... verification fails ...
testContext.captureScreenshot(driver, ScreenshotType.ON_FAILURE)
testContext.failStep(
    errorMessage = "Expected 'Welcome' but found 'Error'",
    expectedValue = "Welcome",
    actualValue = "Error"
)

// End test case
testContext.end(TestStatus.FAILED)

// Generate all reports
val report = reportManager.endExecution()
reportManager.generateAllReports("./test-reports")
```

### Generated HTML Report

The HTML report includes:

1. **Executive Summary Dashboard**
   - Total/Passed/Failed/Skipped counts
   - Overall pass rate with progress bar
   - Module/Feature/Story/Step aggregations
   - Duration and timing info

2. **Module Cards**
   - Visual cards for each module
   - Pass/fail counts and rate
   - Click to drill down

3. **Hierarchical Drill-Down**
   - Expandable Module → Feature → Story → Test
   - Status badges at every level
   - Duration tracking

4. **Test Case Details**
   - Step-by-step execution table
   - Before/after screenshots per step
   - Error messages and stack traces
   - Healed locator indicators

5. **Failure Analysis**
   - Top failure reasons with counts
   - Failures by module breakdown
   - Affected test lists

6. **Filtering & Search**
   - Filter by module
   - Filter by status (passed/failed/skipped)
   - Search by test name or ID

### JSON Report Structure

```json
{
  "reportId": "uuid",
  "name": "Regression Suite",
  "status": "FAILED",
  "environment": "staging",

  "summary": {
    "tests": { "total": 150, "passed": 142, "failed": 8, "passRate": 94.67 },
    "steps": { "total": 1250, "passed": 1240, "failed": 10 },
    "modules": { "total": 5, "passed": 4 },
    "features": { "total": 20, "passed": 18 },
    "stories": { "total": 45, "passed": 43 }
  },

  "modules": [
    {
      "moduleId": "payments",
      "name": "Payment Processing",
      "status": "PASSED",
      "statistics": { "totalTests": 30, "passedTests": 30, "passRate": 100.0 },
      "features": [
        {
          "featureId": "checkout",
          "name": "Checkout Flow",
          "stories": [
            {
              "storyId": "US-456",
              "name": "User can complete checkout",
              "testCases": [
                {
                  "testId": "checkout_001",
                  "name": "Complete checkout flow",
                  "status": "PASSED",
                  "steps": [
                    {
                      "stepNumber": 1,
                      "instruction": "Launch the app",
                      "status": "PASSED",
                      "durationMs": 1500,
                      "screenshotBefore": { "filePath": "..." },
                      "screenshotAfter": { "filePath": "..." }
                    }
                  ]
                }
              ]
            }
          ]
        }
      ]
    }
  ],

  "failureAnalysis": {
    "topReasons": [
      { "message": "Element not found", "count": 5, "affectedTests": ["..."] }
    ],
    "failuresByModule": { "auth": 3, "payments": 0 }
  }
}
```

### CI/CD Integration

#### Jenkins Pipeline

```groovy
pipeline {
    stages {
        stage('Test') {
            steps {
                sh './gradlew test'
            }
            post {
                always {
                    // Publish HTML report
                    publishHTML([
                        reportDir: 'test-reports',
                        reportFiles: 'report.html',
                        reportName: 'TestZen Report'
                    ])
                    // Publish JUnit results
                    junit 'test-reports/junit.xml'
                }
            }
        }
    }
}
```

#### GitHub Actions

```yaml
- name: Run Tests
  run: ./gradlew test

- name: Upload Test Report
  uses: actions/upload-artifact@v3
  with:
    name: test-report
    path: test-reports/

- name: Publish Test Results
  uses: EnricoMi/publish-unit-test-result-action@v2
  with:
    files: test-reports/junit.xml
```

### Report Configuration

```kotlin
val reportConfig = ReportConfig(
    // Output directories
    outputDirectory = "./test-reports",
    screenshotDirectory = "./test-reports/screenshots",

    // Screenshot settings
    captureScreenshots = true,
    screenshotBeforeStep = true,
    screenshotAfterStep = true,
    screenshotOnFailure = true,

    // HTML report settings
    htmlConfig = HtmlReportConfig(
        embedScreenshots = false,  // Link vs embed base64
        includeCharts = true,
        theme = "light"
    ),

    // JSON report settings
    jsonConfig = JsonReportConfig(
        includeSteps = true,
        includeScreenshots = true,
        prettyPrint = true
    )
)
```

### Aggregated Statistics

At every level, you get:

| Statistic | Description |
|-----------|-------------|
| `totalTests` | Total test count |
| `passedTests` | Passed test count |
| `failedTests` | Failed test count |
| `skippedTests` | Skipped test count |
| `passRate` | Pass percentage (0-100) |
| `totalSteps` | Total steps executed |
| `passedSteps` | Steps that passed |
| `durationMs` | Total duration |
| `startTime` | Execution start |
| `endTime` | Execution end |

---

## Intelligent Stability System

TestZen Lite includes a **comprehensive stability system** designed to handle real-world scenarios like network latency, slow page rendering, animations, and dynamic content loading.

### Why Stability Matters

In real-world test automation, flaky tests often result from:
- Elements still animating/moving when clicked
- Network latency causing delayed content loading
- DOM mutations during element interaction
- Stale element references after page updates
- Scrolling before content has settled

The Intelligent Stability System solves these issues automatically.

### Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                     StabilityOrchestrator                           │
│  Coordinates all stability components for unified resilient ops    │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
     ┌───────────────────────────┼───────────────────────────┐
     │                           │                           │
     ▼                           ▼                           ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────────┐
│ElementStability │    │SmartScrollStrat.│    │PageLoadIntelligence │
│    Waiter       │    │                 │    │                     │
├─────────────────┤    ├─────────────────┤    ├─────────────────────┤
│• Position check │    │• End detection  │    │• Network idle wait  │
│• Size stability │    │• Momentum settle│    │• DOM stability wait │
│• Animation wait │    │• Dynamic content│    │• Lazy load detection│
│• Stale recovery │    │• Viewport calc  │    │• AJAX completion    │
└─────────────────┘    └─────────────────┘    └─────────────────────┘
                                 │
                                 ▼
                    ┌─────────────────────┐
                    │  RetryWithBackoff   │
                    ├─────────────────────┤
                    │• Exponential backoff│
                    │• Jitter for spread  │
                    │• Transient detection│
                    │• Progressive timeout│
                    └─────────────────────┘
```

### Key Components

| Component | Purpose |
|-----------|---------|
| `StabilityRegistry` | Centralized registry for configurations and adapters |
| `StabilityOrchestrator` | Coordinates all stability components |
| `ElementStabilityWaiter` | Waits for element position/size to stabilize |
| `SmartScrollStrategy` | Intelligent scrolling with end-of-content detection |
| `PageLoadIntelligence` | Handles network latency and page rendering |
| `RetryWithBackoff` | Exponential backoff retry for transient failures |
| `PlatformStabilityAdapter` | Interface for platform-specific operations |
| `StabilityConfig` | Fine-grained configuration |

### Cross-Platform Support

The stability system works identically across Android, iOS, and Web through platform adapters:

| Platform | Adapter | Detection Technique |
|----------|---------|---------------------|
| **Web** | `WebStabilityAdapter` | JavaScript DOM/CSS/Network monitoring |
| **Android** | `MobileStabilityAdapter` | Element attribute monitoring, XPath |
| **iOS** | `MobileStabilityAdapter` | Element attribute monitoring, XPath |

```kotlin
// Create platform-specific orchestrator
val orchestrator = StabilityOrchestrator.forPlatform(Platform.ANDROID)

// Or use the registry
val registry = StabilityRegistry.default()
val iosOrchestrator = registry.getOrchestrator(Platform.IOS)
val webOrchestrator = registry.getOrchestrator(Platform.WEB)
```

### Stability DSL Builder

Configure stability with a clean DSL:

```kotlin
import com.testzen.core.stability.registry.*

val config = stabilityConfig {
    elementStability {
        enabled = true
        timeoutMs = 5000
        stableReadingsRequired = 3
        positionTolerancePx = 2
    }
    scrolling {
        enabled = true
        maxAttempts = 10
        settleTimeMs = 300
        detectMomentum = true
    }
    pageLoad {
        enabled = true
        networkIdle = true    // Web only
        domStability = true   // Web only
    }
    retry {
        exponentialBackoff = true
        maxRetries = 3
        initialDelayMs = 100
    }
    network(NetworkProfile.SLOW)  // Adjust for network conditions
}

// Or use presets
val fastConfig = stabilityConfig(StabilityPreset.FAST)
val robustConfig = stabilityConfig(StabilityPreset.ROBUST)
val ciConfig = stabilityConfig(StabilityPreset.CI)
```

### Using the Stability Registry

```kotlin
import com.testzen.core.stability.registry.*

// Get default registry with all built-in strategies
val registry = StabilityRegistry.default()

// Get platform-specific adapter
val adapter = registry.getAdapter(Platform.ANDROID)

// Get orchestrator for a platform
val orchestrator = registry.getOrchestrator(Platform.IOS, config)

// Register custom configuration
registry.register(StabilityCategory.CUSTOM, "my_slow_network") {
    network(NetworkProfile.VERY_SLOW)
    retry {
        maxRetries = 5
        initialDelayMs = 500
    }
    elementStability {
        timeoutMs = 8000
    }
}

// Get statistics
println(registry.getStats())
// Output:
// StabilityRegistry Stats:
//   Total configurations: 25
//   Categories: 7
//   Registered platforms: [ANDROID, IOS, WEB]
```

### Element Stability Detection

```kotlin
val stabilityWaiter = ElementStabilityWaiter(config)

// Wait for element to stabilize (position stops changing)
val result = stabilityWaiter.waitForStability(element, timeoutMs = 3000)

when (result) {
    is StabilityResult.Stable -> {
        // Element is stable, safe to interact
        result.element.click()
    }
    is StabilityResult.Unstable -> {
        // Element didn't stabilize within timeout
        println("Element still moving: ${result.reason}")
    }
    is StabilityResult.ElementGone -> {
        // Element disappeared (stale reference)
        println("Element gone: ${result.reason}")
    }
}

// Wait for element to appear AND stabilize
val findResult = stabilityWaiter.waitForElementAndStability(
    findElement = { driver.findElement(By.id("submit")) },
    timeoutMs = 5000
)

// Wait for animations to complete
stabilityWaiter.waitForAnimations(driver, element, timeoutMs = 2000)

// Wait for element to become interactive (displayed + enabled + stable)
if (stabilityWaiter.waitForInteractive(element, driver, timeoutMs = 5000)) {
    element.click()
}
```

### Smart Scrolling

```kotlin
val scrollStrategy = SmartScrollStrategy(config)

// Scroll to find element with end-of-content detection
val result = scrollStrategy.scrollToFind(
    driver = driver,
    findElement = { findElement("Product XYZ") },
    direction = ScrollDirection.DOWN,
    maxAttempts = 10
)

when (result) {
    is ScrollResult.ElementFound -> {
        // Element found after ${result.scrollCount} scrolls
        result.element.click()
    }
    is ScrollResult.EndOfContent -> {
        // Reached end of scrollable area, element not found
        println("End of list after ${result.scrollCount} scrolls")
    }
    is ScrollResult.Failure -> {
        println("Max attempts reached: ${result.reason}")
    }
}

// Scroll in both directions (down first, then up)
val biDirectionalResult = scrollStrategy.scrollAllDirectionsToFind(
    driver = driver,
    findElement = { findElement("Rare Item") },
    primaryDirection = ScrollDirection.DOWN
)

// Check element visibility
if (scrollStrategy.isElementPartiallyVisible(driver, element)) {
    scrollStrategy.scrollElementIntoView(driver, element)
}
```

### Page Load Intelligence

```kotlin
val pageIntelligence = PageLoadIntelligence(config)

// Wait for page to be fully ready
val result = pageIntelligence.waitForPageReady(driver, timeoutMs = 30000)

when (result) {
    is WaitResult.Success -> {
        println("Page ready in ${result.waitTimeMs}ms")
        println("State: ${result.pageState}")
    }
    is WaitResult.Timeout -> {
        println("Page not fully ready: ${result.reason}")
    }
}

// Wait for network to become idle
pageIntelligence.waitForNetworkIdle(driver, timeoutMs = 5000)

// Wait for DOM to stabilize (no mutations)
pageIntelligence.waitForDomStability(driver, timeoutMs = 3000)

// Wait for loading indicators to disappear
pageIntelligence.waitForLoadingIndicatorsGone(
    driver,
    selectors = listOf(".spinner", ".loading", "[class*='skeleton']"),
    timeoutMs = 10000
)

// Check current page state
val state = pageIntelligence.getCurrentPageState(driver)
if (state.isFullyReady) {
    // Safe to proceed
}
```

### Exponential Backoff Retry

```kotlin
val retry = RetryWithBackoff(config)

// Execute with automatic retry on transient failures
val result = retry.execute(
    operation = { ctx ->
        println("Attempt ${ctx.attemptNumber}/${ctx.totalAttempts}")
        driver.findElement(By.id("dynamic-element")).click()
    },
    maxAttempts = 5,
    timeoutMs = 30000
)

when (result) {
    is RetryResult.Success -> {
        println("Succeeded on attempt ${result.attempts}")
    }
    is RetryResult.Failure -> {
        println("Failed after ${result.attempts} attempts: ${result.lastException}")
    }
}

// Execute with automatic stale element recovery
val elementResult = retry.executeWithStaleRecovery(
    refind = { driver.findElement(By.id("item")) },
    operation = { element -> element.text }
)

// Wait for condition with polling
val conditionMet = retry.waitForCondition(
    condition = { driver.findElement(By.id("status")).text == "Complete" },
    timeoutMs = 10000,
    pollIntervalMs = 200,
    description = "status to be Complete"
)

// Simple retry extension function
val text = retryWithBackoff {
    driver.findElement(By.id("flaky-element")).text
}
```

### Stability Orchestrator (Recommended)

```kotlin
val orchestrator = StabilityOrchestrator(config, Platform.ANDROID)

// Find element with full stability handling (page wait + scroll + stability)
val findResult = orchestrator.findElementStable(
    driver = driver,
    description = "Login button",
    timeoutMs = 15000,
    scrollEnabled = true,
    finder = { driver.findElement(By.id("login_btn")) }
)

when (findResult) {
    is FindResult.Found -> {
        // Element found, stable, and ready
        orchestrator.clickStable(driver, findResult.element)
    }
    is FindResult.NotFound -> {
        println("Not found: ${findResult.reason}")
    }
}

// Perform click with full stability handling
orchestrator.clickStable(
    driver = driver,
    element = element,
    waitForPageAfter = true  // Wait for page to settle after click
)

// Enter text with stability
orchestrator.enterTextStable(
    driver = driver,
    element = inputField,
    text = "hello@example.com",
    clearFirst = true
)

// Execute any operation with stability wrapper
val result = orchestrator.executeStable(
    driver = driver,
    description = "Submit form",
    timeoutMs = 10000,
    waitForPageBefore = true,
    waitForPageAfter = true
) {
    submitButton.click()
}

// Verify with intelligent waiting
val verified = orchestrator.verifyWithWait(
    driver = driver,
    finder = { driver.findElement(By.id("success")) },
    verification = { it.isDisplayed && it.text.contains("Success") },
    timeoutMs = 15000,
    description = "success message"
)
```

### Stability Configuration

```kotlin
// Default configuration
val defaultConfig = StabilityConfig.default()

// Fast configuration (for stable environments)
val fastConfig = StabilityConfig.fast()

// Robust configuration (for slow/flaky environments)
val robustConfig = StabilityConfig.robust()

// CI/CD configuration (consistent timing)
val ciConfig = StabilityConfig.ci()

// Custom configuration
val customConfig = StabilityConfig(
    // Element stability
    elementStabilityEnabled = true,
    stabilityTimeoutMs = 3000,
    stableReadingsRequired = 3,
    positionTolerancePx = 2,

    // Smart scrolling
    smartScrollEnabled = true,
    maxScrollAttempts = 10,
    scrollSettleTimeMs = 300,
    endOfContentSnapshots = 2,
    detectScrollMomentum = true,

    // Page load intelligence
    pageLoadIntelligenceEnabled = true,
    waitForNetworkIdle = true,
    networkIdleThresholdMs = 500,
    waitForDomStable = true,

    // Retry with backoff
    exponentialBackoffEnabled = true,
    initialRetryDelayMs = 100,
    maxRetryDelayMs = 5000,
    backoffMultiplier = 2.0,
    retryJitter = 0.1,

    // Network profiles
    networkProfile = StabilityConfig.NetworkProfile.NORMAL,
    adaptiveTimeoutsEnabled = true,

    // Action-specific timeouts
    actionTimeouts = mapOf(
        "CLICK" to 5000,
        "ENTER_TEXT" to 10000,
        "VERIFY" to 15000
    )
)
```

### Network Profiles

| Profile | Multiplier | Use Case |
|---------|------------|----------|
| `FAST` | 0.7x | 5G, WiFi, fast servers |
| `NORMAL` | 1.0x | Standard 4G, broadband |
| `SLOW` | 1.5x | 3G, congested networks |
| `VERY_SLOW` | 2.5x | 2G, high latency |
| `OFFLINE_FIRST` | 0.5x | Apps that work offline |

### Integration with SmartElementFinder

```kotlin
val finder = SmartElementFinder(
    driver = driver,
    platform = Platform.ANDROID,
    config = SmartFinderConfig(
        enableStabilityCheck = true,
        enableSmartScroll = true,
        enablePageLoadWait = true
    ),
    stabilityConfig = StabilityConfig.robust()
)

// Find with automatic stability handling
val result = finder.findElementStable(
    target = "Login",
    action = ActionType.CLICK,
    timeoutMs = 15000
)

// Execute action with stale element recovery
finder.executeOnElementWithRecovery(
    target = "Submit",
    action = ActionType.CLICK
) { element ->
    element.click()
}

// Wait for loading indicator to disappear
finder.waitForElementGone("Loading spinner", timeoutMs = 10000)
```

### Integration with VerificationEngine

```kotlin
val verifier = VerificationEngine(
    driver = driver,
    elementFinder = elementFinder,
    config = testConfig,
    stabilityConfig = StabilityConfig.robust()
)

// Verification with intelligent waiting (recommended for dynamic content)
verifier.verifyWithIntelligentWait("Welcome message").isDisplayed()
verifier.verifyWithIntelligentWait("User name").hasText("John Doe")

// Wait for page to be ready before verification
verifier.waitForPageReady(timeoutMs = 10000)

// Wait for element to appear and stabilize
verifier.waitForElement("Success notification", timeoutMs = 5000)

// Wait for loading indicator to disappear
verifier.waitForElementGone("Loading spinner", timeoutMs = 10000)
```

### Best Practices

1. **Use `findElementStable()` instead of `findElement()`** for dynamic content
2. **Enable smart scrolling** when elements might be below the fold
3. **Use `StabilityConfig.robust()`** for CI/CD pipelines
4. **Wait for page ready** after navigation or form submission
5. **Use stale element recovery** for single-page applications
6. **Configure action-specific timeouts** based on operation complexity
7. **Monitor adaptive timeout adjustments** to understand environment performance

---

## API Reference

### Core Classes

| Class | Description |
|-------|-------------|
| `TestZenRunner` | Main entry point for test execution |
| `TestLoader` | YAML test loader with platform detection |
| `TestZenConfig` | Configuration container |
| `TestExecutor` | Test execution engine |
| `InstructionParser` | NLP-powered instruction parsing |
| `InstructionExecutor` | Action execution |

### NLP Classes

| Class | Description |
|-------|-------------|
| `NLPEngine` | Main NLP orchestrator |
| `IntentMatcher` | Pattern-based intent classification |
| `EntityExtractor` | Entity extraction from text |
| `PatternRegistry` | Central pattern storage |
| `SynonymRegistry` | Central synonym storage |

### Verification Classes

| Class | Description |
|-------|-------------|
| `VerificationEngine` | Main verification orchestrator |
| `ElementVerifier` | Element state assertions |
| `TextVerifier` | Text-based assertions |
| `VerificationRegistry` | Central verification storage |

### Locator Classes

| Class | Description |
|-------|-------------|
| `SmartElementFinder` | Intelligent element finding |
| `SelfHealingLocator` | Self-healing locator system |
| `LocatorCache` | Simple persistent cache |
| `PageObjectRepository` | Page-based locator storage |
| `SpatialAnalyzer` | Spatial relationship analysis |
| `ElementScorer` | Element scoring system |

### Reporting Classes

| Class | Description |
|-------|-------------|
| `ReportManager` | Central report collection & generation |
| `TestExecutionReport` | Top-level execution report |
| `ModuleResult` | Module/LOB level results |
| `FeatureResult` | Feature level results |
| `StoryResult` | User story level results |
| `TestCaseResult` | Individual test results |
| `StepResult` | Step-by-step results with screenshots |
| `HtmlReportGenerator` | Interactive HTML report generator |

### Stability Classes

| Class | Description |
|-------|-------------|
| `StabilityRegistry` | Centralized registry for configurations & adapters |
| `StabilityOrchestrator` | Coordinates all stability features |
| `StabilityConfig` | Configuration for stability system |
| `StabilityBuilder` | DSL builder for stability configuration |
| `PlatformStabilityAdapter` | Interface for platform-specific stability |
| `WebStabilityAdapter` | Web (JavaScript-based) stability adapter |
| `MobileStabilityAdapter` | Android/iOS (Appium-based) stability adapter |
| `DefaultStabilities` | Built-in stability configurations |
| `ElementStabilityWaiter` | Waits for element position/size stability |
| `SmartScrollStrategy` | Intelligent scrolling with end detection |
| `PageLoadIntelligence` | Network latency and page load handling |
| `RetryWithBackoff` | Exponential backoff retry mechanism |

---

## Project Structure

```
testzen-lite/
├── README.md                         # This documentation
│
├── testzen-core/                     # Core library (SDK)
│   ├── build.gradle.kts
│   ├── config/                       # Configuration templates
│   └── src/main/kotlin/com/testzen/core/
│       │
│       ├── TestZenRunner.kt          # Main entry point
│       ├── TestLoader.kt             # YAML loader with platform detection
│       │
│       ├── config/
│       │   └── TestZenConfig.kt      # Configuration (27 options)
│       │
│       ├── model/
│       │   ├── TestCase.kt           # Test case model
│       │   └── TestResult.kt         # Test result model
│       │
│       ├── nlp/                      # Natural Language Processing
│       │   ├── Intent.kt             # 37 intent types + entities
│       │   ├── NLPEngine.kt          # Main NLP orchestrator
│       │   ├── IntentMatcher.kt      # Pattern-based classification
│       │   ├── EntityExtractor.kt    # Entity extraction
│       │   │
│       │   ├── patterns/             # Centralized pattern management
│       │   │   ├── PatternRegistry.kt    # Central storage
│       │   │   ├── PatternBuilder.kt     # DSL for definition
│       │   │   ├── PatternFragments.kt   # Reusable regex components
│       │   │   └── DefaultPatterns.kt    # 43 built-in patterns
│       │   │
│       │   └── synonyms/             # Centralized synonym management
│       │       ├── SynonymRegistry.kt    # Central storage + utilities
│       │       ├── SynonymBuilder.kt     # DSL for definition
│       │       └── DefaultSynonyms.kt    # 30+ built-in synonym sets
│       │
│       ├── execution/                # Test execution
│       │   ├── TestExecutor.kt       # Test execution engine
│       │   ├── InstructionParser.kt  # NLP-powered parsing
│       │   ├── InstructionExecutor.kt# Action execution
│       │   ├── ElementFinder.kt      # Basic element location
│       │   └── GestureHandler.kt     # Touch gestures
│       │
│       ├── verification/             # Verification engine
│       │   ├── VerificationTypes.kt  # 42+ types, results, context
│       │   ├── VerificationEngine.kt # Main orchestrator & fluent API
│       │   ├── VerificationMatcher.kt# Comparison operations
│       │   ├── ElementVerifier.kt    # Element state assertions
│       │   ├── TextVerifier.kt       # Text-based assertions
│       │   │
│       │   └── registry/             # Centralized verification management
│       │       ├── VerificationRegistry.kt   # Central storage
│       │       ├── VerificationBuilder.kt    # DSL for definition
│       │       └── DefaultVerifications.kt   # 42 built-in definitions
│       │
│       ├── locator/                  # Element location & self-healing
│       │   ├── LocatorStrategy.kt    # Locator types and models
│       │   ├── LocatorStorage.kt     # Storage interface
│       │   ├── LocatorCache.kt       # Simple persistent cache
│       │   ├── LocatorGenerator.kt   # Platform-specific generation
│       │   ├── ElementAttributeExtractor.kt  # Learning from elements
│       │   ├── SelfHealingLocator.kt # Smart locator with fallbacks
│       │   ├── PageObjectRepository.kt       # Page-based storage
│       │   ├── PageObjectRepositoryAdapter.kt# Storage adapter
│       │   │
│       │   └── smart/                # Intelligent element finder
│       │       ├── ActionType.kt         # Action types & requirements
│       │       ├── ElementCandidate.kt   # Scored element wrapper
│       │       ├── ElementScorer.kt      # Text match & action scoring
│       │       ├── SpatialAnalyzer.kt    # Spatial relationship analysis
│       │       ├── PlatformElementAdapter.kt  # Platform trait extraction
│       │       ├── CompoundElementResolver.kt # Complex UI patterns
│       │       └── SmartElementFinder.kt # Main orchestrator
│       │
│       ├── reporting/                # Enterprise reporting
│       │   ├── TestResultModels.kt       # Result hierarchy models
│       │   ├── TestExecutionReport.kt    # Top-level report aggregation
│       │   ├── ReportManager.kt          # Report collection & generation
│       │   ├── HtmlReportGenerator.kt    # Interactive HTML reports
│       │   └── JsonReportGenerator.kt    # JSON & JUnit XML reports
│       │
│       ├── stability/                # Intelligent stability system
│       │   ├── StabilityConfig.kt        # Configuration & network profiles
│       │   ├── StabilityOrchestrator.kt  # Central coordinator
│       │   ├── ElementStabilityWaiter.kt # Element position/size stability
│       │   ├── SmartScrollStrategy.kt    # Scroll with end detection
│       │   ├── PageLoadIntelligence.kt   # Network & DOM stability
│       │   ├── RetryWithBackoff.kt       # Exponential backoff retry
│       │   │
│       │   └── registry/                 # Centralized stability management
│       │       ├── StabilityRegistry.kt      # Central registry
│       │       ├── StabilityBuilder.kt       # DSL for configuration
│       │       ├── PlatformStabilityAdapter.kt # Platform interface
│       │       ├── WebStabilityAdapter.kt    # Web (JavaScript-based)
│       │       ├── MobileStabilityAdapter.kt # Android/iOS (Appium-based)
│       │       └── DefaultStabilities.kt     # Built-in configurations
│       │
│       └── platform/
│           └── PlatformDriverFactory.kt  # Driver creation
│
└── testzen-cli/                      # CLI wrapper
    ├── build.gradle.kts
    └── src/main/kotlin/com/testzen/cli/
        └── Main.kt                   # CLI with auto-detection
```

### File Count Summary

| Component | Files | Description |
|-----------|-------|-------------|
| Core | 5 | Runner, loader, config, models |
| NLP | 8 | Engine, matcher, patterns, synonyms |
| Execution | 5 | Executor, parser, gestures |
| Verification | 8 | Engine, matchers, registry |
| Locator | 14 | Self-healing, smart finder |
| Reporting | 5 | Report models, generators, manager |
| Stability | 12 | Stability system + registry + adapters |
| Platform | 1 | Driver factory |
| CLI | 1 | Command-line interface |
| **Total** | **59** | Kotlin source files |

---

## Building

### Build Library

```bash
./gradlew :testzen-lite:testzen-core:build
```

### Build CLI (Fat JAR)

```bash
./gradlew :testzen-lite:testzen-cli:shadowJar
# Output: testzen-lite/testzen-cli/build/libs/testzen-cli-1.0.0-all.jar
```

### Publish to Repository

```bash
# Publish to local repository
./gradlew :testzen-lite:testzen-core:publishToMavenLocal

# Publish to Nexus (requires credentials)
NEXUS_USER=user NEXUS_PASSWORD=pass ./gradlew :testzen-lite:testzen-core:publish
```

---

## Security & Compliance

TestZen Lite is designed for organizations with formal security approval processes.

### Security Scanning

```bash
# Generate SBOM (CycloneDX format)
./gradlew :testzen-lite:testzen-core:cyclonedxBom

# Run OWASP Dependency Check
./gradlew :testzen-lite:testzen-core:dependencyCheckAnalyze

# Full security scan
./gradlew :testzen-lite:testzen-core:securityScan
```

### Dependencies

All dependencies use permissive open-source licenses:

| Dependency | Version | License |
|------------|---------|---------|
| io.appium:java-client | 9.0.0 | Apache 2.0 |
| org.seleniumhq.selenium:selenium-java | 4.15.0 | Apache 2.0 |
| org.jetbrains.kotlin:kotlin-stdlib | 1.9.22 | Apache 2.0 |
| org.jetbrains.kotlinx:kotlinx-coroutines | 1.7.3 | Apache 2.0 |
| org.yaml:snakeyaml | 2.2 | Apache 2.0 |
| org.slf4j:slf4j-api | 2.0.11 | MIT |
| ch.qos.logback:logback-classic | 1.4.14 | EPL 1.0 |

### Compatible Security Scanners

- Snyk
- OWASP Dependency-Check
- Sonatype Nexus IQ
- JFrog Xray
- GitHub Dependabot
- Checkmarx SCA
- WhiteSource/Mend
- Black Duck

---

## Prerequisites

- Java 17+
- Appium Server (for mobile testing)
- Chrome/Firefox (for web testing)
- Android SDK (for Android testing)
- Xcode (for iOS testing on macOS)

---

## License

Apache License 2.0
