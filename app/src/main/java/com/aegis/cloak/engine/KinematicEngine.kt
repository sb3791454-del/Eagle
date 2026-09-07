package com.aegis.cloak.engine

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.SystemClock
import com.aegis.cloak.model.KinematicTelemetry
import com.aegis.cloak.model.TargetCoordinates
import java.util.Random
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * KinematicEngine translates hardware IMU signals (Accelerometer, Gyroscope, Rotation Vector, Step Detector)
 * into geodesic displacement deltas applied to a primary geographic target coordinate.
 * It also injects temporally correlated Gaussian atmospheric GPS drift to bypass anti-spoofing heuristics.
 */
class KinematicEngine(
    private var baseTarget: TargetCoordinates
) : SensorEventListener {

    companion object {
        const val WGS84_EARTH_RADIUS_METERS = 6378137.0
        const val DEFAULT_STRIDE_METERS = 0.762 // Standard tactical walking stride
        const val MAX_WALK_SPEED_MPS = 5.5f
    }

    private val random = Random()

    // Position State
    @Volatile
    var currentLatitude: Double = baseTarget.latitude
        private set

    @Volatile
    var currentLongitude: Double = baseTarget.longitude
        private set

    @Volatile
    var currentAltitude: Double = baseTarget.altitude
        private set

    // Kinematics State
    @Volatile
    var totalDisplacementMeters: Double = 0.0
        private set

    @Volatile
    var stepCount: Int = 0
        private set

    @Volatile
    var currentSpeedMps: Float = 0.0f
        private set

    @Volatile
    var currentHeadingDegrees: Float = 0.0f
        private set

    @Volatile
    var currentHeadingRadians: Float = 0.0f
        private set

    @Volatile
    var accelerationMagnitude: Float = 0.0f
        private set

    @Volatile
    var driftFactor: Float = 1.0f

    // IMU internal registers
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var lastStepTimestamp: Long = 0L
    private var lastKinematicComputeTimestamp: Long = SystemClock.elapsedRealtime()

    // Gaussian Atmospheric Drift State (Ornstein-Uhlenbeck process for continuous correlation)
    private var driftEastMeters: Double = 0.0
    private var driftNorthMeters: Double = 0.0
    private var driftAltMeters: Double = 0.0
    private val driftCorrelationRho = 0.88 // Temporal persistence factor

    fun updateBaseTarget(target: TargetCoordinates) {
        baseTarget = target
        currentLatitude = target.latitude
        currentLongitude = target.longitude
        currentAltitude = target.altitude
        totalDisplacementMeters = 0.0
        driftEastMeters = 0.0
        driftNorthMeters = 0.0
        driftAltMeters = 0.0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                var azimuthRad = orientationAngles[0]
                if (azimuthRad < 0) azimuthRad += (2 * PI).toFloat()
                currentHeadingRadians = azimuthRad
                currentHeadingDegrees = Math.toDegrees(azimuthRad.toDouble()).toFloat()
            }

            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]
                val mag = sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()
                accelerationMagnitude = mag

                // Low-pass velocity estimation from dynamic inertial vector
                val dynamicThrust = max(0f, mag - 0.25f)
                val targetSpeed = min(MAX_WALK_SPEED_MPS, dynamicThrust * 1.35f)
                currentSpeedMps = currentSpeedMps * 0.85f + targetSpeed * 0.15f
            }

            Sensor.TYPE_ACCELEROMETER -> {
                // Fallback for devices without separate linear accelerometer
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]
                val rawMag = sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()
                val netAcc = max(0f, kotlin.math.abs(rawMag - 9.80665f) - 0.35f)
                accelerationMagnitude = netAcc
            }

            Sensor.TYPE_STEP_DETECTOR -> {
                stepCount++
                lastStepTimestamp = SystemClock.elapsedRealtime()
                // Each detected physical step directly triggers dead reckoning displacement
                applyDisplacement(DEFAULT_STRIDE_METERS)
            }

            Sensor.TYPE_STEP_COUNTER -> {
                // Keep step cadence alive if detector is not present
                val currentSteps = event.values[0].toInt()
                if (stepCount == 0) {
                    stepCount = currentSteps
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Telemetry accuracy monitoring
    }

    /**
     * Applies instantaneous kinematic displacement vector along current azimuth heading
     */
    @Synchronized
    fun applyDisplacement(distanceMeters: Double) {
        if (distanceMeters <= 0.0) return

        val headingRad = currentHeadingRadians.toDouble()
        val deltaNorth = distanceMeters * cos(headingRad)
        val deltaEast = distanceMeters * sin(headingRad)

        // Geodesic differential translation on WGS84 ellipsoid
        val deltaLat = (deltaNorth / WGS84_EARTH_RADIUS_METERS) * (180.0 / PI)
        val latRad = currentLatitude * (PI / 180.0)
        val radiusAtLat = WGS84_EARTH_RADIUS_METERS * cos(latRad)
        val deltaLon = if (radiusAtLat > 0.00001) {
            (deltaEast / radiusAtLat) * (180.0 / PI)
        } else {
            0.0
        }

        currentLatitude += deltaLat
        currentLongitude += deltaLon
        totalDisplacementMeters += distanceMeters
    }

    /**
     * Evaluates continuous periodic movement (dead reckoning when user is walking based on velocity).
     */
    @Synchronized
    fun updateContinuousKinematics(): Double {
        val now = SystemClock.elapsedRealtime()
        val dtSec = (now - lastKinematicComputeTimestamp).coerceAtLeast(10L) / 1000.0
        lastKinematicComputeTimestamp = now

        // Check if movement detected either from velocity or recent step detection
        val stepRecentlyActive = (now - lastStepTimestamp) < 1400L
        val effectiveSpeed = if (stepRecentlyActive) {
            max(currentSpeedMps, 1.25f) // Typical brisk walk
        } else {
            currentSpeedMps
        }

        val distance = if (effectiveSpeed > 0.2f) {
            val d = (effectiveSpeed * dtSec).toDouble()
            applyDisplacement(d)
            d
        } else {
            0.0
        }

        // Advance Gaussian atmospheric drift (Ornstein-Uhlenbeck stochastic process)
        computeAtmosphericDrift()

        return distance
    }

    /**
     * Generates Box-Muller Gaussian atmospheric satellite ionospheric jitter (sigma ~ 1.5m).
     * This mimics real GNSS satellite clock and tropospheric propagation noise.
     */
    private fun computeAtmosphericDrift() {
        val u1 = max(1e-7, random.nextDouble())
        val u2 = random.nextDouble()
        val radius = sqrt(-2.0 * ln(u1))
        val theta = 2.0 * PI * u2
        val whiteNoiseX = radius * cos(theta)
        val whiteNoiseY = radius * sin(theta)
        val whiteNoiseZ = (random.nextDouble() - 0.5) * 2.0

        val sigmaMeters = 1.35 * driftFactor.toDouble()
        val sqrtOneMinusRhoSq = sqrt(1.0 - driftCorrelationRho * driftCorrelationRho)

        driftEastMeters = (driftEastMeters * driftCorrelationRho) + (whiteNoiseX * sigmaMeters * sqrtOneMinusRhoSq)
        driftNorthMeters = (driftNorthMeters * driftCorrelationRho) + (whiteNoiseY * sigmaMeters * sqrtOneMinusRhoSq)
        driftAltMeters = (driftAltMeters * driftCorrelationRho) + (whiteNoiseZ * (sigmaMeters * 1.5) * sqrtOneMinusRhoSq)
    }

    /**
     * Synthesizes an ultra-realistic, anti-spoofing resilient Android Location object
     */
    fun createFusedMockLocation(providerName: String = "gps"): Location {
        // Combine raw kinematic coordinate with Gaussian drift
        val driftLatDelta = (driftNorthMeters / WGS84_EARTH_RADIUS_METERS) * (180.0 / PI)
        val latRad = currentLatitude * (PI / 180.0)
        val radiusAtLat = WGS84_EARTH_RADIUS_METERS * cos(latRad)
        val driftLonDelta = if (radiusAtLat > 0.00001) {
            (driftEastMeters / radiusAtLat) * (180.0 / PI)
        } else 0.0

        val injectedLat = currentLatitude + driftLatDelta
        val injectedLon = currentLongitude + driftLonDelta
        val injectedAlt = currentAltitude + driftAltMeters

        val location = Location(providerName).apply {
            latitude = injectedLat
            longitude = injectedLon
            altitude = injectedAlt
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            speed = currentSpeedMps
            bearing = currentHeadingDegrees
            // Dynamic accuracy simulation (1.8m to 3.8m + drift effect)
            val computedAccuracy = (2.2f + (sqrt(driftEastMeters * driftEastMeters + driftNorthMeters * driftNorthMeters) * 0.35f)).toFloat()
            accuracy = computedAccuracy.coerceIn(1.8f, 5.0f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                verticalAccuracyMeters = (3.2f + (kotlin.math.abs(driftAltMeters) * 0.4f)).toFloat().coerceIn(2.5f, 7.0f)
                speedAccuracyMetersPerSecond = 0.25f
                bearingAccuracyDegrees = 3.5f
            }
        }
        return location
    }

    fun getTelemetrySnapshot(isMoving: Boolean, lastDistanceDelta: Double): KinematicTelemetry {
        val totalDriftMag = sqrt(driftEastMeters * driftEastMeters + driftNorthMeters * driftNorthMeters)
        return KinematicTelemetry(
            stepCount = stepCount,
            currentSpeedMps = currentSpeedMps,
            currentSpeedKmh = currentSpeedMps * 3.6f,
            headingDegrees = currentHeadingDegrees,
            headingRadians = currentHeadingRadians,
            accelerationMagnitude = accelerationMagnitude,
            displacementDeltaMeters = lastDistanceDelta,
            totalDisplacementMeters = totalDisplacementMeters,
            simulatedSatellites = 14 + (totalDriftMag % 4).toInt(),
            gaussianDriftMeters = totalDriftMag,
            currentMockLatitude = currentLatitude,
            currentMockLongitude = currentLongitude,
            currentMockAltitude = currentAltitude,
            horizontalAccuracyMeters = (2.2f + (totalDriftMag * 0.35f)).toFloat().coerceIn(1.8f, 5.0f),
            verticalAccuracyMeters = 3.4f,
            isMoving = isMoving,
            lastUpdateTimestamp = System.currentTimeMillis()
        )
    }
}
