package com.testzen.core.locator.smart

import org.slf4j.LoggerFactory

/**
 * Scores elements based on text match quality and action compatibility.
 *
 * Uses multiple factors to determine the best element for an action:
 * - Text/label matching (exact, partial, fuzzy)
 * - Action compatibility (traits match)
 * - Element prominence (size, position)
 * - Accessibility attributes
 *
 * Single Responsibility: Element scoring and ranking.
 */
class ElementScorer {
    private val logger = LoggerFactory.getLogger(ElementScorer::class.java)

    /**
     * Score a candidate for text match quality.
     */
    fun scoreTextMatch(candidate: ElementCandidate, targetText: String): Double {
        val normalizedTarget = normalizeText(targetText)
        if (normalizedTarget.isEmpty()) return 0.5 // Neutral score for empty target

        var maxScore = 0.0

        // Score against main text
        val textScore = calculateTextMatchScore(candidate.text, normalizedTarget)
        maxScore = maxOf(maxScore, textScore)

        // Score against accessibility ID
        candidate.accessibilityId?.let {
            val accessibilityScore = calculateTextMatchScore(it, normalizedTarget)
            maxScore = maxOf(maxScore, accessibilityScore * 0.95) // Slight penalty for non-visible match
        }

        // Score against resource ID
        candidate.resourceId?.let {
            val resourceScore = calculateResourceIdScore(it, normalizedTarget)
            maxScore = maxOf(maxScore, resourceScore * 0.9)
        }

        // Score against class name (for type matching)
        val classScore = calculateClassNameScore(candidate.className, normalizedTarget)
        maxScore = maxOf(maxScore, classScore * 0.7)

        return maxScore
    }

    /**
     * Score a candidate for action compatibility.
     */
    fun scoreActionCompatibility(candidate: ElementCandidate, action: ActionType): Double {
        return action.compatibilityScore(candidate.traits)
    }

    /**
     * Calculate comprehensive score for a candidate.
     */
    fun calculateScore(
        candidate: ElementCandidate,
        targetText: String,
        action: ActionType
    ): ElementCandidate {
        val textMatch = scoreTextMatch(candidate, targetText)
        val actionCompat = scoreActionCompatibility(candidate, action)
        val visibility = candidate.visibilityScore

        return candidate.withScores(
            textMatch = textMatch,
            actionCompatibility = actionCompat,
            spatial = candidate.spatialScore,
            visibility = visibility
        )
    }

    /**
     * Rank candidates by overall score.
     */
    fun rankCandidates(
        candidates: List<ElementCandidate>,
        targetText: String,
        action: ActionType
    ): List<ElementCandidate> {
        return candidates
            .map { calculateScore(it, targetText, action) }
            .sortedByDescending { it.overallScore }
    }

    /**
     * Find the best candidate for an action.
     */
    fun findBestCandidate(
        candidates: List<ElementCandidate>,
        targetText: String,
        action: ActionType,
        minimumScore: Double = 0.3
    ): ElementCandidate? {
        val ranked = rankCandidates(candidates, targetText, action)
        return ranked.firstOrNull { it.overallScore >= minimumScore }
    }

    /**
     * Filter candidates that can support the action.
     */
    fun filterActionCompatible(
        candidates: List<ElementCandidate>,
        action: ActionType
    ): List<ElementCandidate> {
        return candidates.filter { it.supportsAction(action) }
    }

    // ═══════════════════════════════════════════════════════════════
    // TEXT MATCHING
    // ═══════════════════════════════════════════════════════════════

    private fun calculateTextMatchScore(text: String?, target: String): Double {
        if (text.isNullOrBlank()) return 0.0

        val normalizedText = normalizeText(text)
        if (normalizedText.isEmpty()) return 0.0

        // Exact match
        if (normalizedText == target) return 1.0

        // Case-insensitive exact match
        if (normalizedText.equals(target, ignoreCase = true)) return 0.98

        // Contains (text contains target)
        if (normalizedText.contains(target, ignoreCase = true)) {
            val ratio = target.length.toDouble() / normalizedText.length
            return 0.7 + (ratio * 0.25) // 0.7 to 0.95 based on how much of text is the target
        }

        // Contains (target contains text)
        if (target.contains(normalizedText, ignoreCase = true)) {
            val ratio = normalizedText.length.toDouble() / target.length
            return 0.6 + (ratio * 0.2) // 0.6 to 0.8
        }

        // Word match (any word matches)
        val textWords = normalizedText.split(Regex("\\s+")).filter { it.isNotBlank() }
        val targetWords = target.split(Regex("\\s+")).filter { it.isNotBlank() }

        val matchingWords = textWords.count { textWord ->
            targetWords.any { it.equals(textWord, ignoreCase = true) }
        }

        if (matchingWords > 0) {
            val wordMatchRatio = matchingWords.toDouble() / maxOf(textWords.size, targetWords.size)
            return 0.5 + (wordMatchRatio * 0.3) // 0.5 to 0.8
        }

        // Fuzzy match using Levenshtein distance
        val distance = levenshteinDistance(normalizedText.lowercase(), target.lowercase())
        val maxLen = maxOf(normalizedText.length, target.length)
        val similarity = 1.0 - (distance.toDouble() / maxLen)

        return if (similarity > 0.6) similarity * 0.5 else 0.0 // Only count if reasonably similar
    }

    private fun calculateResourceIdScore(resourceId: String, target: String): Double {
        val normalizedId = normalizeResourceId(resourceId)
        val normalizedTarget = target.replace(" ", "_").replace("-", "_").lowercase()

        // Exact match on extracted ID
        if (normalizedId == normalizedTarget) return 1.0

        // Contains match
        if (normalizedId.contains(normalizedTarget, ignoreCase = true)) {
            return 0.85
        }

        // Partial word match
        val idParts = normalizedId.split("_", ".")
        val targetParts = normalizedTarget.split("_", " ")

        val matchingParts = idParts.count { idPart ->
            targetParts.any { it.equals(idPart, ignoreCase = true) }
        }

        if (matchingParts > 0) {
            return 0.6 + (matchingParts.toDouble() / maxOf(idParts.size, targetParts.size) * 0.3)
        }

        return 0.0
    }

    private fun calculateClassNameScore(className: String, target: String): Double {
        val normalizedClass = className.lowercase()
        val normalizedTarget = target.lowercase()

        // Check if class name suggests the element type matches target context
        val typeKeywords = mapOf(
            "button" to listOf("button", "btn", "submit", "click", "tap"),
            "text" to listOf("text", "label", "title", "header"),
            "input" to listOf("input", "field", "edit", "entry", "textfield"),
            "image" to listOf("image", "icon", "img", "picture", "photo"),
            "checkbox" to listOf("check", "checkbox", "tick"),
            "switch" to listOf("switch", "toggle"),
            "dropdown" to listOf("dropdown", "select", "picker", "spinner")
        )

        for ((elementType, keywords) in typeKeywords) {
            if (normalizedClass.contains(elementType)) {
                if (keywords.any { normalizedTarget.contains(it) }) {
                    return 0.5
                }
            }
        }

        return 0.0
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════

    private fun normalizeText(text: String): String {
        return text.trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-zA-Z0-9\\s]"), "")
    }

    private fun normalizeResourceId(resourceId: String): String {
        // Extract the ID part from "com.package:id/element_id"
        val parts = resourceId.split("/")
        val id = parts.lastOrNull() ?: resourceId

        return id.replace(Regex("[^a-zA-Z0-9_]"), "").lowercase()
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length

        if (m == 0) return n
        if (n == 0) return m

        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[m][n]
    }
}

/**
 * Configuration for element scoring.
 */
data class ScoringConfig(
    /** Weight for text match in overall score */
    val textMatchWeight: Double = 0.35,

    /** Weight for action compatibility */
    val actionCompatWeight: Double = 0.30,

    /** Weight for spatial relevance */
    val spatialWeight: Double = 0.20,

    /** Weight for visibility/prominence */
    val visibilityWeight: Double = 0.15,

    /** Minimum score to consider a match */
    val minimumScore: Double = 0.3,

    /** Enable fuzzy text matching */
    val enableFuzzyMatch: Boolean = true,

    /** Maximum Levenshtein distance for fuzzy match */
    val maxFuzzyDistance: Int = 3
)
