package com.aegis.cloak

import android.location.Location
import android.location.LocationManager
import com.aegis.cloak.engine.KinematicEngine
import com.aegis.cloak.model.TargetCoordinates
import com.aegis.cloak.model.ThreatLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KinematicEngineTest {

    @Test
    fun testBaseTargetInitialization() {
        val target = TargetCoordinates(35.658034, 139.701636, 42.0, "Tokyo Base")
        val engine = KinematicEngine(target)

        assertEquals(35.658034, engine.currentLatitude, 0.000001)
        assertEquals(139.701636, engine.currentLongitude, 0.000001)
        assertEquals(42.0, engine.currentAltitude, 0.1)
    }

    @Test
    fun testDeadReckoningDisplacement() {
        val target = TargetCoordinates(0.0, 0.0, 10.0, "Equator")
        val engine = KinematicEngine(target)

        // Apply 100 meters displacement
        engine.applyDisplacement(100.0)

        // At least one coordinate axis shifts depending on heading
        val telemetry = engine.getTelemetrySnapshot(isMoving = true, lastDistanceDelta = 100.0)
        assertTrue("Total displacement should be recorded", telemetry.totalDisplacementMeters >= 99.0)
    }

    @Test
    fun testContinuousKinematicsAndGaussianDrift() {
        val target = TargetCoordinates(48.858844, 2.294351, 35.0, "Paris")
        val engine = KinematicEngine(target)
        engine.driftFactor = 1.0f

        // Advance continuous kinematics multiple cycles
        for (i in 0 until 10) {
            engine.updateContinuousKinematics()
        }

        val snapshot = engine.getTelemetrySnapshot(isMoving = false, lastDistanceDelta = 0.0)
        // Natural GPS atmospheric noise should be recorded and within bounds
        assertTrue("Drift should be tracked", snapshot.gaussianDriftMeters >= 0.0)
        assertTrue("Drift should remain within reasonable physical bounds", snapshot.gaussianDriftMeters < 30.0)
    }

    @Test
    fun testFusedMockLocationAttributes() {
        val target = TargetCoordinates(47.3769, 8.5417, 408.0, "Zurich")
        val engine = KinematicEngine(target)

        val location: Location = engine.createFusedMockLocation(LocationManager.GPS_PROVIDER)

        assertNotNull(location)
        assertEquals(LocationManager.GPS_PROVIDER, location.provider)
        assertTrue("Location should have accuracy set", location.hasAccuracy())
        assertTrue("Location accuracy should be positive", location.accuracy > 0.0f)
        assertTrue("Location should have speed set", location.hasSpeed())
        assertTrue("Location should have bearing set", location.hasBearing())
        assertTrue("Location should have altitude set", location.hasAltitude())
    }

    @Test
    fun testTargetCoordinatesPresets() {
        assertTrue("Should provide tactical coordinate presets", TargetCoordinates.PRESETS.size >= 4)
        val tokyo = TargetCoordinates.PRESETS.first { it.label.contains("Tokyo") }
        assertEquals(35.658034, tokyo.latitude, 0.001)
        assertEquals(139.701636, tokyo.longitude, 0.001)
    }

    @Test
    fun testThreatLevelClassification() {
        val low = ThreatLevel.LOW_SECURE
        assertEquals("LOW // SECURE", low.label)

        val critical = ThreatLevel.CRITICAL_IMSI_CATCHER
        assertEquals("CRITICAL // IMSI-CATCHER DETECTED", critical.label)
    }
}
