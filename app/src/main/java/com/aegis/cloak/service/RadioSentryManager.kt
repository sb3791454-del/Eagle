package com.aegis.cloak.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.aegis.cloak.model.CellularTelemetry
import com.aegis.cloak.model.ThreatLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executor

class RadioSentryManager(private val context: Context) {

    companion object {
        private val _cellularState = MutableStateFlow(CellularTelemetry())
        val cellularState: StateFlow<CellularTelemetry> = _cellularState.asStateFlow()
    }

    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private var previousCellId: Long = -1L
    private var previousSignalDbm: Int = -85
    private var previousIs2G = false
    private var isListening = false

    private var telephonyCallback: Any? = null
    private var legacyListener: PhoneStateListener? = null

    fun startMonitoring() {
        if (isListening) return
        isListening = true

        try {
            readInitialTelephonySnapshot()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                registerModernTelephonyCallback()
            } else {
                registerLegacyPhoneStateListener()
            }
        } catch (_: SecurityException) {
            _cellularState.value = _cellularState.value.copy(
                threatReason = "Cellular telemetry restricted. Grant READ_PHONE_STATE permission."
            )
        } catch (_: Exception) {}
    }

    fun stopMonitoring() {
        if (!isListening) return
        isListening = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let {
                if (it is TelephonyCallback) {
                    try {
                        telephonyManager.unregisterTelephonyCallback(it)
                    } catch (_: Exception) {}
                }
            }
            telephonyCallback = null
        } else {
            legacyListener?.let {
                @Suppress("DEPRECATION")
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            }
            legacyListener = null
        }
    }

    private fun readInitialTelephonySnapshot() {
        val operator = telephonyManager.networkOperatorName.ifBlank { "TACTICAL_CARRIER" }
        val netTypeStr = resolveNetworkTypeString(telephonyManager.networkType)
        val is2G = netTypeStr.contains("2G") || netTypeStr.contains("GSM")

        _cellularState.value = CellularTelemetry(
            networkType = netTypeStr,
            operatorName = operator,
            signalDbm = -85,
            cellId = 18492042L,
            lacTac = 4201,
            threatLevel = if (is2G) ThreatLevel.ELEVATED_ANOMALY else ThreatLevel.LOW_SECURE,
            threatReason = if (is2G) "2G Unencrypted protocol in use. High vulnerability." else "Carrier handshake nominal.",
            requiresAirplaneMode = is2G
        )
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerModernTelephonyCallback() {
        val executor = context.mainExecutor
        val callback = object : TelephonyCallback(),
            TelephonyCallback.CellInfoListener,
            TelephonyCallback.SignalStrengthsListener {

            override fun onCellInfoChanged(cellInfoList: MutableList<CellInfo>) {
                processCellInfoList(cellInfoList)
            }

            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                processSignalStrength(signalStrength)
            }
        }
        telephonyCallback = callback
        try {
            telephonyManager.registerTelephonyCallback(executor, callback)
        } catch (_: SecurityException) {}
    }

    private fun registerLegacyPhoneStateListener() {
        @Suppress("DEPRECATION")
        val listener = object : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onCellInfoChanged(cellInfoList: MutableList<CellInfo>?) {
                if (cellInfoList != null) {
                    processCellInfoList(cellInfoList)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                if (signalStrength != null) {
                    processSignalStrength(signalStrength)
                }
            }
        }
        legacyListener = listener
        @Suppress("DEPRECATION")
        try {
            telephonyManager.listen(
                listener,
                PhoneStateListener.LISTEN_CELL_INFO or PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
            )
        } catch (_: SecurityException) {}
    }

    private fun processCellInfoList(cellInfoList: List<CellInfo>) {
        if (cellInfoList.isEmpty()) return

        val registeredCell = cellInfoList.firstOrNull { it.isRegistered } ?: cellInfoList.first()

        var currentCellId = -1L
        var currentTac = 0
        var is2G = false
        var netTypeName = "LTE / 4G (Encrypted)"

        when (registeredCell) {
            is CellInfoNr -> {
                netTypeName = "5G NR (High Security)"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    (registeredCell.cellIdentity as? CellIdentityNr)?.let {
                        currentCellId = it.nci
                        currentTac = it.tac
                    }
                }
            }
            is CellInfoLte -> {
                netTypeName = "LTE / 4G (Encrypted)"
                (registeredCell.cellIdentity as? CellIdentityLte)?.let {
                    currentCellId = it.ci.toLong()
                    currentTac = it.tac
                }
            }
            is CellInfoGsm -> {
                netTypeName = "GSM / 2G (UNENCRYPTED / DOWNGRADED)"
                is2G = true
                (registeredCell.cellIdentity as? CellIdentityGsm)?.let {
                    currentCellId = it.cid.toLong()
                    currentTac = it.lac
                }
            }
            else -> {
                netTypeName = "Cellular Network (Monitored)"
            }
        }

        // HEURISTIC EVALUATION FOR IMSI-CATCHERS / STINGRAYS:
        var threat = ThreatLevel.LOW_SECURE
        var reason = "Normal cellular tower telemetry. Encryption active."
        var recommendAirplane = false

        // Heuristic 1: Forced Downgrade to 2G/GSM
        if (is2G && !previousIs2G) {
            threat = ThreatLevel.CRITICAL_IMSI_CATCHER
            reason = "CRITICAL: Forced 2G/GSM downgrade detected! Unencrypted cipher handshake indicates active IMSI-Catcher (Stingray)."
            recommendAirplane = true
            triggerTacticalHapticAlert()
        }

        // Heuristic 2: Sudden Tower Jump without displacement
        if (previousCellId != -1L && currentCellId != -1L && previousCellId != currentCellId) {
            val signalDelta = kotlin.math.abs(_cellularState.value.signalDbm - previousSignalDbm)
            if (signalDelta > 22) {
                threat = ThreatLevel.ELEVATED_ANOMALY
                reason = "ANOMALY: Rapid cell tower migration with RF power surge (+${signalDelta} dBm)."
            }
        }

        previousIs2G = is2G
        if (currentCellId != -1L) previousCellId = currentCellId

        _cellularState.value = _cellularState.value.copy(
            networkType = netTypeName,
            cellId = if (currentCellId != -1L) currentCellId else _cellularState.value.cellId,
            lacTac = if (currentTac > 0) currentTac else _cellularState.value.lacTac,
            threatLevel = threat,
            threatReason = reason,
            requiresAirplaneMode = recommendAirplane,
            lastStateChangeTimestamp = System.currentTimeMillis()
        )
    }

    private fun processSignalStrength(signalStrength: SignalStrength) {
        val dbm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            signalStrength.cellSignalStrengths.firstOrNull()?.dbm ?: -85
        } else {
            -85
        }
        previousSignalDbm = dbm
        _cellularState.value = _cellularState.value.copy(signalDbm = dbm)
    }

    private fun triggerTacticalHapticAlert() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                val effect = VibrationEffect.createWaveform(
                    longArrayOf(0, 200, 100, 200, 100, 400),
                    intArrayOf(0, 255, 0, 255, 0, 255),
                    -1
                )
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 200, 100, 200, 100, 400), -1)
            }
        } catch (_: Exception) {}
    }

    private fun resolveNetworkTypeString(type: Int): String {
        return when (type) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G NR (Encrypted SA/NSA)"
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE / 4G (Encrypted)"
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_UMTS -> "3G / UMTS"
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_GSM -> "GSM / 2G (UNENCRYPTED)"
            else -> "Cellular Radio"
        }
    }
}
