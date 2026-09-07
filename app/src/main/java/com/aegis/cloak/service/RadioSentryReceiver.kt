package com.aegis.cloak.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.telephony.TelephonyManager

class RadioSentryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            ConnectivityManager.CONNECTIVITY_ACTION,
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                // Radio state transition triggered
                val sentryManager = RadioSentryManager(context.applicationContext)
                sentryManager.startMonitoring()
            }
        }
    }
}
