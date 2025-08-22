import 'package:blue_thermal_printer/blue_thermal_printer.dart';

class PrinterService {
  BlueThermalPrinter bluetooth = BlueThermalPrinter.instance;

  Future<void> printReceipt(
    List<Map<String, dynamic>> cart,
    double total,
  ) async {
    List<BluetoothDevice> devices = await bluetooth.getBondedDevices();
    if (devices.isNotEmpty) {
      // connect ke device pertama (bisa diganti UI pilihan printer)
      await bluetooth.connect(devices.first);

      // Header toko
      bluetooth.printCustom("TOKO SEDERHANA", 3, 1);
      bluetooth.printCustom("Jl. Raya No. 123", 1, 1);
      bluetooth.printNewLine();

      // Info transaksi
      bluetooth.printCustom("Tanggal: ${DateTime.now()}", 1, 0);
      bluetooth.printCustom("-----------------------------", 1, 1);

      // Daftar produk
      for (var item in cart) {
        String name = item["name"];
        double price = item["price"];
        bluetooth.printLeftRight(name, "Rp$price", 1);
      }

      bluetooth.printCustom("-----------------------------", 1, 1);
      bluetooth.printLeftRight("TOTAL", "Rp$total", 2);

      bluetooth.printNewLine();
      bluetooth.printCustom("Terima Kasih!", 2, 1);
      bluetooth.printNewLine();
      bluetooth.paperCut();
    }
  }
}
