import 'package:cashier/pages/transaction_detail_page.dart';
import 'package:flutter/material.dart';
import '../db/db_helper.dart';

class ReportPage extends StatefulWidget {
  const ReportPage({super.key});

  @override
  State<ReportPage> createState() => _ReportPageState();
}

class _ReportPageState extends State<ReportPage> {
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text("Laporan Transaksi")),
      body: ListView.builder(
        itemCount: _transactions.length,
        itemBuilder: (context, index) {
          var tx = _transactions[index];
          return ListTile(
            title: Text("Tanggal: ${tx["date"]}"),
            subtitle: Text(
              "Total: Rp${tx["total"]} | Diskon: Rp${tx["discount"]} | Pajak: Rp${tx["tax"]} | Grand: Rp${tx["grand_total"]}",
            ),
            trailing: const Icon(Icons.chevron_right),
            onTap: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => TransactionDetailPage(transaction: tx),
                ),
              );
            },
          );
        },
      ),
    );
  }
}
