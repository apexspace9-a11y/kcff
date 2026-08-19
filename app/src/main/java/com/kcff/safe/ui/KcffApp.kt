package com.kcff.safe.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.kcff.safe.data.KcRepository
import com.kcff.safe.data.TransactionType

private enum class DialogMode { NONE, CREATE_VAULT, SAVE, SPEND }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KcffApp(repository: KcRepository) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var dialogMode by remember { mutableStateOf(DialogMode.NONE) }
    var selectedVaultId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Két Sắt KC FF", fontWeight = FontWeight.Bold)
                        Text(
                            text = when (selectedTab) {
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
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Tổng quan") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                    label = { Text("Két") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                    label = { Text("Chi tiêu") }
                )
            }
        },
        floatingActionButton = {
            val createMode = selectedTab == 1 || repository.activeVaults.isEmpty()
            ExtendedFloatingActionButton(
                onClick = {
                    selectedVaultId = null
                    dialogMode = if (createMode) DialogMode.CREATE_VAULT else DialogMode.SPEND
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (createMode) "Tạo két" else "Ghi chi tiêu") }
            )
        }
    ) { contentPadding ->
        when (selectedTab) {
            0 -> DashboardScreen(
                repository = repository,
                modifier = Modifier.padding(contentPadding),
                onCreateVault = { dialogMode = DialogMode.CREATE_VAULT },
                onSave = { vaultId ->
                    selectedVaultId = vaultId
                    dialogMode = DialogMode.SAVE
                },
                onSpend = { vaultId ->
                    selectedVaultId = vaultId
                    dialogMode = DialogMode.SPEND
                }
            )

            1 -> VaultsScreen(
                repository = repository,
                modifier = Modifier.padding(contentPadding),
                onSave = { vaultId ->
                    selectedVaultId = vaultId
                    dialogMode = DialogMode.SAVE
                },
                onSpend = { vaultId ->
                    selectedVaultId = vaultId
                    dialogMode = DialogMode.SPEND
                }
            )

            else -> TransactionsScreen(
                repository = repository,
                modifier = Modifier.padding(contentPadding)
            )
        }
    }

    when (dialogMode) {
        DialogMode.CREATE_VAULT -> CreateVaultDialog(repository, onDismiss = { dialogMode = DialogMode.NONE })
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
