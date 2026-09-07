package com.aegis.cloak.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aegis.cloak.data.TacticalRepository
import com.aegis.cloak.data.entity.AuditLogEntity
import com.aegis.cloak.model.CloakStatus
import com.aegis.cloak.model.TacticalUiState
import com.aegis.cloak.model.TargetCoordinates
import com.aegis.cloak.model.ThreatLevel
import com.aegis.cloak.service.CloakVpnService
import com.aegis.cloak.service.MockLocationService
import com.aegis.cloak.service.RadioSentryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TacticalViewModel(
    private val repository: TacticalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TacticalUiState())
    val uiState: StateFlow<TacticalUiState> = _uiState.asStateFlow()

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Collect real-time telemetry from all modules
        viewModelScope.launch {
            MockLocationService.telemetryState.collect { telemetry ->
                _uiState.value = _uiState.value.copy(kinematicTelemetry = telemetry)
            }
        }

        viewModelScope.launch {
            MockLocationService.serviceStatus.collect { status ->
                val isCloaked = (status == CloakStatus.ACTIVE_CLOAKED)
                _uiState.value = _uiState.value.copy(
                    cloakStatus = status,
                    cloakEngaged = isCloaked
                )
            }
        }

        viewModelScope.launch {
            CloakVpnService.vpnTelemetry.collect { vpnTelemetry ->
                _uiState.value = _uiState.value.copy(vpnTelemetry = vpnTelemetry)
            }
        }

        viewModelScope.launch {
            RadioSentryManager.cellularState.collect { cellular ->
                val currentThreat = cellular.threatLevel
                _uiState.value = _uiState.value.copy(
                    cellularTelemetry = cellular,
                    showAirplaneModeAlert = cellular.requiresAirplaneMode
                )
                if (currentThreat == ThreatLevel.CRITICAL_IMSI_CATCHER) {
                    repository.logEvent(
                        category = "RADIO_SENTRY",
                        severity = "CRITICAL",
                        message = "IMSI-Catcher / Forced Downgrade Detected",
                        details = cellular.threatReason
                    )
                }
            }
        }
    }

    fun checkDeveloperMockPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_MOCK_LOCATION,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_MOCK_LOCATION,
                    Process.myUid(),
                    context.packageName
                )
            }
            val isAllowed = (mode == AppOpsManager.MODE_ALLOWED)
            _uiState.value = _uiState.value.copy(isDeveloperMockGranted = isAllowed)
            isAllowed
        } catch (_: Exception) {
            false
        }
    }

    fun toggleMasterCloak(enable: Boolean, context: Context) {
        viewModelScope.launch {
            if (enable) {
                val isMockAllowed = checkDeveloperMockPermission(context)
                if (!isMockAllowed) {
                    _uiState.value = _uiState.value.copy(
                        cloakStatus = CloakStatus.ERROR_MOCK_PERMISSION_REQUIRED,
                        cloakEngaged = false
                    )
                    repository.logEvent(
                        category = "CLOAK_ENGINE",
                        severity = "WARN",
                        message = "Mock location permission not set in Developer Options"
                    )
                    return@launch
                }

                // 1. Start Kinematic Mock Service
                val target = _uiState.value.targetCoordinates
                val mockIntent = Intent(context, MockLocationService::class.java).apply {
                    action = MockLocationService.ACTION_START
                    putExtra(MockLocationService.EXTRA_LATITUDE, target.latitude)
                    putExtra(MockLocationService.EXTRA_LONGITUDE, target.longitude)
                    putExtra(MockLocationService.EXTRA_ALTITUDE, target.altitude)
                    putExtra(MockLocationService.EXTRA_DRIFT_FACTOR, _uiState.value.driftFactorMultiplier)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(mockIntent)
                } else {
                    context.startService(mockIntent)
                }

                // 2. Start WireGuard Local VPN Sanitizer (if prepared)
                val vpnPrepared = VpnService.prepare(context) == null
                if (vpnPrepared) {
                    val vpnIntent = Intent(context, CloakVpnService::class.java).apply {
                        action = CloakVpnService.ACTION_CONNECT
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(vpnIntent)
                    } else {
                        context.startService(vpnIntent)
                    }
                }

                // 3. Start Radio Sentry
                val sentryManager = RadioSentryManager(context)
                sentryManager.startMonitoring()

                _uiState.value = _uiState.value.copy(
                    cloakEngaged = true,
                    cloakStatus = CloakStatus.ACTIVE_CLOAKED
                )
                repository.logEvent(
                    category = "CLOAK_ENGINE",
                    severity = "INFO",
                    message = "Master Cloak Engaged",
                    details = "Target: ${target.label} (${target.latitude}, ${target.longitude})"
                )
            } else {
                // Disengage All
                val stopMockIntent = Intent(context, MockLocationService::class.java).apply {
                    action = MockLocationService.ACTION_STOP
                }
                context.startService(stopMockIntent)

                val stopVpnIntent = Intent(context, CloakVpnService::class.java).apply {
                    action = CloakVpnService.ACTION_DISCONNECT
                }
                context.startService(stopVpnIntent)

                val sentryManager = RadioSentryManager(context)
                sentryManager.stopMonitoring()

                _uiState.value = _uiState.value.copy(
                    cloakEngaged = false,
                    cloakStatus = CloakStatus.DISENGAGED
                )
                repository.logEvent(
                    category = "CLOAK_ENGINE",
                    severity = "INFO",
                    message = "Master Cloak Disengaged"
                )
            }
        }
    }

    fun setTargetCoordinates(target: TargetCoordinates, context: Context) {
        _uiState.value = _uiState.value.copy(
            targetCoordinates = target,
            showCoordinatesEditDialog = false,
            showPresetsDialog = false
        )

        // If currently cloaked, update the live running mock service coordinates immediately
        if (_uiState.value.cloakEngaged) {
            val updateIntent = Intent(context, MockLocationService::class.java).apply {
                action = MockLocationService.ACTION_UPDATE_COORDINATES
                putExtra(MockLocationService.EXTRA_LATITUDE, target.latitude)
                putExtra(MockLocationService.EXTRA_LONGITUDE, target.longitude)
                putExtra(MockLocationService.EXTRA_ALTITUDE, target.altitude)
            }
            context.startService(updateIntent)
        }

        viewModelScope.launch {
            repository.logEvent(
                category = "KINEMATIC",
                severity = "INFO",
                message = "Target Coordinates Updated",
                details = "${target.label}: ${target.latitude}, ${target.longitude}"
            )
        }
    }

    fun setDriftFactor(factor: Float) {
        _uiState.value = _uiState.value.copy(driftFactorMultiplier = factor)
    }

    fun openDeveloperSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun openAirplaneSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun showCoordinatesEditDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showCoordinatesEditDialog = show)
    }

    fun showAuditLogDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAuditLogDialog = show)
    }

    fun clearAuditLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }
}

class TacticalViewModelFactory(
    private val repository: TacticalRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TacticalViewModel::class.java)) {
            return TacticalViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
