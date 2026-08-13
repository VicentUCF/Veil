package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.repository.SystemStatusRepository
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SystemStatusRepositoryTest {
    @Test
    fun `wifi strength is normalized to five honest levels`() {
        assertEquals(4, SystemStatusRepository.wifiSignalLevel(-50))
        assertEquals(3, SystemStatusRepository.wifiSignalLevel(-60))
        assertEquals(2, SystemStatusRepository.wifiSignalLevel(-70))
        assertEquals(1, SystemStatusRepository.wifiSignalLevel(-80))
        assertEquals(0, SystemStatusRepository.wifiSignalLevel(-90))
    }

    @Test
    fun `unavailable wifi strength remains unavailable`() {
        assertNull(SystemStatusRepository.wifiSignalLevel(Int.MIN_VALUE))
        assertNull(SystemStatusRepository.wifiSignalLevel(-127))
    }
}
