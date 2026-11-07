package com.rfscope.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rfscope.app.ui.RFScopeScreen
import com.rfscope.app.ui.theme.RFScopeTheme

class MainActivity : ComponentActivity() {
    private val viewModel: RFScopeViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.onPermissionsChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RFScopeTheme {
                val state = viewModel.uiState.collectAsStateWithLifecycle().value
                RFScopeScreen(
                    state = state,
                    onRequestCellularPermissions = {
                        requestPermissions(cellularPermissions())
                    },
                    onRequestWifiPermissions = {
                        requestPermissions(wifiPermissions())
                    },
                    onRefresh = viewModel::refresh,
                    onScanWifi = viewModel::scanWifi,
                    onOpenAppSettings = ::openAppSettings,
                    onOpenLocationSettings = ::openLocationSettings,
                )
            }
        }
        window.decorView.post { requestInitialPermissions() }
    }

    private fun requestInitialPermissions() {
        val preferences = getSharedPreferences(PERMISSION_PREFERENCES, MODE_PRIVATE)
        if (preferences.getBoolean(KEY_INITIAL_PERMISSION_ASKED, false)) return

        preferences.edit().putBoolean(KEY_INITIAL_PERMISSION_ASKED, true).apply()
        requestPermissions(allPermissions())
    }

    private fun requestPermissions(permissions: List<String>) {
        val missing = permissions
            .distinct()
            .filterNot(::isGranted)
        if (missing.isEmpty()) {
            viewModel.onPermissionsChanged()
            return
        }
        permissionLauncher.launch(missing.toTypedArray())
    }

    private fun cellularPermissions(): List<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.READ_PHONE_STATE)
    }

    private fun wifiPermissions(): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    private fun allPermissions(): List<String> =
        (cellularPermissions() + wifiPermissions()).distinct()

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }
    }

    private fun openLocationSettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    private companion object {
        const val PERMISSION_PREFERENCES = "rfscope_permissions"
        const val KEY_INITIAL_PERMISSION_ASKED = "initial_permission_asked"
    }
}
