package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.ExpenseDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    suspend fun getExpenses(limit: Int, offset: Int): List<ExpenseEntity> {
        return expenseDao.getExpenses(limit, offset)
    }

    suspend fun getTotalExpensesByDateRange(startDate: String, endDate: String): Double {
        return expenseDao.getTotalExpensesByDateRange(startDate, endDate) ?: 0.0
    }

    suspend fun getTotalExpensesAllTime(): Double {
        return expenseDao.getTotalExpensesAllTime() ?: 0.0
    }

    suspend fun getExpensesByCategoryByDateRange(startDate: String, endDate: String): List<com.wahyuakbarwibowo.aminmartkasir.data.local.dao.CategoryExpenseDto> {
        return expenseDao.getExpensesByCategoryByDateRange(startDate, endDate)
    }

    suspend fun getExpensesByCategoryAllTime(): List<com.wahyuakbarwibowo.aminmartkasir.data.local.dao.CategoryExpenseDto> {
        return expenseDao.getExpensesByCategoryAllTime()
    }

    suspend fun insert(expense: ExpenseEntity): Long {
        return expenseDao.insert(expense)
    }

    suspend fun update(expense: ExpenseEntity) {
        expenseDao.update(expense)
    }

    suspend fun delete(expense: ExpenseEntity) {
        expenseDao.delete(expense)
    }

    suspend fun deleteById(id: Long) {
        expenseDao.deleteById(id)
    }
}
