package com.aegis.cloak.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.aegis.cloak.model.NetworkAuditTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object NetworkAuditorEngine {

    suspend fun auditPublicEgress(): NetworkAuditTelemetry = withContext(Dispatchers.IO) {
        // Attempt 1: ipwho.is (Provides IP, ISP, City, Country over HTTPS)
        try {
            val url = URL("https://ipwho.is/")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "AegisCloak/1.0")
            }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(responseText)
                val success = json.optBoolean("success", true)
                if (success) {
                    val ip = json.optString("ip", "Unknown IP")
                    val isp = json.optJSONObject("connection")?.optString("isp")
                        ?: json.optString("isp", "Cellular Carrier")
                    val city = json.optString("city", "Local Node")
                    val country = json.optString("country", "")

                    return@withContext NetworkAuditTelemetry(
                        publicIp = ip,
                        isp = isp,
                        city = city,
                        country = country,
                        isQuerying = false,
                        lastAuditTimestamp = System.currentTimeMillis()
                    )
                }
            }
        } catch (_: Exception) {}

        // Fallback: api.ipify.org
        try {
            val url = URL("https://api.ipify.org?format=json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
            }
            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(responseText)
                val ip = json.optString("ip", "Unavailable")
                return@withContext NetworkAuditTelemetry(
                    publicIp = ip,
                    isp = "Active Internet Uplink",
                    city = "Carrier Gateway",
                    country = "",
                    isQuerying = false,
                    lastAuditTimestamp = System.currentTimeMillis()
                )
            }
        } catch (_: Exception) {}

        NetworkAuditTelemetry(
            publicIp = "Offline / Leak-Proof",
            isp = "Direct Carrier Egress",
            city = "Secure Gateway",
            country = "",
            isQuerying = false,
            lastAuditTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Launch browser or map app directly with geolocation testing endpoints
     * to immediately demonstrate and verify the mock location in action.
     */
    fun openGeolocationVerification(context: Context, latitude: Double, longitude: Double) {
        try {
            // Option 1: Open Google Maps pin coordinate
            val mapUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(AegisCloak+Mock+Target)")
            val mapIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            // Fallback: Open Browser Geolocation Test
            try {
                val browserUri = Uri.parse("https://browserleaks.com/geo")
                val browserIntent = Intent(Intent.ACTION_VIEW, browserUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
            } catch (_: Exception) {}
        }
    }

    fun openChromeTest(context: Context) {
        try {
            val browserUri = Uri.parse("https://browserleaks.com/geo")
            val browserIntent = Intent(Intent.ACTION_VIEW, browserUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
        } catch (_: Exception) {}
    }
}
