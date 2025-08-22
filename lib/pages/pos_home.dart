import 'package:cashier/pages/report_page.dart';
import 'package:flutter/material.dart';
import 'package:cashier/db/db_helper.dart';
import 'package:cashier/services/printer_service.dart';
import 'package:cashier/pages/barcode_scanner_page.dart';

class POSHome extends StatefulWidget {
  @override
  State<POSHome> createState() => _POSHomeState();
}

class _POSHomeState extends State<POSHome> {
  final printer = PrinterService();
  List<Map<String, dynamic>> cart = [];
  double total = 0;

  void _scanBarcode() async {
    final barcode = await Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => BarcodeScannerPage()),
    );

    if (barcode != null) {
      final product = await DBHelper.getProductByBarcode(barcode);
      if (product != null) {
        setState(() {
          cart.add(product);
          total += product["price"];
        });
      } else {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text("Produk tidak ditemukan")));
      }
    }
  }

  void _saveAndPrint() async {
    if (cart.isEmpty) return;

    await DBHelper.insertTransaction(cart, total);
    await printer.printReceipt(cart, total);

    setState(() {
      cart.clear();
      total = 0;
    });

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text("Transaksi tersimpan & struk dicetak")),
    );
  }

  void _addToCart(Map<String, dynamic> product) {
    setState(() {
      // cek kalau produk sudah ada → tambah qty
      int index = cart.indexWhere((item) => item["id"] == product["id"]);
      if (index >= 0) {
        cart[index]["qty"] += 1;
      } else {
        cart.add({
          "id": product["id"],
          "name": product["name"],
          "price": product["price"],
          "qty": 1,
        });
      }
      total += product["price"];
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Aplikasi Kasir Flutter"),
        actions: [
          IconButton(
            icon: const Icon(Icons.list),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const ReportPage()),
              );
            },
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: ListView.builder(
              itemCount: cart.length,
              itemBuilder: (context, index) {
                final item = cart[index];
                return ListTile(
                  title: Text(item["name"]),
                  subtitle: Text("Rp${item["price"]}"),
                );
              },
            ),
          ),
          Text("Total: Rp$total", style: const TextStyle(fontSize: 20)),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              ElevatedButton.icon(
                onPressed: _scanBarcode,
                icon: const Icon(Icons.qr_code_scanner),
                label: const Text("Scan Produk"),
              ),
              ElevatedButton.icon(
                onPressed: _saveAndPrint,
                icon: const Icon(Icons.print),
                label: const Text("Simpan & Cetak"),
              ),
            ],
          ),
          const SizedBox(height: 20),
        ],
      ),
    );
  }
}
