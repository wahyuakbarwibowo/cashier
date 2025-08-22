import 'package:flutter/material.dart';
import 'pages/pos_home.dart';
import 'pages/product_page.dart';
import 'pages/transaction_list_page.dart';

void main() {
  runApp(const POSApp());
}

class POSApp extends StatelessWidget {
  const POSApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'POS App',
      theme: ThemeData(primarySwatch: Colors.blue),
      home: const POSHome(),
    );
  }
}

class POSScaffold extends StatelessWidget {
  final Widget body;
  final String title;

  const POSScaffold({super.key, required this.body, required this.title});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(title)),
      drawer: Drawer(
        child: ListView(
          children: [
            const DrawerHeader(
              decoration: BoxDecoration(color: Colors.blue),
              child: Text(
                "POS Menu",
                style: TextStyle(color: Colors.white, fontSize: 18),
              ),
            ),
            ListTile(
              leading: const Icon(Icons.point_of_sale),
              title: const Text("Kasir"),
              onTap: () {
                Navigator.pushReplacement(
                  context,
                  MaterialPageRoute(builder: (_) => const POSHome()),
                );
              },
            ),
            ListTile(
              leading: const Icon(Icons.inventory),
              title: const Text("Produk"),
              onTap: () {
                Navigator.pushReplacement(
                  context,
                  MaterialPageRoute(builder: (_) => const ProductPage()),
                );
              },
            ),
            ListTile(
              leading: const Icon(Icons.receipt_long),
              title: const Text("Transaksi"),
              onTap: () {
                Navigator.pushReplacement(
                  context,
                  MaterialPageRoute(
                    builder: (_) => const TransactionListPage(),
                  ),
                );
              },
            ),
          ],
        ),
      ),
      body: body,
    );
  }
}
