package com.wahyuakbarwibowo.aminmartkasir.data.remote.model

import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class StoreFeatureFlagTest {
    private val now = Instant.parse("2026-10-21T00:00:00Z")

    @Test
    fun `enabled flag without expiry remains active`() {
        val flag = StoreFeatureFlag("store-1", "cloud_backup", true)

        assertTrue(flag.isActive(now))
    }

    @Test
    fun `expired or disabled flag is inactive`() {
        val expired = StoreFeatureFlag("store-1", "cloud_backup", true, "2026-10-20T23:59:59Z")
        val disabled = StoreFeatureFlag("store-1", "cloud_backup", false)

        assertFalse(expired.isActive(now))
        assertFalse(disabled.isActive(now))
    }

    @Test
    fun `malformed expiry fails closed`() {
        val malformed = StoreFeatureFlag("store-1", "cloud_backup", true, "not-a-time")

        assertFalse(malformed.isActive(now))
    }

    @Test
    fun `editor expiry uses end of selected WIB day and preserves unchanged timestamp`() {
        assertEquals("2026-11-02T17:00:00Z", expiryFromDate("2026-11-02", null))
        assertEquals("2026-11-02T10:30:00Z", expiryFromDate("2026-11-02", "2026-11-02T10:30:00Z"))
        assertEquals(null, expiryFromDate("", "2026-11-02T10:30:00Z"))
        assertThrows(java.time.format.DateTimeParseException::class.java) {
            expiryFromDate("2026-02-30", null)
        }
    }
}
