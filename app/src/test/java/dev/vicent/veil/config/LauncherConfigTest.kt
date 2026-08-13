package dev.vicent.veil.config

import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.QuickActionSpec
import org.junit.Test
import kotlin.test.assertEquals

class LauncherConfigTest {
    @Test
    fun `work keeps Google Authenticator in the fifth stable slot`() {
        val workPackages = LauncherConfig.contexts
            .single { it.kind == LauncherContextKind.WORK }
            .quickActions
            .filterIsInstance<QuickActionSpec.App>()
            .map(QuickActionSpec.App::packageName)

        assertEquals("com.google.android.apps.authenticator2", workPackages[4])
        assertEquals(false, "com.termux" in workPackages)
    }
}
