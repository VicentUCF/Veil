package dev.vicent.veil.launcher.repository

import android.content.Context
import androidx.core.content.edit

internal class ContinuityOnboardingStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isSeen(): Boolean = preferences.getBoolean(KEY_ONBOARDING_SEEN, false)

    fun markSeen() {
        preferences.edit { putBoolean(KEY_ONBOARDING_SEEN, true) }
    }

    private companion object {
        const val PREFERENCES_NAME = "veil_notification_access"
        const val KEY_ONBOARDING_SEEN = "onboarding_seen_v1"
    }
}
