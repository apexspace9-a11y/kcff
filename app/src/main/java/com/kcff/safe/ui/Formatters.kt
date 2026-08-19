package com.kcff.safe.ui

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun formatKc(value: Int): String =
    NumberFormat.getIntegerInstance(Locale("vi", "VN")).format(value) + " KC"

internal fun formatDate(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
