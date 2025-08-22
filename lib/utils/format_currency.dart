import 'package:intl/intl.dart';

String formatRupiah(num number) {
  final format = NumberFormat.currency(
    locale: 'id',
    symbol: 'Rp',
    decimalDigits: 0,
  );
  return format.format(number);
}
