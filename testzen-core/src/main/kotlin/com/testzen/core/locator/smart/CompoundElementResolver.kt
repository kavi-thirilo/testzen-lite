package com.testzen.core.locator.smart

import org.slf4j.LoggerFactory

/**
 * Resolves compound UI patterns into actionable element sequences.
 *
 * Handles complex UI patterns such as:
 * - Date pickers (separate dropdowns for month/day/year)
 * - Time pickers (hour/minute/AM-PM)
 * - Multi-part forms (address with street/city/zip)
 * - Radio button groups
 * - Checkbox lists
 * - Segmented controls
 *
 * Single Responsibility: Compound UI pattern recognition and resolution.
 */
class CompoundElementResolver(
    private val spatialAnalyzer: SpatialAnalyzer,
    private val scorer: ElementScorer
) {
    private val logger = LoggerFactory.getLogger(CompoundElementResolver::class.java)

    /**
     * Resolve a compound action like "set date to Jan 21 2026".
     *
     * @param instruction The action instruction (e.g., "set date jan 21 2026")
     * @param candidates Available UI elements
     * @param action The intended action type
     * @return List of resolved actions to perform
     */
    fun resolveCompoundAction(
        instruction: String,
        candidates: List<ElementCandidate>,
        action: ActionType
    ): List<ResolvedAction> {
        val normalizedInstruction = instruction.lowercase().trim()

        // Try to identify the compound pattern
        return when {
            isDatePattern(normalizedInstruction) -> resolveDatePicker(normalizedInstruction, candidates)
            isTimePattern(normalizedInstruction) -> resolveTimePicker(normalizedInstruction, candidates)
            isOptionSelectionPattern(normalizedInstruction) -> resolveOptionSelection(normalizedInstruction, candidates)
            isMultiSelectPattern(normalizedInstruction) -> resolveMultiSelect(normalizedInstruction, candidates)
            else -> emptyList()
        }
    }

    /**
     * Detect if this might be a compound element scenario.
     */
    fun isCompoundScenario(instruction: String): Boolean {
        val normalized = instruction.lowercase()
        return isDatePattern(normalized) ||
                isTimePattern(normalized) ||
                isOptionSelectionPattern(normalized) ||
                isMultiSelectPattern(normalized)
    }

    /**
     * Find compound element groups in candidates.
     */
    fun findCompoundGroups(candidates: List<ElementCandidate>): List<CompoundGroup> {
        val groups = mutableListOf<CompoundGroup>()

        // Find dropdown groups (elements in same row with similar traits)
        val dropdowns = candidates.filter { ElementTrait.DROPDOWN in it.traits }
        val groupedDropdowns = groupByRow(dropdowns)

        for ((_, rowDropdowns) in groupedDropdowns) {
            if (rowDropdowns.size >= 2) {
                groups.add(
                    CompoundGroup(
                        type = CompoundType.DROPDOWN_GROUP,
                        elements = rowDropdowns,
                        layout = GroupLayout.HORIZONTAL
                    )
                )
            }
        }

        // Find checkbox groups
        val checkboxes = candidates.filter { ElementTrait.CHECKABLE in it.traits }
        val groupedCheckboxes = groupByColumn(checkboxes)

        for ((_, colCheckboxes) in groupedCheckboxes) {
            if (colCheckboxes.size >= 2) {
                groups.add(
                    CompoundGroup(
                        type = CompoundType.CHECKBOX_GROUP,
                        elements = colCheckboxes,
                        layout = GroupLayout.VERTICAL
                    )
                )
            }
        }

        // Find radio button groups (mutually exclusive checkables)
        val radioButtons = checkboxes.filter {
            it.className.lowercase().contains("radio") ||
                    it.className.lowercase().contains("option")
        }
        if (radioButtons.size >= 2) {
            groups.add(
                CompoundGroup(
                    type = CompoundType.RADIO_GROUP,
                    elements = radioButtons.sortedBy { it.bounds.y }
                )
            )
        }

        return groups
    }

    // ═══════════════════════════════════════════════════════════════
    // DATE PICKER RESOLUTION
    // ═══════════════════════════════════════════════════════════════

    private fun isDatePattern(text: String): Boolean {
        // Match patterns like "jan 21 2026", "01/21/2026", "21-01-2026", "set date"
        val patterns = listOf(
            Regex("""(?:set|select|choose|pick)\s+date"""),
            Regex("""(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\s+\d{1,2}[,\s]+\d{4}"""),
            Regex("""\d{1,2}[/\-]\d{1,2}[/\-]\d{2,4}"""),
            Regex("""\d{4}[/\-]\d{1,2}[/\-]\d{1,2}""")
        )
        return patterns.any { it.containsMatchIn(text) }
    }

    private fun resolveDatePicker(
        instruction: String,
        candidates: List<ElementCandidate>
    ): List<ResolvedAction> {
        val actions = mutableListOf<ResolvedAction>()

        // Parse date components from instruction
        val dateComponents = parseDateComponents(instruction)

        // Find date picker elements
        val dateDropdowns = candidates.filter {
            ElementTrait.DROPDOWN in it.traits || ElementTrait.CLICKABLE in it.traits
        }.filter { candidate ->
            val text = candidate.text.lowercase()
            val id = (candidate.accessibilityId ?: candidate.resourceId ?: "").lowercase()

            text.contains("month") || text.contains("day") || text.contains("year") ||
                    id.contains("month") || id.contains("day") || id.contains("year") ||
                    text.contains("date") || id.contains("date")
        }

        // Group by row to find date picker components
        val dateGroup = if (dateDropdowns.isNotEmpty()) {
            dateDropdowns.sortedBy { it.bounds.x }
        } else {
            // Fallback: find any grouped dropdowns
            val groups = findCompoundGroups(candidates)
            groups.find { it.type == CompoundType.DROPDOWN_GROUP }?.elements ?: emptyList()
        }

        if (dateGroup.size >= 3) {
            // Assume month/day/year order (common US format)
            // Or detect by labels/hints
            val (monthElement, dayElement, yearElement) = identifyDateComponents(dateGroup)

            dateComponents.month?.let { month ->
                monthElement?.let {
                    actions.add(ResolvedAction(ActionType.SELECT, it, month))
                }
            }

            dateComponents.day?.let { day ->
                dayElement?.let {
                    actions.add(ResolvedAction(ActionType.SELECT, it, day))
                }
            }

            dateComponents.year?.let { year ->
                yearElement?.let {
                    actions.add(ResolvedAction(ActionType.SELECT, it, year))
                }
            }
        } else if (dateGroup.size == 1) {
            // Single date picker - click to open then select
            actions.add(
                ResolvedAction(
                    ActionType.CLICK,
                    dateGroup.first(),
                    dateComponents.toString()
                )
            )
        }

        return actions
    }

    private fun parseDateComponents(instruction: String): DateComponents {
        var month: String? = null
        var day: String? = null
        var year: String? = null

        // Try month name
        val monthNames = mapOf(
            "jan" to "January", "feb" to "February", "mar" to "March",
            "apr" to "April", "may" to "May", "jun" to "June",
            "jul" to "July", "aug" to "August", "sep" to "September",
            "oct" to "October", "nov" to "November", "dec" to "December"
        )

        for ((abbr, full) in monthNames) {
            if (instruction.contains(abbr, ignoreCase = true)) {
                month = full
                break
            }
        }

        // Extract numbers
        val numbers = Regex("""\d+""").findAll(instruction).map { it.value }.toList()

        for (num in numbers) {
            val value = num.toIntOrNull() ?: continue
            when {
                value in 1..12 && month == null -> month = value.toString()
                value in 1..31 && day == null -> day = value.toString()
                value in 1900..2100 -> year = value.toString()
                value in 0..99 -> year = (2000 + value).toString()
            }
        }

        return DateComponents(month, day, year)
    }

    private fun identifyDateComponents(
        elements: List<ElementCandidate>
    ): Triple<ElementCandidate?, ElementCandidate?, ElementCandidate?> {
        var month: ElementCandidate? = null
        var day: ElementCandidate? = null
        var year: ElementCandidate? = null

        for (element in elements) {
            val text = element.text.lowercase()
            val id = (element.accessibilityId ?: element.resourceId ?: "").lowercase()

            when {
                text.contains("month") || id.contains("month") || text.contains("jan") -> month = element
                text.contains("day") || id.contains("day") -> day = element
                text.contains("year") || id.contains("year") || text.matches(Regex("\\d{4}")) -> year = element
            }
        }

        // If not identified by labels, use position (left to right: month, day, year)
        if (month == null && day == null && year == null && elements.size >= 3) {
            month = elements[0]
            day = elements[1]
            year = elements[2]
        }

        return Triple(month, day, year)
    }

    // ═══════════════════════════════════════════════════════════════
    // TIME PICKER RESOLUTION
    // ═══════════════════════════════════════════════════════════════

    private fun isTimePattern(text: String): Boolean {
        val patterns = listOf(
            Regex("""(?:set|select|choose|pick)\s+time"""),
            Regex("""\d{1,2}:\d{2}"""),
            Regex("""\d{1,2}\s*(?:am|pm)""")
        )
        return patterns.any { it.containsMatchIn(text) }
    }

    private fun resolveTimePicker(
        instruction: String,
        candidates: List<ElementCandidate>
    ): List<ResolvedAction> {
        val actions = mutableListOf<ResolvedAction>()

        // Parse time components
        val hourMatch = Regex("""(\d{1,2})(?::|(?=\s*(?:am|pm)))""").find(instruction)
        val minuteMatch = Regex(""":(\d{2})""").find(instruction)
        val periodMatch = Regex("""(am|pm)""", RegexOption.IGNORE_CASE).find(instruction)

        val hour = hourMatch?.groupValues?.get(1)
        val minute = minuteMatch?.groupValues?.get(1) ?: "00"
        val period = periodMatch?.value?.uppercase()

        // Find time picker elements
        val timeElements = candidates.filter { candidate ->
            val text = candidate.text.lowercase()
            val id = (candidate.accessibilityId ?: candidate.resourceId ?: "").lowercase()

            text.contains("hour") || text.contains("minute") || text.contains("am") || text.contains("pm") ||
                    id.contains("hour") || id.contains("minute") || id.contains("time")
        }

        if (timeElements.isNotEmpty()) {
            val sorted = timeElements.sortedBy { it.bounds.x }

            hour?.let { h ->
                sorted.getOrNull(0)?.let { actions.add(ResolvedAction(ActionType.SELECT, it, h)) }
            }

            sorted.getOrNull(1)?.let { actions.add(ResolvedAction(ActionType.SELECT, it, minute)) }

            period?.let { p ->
                sorted.getOrNull(2)?.let { actions.add(ResolvedAction(ActionType.SELECT, it, p)) }
            }
        }

        return actions
    }

    // ═══════════════════════════════════════════════════════════════
    // OPTION SELECTION RESOLUTION
    // ═══════════════════════════════════════════════════════════════

    private fun isOptionSelectionPattern(text: String): Boolean {
        return text.contains("select") && text.contains("from") ||
                text.contains("choose") && text.contains("option") ||
                text.contains("pick") && text.contains("from")
    }

    private fun resolveOptionSelection(
        instruction: String,
        candidates: List<ElementCandidate>
    ): List<ResolvedAction> {
        val actions = mutableListOf<ResolvedAction>()

        // Extract option value and dropdown name
        val optionMatch = Regex("""['"]([^'"]+)['"]""").find(instruction)
        val option = optionMatch?.groupValues?.get(1)

        val fromMatch = Regex("""from\s+(?:the\s+)?['"]?([^'"]+)['"]?""").find(instruction)
        val dropdownName = fromMatch?.groupValues?.get(1)?.trim()

        if (dropdownName != null) {
            // Find the dropdown
            val dropdown = scorer.findBestCandidate(
                candidates.filter { ElementTrait.DROPDOWN in it.traits || ElementTrait.CLICKABLE in it.traits },
                dropdownName,
                ActionType.SELECT
            )

            dropdown?.let {
                // Click to open dropdown
                actions.add(ResolvedAction(ActionType.CLICK, it, "open"))

                // Select option (will be found after dropdown opens)
                option?.let { opt ->
                    actions.add(ResolvedAction(ActionType.SELECT, null, opt, waitForOption = true))
                }
            }
        }

        return actions
    }

    // ═══════════════════════════════════════════════════════════════
    // MULTI-SELECT RESOLUTION
    // ═══════════════════════════════════════════════════════════════

    private fun isMultiSelectPattern(text: String): Boolean {
        return text.contains("check") && (text.contains("and") || text.contains(",")) ||
                text.contains("select all") ||
                text.contains("uncheck all")
    }

    private fun resolveMultiSelect(
        instruction: String,
        candidates: List<ElementCandidate>
    ): List<ResolvedAction> {
        val actions = mutableListOf<ResolvedAction>()

        val checkables = candidates.filter { ElementTrait.CHECKABLE in it.traits }

        if (instruction.contains("select all") || instruction.contains("check all")) {
            checkables.forEach { checkbox ->
                if (ElementTrait.CHECKED !in checkbox.traits) {
                    actions.add(ResolvedAction(ActionType.CHECK, checkbox))
                }
            }
        } else if (instruction.contains("uncheck all") || instruction.contains("deselect all")) {
            checkables.forEach { checkbox ->
                if (ElementTrait.CHECKED in checkbox.traits) {
                    actions.add(ResolvedAction(ActionType.CHECK, checkbox))
                }
            }
        } else {
            // Extract individual items to check
            val items = Regex("""['"]([^'"]+)['"]""")
                .findAll(instruction)
                .map { it.groupValues[1] }
                .toList()

            for (item in items) {
                val match = scorer.findBestCandidate(checkables, item, ActionType.CHECK)
                match?.let { actions.add(ResolvedAction(ActionType.CHECK, it)) }
            }
        }

        return actions
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════

    private fun groupByRow(elements: List<ElementCandidate>): Map<Int, List<ElementCandidate>> {
        val rowThreshold = 30
        return elements.groupBy { (it.bounds.y + it.bounds.height / 2) / rowThreshold }
    }

    private fun groupByColumn(elements: List<ElementCandidate>): Map<Int, List<ElementCandidate>> {
        val colThreshold = 50
        return elements.groupBy { (it.bounds.x + it.bounds.width / 2) / colThreshold }
    }
}

/**
 * Parsed date components.
 */
data class DateComponents(
    val month: String?,
    val day: String?,
    val year: String?
)

/**
 * Resolved action to perform.
 */
data class ResolvedAction(
    val action: ActionType,
    val element: ElementCandidate?,
    val value: String? = null,
    val waitForOption: Boolean = false
)

/**
 * Group of related elements forming a compound UI.
 */
data class CompoundGroup(
    val type: CompoundType,
    val elements: List<ElementCandidate>,
    val layout: GroupLayout = GroupLayout.VERTICAL
)

/**
 * Types of compound UI patterns.
 */
enum class CompoundType {
    DATE_PICKER,
    TIME_PICKER,
    DROPDOWN_GROUP,
    RADIO_GROUP,
    CHECKBOX_GROUP,
    SEGMENTED_CONTROL,
    ADDRESS_FORM,
    STEPPER
}

/**
 * Layout direction of compound group.
 */
enum class GroupLayout {
    HORIZONTAL,
    VERTICAL,
    GRID
}
