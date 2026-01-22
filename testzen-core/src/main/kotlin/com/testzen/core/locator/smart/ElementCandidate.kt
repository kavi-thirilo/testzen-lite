package com.testzen.core.locator.smart

import org.openqa.selenium.Rectangle
import org.openqa.selenium.WebElement

/**
 * Represents a candidate element with scoring information.
 *
 * Wraps a WebElement with metadata about its traits, scores,
 * and relationship to the target action.
 */
data class ElementCandidate(
    /** The actual WebElement */
    val element: WebElement,

    /** Extracted traits for this element */
    val traits: Set<ElementTrait>,

    /** Element's text content */
    val text: String,

    /** Element's accessibility ID */
    val accessibilityId: String?,

    /** Element's resource/element ID */
    val resourceId: String?,

    /** Element's class name */
    val className: String,

    /** Element bounds */
    val bounds: Rectangle,

    /** Text match score (0.0 to 1.0) */
    val textMatchScore: Double = 0.0,

    /** Action compatibility score (0.0 to 1.0) */
    val actionCompatibilityScore: Double = 0.0,

    /** Spatial relevance score (0.0 to 1.0) */
    val spatialScore: Double = 0.0,

    /** Visibility/prominence score (0.0 to 1.0) */
    val visibilityScore: Double = 0.0,

    /** How this candidate was found */
    val findMethod: FindMethod = FindMethod.DIRECT,

    /** Relationship to the original target (for spatial/traversal finds) */
    val relationship: ElementRelationship = ElementRelationship.SELF
) {
    /** Combined overall score */
    val overallScore: Double
        get() = calculateOverallScore()

    /** Check if element supports the given action */
    fun supportsAction(action: ActionType): Boolean {
        return action.isCompatible(traits)
    }

    /** Check if element is interactable */
    val isInteractable: Boolean
        get() = ElementTrait.VISIBLE in traits && ElementTrait.ENABLED in traits

    /** Check if element is clickable */
    val isClickable: Boolean
        get() = ElementTrait.CLICKABLE in traits

    /** Check if element is editable */
    val isEditable: Boolean
        get() = ElementTrait.EDITABLE in traits

    /** Check if element is a container */
    val isContainer: Boolean
        get() = ElementTrait.CONTAINER in traits

    private fun calculateOverallScore(): Double {
        // Weighted combination of scores
        val weights = mapOf(
            "textMatch" to 0.35,
            "actionCompatibility" to 0.30,
            "spatial" to 0.20,
            "visibility" to 0.15
        )

        return (textMatchScore * weights["textMatch"]!!) +
                (actionCompatibilityScore * weights["actionCompatibility"]!!) +
                (spatialScore * weights["spatial"]!!) +
                (visibilityScore * weights["visibility"]!!)
    }

    /**
     * Create a copy with updated scores.
     */
    fun withScores(
        textMatch: Double = this.textMatchScore,
        actionCompatibility: Double = this.actionCompatibilityScore,
        spatial: Double = this.spatialScore,
        visibility: Double = this.visibilityScore
    ): ElementCandidate {
        return copy(
            textMatchScore = textMatch,
            actionCompatibilityScore = actionCompatibility,
            spatialScore = spatial,
            visibilityScore = visibility
        )
    }

    /**
     * Create a copy with relationship info.
     */
    fun withRelationship(
        method: FindMethod,
        rel: ElementRelationship
    ): ElementCandidate {
        return copy(findMethod = method, relationship = rel)
    }

    override fun toString(): String {
        return "ElementCandidate(text='$text', score=${String.format("%.2f", overallScore)}, " +
                "traits=${traits.take(3)}, method=$findMethod)"
    }
}

/**
 * How the element candidate was found.
 */
enum class FindMethod {
    /** Found directly by locator */
    DIRECT,

    /** Found by traversing to parent */
    PARENT_TRAVERSAL,

    /** Found by traversing to child */
    CHILD_TRAVERSAL,

    /** Found by spatial search (nearby element) */
    SPATIAL_SEARCH,

    /** Found by label association */
    LABEL_ASSOCIATION,

    /** Found as part of compound element (date picker, etc.) */
    COMPOUND_RESOLUTION,

    /** Found in dropdown/picker options */
    DROPDOWN_OPTION,

    /** Fallback/best effort */
    FALLBACK
}

/**
 * Relationship of candidate to the original target.
 */
enum class ElementRelationship {
    /** The element itself */
    SELF,

    /** Parent of the target */
    PARENT,

    /** Child of the target */
    CHILD,

    /** Sibling of the target */
    SIBLING,

    /** Spatially adjacent (above) */
    ABOVE,

    /** Spatially adjacent (below) */
    BELOW,

    /** Spatially adjacent (left) */
    LEFT_OF,

    /** Spatially adjacent (right) */
    RIGHT_OF,

    /** Label for the target */
    LABEL_FOR,

    /** Part of same compound element */
    COMPOUND_PART
}

/**
 * Builder for creating ElementCandidate instances.
 */
class ElementCandidateBuilder(
    private val element: WebElement,
    private val adapter: PlatformElementAdapter
) {
    private var textMatchScore: Double = 0.0
    private var actionCompatibilityScore: Double = 0.0
    private var spatialScore: Double = 1.0
    private var visibilityScore: Double = 0.0
    private var findMethod: FindMethod = FindMethod.DIRECT
    private var relationship: ElementRelationship = ElementRelationship.SELF

    fun withTextMatchScore(score: Double) = apply { textMatchScore = score }
    fun withActionCompatibilityScore(score: Double) = apply { actionCompatibilityScore = score }
    fun withSpatialScore(score: Double) = apply { spatialScore = score }
    fun withVisibilityScore(score: Double) = apply { visibilityScore = score }
    fun withFindMethod(method: FindMethod) = apply { findMethod = method }
    fun withRelationship(rel: ElementRelationship) = apply { relationship = rel }

    fun build(): ElementCandidate {
        val traits = adapter.extractTraits(element)
        val bounds = adapter.getBounds(element)

        // Calculate visibility score based on element size and position
        val visScore = if (visibilityScore > 0) visibilityScore else calculateVisibilityScore(bounds, traits)

        return ElementCandidate(
            element = element,
            traits = traits,
            text = adapter.getText(element),
            accessibilityId = adapter.getAccessibilityId(element),
            resourceId = adapter.getResourceId(element),
            className = adapter.getClassName(element),
            bounds = bounds,
            textMatchScore = textMatchScore,
            actionCompatibilityScore = actionCompatibilityScore,
            spatialScore = spatialScore,
            visibilityScore = visScore,
            findMethod = findMethod,
            relationship = relationship
        )
    }

    private fun calculateVisibilityScore(bounds: Rectangle, traits: Set<ElementTrait>): Double {
        var score = 0.0

        // Visible trait
        if (ElementTrait.VISIBLE in traits) score += 0.4

        // Enabled trait
        if (ElementTrait.ENABLED in traits) score += 0.2

        // Size-based score (larger elements are more prominent)
        val area = bounds.width * bounds.height
        score += when {
            area > 10000 -> 0.2
            area > 5000 -> 0.15
            area > 1000 -> 0.1
            area > 100 -> 0.05
            else -> 0.0
        }

        // Position-based score (elements near top-left are often primary)
        val positionScore = 1.0 - (bounds.y / 2000.0).coerceIn(0.0, 0.2)
        score += positionScore

        return score.coerceIn(0.0, 1.0)
    }
}
