package com.testzen.core.nlp.patterns

import com.testzen.core.nlp.ExtractedEntities
import com.testzen.core.nlp.Intent
import com.testzen.core.nlp.synonyms.SynonymRegistry

/**
 * Default pattern definitions using the centralized DSL.
 *
 * All patterns are organized by category and use:
 * - PatternFragments (F) for consistent regex construction
 * - SynonymRegistry (S) for centralized keyword management
 *
 * Pattern Organization:
 * - Each category has its own registration method
 * - Patterns use F (PatternFragments) for regex brevity
 * - Keywords come from SynonymRegistry for consistency
 */
object DefaultPatterns {
    // Lazy-initialized synonym registry
    private val S: SynonymRegistry by lazy { SynonymRegistry.default() }

    /**
     * Register all default patterns in the registry.
     */
    fun registerAll(registry: PatternRegistry) {
        registerInteractionPatterns(registry)
        registerVerificationPatterns(registry)
        registerNavigationPatterns(registry)
        registerWaitPatterns(registry)
        registerAppLifecyclePatterns(registry)
        registerFormControlPatterns(registry)
    }

    /**
     * Register patterns for a specific category.
     */
    fun registerCategory(registry: PatternRegistry, category: PatternCategory) {
        when (category) {
            PatternCategory.INTERACTION -> registerInteractionPatterns(registry)
            PatternCategory.VERIFICATION -> registerVerificationPatterns(registry)
            PatternCategory.NAVIGATION -> registerNavigationPatterns(registry)
            PatternCategory.WAIT -> registerWaitPatterns(registry)
            PatternCategory.APP_LIFECYCLE -> registerAppLifecyclePatterns(registry)
            PatternCategory.FORM_CONTROL -> registerFormControlPatterns(registry)
            PatternCategory.CUSTOM -> { /* No default custom patterns */ }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // INTERACTION PATTERNS
    // ═══════════════════════════════════════════════════════════════

    private fun registerInteractionPatterns(registry: PatternRegistry) {
        registry.register(PatternCategory.INTERACTION) {

            // Click/Tap
            pattern(Intent.CLICK) {
                keywords(S.get("CLICK"))
                regex { "${F.CLICK}\\s+${F.ON}${F.THE}${F.TARGET_CLEAN}${F.BUTTON}$" }
                priority(1.0)
            }

            // Double Click/Tap
            pattern(Intent.DOUBLE_CLICK) {
                keywords(S.get("DOUBLE_CLICK"))
                regex { "${F.DOUBLE_CLICK}\\s+${F.ON}${F.THE}${F.TARGET_CLEAN}$" }
                priority(1.2)
            }

            // Long Press
            pattern(Intent.LONG_PRESS) {
                keywords(S.get("LONG_PRESS"))
                regex { "${F.LONG_PRESS}\\s+${F.ON}${F.THE}${F.TARGET_CLEAN}$" }
                priority(1.2)
            }

            // Enter Text: "enter 'text' in field"
            pattern(Intent.ENTER_TEXT) {
                keywords(S.get("ENTER_TEXT"))
                regex { "${F.ENTER_TEXT}\\s+${F.QUOTED_VALUE}\\s+${F.IN}\\s+${F.THE}${F.TARGET}${F.FIELD}$" }
                priority(1.3)
            }

            // Enter Text: "in field, enter 'text'" (reversed)
            pattern(Intent.ENTER_TEXT) {
                keywords(S.get("ENTER_TEXT"))
                regex { "${F.IN}\\s+${F.THE}${F.TARGET}\\s*,?\\s*${F.ENTER_TEXT}\\s+${F.QUOTED_VALUE}$" }
                priority(1.3)
                extractEntities { match ->
                    ExtractedEntities(
                        target = match.groupValues[1],
                        value = match.groupValues[2]
                    )
                }
            }

            // Enter Text: "set field to 'text'"
            pattern(Intent.ENTER_TEXT) {
                keywords("set")
                regex { "set\\s+${F.THE}${F.TARGET}\\s+(?:to|as|=)\\s+${F.QUOTED_VALUE}$" }
                priority(1.2)
                extractTargetAndValue()
            }

            // Clear Text
            pattern(Intent.CLEAR_TEXT) {
                keywords(S.get("CLEAR"))
                regex { "${F.CLEAR}\\s+${F.THE}${F.TEXT}(?:${F.IN}\\s+)?${F.TARGET_CLEAN}${F.FIELD}$" }
                priority(1.0)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // VERIFICATION PATTERNS
    // ═══════════════════════════════════════════════════════════════

    private fun registerVerificationPatterns(registry: PatternRegistry) {
        registry.register(PatternCategory.VERIFICATION) {

            // ─── Presence Verification ────────────────────────────────

            // Verify Displayed
            pattern(Intent.VERIFY_DISPLAYED) {
                keywords(S.get("VERIFY"))
                regex { "${F.VERIFY}\\s+${F.THAT}${F.TARGET}\\s+${F.IS}${F.DISPLAYED}" }
                priority(1.0)
            }

            // Verify Displayed: "element should be visible"
            pattern(Intent.VERIFY_DISPLAYED) {
                keywords(S.get("DISPLAYED"))
                regex { "${F.TARGET}\\s+(?:should|must|is)\\s+${F.BE}${F.DISPLAYED}" }
                priority(0.9)
            }

            // Verify Not Displayed
            pattern(Intent.VERIFY_NOT_DISPLAYED) {
                keywords(S.get("NOT_DISPLAYED"))
                regex { "${F.VERIFY_SIMPLE}\\s+${F.THAT}${F.TARGET}\\s+${F.IS}${F.NOT_DISPLAYED}" }
                priority(1.1)
                requiresAny("not", "hidden", "gone", "absent")
            }

            // ─── State Verification ───────────────────────────────────

            // Verify Enabled
            pattern(Intent.VERIFY_ENABLED) {
                keywords(S.get("ENABLED"))
                regex { "${F.VERIFY_SIMPLE}\\s+${F.THAT}${F.TARGET}\\s+${F.IS}${F.ENABLED}" }
                priority(1.0)
            }

            // Verify Disabled
            pattern(Intent.VERIFY_DISABLED) {
                keywords(S.get("DISABLED"))
                regex { "${F.VERIFY_SIMPLE}\\s+${F.THAT}${F.TARGET}\\s+${F.IS}${F.DISABLED}" }
                priority(1.0)
            }

            // Verify Checked
            pattern(Intent.VERIFY_CHECKED) {
                keywords("check", "verify", "assert", "confirm", "ticked", "marked")
                regex { "${F.VERIFY_SIMPLE}\\s+${F.THAT}${F.TARGET}\\s+${F.IS}${F.CHECKED}" }
                priority(1.1)
                requiresNot("not ")
                validate { text -> !text.contains("unchecked") }
            }

            // Verify Not Checked
            pattern(Intent.VERIFY_NOT_CHECKED) {
                keywords("check", "verify", "unchecked", "not")
                regex { "${F.VERIFY_SIMPLE}\\s+${F.THAT}${F.TARGET}\\s+${F.IS}${F.NOT_CHECKED}" }
                priority(1.2)
            }

            // Verify Selected
            pattern(Intent.VERIFY_SELECTED) {
                keywords("verify", "check", "selected")
                regex { "${F.VERIFY_SIMPLE}\\s+${F.THAT}${F.TARGET}\\s+${F.IS}${F.SELECTED}" }
                priority(1.0)
                requiresNot("not ")
            }

            // Verify Not Selected
            pattern(Intent.VERIFY_NOT_SELECTED) {
                keywords("verify", "check", "not", "selected")
                regex { "${F.VERIFY_SIMPLE}\\s+${F.THAT}${F.TARGET}\\s+${F.IS}${F.NOT_SELECTED}" }
                priority(1.1)
            }

            // ─── Text Verification ────────────────────────────────────

            // Verify Text: "verify element shows 'text'"
            pattern(Intent.VERIFY_TEXT) {
                keywords(S.get("VERIFY"))
                regex { "${F.VERIFY_SIMPLE}\\s+${F.THAT}${F.THE}${F.TARGET}\\s+${F.SHOWS}\\s+${F.QUOTED_VALUE}" }
                priority(1.2)
                extractTargetAndValue()
            }

            // Verify Text: "text 'X' should be displayed"
            pattern(Intent.VERIFY_TEXT) {
                keywords("text")
                regex { "${F.TEXT}${F.QUOTED_VALUE}\\s+(?:should|must|is)\\s+${F.BE}${F.DISPLAYED}" }
                priority(1.0)
                extractEntities { match ->
                    ExtractedEntities(target = "screen", value = match.groupValues[1])
                }
            }

            // Verify Text Contains
            pattern(Intent.VERIFY_TEXT_CONTAINS) {
                keywords("verify", "check", "contains", "includes")
                regex { "${F.VERIFY_SIMPLE}\\s+${F.THAT}${F.TARGET}\\s+${F.TEXT}${F.CONTAINS}\\s+${F.QUOTED_VALUE}" }
                priority(1.2)
                extractTargetAndValue()
            }

            // Verify Text Matches (regex)
            pattern(Intent.VERIFY_TEXT_MATCHES) {
                keywords("verify", "check", "matches", "regex", "pattern")
                regex { "${F.VERIFY_SIMPLE}\\s+${F.THAT}${F.TARGET}\\s+${F.TEXT}${F.MATCHES}\\s+(?:pattern\\s+)?${F.QUOTED_VALUE}" }
                priority(1.2)
                extractRegexPattern()
            }

            // Verify Text Empty
            pattern(Intent.VERIFY_TEXT_EMPTY) {
                keywords("verify", "check", "empty", "blank")
                regex { "${F.VERIFY_SIMPLE}\\s+${F.THAT}${F.TARGET}\\s+${F.TEXT}${F.IS}${F.EMPTY}" }
                priority(1.0)
                requiresNot("not ")
            }

            // Verify Text Not Empty
            pattern(Intent.VERIFY_TEXT_NOT_EMPTY) {
                keywords("verify", "check", "not", "empty")
                regex { "${F.VERIFY_SIMPLE}\\s+${F.THAT}${F.TARGET}\\s+${F.TEXT}${F.IS}not\\s+${F.EMPTY}" }
                priority(1.1)
            }

            // ─── Count/Numeric Verification ───────────────────────────

            // Verify Count
            pattern(Intent.VERIFY_COUNT) {
                keywords("verify", "check", "count", "number")
                regex { "${F.VERIFY_SIMPLE}\\s+${F.THAT}${F.THE}${F.COUNT}\\s+(?:of\\s+)?${F.TARGET}\\s+${F.IS}(?:equals?\\s+)?${F.INTEGER}" }
                priority(1.1)
                extractNumericValue()
            }

            // Verify Value Greater Than
            pattern(Intent.VERIFY_VALUE_GREATER) {
                keywords("verify", "check", "greater", "more", "above")
                regex { "${F.VERIFY_SIMPLE}\\s+${F.THAT}${F.TARGET}\\s+${F.IS}${F.GREATER_THAN}\\s+${F.INTEGER}" }
                priority(1.1)
                extractEntities { match ->
                    ExtractedEntities(
                        target = match.groupValues[1],
                        numericValue = match.groupValues[2].toLongOrNull(),
                        comparisonOperator = "greater"
                    )
                }
            }

            // Verify Value Less Than
            pattern(Intent.VERIFY_VALUE_LESS) {
                keywords("verify", "check", "less", "fewer", "below")
                regex { "${F.VERIFY_SIMPLE}\\s+${F.THAT}${F.TARGET}\\s+${F.IS}${F.LESS_THAN}\\s+${F.INTEGER}" }
                priority(1.1)
                extractEntities { match ->
                    ExtractedEntities(
                        target = match.groupValues[1],
                        numericValue = match.groupValues[2].toLongOrNull(),
                        comparisonOperator = "less"
                    )
                }
            }

            // ─── Attribute Verification ───────────────────────────────

            // Verify Attribute
            pattern(Intent.VERIFY_ATTRIBUTE) {
                keywords("verify", "check", "attribute", "has")
                regex { "${F.VERIFY_SIMPLE}\\s+${F.THAT}${F.TARGET}\\s+(?:has\\s+)?${F.ATTRIBUTE}\\s+${F.QUOTED_VALUE}\\s+${F.EQUALS}\\s+${F.QUOTED_VALUE}" }
                priority(1.2)
                extractAttribute()
            }

            // ─── Page/Screen Verification ─────────────────────────────

            // Verify Page Title
            pattern(Intent.VERIFY_PAGE_TITLE) {
                keywords("verify", "check", "title", "page")
                regex { "${F.VERIFY_SIMPLE}\\s+${F.THAT}${F.THE}${F.PAGE_TITLE}\\s+(?:is|equals?|contains?)\\s+${F.QUOTED_VALUE}" }
                priority(1.1)
                extractEntities { match ->
                    ExtractedEntities(target = "page title", value = match.groupValues[1])
                }
            }

            // Verify URL
            pattern(Intent.VERIFY_URL) {
                keywords("verify", "check", "url", "address")
                regex { "${F.VERIFY_SIMPLE}\\s+${F.THAT}${F.THE}${F.URL}\\s+(?:is|equals?|contains?)\\s+${F.QUOTED_VALUE}" }
                priority(1.1)
                extractEntities { match ->
                    ExtractedEntities(target = "url", value = match.groupValues[1])
                }
            }

            // Verify Text On Screen
            pattern(Intent.VERIFY_TEXT_ON_SCREEN) {
                keywords("verify", "check", "see", "screen", "page")
                regex { "${F.SEE}\\s+${F.THAT}${F.QUOTED_VALUE}\\s+${F.IS}${F.ON}${F.SCREEN}" }
                priority(1.0)
                extractEntities { match ->
                    ExtractedEntities(target = "screen", value = match.groupValues[1])
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // NAVIGATION PATTERNS
    // ═══════════════════════════════════════════════════════════════

    private fun registerNavigationPatterns(registry: PatternRegistry) {
        registry.register(PatternCategory.NAVIGATION) {

            // Scroll
            pattern(Intent.SCROLL) {
                keywords(S.get("SCROLL"))
                regex { "${F.SCROLL}\\s+${F.DIRECTION}(?:\\s+${F.TO}(?:find\\s+)?${F.TARGET})?$" }
                priority(1.0)
                extractDirection()
            }

            // Swipe
            pattern(Intent.SWIPE) {
                keywords(S.get("SWIPE"))
                regex { "${F.SWIPE}\\s+${F.DIRECTION}(?:\\s+${F.ON}${F.THE}${F.TARGET})?$" }
                priority(1.0)
                extractDirection()
            }

            // Navigate Back
            pattern(Intent.NAVIGATE_BACK) {
                keywords(S.get("BACK"))
                regex { F.BACK }
                priority(1.0)
            }

            // Navigate Forward
            pattern(Intent.NAVIGATE_FORWARD) {
                keywords(S.get("FORWARD"))
                regex { F.FORWARD }
                priority(1.0)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // WAIT PATTERNS
    // ═══════════════════════════════════════════════════════════════

    private fun registerWaitPatterns(registry: PatternRegistry) {
        registry.register(PatternCategory.WAIT) {

            // Wait Duration
            pattern(Intent.WAIT_DURATION) {
                keywords(S.get("WAIT"))
                regex { "${F.WAIT}\\s+(?:for\\s+)?${F.DURATION}" }
                priority(1.0)
                extractEntities { match ->
                    val duration = S.parseDuration(match.value) ?: 1000
                    ExtractedEntities(numericValue = duration)
                }
            }

            // Wait For Element
            pattern(Intent.WAIT_FOR_ELEMENT) {
                keywords(S.get("WAIT"))
                regex { "${F.WAIT}\\s+(?:for\\s+)?${F.THE}${F.TARGET}\\s+${F.TO}${F.APPEAR}" }
                priority(1.1)
            }

            // Wait For Element Gone
            pattern(Intent.WAIT_FOR_ELEMENT_GONE) {
                keywords(S.get("WAIT"))
                regex { "${F.WAIT}\\s+(?:for\\s+)?${F.THE}${F.TARGET}\\s+${F.TO}${F.DISAPPEAR}" }
                priority(1.1)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // APP LIFECYCLE PATTERNS
    // ═══════════════════════════════════════════════════════════════

    private fun registerAppLifecyclePatterns(registry: PatternRegistry) {
        registry.register(PatternCategory.APP_LIFECYCLE) {

            // Launch App
            pattern(Intent.LAUNCH_APP) {
                keywords(S.get("LAUNCH"))
                regex { "${F.LAUNCH}\\s+${F.THE}${F.APP}" }
                priority(1.0)
            }

            // Close App
            pattern(Intent.CLOSE_APP) {
                keywords(S.get("CLOSE"))
                regex { "${F.CLOSE}\\s+${F.THE}${F.APP}" }
                priority(1.0)
            }

            // Take Screenshot
            pattern(Intent.TAKE_SCREENSHOT) {
                keywords(S.get("SCREENSHOT"))
                regex { F.SCREENSHOT }
                priority(1.0)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // FORM CONTROL PATTERNS
    // ═══════════════════════════════════════════════════════════════

    private fun registerFormControlPatterns(registry: PatternRegistry) {
        registry.register(PatternCategory.FORM_CONTROL) {

            // Select Option
            pattern(Intent.SELECT_OPTION) {
                keywords(S.get("SELECT"))
                regex { "${F.SELECT}\\s+${F.QUOTED_VALUE}\\s+${F.FROM}\\s+${F.THE}${F.TARGET}${F.DROPDOWN}$" }
                priority(1.2)
                extractOptionAndTarget()
            }

            // Check Checkbox
            pattern(Intent.CHECK_CHECKBOX) {
                keywords("check", "tick", "mark", "enable")
                regex { "${F.CHECK}\\s+${F.THE}${F.TARGET}${F.CHECKBOX}$" }
                priority(1.0)
            }

            // Uncheck Checkbox
            pattern(Intent.UNCHECK_CHECKBOX) {
                keywords("uncheck", "untick", "unmark", "disable")
                regex { "${F.UNCHECK}\\s+${F.THE}${F.TARGET}${F.CHECKBOX}$" }
                priority(1.0)
            }

            // Toggle Switch
            pattern(Intent.TOGGLE_SWITCH) {
                keywords(S.get("TOGGLE"))
                regex { "${F.TOGGLE}\\s+${F.THE}${F.TARGET}${F.ON_OFF}$" }
                priority(1.0)
                extractEntities { match ->
                    val text = match.value.lowercase()
                    ExtractedEntities(
                        target = match.groupValues[1],
                        modifiers = mapOf("state" to when {
                            text.contains(" on") -> "on"
                            text.contains(" off") -> "off"
                            else -> "toggle"
                        })
                    )
                }
            }
        }
    }
}
