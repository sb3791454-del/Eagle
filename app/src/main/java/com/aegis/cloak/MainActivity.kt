package com.aegis.cloak

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.aegis.cloak.data.AegisDatabase
import com.aegis.cloak.data.TacticalRepository
import com.aegis.cloak.ui.TacticalHudScreen
import com.aegis.cloak.ui.TacticalViewModel
import com.aegis.cloak.ui.TacticalViewModelFactory
import com.aegis.cloak.ui.theme.AegisCloakTheme

class MainActivity : ComponentActivity() {

    private val repository by lazy {
        val database = AegisDatabase.getInstance(applicationContext)
        TacticalRepository(database)
    }

    private val viewModel: TacticalViewModel by viewModels {
        TacticalViewModelFactory(repository)
    }

    private val vpnPrepareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // VPN permission result
    }

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.checkDeveloperMockPermission(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestRequiredTacticalPermissions()
        prepareVpnInterface()

        setContent {
            AegisCloakTheme {
                TacticalHudScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkDeveloperMockPermission(this)
    }

    private fun requestRequiredTacticalPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            permissions.add(Manifest.permission.BODY_SENSORS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        permissionsLauncher.launch(permissions.toTypedArray())
    }

    private fun prepareVpnInterface() {
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            vpnPrepareLauncher.launch(prepareIntent)
        }
    }
}
