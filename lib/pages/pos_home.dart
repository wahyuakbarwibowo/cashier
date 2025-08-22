import 'package:flutter/material.dart';
import '../db/db_helper.dart';
import '../services/printer_service.dart';
import 'barcode_scanner_page.dart';
import 'report_page.dart';

class POSHome extends StatefulWidget {
  const POSHome({super.key});

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
      MaterialPageRoute(builder: (_) => const BarcodeScannerPage()),
    );

    if (barcode != null) {
      final product = await DBHelper.getProductByBarcode(barcode);
      if (product != null) {
        _addToCart(product);
      } else {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text("Produk tidak ditemukan")));
      }
    }
  }

  void _addToCart(Map<String, dynamic> product) {
    setState(() {
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
                  title: Text("${item['name']} x${item['qty']}"),
                  trailing: Text("Rp${item['price'] * item['qty']}"),
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
