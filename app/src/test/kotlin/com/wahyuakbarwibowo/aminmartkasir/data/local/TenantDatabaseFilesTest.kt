package com.wahyuakbarwibowo.aminmartkasir.data.local

import android.content.Context
import android.content.SharedPreferences
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class TenantDatabaseFilesTest {
    @get:Rule val files = TemporaryFolder()
    private val storeId = "11111111-1111-1111-1111-111111111111"

    @Test
    fun `database name is scoped to tenant and rejects unsafe ids`() {
        assertTrue(TenantDatabaseFiles.name(storeId).contains(storeId))
        assertFalse(TenantDatabaseFiles.name(storeId) == TenantDatabaseFiles.name("22222222-2222-2222-2222-222222222222"))
        assertThrows(IllegalArgumentException::class.java) {
            TenantDatabaseFiles.name("../another-database")
        }
    }

    @Test
    fun `legacy choice is required only before first tenant database`() {
        val context = mock<Context>()
        val prefs = mock<SharedPreferences>()
        whenever(context.getDatabasePath(any())).thenAnswer { File(files.root, it.getArgument<String>(0)) }
        whenever(context.getSharedPreferences(eq("tenant_database_migration"), eq(Context.MODE_PRIVATE))).thenReturn(prefs)
        whenever(prefs.getBoolean(eq("legacy_handled"), eq(false))).thenReturn(false)
        assertFalse(TenantDatabaseFiles.needsChoice(context, storeId))

        files.newFile("kasir_database")
        assertTrue(TenantDatabaseFiles.needsChoice(context, storeId))
        whenever(prefs.getBoolean(eq("skip:$storeId"), eq(false))).thenReturn(true)
        assertFalse(TenantDatabaseFiles.needsChoice(context, storeId))
        whenever(prefs.getBoolean(eq("skip:$storeId"), eq(false))).thenReturn(false)
        files.newFile(TenantDatabaseFiles.name(storeId))
        assertFalse(TenantDatabaseFiles.needsChoice(context, storeId))
        assertFalse(TenantDatabaseFiles.needsChoice(context, TenantDatabaseFiles.platformStore(storeId)))
    }
}
