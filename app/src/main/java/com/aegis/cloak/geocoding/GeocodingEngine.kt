package com.aegis.cloak.geocoding

import android.util.Log
import com.aegis.cloak.model.SearchPlaceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Geocoding & Place Search Engine for Project AegisCloak.
 * Queries OpenStreetMap Nominatim with forced English localization headers (`accept-language: en`).
 * Implements non-blocking Coroutine execution with network timeout resiliency.
 */
object GeocodingEngine {
    private const val TAG = "GeocodingEngine"
    private const val NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/search"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Search global places, landmarks, coordinates, or addresses in English.
     */
    suspend fun searchPlaces(query: String): List<SearchPlaceResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        try {
            val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
            val url = "$NOMINATIM_BASE_URL?q=$encodedQuery&format=json&accept-language=en&addressdetails=1&limit=7"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ProjectAegisCloakTacticalMap/1.0 (Android; English-Loc)")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Nominatim returned HTTP error code: ${response.code}")
                return@withContext getLocalFallbackResults(trimmed)
            }

            val responseBody = response.body?.string() ?: return@withContext emptyList()
            val jsonArray = JSONArray(responseBody)
            val parsedList = mutableListOf<SearchPlaceResult>()

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val fullDisplayName = item.optString("display_name", "")
                val lat = item.optDouble("lat", Double.NaN)
                val lon = item.optDouble("lon", Double.NaN)
                val placeId = item.optLong("place_id", i.toLong())
                val type = item.optString("type", "location").uppercase()

                if (!lat.isNaN() && !lon.isNaN() && fullDisplayName.isNotBlank()) {
                    // Extract clean title and localized subtitle
                    val segments = fullDisplayName.split(",").map { it.trim() }
                    val title = segments.firstOrNull() ?: fullDisplayName
                    val subtitle = if (segments.size > 1) {
                        segments.drop(1).take(4).joinToString(", ")
                    } else {
                        "Coordinates: %.4f, %.4f".format(lat, lon)
                    }

                    parsedList.add(
                        SearchPlaceResult(
                            placeId = placeId,
                            displayName = fullDisplayName,
                            title = title,
                            subtitle = subtitle,
                            latitude = lat,
                            longitude = lon,
                            type = type
                        )
                    )
                }
            }

            if (parsedList.isEmpty()) {
                getLocalFallbackResults(trimmed)
            } else {
                parsedList
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network search error occurred: ${e.message}")
            getLocalFallbackResults(trimmed)
        }
    }

    /**
     * Tactical offline fallback database for prominent global landmarks
     * ensures immediate response even in disconnected / flight-mode simulations.
     */
    private fun getLocalFallbackResults(query: String): List<SearchPlaceResult> {
        val q = query.lowercase()
        val tacticalCatalog = listOf(
            SearchPlaceResult(
                101, "Shibuya Crossing, Shibuya, Tokyo, Japan",
                "Shibuya Crossing", "Shibuya, Tokyo, Japan",
                35.6595, 139.7005, "LANDMARK"
            ),
            SearchPlaceResult(
                102, "Times Square, Manhattan, New York, United States",
                "Times Square", "Manhattan, New York, United States",
                40.7580, -73.9855, "LANDMARK"
            ),
            SearchPlaceResult(
                103, "Eiffel Tower, Champ de Mars, Paris, France",
                "Eiffel Tower", "Champ de Mars, Paris, France",
                48.8584, 2.2945, "LANDMARK"
            ),
            SearchPlaceResult(
                104, "Brandenburg Gate, Mitte, Berlin, Germany",
                "Brandenburg Gate", "Mitte, Berlin, Germany",
                52.5163, 13.3777, "HISTORIC"
            ),
            SearchPlaceResult(
                105, "Berlin Brandenburg Airport (BER), Schönefeld, Germany",
                "Berlin Airport (BER)", "Schönefeld, Brandenburg, Germany",
                52.3667, 13.5033, "AERODROME"
            ),
            SearchPlaceResult(
                106, "Marina Bay Sands, 10 Bayfront Ave, Singapore",
                "Marina Bay Sands", "Downtown Core, Singapore",
                1.2834, 103.8607, "RESORT"
            ),
            SearchPlaceResult(
                107, "Big Ben, Westminster, London, United Kingdom",
                "Big Ben", "Westminster, London, United Kingdom",
                51.5007, -0.1246, "MONUMENT"
            ),
            SearchPlaceResult(
                108, "Sydney Opera House, Sydney, Australia",
                "Sydney Opera House", "Bennelong Point, Sydney, Australia",
                -33.8568, 151.2153, "ARTS_CENTRE"
            ),
            SearchPlaceResult(
                109, "Seoul Station, Yongsan-gu, Seoul, South Korea",
                "Seoul Station", "Yongsan-gu, Seoul, South Korea",
                37.5547, 126.9707, "STATION"
            ),
            SearchPlaceResult(
                110, "Burj Khalifa, 1 Sheikh Mohammed bin Rashid Blvd, Dubai, UAE",
                "Burj Khalifa", "Downtown Dubai, United Arab Emirates",
                25.1972, 55.2744, "SKYSCRAPER"
            )
        )

        return tacticalCatalog.filter {
            it.title.lowercase().contains(q) ||
            it.subtitle.lowercase().contains(q) ||
            it.displayName.lowercase().contains(q)
        }
    }
}
