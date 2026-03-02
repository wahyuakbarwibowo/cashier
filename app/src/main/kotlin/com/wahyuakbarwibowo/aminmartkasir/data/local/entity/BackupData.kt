package com.wahyuakbarwibowo.aminmartkasir.data.local.entity

data class BackupData(
    val products: List<ProductEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val paymentMethods: List<PaymentMethodEntity> = emptyList(),
    val shopProfile: ShopProfileEntity? = null,
    val sales: List<SaleEntity> = emptyList(),
    val saleItems: List<SaleItemEntity> = emptyList(),
    val suppliers: List<SupplierEntity> = emptyList(),
    val purchases: List<PurchaseEntity> = emptyList(),
    val purchaseItems: List<PurchaseItemEntity> = emptyList(),
    val receivables: List<ReceivableEntity> = emptyList(),
    val payables: List<PayableEntity> = emptyList(),
    val phoneHistory: List<PhoneHistoryEntity> = emptyList(),
    val digitalProducts: List<DigitalProductEntity> = emptyList(),
    val digitalCategories: List<DigitalCategoryEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val customerPointsHistory: List<CustomerPointsHistoryEntity> = emptyList(),
    val stockHistory: List<StockHistoryEntity> = emptyList()
)
