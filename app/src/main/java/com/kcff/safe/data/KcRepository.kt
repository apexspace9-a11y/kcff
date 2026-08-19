package com.kcff.safe.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.YearMonth

class KcRepository(context: Context) {
    private val prefs = context.getSharedPreferences("kcff_store", Context.MODE_PRIVATE)
    private val _vaults = mutableStateListOf<Vault>()
    private val _transactions = mutableStateListOf<KcTransaction>()

    val vaults: List<Vault> get() = _vaults
    val transactions: List<KcTransaction> get() = _transactions.sortedByDescending { it.createdAt }
    val activeVaults: List<Vault> get() = _vaults.filterNot { it.archived }.sortedByDescending { it.createdAt }

    init {
        load()
    }

    fun createVault(name: String, target: Int, deadline: String, initialAmount: Int): Result<Long> {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return Result.failure(IllegalArgumentException("Nhập tên chiến dịch"))
        if (target <= 0) return Result.failure(IllegalArgumentException("Mục tiêu KC phải lớn hơn 0"))
        if (initialAmount < 0) return Result.failure(IllegalArgumentException("KC ban đầu không hợp lệ"))

        val now = System.currentTimeMillis()
        val id = nextId(now)
        _vaults += Vault(
            id = id,
            name = cleanName,
            target = target,
            deadline = deadline.trim(),
            archived = false,
            createdAt = now
        )
        if (initialAmount > 0) {
            _transactions += KcTransaction(
                id = nextId(now + 1),
                vaultId = id,
                type = TransactionType.SAVE,
                amount = initialAmount,
                category = "",
                note = "KC ban đầu",
                createdAt = now
            )
        }
        persist()
        return Result.success(id)
    }

    fun addTransaction(
        vaultId: Long,
        type: TransactionType,
        amount: Int,
        category: String,
        note: String
    ): Result<Unit> {
        val vault = _vaults.firstOrNull { it.id == vaultId }
            ?: return Result.failure(IllegalArgumentException("Két không tồn tại"))
        if (vault.archived) return Result.failure(IllegalArgumentException("Két này đã đóng"))
        if (amount <= 0) return Result.failure(IllegalArgumentException("Số KC phải lớn hơn 0"))
        if (type == TransactionType.SPEND && amount > balance(vaultId)) {
            return Result.failure(IllegalArgumentException("Két không đủ KC"))
        }

        _transactions += KcTransaction(
            id = nextId(System.currentTimeMillis()),
            vaultId = vaultId,
            type = type,
            amount = amount,
            category = if (type == TransactionType.SPEND) category.ifBlank { "Khác" } else "",
            note = note.trim(),
            createdAt = System.currentTimeMillis()
        )
        persist()
        return Result.success(Unit)
    }

    fun toggleArchive(vaultId: Long) {
        val index = _vaults.indexOfFirst { it.id == vaultId }
        if (index < 0) return
        _vaults[index] = _vaults[index].copy(archived = !_vaults[index].archived)
        persist()
    }

    fun deleteVault(vaultId: Long): Result<Unit> {
        val vault = _vaults.firstOrNull { it.id == vaultId }
            ?: return Result.failure(IllegalArgumentException("Két không tồn tại"))
        if (balance(vaultId) != 0) {
            return Result.failure(IllegalArgumentException("Hãy đưa số dư két về 0 trước khi xoá"))
        }
        _vaults.remove(vault)
        _transactions.removeAll { it.vaultId == vaultId }
        persist()
        return Result.success(Unit)
    }

    fun balance(vaultId: Long): Int = _transactions
        .asSequence()
        .filter { it.vaultId == vaultId }
        .sumOf { if (it.type == TransactionType.SAVE) it.amount else -it.amount }

    fun totalBalance(): Int = _vaults.sumOf { balance(it.id) }

    fun totalTarget(): Int = activeVaults.sumOf { it.target }

    fun spentThisMonth(): Int {
        val currentMonth = YearMonth.now()
        val zone = ZoneId.systemDefault()
        return _transactions.asSequence()
            .filter { it.type == TransactionType.SPEND }
            .filter {
                val date = Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
                YearMonth.from(date) == currentMonth
            }
            .sumOf { it.amount }
    }

    fun totalSpent(): Int = _transactions
        .filter { it.type == TransactionType.SPEND }
        .sumOf { it.amount }

    fun vaultName(vaultId: Long): String = _vaults.firstOrNull { it.id == vaultId }?.name ?: "Két đã xoá"

    private fun nextId(seed: Long): Long {
        var candidate = seed
        while (_vaults.any { it.id == candidate } || _transactions.any { it.id == candidate }) {
            candidate++
        }
        return candidate
    }

    private fun persist() {
        val vaultArray = JSONArray()
        _vaults.forEach { vault ->
            vaultArray.put(JSONObject().apply {
                put("id", vault.id)
                put("name", vault.name)
                put("target", vault.target)
                put("deadline", vault.deadline)
                put("archived", vault.archived)
                put("createdAt", vault.createdAt)
            })
        }

        val transactionArray = JSONArray()
        _transactions.forEach { tx ->
            transactionArray.put(JSONObject().apply {
                put("id", tx.id)
                put("vaultId", tx.vaultId)
                put("type", tx.type.name)
                put("amount", tx.amount)
                put("category", tx.category)
                put("note", tx.note)
                put("createdAt", tx.createdAt)
            })
        }

        prefs.edit()
            .putString("vaults", vaultArray.toString())
            .putString("transactions", transactionArray.toString())
            .apply()
    }

    private fun load() {
        runCatching {
            val vaultArray = JSONArray(prefs.getString("vaults", "[]") ?: "[]")
            for (i in 0 until vaultArray.length()) {
                val item = vaultArray.getJSONObject(i)
                _vaults += Vault(
                    id = item.getLong("id"),
                    name = item.getString("name"),
                    target = item.getInt("target"),
                    deadline = item.optString("deadline", ""),
                    archived = item.optBoolean("archived", false),
                    createdAt = item.optLong("createdAt", item.getLong("id"))
                )
            }

            val txArray = JSONArray(prefs.getString("transactions", "[]") ?: "[]")
            for (i in 0 until txArray.length()) {
                val item = txArray.getJSONObject(i)
                _transactions += KcTransaction(
                    id = item.getLong("id"),
                    vaultId = item.getLong("vaultId"),
                    type = TransactionType.valueOf(item.getString("type")),
                    amount = item.getInt("amount"),
                    category = item.optString("category", ""),
                    note = item.optString("note", ""),
                    createdAt = item.optLong("createdAt", item.getLong("id"))
                )
            }
        }.onFailure {
            _vaults.clear()
            _transactions.clear()
        }
    }
}
