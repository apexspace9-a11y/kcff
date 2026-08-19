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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kcff.safe.R
import com.kcff.safe.data.KcRepository
import com.kcff.safe.data.Vault
import kotlin.math.max

@Composable
internal fun EnhancedDashboardScreen(
    repository: KcRepository,
    contentPadding: PaddingValues,
    onCreateVault: () -> Unit,
    onSave: (Long) -> Unit,
    onSpend: (Long) -> Unit
) {
    val active = repository.activeVaults
    val totalBalance = repository.totalBalance()
    val totalTarget = repository.totalTarget()
    val totalSpent = repository.totalSpent()
    val monthSpent = repository.spentThisMonth()
    val monthSaved = repository.savedThisMonth()
    val overall = if (totalTarget <= 0) 0f else (totalBalance.toFloat() / totalTarget).coerceIn(0f, 1f)
    val closest = active
        .filter { repository.balance(it.id) < it.target }
        .maxByOrNull { repository.balance(it.id).toFloat() / it.target }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(KcNavy)
            .padding(contentPadding),
        contentPadding = PaddingValues(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeroArtCard(
                totalBalance = totalBalance,
                overall = overall,
                openVaults = active.size,
                totalTarget = totalTarget
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArtMetric(
                    title = "KC CẤT THÁNG NÀY",
                    value = formatKc(monthSaved),
                    accent = KcGreen,
                    icon = { Icon(Icons.Default.ArrowDownward, null) },
                    modifier = Modifier.weight(1f)
                )
                ArtMetric(
                    title = "KC CHI THÁNG NÀY",
                    value = formatKc(monthSpent),
                    accent = KcRed,
                    icon = { Icon(Icons.Default.ArrowUpward, null) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArtMetric(
                    title = "KÉT ĐANG MỞ",
                    value = active.size.toString(),
                    accent = KcCyan,
                    icon = { Icon(Icons.Default.Savings, null) },
                    modifier = Modifier.weight(1f)
                )
                ArtMetric(
                    title = "TỔNG ĐÃ CHI",
                    value = formatKc(totalSpent),
                    accent = KcGold,
                    icon = { Icon(Icons.Default.Diamond, null) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        closest?.let { vault ->
            item {
                val balance = repository.balance(vault.id)
                val progress = (balance.toFloat() / vault.target).coerceIn(0f, 1f)
                HighlightGoalCard(vault, balance, progress)
            }
        }

        item {
            SectionStrip("CHIẾN DỊCH NỔI BẬT", "Gom KC có mục tiêu, khỏi tiêu như nước")
        }

        if (active.isEmpty()) {
            item {
                EmptyArtCard(onCreateVault)
            }
        } else {
            items(active.take(4), key = { it.id }) { vault ->
                CampaignImageCard(
                    vault = vault,
                    balance = repository.balance(vault.id),
                    onSave = { onSave(vault.id) },
                    onSpend = { onSpend(vault.id) }
                )
            }
        }

        item {
            Button(
                onClick = onCreateVault,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KcGold,
                    contentColor = Color(0xFF171100)
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("TẠO KÉT MỚI", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun HeroArtCard(
    totalBalance: Int,
    overall: Float,
    openVaults: Int,
    totalTarget: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, KcCyan.copy(alpha = 0.75f), RoundedCornerShape(22.dp))
    ) {
        Image(
            painter = painterResource(R.drawable.kcff_hero),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xE9030814),
                            Color(0xA8081428),
                            Color.Transparent
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text("KCFF", color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            Text("KÉT SẮT KC", color = KcGold, fontWeight = FontWeight.Black)
            Text("TỔNG KC", color = KcTextMuted, style = MaterialTheme.typography.labelMedium)
            Text(formatKc(totalBalance), color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            LinearProgressIndicator(
                progress = { overall },
                modifier = Modifier.width(190.dp),
                color = KcGold,
                trackColor = Color.White.copy(alpha = 0.12f)
            )
            Text(
                "$openVaults két đang mở · Mục tiêu ${formatKc(totalTarget)}",
                color = Color(0xFFD8E8FF),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ArtMetric(
    title: String,
    value: String,
    accent: Color,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF071525)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.LocalContentColor provides accent
                icon()
            }
            Text(title, color = KcTextMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun HighlightGoalCard(vault: Vault, balance: Int, progress: Float) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, KcGold.copy(alpha = 0.72f), RoundedCornerShape(17.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111B2A)),
        shape = RoundedCornerShape(17.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(KcGold.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = KcGold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("SẮP ĐẠT MỤC TIÊU", color = KcGold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                Text(vault.name, color = Color.White, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = KcGold,
                    trackColor = Color.White.copy(alpha = 0.10f)
                )
                Text("Còn ${formatKc(max(vault.target - balance, 0))}", color = KcTextMuted, style = MaterialTheme.typography.bodySmall)
            }
            Text("${(progress * 100).toInt()}%", color = KcCyan, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SectionStrip(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(5.dp).height(22.dp).background(KcGold))
            Spacer(Modifier.width(8.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Black)
        }
        Text(subtitle, color = KcTextMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CampaignImageCard(
    vault: Vault,
    balance: Int,
    onSave: () -> Unit,
    onSpend: () -> Unit
) {
    val progress = (balance.toFloat() / vault.target).coerceIn(0f, 1f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, KcBlue.copy(alpha = 0.62f), RoundedCornerShape(19.dp)),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF071322))
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(104.dp)) {
                Image(
                    painter = painterResource(R.drawable.kcff_event),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6071322))))
                )
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(13.dp)
                ) {
                    Text(vault.name, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    if (vault.deadline.isNotBlank()) {
                        Text("Hạn ${vault.deadline}", color = Color(0xFFD7E6FA), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${formatKc(balance)} / ${formatKc(vault.target)}", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("${(progress * 100).toInt()}%", color = KcGold, fontWeight = FontWeight.Black)
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = KcGold,
                    trackColor = Color.White.copy(alpha = 0.09f)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedButton(onClick = onSave, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("THU KC")
                    }
                    Button(
                        onClick = onSpend,
                        modifier = Modifier.weight(1f),
                        enabled = balance > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = KcGold, contentColor = Color(0xFF171100))
                    ) {
                        Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("CHI KC", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyArtCard(onCreateVault: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, KcBlue.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF081422)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Savings, null, tint = KcCyan, modifier = Modifier.size(40.dp))
            Text("Chưa có két chiến dịch", color = Color.White, fontWeight = FontWeight.Black)
            Text("Tạo một két để bắt đầu gom KC có mục tiêu.", color = KcTextMuted, style = MaterialTheme.typography.bodySmall)
            Button(onClick = onCreateVault, colors = ButtonDefaults.buttonColors(containerColor = KcGold, contentColor = Color(0xFF171100))) {
                Text("TẠO KÉT", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
internal fun EnhancedEventsScreen(
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
        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("SỰ KIỆN", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("Theo dõi các chiến dịch KC đang chạy", color = KcTextMuted, style = MaterialTheme.typography.bodySmall)
        }

        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, KcGold.copy(alpha = 0.72f), RoundedCornerShape(20.dp))
            ) {
                Image(
                    painter = painterResource(R.drawable.kcff_event),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArtMetric(
                    title = "ĐANG CHẠY",
                    value = active.size.toString(),
                    accent = KcCyan,
                    icon = { Icon(Icons.Default.LocalFireDepartment, null) },
                    modifier = Modifier.weight(1f)
                )
                ArtMetric(
                    title = "TỔNG MỤC TIÊU",
                    value = formatKc(repository.totalTarget()),
                    accent = KcGold,
                    icon = { Icon(Icons.Default.Diamond, null) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item { SectionStrip("KÉT SỰ KIỆN", "Tiến độ được cập nhật từ giao dịch của bạn") }

        if (active.isEmpty()) {
            item { EmptyArtCard(onCreateVault) }
        } else {
            items(active, key = { it.id }) { vault ->
                CampaignImageCard(
                    vault = vault,
                    balance = repository.balance(vault.id),
                    onSave = { onSave(vault.id) },
                    onSpend = { onSpend(vault.id) }
                )
            }
        }

        item {
            Button(
                onClick = onCreateVault,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = KcGold, contentColor = Color(0xFF171100))
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(7.dp))
                Text("TẠO CHIẾN DỊCH MỚI", fontWeight = FontWeight.Black)
            }
        }
    }
}
