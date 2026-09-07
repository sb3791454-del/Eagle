package com.aegis.cloak.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Criteria
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.aegis.cloak.MainActivity
import com.example.R
import com.aegis.cloak.engine.KinematicEngine
import com.aegis.cloak.model.CloakStatus
import com.aegis.cloak.model.KinematicTelemetry
import com.aegis.cloak.model.TargetCoordinates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MockLocationService : LifecycleService() {

    companion object {
        const val ACTION_START = "com.aegis.cloak.action.START_MOCK"
        const val ACTION_STOP = "com.aegis.cloak.action.STOP_MOCK"
        const val ACTION_UPDATE_COORDINATES = "com.aegis.cloak.action.UPDATE_COORDINATES"
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
        const val EXTRA_ALTITUDE = "extra_altitude"
        const val EXTRA_DRIFT_FACTOR = "extra_drift_factor"

        private const val NOTIFICATION_ID = 4040
        private const val CHANNEL_ID = "aegis_mock_location_channel"

        private val _telemetryState = MutableStateFlow(KinematicTelemetry())
        val telemetryState: StateFlow<KinematicTelemetry> = _telemetryState.asStateFlow()

        private val _serviceStatus = MutableStateFlow(CloakStatus.DISENGAGED)
        val serviceStatus: StateFlow<CloakStatus> = _serviceStatus.asStateFlow()
    }

    private lateinit var locationManager: LocationManager
    private lateinit var sensorManager: SensorManager
    private var kinematicEngine: KinematicEngine? = null
    private var injectionJob: Job? = null
    private var isProviderActive = false

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START -> {
                val lat = intent.getDoubleExtra(EXTRA_LATITUDE, 35.658034)
                val lon = intent.getDoubleExtra(EXTRA_LONGITUDE, 139.701636)
                val alt = intent.getDoubleExtra(EXTRA_ALTITUDE, 42.0)
                val driftFactor = intent.getFloatExtra(EXTRA_DRIFT_FACTOR, 1.0f)
                startMockEngine(TargetCoordinates(lat, lon, alt), driftFactor)
            }

            ACTION_UPDATE_COORDINATES -> {
                val lat = intent.getDoubleExtra(EXTRA_LATITUDE, 35.658034)
                val lon = intent.getDoubleExtra(EXTRA_LONGITUDE, 139.701636)
                val alt = intent.getDoubleExtra(EXTRA_ALTITUDE, 42.0)
                kinematicEngine?.updateBaseTarget(TargetCoordinates(lat, lon, alt))
            }

            ACTION_STOP -> {
                stopMockEngine()
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startMockEngine(target: TargetCoordinates, driftFactor: Float) {
        val notification = buildForegroundNotification(target.latitude, target.longitude, "INITIALIZING CLOAK")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        kinematicEngine = KinematicEngine(target).apply {
            this.driftFactor = driftFactor
        }

        registerSensors()

        val setupSuccess = setupTestProviders()
        if (!setupSuccess) {
            _serviceStatus.value = CloakStatus.ERROR_MOCK_PERMISSION_REQUIRED
            updateNotification("ERROR: SET AS MOCK APP IN DEVELOPER OPTIONS")
            return
        }

        _serviceStatus.value = CloakStatus.ACTIVE_CLOAKED
        isProviderActive = true

        startInjectionLoop()
    }

    private fun registerSensors() {
        val engine = kinematicEngine ?: return

        // 1. Rotation Vector for high-precision azimuth tracking
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        rotationSensor?.let {
            sensorManager.registerListener(engine, it, SensorManager.SENSOR_DELAY_GAME)
        }

        // 2. Linear Acceleration for pure user movement thrust
        val linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        linearAccel?.let {
            sensorManager.registerListener(engine, it, SensorManager.SENSOR_DELAY_GAME)
        }

        // 3. Hardware Step Detector
        val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepDetector?.let {
            sensorManager.registerListener(engine, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    private fun unregisterSensors() {
        kinematicEngine?.let {
            sensorManager.unregisterListener(it)
        }
    }

    private fun setupTestProviders(): Boolean {
        return try {
            safeSetupSingleProvider(LocationManager.GPS_PROVIDER)
            try {
                safeSetupSingleProvider(LocationManager.NETWORK_PROVIDER)
            } catch (_: Exception) {
                // Network provider optional
            }
            true
        } catch (e: SecurityException) {
            _serviceStatus.value = CloakStatus.ERROR_MOCK_PERMISSION_REQUIRED
            false
        } catch (e: Exception) {
            _serviceStatus.value = CloakStatus.ERROR_MOCK_PERMISSION_REQUIRED
            false
        }
    }

    private fun safeSetupSingleProvider(providerName: String) {
        try {
            locationManager.removeTestProvider(providerName)
        } catch (_: Exception) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val properties = ProviderProperties.Builder()
                .setHasAltitudeSupport(true)
                .setHasSpeedSupport(true)
                .setHasBearingSupport(true)
                .setPowerUsage(ProviderProperties.POWER_USAGE_LOW)
                .setAccuracy(ProviderProperties.ACCURACY_FINE)
                .build()
            locationManager.addTestProvider(
                providerName,
                properties,
                emptySet()
            )
        } else {
            @Suppress("DEPRECATION")
            locationManager.addTestProvider(
                providerName,
                false, // requiresNetwork
                false, // requiresSatellite
                false, // requiresCell
                false, // hasMonetaryCost
                true,  // supportsAltitude
                true,  // supportsSpeed
                true,  // supportsBearing
                Criteria.POWER_LOW,
                Criteria.ACCURACY_FINE
            )
        }
        locationManager.setTestProviderEnabled(providerName, true)
    }

    private fun startInjectionLoop() {
        injectionJob?.cancel()
        injectionJob = lifecycleScope.launch(Dispatchers.Default) {
            while (isActive && isProviderActive) {
                val engine = kinematicEngine ?: break

                // 1. Advance dead reckoning and Gaussian atmospheric drift
                val deltaMeters = engine.updateContinuousKinematics()
                val isMoving = deltaMeters > 0.05 || engine.currentSpeedMps > 0.3f

                // 2. Synthesize mock location payload
                val mockGpsLocation = engine.createFusedMockLocation(LocationManager.GPS_PROVIDER)
                val mockNetLocation = engine.createFusedMockLocation(LocationManager.NETWORK_PROVIDER)

                // 3. Inject to Android Location Subsystem
                try {
                    locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, mockGpsLocation)
                    try {
                        locationManager.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, mockNetLocation)
                    } catch (_: Exception) {}
                } catch (e: SecurityException) {
                    _serviceStatus.value = CloakStatus.ERROR_MOCK_PERMISSION_REQUIRED
                    break
                } catch (_: Exception) {}

                // 4. Expose live telemetry to UI
                val snapshot = engine.getTelemetrySnapshot(isMoving, deltaMeters)
                _telemetryState.value = snapshot

                // Update notification text periodically
                if (System.currentTimeMillis() % 4000 < 500) {
                    updateNotification(
                        String.format(
                            "CLOAKED: %.5f, %.5f | Speed: %.1f km/h | Drift: ±%.1fm",
                            snapshot.currentMockLatitude,
                            snapshot.currentMockLongitude,
                            snapshot.currentSpeedKmh,
                            snapshot.gaussianDriftMeters
                        )
                    )
                }

                delay(500L) // 2 Hz injection cycle (optimal natural GPS cadence)
            }
        }
    }

    private fun stopMockEngine() {
        isProviderActive = false
        injectionJob?.cancel()
        injectionJob = null

        unregisterSensors()

        try {
            locationManager.removeTestProvider(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {}
        try {
            locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {}

        _serviceStatus.value = CloakStatus.DISENGAGED
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AegisCloak Kinematic Engine",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tactical persistent mock location provider"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(lat: Double, lon: Double, status: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MockLocationService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AEGIS CLOAK // KINEMATIC ENGINE ACTIVE")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "DISENGAGE", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val engine = kinematicEngine
        val lat = engine?.currentLatitude ?: 0.0
        val lon = engine?.currentLongitude ?: 0.0
        val notification = buildForegroundNotification(lat, lon, text)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        stopMockEngine()
        super.onDestroy()
    }
}
