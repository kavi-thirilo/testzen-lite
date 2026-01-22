package com.testzen.core.execution

import com.testzen.core.nlp.*
import org.slf4j.LoggerFactory

/**
 * Parses natural language test instructions into executable commands.
 *
 * Uses the NLP engine for robust intent classification and entity extraction.
 * Supports a wide variety of natural language patterns through synonym support.
 *
 * Supported instruction types:
 * - **Interactions**: click, double-click, long-press, enter text, clear
 * - **Verification**: verify displayed, verify text, verify enabled/disabled
 * - **Waits**: wait duration, wait for element appear/disappear
 * - **Navigation**: scroll, swipe, back, forward
 * - **App lifecycle**: launch, close, screenshot
 * - **Form controls**: select option, check/uncheck, toggle
 *
 * Example patterns:
 * ```
 * "Click the login button"
 * "Tap on Submit"
 * "Enter 'hello' in the search field"
 * "Type 'user@email.com' into Email"
 * "Verify 'Welcome' is displayed"
 * "Check that the title shows 'Home'"
 * "Wait for 3 seconds"
 * "Wait for the loading spinner to disappear"
 * "Scroll down to find Settings"
 * "Swipe left on the card"
 * "Select 'Option 1' from the dropdown"
 * "Toggle dark mode on"
 * ```
 */
class InstructionParser {
    private val logger = LoggerFactory.getLogger(InstructionParser::class.java)
    private val nlpEngine = NLPEngine()

    /**
     * Parse a natural language instruction into a structured Instruction.
     */
    fun parse(text: String): Instruction {
        val trimmed = text.trim()
        logger.debug("Parsing instruction: $trimmed")

        // Use NLP engine for parsing
        val result = nlpEngine.parseWithFallback(trimmed)
        logger.debug("NLP result: intent={}, confidence={}", result.intent, result.confidence)

        // Convert NLP result to Instruction
        return convertToInstruction(result)
    }

    /**
     * Parse with detailed result including confidence.
     */
    fun parseWithDetails(text: String): ParseResult {
        val result = nlpEngine.parse(text.trim())
        val instruction = convertToInstruction(result)

        return ParseResult(
            instruction = instruction,
            confidence = result.confidence,
            intent = result.intent,
            alternatives = result.alternatives.map { alt ->
                ParseResult(
                    instruction = convertToInstruction(alt),
                    confidence = alt.confidence,
                    intent = alt.intent
                )
            }
        )
    }

    /**
     * Check if text appears to be a valid instruction.
     */
    fun isValid(text: String): Boolean {
        return nlpEngine.isValidInstruction(text)
    }

    /**
     * Get suggested intents for partial input (for autocomplete).
     */
    fun suggest(partialText: String): List<Intent> {
        return nlpEngine.suggestIntents(partialText)
    }

    // ═══════════════════════════════════════════════════════════════
    // CONVERSION TO INSTRUCTION
    // ═══════════════════════════════════════════════════════════════

    private fun convertToInstruction(result: NLPResult): Instruction {
        val entities = result.entities

        return when (result.intent) {
            // Interaction instructions
            Intent.CLICK -> Instruction.Click(
                target = entities.target ?: result.originalText
            )

            Intent.DOUBLE_CLICK -> Instruction.DoubleTap(
                target = entities.target ?: result.originalText
            )

            Intent.LONG_PRESS -> Instruction.LongPress(
                target = entities.target ?: result.originalText,
                durationMs = entities.numericValue ?: 1000
            )

            Intent.ENTER_TEXT -> Instruction.EnterText(
                target = entities.target ?: "",
                value = entities.value ?: "",
                clearFirst = true
            )

            Intent.CLEAR_TEXT -> Instruction.ClearText(
                target = entities.target ?: ""
            )

            // Verification instructions
            Intent.VERIFY_DISPLAYED -> Instruction.VerifyDisplayed(
                target = entities.target ?: ""
            )

            Intent.VERIFY_NOT_DISPLAYED -> Instruction.VerifyNotDisplayed(
                target = entities.target ?: ""
            )

            Intent.VERIFY_TEXT -> Instruction.VerifyText(
                target = entities.target ?: "screen",
                expectedText = entities.value ?: ""
            )

            Intent.VERIFY_ENABLED -> Instruction.VerifyEnabled(
                target = entities.target ?: ""
            )

            Intent.VERIFY_DISABLED -> Instruction.VerifyDisabled(
                target = entities.target ?: ""
            )

            // Wait instructions
            Intent.WAIT_DURATION -> Instruction.Wait(
                durationMs = entities.numericValue ?: 1000
            )

            Intent.WAIT_FOR_ELEMENT -> Instruction.WaitForElement(
                target = entities.target ?: "",
                shouldDisappear = false
            )

            Intent.WAIT_FOR_ELEMENT_GONE -> Instruction.WaitForElement(
                target = entities.target ?: "",
                shouldDisappear = true
            )

            // Navigation instructions
            Intent.SCROLL -> Instruction.Scroll(
                direction = parseDirection(entities.direction),
                target = entities.target
            )

            Intent.SWIPE -> Instruction.Swipe(
                direction = parseDirection(entities.direction),
                target = entities.target
            )

            Intent.NAVIGATE_BACK -> Instruction.NavigateBack

            Intent.NAVIGATE_FORWARD -> Instruction.NavigateForward

            // App lifecycle instructions
            Intent.LAUNCH_APP -> Instruction.LaunchApp

            Intent.CLOSE_APP -> Instruction.CloseApp

            Intent.TAKE_SCREENSHOT -> Instruction.TakeScreenshot(
                name = entities.value
            )

            // Form control instructions
            Intent.SELECT_OPTION -> Instruction.SelectOption(
                target = entities.target ?: "",
                option = entities.option ?: ""
            )

            Intent.CHECK_CHECKBOX -> Instruction.CheckCheckbox(
                target = entities.target ?: ""
            )

            Intent.UNCHECK_CHECKBOX -> Instruction.UncheckCheckbox(
                target = entities.target ?: ""
            )

            Intent.TOGGLE_SWITCH -> {
                val state = (entities.modifiers["state"] as? String) ?: "toggle"
                Instruction.ToggleSwitch(
                    target = entities.target ?: "",
                    targetState = when (state) {
                        "on" -> true
                        "off" -> false
                        else -> null
                    }
                )
            }

            // Fallback
            Intent.UNKNOWN -> {
                logger.warn("Could not parse instruction, treating as click: ${result.originalText}")
                Instruction.Click(target = result.originalText)
            }
        }
    }

    private fun parseDirection(direction: String?): ScrollDirection {
        return when (direction?.lowercase()) {
            "up" -> ScrollDirection.UP
            "down" -> ScrollDirection.DOWN
            "left" -> ScrollDirection.LEFT
            "right" -> ScrollDirection.RIGHT
            else -> ScrollDirection.DOWN
        }
    }
}

/**
 * Parse result with confidence score and alternatives.
 */
data class ParseResult(
    val instruction: Instruction,
    val confidence: Double,
    val intent: Intent,
    val alternatives: List<ParseResult> = emptyList()
)

/**
 * Parsed instruction types.
 *
 * Comprehensive set of instructions covering:
 * - Basic interactions (click, type, clear)
 * - Advanced gestures (long press, double tap, swipe)
 * - Verification (displayed, text, enabled state)
 * - Waits (duration, element appear/disappear)
 * - Navigation (scroll, back, forward)
 * - App lifecycle (launch, close, screenshot)
 * - Form controls (select, checkbox, toggle)
 */
sealed class Instruction {
    // ═══════════════════════════════════════════════════════════════
    // INTERACTION INSTRUCTIONS
    // ═══════════════════════════════════════════════════════════════

    /** Click/tap on an element */
    data class Click(val target: String) : Instruction()

    /** Double-click/double-tap on an element */
    data class DoubleTap(val target: String) : Instruction()

    /** Long press/hold on an element */
    data class LongPress(val target: String, val durationMs: Long = 1000) : Instruction()

    /** Enter text into an element */
    data class EnterText(
        val target: String,
        val value: String,
        val clearFirst: Boolean = true
    ) : Instruction()

    /** Clear text from an element */
    data class ClearText(val target: String) : Instruction()

    // ═══════════════════════════════════════════════════════════════
    // VERIFICATION INSTRUCTIONS
    // ═══════════════════════════════════════════════════════════════

    /** Verify element is displayed/visible */
    data class VerifyDisplayed(val target: String) : Instruction()

    /** Verify element is NOT displayed/hidden */
    data class VerifyNotDisplayed(val target: String) : Instruction()

    /** Verify element contains expected text */
    data class VerifyText(val target: String, val expectedText: String) : Instruction()

    /** Verify element is enabled */
    data class VerifyEnabled(val target: String) : Instruction()

    /** Verify element is disabled */
    data class VerifyDisabled(val target: String) : Instruction()

    // ═══════════════════════════════════════════════════════════════
    // WAIT INSTRUCTIONS
    // ═══════════════════════════════════════════════════════════════

    /** Wait for a fixed duration */
    data class Wait(val durationMs: Long) : Instruction()

    /** Wait for element to appear or disappear */
    data class WaitForElement(
        val target: String,
        val shouldDisappear: Boolean = false
    ) : Instruction()

    // ═══════════════════════════════════════════════════════════════
    // NAVIGATION INSTRUCTIONS
    // ═══════════════════════════════════════════════════════════════

    /** Scroll in a direction, optionally until target is found */
    data class Scroll(
        val direction: ScrollDirection,
        val target: String? = null
    ) : Instruction()

    /** Swipe in a direction */
    data class Swipe(
        val direction: ScrollDirection,
        val target: String? = null
    ) : Instruction()

    /** Navigate back */
    data object NavigateBack : Instruction()

    /** Navigate forward */
    data object NavigateForward : Instruction()

    // ═══════════════════════════════════════════════════════════════
    // APP LIFECYCLE INSTRUCTIONS
    // ═══════════════════════════════════════════════════════════════

    /** Launch the app */
    data object LaunchApp : Instruction()

    /** Close the app */
    data object CloseApp : Instruction()

    /** Take a screenshot */
    data class TakeScreenshot(val name: String? = null) : Instruction()

    // ═══════════════════════════════════════════════════════════════
    // FORM CONTROL INSTRUCTIONS
    // ═══════════════════════════════════════════════════════════════

    /** Select an option from a dropdown/picker */
    data class SelectOption(
        val target: String,
        val option: String
    ) : Instruction()

    /** Check a checkbox */
    data class CheckCheckbox(val target: String) : Instruction()

    /** Uncheck a checkbox */
    data class UncheckCheckbox(val target: String) : Instruction()

    /** Toggle a switch on/off */
    data class ToggleSwitch(
        val target: String,
        /** null = toggle, true = turn on, false = turn off */
        val targetState: Boolean? = null
    ) : Instruction()
}

/**
 * Scroll/Swipe direction.
 */
enum class ScrollDirection {
    UP, DOWN, LEFT, RIGHT;

    companion object {
        fun fromString(value: String): ScrollDirection {
            return when (value.lowercase()) {
                "up" -> UP
                "down" -> DOWN
                "left" -> LEFT
                "right" -> RIGHT
                else -> DOWN
            }
        }
    }
}
