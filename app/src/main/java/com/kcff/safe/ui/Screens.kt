package com.kcff.safe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kcff.safe.data.KcRepository
import com.kcff.safe.data.TransactionType
import com.kcff.safe.data.Vault

@Composable
internal fun DashboardScreen(
    repository: KcRepository,
    modifier: Modifier = Modifier,
    onCreateVault: () -> Unit,
    onSave: (Long) -> Unit,
    onSpend: (Long) -> Unit
) {
    val activeVaults = repository.activeVaults
    val totalTarget = repository.totalTarget()
    val activeBalance = activeVaults.sumOf { repository.balance(it.id) }
    val overallProgress = if (totalTarget == 0) 0f else (activeBalance.toFloat() / totalTarget).coerceIn(0f, 1f)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Tổng KC trong két", style = MaterialTheme.typography.labelLarge)
                    Text(
                        formatKc(repository.totalBalance()),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black
                    )
                    LinearProgressIndicator(progress = { overallProgress }, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${activeVaults.size} két đang mở", style = MaterialTheme.typography.bodySmall)
                        Text("Mục tiêu ${formatKc(totalTarget)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Đã tiêu tháng này", formatKc(repository.spentThisMonth()), Modifier.weight(1f))
                SummaryCard("Tổng đã tiêu", formatKc(repository.totalSpent()), Modifier.weight(1f))
            }
        }

        if (activeVaults.isEmpty()) {
            item {
                EmptyCard(
                    title = "Chưa có két chiến dịch",
                    action = "Tạo két đầu tiên",
                    onAction = onCreateVault
                )
            }
        } else {
            item { SectionTitle("Két đang chạy") }
            items(activeVaults.take(3), key = { it.id }) { vault ->
                CompactVaultCard(
                    vault = vault,
                    balance = repository.balance(vault.id),
                    onSave = onSave,
                    onSpend = onSpend
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CompactVaultCard(
    vault: Vault,
    balance: Int,
    onSave: (Long) -> Unit,
    onSpend: (Long) -> Unit
) {
    val progress = (balance.toFloat() / vault.target).coerceIn(0f, 1f)
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(vault.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (vault.deadline.isNotBlank()) {
                        Text(
                            "Hạn ${vault.deadline}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text("${formatKc(balance)} / ${formatKc(vault.target)}")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onSave(vault.id) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Cất KC")
                }
                Button(
                    onClick = { onSpend(vault.id) },
                    modifier = Modifier.weight(1f),
                    enabled = balance > 0
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Chi KC")
                }
            }
        }
    }
}

@Composable
internal fun VaultsScreen(
    repository: KcRepository,
    modifier: Modifier = Modifier,
    onSave: (Long) -> Unit,
    onSpend: (Long) -> Unit
) {
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Vault?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val visibleVaults = repository.vaults
        .filter { if (showArchived) it.archived else !it.archived }
        .sortedByDescending { it.createdAt }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showArchived,
                    onClick = { showArchived = false },
                    label = { Text("Đang mở") }
                )
                FilterChip(
                    selected = showArchived,
                    onClick = { showArchived = true },
                    label = { Text("Đã đóng") }
                )
            }
        }

        if (visibleVaults.isEmpty()) {
            item { EmptyCard(if (showArchived) "Chưa có két đã đóng" else "Chưa có két đang mở") }
        } else {
            items(visibleVaults, key = { it.id }) { vault ->
                VaultCard(
                    vault = vault,
                    balance = repository.balance(vault.id),
                    onSave = { onSave(vault.id) },
                    onSpend = { onSpend(vault.id) },
                    onArchive = { repository.toggleArchive(vault.id) },
                    onDelete = { deleteTarget = vault }
                )
            }
        }
    }

    deleteTarget?.let { vault ->
        AlertDialog(
            onDismissRequest = {
                deleteTarget = null
                deleteError = null
            },
            title = { Text("Xoá két ${vault.name}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Chỉ xoá được két có số dư bằng 0.")
                    deleteError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.deleteVault(vault.id)
                            .onSuccess {
                                deleteTarget = null
                                deleteError = null
                            }
                            .onFailure { deleteError = it.message }
                    }
                ) { Text("Xoá") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deleteTarget = null
                        deleteError = null
                    }
                ) { Text("Huỷ") }
            }
        )
    }
}

@Composable
private fun VaultCard(
    vault: Vault,
    balance: Int,
    onSave: () -> Unit,
    onSpend: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = (balance.toFloat() / vault.target).coerceIn(0f, 1f)
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        vault.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (vault.deadline.isNotBlank()) {
                        Text("Hạn ${vault.deadline}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                IconButton(onClick = onArchive) {
                    Icon(if (vault.archived) Icons.Default.Restore else Icons.Default.Archive, contentDescription = null)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
            Text(formatKc(balance), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Mục tiêu ${formatKc(vault.target)}", style = MaterialTheme.typography.bodySmall)
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            if (!vault.archived) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onSave, modifier = Modifier.weight(1f)) { Text("Cất KC") }
                    Button(onClick = onSpend, modifier = Modifier.weight(1f), enabled = balance > 0) { Text("Chi KC") }
                }
            }
        }
    }
}

@Composable
internal fun TransactionsScreen(repository: KcRepository, modifier: Modifier = Modifier) {
    var filter by rememberSaveable { mutableStateOf("ALL") }
    val filteredTransactions = repository.transactions.filter { transaction ->
        when (filter) {
            "SAVE" -> transaction.type == TransactionType.SAVE
            "SPEND" -> transaction.type == TransactionType.SPEND
            else -> true
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter == "ALL", onClick = { filter = "ALL" }, label = { Text("Tất cả") })
                FilterChip(selected = filter == "SAVE", onClick = { filter = "SAVE" }, label = { Text("Cất vào") })
                FilterChip(selected = filter == "SPEND", onClick = { filter = "SPEND" }, label = { Text("Chi ra") })
            }
        }

        if (filteredTransactions.isEmpty()) {
            item { EmptyCard("Chưa có giao dịch KC") }
        } else {
            items(filteredTransactions, key = { it.id }) { transaction ->
                Card(shape = RoundedCornerShape(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (transaction.type == TransactionType.SAVE) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (transaction.type == TransactionType.SAVE) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                        )
                        Column(Modifier.weight(1f)) {
                            Text(repository.vaultName(transaction.vaultId), fontWeight = FontWeight.Bold)
                            val detail = listOf(transaction.category, transaction.note)
                                .filter { it.isNotBlank() }
                                .joinToString(" · ")
                            if (detail.isNotBlank()) {
                                Text(
                                    detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                formatDate(transaction.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = (if (transaction.type == TransactionType.SAVE) "+" else "−") + formatKc(transaction.amount),
                            fontWeight = FontWeight.Black,
                            color = if (transaction.type == TransactionType.SAVE) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCard(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (action != null && onAction != null) {
                Button(onClick = onAction) { Text(action) }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}
