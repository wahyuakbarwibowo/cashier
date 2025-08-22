import 'package:flutter/material.dart';
import '../db/db_helper.dart';

class TransactionDetailPage extends StatefulWidget {
  final Map<String, dynamic> transaction;
  const TransactionDetailPage({super.key, required this.transaction});

  @override
  State<TransactionDetailPage> createState() => _TransactionDetailPageState();
}

class _TransactionDetailPageState extends State<TransactionDetailPage> {
  List<Map<String, dynamic>> _items = [];

  @override
  void initState() {
    super.initState();
    _loadItems();
  }

  void _loadItems() async {
    var data = await DBHelper.getTransactionItems(widget.transaction["id"]);
    setState(() {
      _items = data;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text("Detail Transaksi")),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ListTile(
            title: Text("Tanggal: ${widget.transaction["date"]}"),
            subtitle: Text(
              "Total: Rp${widget.transaction["total"]}\n"
              "Diskon: Rp${widget.transaction["discount"]}\n"
              "Pajak: Rp${widget.transaction["tax"]}\n"
              "Grand Total: Rp${widget.transaction["grand_total"]}",
            ),
          ),
          const Divider(),
          const Padding(
            padding: EdgeInsets.all(8.0),
            child: Text(
              "Item Belanja:",
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
          Expanded(
            child: ListView.builder(
              itemCount: _items.length,
              itemBuilder: (context, index) {
                var item = _items[index];
                return ListTile(
                  title: Text(
                    item["product_name"] ?? "Produk #${item["product_id"]}",
                  ),
                  subtitle: Text("Qty: ${item["qty"]} x Rp${item["price"]}"),
                  trailing: Text("Rp${item["qty"] * item["price"]}"),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
