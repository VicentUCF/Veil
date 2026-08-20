package dev.vicent.veil.launcher.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vicent.veil.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class SearchLearningBackupRulesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun searchLearningPreferencesAreExcludedFromLegacyBackup() {
        assertTrue(excludedPaths(R.xml.backup_rules).contains(SEARCH_PREFERENCES_FILE))
    }

    @Test
    fun searchLearningPreferencesAreExcludedFromCloudAndDeviceTransfer() {
        val paths = excludedPaths(R.xml.data_extraction_rules)
        assertTrue(paths.count { it == SEARCH_PREFERENCES_FILE } >= 2)
    }

    private fun excludedPaths(resourceId: Int): List<String> {
        val parser = context.resources.getXml(resourceId)
        return buildList {
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "exclude") {
                    parser.getAttributeValue(null, "path")?.let(::add)
                }
                parser.next()
            }
        }.also { parser.close() }
    }

    private companion object {
        const val SEARCH_PREFERENCES_FILE = "veil_search_learning.xml"
    }
}
