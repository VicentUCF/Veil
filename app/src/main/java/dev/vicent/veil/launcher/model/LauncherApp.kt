package dev.vicent.veil.launcher.model

import android.content.ComponentName
import android.graphics.Bitmap

class LauncherAppIcon internal constructor(
    internal val bitmap: Bitmap,
)

data class LauncherApp(
    val packageName: String,
    val label: String,
    val componentName: ComponentName,
    val icon: LauncherAppIcon? = null,
    val category: AppCategory = AppCategory.GENERAL,
)

enum class AppCategory {
    WORK,
    MEDIA,
    GAME,
    GENERAL,
}
