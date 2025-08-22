import 'package:flutter/material.dart';
import '../db/db_helper.dart';

class ReportPage extends StatefulWidget {
  const ReportPage({super.key});

  @override
  State<ReportPage> createState() => _ReportPageState();
}

class _ReportPageState extends State<ReportPage> {
  List<Map<String, dynamic>> transactions = [];

  @override
  void initState() {
    super.initState();
    _loadTransactions();
  }

  Future<void> _loadTransactions() async {
    final trx = await DBHelper.getTransactions();
    setState(() {
      transactions = trx;
    });
  }

  void _openDetail(int trxId) async {
    final items = await DBHelper.getTransactionItems(trxId);
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text("Detail Transaksi"),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: items
              .map(
                (item) => ListTile(
                  title: Text(item["product_name"]),
                  subtitle: Text("Qty: ${item["qty"]}"),
                  trailing: Text("Rp${item["price"] * item["qty"]}"),
                ),
              )
              .toList(),
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
    return Scaffold(
      appBar: AppBar(title: const Text("Laporan Penjualan")),
      body: ListView.builder(
        itemCount: transactions.length,
        itemBuilder: (context, index) {
          final trx = transactions[index];
          return ListTile(
            title: Text("Transaksi #${trx['id']}"),
            subtitle: Text(
              "Total: Rp${trx["total"]} "
              "Diskon: Rp${trx["discount"]} "
              "Pajak: Rp${trx["tax"]} "
              "Grand: Rp${trx["grand_total"]}",
            ),
            trailing: Text("Rp${trx['total']}"),
            onTap: () => _openDetail(trx['id']),
          );
        },
      ),
    );
  }
}
