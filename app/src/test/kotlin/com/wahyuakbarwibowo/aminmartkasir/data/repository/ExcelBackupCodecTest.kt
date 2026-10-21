package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.google.gson.Gson
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.BackupData
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.CustomerEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SaleEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ShopProfileEntity
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ExcelBackupCodecTest {
    private val gson = Gson()

    @Test
    fun `all sheets round trip IDs text and deleted products`() {
        val original = BackupData(
            products = listOf(ProductEntity(id = 12, name = "Teh & Roti <Pagi>", stock = 7, isDeleted = true)),
            customers = listOf(CustomerEntity(id = 5, name = "Budi", phone = "001234")),
            sales = listOf(SaleEntity(id = 31, customerId = 5, total = 12000.0)),
            shopProfile = ShopProfileEntity(id = 1, name = "Toko Pagi", phoneNumber = "001234")
        )
        val output = ByteArrayOutputStream()

        ExcelBackupCodec.write(output, gson.toJson(original))
        val restored = gson.fromJson(ExcelBackupCodec.read(output.toByteArray().inputStream()), BackupData::class.java)

        assertEquals(19, ExcelBackupCodec.sheetNames().size)
        assertEquals(original.products, restored.products)
        assertEquals(original.customers, restored.customers)
        assertEquals(original.sales, restored.sales)
        assertEquals(original.shopProfile, restored.shopProfile)
    }

    @Test
    fun `empty template cannot erase existing data`() {
        val output = ByteArrayOutputStream()
        ExcelBackupCodec.write(output)

        val error = assertThrows(IllegalArgumentException::class.java) {
            ExcelBackupCodec.read(output.toByteArray().inputStream())
        }
        assertTrue(error.message.orEmpty().contains("Template masih kosong"))
    }

    @Test
    fun `missing data sheet is rejected before restore`() {
        val complete = ByteArrayOutputStream()
        ExcelBackupCodec.write(complete, gson.toJson(BackupData(products = listOf(ProductEntity(name = "Teh")))))
        val broken = ByteArrayOutputStream()
        ZipInputStream(complete.toByteArray().inputStream()).use { input ->
            ZipOutputStream(broken).use { output ->
                while (true) {
                    val entry = input.nextEntry ?: break
                    if (entry.name != "xl/worksheets/sheet1.xml") {
                        output.putNextEntry(ZipEntry(entry.name))
                        input.copyTo(output)
                        output.closeEntry()
                    }
                    input.closeEntry()
                }
            }
        }

        assertThrows(IllegalStateException::class.java) {
            ExcelBackupCodec.read(broken.toByteArray().inputStream())
        }
    }

    @Test
    fun `imports shared strings used by spreadsheet editors`() {
        val complete = ByteArrayOutputStream()
        ExcelBackupCodec.write(complete, gson.toJson(BackupData(customers = listOf(CustomerEntity(id = 5, name = "Budi")))))
        val customerSheet = "xl/worksheets/sheet${ExcelBackupCodec.sheetNames().indexOf("customers") + 1}.xml"
        val edited = ByteArrayOutputStream()
        ZipInputStream(complete.toByteArray().inputStream()).use { input ->
            ZipOutputStream(edited).use { output ->
                while (true) {
                    val entry = input.nextEntry ?: break
                    output.putNextEntry(ZipEntry(entry.name))
                    val bytes = input.readBytes()
                    if (entry.name == customerSheet) {
                        val xml = String(bytes, StandardCharsets.UTF_8)
                        val pattern = Regex("<c r=\"([A-Z]+)2\" s=\"1\" t=\"inlineStr\"><is><t xml:space=\"preserve\">Budi</t></is></c>")
                        assertTrue(pattern.containsMatchIn(xml))
                        output.write(xml.replace(pattern) { match ->
                            "<c r=\"${match.groupValues[1]}2\" t=\"s\"><v>0</v></c>"
                        }.toByteArray(StandardCharsets.UTF_8))
                    } else {
                        output.write(bytes)
                    }
                    output.closeEntry()
                    input.closeEntry()
                }
                output.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
                output.write("<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><si><t>Budi</t></si></sst>".toByteArray())
                output.closeEntry()
            }
        }

        val restored = gson.fromJson(ExcelBackupCodec.read(edited.toByteArray().inputStream()), BackupData::class.java)
        assertEquals("Budi", restored.customers?.single()?.name)
    }
}
