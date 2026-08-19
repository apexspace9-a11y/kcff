package com.kcff.safe.data

enum class TransactionType {
    SAVE,
    SPEND
}

data class Vault(
    val id: Long,
    val name: String,
    val target: Int,
    val deadline: String,
    val archived: Boolean,
    val createdAt: Long
)

data class KcTransaction(
    val id: Long,
    val vaultId: Long,
    val type: TransactionType,
    val amount: Int,
    val category: String,
    val note: String,
    val createdAt: Long
)

val expenseCategories = listOf(
    "Vòng quay",
    "Trang phục",
    "Súng",
    "Thẻ tháng",
    "Sự kiện",
    "Quà tặng",
    "Khác"
)
