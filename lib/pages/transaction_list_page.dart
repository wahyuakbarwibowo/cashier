import 'package:flutter/material.dart';
import '../db/db_helper.dart';
import '../main.dart';

class TransactionListPage extends StatefulWidget {
  const TransactionListPage({super.key});

  @override
  State<TransactionListPage> createState() => _TransactionListPageState();
}

class _TransactionListPageState extends State<TransactionListPage> {
  List<Map<String, dynamic>> _transactions = [];

  @override
  void initState() {
    super.initState();
    _loadTransactions();
  }

  void _loadTransactions() async {
    var data = await DBHelper.getTransactions();
    setState(() {
      _transactions = data;
    });
  }

  void _openDetail(Map<String, dynamic> tx) async {
    var items = await DBHelper.getTransactionItems(tx["id"]);
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: Text("Detail Transaksi #${tx["id"]}"),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text("Tanggal: ${tx["date"]}"),
            Text("Total: Rp${tx["total"]}"),
            Text("Diskon: Rp${tx["discount"]}"),
            Text("Pajak: Rp${tx["tax"]}"),
            Text("Grand Total: Rp${tx["grand_total"]}"),
            const Divider(),
            const Text("Items:"),
            ...items.map(
              (item) =>
                  Text("- ${item["name"]} x${item["qty"]} @Rp${item["price"]}"),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text("Tutup"),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return POSScaffold(
      title: "Laporan Transaksi",
      body: ListView.builder(
        itemCount: _transactions.length,
        itemBuilder: (context, index) {
          var tx = _transactions[index];
          return ListTile(
            title: Text("Transaksi #${tx["id"]} - Rp${tx["grand_total"]}"),
            subtitle: Text("Tanggal: ${tx["date"]}"),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => _openDetail(tx),
          );
        },
      ),
    );
  }
}
