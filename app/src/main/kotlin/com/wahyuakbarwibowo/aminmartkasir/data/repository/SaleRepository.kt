package com.wahyuakbarwibowo.aminmartkasir.data.repository

import androidx.room.withTransaction
import com.wahyuakbarwibowo.aminmartkasir.data.local.AppDatabase
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SaleEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SaleItemEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.CustomerPointsHistoryEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ReceivableEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.StockHistoryEntity
import kotlinx.coroutines.flow.Flow

class SaleRepository(
    private val database: AppDatabase
) {
    private val saleDao = database.saleDao()
    private val saleItemDao = database.saleItemDao()

    val allSales: Flow<List<SaleEntity>> = saleDao.getAllSales()
    val saleCount: Flow<Int> = saleDao.getSaleCount()

    suspend fun getSales(limit: Int, offset: Int): List<SaleEntity> {
        return saleDao.getSales(limit, offset)
    }

    suspend fun getSaleById(id: Long): SaleEntity? {
        return saleDao.getSaleById(id)
    }

    fun getSaleItems(saleId: Long): Flow<List<SaleItemEntity>> {
        return saleItemDao.getSaleItemsBySaleId(saleId)
    }

    suspend fun getSaleItemsOnce(saleId: Long): List<SaleItemEntity> {
        return saleItemDao.getSaleItemsBySaleIdOnce(saleId)
    }

    suspend fun getTopSellingProductsOnce(limit: Int): List<com.wahyuakbarwibowo.aminmartkasir.data.local.dao.ProductQuantityDto> {
        return saleItemDao.getTopSellingProductsOnce(limit)
    }

    suspend fun getTotalSalesByDateRange(startDate: String, endDate: String): Double {
        return saleDao.getTotalSalesByDateRange(startDate, endDate) ?: 0.0
    }

    suspend fun getTotalProfitByDateRange(startDate: String, endDate: String): Double {
        return saleDao.getTotalProfitByDateRange(startDate, endDate) ?: 0.0
    }

    suspend fun getTotalSalesAllTime(): Double {
        return saleDao.getTotalSalesAllTime() ?: 0.0
    }

    suspend fun getTotalProfitAllTime(): Double {
        return saleDao.getTotalProfitAllTime() ?: 0.0
    }

    suspend fun getSalesSince(startDate: String): List<SaleEntity> {
        return saleDao.getSalesSince(startDate)
    }

    suspend fun searchSales(query: String, limit: Int = 20): List<SaleEntity> {
        return saleDao.searchSales(query, limit)
    }

    suspend fun deleteSale(sale: SaleEntity) {
        saleDao.delete(sale)
    }

    /**
     * Memproses checkout transaksi penjualan secara atomik di dalam sebuah Room Database Transaction.
     */
    suspend fun checkoutSaleTransaction(
        sale: SaleEntity,
        items: List<SaleItemEntity>,
        stockUpdates: List<Triple<Long, Long?, Int>>, // productId to variantId to quantity
        stockHistories: List<StockHistoryEntity>,
        pointsEarned: Int,
        pointsRedeemed: Int,
        customerId: Long?,
        receivable: ReceivableEntity?
    ): Long = database.withTransaction {
        val saleId = saleDao.insert(sale)
        
        items.forEach { item ->
            saleItemDao.insert(item.copy(saleId = saleId))
        }

        stockUpdates.forEach { (productId, variantId, qty) ->
            if (variantId != null) {
                database.productVariantDao().decreaseStock(variantId, qty)
            } else {
                database.productDao().decreaseStock(productId, qty)
            }
        }

        stockHistories.forEach { history ->
            database.stockHistoryDao().insert(
                history.copy(reason = "Transaksi penjualan #$saleId")
            )
        }

        if (customerId != null) {
            if (pointsEarned > 0) {
                database.customerDao().addPoints(customerId, pointsEarned)
                database.customerPointsHistoryDao().insert(
                    CustomerPointsHistoryEntity(
                        customerId = customerId,
                        saleId = saleId,
                        points = pointsEarned,
                        type = "EARNED",
                        notes = "Poin dari belanja",
                        createdAt = sale.createdAt ?: ""
                    )
                )
            }
            if (pointsRedeemed > 0) {
                database.customerDao().deductPoints(customerId, pointsRedeemed)
                database.customerPointsHistoryDao().insert(
                    CustomerPointsHistoryEntity(
                        customerId = customerId,
                        saleId = saleId,
                        points = -pointsRedeemed,
                        type = "REDEEMED",
                        notes = "Penukaran poin",
                        createdAt = sale.createdAt ?: ""
                    )
                )
            }
        }

        if (receivable != null) {
            database.receivableDao().insert(
                receivable.copy(saleId = saleId, notes = "Hutang dari transaksi #$saleId")
            )
        }

        saleId
    }

    /**
     * Memperbarui transaksi penjualan secara atomik di dalam sebuah Room Database Transaction.
     */
    suspend fun updateSaleTransaction(
        saleId: Long,
        updatedSale: SaleEntity,
        newSaleItems: List<SaleItemEntity>,
        oldItems: List<SaleItemEntity>,
        oldSale: SaleEntity,
        stockRestores: List<Triple<Long, Long?, Int>>, // productId to variantId ke quantity lama untuk dikembalikan
        stockDecreases: List<Triple<Long, Long?, Int>>, // productId to variantId ke quantity baru untuk dikurangi
        pointsEarnedNew: Int,
        pointsRedeemedNew: Int,
        customerIdNew: Long?
    ) = database.withTransaction {
        // 1. Kembalikan stok lama
        stockRestores.forEach { (productId, variantId, qty) ->
            val product = database.productDao().getProductById(productId)
            if (product != null) {
                if (variantId != null) {
                    val variant = database.productVariantDao().getVariantById(variantId)
                    if (variant != null) {
                        database.stockHistoryDao().insert(
                            StockHistoryEntity(
                                productId = productId,
                                productName = "${product.name} - ${variant.name}",
                                changeQty = qty,
                                stockBefore = variant.stock,
                                stockAfter = variant.stock + qty,
                                reason = "Restock dari update transaksi #$saleId",
                                createdAt = updatedSale.createdAt ?: ""
                            )
                        )
                        database.productVariantDao().increaseStock(variantId, qty)
                    }
                } else {
                    database.stockHistoryDao().insert(
                        StockHistoryEntity(
                            productId = productId,
                            productName = product.name,
                            changeQty = qty,
                            stockBefore = product.stock,
                            stockAfter = product.stock + qty,
                            reason = "Restock dari update transaksi #$saleId",
                            createdAt = updatedSale.createdAt ?: ""
                        )
                    )
                    database.productDao().increaseStock(productId, qty)
                }
            }
        }

        // 2. Batalkan poin lama
        if (oldSale.customerId != null) {
            if (oldSale.pointsEarned > 0) {
                database.customerDao().deductPoints(oldSale.customerId, oldSale.pointsEarned)
                database.customerPointsHistoryDao().insert(
                    CustomerPointsHistoryEntity(
                        customerId = oldSale.customerId,
                        saleId = saleId,
                        points = -oldSale.pointsEarned,
                        type = "REVERSAL",
                        notes = "Reversal poin dari update transaksi",
                        createdAt = updatedSale.createdAt ?: ""
                    )
                )
            }
            if (oldSale.pointsRedeemed > 0) {
                database.customerDao().addPoints(oldSale.customerId, oldSale.pointsRedeemed)
                database.customerPointsHistoryDao().insert(
                    CustomerPointsHistoryEntity(
                        customerId = oldSale.customerId,
                        saleId = saleId,
                        points = oldSale.pointsRedeemed,
                        type = "REVERSAL",
                        notes = "Reversal poin dari update transaksi",
                        createdAt = updatedSale.createdAt ?: ""
                    )
                )
            }
        }

        // 3. Update entitas Sale utama
        saleDao.update(updatedSale)
        
        // 4. Hapus item penjualan lama dan masukkan yang baru
        saleItemDao.deleteBySaleId(saleId)
        newSaleItems.forEach { item ->
            saleItemDao.insert(item.copy(saleId = saleId))
        }

        // 5. Kurangi stok baru
        stockDecreases.forEach { (productId, variantId, qty) ->
            val product = database.productDao().getProductById(productId)
            if (product != null) {
                if (variantId != null) {
                    val variant = database.productVariantDao().getVariantById(variantId)
                    if (variant != null) {
                        database.stockHistoryDao().insert(
                            StockHistoryEntity(
                                productId = productId,
                                productName = "${product.name} - ${variant.name}",
                                changeQty = -qty,
                                stockBefore = variant.stock,
                                stockAfter = (variant.stock - qty).coerceAtLeast(0),
                                reason = "Transaksi penjualan update #$saleId",
                                createdAt = updatedSale.createdAt ?: ""
                            )
                        )
                        database.productVariantDao().decreaseStock(variantId, qty)
                    }
                } else {
                    database.stockHistoryDao().insert(
                        StockHistoryEntity(
                            productId = productId,
                            productName = product.name,
                            changeQty = -qty,
                            stockBefore = product.stock,
                            stockAfter = (product.stock - qty).coerceAtLeast(0),
                            reason = "Transaksi penjualan update #$saleId",
                            createdAt = updatedSale.createdAt ?: ""
                        )
                    )
                    database.productDao().decreaseStock(productId, qty)
                }
            }
        }

        // 6. Tambahkan poin baru
        if (customerIdNew != null) {
            if (pointsEarnedNew > 0) {
                database.customerDao().addPoints(customerIdNew, pointsEarnedNew)
                database.customerPointsHistoryDao().insert(
                    CustomerPointsHistoryEntity(
                        customerId = customerIdNew,
                        saleId = saleId,
                        points = pointsEarnedNew,
                        type = "EARNED",
                        notes = "Poin dari update transaksi",
                        createdAt = updatedSale.createdAt ?: ""
                    )
                )
            }
            if (pointsRedeemedNew > 0) {
                database.customerDao().deductPoints(customerIdNew, pointsRedeemedNew)
                database.customerPointsHistoryDao().insert(
                    CustomerPointsHistoryEntity(
                        customerId = customerIdNew,
                        saleId = saleId,
                        points = -pointsRedeemedNew,
                        type = "REDEEMED",
                        notes = "Penukaran poin dari update transaksi",
                        createdAt = updatedSale.createdAt ?: ""
                    )
                )
            }
        }
    }
}
