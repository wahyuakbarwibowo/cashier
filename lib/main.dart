import 'package:flutter/material.dart';
import 'package:cashier/pages/pos_home.dart'; // pastikan path sesuai

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Aplikasi Kasir Flutter',
      theme: ThemeData(primarySwatch: Colors.blue),
      home: POSHome(), // 🔥 set POSHome sebagai halaman utama
    );
  }
}
