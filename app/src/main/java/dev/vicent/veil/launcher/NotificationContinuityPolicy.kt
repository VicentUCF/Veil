package dev.vicent.veil.launcher

enum class NotificationContinuityKind { NAVIGATION, PROGRESS }

object NotificationContinuityPolicy {
    private val privateOrDistractingCategories = setOf(
        "call",
        "msg",
        "alarm",
        "email",
        "social",
    )

    fun classify(category: String?, progressMax: Int): NotificationContinuityKind? = when {
        category in privateOrDistractingCategories -> null
        category == "navigation" -> NotificationContinuityKind.NAVIGATION
        progressMax > 0 || category == "progress" -> NotificationContinuityKind.PROGRESS
        else -> null
    }
}
