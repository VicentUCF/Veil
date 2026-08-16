package dev.vicent.veil.launcher.system

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import dev.vicent.veil.launcher.ExternalLinkPolicy

class AndroidWebLauncher(private val context: Context) {
    fun open(url: String): Boolean {
        if (!ExternalLinkPolicy.isAllowedSteamBrowserLink(url)) return false
        return openValidated(url)
    }

    fun openPrivacyPolicy(url: String): Boolean {
        if (!ExternalLinkPolicy.isSafeHttps(url)) return false
        return openValidated(url)
    }

    private fun openValidated(url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
