import 'package:cashier/main.dart';
import 'package:flutter/material.dart';
import '../db/db_helper.dart';

class POSHome extends StatefulWidget {
  const POSHome({super.key});

  @override
  State<POSHome> createState() => _POSHomeState();
}

class _POSHomeState extends State<POSHome> {
  List<Map<String, dynamic>> _products = [];
  List<Map<String, dynamic>> _cart = [];

  final _discountController = TextEditingController(text: "0");
  final _taxController = TextEditingController(text: "0");

  @override
  void initState() {
    super.initState();
    _loadProducts();
  }

  void _loadProducts() async {
    var data = await DBHelper.getProducts();
    setState(() {
      _products = data;
    });
  }

  void _addToCart(Map<String, dynamic> product) {
    setState(() {
      // cek apakah produk sudah ada di cart
      var index = _cart.indexWhere((item) => item["id"] == product["id"]);
      if (index != -1) {
        // kalau sudah ada → tambah qty
        _cart[index]["qty"] += 1;
      } else {
        // kalau belum ada → tambah baru
        _cart.add({
          "id": product["id"],
          "name": product["name"],
          "price": product["price"],
          "qty": 1,
        });
      }
    });
  }

  void _removeFromCart(int index) {
    setState(() {
      _cart.removeAt(index);
    });
  }

  double get total {
    return _cart.fold(0, (sum, item) => sum + (item["price"] * item["qty"]));
  }

  double get discount => double.tryParse(_discountController.text) ?? 0;
  double get tax => double.tryParse(_taxController.text) ?? 0;
  double get grandTotal => total - discount + tax;

  void _saveTransaction() async {
    int txId = await DBHelper.insertTransaction(
      {
            "date": DateTime.now().toIso8601String(),
            "total": total,
            "discount": discount,
            "tax": tax,
            "grand_total": grandTotal,
          }
          as List<Map<String, dynamic>>,
      grandTotal,
    );

    for (var item in _cart) {
      await DBHelper.insertTransactionItem({
        "transaction_id": txId,
        "product_id": item["id"],
        "qty": item["qty"],
        "price": item["price"],
      });
    }

    setState(() {
      _cart.clear();
      _discountController.text = "0";
      _taxController.text = "0";
    });

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text("Transaksi berhasil disimpan!")),
    );
  }

  @override
  Widget build(BuildContext context) {
    return POSScaffold(
      title: "Kasir POS",
      body: Row(
        children: [
          // =======================
          // List Produk
          // =======================
          Expanded(
            child: ListView.builder(
              itemCount: _products.length,
              itemBuilder: (context, index) {
                var p = _products[index];
                return ListTile(
                  title: Text(p["name"]),
                  subtitle: Text("Rp${p["price"]}"),
                  trailing: IconButton(
                    icon: const Icon(Icons.add_shopping_cart),
                    onPressed: () =>
                        _addToCart(p), // ⬅️ ini tombol tambah produk
                  ),
                );
              },
            ),
          ),

          // =======================
          // Cart
          // =======================
          Expanded(
            child: Column(
              children: [
                const Padding(
                  padding: EdgeInsets.all(8),
                  child: Text(
                    "Cart",
                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                  ),
                ),
                Expanded(
                  child: ListView.builder(
                    itemCount: _cart.length,
                    itemBuilder: (context, index) {
                      var item = _cart[index];
                      return ListTile(
                        title: Text(item["name"]),
                        subtitle: Text("Rp${item["price"]}"),
                        trailing: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            IconButton(
                              icon: const Icon(
                                Icons.remove_circle,
                                color: Colors.red,
                              ),
                              onPressed: () {
                                setState(() {
                                  if (item["qty"] > 1) {
                                    item["qty"] -= 1;
                                  } else {
                                    _cart.removeAt(index);
                                  }
                                });
                              },
                            ),
                            Text("${item["qty"]}"),
                            IconButton(
                              icon: const Icon(
                                Icons.add_circle,
                                color: Colors.green,
                              ),
                              onPressed: () {
                                setState(() {
                                  item["qty"] += 1;
                                });
                              },
                            ),
                          ],
                        ),
                      );
                    },
                  ),
                ),

                // Diskon & Pajak
                Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Column(
                    children: [
                      TextField(
                        controller: _discountController,
                        decoration: const InputDecoration(
                          labelText: "Diskon (Rp)",
                        ),
                        keyboardType: TextInputType.number,
                      ),
                      TextField(
                        controller: _taxController,
                        decoration: const InputDecoration(
                          labelText: "Pajak (Rp)",
                        ),
                        keyboardType: TextInputType.number,
                      ),
                    ],
                  ),
                ),

                // Summary
                Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text("Total: Rp$total"),
                      Text("Diskon: Rp$discount"),
                      Text("Pajak: Rp$tax"),
                      Text(
                        "Grand Total: Rp$grandTotal",
                        style: const TextStyle(fontWeight: FontWeight.bold),
                      ),
                    ],
                  ),
                ),

                // Tombol Simpan
                Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: ElevatedButton.icon(
                    icon: const Icon(Icons.save),
                    label: const Text("Simpan Transaksi"),
                    onPressed: _cart.isEmpty ? null : _saveTransaction,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
