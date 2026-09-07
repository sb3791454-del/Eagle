package com.aegis.cloak.ui

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.preference.PreferenceManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aegis.cloak.model.KinematicTelemetry
import com.aegis.cloak.model.TargetCoordinates
import com.aegis.cloak.ui.theme.TacticalAmber
import com.aegis.cloak.ui.theme.TacticalBorder
import com.aegis.cloak.ui.theme.TacticalCardBg
import com.aegis.cloak.ui.theme.TacticalCyan
import com.aegis.cloak.ui.theme.TacticalDarkBg
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TacticalMapView(
    targetCoordinates: TargetCoordinates,
    telemetry: KinematicTelemetry,
    isCloakActive: Boolean,
    onMapCoordinateSelected: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Initialize OSMDroid config
    remember {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = "ProjectAegisCloakTacticalMap"
    }

    // Radar sweep rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    var internalMapView: MapView? = null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp))
            .background(TacticalDarkBg)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("tactical_map_view"),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(16.5)

                    val targetPoint = GeoPoint(targetCoordinates.latitude, targetCoordinates.longitude)
                    controller.setCenter(targetPoint)

                    // Tactical OLED Dark Matrix Filter for Tiles
                    val darkMatrix = ColorMatrix(
                        floatArrayOf(
                            -0.25f, 0f, 0f, 0f, 60f,
                            0f, -0.25f, 0f, 0f, 70f,
                            0f, 0f, -0.25f, 0f, 85f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                    overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(darkMatrix))

                    // Map click receiver to set new target coordinate
                    val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            if (p != null) {
                                onMapCoordinateSelected(p.latitude, p.longitude)
                                return true
                            }
                            return false
                        }

                        override fun longPressHelper(p: GeoPoint?): Boolean {
                            if (p != null) {
                                onMapCoordinateSelected(p.latitude, p.longitude)
                                return true
                            }
                            return false
                        }
                    })
                    overlays.add(0, mapEventsOverlay)

                    internalMapView = this
                }
            },
            update = { mapView ->
                // Update Target Marker
                val targetPoint = GeoPoint(targetCoordinates.latitude, targetCoordinates.longitude)
                val mockPoint = GeoPoint(telemetry.currentMockLatitude, telemetry.currentMockLongitude)

                mapView.overlays.removeAll { it is Marker || it is Polygon }

                // 1. Target Injected Coordinate Marker
                val targetMarker = Marker(mapView).apply {
                    position = targetPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "TARGET // ${targetCoordinates.label}"
                    snippet = String.format("%.6f, %.6f", targetPoint.latitude, targetPoint.longitude)
                }
                mapView.overlays.add(targetMarker)

                // 2. Real-time Fused Kinematic Mock Marker (when active)
                if (isCloakActive) {
                    val mockMarker = Marker(mapView).apply {
                        position = mockPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "ACTIVE MOCK // KINEMATIC MIRROR"
                        rotation = telemetry.headingDegrees
                    }
                    mapView.overlays.add(mockMarker)

                    // Draw Gaussian drift accuracy ring
                    val driftCircle = Polygon(mapView).apply {
                        points = Polygon.pointsAsCircle(mockPoint, telemetry.horizontalAccuracyMeters.toDouble())
                        outlinePaint.color = android.graphics.Color.argb(160, 0, 240, 255)
                        outlinePaint.strokeWidth = 2.0f
                        fillPaint.color = android.graphics.Color.argb(35, 0, 240, 255)
                    }
                    mapView.overlays.add(driftCircle)
                }

                mapView.invalidate()
            }
        )

        // Tactical HUD Vector Canvas Overlay (Crosshairs, Compass Reticle, Radar Sweep)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val minDim = minOf(size.width, size.height)

            // Outer Reticle Circles
            drawCircle(
                color = Color(0x3300F0FF),
                radius = minDim * 0.42f,
                center = center,
                style = Stroke(width = 1.2f)
            )
            drawCircle(
                color = Color(0x2200F0FF),
                radius = minDim * 0.25f,
                center = center,
                style = Stroke(width = 1.0f)
            )

            // Crosshair ticks
            val crosshairLen = 18.dp.toPx()
            drawLine(
                color = TacticalCyan,
                start = Offset(center.x - crosshairLen, center.y),
                end = Offset(center.x + crosshairLen, center.y),
                strokeWidth = 1.5f
            )
            drawLine(
                color = TacticalCyan,
                start = Offset(center.x, center.y - crosshairLen),
                end = Offset(center.x, center.y + crosshairLen),
                strokeWidth = 1.5f
            )

            // Radar Sweep line when Cloak is active
            if (isCloakActive) {
                val rad = Math.toRadians(sweepAngle.toDouble())
                val sweepRadius = minDim * 0.42f
                val sweepEnd = Offset(
                    center.x + (sweepRadius * cos(rad)).toFloat(),
                    center.y + (sweepRadius * sin(rad)).toFloat()
                )
                drawLine(
                    color = TacticalCyan.copy(alpha = 0.65f),
                    start = center,
                    end = sweepEnd,
                    strokeWidth = 2.0f
                )
            }
        }

        // Tactical HUD Map Header Overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(TacticalCardBg.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                .border(0.8.dp, TacticalBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Column {
                Text(
                    text = "TARGET: ${targetCoordinates.label}",
                    color = TacticalAmber,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = String.format(
                        "LAT: %.6f | LON: %.6f | ALT: %.0fm",
                        if (isCloakActive) telemetry.currentMockLatitude else targetCoordinates.latitude,
                        if (isCloakActive) telemetry.currentMockLongitude else targetCoordinates.longitude,
                        if (isCloakActive) telemetry.currentMockAltitude else targetCoordinates.altitude
                    ),
                    color = TacticalCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Map Control Buttons (Center, Zoom)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            IconButton(
                onClick = {
                    internalMapView?.let { map ->
                        val pt = if (isCloakActive) {
                            GeoPoint(telemetry.currentMockLatitude, telemetry.currentMockLongitude)
                        } else {
                            GeoPoint(targetCoordinates.latitude, targetCoordinates.longitude)
                        }
                        map.controller.animateTo(pt)
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(TacticalCardBg.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                    .border(0.8.dp, TacticalCyan, RoundedCornerShape(4.dp))
                    .testTag("center_map_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Center Map",
                    tint = TacticalCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            IconButton(
                onClick = { internalMapView?.controller?.zoomIn() },
                modifier = Modifier
                    .size(36.dp)
                    .background(TacticalCardBg.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                    .border(0.8.dp, TacticalBorder, RoundedCornerShape(4.dp))
                    .testTag("zoom_in_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Zoom In",
                    tint = TacticalCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            IconButton(
                onClick = { internalMapView?.controller?.zoomOut() },
                modifier = Modifier
                    .size(36.dp)
                    .background(TacticalCardBg.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                    .border(0.8.dp, TacticalBorder, RoundedCornerShape(4.dp))
                    .testTag("zoom_out_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Zoom Out",
                    tint = TacticalCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            internalMapView?.onDetach()
        }
    }
}
