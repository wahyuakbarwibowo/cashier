package com.wahyuakbarwibowo.aminmartkasir.utils

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.*

class BluetoothPrinterHelper(private val context: Context) {
    
    private var bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    private var bluetoothSocket: BluetoothSocket? = null
    private var connectedDevice: BluetoothDevice? = null
    private var isConnected = false
    
    companion object {
        // ESC/POS Commands
        private val INIT = byteArrayOf(0x1B, 0x40) // Initialize printer
        private val LINE_FEED = byteArrayOf(0x0A) // Line feed
        private val CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x00) // Cut paper
        private val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00) // Align left
        private val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01) // Align center
        private val ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02) // Align right
        private val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01) // Bold on
        private val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00) // Bold off
        private val DOUBLE_SIZE_ON = byteArrayOf(0x1D, 0x21, 0x11) // Double size
        private val DOUBLE_SIZE_OFF = byteArrayOf(0x1D, 0x21, 0x00) // Normal size
        private val UNDERLINE_ON = byteArrayOf(0x1B, 0x2D, 0x01) // Underline on
        private val UNDERLINE_OFF = byteArrayOf(0x1B, 0x2D, 0x00) // Underline off
    }
    
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter != null
    }
    
    fun isBluetoothPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!isBluetoothEnabled()) return emptyList()
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }
    
    fun connect(device: BluetoothDevice): Boolean {
        try {
            if (isConnected) disconnect()
            
            bluetoothSocket = device.createRfcommSocketToServiceRecord(
                UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            )
            bluetoothAdapter?.cancelDiscovery()
            bluetoothSocket?.connect()
            connectedDevice = device
            isConnected = true
            
            // Initialize printer
            sendCommand(INIT)
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            isConnected = false
            return false
        }
    }
    
    fun disconnect() {
        try {
            bluetoothSocket?.close()
            bluetoothSocket = null
            connectedDevice = null
            isConnected = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun printText(text: String) {
        if (!isConnected) return
        try {
            bluetoothSocket?.outputStream?.write(text.toByteArray(Charsets.UTF_8))
            bluetoothSocket?.outputStream?.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    
    fun sendCommand(command: ByteArray) {
        if (!isConnected) return
        try {
            bluetoothSocket?.outputStream?.write(command)
            bluetoothSocket?.outputStream?.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    
    fun printReceipt(
        shopName: String,
        shopAddress: String?,
        shopPhone: String?,
        transactionId: String,
        date: String,
        cashierName: String?,
        items: List<ReceiptItem>,
        subtotal: Double,
        discount: Double,
        total: Double,
        paid: Double,
        change: Double,
        pointsEarned: Int = 0,
        footerNote: String? = null
    ) {
        if (!isConnected) return
        
        try {
            // Initialize
            sendCommand(INIT)
            
            // Shop name (center, bold, double size)
            sendCommand(ALIGN_CENTER)
            sendCommand(BOLD_ON)
            sendCommand(DOUBLE_SIZE_ON)
            printText("$shopName\n")
            sendCommand(DOUBLE_SIZE_OFF)
            sendCommand(BOLD_OFF)
            
            // Shop info
            if (!shopAddress.isNullOrBlank()) {
                printText("$shopAddress\n")
            }
            if (!shopPhone.isNullOrBlank()) {
                printText("Telp: $shopPhone\n")
            }
            printText("\n")
            
            // Transaction info
            sendCommand(ALIGN_LEFT)
            sendCommand(BOLD_ON)
            printText("--------------------------------\n")
            sendCommand(BOLD_OFF)
            printText("No: $transactionId\n")
            printText("Tgl: $date\n")
            if (!cashierName.isNullOrBlank()) {
                printText("Kasir: $cashierName\n")
            }
            printText("\n")
            
            // Items header
            sendCommand(BOLD_ON)
            printText("%-4s %-20s %5s %8s\n".format("Qty", "Item", "Harga", "Subtotal"))
            sendCommand(BOLD_OFF)
            printText("--------------------------------\n")
            
            // Items
            items.forEach { item ->
                val name = if (item.name.length > 20) item.name.substring(0, 17) + "..." else item.name
                printText(
                    "%-4d %-20s %5s %8s\n".format(
                        item.qty,
                        name,
                        formatCurrency(item.price),
                        formatCurrency(item.subtotal)
                    )
                )
            }
            
            printText("\n")
            sendCommand(BOLD_ON)
            printText("--------------------------------\n")
            sendCommand(BOLD_OFF)
            
            // Summary
            sendCommand(ALIGN_RIGHT)
            printText("Subtotal:    %10s\n".format(formatCurrency(subtotal)))
            if (discount > 0) {
                printText("Diskon:      %10s\n".format(formatCurrency(discount)))
            }
            sendCommand(BOLD_ON)
            sendCommand(DOUBLE_SIZE_ON)
            printText("TOTAL:       %10s\n".format(formatCurrency(total)))
            sendCommand(DOUBLE_SIZE_OFF)
            sendCommand(BOLD_OFF)
            printText("Dibayar:     %10s\n".format(formatCurrency(paid)))
            printText("Kembalian:   %10s\n".format(formatCurrency(change)))
            
            if (pointsEarned > 0) {
                printText("\n")
                printText("Poin Earned: %d\n".format(pointsEarned))
            }
            
            printText("\n")
            sendCommand(ALIGN_CENTER)
            sendCommand(BOLD_ON)
            printText("--------------------------------\n")
            sendCommand(BOLD_OFF)
            
            // Footer
            if (!footerNote.isNullOrBlank()) {
                printText("$footerNote\n")
            }
            printText("Terima kasih atas kunjungan Anda!\n")
            printText("\n")
            
            // Cut paper
            sendCommand(CUT_PAPER)
            
            // Feed paper
            for (i in 1..3) {
                sendCommand(LINE_FEED)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun formatCurrency(amount: Double): String {
        return String.format(java.util.Locale("id", "ID"), "Rp %,d", amount.toLong())
            .replace("Rp", "")
            .trim()
    }
    
    data class ReceiptItem(
        val name: String,
        val qty: Int,
        val price: Double,
        val subtotal: Double
    )
}
