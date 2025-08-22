import 'package:cashier/pages/product_page.dart';
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

  void _saveTransaction(double discount, double tax) async {
    double total = cart.fold(0, (sum, item) => sum + item["price"]);
    double grandTotal = total - discount + tax;

    int id = await DBHelper.insertTransaction(
      {
            "date": DateTime.now().toIso8601String(),
            "total": total,
            "discount": discount,
            "tax": tax,
            "grand_total": grandTotal,
          }
          as List<Map<String, dynamic>>,
      total,
    );

    for (var item in cart) {
      await DBHelper.insertTransactionItem({
        "transaction_id": id,
        "product_id": item["id"],
        "qty": 1,
        "price": item["price"],
      });
    }

    setState(() {
      cart.clear();
    });
  }

  void _checkout() {
    final _discountCtrl = TextEditingController();
    bool includeTax = false;

    showDialog(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setStateDialog) {
            double subtotal = cart.fold(0, (sum, item) => sum + item["price"]);
            double discount = double.tryParse(_discountCtrl.text) ?? 0;
            double tax = includeTax ? (subtotal - discount) * 0.1 : 0;
            double grandTotal = subtotal - discount + tax;

            return AlertDialog(
              title: const Text("Checkout"),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text("Subtotal: Rp$subtotal"),
                  TextField(
                    controller: _discountCtrl,
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(labelText: "Diskon (Rp)"),
                    onChanged: (_) => setStateDialog(() {}),
                  ),
                  Row(
                    children: [
                      Checkbox(
                        value: includeTax,
                        onChanged: (val) {
                          setStateDialog(() {
                            includeTax = val!;
                          });
                        },
                      ),
                      const Text("Tambahkan Pajak 10%"),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Text(
                    "Grand Total: Rp$grandTotal",
                    style: const TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 18,
                    ),
                  ),
                ],
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text("Batal"),
                ),
                ElevatedButton(
                  onPressed: () {
                    _saveTransaction(discount, tax);
                    Navigator.pop(context);
                  },
                  child: const Text("Simpan"),
                ),
              ],
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Aplikasi Kasir Flutter"),
        actions: [
          IconButton(
            icon: const Icon(Icons.inventory),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const ProductPage()),
              );
            },
          ),
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
