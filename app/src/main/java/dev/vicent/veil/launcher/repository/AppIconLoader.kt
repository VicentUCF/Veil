package dev.vicent.veil.launcher.repository

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import dev.vicent.veil.launcher.model.LauncherAppIcon
import kotlin.math.min

internal class AppIconLoader(
    private val packageManager: PackageManager,
) {
    fun load(drawable: Drawable): LauncherAppIcon {
        val mutableBitmap = createBitmap(ICON_SIZE_PX, ICON_SIZE_PX)
        val canvas = Canvas(mutableBitmap)
        val renderedDrawable = drawable.constantState?.newDrawable()?.mutate() ?: drawable.mutate()
        val intrinsicWidth = renderedDrawable.intrinsicWidth.coerceAtLeast(1)
        val intrinsicHeight = renderedDrawable.intrinsicHeight.coerceAtLeast(1)
        val scale = min(
            ICON_SIZE_PX.toFloat() / intrinsicWidth,
            ICON_SIZE_PX.toFloat() / intrinsicHeight,
        )
        val width = (intrinsicWidth * scale).toInt().coerceAtLeast(1)
        val height = (intrinsicHeight * scale).toInt().coerceAtLeast(1)
        val left = (ICON_SIZE_PX - width) / 2
        val top = (ICON_SIZE_PX - height) / 2

        renderedDrawable.setBounds(left, top, left + width, top + height)
        renderedDrawable.draw(canvas)
        val immutableBitmap = mutableBitmap.copy(Bitmap.Config.ARGB_8888, false)
        mutableBitmap.recycle()
        return LauncherAppIcon(immutableBitmap)
    }

    fun load(resolveInfo: android.content.pm.ResolveInfo): LauncherAppIcon? = runCatching {
        load(resolveInfo.loadIcon(packageManager))
    }.getOrNull()

    private companion object {
        const val ICON_SIZE_PX = 128
    }
}
