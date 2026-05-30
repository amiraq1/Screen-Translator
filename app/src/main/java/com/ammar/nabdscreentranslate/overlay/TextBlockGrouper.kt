package com.ammar.nabdscreentranslate.overlay

import android.graphics.Rect
import android.util.Log
import com.ammar.nabdscreentranslate.ocr.TextBlockResult
import kotlin.math.max
import kotlin.math.min

/**
 * Groups nearby TextBlockResult items into logical paragraphs/clusters
 * before translation, reducing the number of overlay bubbles displayed.
 *
 * Grouping criteria:
 * - Small vertical distance between blocks
 * - Blocks within the same column (similar x-range)
 * - Consecutive lines
 */
class TextBlockGrouper {

    companion object {
        private const val TAG = "NabdScreenTranslate"

        /**
         * Maximum vertical gap (as a fraction of average line height) to consider
         * two blocks as belonging to the same paragraph.
         */
        private const val VERTICAL_GAP_FACTOR = 1.8f

        /**
         * Minimum horizontal overlap ratio to consider blocks in the same column.
         * 0.3 means at least 30% of the narrower block overlaps horizontally.
         */
        private const val HORIZONTAL_OVERLAP_RATIO = 0.3f
    }

    data class BlockGroup(
        val blocks: List<TextBlockResult>,
        val mergedText: String,
        val mergedBoundingBox: Rect?
    )

    /**
     * Groups text blocks into logical clusters based on spatial proximity.
     * Blocks without bounding boxes are each placed in their own group.
     */
    fun group(blocks: List<TextBlockResult>): List<BlockGroup> {
        if (blocks.isEmpty()) return emptyList()

        // Separate blocks with and without positions
        val positioned = blocks.filter { it.boundingBox != null }
        val unpositioned = blocks.filter { it.boundingBox == null }

        if (positioned.isEmpty()) {
            // No positioning info: treat all as one group
            val merged = blocks.joinToString(" ") { it.text }
            return listOf(BlockGroup(blocks, merged, null))
        }

        // Sort by vertical position (top), then by horizontal position
        val sorted = positioned.sortedWith(compareBy({ it.boundingBox!!.top }, { it.boundingBox!!.left }))

        // Compute average line height for threshold calculation
        val avgHeight = sorted.map { it.boundingBox!!.height() }.average().toFloat()
        val maxVerticalGap = (avgHeight * VERTICAL_GAP_FACTOR).toInt()

        // Union-Find grouping
        val parent = IntArray(sorted.size) { it }

        fun find(i: Int): Int {
            var x = i
            while (parent[x] != x) {
                parent[x] = parent[parent[x]]
                x = parent[x]
            }
            return x
        }

        fun union(a: Int, b: Int) {
            val ra = find(a)
            val rb = find(b)
            if (ra != rb) parent[ra] = rb
        }

        // Compare each block with subsequent blocks that could be in the same group
        for (i in sorted.indices) {
            val boxA = sorted[i].boundingBox!!
            for (j in i + 1 until sorted.size) {
                val boxB = sorted[j].boundingBox!!

                // If block B is too far below A, skip (since sorted by top)
                val verticalGap = boxB.top - boxA.bottom
                if (verticalGap > maxVerticalGap) break

                // Check vertical proximity (could overlap or be close)
                val isVerticallyClose = verticalGap <= maxVerticalGap

                // Check horizontal overlap
                val overlapLeft = max(boxA.left, boxB.left)
                val overlapRight = min(boxA.right, boxB.right)
                val overlap = max(0, overlapRight - overlapLeft)
                val narrowerWidth = min(boxA.width(), boxB.width()).toFloat()
                val overlapRatio = if (narrowerWidth > 0) overlap / narrowerWidth else 0f
                val isHorizontallyAligned = overlapRatio >= HORIZONTAL_OVERLAP_RATIO

                // Check if similar column width (within 50% difference)
                val widthRatio = min(boxA.width(), boxB.width()).toFloat() /
                        max(boxA.width(), boxB.width()).toFloat()
                val isSimilarWidth = widthRatio >= 0.5f

                if (isVerticallyClose && (isHorizontallyAligned || isSimilarWidth)) {
                    union(i, j)
                }
            }
        }

        // Collect groups
        val groupMap = mutableMapOf<Int, MutableList<Int>>()
        for (i in sorted.indices) {
            val root = find(i)
            groupMap.getOrPut(root) { mutableListOf() }.add(i)
        }

        val groups = mutableListOf<BlockGroup>()

        for ((_, indices) in groupMap) {
            val groupBlocks = indices.map { sorted[it] }
            val mergedText = groupBlocks.joinToString(" ") { it.text }
            val mergedBox = mergeBoundingBoxes(groupBlocks.mapNotNull { it.boundingBox })
            groups.add(BlockGroup(groupBlocks, mergedText, mergedBox))
        }

        // Add unpositioned blocks as individual groups
        unpositioned.forEach { block ->
            groups.add(BlockGroup(listOf(block), block.text, null))
        }

        Log.d(TAG, "TextBlockGrouper: ${blocks.size} blocks → ${groups.size} groups")
        return groups
    }

    private fun mergeBoundingBoxes(boxes: List<Rect>): Rect? {
        if (boxes.isEmpty()) return null
        var left = Int.MAX_VALUE
        var top = Int.MAX_VALUE
        var right = Int.MIN_VALUE
        var bottom = Int.MIN_VALUE
        for (box in boxes) {
            left = min(left, box.left)
            top = min(top, box.top)
            right = max(right, box.right)
            bottom = max(bottom, box.bottom)
        }
        return Rect(left, top, right, bottom)
    }
}
