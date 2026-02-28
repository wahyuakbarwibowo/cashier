package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.ShopProfileDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ShopProfileEntity
import kotlinx.coroutines.flow.Flow

class ShopProfileRepository(private val shopProfileDao: ShopProfileDao) {
    val shopProfile: Flow<ShopProfileEntity?> = shopProfileDao.getShopProfile()

    suspend fun getShopProfileOnce(): ShopProfileEntity? {
        return shopProfileDao.getShopProfileOnce()
    }

    suspend fun insert(profile: ShopProfileEntity): Long {
        return shopProfileDao.insert(profile)
    }

    suspend fun update(profile: ShopProfileEntity) {
        shopProfileDao.update(profile)
    }

    suspend fun delete(profile: ShopProfileEntity) {
        shopProfileDao.delete(profile)
    }
}
