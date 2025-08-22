import 'package:blue_thermal_printer/blue_thermal_printer.dart';

class PrinterService {
  BlueThermalPrinter bluetooth = BlueThermalPrinter.instance;

  Future<void> printReceipt(
    List<Map<String, dynamic>> cart,
    double total,
  ) async {
    List<BluetoothDevice> devices = await bluetooth.getBondedDevices();
    if (devices.isNotEmpty) {
      await bluetooth.connect(devices.first);

      bluetooth.printCustom("TOKO SEDERHANA", 3, 1);
      bluetooth.printCustom("Jl. Raya No. 123", 1, 1);
      bluetooth.printNewLine();

      bluetooth.printCustom("Tanggal: ${DateTime.now()}", 1, 0);
      bluetooth.printCustom("-----------------------------", 1, 1);

      for (var item in cart) {
        String name = item["name"];
        int qty = item["qty"];
        double price = item["price"];
        double subtotal = qty * price;
        bluetooth.printLeftRight("$name x$qty", "Rp$subtotal", 1);
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
