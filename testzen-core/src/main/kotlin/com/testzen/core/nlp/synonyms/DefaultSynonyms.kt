package com.testzen.core.nlp.synonyms

/**
 * Default synonym definitions using the centralized DSL.
 *
 * All synonyms are organized by category for easy maintenance
 * and extension. Each category groups related synonym sets.
 */
object DefaultSynonyms {

    /**
     * Register all default synonyms in the registry.
     */
    fun registerAll(registry: SynonymRegistry) {
        registerActionVerbs(registry)
        registerStateWords(registry)
        registerPrepositions(registry)
        registerDirections(registry)
        registerTimeUnits(registry)
        registerElementTypes(registry)
    }

    /**
     * Register synonyms for a specific category.
     */
    fun registerCategory(registry: SynonymRegistry, category: SynonymCategory) {
        when (category) {
            SynonymCategory.ACTION_VERBS -> registerActionVerbs(registry)
            SynonymCategory.STATE_WORDS -> registerStateWords(registry)
            SynonymCategory.PREPOSITIONS -> registerPrepositions(registry)
            SynonymCategory.DIRECTIONS -> registerDirections(registry)
            SynonymCategory.TIME_UNITS -> registerTimeUnits(registry)
            SynonymCategory.ELEMENT_TYPES -> registerElementTypes(registry)
            SynonymCategory.CUSTOM -> { /* No default custom synonyms */ }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ACTION VERBS
    // ═══════════════════════════════════════════════════════════════

    private fun registerActionVerbs(registry: SynonymRegistry) {
        registry.register(SynonymCategory.ACTION_VERBS) {

            synonymSet("CLICK") {
                words("click", "tap", "press", "touch", "select", "hit", "push", "activate", "trigger", "invoke")
                description("Click/tap action words")
                aliases("TAP", "PRESS")
            }

            synonymSet("DOUBLE_CLICK") {
                words("double click", "double tap", "double-click", "double-tap", "doubleclick", "doubletap", "dbl click", "dbl tap")
                description("Double click/tap action words")
                aliases("DOUBLE_TAP")
            }

            synonymSet("LONG_PRESS") {
                words("long press", "long-press", "longpress", "hold", "press and hold", "touch and hold", "long tap", "long click")
                description("Long press/hold action words")
                aliases("HOLD", "LONG_TAP")
            }

            synonymSet("ENTER_TEXT") {
                words("enter", "type", "input", "fill", "write", "put", "set", "key in", "insert", "populate")
                description("Text entry action words")
                aliases("TYPE", "INPUT")
            }

            synonymSet("CLEAR") {
                words("clear", "erase", "delete", "remove", "empty", "wipe", "reset")
                description("Clear text action words")
                aliases("ERASE", "DELETE")
            }

            synonymSet("VERIFY") {
                words("verify", "check", "assert", "confirm", "ensure", "validate", "expect", "should", "must", "see", "find")
                description("Verification action words")
                aliases("CHECK", "ASSERT")
            }

            synonymSet("WAIT") {
                words("wait", "pause", "delay", "sleep", "hold on")
                description("Wait action words")
                aliases("PAUSE", "DELAY")
            }

            synonymSet("SCROLL") {
                words("scroll", "drag", "slide", "pan", "move")
                description("Scroll action words")
                aliases("DRAG", "SLIDE")
            }

            synonymSet("SWIPE") {
                words("swipe", "flick", "fling", "sweep")
                description("Swipe action words")
                aliases("FLICK", "FLING")
            }

            synonymSet("BACK") {
                words("back", "return", "previous", "go back", "navigate back", "press back")
                description("Navigate back action words")
                aliases("NAVIGATE_BACK")
            }

            synonymSet("FORWARD") {
                words("forward", "next", "go forward", "navigate forward")
                description("Navigate forward action words")
                aliases("NAVIGATE_FORWARD")
            }

            synonymSet("LAUNCH") {
                words("launch", "open", "start", "run", "execute", "begin")
                description("Launch/open action words")
                aliases("OPEN", "START")
            }

            synonymSet("CLOSE") {
                words("close", "quit", "exit", "terminate", "end", "stop", "kill")
                description("Close action words")
                aliases("QUIT", "EXIT")
            }

            synonymSet("SCREENSHOT") {
                words("screenshot", "capture", "snap", "take screenshot", "take picture", "screen capture", "screencap", "grab screen")
                description("Screenshot action words")
                aliases("CAPTURE", "SNAP")
            }

            synonymSet("SELECT") {
                words("select", "choose", "pick", "opt for", "go with")
                description("Select option action words")
                aliases("CHOOSE", "PICK")
            }

            synonymSet("TOGGLE") {
                words("toggle", "switch", "flip", "turn")
                description("Toggle/switch action words")
                aliases("SWITCH", "FLIP")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // STATE WORDS
    // ═══════════════════════════════════════════════════════════════

    private fun registerStateWords(registry: SynonymRegistry) {
        registry.register(SynonymCategory.STATE_WORDS) {

            synonymSet("DISPLAYED") {
                words("displayed", "visible", "shown", "present", "appears", "appearing", "exists", "found", "seen", "there")
                description("Visibility state words")
                aliases("VISIBLE", "SHOWN")
            }

            synonymSet("NOT_DISPLAYED") {
                words("hidden", "invisible", "gone", "absent", "missing", "disappeared", "not displayed", "not visible", "not shown", "not present")
                description("Hidden state words")
                aliases("HIDDEN", "INVISIBLE")
            }

            synonymSet("ENABLED") {
                words("enabled", "active", "clickable", "tappable", "interactable")
                description("Enabled state words")
                aliases("ACTIVE", "CLICKABLE")
            }

            synonymSet("DISABLED") {
                words("disabled", "inactive", "grayed", "greyed", "not clickable")
                description("Disabled state words")
                aliases("INACTIVE", "GRAYED")
            }

            synonymSet("CHECKED") {
                words("checked", "ticked", "marked", "selected", "on")
                description("Checked state words")
                aliases("TICKED", "MARKED")
            }

            synonymSet("UNCHECKED") {
                words("unchecked", "unticked", "unmarked", "unselected", "off")
                description("Unchecked state words")
                aliases("UNTICKED", "UNMARKED")
            }

            synonymSet("FOCUSED") {
                words("focused", "focus", "has focus", "active focus")
                description("Focused state words")
                aliases("FOCUS")
            }

            synonymSet("EMPTY") {
                words("empty", "blank", "clear", "no text", "no value")
                description("Empty state words")
                aliases("BLANK")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PREPOSITIONS
    // ═══════════════════════════════════════════════════════════════

    private fun registerPrepositions(registry: SynonymRegistry) {
        registry.register(SynonymCategory.PREPOSITIONS) {

            synonymSet("TARGET_PREPOSITIONS") {
                words("on", "the", "a", "an", "in", "into", "at", "to", "from", "with")
                description("Words connecting action to target")
            }

            synonymSet("INPUT_PREPOSITIONS") {
                words("in", "into", "on", "at", "to", "inside", "within")
                description("Words indicating text input destination")
            }

            synonymSet("SOURCE_PREPOSITIONS") {
                words("from", "of", "out of", "within")
                description("Words indicating source")
            }

            synonymSet("COMPARISON_PREPOSITIONS") {
                words("than", "to", "as", "with")
                description("Words for comparisons")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DIRECTIONS
    // ═══════════════════════════════════════════════════════════════

    private fun registerDirections(registry: SynonymRegistry) {
        registry.register(SynonymCategory.DIRECTIONS) {

            synonymSet("UP") {
                words("up", "upward", "upwards", "top", "above")
                description("Upward direction words")
            }

            synonymSet("DOWN") {
                words("down", "downward", "downwards", "bottom", "below")
                description("Downward direction words")
            }

            synonymSet("LEFT") {
                words("left", "leftward", "leftwards")
                description("Leftward direction words")
            }

            synonymSet("RIGHT") {
                words("right", "rightward", "rightwards")
                description("Rightward direction words")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // TIME UNITS
    // ═══════════════════════════════════════════════════════════════

    private fun registerTimeUnits(registry: SynonymRegistry) {
        registry.register(SynonymCategory.TIME_UNITS) {

            synonymSet("SECONDS") {
                words("second", "seconds", "sec", "secs", "s")
                description("Second unit words")
            }

            synonymSet("MILLISECONDS") {
                words("millisecond", "milliseconds", "ms", "millis")
                description("Millisecond unit words")
            }

            synonymSet("MINUTES") {
                words("minute", "minutes", "min", "mins", "m")
                description("Minute unit words")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ELEMENT TYPES
    // ═══════════════════════════════════════════════════════════════

    private fun registerElementTypes(registry: SynonymRegistry) {
        registry.register(SynonymCategory.ELEMENT_TYPES) {

            synonymSet("BUTTON") {
                words("button", "btn", "cta")
                description("Button element words")
            }

            synonymSet("FIELD") {
                words("field", "input", "textbox", "text box", "edittext", "textarea")
                description("Field/input element words")
                aliases("INPUT", "TEXTBOX")
            }

            synonymSet("LINK") {
                words("link", "hyperlink", "anchor", "url")
                description("Link element words")
                aliases("HYPERLINK", "ANCHOR")
            }

            synonymSet("CHECKBOX") {
                words("checkbox", "check box", "checkmark", "tick box")
                description("Checkbox element words")
                aliases("CHECK_BOX")
            }

            synonymSet("RADIO") {
                words("radio", "radio button", "option button")
                description("Radio button element words")
                aliases("RADIO_BUTTON")
            }

            synonymSet("DROPDOWN") {
                words("dropdown", "drop down", "drop-down", "select", "picker", "combobox")
                description("Dropdown element words")
                aliases("SELECT", "PICKER")
            }

            synonymSet("SWITCH") {
                words("switch", "toggle", "slider")
                description("Switch/toggle element words")
                aliases("TOGGLE_SWITCH")
            }

            synonymSet("TAB") {
                words("tab", "tab item", "tab button")
                description("Tab element words")
            }

            synonymSet("MENU") {
                words("menu", "menu item", "menu option", "hamburger")
                description("Menu element words")
            }

            synonymSet("IMAGE") {
                words("image", "img", "picture", "photo", "icon")
                description("Image element words")
                aliases("PICTURE", "ICON")
            }

            synonymSet("TEXT") {
                words("text", "label", "title", "heading", "header", "message")
                description("Text element words")
                aliases("LABEL", "TITLE")
            }
        }
    }
}
