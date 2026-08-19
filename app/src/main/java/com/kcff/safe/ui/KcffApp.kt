package com.kcff.safe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kcff.safe.data.KcRepository
import com.kcff.safe.data.TransactionType
import com.kcff.safe.data.Vault
import com.kcff.safe.data.expenseCategories
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class DialogMode { NONE, CREATE_VAULT, SAVE, SPEND }

@Composable
fun KcffApp(repository: KcRepository) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var dialogMode by remember { mutableStateOf(DialogMode.NONE) }
    var selectedVaultId by remember { mutableStateOf<Long?>(null) }
    val snackbar = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Két Sắt KC", fontWeight = FontWeight.Bold)
                        Text(
                            when (selectedTab) {
                                0 -> "Tổng quan"
                                1 -> "Chiến dịch & sự kiện"
                                else -> "Lịch sử KC"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Tổng quan") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, null) },
                    label = { Text("Két") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.ReceiptLong, null) },
                    label = { Text("Chi tiêu") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    selectedVaultId = null
                    dialogMode = if (selectedTab == 1 || repository.activeVaults.isEmpty()) {
                        DialogMode.CREATE_VAULT
                    } else {
                        DialogMode.SPEND
                    }
                },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(if (selectedTab == 1 || repository.activeVaults.isEmpty()) "Tạo két" else "Ghi chi tiêu") }
            )
        }
    ) { padding ->
        when (selectedTab) {
            0 -> DashboardScreen(
                repository = repository,
                modifier = Modifier.padding(padding),
                onCreateVault = { dialogMode = DialogMode.CREATE_VAULT },
                onSave = { id -> selectedVaultId = id; dialogMode = DialogMode.SAVE },
                onSpend = { id -> selectedVaultId = id; dialogMode = DialogMode.SPEND }
            )
            1 -> VaultsScreen(
                repository = repository,
                modifier = Modifier.padding(padding),
                onSave = { id -> selectedVaultId = id; dialogMode = DialogMode.SAVE },
                onSpend = { id -> selectedVaultId = id; dialogMode = DialogMode.SPEND }
            )
            else -> TransactionsScreen(repository, Modifier.padding(padding))
        }
    }

    when (dialogMode) {
        DialogMode.CREATE_VAULT -> CreateVaultDialog(repository) { dialogMode = DialogMode.NONE }
        DialogMode.SAVE -> TransactionDialog(
            repository = repository,
            type = TransactionType.SAVE,
            preselectedVaultId = selectedVaultId,
            onDismiss = { dialogMode = DialogMode.NONE }
        )
        DialogMode.SPEND -> TransactionDialog(
            repository = repository,
            type = TransactionType.SPEND,
            preselectedVaultId = selectedVaultId,
            onDismiss = { dialogMode = DialogMode.NONE }
        )
        DialogMode.NONE -> Unit
    }
}

@Composable
private fun DashboardScreen(
    repository: KcRepository,
    modifier: Modifier = Modifier,
    onCreateVault: () -> Unit,
    onSave: (Long) -> Unit,
    onSpend: (Long) -> Unit
) {
    val active = repository.activeVaults
    val totalBalance = repository.totalBalance()
    val totalTarget = repository.totalTarget()
    val overallProgress = if (totalTarget <= 0) 0f else (active.sumOf { repository.balance(it.id) }.toFloat() / totalTarget).coerceIn(0f, 1f)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tổng KC trong két", style = MaterialTheme.typography.labelLarge)
                    Text(formatKc(totalBalance), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                    LinearProgressIndicator(progress = { overallProgress }, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${active.size} két đang mở", style = MaterialTheme.typography.bodySmall)
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

        if (active.isEmpty()) {
            item {
                EmptyCard(
                    title = "Chưa có két chiến dịch",
                    action = "Tạo két đầu tiên",
                    onAction = onCreateVault
                )
            }
        } else {
            item { SectionTitle("Két đang chạy") }
            items(active.take(3), key = { it.id }) { vault ->
                CompactVaultCard(vault, repository.balance(vault.id), onSave, onSpend)
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
private fun CompactVaultCard(vault: Vault, balance: Int, onSave: (Long) -> Unit, onSpend: (Long) -> Unit) {
    val progress = (balance.toFloat() / vault.target).coerceIn(0f, 1f)
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(vault.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (vault.deadline.isNotBlank()) {
                        Text("Hạn ${vault.deadline}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text("${formatKc(balance)} / ${formatKc(vault.target)}", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onSave(vault.id) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.ArrowDownward, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Cất KC")
                }
                Button(onClick = { onSpend(vault.id) }, modifier = Modifier.weight(1f), enabled = balance > 0) {
                    Icon(Icons.Default.ArrowUpward, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Chi KC")
                }
            }
        }
    }
}

@Composable
private fun VaultsScreen(
    repository: KcRepository,
    modifier: Modifier = Modifier,
    onSave: (Long) -> Unit,
    onSpend: (Long) -> Unit
) {
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Vault?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val display = repository.vaults
        .filter { if (showArchived) it.archived else !it.archived }
        .sortedByDescending { it.createdAt }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { showArchived = false }, label = { Text("Đang mở") })
                AssistChip(onClick = { showArchived = true }, label = { Text("Đã đóng") })
            }
        }
        if (display.isEmpty()) {
            item { EmptyCard(if (showArchived) "Chưa có két đã đóng" else "Chưa có két đang mở", null, null) }
        } else {
            items(display, key = { it.id }) { vault ->
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
            onDismissRequest = { deleteTarget = null; deleteError = null },
            title = { Text("Xoá két ${vault.name}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Chỉ xoá được két có số dư bằng 0.")
                    deleteError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    repository.deleteVault(vault.id)
                        .onSuccess { deleteTarget = null; deleteError = null }
                        .onFailure { deleteError = it.message }
                }) { Text("Xoá") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null; deleteError = null }) { Text("Huỷ") } }
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
                    Text(vault.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (vault.deadline.isNotBlank()) Text("Hạn ${vault.deadline}", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onArchive) {
                    Icon(if (vault.archived) Icons.Default.Restore else Icons.Default.Archive, null)
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null) }
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
private fun TransactionsScreen(repository: KcRepository, modifier: Modifier = Modifier) {
    var filter by rememberSaveable { mutableStateOf("ALL") }
    val items = repository.transactions.filter {
        when (filter) {
            "SAVE" -> it.type == TransactionType.SAVE
            "SPEND" -> it.type == TransactionType.SPEND
            else -> true
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { filter = "ALL" }, label = { Text("Tất cả") })
                AssistChip(onClick = { filter = "SAVE" }, label = { Text("Cất vào") })
                AssistChip(onClick = { filter = "SPEND" }, label = { Text("Chi ra") })
            }
        }
        if (items.isEmpty()) {
            item { EmptyCard("Chưa có giao dịch KC", null, null) }
        } else {
            items(items, key = { it.id }) { tx ->
                Card(shape = RoundedCornerShape(18.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            if (tx.type == TransactionType.SAVE) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            null,
                            tint = if (tx.type == TransactionType.SAVE) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                        )
                        Column(Modifier.weight(1f)) {
                            Text(repository.vaultName(tx.vaultId), fontWeight = FontWeight.Bold)
                            val detail = listOf(tx.category, tx.note).filter { it.isNotBlank() }.joinToString(" · ")
                            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatDate(tx.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            (if (tx.type == TransactionType.SAVE) "+" else "−") + formatKc(tx.amount),
                            fontWeight = FontWeight.Black,
                            color = if (tx.type == TransactionType.SAVE) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateVaultDialog(repository: KcRepository, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var initial by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo két chiến dịch") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Tên chiến dịch / sự kiện") }, singleLine = true)
                OutlinedTextField(
                    target,
                    { target = it.filter(Char::isDigit) },
                    label = { Text("Mục tiêu KC") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(deadline, { deadline = it }, label = { Text("Hạn (vd: 31/12/2026)") }, singleLine = true)
                OutlinedTextField(
                    initial,
                    { initial = it.filter(Char::isDigit) },
                    label = { Text("KC đang có (không bắt buộc)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                repository.createVault(
                    name = name,
                    target = target.toIntOrNull() ?: 0,
                    deadline = deadline,
                    initialAmount = initial.toIntOrNull() ?: 0
                ).onSuccess { onDismiss() }.onFailure { error = it.message }
            }) { Text("Tạo két") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } }
    )
}

@Composable
private fun TransactionDialog(
    repository: KcRepository,
    type: TransactionType,
    preselectedVaultId: Long?,
    onDismiss: () -> Unit
) {
    val availableVaults = repository.activeVaults
    var vaultId by remember { mutableStateOf(preselectedVaultId ?: availableVaults.firstOrNull()?.id) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(expenseCategories.first()) }
    var vaultMenu by remember { mutableStateOf(false) }
    var categoryMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val currentVault = availableVaults.firstOrNull { it.id == vaultId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (type == TransactionType.SAVE) "Cất KC vào két" else "Ghi chi tiêu KC") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                    OutlinedButton(onClick = { vaultMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(currentVault?.name ?: "Chọn két")
                    }
                    DropdownMenu(expanded = vaultMenu, onDismissRequest = { vaultMenu = false }) {
                        availableVaults.forEach { vault ->
                            DropdownMenuItem(
                                text = { Text(vault.name) },
                                onClick = { vaultId = vault.id; vaultMenu = false }
                            )
                        }
                    }
                }
                if (type == TransactionType.SPEND && currentVault != null) {
                    Text("Số dư: ${formatKc(repository.balance(currentVault.id))}", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    amount,
                    { amount = it.filter(Char::isDigit) },
                    label = { Text("Số KC") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (type == TransactionType.SPEND) {
                    Box {
                        OutlinedButton(onClick = { categoryMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(category) }
                        DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                            expenseCategories.forEach { item ->
                                DropdownMenuItem(text = { Text(item) }, onClick = { category = item; categoryMenu = false })
                            }
                        }
                    }
                }
                OutlinedTextField(note, { note = it }, label = { Text("Ghi chú") }, maxLines = 2)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                enabled = vaultId != null,
                onClick = {
                    val id = vaultId ?: return@Button
                    repository.addTransaction(
                        vaultId = id,
                        type = type,
                        amount = amount.toIntOrNull() ?: 0,
                        category = category,
                        note = note
                    ).onSuccess { onDismiss() }.onFailure { error = it.message }
                }
            ) { Text(if (type == TransactionType.SAVE) "Cất KC" else "Ghi chi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } }
    )
}

@Composable
private fun EmptyCard(title: String, action: String?, onAction: (() -> Unit)?) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (action != null && onAction != null) Button(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

private fun formatKc(value: Int): String = NumberFormat.getIntegerInstance(Locale("vi", "VN")).format(value) + " KC"

private fun formatDate(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
