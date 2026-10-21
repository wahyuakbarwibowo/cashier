package com.wahyuakbarwibowo.aminmartkasir.data.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductSyncWorkerTest {
    @Test
    fun `automatic sync requires same online store`() {
        assertTrue(canAutoSync("store-1", "store-1", false))
        assertFalse(canAutoSync("store-1", "store-2", false))
        assertFalse(canAutoSync("store-1", null, false))
        assertFalse(canAutoSync("store-1", "store-1", true))
    }
}
