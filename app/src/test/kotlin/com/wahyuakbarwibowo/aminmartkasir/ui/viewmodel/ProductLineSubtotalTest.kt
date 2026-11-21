package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductLineSubtotalTest {

    private val unitOnly = ProductEntity(name = "Teh", sellingPrice = 1000.0)
    private val withPackage = unitOnly.copy(packagePrice = 9000.0, packageQty = 10)

    @Test
    fun `harga satuan bila tidak ada paket`() {
        assertEquals(3000.0, productLineSubtotal(unitOnly, 3), 0.0)
    }

    @Test
    fun `paket tanpa harga paket tidak membuat harga nol`() {
        assertEquals(12000.0, productLineSubtotal(unitOnly.copy(packageQty = 12), 12), 0.0)
    }

    @Test
    fun `sisa di luar paket memakai harga satuan`() {
        assertEquals(10000.0, productLineSubtotal(withPackage, 11), 0.0)
        assertEquals(18000.0, productLineSubtotal(withPackage, 20), 0.0)
        assertEquals(9000.0, productLineSubtotal(withPackage, 9), 0.0)
    }

    @Test
    fun `diskon persen dibulatkan ke rupiah`() {
        assertEquals(2700.0, productLineSubtotal(unitOnly.copy(discount = 10.0), 3), 0.0)
        assertEquals(3316.0, productLineSubtotal(unitOnly.copy(sellingPrice = 1111.0, discount = 0.5), 3), 0.0)
    }
}
