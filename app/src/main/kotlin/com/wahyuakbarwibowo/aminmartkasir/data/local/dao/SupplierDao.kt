package com.wahyuakbarwibowo.aminmartkasir.data.local.dao

import androidx.room.*
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SupplierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getSuppliers(limit: Int, offset: Int): List<SupplierEntity>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getSupplierById(id: Long): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(supplier: SupplierEntity): Long

    @Update
    suspend fun update(supplier: SupplierEntity)

    @Delete
    suspend fun delete(supplier: SupplierEntity)

    /** Tolak bila masih ada hutang belum lunas; histori lunas tetap ada (FK SET NULL). Return 0 = ditolak. */
    @Query("DELETE FROM suppliers WHERE id = :id AND NOT EXISTS (SELECT 1 FROM payables WHERE supplierId = :id AND status != 'paid')")
    suspend fun deleteIfNoUnpaidPayable(id: Long): Int

    @Query("SELECT COUNT(*) FROM suppliers")
    fun getSupplierCount(): Flow<Int>
}
