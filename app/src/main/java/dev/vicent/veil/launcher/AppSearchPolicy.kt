package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.AppSearchLearningEntry
import java.text.Normalizer
import java.util.Locale
import kotlin.math.max

internal data class AppSearchCandidate(
    val packageName: String,
    val label: String,
    val sourceIndex: Int,
)

internal object AppSearchPolicy {
    fun rank(
        candidates: List<AppSearchCandidate>,
        rawQuery: String,
        learning: List<AppSearchLearningEntry>,
        nowMillis: Long,
    ): List<AppSearchCandidate> {
        val query = normalizeSearchText(rawQuery)
        if (query.isBlank()) return candidates.sortedBy(AppSearchCandidate::sourceIndex)

        val terms = query.split(' ').filter(String::isNotBlank)
        val activeLearning = learning.filter {
            it.lastSelectedAtMillis <= nowMillis &&
                nowMillis - it.lastSelectedAtMillis <= SearchLearningPolicy.RETENTION_MILLIS
        }
        val globalScores = activeLearning.groupBy(AppSearchLearningEntry::packageName)
            .mapValues { (_, entries) -> entries.sumOf { it.decayedScore(nowMillis) } }
        val globalRecency = activeLearning.groupBy(AppSearchLearningEntry::packageName)
            .mapValues { (_, entries) -> entries.maxOf(AppSearchLearningEntry::lastSelectedAtMillis) }

        return candidates.mapNotNull { candidate ->
            val label = normalizeSearchText(candidate.label)
            val packageName = normalizeSearchText(candidate.packageName)
            val labelWords = label.split(SEARCH_WORD_BOUNDARIES).filter(String::isNotBlank)
            val directAssociations = activeLearning.filter {
                it.packageName == candidate.packageName && it.query == query
            }
            val compatibleAssociations = activeLearning.filter {
                it.packageName == candidate.packageName &&
                    it.query != query &&
                    queriesAreCompatible(query, it.query)
            }
            val textRank = textualRank(
                query = query,
                terms = terms,
                label = label,
                labelWords = labelWords,
                packageName = packageName,
            )
            val primaryRank = when {
                label == query -> Rank.EXACT_LABEL
                directAssociations.isNotEmpty() -> Rank.DIRECT_ASSOCIATION
                compatibleAssociations.isNotEmpty() -> Rank.COMPATIBLE_ASSOCIATION
                textRank != null -> textRank
                else -> return@mapNotNull null
            }
            val relevantAssociations = if (directAssociations.isNotEmpty()) {
                directAssociations
            } else {
                compatibleAssociations
            }
            RankedCandidate(
                candidate = candidate,
                primaryRank = primaryRank,
                associationScore = relevantAssociations.sumOf { it.decayedScore(nowMillis) },
                associationRecency = relevantAssociations.maxOfOrNull {
                    it.lastSelectedAtMillis
                } ?: Long.MIN_VALUE,
                globalScore = globalScores[candidate.packageName] ?: 0.0,
                globalRecency = globalRecency[candidate.packageName] ?: Long.MIN_VALUE,
                normalizedLabel = label,
            )
        }.sortedWith(
            compareBy<RankedCandidate> { it.primaryRank }
                .thenByDescending { it.associationScore }
                .thenByDescending { it.associationRecency }
                .thenByDescending { it.globalScore }
                .thenByDescending { it.globalRecency }
                .thenBy { it.normalizedLabel }
                .thenBy { it.candidate.packageName },
        ).map(RankedCandidate::candidate)
    }

    fun matches(rawQuery: String, searchable: String): Boolean {
        val query = normalizeSearchText(rawQuery)
        if (query.isBlank()) return true
        val normalized = normalizeSearchText(searchable)
        val words = normalized.split(SEARCH_WORD_BOUNDARIES).filter(String::isNotBlank)
        return textualRank(
            query = query,
            terms = query.split(' ').filter(String::isNotBlank),
            label = normalized,
            labelWords = words,
            packageName = normalized,
        ) != null
    }

    private fun textualRank(
        query: String,
        terms: List<String>,
        label: String,
        labelWords: List<String>,
        packageName: String,
    ): Int? = when {
        label == query -> Rank.EXACT_LABEL
        label.startsWith(query) -> Rank.LABEL_PREFIX
        terms.all { term -> labelWords.any { it.startsWith(term) } } -> Rank.WORD_PREFIX
        terms.all(label::contains) -> Rank.LABEL_CONTAINS
        terms.all(packageName::contains) -> Rank.PACKAGE_CONTAINS
        terms.all { term ->
            term.length >= MIN_FUZZY_TERM_LENGTH &&
                labelWords.any { word -> isWithinOneEdit(term, word) }
        } -> Rank.FUZZY_LABEL
        else -> null
    }

    private fun queriesAreCompatible(first: String, second: String): Boolean =
        minOf(first.length, second.length) >= MIN_COMPATIBLE_QUERY_LENGTH &&
            (first.startsWith(second) || second.startsWith(first))

    private fun AppSearchLearningEntry.decayedScore(nowMillis: Long): Double {
        val ageDays = max(0L, nowMillis - lastSelectedAtMillis).toDouble() / DAY_MILLIS
        return selectionCount.coerceAtLeast(1) / (1.0 + ageDays / RECENCY_HALF_WEIGHT_DAYS)
    }

    private fun isWithinOneEdit(first: String, second: String): Boolean {
        if (kotlin.math.abs(first.length - second.length) > 1) return false
        var firstIndex = 0
        var secondIndex = 0
        var edits = 0
        while (firstIndex < first.length && secondIndex < second.length) {
            if (first[firstIndex] == second[secondIndex]) {
                firstIndex += 1
                secondIndex += 1
            } else {
                edits += 1
                if (edits > 1) return false
                when {
                    first.length > second.length -> firstIndex += 1
                    second.length > first.length -> secondIndex += 1
                    else -> {
                        firstIndex += 1
                        secondIndex += 1
                    }
                }
            }
        }
        if (firstIndex < first.length || secondIndex < second.length) edits += 1
        return edits <= 1
    }

    private data class RankedCandidate(
        val candidate: AppSearchCandidate,
        val primaryRank: Int,
        val associationScore: Double,
        val associationRecency: Long,
        val globalScore: Double,
        val globalRecency: Long,
        val normalizedLabel: String,
    )

    private object Rank {
        const val EXACT_LABEL = 0
        const val DIRECT_ASSOCIATION = 1
        const val COMPATIBLE_ASSOCIATION = 2
        const val LABEL_PREFIX = 3
        const val WORD_PREFIX = 4
        const val LABEL_CONTAINS = 5
        const val PACKAGE_CONTAINS = 6
        const val FUZZY_LABEL = 7
    }

    private const val MIN_FUZZY_TERM_LENGTH = 4
    private const val MIN_COMPATIBLE_QUERY_LENGTH = 2
    private const val DAY_MILLIS = 24.0 * 60.0 * 60.0 * 1_000.0
    private const val RECENCY_HALF_WEIGHT_DAYS = 14.0
    private val SEARCH_WORD_BOUNDARIES = Regex("[^\\p{L}\\p{N}]+")
}

internal fun normalizeSearchText(value: String): String = Normalizer
    .normalize(value, Normalizer.Form.NFD)
    .replace(COMBINING_MARKS, "")
    .lowercase(Locale.ROOT)
    .trim()
    .replace(WHITESPACE, " ")

private val COMBINING_MARKS = Regex("\\p{M}+")
private val WHITESPACE = Regex("\\s+")
