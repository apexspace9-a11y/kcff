package com.kcff.safe.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kcff.safe.R
import com.kcff.safe.data.KcRepository
import com.kcff.safe.data.TransactionType
import com.kcff.safe.data.Vault
import kotlin.math.max

@Composable
internal fun DashboardScreen(
    repository: KcRepository,
    modifier: Modifier = Modifier,
    onCreateVault: () -> Unit,
    onEditBudget: () -> Unit,
    onSave: (Long) -> Unit,
    onSpend: (Long) -> Unit
) {
    val activeVaults = repository.activeVaults
    val totalTarget = repository.totalTarget()
    val activeBalance = activeVaults.sumOf { repository.balance(it.id) }
    val totalBalance = repository.totalBalance()
    val overallProgress = if (totalTarget == 0) 0f else (activeBalance.toFloat() / totalTarget).coerceIn(0f, 1f)
    val spentMonth = repository.spentThisMonth()
    val savedMonth = repository.savedThisMonth()
    val monthlyBudget = repository.monthlyBudget
    val budgetProgress = if (monthlyBudget <= 0) 0f else (spentMonth.toFloat() / monthlyBudget).coerceIn(0f, 1f)
    val topExpense = repository.topExpenseCategory()
    val closestVault = activeVaults
        .filter { repository.balance(it.id) < it.target }
        .maxByOrNull { repository.balance(it.id).toFloat() / it.target }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            BrandHeroCard(
                balance = totalBalance,
                vaultCount = activeVaults.size,
                overallProgress = overallProgress,
                totalTarget = totalTarget
            )
        }

        item { BrandSectionTitle("Tài nguyên tháng này", "Dòng KC vào và ra") }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Cất vào", formatKc(savedMonth), KcGreen, Modifier.weight(1f))
                MetricCard("Chi ra", formatKc(spentMonth), KcRed, Modifier.weight(1f))
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    "Còn thiếu mục tiêu",
                    formatKc(max(totalTarget - activeBalance, 0)),
                    KcCyan,
                    Modifier.weight(1f)
                )
                MetricCard(
                    "Nhóm chi nhiều nhất",
                    topExpense?.let { "${it.first}\n${formatKc(it.second)}" } ?: "Chưa có",
                    KcViolet,
                    Modifier.weight(1f)
                )
            }
        }

        item {
            BudgetCard(
                budget = monthlyBudget,
                spent = spentMonth,
                progress = budgetProgress,
                onEdit = onEditBudget
            )
        }

        if (activeVaults.isEmpty()) {
            item {
                EmptyCard(
                    title = "Chưa có két chiến dịch",
                    subtitle = "Tạo một két cho sự kiện, skin hoặc vòng quay mà bạn đang gom KC.",
                    action = "Tạo két đầu tiên",
                    onAction = onCreateVault
                )
            }
        } else {
            item { BrandSectionTitle("Két đang chạy", "${activeVaults.size} mục tiêu đang mở") }

            closestVault?.let { vault ->
                item {
                    val balance = repository.balance(vault.id)
                    val remaining = max(vault.target - balance, 0)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10263B)),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.border(1.dp, KcGold.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Sắp chạm mục tiêu", color = KcGold, fontWeight = FontWeight.Bold)
                                Text(vault.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "Còn ${formatKc(remaining)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "${((balance.toFloat() / vault.target) * 100).toInt()}%",
                                style = MaterialTheme.typography.headlineSmall,
                                color = KcCyan,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

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
private fun BrandHeroCard(
    balance: Int,
    vaultCount: Int,
    overallProgress: Float,
    totalTarget: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF09243B), Color(0xFF111B3D), Color(0xFF07101D))
                )
            )
            .border(1.dp, KcCyan.copy(alpha = 0.35f), RoundedCornerShape(26.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "TÀI SẢN KC",
                    color = KcGold,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    formatKc(balance),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
                LinearProgressIndicator(
                    progress = { overallProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = KcCyan,
                    trackColor = Color.White.copy(alpha = 0.12f)
                )
                Text(
                    if (totalTarget > 0) "$vaultCount két · Mục tiêu ${formatKc(totalTarget)}" else "$vaultCount két đang mở",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(10.dp))
            Image(
                painter = painterResource(R.drawable.ic_kc_crystal),
                contentDescription = null,
                modifier = Modifier.size(width = 112.dp, height = 86.dp)
            )
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = KcSurfaceRaised),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(
                Modifier
                    .size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent)
            )
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BudgetCard(budget: Int, spent: Int, progress: Float, onEdit: () -> Unit) {
    val exceeded = budget > 0 && spent > budget
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = KcSurfaceRaised),
        modifier = Modifier.border(
            1.dp,
            (if (exceeded) KcRed else KcGold).copy(alpha = 0.32f),
            RoundedCornerShape(22.dp)
        )
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Kỷ luật chi tháng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (budget > 0) {
                            if (exceeded) "Đã vượt ${formatKc(spent - budget)}" else "Còn ${formatKc(max(budget - spent, 0))} trước hạn mức"
                        } else {
                            "Chưa đặt trần chi KC"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (exceeded) KcRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Sửa hạn mức", tint = KcGold)
                }
            }
            if (budget > 0) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (exceeded) KcRed else KcGold,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Đã chi ${formatKc(spent)}", style = MaterialTheme.typography.labelSmall)
                    Text("Trần ${formatKc(budget)}", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Text("Đặt hạn mức tháng")
                }
            }
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
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = KcSurface),
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
    ) {
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
                Text("${(progress * 100).toInt()}%", color = KcCyan, fontWeight = FontWeight.Black)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = KcCyan,
                trackColor = Color.White.copy(alpha = 0.08f)
            )
            Text("${formatKc(balance)} / ${formatKc(vault.target)}", fontWeight = FontWeight.Bold)
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
    val archivedCount = repository.vaults.count { it.archived }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = KcSurfaceRaised),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.border(1.dp, KcCyan.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_kc_crystal),
                        contentDescription = null,
                        modifier = Modifier.size(width = 70.dp, height = 52.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text("Kho két KC", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(
                            "${repository.activeVaults.size} đang mở · $archivedCount đã đóng",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(formatKc(repository.totalBalance()), color = KcCyan, fontWeight = FontWeight.Black)
                }
            }
        }

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
            item {
                EmptyCard(
                    title = if (showArchived) "Chưa có két đã đóng" else "Chưa có két đang mở",
                    subtitle = if (showArchived) "Các két hoàn tất sẽ nằm ở đây." else "Tạo két mới để bắt đầu gom KC theo mục tiêu."
                )
            }
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
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = KcSurface),
        modifier = Modifier.border(1.dp, KcBlue.copy(alpha = 0.28f), RoundedCornerShape(22.dp))
    ) {
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
                        Text(
                            "Hạn ${vault.deadline}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onArchive) {
                    Icon(
                        if (vault.archived) Icons.Default.Restore else Icons.Default.Archive,
                        contentDescription = null,
                        tint = KcGold
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = KcRed)
                }
            }
            Text(formatKc(balance), style = MaterialTheme.typography.headlineMedium, color = KcCyan, fontWeight = FontWeight.Black)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = KcCyan,
                trackColor = Color.White.copy(alpha = 0.08f)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Mục tiêu ${formatKc(vault.target)}", style = MaterialTheme.typography.bodySmall)
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = KcGold, fontWeight = FontWeight.Bold)
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
    var query by rememberSaveable { mutableStateOf("") }
    val filteredTransactions = repository.transactions.filter { transaction ->
        val matchesType = when (filter) {
            "SAVE" -> transaction.type == TransactionType.SAVE
            "SPEND" -> transaction.type == TransactionType.SPEND
            else -> true
        }
        val searchable = listOf(
            repository.vaultName(transaction.vaultId),
            transaction.category,
            transaction.note
        ).joinToString(" ")
        matchesType && (query.isBlank() || searchable.contains(query.trim(), ignoreCase = true))
    }
    val visibleAmount = filteredTransactions.sumOf { it.amount }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("Tìm két, nhóm chi, ghi chú") }
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter == "ALL", onClick = { filter = "ALL" }, label = { Text("Tất cả") })
                FilterChip(selected = filter == "SAVE", onClick = { filter = "SAVE" }, label = { Text("Cất vào") })
                FilterChip(selected = filter == "SPEND", onClick = { filter = "SPEND" }, label = { Text("Chi ra") })
            }
        }

        item {
            Text(
                "${filteredTransactions.size} giao dịch · ${formatKc(visibleAmount)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (filteredTransactions.isEmpty()) {
            item {
                EmptyCard(
                    title = "Không có giao dịch phù hợp",
                    subtitle = if (query.isBlank()) "Nhật ký KC sẽ xuất hiện sau khi bạn cất hoặc chi." else "Thử từ khoá khác hoặc đổi bộ lọc."
                )
            }
        } else {
            items(filteredTransactions, key = { it.id }) { transaction ->
                val isSave = transaction.type == TransactionType.SAVE
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = KcSurface),
                    modifier = Modifier.border(
                        1.dp,
                        (if (isSave) KcGreen else KcRed).copy(alpha = 0.18f),
                        RoundedCornerShape(18.dp)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background((if (isSave) KcGreen else KcRed).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSave) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (isSave) KcGreen else KcRed
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(repository.vaultName(transaction.vaultId), fontWeight = FontWeight.Bold)
                            val detail = listOf(transaction.category, transaction.note)
                                .filter { it.isNotBlank() }
                                .joinToString(" · ")
                            if (detail.isNotBlank()) {
                                Text(
                                    detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                formatDate(transaction.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = (if (isSave) "+" else "−") + formatKc(transaction.amount),
                            fontWeight = FontWeight.Black,
                            color = if (isSave) KcGreen else KcRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCard(
    title: String,
    subtitle: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = KcSurfaceRaised)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_kc_crystal),
                contentDescription = null,
                modifier = Modifier.size(width = 86.dp, height = 64.dp)
            )
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (action != null && onAction != null) {
                Button(onClick = onAction) { Text(action) }
            }
        }
    }
}

@Composable
private fun BrandSectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
