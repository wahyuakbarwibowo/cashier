package com.wahyuakbarwibowo.aminmartkasir.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.nio.file.Files
import java.util.UUID

object TenantDatabaseFiles {
    private const val LEGACY_NAME = "kasir_database"
    private const val PREFS = "tenant_database_migration"
    private const val HANDLED = "legacy_handled"
    private const val PLATFORM_PREFIX = "platform-"

    fun platformStore(userId: String): String = "$PLATFORM_PREFIX${UUID.fromString(userId)}"
    fun isPlatformStore(storeId: String): Boolean = storeId.startsWith(PLATFORM_PREFIX)

    fun name(storeId: String): String = if (isPlatformStore(storeId)) {
        "kasir_platform_${UUID.fromString(storeId.removePrefix(PLATFORM_PREFIX))}"
    } else {
        "kasir_${UUID.fromString(storeId)}"
    }

    fun needsChoice(context: Context, storeId: String): Boolean =
        !isPlatformStore(storeId) &&
            context.getDatabasePath(LEGACY_NAME).exists() &&
            !context.getDatabasePath(name(storeId)).exists() &&
            !context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(HANDLED, false) &&
            !context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("skip:$storeId", false)

    fun claimLegacy(context: Context, storeId: String) {
        require(needsChoice(context, storeId)) { "Data lama sudah dipilih atau toko sudah memiliki data." }
        val source = context.getDatabasePath(LEGACY_NAME)
        val target = context.getDatabasePath(name(storeId))
        SQLiteDatabase.openDatabase(source.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { cursor ->
                check(cursor.moveToFirst() && cursor.getInt(0) == 0) { "Data lama masih dipakai; coba lagi." }
            }
        }
        val temporary = target.resolveSibling("${target.name}.copying")
        try {
            Files.copy(source.toPath(), temporary.toPath())
            Files.move(temporary.toPath(), target.toPath())
            finish(context)
        } finally {
            Files.deleteIfExists(temporary.toPath())
        }
    }

    fun keepLegacySeparate(context: Context, storeId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("skip:$storeId", true).apply()
    }

    private fun finish(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(HANDLED, true).apply()
    }
}
