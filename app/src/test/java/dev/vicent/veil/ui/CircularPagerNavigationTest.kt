package dev.vicent.veil.ui

import kotlin.test.assertEquals
import org.junit.Test

class CircularPagerNavigationTest {
    @Test
    fun `five contexts use boundary copies`() {
        assertEquals(7, circularPagerPageCount(contextCount = 5))
        assertEquals(
            listOf(4, 0, 1, 2, 3, 4, 0),
            (0 until 7).map { page -> contextIndexForPagerPage(page, contextCount = 5) },
        )
    }

    @Test
    fun `boundary copies return to their canonical pages`() {
        assertEquals(5, canonicalPageForBoundary(page = 0, contextCount = 5))
        assertEquals(1, canonicalPageForBoundary(page = 6, contextCount = 5))
        assertEquals(3, canonicalPageForBoundary(page = 3, contextCount = 5))
    }

    @Test
    fun `current and tools are adjacent in both directions`() {
        assertEquals(listOf(-1), circularStepDirections(0, 4, contextCount = 5))
        assertEquals(listOf(1), circularStepDirections(4, 0, contextCount = 5))
    }

    @Test
    fun `top rail selection follows the shortest circular route`() {
        assertEquals(listOf(-1, -1), circularStepDirections(1, 4, contextCount = 5))
        assertEquals(listOf(1, 1), circularStepDirections(4, 1, contextCount = 5))
        assertEquals(emptyList(), circularStepDirections(2, 2, contextCount = 5))
    }
}
