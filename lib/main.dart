import 'package:flutter/material.dart';
import 'pages/pos_home.dart';

void main() {
  runApp(const POSApp());
}

class POSApp extends StatelessWidget {
  const POSApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: "Aplikasi Kasir Flutter",
      theme: ThemeData(primarySwatch: Colors.blue),
      home: const POSHome(),
    );
  }
}
