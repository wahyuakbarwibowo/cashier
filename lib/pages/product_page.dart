import 'package:cashier/main.dart';
import 'package:flutter/material.dart';
import '../db/db_helper.dart';
import 'product_form_page.dart';

class ProductPage extends StatefulWidget {
  const ProductPage({super.key});

  @override
  State<ProductPage> createState() => _ProductPageState();
}

class _ProductPageState extends State<ProductPage> {
  List<Map<String, dynamic>> products = [];

  @override
  void initState() {
    super.initState();
    _loadProducts();
  }

  Future<void> _loadProducts() async {
    final data = await DBHelper.getProducts();
    setState(() {
      products = data;
    });
  }

  void _deleteProduct(int id) async {
    await DBHelper.deleteProduct(id);
    _loadProducts();
  }

  void _openForm({Map<String, dynamic>? product}) async {
    await Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => ProductFormPage(product: product)),
    );
    _loadProducts();
  }

  @override
  Widget build(BuildContext context) {
    return POSScaffold(
      title: "Manajemen Produk", // ⬅️ ini harus String
      body: Column(
        children: [
          Expanded(
            child: ListView.builder(
              itemCount: products.length,
              itemBuilder: (context, index) {
                var p = products[index];
                return ListTile(
                  title: Text(p["name"]),
                  subtitle: Text("Rp${p["price"]}"),
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      IconButton(
                        icon: const Icon(Icons.edit),
                        onPressed: () => _openForm(product: p),
                      ),
                      IconButton(
                        icon: const Icon(Icons.delete),
                        onPressed: () {
                          DBHelper.deleteProduct(p["id"]);
                          _loadProducts();
                        },
                      ),
                    ],
                  ),
                );
              },
            ),
          ),
          ElevatedButton(
            // ⬅️ ini sudah benar ada di children Column
            onPressed: () => _openForm(),
            child: const Text("Tambah Produk"),
          ),
        ],
      ),
    );
  }
}
