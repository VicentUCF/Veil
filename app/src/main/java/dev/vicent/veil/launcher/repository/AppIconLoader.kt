package dev.vicent.veil.launcher.repository

import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import dev.vicent.veil.launcher.model.LauncherAppIcon
import kotlin.math.min

internal class AppIconLoader(
    private val packageManager: PackageManager,
    private val iconSizePx: Int,
) {
    private val cache = object : LinkedHashMap<ComponentName, CachedIcon>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ComponentName, CachedIcon>?): Boolean =
            size > MAX_CACHED_ICONS
    }

    fun load(drawable: Drawable): LauncherAppIcon {
        val mutableBitmap = createBitmap(iconSizePx, iconSizePx)
        val canvas = Canvas(mutableBitmap)
        val renderedDrawable = drawable.constantState?.newDrawable()?.mutate() ?: drawable.mutate()
        val intrinsicWidth = renderedDrawable.intrinsicWidth.coerceAtLeast(1)
        val intrinsicHeight = renderedDrawable.intrinsicHeight.coerceAtLeast(1)
        val scale = min(
            iconSizePx.toFloat() / intrinsicWidth,
            iconSizePx.toFloat() / intrinsicHeight,
        )
        val width = (intrinsicWidth * scale).toInt().coerceAtLeast(1)
        val height = (intrinsicHeight * scale).toInt().coerceAtLeast(1)
        val left = (iconSizePx - width) / 2
        val top = (iconSizePx - height) / 2

        renderedDrawable.setBounds(left, top, left + width, top + height)
        renderedDrawable.draw(canvas)
        val immutableBitmap = mutableBitmap.copy(Bitmap.Config.ARGB_8888, false)
        mutableBitmap.recycle()
        return LauncherAppIcon(immutableBitmap)
    }

    @Synchronized
    fun load(resolveInfo: ResolveInfo): LauncherAppIcon? {
        val activityInfo = resolveInfo.activityInfo ?: return null
        val component = ComponentName(activityInfo.packageName, activityInfo.name)
        cache[component]?.let { return it.icon }
        val icon = runCatching { load(resolveInfo.loadIcon(packageManager)) }.getOrNull()
        cache[component] = CachedIcon(icon)
        return icon
    }

    @Synchronized
    fun clear() = cache.clear()

    private data class CachedIcon(val icon: LauncherAppIcon?)

    private companion object {
        const val MAX_CACHED_ICONS = 512
    }
}
