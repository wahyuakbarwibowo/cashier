import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';

class DBHelper {
  static Database? _db;

  static Future<Database> get database async {
    if (_db != null) return _db!;
    _db = await _initDB();
    return _db!;
  }

  static Future<Database> _initDB() async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, 'pos.db');

    return await openDatabase(
      path,
      version: 2,
      onCreate: (db, version) async {
        await db.execute('''
          CREATE TABLE products (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            date TEXT,
            total REAL,
            discount REAL,
            tax REAL,
            grand_total REAL
          )
        ''');

        await db.execute('''
          CREATE TABLE transactions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            date TEXT,
            total REAL
          )
        ''');

        await db.execute('''
          CREATE TABLE transaction_items (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            transaction_id INTEGER,
            product_id INTEGER,
            price REAL,
            qty INTEGER,
            FOREIGN KEY(transaction_id) REFERENCES transactions(id)
          )
        ''');

        // contoh produk awal
        await db.insert("products", {
          "barcode": "123456789",
          "name": "Teh Botol",
          "price": 5000,
        });
        await db.insert("products", {
          "barcode": "987654321",
          "name": "Indomie Goreng",
          "price": 3000,
        });
      },
    );
  }

  static Future<List<Map<String, dynamic>>> getTransactionItems(
    int transactionId,
  ) async {
    final db = await database;
    return await db.rawQuery(
      '''
        SELECT ti.id, ti.qty, ti.price, ti.product_id, p.name as product_name
        FROM transaction_items ti
        LEFT JOIN products p ON p.id = ti.product_id
        WHERE ti.transaction_id = ?
      ''',
      [transactionId],
    );
  }

  static Future<Map<String, dynamic>?> getProductByBarcode(
    String barcode,
  ) async {
    final db = await database;
    final result = await db.query(
      "products",
      where: "barcode = ?",
      whereArgs: [barcode],
    );
    return result.isNotEmpty ? result.first : null;
  }

  static Future<int> insertTransaction(
    List<Map<String, dynamic>> cart,
    double total,
  ) async {
    final db = await database;
    int trxId = await db.insert("transactions", {
      "date": DateTime.now().toIso8601String(),
      "total": total,
    });

    for (var item in cart) {
      await db.insert("transaction_items", {
        "transaction_id": trxId,
        "product_name": item["name"],
        "price": item["price"],
        "qty": item["qty"] ?? 1,
      });
    }

    return trxId;
  }

  static Future<List<Map<String, dynamic>>> getTransactions() async {
    final db = await database;
    return await db.query("transactions", orderBy: "date DESC");
  }

  static Future<int> insertTransactionItem(Map<String, dynamic> item) async {
    final db = await database;
    return await db.insert("transaction_items", item);
  }

  static Future<int> insertProduct(Map<String, dynamic> product) async {
    final db = await database;
    return await db.insert("products", product);
  }

  static Future<int> updateProduct(int id, Map<String, dynamic> product) async {
    final db = await database;
    return await db.update(
      "products",
      product,
      where: "id = ?",
      whereArgs: [id],
    );
  }

  static Future<int> deleteProduct(int id) async {
    final db = await database;
    return await db.delete("products", where: "id = ?", whereArgs: [id]);
  }

  static Future<List<Map<String, dynamic>>> getProducts() async {
    final db = await database;
    return await db.query("products", orderBy: "name ASC");
  }
}
