package com.kcff.safe.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kcff.safe.data.KcRepository
import com.kcff.safe.data.TransactionType

private enum class DialogMode { NONE, CREATE_VAULT, SAVE, SPEND, BUDGET }

@Composable
fun KcffApp(repository: KcRepository) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var dialogMode by remember { mutableStateOf(DialogMode.NONE) }
    var selectedVaultId by remember { mutableStateOf<Long?>(null) }

    fun openTransaction(type: TransactionType, vaultId: Long?) {
        selectedVaultId = vaultId
        dialogMode = if (type == TransactionType.SAVE) DialogMode.SAVE else DialogMode.SPEND
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = KcNavy,
        bottomBar = {
            KcffBottomBar(
                selected = selectedTab,
                onSelected = { selectedTab = it }
            )
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(width = 0.dp, color = Color.Transparent)
        ) {
            when (selectedTab) {
                0 -> EnhancedDashboardScreen(
                    repository = repository,
                    contentPadding = contentPadding,
                    onCreateVault = { dialogMode = DialogMode.CREATE_VAULT },
                    onSave = { openTransaction(TransactionType.SAVE, it) },
                    onSpend = { openTransaction(TransactionType.SPEND, it) }
                )

                1 -> VaultsScreen(
                    repository = repository,
                    contentPadding = contentPadding,
                    onCreateVault = { dialogMode = DialogMode.CREATE_VAULT },
                    onSave = { openTransaction(TransactionType.SAVE, it) },
                    onSpend = { openTransaction(TransactionType.SPEND, it) }
                )

                2 -> StatisticsScreen(
                    repository = repository,
                    contentPadding = contentPadding
                )

                3 -> EnhancedEventsScreen(
                    repository = repository,
                    contentPadding = contentPadding,
                    onCreateVault = { dialogMode = DialogMode.CREATE_VAULT },
                    onSave = { openTransaction(TransactionType.SAVE, it) },
                    onSpend = { openTransaction(TransactionType.SPEND, it) }
                )

                else -> SettingsScreen(
                    repository = repository,
                    contentPadding = contentPadding,
                    onEditBudget = { dialogMode = DialogMode.BUDGET }
                )
            }
        }
    }

    when (dialogMode) {
        DialogMode.CREATE_VAULT -> CreateVaultDialog(
            repository = repository,
            onDismiss = { dialogMode = DialogMode.NONE }
        )

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

        DialogMode.BUDGET -> BudgetDialog(
            repository = repository,
            onDismiss = { dialogMode = DialogMode.NONE }
        )

        DialogMode.NONE -> Unit
    }
}

@Composable
private fun KcffBottomBar(selected: Int, onSelected: (Int) -> Unit) {
    val items = listOf(
        Triple("Trang chủ", Icons.Default.Home, 0),
        Triple("Két", Icons.Default.AccountBalanceWallet, 1),
        Triple("Thống kê", Icons.Default.BarChart, 2),
        Triple("Sự kiện", Icons.Default.Event, 3),
        Triple("Cài đặt", Icons.Default.Settings, 4)
    )

    NavigationBar(
        modifier = Modifier
            .navigationBarsPadding()
            .border(width = 1.dp, color = KcBlue.copy(alpha = 0.45f)),
        containerColor = Color(0xFF050D19),
        tonalElevation = 0.dp
    ) {
        items.forEach { (label, icon, index) ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { onSelected(index) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = KcGold,
                    selectedTextColor = KcGold,
                    indicatorColor = KcSurfaceBlue.copy(alpha = 0.55f),
                    unselectedIconColor = KcTextMuted,
                    unselectedTextColor = KcTextMuted
                )
            )
        }
    }
}
