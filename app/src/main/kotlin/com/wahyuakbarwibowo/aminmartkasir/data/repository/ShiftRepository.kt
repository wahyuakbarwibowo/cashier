package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.ShiftDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ShiftEntity
import kotlinx.coroutines.flow.Flow

class ShiftRepository(private val shiftDao: ShiftDao) {
    val allShifts: Flow<List<ShiftEntity>> = shiftDao.getAllShifts()
    val openShift: Flow<ShiftEntity?> = shiftDao.observeOpenShift()

    suspend fun getOpenShift(): ShiftEntity? = shiftDao.getOpenShift()

    suspend fun getById(id: Long): ShiftEntity? = shiftDao.getById(id)

    suspend fun insert(shift: ShiftEntity): Long = shiftDao.insert(shift)

    suspend fun update(shift: ShiftEntity) = shiftDao.update(shift)
}
