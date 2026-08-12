package dev.vicent.veil.launcher.system

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import dev.vicent.veil.launcher.model.LauncherApp

class AndroidAppLauncher(private val context: Context) {
    fun launch(app: LauncherApp): Boolean {
        val intent = Intent.makeMainActivity(app.componentName).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
