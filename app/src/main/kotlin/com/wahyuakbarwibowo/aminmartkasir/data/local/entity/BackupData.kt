package com.wahyuakbarwibowo.aminmartkasir.data.local.entity

data class BackupData(
    val products: List<ProductEntity>? = null,
    val productVariants: List<ProductVariantEntity>? = null,
    val customers: List<CustomerEntity>? = null,
    val paymentMethods: List<PaymentMethodEntity>? = null,
    val shopProfile: ShopProfileEntity? = null,
    val sales: List<SaleEntity>? = null,
    val saleItems: List<SaleItemEntity>? = null,
    val suppliers: List<SupplierEntity>? = null,
    val purchases: List<PurchaseEntity>? = null,
    val purchaseItems: List<PurchaseItemEntity>? = null,
    val receivables: List<ReceivableEntity>? = null,
    val payables: List<PayableEntity>? = null,
    val phoneHistory: List<PhoneHistoryEntity>? = null,
    val digitalProducts: List<DigitalProductEntity>? = null,
    val digitalCategories: List<DigitalCategoryEntity>? = null,
    val expenses: List<ExpenseEntity>? = null,
    val customerPointsHistory: List<CustomerPointsHistoryEntity>? = null,
    val stockHistory: List<StockHistoryEntity>? = null
)
