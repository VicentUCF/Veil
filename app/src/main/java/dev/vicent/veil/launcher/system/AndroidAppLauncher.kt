package dev.vicent.veil.launcher.system

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import dev.vicent.veil.launcher.model.LauncherApp

class AndroidAppLauncher(private val context: Context) {
    fun launch(app: LauncherApp): Boolean {
        val intent = Intent.makeMainActivity(app.componentName).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }

        return start(intent)
    }

    fun openAppInfo(app: LauncherApp): Boolean {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:${app.packageName}".toUri(),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(intent)
    }

    fun requestUninstall(app: LauncherApp): Boolean {
        val intent = Intent(
            Intent.ACTION_DELETE,
            "package:${app.packageName}".toUri(),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(intent)
    }

    private fun start(intent: Intent): Boolean {
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
