package com.aegis.cloak.model

enum class CloakStatus {
    DISENGAGED,
    INITIALIZING,
    ACTIVE_CLOAKED,
    ERROR_MOCK_PERMISSION_REQUIRED,
    ERROR_SENSOR_UNAVAILABLE
}

enum class VpnState {
    DISCONNECTED,
    CONNECTING,
    SECURED_TUNNEL,
    KILL_SWITCH_ENGAGED,
    ERROR
}

enum class ThreatLevel(val label: String, val colorHex: Long) {
    LOW_SECURE("LOW // SECURE", 0xFF00E676),
    ELEVATED_ANOMALY("ELEVATED // ANOMALY", 0xFFFFB000),
    CRITICAL_IMSI_CATCHER("CRITICAL // IMSI-CATCHER DETECTED", 0xFFFF3B30)
}

enum class MapLayerMode(val displayName: String, val shortCode: String) {
    GOOGLE_ROADS("Google Road", "ROAD"),
    DARK_MODE("Dark Road", "DARK"),
    SATELLITE_HYBRID("Satellite Hybrid", "HYBRID")
}

data class SearchPlaceResult(
    val placeId: Long,
    val displayName: String,
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val type: String
)

data class TargetCoordinates(
    val latitude: Double = 35.658034,
    val longitude: Double = 139.701636,
    val altitude: Double = 42.0,
    val label: String = "Tokyo - Shibuya Crossing"
) {
    companion object {
        val PRESETS = listOf(
            TargetCoordinates(35.658034, 139.701636, 42.0, "Tokyo // Shibuya HQ"),
            TargetCoordinates(48.873792, 2.295028, 120.0, "Paris // Étoile Bunker"),
            TargetCoordinates(47.376887, 8.541694, 408.0, "Zurich // Limmat Outpost"),
            TargetCoordinates(1.283375, 103.860725, 15.0, "Singapore // Marina Relay"),
            TargetCoordinates(78.223172, 15.626723, 130.0, "Svalbard // Arctic Seed Vault"),
            TargetCoordinates(37.566535, 126.977969, 38.0, "Seoul // Jongno Bastion")
        )
    }
}

data class KinematicTelemetry(
    val stepCount: Int = 0,
    val currentSpeedMps: Float = 0.0f,
    val currentSpeedKmh: Float = 0.0f,
    val headingDegrees: Float = 0.0f,
    val headingRadians: Float = 0.0f,
    val accelerationMagnitude: Float = 0.0f,
    val displacementDeltaMeters: Double = 0.0,
    val totalDisplacementMeters: Double = 0.0,
    val simulatedSatellites: Int = 14,
    val gaussianDriftMeters: Double = 0.0,
    val currentMockLatitude: Double = 35.658034,
    val currentMockLongitude: Double = 139.701636,
    val currentMockAltitude: Double = 42.0,
    val horizontalAccuracyMeters: Float = 2.4f,
    val verticalAccuracyMeters: Float = 3.1f,
    val isMoving: Boolean = false,
    val lastUpdateTimestamp: Long = System.currentTimeMillis()
)

data class VpnTelemetry(
    val state: VpnState = VpnState.DISCONNECTED,
    val bytesIn: Long = 0L,
    val bytesOut: Long = 0L,
    val packetsSanitized: Long = 0L,
    val dnsQueriesSecured: Long = 0L,
    val webrtcLeaksBlocked: Long = 0L,
    val dohServer: String = "Cloudflare DoH (1.1.1.1)",
    val killSwitchEngaged: Boolean = true,
    val virtualIp: String = "10.13.37.2"
)

data class CellularTelemetry(
    val networkType: String = "LTE / 4G (Encrypted)",
    val operatorName: String = "SECURE_CARRIER",
    val signalDbm: Int = -85,
    val cellId: Long = 18492042L,
    val lacTac: Int = 4201,
    val threatLevel: ThreatLevel = ThreatLevel.LOW_SECURE,
    val threatReason: String = "Encrypted tower handshake verified. No downdraft detected.",
    val requiresAirplaneMode: Boolean = false,
    val lastStateChangeTimestamp: Long = System.currentTimeMillis()
)

data class TacticalUiState(
    val cloakEngaged: Boolean = false,
    val cloakStatus: CloakStatus = CloakStatus.DISENGAGED,
    val targetCoordinates: TargetCoordinates = TargetCoordinates(),
    val kinematicTelemetry: KinematicTelemetry = KinematicTelemetry(),
    val vpnTelemetry: VpnTelemetry = VpnTelemetry(),
    val cellularTelemetry: CellularTelemetry = CellularTelemetry(),
    val isDeveloperMockGranted: Boolean = false,
    val requiredPermissionsGranted: Boolean = false,
    val driftFactorMultiplier: Float = 1.0f,
    val showAirplaneModeAlert: Boolean = false,
    val showPresetsDialog: Boolean = false,
    val showCoordinatesEditDialog: Boolean = false,
    val showAuditLogDialog: Boolean = false,
    val systemStatusMessage: String = "AEGIS ENGINE STANDBY // SYSTEM READY"
)
