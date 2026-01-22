package com.testzen.core.verification.registry

import com.testzen.core.verification.ComparisonOperator
import com.testzen.core.verification.VerificationType

/**
 * Default verification definitions using the centralized DSL.
 *
 * All verifications are organized by category for easy maintenance
 * and extension. Each category groups related verification types.
 */
object DefaultVerifications {

    /**
     * Register all default verifications in the registry.
     */
    fun registerAll(registry: VerificationRegistry) {
        registerPresenceVerifications(registry)
        registerStateVerifications(registry)
        registerTextVerifications(registry)
        registerAttributeVerifications(registry)
        registerCssVerifications(registry)
        registerCountVerifications(registry)
        registerCollectionVerifications(registry)
        registerPageVerifications(registry)
    }

    /**
     * Register verifications for a specific category.
     */
    fun registerCategory(registry: VerificationRegistry, category: VerificationCategory) {
        when (category) {
            VerificationCategory.PRESENCE -> registerPresenceVerifications(registry)
            VerificationCategory.STATE -> registerStateVerifications(registry)
            VerificationCategory.TEXT -> registerTextVerifications(registry)
            VerificationCategory.ATTRIBUTE -> registerAttributeVerifications(registry)
            VerificationCategory.CSS -> registerCssVerifications(registry)
            VerificationCategory.COUNT -> registerCountVerifications(registry)
            VerificationCategory.COLLECTION -> registerCollectionVerifications(registry)
            VerificationCategory.PAGE -> registerPageVerifications(registry)
            VerificationCategory.CUSTOM -> { /* No default custom verifications */ }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PRESENCE VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    private fun registerPresenceVerifications(registry: VerificationRegistry) {
        registry.register(VerificationCategory.PRESENCE) {

            verification(VerificationType.DISPLAYED) {
                displayName("Is Displayed")
                description("Verify that an element is visible on the screen")
                keywords("displayed", "visible", "shown", "present", "appears", "see")
                patterns(
                    "verify .* is displayed",
                    "check .* is visible",
                    "confirm .* is shown"
                )
                requiresTarget()
                examples(
                    "Verify 'Login' is displayed",
                    "Check that button is visible"
                )
            }

            verification(VerificationType.NOT_DISPLAYED) {
                displayName("Is Not Displayed")
                description("Verify that an element is not visible on the screen")
                keywords("not displayed", "hidden", "invisible", "gone", "absent", "not visible")
                patterns(
                    "verify .* is not displayed",
                    "check .* is hidden",
                    "confirm .* is gone"
                )
                requiresTarget()
                examples(
                    "Verify error is not displayed",
                    "Check popup is hidden"
                )
            }

            verification(VerificationType.EXISTS) {
                displayName("Exists")
                description("Verify that an element exists in the DOM (may be hidden)")
                keywords("exists", "present", "found", "in dom")
                patterns("verify .* exists", "check .* is present")
                requiresTarget()
                examples("Verify hidden input exists")
            }

            verification(VerificationType.NOT_EXISTS) {
                displayName("Not Exists")
                description("Verify that an element does not exist in the DOM")
                keywords("not exists", "not found", "not in dom")
                patterns("verify .* does not exist", "check .* is not present")
                requiresTarget()
                examples("Verify deleted item does not exist")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // STATE VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    private fun registerStateVerifications(registry: VerificationRegistry) {
        registry.register(VerificationCategory.STATE) {

            verification(VerificationType.ENABLED) {
                displayName("Is Enabled")
                description("Verify that an element is enabled and interactive")
                keywords("enabled", "active", "clickable", "interactable")
                patterns("verify .* is enabled", "check .* is active")
                requiresTarget()
                examples("Verify Submit button is enabled")
            }

            verification(VerificationType.DISABLED) {
                displayName("Is Disabled")
                description("Verify that an element is disabled and not interactive")
                keywords("disabled", "inactive", "grayed", "not clickable")
                patterns("verify .* is disabled", "check .* is inactive")
                requiresTarget()
                examples("Verify Submit is disabled")
            }

            verification(VerificationType.SELECTED) {
                displayName("Is Selected")
                description("Verify that an element is selected (e.g., dropdown option)")
                keywords("selected", "chosen", "picked")
                patterns("verify .* is selected")
                requiresTarget()
                examples("Verify option is selected")
            }

            verification(VerificationType.NOT_SELECTED) {
                displayName("Is Not Selected")
                description("Verify that an element is not selected")
                keywords("not selected", "unselected")
                patterns("verify .* is not selected")
                requiresTarget()
                examples("Verify option is not selected")
            }

            verification(VerificationType.CHECKED) {
                displayName("Is Checked")
                description("Verify that a checkbox or radio button is checked")
                keywords("checked", "ticked", "marked", "on")
                patterns("verify .* is checked", "check .* is ticked")
                requiresTarget()
                examples("Verify checkbox is checked", "Check toggle is on")
            }

            verification(VerificationType.NOT_CHECKED) {
                displayName("Is Not Checked")
                description("Verify that a checkbox or radio button is not checked")
                keywords("not checked", "unchecked", "unticked", "unmarked", "off")
                patterns("verify .* is not checked", "check .* is unchecked")
                requiresTarget()
                examples("Verify checkbox is unchecked")
            }

            verification(VerificationType.FOCUSED) {
                displayName("Is Focused")
                description("Verify that an element has focus")
                keywords("focused", "has focus", "active focus")
                patterns("verify .* is focused", "check .* has focus")
                requiresTarget()
                examples("Verify input is focused")
            }

            verification(VerificationType.NOT_FOCUSED) {
                displayName("Is Not Focused")
                description("Verify that an element does not have focus")
                keywords("not focused", "no focus")
                patterns("verify .* is not focused")
                requiresTarget()
                examples("Verify input is not focused")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // TEXT VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    private fun registerTextVerifications(registry: VerificationRegistry) {
        registry.register(VerificationCategory.TEXT) {

            verification(VerificationType.TEXT_EQUALS) {
                displayName("Text Equals")
                description("Verify element text equals expected value exactly")
                keywords("text equals", "text is", "shows", "displays")
                patterns("verify .* text is .*", "check .* shows .*")
                operators(ComparisonOperator.EQUALS)
                requiresTarget()
                requiresExpectedValue()
                examples("Verify title text is 'Dashboard'")
            }

            verification(VerificationType.TEXT_CONTAINS) {
                displayName("Text Contains")
                description("Verify element text contains expected value")
                keywords("contains", "includes", "has text")
                patterns("verify .* contains .*", "check .* includes .*")
                operators(ComparisonOperator.CONTAINS)
                requiresTarget()
                requiresExpectedValue()
                examples("Verify message contains 'success'")
            }

            verification(VerificationType.TEXT_STARTS_WITH) {
                displayName("Text Starts With")
                description("Verify element text starts with expected value")
                keywords("starts with", "begins with", "prefix")
                patterns("verify .* starts with .*")
                operators(ComparisonOperator.STARTS_WITH)
                requiresTarget()
                requiresExpectedValue()
                examples("Verify title starts with 'Welcome'")
            }

            verification(VerificationType.TEXT_ENDS_WITH) {
                displayName("Text Ends With")
                description("Verify element text ends with expected value")
                keywords("ends with", "suffix")
                patterns("verify .* ends with .*")
                operators(ComparisonOperator.ENDS_WITH)
                requiresTarget()
                requiresExpectedValue()
                examples("Verify message ends with 'complete'")
            }

            verification(VerificationType.TEXT_MATCHES_REGEX) {
                displayName("Text Matches Regex")
                description("Verify element text matches regular expression")
                keywords("matches", "regex", "pattern")
                patterns("verify .* matches .*")
                operators(ComparisonOperator.MATCHES_REGEX)
                requiresTarget()
                requiresExpectedValue()
                examples("Verify email matches '[a-z]+@[a-z]+\\.com'")
            }

            verification(VerificationType.TEXT_NOT_CONTAINS) {
                displayName("Text Does Not Contain")
                description("Verify element text does not contain expected value")
                keywords("not contains", "does not contain", "excludes")
                patterns("verify .* does not contain .*")
                operators(ComparisonOperator.NOT_CONTAINS)
                requiresTarget()
                requiresExpectedValue()
                examples("Verify message does not contain 'error'")
            }

            verification(VerificationType.TEXT_IS_EMPTY) {
                displayName("Text Is Empty")
                description("Verify element text is empty")
                keywords("empty", "blank", "no text")
                patterns("verify .* is empty", "check .* is blank")
                operators(ComparisonOperator.IS_EMPTY)
                requiresTarget()
                examples("Verify input is empty")
            }

            verification(VerificationType.TEXT_IS_NOT_EMPTY) {
                displayName("Text Is Not Empty")
                description("Verify element text is not empty")
                keywords("not empty", "has text", "not blank")
                patterns("verify .* is not empty", "check .* has text")
                operators(ComparisonOperator.IS_NOT_EMPTY)
                requiresTarget()
                examples("Verify field has text")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ATTRIBUTE VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    private fun registerAttributeVerifications(registry: VerificationRegistry) {
        registry.register(VerificationCategory.ATTRIBUTE) {

            verification(VerificationType.ATTRIBUTE_EQUALS) {
                displayName("Attribute Equals")
                description("Verify element attribute equals expected value")
                keywords("attribute equals", "attribute is", "has attribute")
                patterns("verify .* attribute .* equals .*")
                operators(ComparisonOperator.EQUALS)
                requiresTarget()
                requiresExpectedValue()
                metadata("requiresAttributeName", true)
                examples("Verify button attribute 'disabled' equals 'true'")
            }

            verification(VerificationType.ATTRIBUTE_CONTAINS) {
                displayName("Attribute Contains")
                description("Verify element attribute contains expected value")
                keywords("attribute contains", "attribute includes")
                patterns("verify .* attribute .* contains .*")
                operators(ComparisonOperator.CONTAINS)
                requiresTarget()
                requiresExpectedValue()
                metadata("requiresAttributeName", true)
                examples("Verify input attribute 'class' contains 'error'")
            }

            verification(VerificationType.ATTRIBUTE_EXISTS) {
                displayName("Attribute Exists")
                description("Verify element has a specific attribute")
                keywords("has attribute", "attribute exists", "attribute present")
                patterns("verify .* has attribute .*")
                requiresTarget()
                metadata("requiresAttributeName", true)
                examples("Verify element has 'data-testid' attribute")
            }

            verification(VerificationType.ATTRIBUTE_NOT_EXISTS) {
                displayName("Attribute Not Exists")
                description("Verify element does not have a specific attribute")
                keywords("no attribute", "attribute not exists", "missing attribute")
                patterns("verify .* does not have attribute .*")
                requiresTarget()
                metadata("requiresAttributeName", true)
                examples("Verify element has no 'disabled' attribute")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CSS VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    private fun registerCssVerifications(registry: VerificationRegistry) {
        registry.register(VerificationCategory.CSS) {

            verification(VerificationType.CSS_PROPERTY_EQUALS) {
                displayName("CSS Property Equals")
                description("Verify element CSS property equals expected value")
                keywords("css", "style", "css property")
                patterns("verify .* css .* is .*")
                operators(ComparisonOperator.EQUALS)
                requiresTarget()
                requiresExpectedValue()
                metadata("requiresCssProperty", true)
                examples("Verify button CSS 'background-color' is 'red'")
            }

            verification(VerificationType.CSS_PROPERTY_CONTAINS) {
                displayName("CSS Property Contains")
                description("Verify element CSS property contains expected value")
                keywords("css contains", "style contains")
                patterns("verify .* css .* contains .*")
                operators(ComparisonOperator.CONTAINS)
                requiresTarget()
                requiresExpectedValue()
                metadata("requiresCssProperty", true)
                examples("Verify text CSS 'font-family' contains 'Arial'")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // COUNT VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    private fun registerCountVerifications(registry: VerificationRegistry) {
        registry.register(VerificationCategory.COUNT) {

            verification(VerificationType.COUNT_EQUALS) {
                displayName("Count Equals")
                description("Verify count of elements equals expected number")
                keywords("count", "number", "total")
                patterns("verify count of .* is .*", "verify there are .* items")
                operators(ComparisonOperator.EQUALS)
                requiresTarget()
                requiresExpectedValue()
                examples("Verify there are 5 items", "Verify count of results is 10")
            }

            verification(VerificationType.COUNT_GREATER_THAN) {
                displayName("Count Greater Than")
                description("Verify count of elements is greater than expected")
                keywords("more than", "greater than", "above", ">")
                patterns("verify count of .* is greater than .*")
                operators(ComparisonOperator.GREATER_THAN)
                requiresTarget()
                requiresExpectedValue()
                examples("Verify more than 3 items")
            }

            verification(VerificationType.COUNT_LESS_THAN) {
                displayName("Count Less Than")
                description("Verify count of elements is less than expected")
                keywords("less than", "fewer than", "below", "<")
                patterns("verify count of .* is less than .*")
                operators(ComparisonOperator.LESS_THAN)
                requiresTarget()
                requiresExpectedValue()
                examples("Verify fewer than 10 items")
            }

            verification(VerificationType.COUNT_GREATER_OR_EQUAL) {
                displayName("Count Greater Or Equal")
                description("Verify count of elements is at least expected number")
                keywords("at least", "minimum", ">=")
                patterns("verify count of .* is at least .*")
                operators(ComparisonOperator.GREATER_OR_EQUAL)
                requiresTarget()
                requiresExpectedValue()
                examples("Verify at least 1 item")
            }

            verification(VerificationType.COUNT_LESS_OR_EQUAL) {
                displayName("Count Less Or Equal")
                description("Verify count of elements is at most expected number")
                keywords("at most", "maximum", "<=")
                patterns("verify count of .* is at most .*")
                operators(ComparisonOperator.LESS_OR_EQUAL)
                requiresTarget()
                requiresExpectedValue()
                examples("Verify at most 5 errors")
            }

            verification(VerificationType.COUNT_BETWEEN) {
                displayName("Count Between")
                description("Verify count of elements is between min and max")
                keywords("between", "range")
                patterns("verify count of .* is between .* and .*")
                operators(ComparisonOperator.BETWEEN)
                requiresTarget()
                requiresExpectedValue()
                examples("Verify between 5 and 10 items")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // COLLECTION VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    private fun registerCollectionVerifications(registry: VerificationRegistry) {
        registry.register(VerificationCategory.COLLECTION) {

            verification(VerificationType.ALL_DISPLAYED) {
                displayName("All Displayed")
                description("Verify all matching elements are displayed")
                keywords("all displayed", "all visible", "every")
                patterns("verify all .* are displayed")
                requiresTarget()
                examples("Verify all items are visible")
            }

            verification(VerificationType.ANY_DISPLAYED) {
                displayName("Any Displayed")
                description("Verify at least one matching element is displayed")
                keywords("any displayed", "at least one", "some")
                patterns("verify any .* is displayed")
                requiresTarget()
                examples("Verify at least one result is shown")
            }

            verification(VerificationType.NONE_DISPLAYED) {
                displayName("None Displayed")
                description("Verify no matching elements are displayed")
                keywords("none displayed", "no", "zero")
                patterns("verify no .* are displayed")
                requiresTarget()
                examples("Verify no errors are displayed")
            }

            verification(VerificationType.ALL_CONTAIN_TEXT) {
                displayName("All Contain Text")
                description("Verify all matching elements contain expected text")
                keywords("all contain", "every contains")
                patterns("verify all .* contain .*")
                operators(ComparisonOperator.CONTAINS)
                requiresTarget()
                requiresExpectedValue()
                examples("Verify all items contain 'Active'")
            }

            verification(VerificationType.ANY_CONTAINS_TEXT) {
                displayName("Any Contains Text")
                description("Verify at least one element contains expected text")
                keywords("any contains", "some contain")
                patterns("verify any .* contains .*")
                operators(ComparisonOperator.CONTAINS)
                requiresTarget()
                requiresExpectedValue()
                examples("Verify any message contains 'success'")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PAGE VERIFICATIONS
    // ═══════════════════════════════════════════════════════════════

    private fun registerPageVerifications(registry: VerificationRegistry) {
        registry.register(VerificationCategory.PAGE) {

            verification(VerificationType.PAGE_TITLE_EQUALS) {
                displayName("Page Title Equals")
                description("Verify page title equals expected value")
                keywords("page title", "title is", "title equals")
                patterns("verify page title is .*")
                operators(ComparisonOperator.EQUALS)
                noTarget()
                requiresExpectedValue()
                examples("Verify page title is 'Dashboard'")
            }

            verification(VerificationType.PAGE_TITLE_CONTAINS) {
                displayName("Page Title Contains")
                description("Verify page title contains expected value")
                keywords("title contains", "title includes")
                patterns("verify page title contains .*")
                operators(ComparisonOperator.CONTAINS)
                noTarget()
                requiresExpectedValue()
                examples("Verify page title contains 'Login'")
            }

            verification(VerificationType.URL_EQUALS) {
                displayName("URL Equals")
                description("Verify current URL equals expected value")
                keywords("url is", "url equals", "address is")
                patterns("verify url is .*", "verify url equals .*")
                operators(ComparisonOperator.EQUALS)
                noTarget()
                requiresExpectedValue()
                examples("Verify URL equals 'https://example.com'")
            }

            verification(VerificationType.URL_CONTAINS) {
                displayName("URL Contains")
                description("Verify current URL contains expected value")
                keywords("url contains", "address contains")
                patterns("verify url contains .*")
                operators(ComparisonOperator.CONTAINS)
                noTarget()
                requiresExpectedValue()
                examples("Verify URL contains '/dashboard'")
            }

            verification(VerificationType.PAGE_SOURCE_CONTAINS) {
                displayName("Page Source Contains")
                description("Verify page source contains expected text")
                keywords("page contains", "source contains", "html contains")
                patterns("verify page contains .*")
                operators(ComparisonOperator.CONTAINS)
                noTarget()
                requiresExpectedValue()
                examples("Verify page contains 'Copyright 2024'")
            }
        }
    }
}
