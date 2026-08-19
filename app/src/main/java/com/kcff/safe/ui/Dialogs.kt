package com.kcff.safe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kcff.safe.data.KcRepository
import com.kcff.safe.data.TransactionType
import com.kcff.safe.data.expenseCategories

@Composable
internal fun CreateVaultDialog(repository: KcRepository, onDismiss: () -> Unit) {
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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tên chiến dịch / sự kiện") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { input -> target = input.filter { it.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mục tiêu KC") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Hạn (vd: 31/12/2026)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = initial,
                    onValueChange = { input -> initial = input.filter { it.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("KC đang có") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    repository.createVault(
                        name = name,
                        target = target.toIntOrNull() ?: 0,
                        deadline = deadline,
                        initialAmount = initial.toIntOrNull() ?: 0
                    ).onSuccess { onDismiss() }
                        .onFailure { error = it.message }
                }
            ) { Text("Tạo két") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Huỷ") }
        }
    )
}

@Composable
internal fun TransactionDialog(
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
    var vaultMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val selectedVault = availableVaults.firstOrNull { it.id == vaultId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (type == TransactionType.SAVE) "Cất KC vào két" else "Ghi chi tiêu KC") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { vaultMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedVault?.name ?: "Chọn két")
                    }
                    DropdownMenu(
                        expanded = vaultMenuExpanded,
                        onDismissRequest = { vaultMenuExpanded = false }
                    ) {
                        availableVaults.forEach { vault ->
                            DropdownMenuItem(
                                text = { Text(vault.name) },
                                onClick = {
                                    vaultId = vault.id
                                    vaultMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (type == TransactionType.SPEND && selectedVault != null) {
                    Text(
                        "Số dư: ${formatKc(repository.balance(selectedVault.id))}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { input -> amount = input.filter { it.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Số KC") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                if (type == TransactionType.SPEND) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { categoryMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(category) }
                        DropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false }
                        ) {
                            expenseCategories.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    onClick = {
                                        category = item
                                        categoryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ghi chú") },
                    maxLines = 2
                )

                if (availableVaults.isEmpty()) {
                    Text("Chưa có két đang mở", color = MaterialTheme.colorScheme.error)
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = vaultId != null && availableVaults.isNotEmpty(),
                onClick = {
                    val selectedId = vaultId ?: return@Button
                    repository.addTransaction(
                        vaultId = selectedId,
                        type = type,
                        amount = amount.toIntOrNull() ?: 0,
                        category = category,
                        note = note
                    ).onSuccess { onDismiss() }
                        .onFailure { error = it.message }
                }
            ) {
                Text(if (type == TransactionType.SAVE) "Cất KC" else "Ghi chi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Huỷ") }
        }
    )
}
