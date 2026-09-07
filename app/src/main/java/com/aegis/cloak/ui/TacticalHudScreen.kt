package com.aegis.cloak.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.cloak.model.CloakStatus
import com.aegis.cloak.model.TargetCoordinates
import com.aegis.cloak.ui.components.AuditLogDialog
import com.aegis.cloak.ui.components.CellularSentryCard
import com.aegis.cloak.ui.components.CoordinatesControlPanel
import com.aegis.cloak.ui.components.DeveloperMockPermissionBanner
import com.aegis.cloak.ui.components.KinematicMetricsCard
import com.aegis.cloak.ui.components.ManualCoordinatesDialog
import com.aegis.cloak.ui.components.TacticalHeader
import com.aegis.cloak.ui.components.TacticalMasterEngageCard
import com.aegis.cloak.ui.components.VpnKillSwitchCard
import com.aegis.cloak.ui.theme.TacticalDarkBg

@Composable
fun TacticalHudScreen(
    viewModel: TacticalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()

    // Check Developer Options Mock Location status on load
    LaunchedEffect(Unit) {
        viewModel.checkDeveloperMockPermission(context)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TacticalDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // 1. Tactical Header Status Bar
            TacticalHeader(
                cloakStatus = uiState.cloakStatus,
                threatLevel = uiState.cellularTelemetry.threatLevel,
                onOpenLogs = { viewModel.showAuditLogDialog(true) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Developer Mock Options Warning Banner if needed
            if (!uiState.isDeveloperMockGranted) {
                DeveloperMockPermissionBanner(
                    onOpenDevSettings = { viewModel.openDeveloperSettings(context) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. Master Engage Cloak Switch Card
            TacticalMasterEngageCard(
                isCloaked = uiState.cloakEngaged,
                cloakStatus = uiState.cloakStatus,
                onToggle = { enable ->
                    viewModel.toggleMasterCloak(enable, context)
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Interactive Tactical Map View with Vector Radar Overlay
            TacticalMapView(
                targetCoordinates = uiState.targetCoordinates,
                telemetry = uiState.kinematicTelemetry,
                isCloakActive = uiState.cloakEngaged,
                onMapCoordinateSelected = { lat, lon ->
                    viewModel.setTargetCoordinates(
                        TargetCoordinates(
                            latitude = lat,
                            longitude = lon,
                            altitude = uiState.targetCoordinates.altitude,
                            label = "Custom Pin // Lat: ${String.format("%.4f", lat)}"
                        ),
                        context
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Target Coordinate Presets & Manual Input Trigger
            CoordinatesControlPanel(
                targetCoordinates = uiState.targetCoordinates,
                onPresetSelected = { preset ->
                    viewModel.setTargetCoordinates(preset, context)
                },
                onOpenEditDialog = { viewModel.showCoordinatesEditDialog(true) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Kinematic Motion Mirroring Telemetry HUD Card
            KinematicMetricsCard(
                telemetry = uiState.kinematicTelemetry,
                driftFactor = uiState.driftFactorMultiplier,
                onDriftFactorChanged = { factor ->
                    viewModel.setDriftFactor(factor)
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 7. Network Sanitizer & WireGuard Local VPN Bridge Card
            VpnKillSwitchCard(
                telemetry = uiState.vpnTelemetry,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 8. Cellular Radio Sentry & IMSI-Catcher Alert Card
            CellularSentryCard(
                telemetry = uiState.cellularTelemetry,
                onAirplaneModeClick = { viewModel.openAirplaneSettings(context) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Dialogs
        if (uiState.showCoordinatesEditDialog) {
            ManualCoordinatesDialog(
                initialTarget = uiState.targetCoordinates,
                onDismiss = { viewModel.showCoordinatesEditDialog(false) },
                onApply = { newTarget ->
                    viewModel.setTargetCoordinates(newTarget, context)
                }
            )
        }

        if (uiState.showAuditLogDialog) {
            AuditLogDialog(
                logs = auditLogs,
                onDismiss = { viewModel.showAuditLogDialog(false) },
                onClearLogs = { viewModel.clearAuditLogs() }
            )
        }
    }
}
