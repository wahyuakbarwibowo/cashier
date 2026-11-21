package com.wahyuakbarwibowo.aminmartkasir.data.repository

import androidx.room.withTransaction
import com.wahyuakbarwibowo.aminmartkasir.data.local.AppDatabase
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SaleEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SaleItemEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.CustomerPointsHistoryEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ReceivableEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.StockHistoryEntity
import com.wahyuakbarwibowo.aminmartkasir.utils.DateUtils
import kotlinx.coroutines.flow.Flow

class SaleRepository(
    private val database: AppDatabase,
    private val onStockChanged: () -> Unit = {}
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

    suspend fun getSalesByDateRange(startDate: String, endDate: String): List<SaleEntity> {
        return saleDao.getSalesByDateRange(startDate, endDate)
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
        pointsEarned: Int,
        pointsRedeemed: Int,
        customerId: Long?,
        receivable: ReceivableEntity?
    ): Long = database.withTransaction {
        val saleId = saleDao.insert(sale)
        
        items.forEach { item ->
            saleItemDao.insert(item.copy(saleId = saleId))
        }

        val now = DateUtils.nowDateTime()
        items.forEach { item ->
            val productId = item.productId ?: return@forEach
            decreaseStockChecked(productId, item.variantId, item.qty, "Transaksi penjualan #$saleId", now)
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
    }.also { runCatching(onStockChanged) }

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
        customerIdNew: Long?,
        receivable: ReceivableEntity?
    ) = database.withTransaction {
        val now = DateUtils.nowDateTime()

        // 1. Kembalikan stok lama
        stockRestores.forEach { (productId, variantId, qty) ->
            increaseStockLogged(productId, variantId, qty, "Restock dari update transaksi #$saleId", now)
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
                        createdAt = now
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
                        createdAt = now
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
            decreaseStockChecked(productId, variantId, qty, "Transaksi penjualan update #$saleId", now)
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
                        createdAt = now
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
                        createdAt = now
                    )
                )
            }
        }

        // 7. Reconcile piutang (receivable) sesuai status hutang terbaru dari sale yang diedit
        val existingReceivable = database.receivableDao().getReceivableBySaleId(saleId)
        if (receivable != null) {
            if (existingReceivable != null) {
                database.receivableDao().update(
                    receivable.copy(id = existingReceivable.id, createdAt = existingReceivable.createdAt)
                )
            } else {
                database.receivableDao().insert(receivable)
            }
        } else if (existingReceivable != null) {
            database.receivableDao().delete(existingReceivable)
        }
    }.also { runCatching(onStockChanged) }

    /**
     * Kurangi stok setelah memastikan stok saat ini (dibaca di dalam transaksi) cukup.
     * Daftar produk di layar kasir bisa basi, jadi cek di UI saja tidak cukup.
     */
    private suspend fun decreaseStockChecked(productId: Long, variantId: Long?, qty: Int, reason: String, now: String) {
        val product = database.productDao().getProductById(productId)
            ?: throw IllegalStateException("Produk sudah dihapus. Hapus dari keranjang lalu coba lagi.")
        val variant = variantId?.let {
            database.productVariantDao().getVariantById(it)
                ?: throw IllegalStateException("Varian ${product.name} sudah dihapus. Hapus dari keranjang lalu coba lagi.")
        }
        val name = variant?.let { "${product.name} - ${it.name}" } ?: product.name
        val before = variant?.stock ?: product.stock
        if (before < qty) throw IllegalStateException("Stok $name tidak cukup (sisa $before).")
        if (variant != null) {
            database.productVariantDao().decreaseStock(variant.id, qty)
        } else {
            database.productDao().decreaseStock(productId, qty)
        }
        database.stockHistoryDao().insert(
            StockHistoryEntity(
                productId = productId,
                productName = name,
                changeQty = -qty,
                stockBefore = before,
                stockAfter = before - qty,
                reason = reason,
                createdAt = now
            )
        )
    }

    /** Kembalikan stok; produk/varian yang sudah dihapus dilewati. */
    private suspend fun increaseStockLogged(productId: Long, variantId: Long?, qty: Int, reason: String, now: String) {
        val product = database.productDao().getProductById(productId) ?: return
        val variant = variantId?.let { database.productVariantDao().getVariantById(it) ?: return }
        val before = variant?.stock ?: product.stock
        if (variant != null) {
            database.productVariantDao().increaseStock(variant.id, qty)
        } else {
            database.productDao().increaseStock(productId, qty)
        }
        database.stockHistoryDao().insert(
            StockHistoryEntity(
                productId = productId,
                productName = variant?.let { "${product.name} - ${it.name}" } ?: product.name,
                changeQty = qty,
                stockBefore = before,
                stockAfter = before + qty,
                reason = reason,
                createdAt = now
            )
        )
    }
}
