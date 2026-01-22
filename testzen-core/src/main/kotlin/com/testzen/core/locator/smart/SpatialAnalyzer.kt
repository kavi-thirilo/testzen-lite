package com.testzen.core.locator.smart

import org.openqa.selenium.Point
import org.openqa.selenium.Rectangle
import org.openqa.selenium.WebElement
import org.slf4j.LoggerFactory
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Analyzes spatial relationships between UI elements.
 *
 * Provides methods to:
 * - Find nearby elements based on direction and proximity
 * - Calculate distance and alignment between elements
 * - Identify element groupings (rows, columns, grids)
 * - Resolve compound UI patterns (date pickers, form fields)
 *
 * Single Responsibility: Spatial analysis of element positions.
 */
class SpatialAnalyzer(
    private val adapter: PlatformElementAdapter
) {
    private val logger = LoggerFactory.getLogger(SpatialAnalyzer::class.java)

    // Distance thresholds for spatial relationships
    companion object {
        /** Maximum distance to consider elements as "nearby" */
        const val NEARBY_THRESHOLD_PX = 200

        /** Maximum distance for elements in same row/column */
        const val ALIGNMENT_THRESHOLD_PX = 30

        /** Distance considered "adjacent" */
        const val ADJACENT_THRESHOLD_PX = 50

        /** Maximum gap between compound element parts */
        const val COMPOUND_GAP_THRESHOLD_PX = 100
    }

    /**
     * Find the relationship between two elements.
     */
    fun getRelationship(from: Rectangle, to: Rectangle): ElementRelationship {
        val fromCenter = getCenter(from)
        val toCenter = getCenter(to)

        val dx = toCenter.x - fromCenter.x
        val dy = toCenter.y - fromCenter.y

        // Check for overlap
        if (rectanglesOverlap(from, to)) {
            return if (contains(from, to)) ElementRelationship.CHILD
            else if (contains(to, from)) ElementRelationship.PARENT
            else ElementRelationship.SIBLING
        }

        // Determine spatial relationship
        val absX = abs(dx)
        val absY = abs(dy)

        return when {
            absY > absX * 1.5 -> if (dy < 0) ElementRelationship.ABOVE else ElementRelationship.BELOW
            absX > absY * 1.5 -> if (dx < 0) ElementRelationship.LEFT_OF else ElementRelationship.RIGHT_OF
            else -> ElementRelationship.SIBLING // Diagonal - treat as sibling
        }
    }

    /**
     * Calculate distance between two element centers.
     */
    fun distanceBetween(a: Rectangle, b: Rectangle): Double {
        val centerA = getCenter(a)
        val centerB = getCenter(b)

        return sqrt(
            (centerB.x - centerA.x).toDouble().pow(2) +
                    (centerB.y - centerA.y).toDouble().pow(2)
        )
    }

    /**
     * Calculate edge-to-edge distance (minimum gap between elements).
     */
    fun edgeDistance(a: Rectangle, b: Rectangle): Double {
        val horizontalGap = when {
            a.x + a.width < b.x -> b.x - (a.x + a.width) // a is left of b
            b.x + b.width < a.x -> a.x - (b.x + b.width) // b is left of a
            else -> 0 // Overlapping horizontally
        }

        val verticalGap = when {
            a.y + a.height < b.y -> b.y - (a.y + a.height) // a is above b
            b.y + b.height < a.y -> a.y - (b.y + b.height) // b is above a
            else -> 0 // Overlapping vertically
        }

        return sqrt(horizontalGap.toDouble().pow(2) + verticalGap.toDouble().pow(2))
    }

    /**
     * Check if two elements are in the same row (horizontally aligned).
     */
    fun areInSameRow(a: Rectangle, b: Rectangle): Boolean {
        val aCenterY = a.y + a.height / 2
        val bCenterY = b.y + b.height / 2
        return abs(aCenterY - bCenterY) <= ALIGNMENT_THRESHOLD_PX
    }

    /**
     * Check if two elements are in the same column (vertically aligned).
     */
    fun areInSameColumn(a: Rectangle, b: Rectangle): Boolean {
        val aCenterX = a.x + a.width / 2
        val bCenterX = b.x + b.width / 2
        return abs(aCenterX - bCenterX) <= ALIGNMENT_THRESHOLD_PX
    }

    /**
     * Find elements spatially related to a reference element.
     *
     * @param reference The reference element bounds
     * @param candidates List of candidate elements to check
     * @param direction Optional direction filter (null = all directions)
     * @param maxDistance Maximum distance to consider
     * @return List of candidates sorted by proximity
     */
    fun findNearbyElements(
        reference: Rectangle,
        candidates: List<ElementCandidate>,
        direction: SpatialDirection? = null,
        maxDistance: Double = NEARBY_THRESHOLD_PX.toDouble()
    ): List<SpatialMatch> {
        val matches = mutableListOf<SpatialMatch>()

        for (candidate in candidates) {
            val candidateBounds = candidate.bounds

            // Skip if same element (overlapping significantly)
            if (rectanglesOverlap(reference, candidateBounds) &&
                overlapPercentage(reference, candidateBounds) > 0.8
            ) {
                continue
            }

            val distance = distanceBetween(reference, candidateBounds)
            if (distance > maxDistance) continue

            val relationship = getRelationship(reference, candidateBounds)

            // Apply direction filter if specified
            if (direction != null && !matchesDirection(relationship, direction)) {
                continue
            }

            // Calculate spatial relevance score
            val proximityScore = 1.0 - (distance / maxDistance)
            val alignmentScore = calculateAlignmentScore(reference, candidateBounds)
            val spatialScore = (proximityScore * 0.6) + (alignmentScore * 0.4)

            matches.add(
                SpatialMatch(
                    candidate = candidate,
                    relationship = relationship,
                    distance = distance,
                    spatialScore = spatialScore
                )
            )
        }

        return matches.sortedByDescending { it.spatialScore }
    }

    /**
     * Find elements that could be labels for a target element.
     *
     * Labels are typically:
     * - Text elements
     * - Positioned above or to the left of the target
     * - Closely aligned with the target
     */
    fun findPotentialLabels(
        target: Rectangle,
        candidates: List<ElementCandidate>
    ): List<SpatialMatch> {
        return candidates
            .filter { ElementTrait.HAS_TEXT in it.traits || ElementTrait.LABEL in it.traits }
            .mapNotNull { candidate ->
                val bounds = candidate.bounds
                val relationship = getRelationship(bounds, target)

                // Labels are typically above or to the left
                if (relationship !in listOf(
                        ElementRelationship.ABOVE,
                        ElementRelationship.LEFT_OF
                    )
                ) {
                    return@mapNotNull null
                }

                val distance = edgeDistance(bounds, target)
                if (distance > ADJACENT_THRESHOLD_PX) return@mapNotNull null

                val alignmentScore = if (relationship == ElementRelationship.ABOVE) {
                    if (areInSameColumn(bounds, target)) 1.0 else 0.5
                } else {
                    if (areInSameRow(bounds, target)) 1.0 else 0.5
                }

                val proximityScore = 1.0 - (distance / ADJACENT_THRESHOLD_PX)
                val spatialScore = (proximityScore * 0.5) + (alignmentScore * 0.5)

                SpatialMatch(
                    candidate = candidate,
                    relationship = ElementRelationship.LABEL_FOR,
                    distance = distance,
                    spatialScore = spatialScore
                )
            }
            .sortedByDescending { it.spatialScore }
    }

    /**
     * Find elements that form a compound group (like date picker parts).
     *
     * Looks for elements that:
     * - Are in the same row or column
     * - Have similar sizes
     * - Are closely spaced
     * - Have similar types/traits
     */
    fun findCompoundGroup(
        reference: ElementCandidate,
        candidates: List<ElementCandidate>
    ): List<ElementCandidate> {
        val refBounds = reference.bounds
        val groupMembers = mutableListOf(reference)

        // Find elements in same row with similar characteristics
        val sameRowCandidates = candidates.filter { candidate ->
            candidate.element != reference.element &&
                    areInSameRow(refBounds, candidate.bounds) &&
                    edgeDistance(refBounds, candidate.bounds) < COMPOUND_GAP_THRESHOLD_PX
        }

        // Check if they have similar traits (suggesting same type of control)
        val refTraitSignature = reference.traits.filter {
            it in listOf(
                ElementTrait.DROPDOWN,
                ElementTrait.INPUT_FIELD,
                ElementTrait.BUTTON,
                ElementTrait.CHECKABLE
            )
        }.toSet()

        for (candidate in sameRowCandidates) {
            val candidateSignature = candidate.traits.filter {
                it in listOf(
                    ElementTrait.DROPDOWN,
                    ElementTrait.INPUT_FIELD,
                    ElementTrait.BUTTON,
                    ElementTrait.CHECKABLE
                )
            }.toSet()

            // Similar control types and similar size
            if (candidateSignature.intersect(refTraitSignature).isNotEmpty() ||
                (candidateSignature.isEmpty() && refTraitSignature.isEmpty())
            ) {
                val sizeDiff = abs(refBounds.width - candidate.bounds.width) +
                        abs(refBounds.height - candidate.bounds.height)
                if (sizeDiff < 100) { // Similar size
                    groupMembers.add(candidate)
                }
            }
        }

        // Sort by x position (left to right)
        return groupMembers.sortedBy { it.bounds.x }
    }

    /**
     * Find input field associated with a label.
     */
    fun findInputForLabel(
        label: ElementCandidate,
        candidates: List<ElementCandidate>
    ): ElementCandidate? {
        val labelBounds = label.bounds

        // Look for input fields below or to the right of the label
        val inputCandidates = candidates
            .filter { ElementTrait.EDITABLE in it.traits || ElementTrait.INPUT_FIELD in it.traits }
            .mapNotNull { candidate ->
                val bounds = candidate.bounds
                val relationship = getRelationship(labelBounds, bounds)

                if (relationship !in listOf(
                        ElementRelationship.BELOW,
                        ElementRelationship.RIGHT_OF
                    )
                ) {
                    return@mapNotNull null
                }

                val distance = edgeDistance(labelBounds, bounds)
                if (distance > ADJACENT_THRESHOLD_PX) return@mapNotNull null

                val alignmentScore = if (relationship == ElementRelationship.BELOW) {
                    if (areInSameColumn(labelBounds, bounds)) 1.0 else 0.5
                } else {
                    if (areInSameRow(labelBounds, bounds)) 1.0 else 0.5
                }

                Pair(candidate, alignmentScore * (1.0 - distance / ADJACENT_THRESHOLD_PX))
            }
            .sortedByDescending { it.second }

        return inputCandidates.firstOrNull()?.first
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════

    private fun getCenter(rect: Rectangle): Point {
        return Point(rect.x + rect.width / 2, rect.y + rect.height / 2)
    }

    private fun rectanglesOverlap(a: Rectangle, b: Rectangle): Boolean {
        return !(a.x + a.width < b.x ||
                b.x + b.width < a.x ||
                a.y + a.height < b.y ||
                b.y + b.height < a.y)
    }

    private fun contains(outer: Rectangle, inner: Rectangle): Boolean {
        return inner.x >= outer.x &&
                inner.y >= outer.y &&
                inner.x + inner.width <= outer.x + outer.width &&
                inner.y + inner.height <= outer.y + outer.height
    }

    private fun overlapPercentage(a: Rectangle, b: Rectangle): Double {
        if (!rectanglesOverlap(a, b)) return 0.0

        val overlapX = maxOf(
            0,
            minOf(a.x + a.width, b.x + b.width) - maxOf(a.x, b.x)
        )
        val overlapY = maxOf(
            0,
            minOf(a.y + a.height, b.y + b.height) - maxOf(a.y, b.y)
        )

        val overlapArea = overlapX * overlapY
        val smallerArea = minOf(a.width * a.height, b.width * b.height)

        return if (smallerArea > 0) overlapArea.toDouble() / smallerArea else 0.0
    }

    private fun calculateAlignmentScore(a: Rectangle, b: Rectangle): Double {
        val rowAlignment = if (areInSameRow(a, b)) 0.5 else 0.0
        val columnAlignment = if (areInSameColumn(a, b)) 0.5 else 0.0
        return rowAlignment + columnAlignment
    }

    private fun matchesDirection(
        relationship: ElementRelationship,
        direction: SpatialDirection
    ): Boolean {
        return when (direction) {
            SpatialDirection.ABOVE -> relationship == ElementRelationship.ABOVE
            SpatialDirection.BELOW -> relationship == ElementRelationship.BELOW
            SpatialDirection.LEFT -> relationship == ElementRelationship.LEFT_OF
            SpatialDirection.RIGHT -> relationship == ElementRelationship.RIGHT_OF
            SpatialDirection.HORIZONTAL -> relationship in listOf(
                ElementRelationship.LEFT_OF,
                ElementRelationship.RIGHT_OF
            )

            SpatialDirection.VERTICAL -> relationship in listOf(
                ElementRelationship.ABOVE,
                ElementRelationship.BELOW
            )

            SpatialDirection.ANY -> true
        }
    }
}

/**
 * Direction for spatial search.
 */
enum class SpatialDirection {
    ABOVE,
    BELOW,
    LEFT,
    RIGHT,
    HORIZONTAL,
    VERTICAL,
    ANY
}

/**
 * Result of spatial element matching.
 */
data class SpatialMatch(
    val candidate: ElementCandidate,
    val relationship: ElementRelationship,
    val distance: Double,
    val spatialScore: Double
)
