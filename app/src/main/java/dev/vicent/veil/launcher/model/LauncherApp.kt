package dev.vicent.veil.launcher.model

import android.content.ComponentName
import android.graphics.drawable.Drawable

data class LauncherApp(
    val packageName: String,
    val label: String,
    val componentName: ComponentName,
    val icon: Drawable? = null,
    val category: AppCategory = AppCategory.GENERAL,
)

enum class AppCategory {
    WORK,
    MEDIA,
    SOCIAL,
    GENERAL,
}
