package com.aegis.cloak.ui

import android.preference.PreferenceManager
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.R
import com.aegis.cloak.model.KinematicTelemetry
import com.aegis.cloak.model.TargetCoordinates
import com.aegis.cloak.ui.theme.TacticalAmber
import com.aegis.cloak.ui.theme.TacticalBorder
import com.aegis.cloak.ui.theme.TacticalCardBg
import com.aegis.cloak.ui.theme.TacticalCyan
import com.aegis.cloak.ui.theme.TacticalDarkBg
import com.aegis.cloak.ui.theme.TacticalGreen
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

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

    var internalMapView: MapView? = null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
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
                    // Crisp, clear, standard high-visibility Mapnik vector/raster tiles
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(16.5)

                    val targetPoint = GeoPoint(targetCoordinates.latitude, targetCoordinates.longitude)
                    controller.setCenter(targetPoint)

                    // Clear any color filters to guarantee natural, crisp readability of streets and landmarks
                    overlayManager.tilesOverlay.setColorFilter(null)

                    // Interactive Tap / Click listener to set new mock coordinate on exact tapped spot
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
                    overlays.add(mapEventsOverlay)

                    internalMapView = this
                }
            },
            update = { mapView ->
                val targetPoint = GeoPoint(targetCoordinates.latitude, targetCoordinates.longitude)
                val mockPoint = GeoPoint(telemetry.currentMockLatitude, telemetry.currentMockLongitude)

                // Remove existing markers & polygons while keeping the MapEventsOverlay
                mapView.overlays.removeAll { it is Marker || it is Polygon }

                // 1. Interactive Tactical Crosshair Marker at Tapped / Target Location
                val targetCrosshairDrawable = ContextCompat.getDrawable(context, R.drawable.ic_tactical_crosshair)
                val targetMarker = Marker(mapView).apply {
                    position = targetPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    if (targetCrosshairDrawable != null) {
                        icon = targetCrosshairDrawable
                    }
                    title = "TARGET // ${targetCoordinates.label}"
                    snippet = String.format("LAT: %.6f | LON: %.6f", targetPoint.latitude, targetPoint.longitude)
                    setOnMarkerClickListener { marker, _ ->
                        marker.showInfoWindow()
                        true
                    }
                }
                mapView.overlays.add(targetMarker)

                // Tactical Target Range Ring
                val targetRangeCircle = Polygon(mapView).apply {
                    points = Polygon.pointsAsCircle(targetPoint, 30.0)
                    outlinePaint.color = android.graphics.Color.argb(220, 0, 240, 255)
                    outlinePaint.strokeWidth = 2.0f
                    fillPaint.color = android.graphics.Color.argb(35, 0, 240, 255)
                }
                mapView.overlays.add(targetRangeCircle)

                // 2. Real-time Fused Kinematic Mock Marker (when active)
                if (isCloakActive) {
                    val mockArrowDrawable = ContextCompat.getDrawable(context, R.drawable.ic_mock_live_arrow)
                    val mockMarker = Marker(mapView).apply {
                        position = mockPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        if (mockArrowDrawable != null) {
                            icon = mockArrowDrawable
                        }
                        title = "LIVE MOCK // KINEMATIC MIRROR"
                        snippet = String.format("SPEED: %.1f km/h | HEADING: %.0f°", telemetry.currentSpeedKmh, telemetry.headingDegrees)
                        rotation = telemetry.headingDegrees
                    }
                    mapView.overlays.add(mockMarker)

                    // Draw Gaussian drift accuracy ring
                    val driftCircle = Polygon(mapView).apply {
                        points = Polygon.pointsAsCircle(mockPoint, telemetry.horizontalAccuracyMeters.toDouble().coerceAtLeast(5.0))
                        outlinePaint.color = android.graphics.Color.argb(200, 0, 230, 118)
                        outlinePaint.strokeWidth = 2.0f
                        fillPaint.color = android.graphics.Color.argb(35, 0, 230, 118)
                    }
                    mapView.overlays.add(driftCircle)
                }

                mapView.invalidate()
            }
        )

        // Tactical HUD Header Overlay: Crisp High-Contrast Telemetry
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(TacticalCardBg.copy(alpha = 0.92f), RoundedCornerShape(6.dp))
                .border(1.dp, TacticalBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TARGET: ",
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = targetCoordinates.label,
                        color = TacticalAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = String.format(
                        "LAT: %.6f | LON: %.6f",
                        if (isCloakActive) telemetry.currentMockLatitude else targetCoordinates.latitude,
                        if (isCloakActive) telemetry.currentMockLongitude else targetCoordinates.longitude
                    ),
                    color = TacticalCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (isCloakActive) "TAP MAP TO RELOCATE MOCK IN REAL TIME" else "TAP MAP TO SET MOCK COORDINATES",
                    color = if (isCloakActive) TacticalGreen else Color(0xFF94A3B8),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Map Control Buttons (Recenter Target, Zoom In, Zoom Out)
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
                    .background(TacticalCardBg.copy(alpha = 0.95f), RoundedCornerShape(6.dp))
                    .border(1.dp, TacticalCyan, RoundedCornerShape(6.dp))
                    .testTag("center_map_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Center Map",
                    tint = TacticalCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            IconButton(
                onClick = { internalMapView?.controller?.zoomIn() },
                modifier = Modifier
                    .size(36.dp)
                    .background(TacticalCardBg.copy(alpha = 0.95f), RoundedCornerShape(6.dp))
                    .border(1.dp, TacticalBorder, RoundedCornerShape(6.dp))
                    .testTag("zoom_in_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Zoom In",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            IconButton(
                onClick = { internalMapView?.controller?.zoomOut() },
                modifier = Modifier
                    .size(36.dp)
                    .background(TacticalCardBg.copy(alpha = 0.95f), RoundedCornerShape(6.dp))
                    .border(1.dp, TacticalBorder, RoundedCornerShape(6.dp))
                    .testTag("zoom_out_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Zoom Out",
                    tint = Color.White,
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
