package com.ammar.nabdscreentranslate.overlay

import android.util.Log

/**
 * Prioritizes and selects the most important text block groups for inline
 * overlay display when the number of groups exceeds MAX_INLINE_BUBBLES.
 *
 * Priority scoring:
 * - Text length (longer = more important)
 * - Bounding box area (larger = more prominent)
 * - Position (center of screen = higher priority)
 * - Penalize tiny text near screen edges (top/bottom)
 */
class BubblePrioritizer(
    private val screenWidth: Int,
    private val screenHeight: Int
) {

    companion object {
        private const val TAG = "NabdScreenTranslate"
        const val MAX_INLINE_BUBBLES = 7
    }

    data class PrioritizedResult(
        /** Groups shown as inline bubbles (top priority, max MAX_INLINE_BUBBLES) */
        val inlineBubbles: List<TextBlockGrouper.BlockGroup>,
        /** Groups overflow to bottom sheet */
        val overflowGroups: List<TextBlockGrouper.BlockGroup>
    )

    /**
     * Scores and selects groups. If total groups <= MAX_INLINE_BUBBLES, all go inline.
     * Otherwise, top-scoring groups go inline, the rest go to overflow.
     */
    fun prioritize(groups: List<TextBlockGrouper.BlockGroup>): PrioritizedResult {
        if (groups.size <= MAX_INLINE_BUBBLES) {
            Log.d(TAG, "BubblePrioritizer: ${groups.size} groups ≤ $MAX_INLINE_BUBBLES, all inline")
            return PrioritizedResult(inlineBubbles = groups, overflowGroups = emptyList())
        }

        // Score each group
        val scored = groups.map { group -> group to computeScore(group) }
            .sortedByDescending { it.second }

        val inline = scored.take(MAX_INLINE_BUBBLES).map { it.first }
        val overflow = scored.drop(MAX_INLINE_BUBBLES).map { it.first }

        Log.d(TAG, "BubblePrioritizer: ${groups.size} groups → $MAX_INLINE_BUBBLES inline, ${overflow.size} overflow")
        return PrioritizedResult(inlineBubbles = inline, overflowGroups = overflow)
    }

    private fun computeScore(group: TextBlockGrouper.BlockGroup): Float {
        var score = 0f

        // Text length score (0-40 points)
        val textLen = group.mergedText.length.toFloat()
        score += (textLen / 10f).coerceAtMost(40f)

        // Bounding box area score (0-30 points)
        group.mergedBoundingBox?.let { box ->
            val area = box.width().toFloat() * box.height().toFloat()
            val screenArea = screenWidth.toFloat() * screenHeight.toFloat()
            val areaRatio = area / screenArea
            score += (areaRatio * 300f).coerceAtMost(30f)
        }

        // Center position bonus (0-20 points)
        group.mergedBoundingBox?.let { box ->
            val centerY = box.centerY().toFloat()
            val centerX = box.centerX().toFloat()

            // Vertical: prefer center (20%-80% of screen height)
            val vCenter = screenHeight / 2f
            val vDist = kotlin.math.abs(centerY - vCenter) / vCenter
            val vScore = (1f - vDist).coerceIn(0f, 1f) * 15f
            score += vScore

            // Horizontal: slight bonus for center
            val hCenter = screenWidth / 2f
            val hDist = kotlin.math.abs(centerX - hCenter) / hCenter
            val hScore = (1f - hDist).coerceIn(0f, 1f) * 5f
            score += hScore
        }

        // Penalty for extreme top/bottom (status bar, nav bar area)
        group.mergedBoundingBox?.let { box ->
            val topThreshold = screenHeight * 0.08f
            val bottomThreshold = screenHeight * 0.92f
            if (box.centerY() < topThreshold || box.centerY() > bottomThreshold) {
                score -= 15f
            }
        }

        // Penalty for very short text (even if passed noise filter)
        if (group.mergedText.length < 10) {
            score -= 10f
        }

        return score
    }
}
