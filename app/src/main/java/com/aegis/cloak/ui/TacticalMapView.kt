package com.aegis.cloak.ui

import android.preference.PreferenceManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.example.R
import com.aegis.cloak.geocoding.GeocodingEngine
import com.aegis.cloak.model.KinematicTelemetry
import com.aegis.cloak.model.MapLayerMode
import com.aegis.cloak.model.SearchPlaceResult
import com.aegis.cloak.model.TargetCoordinates
import com.aegis.cloak.ui.theme.TacticalAmber
import com.aegis.cloak.ui.theme.TacticalBorder
import com.aegis.cloak.ui.theme.TacticalCardBg
import com.aegis.cloak.ui.theme.TacticalCyan
import com.aegis.cloak.ui.theme.TacticalDarkBg
import com.aegis.cloak.ui.theme.TacticalGreen
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

/**
 * Authentic Google Maps Standard Road Tile Source with forced English localization.
 * Server parameter `hl=en` forces English street names, cities, landmarks worldwide.
 */
object GoogleRoadsTileSource : OnlineTileSourceBase(
    "GoogleRoadsEn",
    0, 20, 256, ".png",
    arrayOf(
        "https://mt0.google.com/vt/lyrs=m&hl=en&",
        "https://mt1.google.com/vt/lyrs=m&hl=en&",
        "https://mt2.google.com/vt/lyrs=m&hl=en&",
        "https://mt3.google.com/vt/lyrs=m&hl=en&"
    )
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "${baseUrl}x=$x&y=$y&z=$zoom"
    }
}

/**
 * Google Satellite Hybrid Tile Source with authentic aerial photography + English road overlays.
 */
object GoogleSatelliteHybridTileSource : OnlineTileSourceBase(
    "GoogleHybridEn",
    0, 20, 256, ".jpg",
    arrayOf(
        "https://mt0.google.com/vt/lyrs=y&hl=en&",
        "https://mt1.google.com/vt/lyrs=y&hl=en&",
        "https://mt2.google.com/vt/lyrs=y&hl=en&",
        "https://mt3.google.com/vt/lyrs=y&hl=en&"
    )
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "${baseUrl}x=$x&y=$y&z=$zoom"
    }
}

/**
 * Tactical Dark Mode Vector Tile Source with clear contrast and English typography.
 */
object DarkRoadsTileSource : OnlineTileSourceBase(
    "CartoDarkEn",
    0, 20, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/dark_all/",
        "https://b.basemaps.cartocdn.com/dark_all/",
        "https://c.basemaps.cartocdn.com/dark_all/",
        "https://d.basemaps.cartocdn.com/dark_all/"
    )
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "$baseUrl$zoom/$x/$y.png"
    }
}

@Composable
fun TacticalMapView(
    targetCoordinates: TargetCoordinates,
    telemetry: KinematicTelemetry,
    isCloakActive: Boolean,
    onMapCoordinateSelected: (Double, Double, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Search Engine State
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchPlaceResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var isDropdownOpen by remember { mutableStateOf(false) }

    // Map Layer Mode State (Default: Google Standard Road View)
    var selectedLayerMode by remember { mutableStateOf(MapLayerMode.GOOGLE_ROADS) }

    // Reference to MapView
    var internalMapView by remember { mutableStateOf<MapView?>(null) }

    // Initialize OSMDroid config with English locale headers
    remember {
        @Suppress("DEPRECATION")
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        Configuration.getInstance().load(context, prefs)
        Configuration.getInstance().userAgentValue = "ProjectAegisCloakTacticalMap/1.0 (Android; English)"
        Configuration.getInstance().additionalHttpRequestProperties["Accept-Language"] = "en-US,en;q=0.9"
    }

    // Debounced Place Geocoding search query
    LaunchedEffect(searchQuery) {
        val query = searchQuery.trim()
        if (query.length >= 2) {
            delay(400) // 400ms debounce
            isSearching = true
            val results = GeocodingEngine.searchPlaces(query)
            searchResults = results
            isSearching = false
            isDropdownOpen = results.isNotEmpty()
        } else {
            searchResults = emptyList()
            isDropdownOpen = false
            isSearching = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(380.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp))
            .background(TacticalDarkBg)
    ) {
        // 1. Core Map View
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("tactical_map_view"),
            factory = { ctx ->
                MapView(ctx).apply {
                    // Start with Google Standard Road View with forced English labels
                    setTileSource(GoogleRoadsTileSource)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(16.5)

                    val targetPoint = GeoPoint(targetCoordinates.latitude, targetCoordinates.longitude)
                    controller.setCenter(targetPoint)

                    // Clear any color filters for authentic visual rendering
                    overlayManager.tilesOverlay.setColorFilter(null)

                    // Interactive Tap / Click listener for Seamless Tap-to-Mock
                    val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            if (p != null) {
                                isDropdownOpen = false
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                onMapCoordinateSelected(p.latitude, p.longitude, null)
                                return true
                            }
                            return false
                        }

                        override fun longPressHelper(p: GeoPoint?): Boolean {
                            if (p != null) {
                                isDropdownOpen = false
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                onMapCoordinateSelected(p.latitude, p.longitude, null)
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
                internalMapView = mapView

                // Switch tile source dynamically based on user layer selection
                val activeTileSource = when (selectedLayerMode) {
                    MapLayerMode.GOOGLE_ROADS -> GoogleRoadsTileSource
                    MapLayerMode.DARK_MODE -> DarkRoadsTileSource
                    MapLayerMode.SATELLITE_HYBRID -> GoogleSatelliteHybridTileSource
                }
                if (mapView.tileProvider.tileSource.name() != activeTileSource.name()) {
                    mapView.setTileSource(activeTileSource)
                }

                val targetPoint = GeoPoint(targetCoordinates.latitude, targetCoordinates.longitude)
                val mockPoint = GeoPoint(telemetry.currentMockLatitude, telemetry.currentMockLongitude)

                // Remove existing markers & polygons while keeping the MapEventsOverlay
                mapView.overlays.removeAll { it is Marker || it is Polygon }

                // Tactical Crosshair Marker at Target Location
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

                // Live Kinematic Mock Marker & Gaussian Drift Ring (when cloak active)
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

        // 2. Top-Docked Floating Tactical Place Search Bar & Layer Controls
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(8.dp)
                .zIndex(10f)
        ) {
            // Tactical Search Input Field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D1117).copy(alpha = 0.96f), RoundedCornerShape(6.dp))
                    .border(1.dp, if (isDropdownOpen) TacticalCyan else TacticalBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Places",
                        tint = TacticalCyan,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("map_place_search_input"),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(TacticalCyan),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        ),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search global landmark, city, address...",
                                    color = Color(0xFF6E7681),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (isSearching) {
                        CircularProgressIndicator(
                            color = TacticalCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear Search",
                            tint = Color(0xFF8B949E),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    searchQuery = ""
                                    searchResults = emptyList()
                                    isDropdownOpen = false
                                }
                        )
                    }
                }
            }

            // Dropdown Autocomplete Search Results Card
            AnimatedVisibility(
                visible = isDropdownOpen && searchResults.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .background(Color(0xFF161B22).copy(alpha = 0.98f), RoundedCornerShape(6.dp))
                        .border(1.dp, TacticalCyan.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                        .heightIn(max = 210.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(searchResults) { place ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        searchQuery = place.title
                                        isDropdownOpen = false
                                        keyboardController?.hide()
                                        focusManager.clearFocus()

                                        // Relocate target and kinematic trajectory immediately
                                        onMapCoordinateSelected(place.latitude, place.longitude, place.title)

                                        // Smoothly animate map camera to coordinates at zoom 17
                                        internalMapView?.let { map ->
                                            val geoPoint = GeoPoint(place.latitude, place.longitude)
                                            map.controller.animateTo(geoPoint)
                                            map.controller.setZoom(17.0)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = TacticalAmber,
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = place.title,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = place.type,
                                            color = TacticalCyan,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier
                                                .background(TacticalCyan.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Text(
                                        text = place.subtitle,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Map Layer Mode Selector Pills: [GOOGLE ROAD] | [DARK ROAD] | [HYBRID SATELLITE]
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                MapLayerMode.values().forEach { mode ->
                    val isSelected = selectedLayerMode == mode
                    val chipBg = if (isSelected) TacticalCyan.copy(alpha = 0.25f) else Color(0xFF0D1117).copy(alpha = 0.92f)
                    val chipBorder = if (isSelected) TacticalCyan else TacticalBorder
                    val chipText = if (isSelected) TacticalCyan else Color(0xFFCBD5E1)

                    Box(
                        modifier = Modifier
                            .background(chipBg, RoundedCornerShape(4.dp))
                            .border(1.dp, chipBorder, RoundedCornerShape(4.dp))
                            .clickable {
                                selectedLayerMode = mode
                                internalMapView?.let { map ->
                                    val source = when (mode) {
                                        MapLayerMode.GOOGLE_ROADS -> GoogleRoadsTileSource
                                        MapLayerMode.DARK_MODE -> DarkRoadsTileSource
                                        MapLayerMode.SATELLITE_HYBRID -> GoogleSatelliteHybridTileSource
                                    }
                                    map.setTileSource(source)
                                    map.tileProvider.clearTileCache()
                                    map.invalidate()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("layer_btn_${mode.shortCode.lowercase()}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (mode == MapLayerMode.SATELLITE_HYBRID) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = chipText,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = mode.shortCode,
                                color = chipText,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Real-time Tap Hint Banner
                Text(
                    text = if (isCloakActive) "TAP MAP TO REROUTE" else "TAP MAP TO PIN",
                    color = if (isCloakActive) TacticalGreen else TacticalAmber,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .background(Color(0xFF0D1117).copy(alpha = 0.90f), RoundedCornerShape(4.dp))
                        .border(1.dp, if (isCloakActive) TacticalGreen.copy(alpha = 0.5f) else TacticalAmber.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }

        // 3. Bottom-Start Target Coordinates Status Pill
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(TacticalCardBg.copy(alpha = 0.94f), RoundedCornerShape(6.dp))
                .border(1.dp, TacticalBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TARGET: ",
                        color = Color(0xFFE2E8F0),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = targetCoordinates.label.take(24),
                        color = TacticalAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = String.format(
                        "LAT: %.5f | LON: %.5f",
                        if (isCloakActive) telemetry.currentMockLatitude else targetCoordinates.latitude,
                        if (isCloakActive) telemetry.currentMockLongitude else targetCoordinates.longitude
                    ),
                    color = TacticalCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 4. Bottom-End Map Navigation Controls (Recenter Target, Zoom In, Zoom Out)
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
                    .size(34.dp)
                    .background(TacticalCardBg.copy(alpha = 0.95f), RoundedCornerShape(6.dp))
                    .border(1.dp, TacticalCyan, RoundedCornerShape(6.dp))
                    .testTag("center_map_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Center Map",
                    tint = TacticalCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            IconButton(
                onClick = { internalMapView?.controller?.zoomIn() },
                modifier = Modifier
                    .size(34.dp)
                    .background(TacticalCardBg.copy(alpha = 0.95f), RoundedCornerShape(6.dp))
                    .border(1.dp, TacticalBorder, RoundedCornerShape(6.dp))
                    .testTag("zoom_in_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Zoom In",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            IconButton(
                onClick = { internalMapView?.controller?.zoomOut() },
                modifier = Modifier
                    .size(34.dp)
                    .background(TacticalCardBg.copy(alpha = 0.95f), RoundedCornerShape(6.dp))
                    .border(1.dp, TacticalBorder, RoundedCornerShape(6.dp))
                    .testTag("zoom_out_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Zoom Out",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
