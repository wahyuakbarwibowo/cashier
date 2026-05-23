package com.wahyuakbarwibowo.aminmartkasir.data.local.dao

import androidx.room.*
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

data class CategoryExpenseDto(
    val category: String,
    val totalAmount: Double
)

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY id DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getExpenses(limit: Int, offset: Int): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses ORDER BY id DESC")
    fun getExpensesByDateRange(): Flow<List<ExpenseEntity>>

    @Query("SELECT SUM(amount) FROM expenses WHERE createdAt >= :startDate AND createdAt <= :endDate")
    suspend fun getTotalExpensesByDateRange(startDate: String, endDate: String): Double?

    @Query("SELECT SUM(amount) FROM expenses")
    suspend fun getTotalExpensesAllTime(): Double?

    @Query("SELECT category, SUM(amount) as totalAmount FROM expenses WHERE createdAt >= :startDate AND createdAt <= :endDate GROUP BY category ORDER BY totalAmount DESC")
    suspend fun getExpensesByCategoryByDateRange(startDate: String, endDate: String): List<CategoryExpenseDto>

    @Query("SELECT category, SUM(amount) as totalAmount FROM expenses GROUP BY category ORDER BY totalAmount DESC")
    suspend fun getExpensesByCategoryAllTime(): List<CategoryExpenseDto>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)
}
