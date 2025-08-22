import 'package:flutter/material.dart';
import '../db/db_helper.dart';

class ProductFormPage extends StatefulWidget {
  final Map<String, dynamic>? product;
  const ProductFormPage({super.key, this.product});

  @override
  State<ProductFormPage> createState() => _ProductFormPageState();
}

class _ProductFormPageState extends State<ProductFormPage> {
  final _formKey = GlobalKey<FormState>();
  final _barcodeCtrl = TextEditingController();
  final _nameCtrl = TextEditingController();
  final _priceCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    if (widget.product != null) {
      _barcodeCtrl.text = widget.product!["barcode"];
      _nameCtrl.text = widget.product!["name"];
      _priceCtrl.text = widget.product!["price"].toString();
    }
  }

  void _saveProduct() async {
    if (_formKey.currentState!.validate()) {
      final product = {
        "barcode": _barcodeCtrl.text,
        "name": _nameCtrl.text,
        "price": double.tryParse(_priceCtrl.text) ?? 0,
      };

      if (widget.product == null) {
        await DBHelper.insertProduct(product);
      } else {
        await DBHelper.updateProduct(widget.product!["id"], product);
      }

      Navigator.pop(context);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.product == null ? "Tambah Produk" : "Edit Produk"),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Form(
          key: _formKey,
          child: Column(
            children: [
              TextFormField(
                controller: _barcodeCtrl,
                decoration: const InputDecoration(labelText: "Barcode"),
                validator: (v) => v!.isEmpty ? "Barcode wajib diisi" : null,
              ),
              TextFormField(
                controller: _nameCtrl,
                decoration: const InputDecoration(labelText: "Nama Produk"),
                validator: (v) => v!.isEmpty ? "Nama wajib diisi" : null,
              ),
              TextFormField(
                controller: _priceCtrl,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: "Harga"),
                validator: (v) => v!.isEmpty ? "Harga wajib diisi" : null,
              ),
              const SizedBox(height: 20),
              ElevatedButton.icon(
                onPressed: _saveProduct,
                icon: const Icon(Icons.save),
                label: const Text("Simpan"),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
