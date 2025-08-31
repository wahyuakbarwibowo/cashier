import 'package:flutter/material.dart';
import '../db/db_helper.dart';
import 'product_form_page.dart';
import '../main.dart';

class ProductPage extends StatefulWidget {
  const ProductPage({super.key});

  @override
  State<ProductPage> createState() => _ProductPageState();
}

class _ProductPageState extends State<ProductPage> {
  List<Map<String, dynamic>> _products = [];
  List<Map<String, dynamic>> _filtered = [];
  final TextEditingController _searchCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    _loadProducts();
    _searchCtrl.addListener(_filterProducts);
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadProducts() async {
    final data = await DBHelper.getProducts();
    setState(() {
      _products = data;
      _filtered = data; // awalnya semua tampil
    });
  }

  void _filterProducts() {
    String query = _searchCtrl.text.toLowerCase();
    setState(() {
      _filtered = _products
          .where((p) => p["name"].toLowerCase().contains(query))
          .toList();
    });
  }

  /// buka form tambah/edit
  void _openForm({Map<String, dynamic>? product}) async {
    await Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => ProductFormPage(product: product)),
    );
    _loadProducts(); // refresh setelah balik dari form
  }

  @override
  Widget build(BuildContext context) {
    return POSScaffold(
      title: "Manajemen Produk",
      body: Column(
        children: [
          // 🔍 Search bar
          Padding(
            padding: const EdgeInsets.all(8.0),
            child: TextField(
              controller: _searchCtrl,
              decoration: InputDecoration(
                prefixIcon: const Icon(Icons.search),
                hintText: "Cari produk...",
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
            ),
          ),

          // 📦 List produk
          Expanded(
            child: _filtered.isEmpty
                ? const Center(child: Text("Produk tidak ditemukan"))
                : ListView.builder(
                    itemCount: _filtered.length,
                    itemBuilder: (context, index) {
                      var p = _filtered[index];
                      return ListTile(
                        leading: CircleAvatar(
                          child: Text(p["name"][0].toUpperCase()),
                        ),
                        title: Text(p["name"]),
                        subtitle: Text("Rp${p["price"]}"),
                        trailing: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            IconButton(
                              icon: const Icon(Icons.edit, color: Colors.blue),
                              onPressed: () => _openForm(product: p),
                            ),
                            IconButton(
                              icon: const Icon(Icons.delete, color: Colors.red),
                              onPressed: () async {
                                await DBHelper.deleteProduct(p["id"]);
                                _loadProducts();
                              },
                            ),
                          ],
                        ),
                      );
                    },
                  ),
          ),

          // ➕ Tombol tambah produk
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: ElevatedButton.icon(
              onPressed: () => _openForm(), // tambah produk baru
              icon: const Icon(Icons.add),
              label: const Text("Tambah Produk"),
            ),
          ),
        ],
      ),
    );
  }
}
