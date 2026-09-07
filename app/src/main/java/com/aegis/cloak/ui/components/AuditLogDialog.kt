package com.aegis.cloak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.cloak.data.entity.AuditLogEntity
import com.aegis.cloak.ui.theme.TacticalAmber
import com.aegis.cloak.ui.theme.TacticalBorder
import com.aegis.cloak.ui.theme.TacticalCardBg
import com.aegis.cloak.ui.theme.TacticalCrimson
import com.aegis.cloak.ui.theme.TacticalCyan
import com.aegis.cloak.ui.theme.TacticalDarkBg
import com.aegis.cloak.ui.theme.TacticalGreen
import com.aegis.cloak.ui.theme.TacticalSurface
import com.aegis.cloak.ui.theme.TacticalTextMuted
import com.aegis.cloak.ui.theme.TacticalTextPrimary
import com.aegis.cloak.ui.theme.TacticalTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AuditLogDialog(
    logs: List<AuditLogEntity>,
    onDismiss: () -> Unit,
    onClearLogs: () -> Unit
) {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TacticalCardBg,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Audit Trail",
                        tint = TacticalCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SECURE AUDIT TRAIL",
                        color = TacticalCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                OutlinedButton(
                    onClick = onClearLogs,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalCrimson),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear",
                        tint = TacticalCrimson,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PURGE", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                Text(
                    text = "Encrypted local audit log stored in Room DB. Zero remote sync.",
                    color = TacticalTextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(TacticalSurface, RoundedCornerShape(4.dp))
                            .border(0.8.dp, TacticalBorder, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO AUDIT LOGS RECORDED",
                            color = TacticalTextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(TacticalDarkBg, RoundedCornerShape(4.dp))
                            .border(0.8.dp, TacticalBorder, RoundedCornerShape(4.dp))
                            .padding(6.dp)
                    ) {
                        items(logs, key = { it.id }) { log ->
                            AuditLogItem(log = log, timeStr = dateFormat.format(Date(log.timestamp)))
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = TacticalCyan),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("CLOSE", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

@Composable
fun AuditLogItem(log: AuditLogEntity, timeStr: String) {
    val severityColor = when (log.severity) {
        "CRITICAL" -> TacticalCrimson
        "WARN" -> TacticalAmber
        else -> TacticalGreen
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(TacticalSurface)
            .border(0.5.dp, severityColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "[${log.category}]",
                    color = TacticalCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = timeStr,
                    color = TacticalTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = log.message,
                color = TacticalTextPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            if (log.details.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = log.details,
                    color = TacticalTextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
