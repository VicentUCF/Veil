package dev.vicent.veil.ui

internal fun circularPagerPageCount(contextCount: Int): Int = when {
    contextCount <= 0 -> 0
    contextCount == 1 -> 1
    else -> contextCount + 2
}

internal fun contextIndexForPagerPage(page: Int, contextCount: Int): Int {
    require(contextCount > 0) { "At least one context is required" }
    if (contextCount == 1) return 0

    return when (page) {
        0 -> contextCount - 1
        contextCount + 1 -> 0
        else -> (page - 1).coerceIn(0, contextCount - 1)
    }
}

internal fun canonicalPagerPage(contextIndex: Int, contextCount: Int): Int {
    require(contextIndex in 0 until contextCount) { "Context index is out of bounds" }
    return if (contextCount == 1) 0 else contextIndex + 1
}

internal fun canonicalPageForBoundary(page: Int, contextCount: Int): Int {
    if (contextCount <= 1) return 0
    return when (page) {
        0 -> contextCount
        contextCount + 1 -> 1
        else -> page
    }
}

internal fun circularStepDirections(
    fromContextIndex: Int,
    toContextIndex: Int,
    contextCount: Int,
): List<Int> {
    require(contextCount > 0) { "At least one context is required" }
    require(fromContextIndex in 0 until contextCount) { "Source context is out of bounds" }
    require(toContextIndex in 0 until contextCount) { "Target context is out of bounds" }

    val forwardSteps = (toContextIndex - fromContextIndex).floorMod(contextCount)
    val backwardSteps = (fromContextIndex - toContextIndex).floorMod(contextCount)
    val direction = if (forwardSteps <= backwardSteps) 1 else -1
    val stepCount = minOf(forwardSteps, backwardSteps)
    return List(stepCount) { direction }
}

private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus
