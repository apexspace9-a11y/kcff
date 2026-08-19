package com.kcff.safe.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kcff.safe.R
import com.kcff.safe.data.KcRepository
import com.kcff.safe.data.KcTransaction
import com.kcff.safe.data.TransactionType
import com.kcff.safe.data.Vault
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

@Composable
internal fun DashboardScreen(
    repository: KcRepository,
    contentPadding: PaddingValues,
    onCreateVault: () -> Unit,
    onSave: (Long) -> Unit,
    onSpend: (Long) -> Unit
) {
    val activeVaults = repository.activeVaults
    val totalBalance = repository.totalBalance()
    val totalSaved = repository.totalSaved()
    val totalSpent = repository.totalSpent()
    val spentMonth = repository.spentThisMonth()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(KcNavy)
            .padding(contentPadding),
        contentPadding = PaddingValues(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item { KcffHeader(title = "KCFF", subtitle = "Két Sắt KC") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Xin chào, Pro Player!", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    "Cùng quản lý KC thật thông minh!",
                    style = MaterialTheme.typography.bodySmall,
                    color = KcTextMuted
                )
            }
        }
        item { TotalKcHud(totalBalance = totalBalance) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricHud("TỔNG THU", formatKc(totalSaved), KcGreen, Modifier.weight(1f))
                MetricHud("TỔNG CHI", formatKc(totalSpent), KcRed, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricHud("KÉT ĐANG MỞ", activeVaults.size.toString(), KcBlue, Modifier.weight(1f))
                MetricHud("CHI TIÊU THÁNG", formatKc(spentMonth), KcCyan, Modifier.weight(1f))
            }
        }
        item { HazardTitle("CHIẾN DỊCH NỔI BẬT") }

        if (activeVaults.isEmpty()) {
            item {
                EmptyHud(
                    title = "Chưa có chiến dịch KC",
                    subtitle = "Tạo két đầu tiên để gom KC theo sự kiện hoặc vật phẩm bạn muốn.",
                    action = "TẠO KÉT MỚI",
                    onAction = onCreateVault
                )
            }
        } else {
            items(activeVaults.take(3), key = { it.id }) { vault ->
                CampaignHudCard(
                    vault = vault,
                    balance = repository.balance(vault.id),
                    onSave = { onSave(vault.id) },
                    onSpend = { onSpend(vault.id) },
                    accent = if (activeVaults.indexOf(vault) % 2 == 0) KcGold else KcCyan
                )
            }
        }

        item {
            Button(
                onClick = onCreateVault,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = KcGold, contentColor = Color(0xFF251A00)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("TẠO KÉT MỚI", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
internal fun VaultsScreen(
    repository: KcRepository,
    contentPadding: PaddingValues,
    onCreateVault: () -> Unit,
    onSave: (Long) -> Unit,
    onSpend: (Long) -> Unit
) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var deleteTarget by remember { mutableStateOf<Vault?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    val visibleVaults = repository.vaults
        .filter {
            when (filter) {
                0 -> !it.archived
                1 -> it.archived
                else -> true
            }
        }
        .sortedByDescending { it.createdAt }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(KcNavy)
            .padding(contentPadding),
        contentPadding = PaddingValues(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader("Két của tôi", "Quản lý chiến dịch & mục tiêu KC") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SegmentButton("Đang mở", filter == 0, Modifier.weight(1f)) { filter = 0 }
                SegmentButton("Đã đóng", filter == 1, Modifier.weight(1f)) { filter = 1 }
                SegmentButton("Tất cả", filter == 2, Modifier.weight(1f)) { filter = 2 }
            }
        }
        item { VaultTotalHud(repository.totalBalance()) }

        if (visibleVaults.isEmpty()) {
            item {
                EmptyHud(
                    title = if (filter == 1) "Chưa có két đã đóng" else "Chưa có két phù hợp",
                    subtitle = "Các két chiến dịch sẽ xuất hiện ở đây.",
                    action = if (filter != 1) "TẠO KÉT" else null,
                    onAction = if (filter != 1) onCreateVault else null
                )
            }
        } else {
            items(visibleVaults, key = { it.id }) { vault ->
                ManagedVaultCard(
                    vault = vault,
                    balance = repository.balance(vault.id),
                    onSave = { onSave(vault.id) },
                    onSpend = { onSpend(vault.id) },
                    onArchive = { repository.toggleArchive(vault.id) },
                    onDelete = { deleteTarget = vault }
                )
            }
        }

        if (filter != 1) {
            item {
                Button(
                    onClick = onCreateVault,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = KcGold, contentColor = Color(0xFF251A00)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("TẠO KÉT MỚI", fontWeight = FontWeight.Black)
                }
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
                    deleteError?.let { Text(it, color = KcRed) }
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
                ) { Text("Xoá", color = KcRed) }
            },
            dismissButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    deleteError = null
                }) { Text("Huỷ") }
            }
        )
    }
}

@Composable
internal fun StatisticsScreen(repository: KcRepository, contentPadding: PaddingValues) {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    val spendTransactions = repository.transactions.filter { it.type == TransactionType.SPEND }
    val spendByCategory = spendTransactions
        .groupBy { it.category.ifBlank { "Khác" } }
        .mapValues { (_, txs) -> txs.sumOf { it.amount } }
        .entries
        .sortedByDescending { it.value }
        .take(6)
    val totalSpent = spendTransactions.sumOf { it.amount }
    val monthLabel = YearMonth.now().format(DateTimeFormatter.ofPattern("'Tháng' M/yyyy"))
    val lastSeven = remember(repository.transactions.size) { lastSevenDaySpend(repository.transactions) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(KcNavy)
            .padding(contentPadding),
        contentPadding = PaddingValues(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader("Thống kê", "Biểu đồ trực quan") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SegmentButton("Tổng quan", mode == 0, Modifier.weight(1f)) { mode = 0 }
                SegmentButton("Theo tháng", mode == 1, Modifier.weight(1f)) { mode = 1 }
                SegmentButton("Theo loại", mode == 2, Modifier.weight(1f)) { mode = 2 }
            }
        }
        item {
            HudPanel(accent = KcBlue) {
                Text(monthLabel, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        item {
            HudPanel(accent = KcCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Chi tiêu theo loại", color = Color.White, fontWeight = FontWeight.Black)
                    if (spendByCategory.isEmpty()) {
                        Text("Chưa có dữ liệu chi KC", color = KcTextMuted)
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DonutChart(
                                values = spendByCategory.map { it.value },
                                total = totalSpent,
                                modifier = Modifier.size(148.dp)
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                spendByCategory.forEachIndexed { index, entry ->
                                    LegendRow(
                                        label = entry.key,
                                        value = entry.value,
                                        color = chartColors[index % chartColors.size],
                                        total = totalSpent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            HudPanel(accent = KcBlue) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Lịch sử 7 ngày", color = Color.White, fontWeight = FontWeight.Black)
                    SevenDayBarChart(lastSeven)
                }
            }
        }

        item { HazardTitle("GIAO DỊCH GẦN ĐÂY") }
        if (repository.transactions.isEmpty()) {
            item { EmptyHud("Chưa có giao dịch KC", "Thu và chi KC sẽ được ghi lại tại đây.") }
        } else {
            items(repository.transactions.take(12), key = { it.id }) { tx ->
                TransactionHudRow(repository = repository, transaction = tx)
            }
        }
    }
}

@Composable
internal fun EventsScreen(
    repository: KcRepository,
    contentPadding: PaddingValues,
    onCreateVault: () -> Unit,
    onSave: (Long) -> Unit,
    onSpend: (Long) -> Unit
) {
    val active = repository.activeVaults

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(KcNavy)
            .padding(contentPadding),
        contentPadding = PaddingValues(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader("Sự kiện", "Chiến dịch Free Fire đang theo dõi") }
        item {
            HudPanel(accent = KcGold) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_kc_crystal),
                        contentDescription = null,
                        modifier = Modifier.size(width = 96.dp, height = 64.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text("${active.size} chiến dịch đang chạy", fontWeight = FontWeight.Black, color = Color.White)
                        Text(
                            "Tổng mục tiêu ${formatKc(repository.totalTarget())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = KcTextMuted
                        )
                    }
                }
            }
        }

        if (active.isEmpty()) {
            item {
                EmptyHud(
                    title = "Chưa theo dõi sự kiện nào",
                    subtitle = "Tạo két cho vòng quay, trang phục, thẻ hoặc sự kiện bạn đang chờ.",
                    action = "TẠO CHIẾN DỊCH",
                    onAction = onCreateVault
                )
            }
        } else {
            items(active, key = { it.id }) { vault ->
                CampaignHudCard(
                    vault = vault,
                    balance = repository.balance(vault.id),
                    onSave = { onSave(vault.id) },
                    onSpend = { onSpend(vault.id) },
                    accent = KcGold
                )
            }
        }
    }
}

@Composable
internal fun SettingsScreen(
    repository: KcRepository,
    contentPadding: PaddingValues,
    onEditBudget: () -> Unit
) {
    val budget = repository.monthlyBudget
    val spent = repository.spentThisMonth()
    val budgetProgress = if (budget <= 0) 0f else (spent.toFloat() / budget).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(KcNavy)
            .padding(contentPadding),
        contentPadding = PaddingValues(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader("Cài đặt", "Tùy chỉnh quản lý KC") }
        item {
            HudPanel(accent = KcGold) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Hạn mức chi tháng", color = Color.White, fontWeight = FontWeight.Black)
                            Text(
                                if (budget > 0) "Trần ${formatKc(budget)}" else "Chưa đặt hạn mức",
                                style = MaterialTheme.typography.bodySmall,
                                color = KcTextMuted
                            )
                        }
                        IconButton(onClick = onEditBudget) {
                            Icon(Icons.Default.Edit, contentDescription = "Sửa hạn mức", tint = KcGold)
                        }
                    }
                    if (budget > 0) {
                        LinearProgressIndicator(
                            progress = { budgetProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (spent > budget) KcRed else KcGold,
                            trackColor = Color.White.copy(alpha = 0.08f)
                        )
                        Text(
                            "Đã chi ${formatKc(spent)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (spent > budget) KcRed else KcTextMuted
                        )
                    }
                }
            }
        }
        item {
            HudPanel(accent = KcCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Dữ liệu KC", color = Color.White, fontWeight = FontWeight.Black)
                    SettingsStat("Két đang mở", repository.activeVaults.size.toString())
                    SettingsStat("Két đã đóng", repository.vaults.count { it.archived }.toString())
                    SettingsStat("Giao dịch", repository.transactions.size.toString())
                    SettingsStat("Tổng KC hiện có", formatKc(repository.totalBalance()))
                }
            }
        }
        item {
            HudPanel(accent = KcBlue) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Giao diện KCFF", color = Color.White, fontWeight = FontWeight.Black)
                    Text(
                        "Nền navy · neon cyan · điểm nhấn vàng",
                        style = MaterialTheme.typography.bodySmall,
                        color = KcTextMuted
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(KcNavy, KcBlue, KcCyan, KcGold, KcViolet).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(color)
                                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(9.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KcffHeader(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(KcBlue.copy(alpha = 0.7f), KcNavy)))
                .border(1.dp, KcCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_kc_crystal),
                contentDescription = null,
                modifier = Modifier.size(43.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, color = KcGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(
            onClick = {},
            modifier = Modifier
                .size(42.dp)
                .border(1.dp, KcBlue.copy(alpha = 0.65f), CircleShape)
        ) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
private fun ScreenHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            Image(
                painter = painterResource(R.drawable.ic_kc_crystal),
                contentDescription = null,
                modifier = Modifier.size(width = 74.dp, height = 46.dp)
            )
        }
        Text(subtitle, color = KcTextMuted, style = MaterialTheme.typography.bodySmall)
        HazardDivider()
    }
}

@Composable
private fun TotalKcHud(totalBalance: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF052D66), Color(0xFF063A86), Color(0xFF07182C))
                )
            )
            .border(2.dp, KcCyan, RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_kc_crystal),
                contentDescription = null,
                modifier = Modifier.size(width = 122.dp, height = 76.dp)
            )
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("TỔNG KC", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                    formatKc(totalBalance),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun VaultTotalHud(totalBalance: Int) {
    HudPanel(accent = KcCyan) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tổng KC:", color = KcGold, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Image(
                painter = painterResource(R.drawable.ic_kc_crystal),
                contentDescription = null,
                modifier = Modifier.size(width = 46.dp, height = 28.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(formatKc(totalBalance), color = KcCyan, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun MetricHud(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFF061426))
            .border(1.dp, accent.copy(alpha = 0.78f), RoundedCornerShape(13.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                Modifier
                    .width(34.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent)
            )
            Text(title, color = KcTextMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CampaignHudCard(
    vault: Vault,
    balance: Int,
    onSave: () -> Unit,
    onSpend: () -> Unit,
    accent: Color
) {
    val progress = (balance.toFloat() / vault.target).coerceIn(0f, 1f)
    val background = if (accent == KcGold) {
        Brush.horizontalGradient(listOf(Color(0xFF1C1420), Color(0xFF4A1B16), Color(0xFF081321)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF061933), Color(0xFF0A2A58), Color(0xFF07101D)))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .border(2.dp, accent.copy(alpha = 0.9f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(vault.name.uppercase(), color = Color.White, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Chiến dịch KCFF", color = KcTextMuted, style = MaterialTheme.typography.labelSmall)
                }
                Text("SỰ KIỆN", color = accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_kc_crystal),
                    contentDescription = null,
                    modifier = Modifier.size(width = 118.dp, height = 74.dp)
                )
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("KC hiện có", color = KcTextMuted, style = MaterialTheme.typography.labelSmall)
                    Text(formatKc(balance), color = KcCyan, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                    Text("Mục tiêu: ${formatKc(vault.target)}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = KcGold,
                trackColor = Color(0xFF0C2C5A)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (vault.deadline.isBlank()) "Không đặt hạn" else "Hạn: ${vault.deadline}", color = KcTextMuted, style = MaterialTheme.typography.labelSmall)
                Text("${(progress * 100).toInt()}%", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = KcBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("THU KC", fontWeight = FontWeight.Black)
                }
                Button(
                    onClick = onSpend,
                    modifier = Modifier.weight(1f),
                    enabled = balance > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = KcGold, contentColor = Color(0xFF251A00)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("CHI KC", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun ManagedVaultCard(
    vault: Vault,
    balance: Int,
    onSave: () -> Unit,
    onSpend: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = (balance.toFloat() / vault.target).coerceIn(0f, 1f)
    HudPanel(accent = if (vault.archived) KcTextMuted else KcCyan) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_kc_crystal),
                    contentDescription = null,
                    modifier = Modifier.size(width = 82.dp, height = 50.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(vault.name, color = Color.White, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (vault.archived) "Đã đóng" else if (vault.deadline.isBlank()) "Đang mở" else "Hạn ${vault.deadline}",
                        color = if (vault.archived) KcTextMuted else KcGold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                IconButton(onClick = onArchive) {
                    Icon(
                        if (vault.archived) Icons.Default.Restore else Icons.Default.Archive,
                        contentDescription = null,
                        tint = if (vault.archived) KcCyan else KcTextMuted
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = KcRed)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("KC hiện có", color = KcTextMuted, style = MaterialTheme.typography.labelSmall)
                    Text(formatKc(balance), color = KcCyan, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Mục tiêu", color = KcTextMuted, style = MaterialTheme.typography.labelSmall)
                    Text(formatKc(vault.target), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (vault.archived) KcTextMuted else KcGold,
                trackColor = Color.White.copy(alpha = 0.08f)
            )
            if (!vault.archived) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onSave, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Thu KC")
                    }
                    Button(
                        onClick = onSpend,
                        modifier = Modifier.weight(1f),
                        enabled = balance > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = KcGold, contentColor = Color(0xFF251A00)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Chi KC", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HudPanel(
    accent: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF081A2F), Color(0xFF06101D))
                )
            )
            .border(1.dp, accent.copy(alpha = 0.68f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        content()
    }
}

@Composable
private fun SegmentButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) KcBlue else Color(0xFF07162A),
            contentColor = if (selected) Color.White else KcTextMuted
        ),
        shape = RoundedCornerShape(9.dp),
        contentPadding = PaddingValues(horizontal = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.Black else FontWeight.Medium)
    }
}

@Composable
private fun HazardTitle(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text, color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
        HazardDivider()
    }
}

@Composable
private fun HazardDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(KcGold)
        )
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 4.dp)
                    .rotate(-35f)
                    .background(KcGold)
            )
        }
    }
}

@Composable
private fun EmptyHud(
    title: String,
    subtitle: String,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    HudPanel(accent = KcBlue) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_kc_crystal),
                contentDescription = null,
                modifier = Modifier.size(width = 110.dp, height = 68.dp)
            )
            Text(title, color = Color.White, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(subtitle, color = KcTextMuted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            if (action != null && onAction != null) {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = KcGold, contentColor = Color(0xFF251A00)),
                    shape = RoundedCornerShape(10.dp)
                ) { Text(action, fontWeight = FontWeight.Black) }
            }
        }
    }
}

private val chartColors = listOf(
    Color(0xFF10D9FF),
    Color(0xFF9C63FF),
    Color(0xFFFF4BA8),
    Color(0xFFFF9F2E),
    Color(0xFF4DE58D),
    Color(0xFF397DFF)
)

@Composable
private fun DonutChart(values: List<Int>, total: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 25.dp.toPx()
            val inset = stroke / 2f + 2.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            var start = -90f
            values.forEachIndexed { index, value ->
                val sweep = if (total <= 0) 0f else value.toFloat() / total * 360f
                drawArc(
                    color = chartColors[index % chartColors.size],
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Tổng chi", color = KcTextMuted, style = MaterialTheme.typography.labelSmall)
            Text(formatKc(total), color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun LegendRow(label: String, value: Int, color: Color, total: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(7.dp))
        Text(label, color = KcTextMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), maxLines = 1)
        val percent = if (total <= 0) 0 else (value * 100 / total)
        Text("$percent%", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SevenDayBarChart(data: List<Pair<LocalDate, Int>>) {
    val maxValue = max(data.maxOfOrNull { it.second } ?: 0, 1)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val left = 8.dp.toPx()
            val bottom = size.height - 4.dp.toPx()
            val availableWidth = size.width - left * 2
            val slot = availableWidth / data.size.coerceAtLeast(1)
            val barWidth = slot * 0.55f

            drawLine(
                color = Color.White.copy(alpha = 0.12f),
                start = Offset(left, bottom),
                end = Offset(size.width - left, bottom),
                strokeWidth = 1.dp.toPx()
            )

            data.forEachIndexed { index, entry ->
                val ratio = entry.second.toFloat() / maxValue
                val h = (size.height - 14.dp.toPx()) * ratio
                val x = left + index * slot + (slot - barWidth) / 2f
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(KcCyan, KcBlueDeep)),
                    topLeft = Offset(x, bottom - h),
                    size = Size(barWidth, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            data.forEach { (date, _) ->
                Text(
                    date.dayOfMonth.toString(),
                    color = KcTextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TransactionHudRow(repository: KcRepository, transaction: KcTransaction) {
    val isSave = transaction.type == TransactionType.SAVE
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF061426)),
        shape = RoundedCornerShape(13.dp),
        modifier = Modifier.border(
            1.dp,
            (if (isSave) KcCyan else KcRed).copy(alpha = 0.42f),
            RoundedCornerShape(13.dp)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background((if (isSave) KcBlue else KcRed).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isSave) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (isSave) KcCyan else KcRed
                )
            }
            Column(Modifier.weight(1f)) {
                Text(repository.vaultName(transaction.vaultId), color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                val detail = listOf(transaction.category, transaction.note).filter { it.isNotBlank() }.joinToString(" · ")
                if (detail.isNotBlank()) {
                    Text(detail, color = KcTextMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(formatDate(transaction.createdAt), color = KcTextMuted, style = MaterialTheme.typography.labelSmall)
            }
            Text(
                (if (isSave) "+" else "−") + formatKc(transaction.amount),
                color = if (isSave) KcGreen else KcRed,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun SettingsStat(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = KcTextMuted)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

private fun lastSevenDaySpend(transactions: List<KcTransaction>): List<Pair<LocalDate, Int>> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    return days.map { day ->
        val total = transactions.asSequence()
            .filter { it.type == TransactionType.SPEND }
            .filter {
                Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() == day
            }
            .sumOf { it.amount }
        day to total
    }
}
