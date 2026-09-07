package com.aegis.cloak.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.cloak.model.CellularTelemetry
import com.aegis.cloak.model.CloakStatus
import com.aegis.cloak.model.KinematicTelemetry
import com.aegis.cloak.model.TargetCoordinates
import com.aegis.cloak.model.ThreatLevel
import com.aegis.cloak.model.VpnState
import com.aegis.cloak.model.VpnTelemetry
import com.aegis.cloak.ui.theme.TacticalAmber
import com.aegis.cloak.ui.theme.TacticalAmberGlow
import com.aegis.cloak.ui.theme.TacticalBorder
import com.aegis.cloak.ui.theme.TacticalCardBg
import com.aegis.cloak.ui.theme.TacticalCrimson
import com.aegis.cloak.ui.theme.TacticalCrimsonGlow
import com.aegis.cloak.ui.theme.TacticalCyan
import com.aegis.cloak.ui.theme.TacticalCyanGlow
import com.aegis.cloak.ui.theme.TacticalDarkBg
import com.aegis.cloak.ui.theme.TacticalGreen
import com.aegis.cloak.ui.theme.TacticalSurface
import com.aegis.cloak.ui.theme.TacticalTextMuted
import com.aegis.cloak.ui.theme.TacticalTextPrimary
import com.aegis.cloak.ui.theme.TacticalTextSecondary

@Composable
fun TacticalHeader(
    cloakStatus: CloakStatus,
    threatLevel: ThreatLevel,
    onOpenLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (cloakStatus) {
            CloakStatus.ACTIVE_CLOAKED -> TacticalCyan
            CloakStatus.ERROR_MOCK_PERMISSION_REQUIRED -> TacticalCrimson
            CloakStatus.INITIALIZING -> TacticalAmber
            else -> TacticalTextMuted
        },
        label = "StatusGlow"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PROJECT AEGIS // CLOAK",
                    color = TacticalTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                )
            }
            Text(
                text = when (cloakStatus) {
                    CloakStatus.ACTIVE_CLOAKED -> "SYSTEM ACTIVE // TELEMETRY MASKED"
                    CloakStatus.ERROR_MOCK_PERMISSION_REQUIRED -> "ERROR // MOCK PERMISSION REQUIRED"
                    CloakStatus.INITIALIZING -> "INITIALIZING KINEMATIC ENGINE..."
                    else -> "DEFENSE STANDBY // READY"
                },
                color = statusColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(threatLevel.colorHex).copy(alpha = 0.2f))
                    .border(1.dp, Color(threatLevel.colorHex), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = threatLevel.label,
                    color = Color(threatLevel.colorHex),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButtonCustom(
                onClick = onOpenLogs,
                icon = Icons.Default.Security,
                contentDescription = "Audit Logs",
                tint = TacticalCyan
            )
        }
    }
}

@Composable
fun TacticalMasterEngageCard(
    isCloaked: Boolean,
    cloakStatus: CloakStatus,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isCloaked) TacticalCyan else TacticalBorder
    val glowColor = if (isCloaked) TacticalCyanGlow else Color.Transparent

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = TacticalCardBg),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(glowColor)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isCloaked) TacticalCyan.copy(alpha = 0.2f) else TacticalSurface)
                        .border(1.dp, if (isCloaked) TacticalCyan else TacticalBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Cloak Shield",
                        tint = if (isCloaked) TacticalCyan else TacticalTextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (isCloaked) "CLOAK ENGAGED" else "CLOAK DISENGAGED",
                        color = if (isCloaked) TacticalCyan else TacticalTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isCloaked) "Kinematics & VPN Active" else "Physical Footprint Exposed",
                        color = if (isCloaked) TacticalGreen else TacticalAmber,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Switch(
                checked = isCloaked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TacticalCyan,
                    checkedTrackColor = TacticalCyan.copy(alpha = 0.4f),
                    uncheckedThumbColor = TacticalTextMuted,
                    uncheckedTrackColor = TacticalDarkBg
                ),
                modifier = Modifier.testTag("master_cloak_switch")
            )
        }
    }
}

@Composable
fun KinematicMetricsCard(
    telemetry: KinematicTelemetry,
    driftFactor: Float,
    onDriftFactorChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = TacticalCardBg),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = "Kinematic Mirror",
                        tint = TacticalCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "KINEMATIC MOTION MIRRORING",
                        color = TacticalCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = if (telemetry.isMoving) "WALKING // LIVE" else "STATIONARY",
                    color = if (telemetry.isMoving) TacticalGreen else TacticalTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Metric Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryMetricItem(
                    label = "WALK SPEED",
                    value = String.format("%.1f", telemetry.currentSpeedKmh),
                    unit = "km/h",
                    valueColor = TacticalAmber,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricItem(
                    label = "STEPS MIRRORED",
                    value = "${telemetry.stepCount}",
                    unit = "steps",
                    valueColor = TacticalGreen,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricItem(
                    label = "HEADING",
                    value = String.format("%.0f°", telemetry.headingDegrees),
                    unit = "azimuth",
                    valueColor = TacticalCyan,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryMetricItem(
                    label = "DISPLACEMENT",
                    value = String.format("%.1f", telemetry.totalDisplacementMeters),
                    unit = "meters",
                    valueColor = TacticalAmber,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricItem(
                    label = "GAUSSIAN DRIFT",
                    value = String.format("±%.1f", telemetry.gaussianDriftMeters),
                    unit = "jitter m",
                    valueColor = TacticalGreen,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricItem(
                    label = "ACCURACY",
                    value = String.format("±%.1f", telemetry.horizontalAccuracyMeters),
                    unit = "WGS84 m",
                    valueColor = TacticalCyan,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = TacticalBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Atmospheric Gaussian Drift Variance Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ATMOSPHERIC DRIFT INTENSITY",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = String.format("%.1fx", driftFactor),
                    color = TacticalAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Slider(
                value = driftFactor,
                onValueChange = onDriftFactorChanged,
                valueRange = 0.5f..3.0f,
                steps = 5,
                colors = SliderDefaults.colors(
                    thumbColor = TacticalAmber,
                    activeTrackColor = TacticalAmber,
                    inactiveTrackColor = TacticalSurface
                ),
                modifier = Modifier.testTag("drift_slider")
            )
        }
    }
}

@Composable
fun TelemetryMetricItem(
    label: String,
    value: String,
    unit: String,
    valueColor: Color = TacticalAmber,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(4.dp)) {
        Text(
            text = label,
            color = Color(0xFFCBD5E1),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = valueColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = unit,
                color = TacticalGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 1.dp)
            )
        }
    }
}

@Composable
fun VpnKillSwitchCard(
    telemetry: VpnTelemetry,
    modifier: Modifier = Modifier
) {
    val isSecured = telemetry.state == VpnState.SECURED_TUNNEL
    val isKillSwitch = telemetry.killSwitchEngaged

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = TacticalCardBg),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = "VPN Tunnel",
                        tint = TacticalCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NETWORK SANITIZER & WIREGUARD BRIDGE",
                        color = TacticalCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSecured) TacticalGreen.copy(alpha = 0.2f) else TacticalAmber.copy(alpha = 0.2f))
                        .border(1.dp, if (isSecured) TacticalGreen else TacticalAmber, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isSecured) "TUNNEL ACTIVE" else "STANDBY",
                        color = if (isSecured) TacticalGreen else TacticalAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryMetricItem(
                    label = "PACKETS SANITIZED",
                    value = "${telemetry.packetsSanitized}",
                    unit = "pkts",
                    valueColor = TacticalGreen,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricItem(
                    label = "DOH DNS ENCRYPTED",
                    value = "${telemetry.dnsQueriesSecured}",
                    unit = "queries",
                    valueColor = TacticalCyan,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricItem(
                    label = "WEBRTC LEAKS BLOCKED",
                    value = "${telemetry.webrtcLeaksBlocked}",
                    unit = "STUN drops",
                    valueColor = TacticalAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TacticalSurface, RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FAIL-CLOSED KILL-SWITCH",
                    color = TacticalTextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (isKillSwitch) "ARMED // NON-VPN DROPPED" else "OFF",
                    color = if (isKillSwitch) TacticalCyan else TacticalTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun CellularSentryCard(
    telemetry: CellularTelemetry,
    onAirplaneModeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isThreat = telemetry.threatLevel == ThreatLevel.CRITICAL_IMSI_CATCHER
    val cardBorder = if (isThreat) TacticalCrimson else TacticalBorder

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.2.dp, cardBorder, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = TacticalCardBg),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = "Cellular Sentry",
                        tint = if (isThreat) TacticalCrimson else TacticalAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CELLULAR RADIO SENTRY",
                        color = if (isThreat) TacticalCrimson else TacticalAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = telemetry.networkType,
                    color = if (isThreat) TacticalCrimson else TacticalCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryMetricItem(
                    label = "CELL TOWER ID",
                    value = "${telemetry.cellId}",
                    unit = "CID",
                    valueColor = TacticalCyan,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricItem(
                    label = "TAC / LAC",
                    value = "${telemetry.lacTac}",
                    unit = "AreaCode",
                    valueColor = TacticalAmber,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricItem(
                    label = "SIGNAL POWER",
                    value = "${telemetry.signalDbm}",
                    unit = "dBm",
                    valueColor = TacticalGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Threat Diagnostic Reason
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isThreat) TacticalCrimson.copy(alpha = 0.15f) else TacticalSurface, RoundedCornerShape(4.dp))
                    .border(1.dp, if (isThreat) TacticalCrimson else TacticalBorder, RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = telemetry.threatReason,
                    color = if (isThreat) TacticalCrimson else Color(0xFFE2E8F0),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (telemetry.requiresAirplaneMode || isThreat) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onAirplaneModeClick,
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalCrimson),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("airplane_mode_advisory_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AirplanemodeActive,
                        contentDescription = "Airplane Mode",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OPERATOR ACTION: TOGGLE AIRPLANE MODE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun CoordinatesControlPanel(
    targetCoordinates: TargetCoordinates,
    onPresetSelected: (TargetCoordinates) -> Unit,
    onOpenEditDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = TacticalCardBg),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = "Target Coordinates",
                        tint = TacticalCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TARGET COORDINATE PRESETS",
                        color = TacticalCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                OutlinedButton(
                    onClick = onOpenEditDialog,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalAmber),
                    border = BorderStroke(1.dp, TacticalAmber),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("edit_coordinates_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.EditLocation,
                        contentDescription = "Edit",
                        tint = TacticalAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "MANUAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TacticalAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontally scrollable presets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                TargetCoordinates.PRESETS.forEach { preset ->
                    val isSelected = targetCoordinates.label == preset.label
                    val chipBg = if (isSelected) TacticalCyan.copy(alpha = 0.25f) else Color(0xFF1E2630)
                    val chipBorder = if (isSelected) TacticalCyan else TacticalBorder

                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(chipBg)
                            .border(1.dp, chipBorder, RoundedCornerShape(4.dp))
                            .clickable { onPresetSelected(preset) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("preset_${preset.label.take(5).lowercase()}")
                    ) {
                        Text(
                            text = preset.label,
                            color = if (isSelected) TacticalCyan else Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ManualCoordinatesDialog(
    initialTarget: TargetCoordinates,
    onDismiss: () -> Unit,
    onApply: (TargetCoordinates) -> Unit
) {
    var latText by remember { mutableStateOf(initialTarget.latitude.toString()) }
    var lonText by remember { mutableStateOf(initialTarget.longitude.toString()) }
    var altText by remember { mutableStateOf(initialTarget.altitude.toString()) }
    var labelText by remember { mutableStateOf(initialTarget.label) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TacticalCardBg,
        title = {
            Text(
                text = "MANUAL COORDINATE INPUT",
                color = TacticalCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column {
                Text(
                    text = "Input target geographic reference coordinates (WGS84 decimal degrees):",
                    color = TacticalTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = labelText,
                    onValueChange = { labelText = it },
                    label = { Text("Sector / Label Name", color = TacticalTextSecondary, fontSize = 10.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TacticalTextPrimary,
                        unfocusedTextColor = TacticalTextPrimary,
                        focusedBorderColor = TacticalCyan,
                        unfocusedBorderColor = TacticalBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it },
                    label = { Text("Latitude (-90.0 to 90.0)", color = TacticalTextSecondary, fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TacticalTextPrimary,
                        unfocusedTextColor = TacticalTextPrimary,
                        focusedBorderColor = TacticalCyan,
                        unfocusedBorderColor = TacticalBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = lonText,
                    onValueChange = { lonText = it },
                    label = { Text("Longitude (-180.0 to 180.0)", color = TacticalTextSecondary, fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TacticalTextPrimary,
                        unfocusedTextColor = TacticalTextPrimary,
                        focusedBorderColor = TacticalCyan,
                        unfocusedBorderColor = TacticalBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = altText,
                    onValueChange = { altText = it },
                    label = { Text("Altitude (Meters MSL)", color = TacticalTextSecondary, fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TacticalTextPrimary,
                        unfocusedTextColor = TacticalTextPrimary,
                        focusedBorderColor = TacticalCyan,
                        unfocusedBorderColor = TacticalBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = TacticalCrimson,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latText.toDoubleOrNull()
                    val lon = lonText.toDoubleOrNull()
                    val alt = altText.toDoubleOrNull() ?: 0.0

                    if (lat == null || lat < -90.0 || lat > 90.0) {
                        errorMessage = "Invalid Latitude (-90.0 to 90.0)"
                        return@Button
                    }
                    if (lon == null || lon < -180.0 || lon > 180.0) {
                        errorMessage = "Invalid Longitude (-180.0 to 180.0)"
                        return@Button
                    }

                    onApply(
                        TargetCoordinates(
                            latitude = lat,
                            longitude = lon,
                            altitude = alt,
                            label = labelText.ifBlank { "Custom Target" }
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TacticalCyan),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "ENGAGE COORDINATES",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TacticalTextSecondary, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

@Composable
fun DeveloperMockPermissionBanner(
    onOpenDevSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, TacticalAmber, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = TacticalAmber.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = TacticalAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "MOCK LOCATION APP PERMISSION REQUIRED",
                    color = TacticalAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Android Developer Options > Select mock location app > Choose 'AegisCloak'.",
                color = TacticalTextPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onOpenDevSettings,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalAmber),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .testTag("open_developer_options_button")
            ) {
                Text(
                    text = "OPEN DEVELOPER OPTIONS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun IconButtonCustom(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    tint: Color
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(TacticalCardBg)
            .border(0.8.dp, TacticalBorder, RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}
