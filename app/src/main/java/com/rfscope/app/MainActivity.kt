package com.rfscope.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rfscope.app.ui.RFScopeScreen
import com.rfscope.app.ui.theme.RFScopeTheme

class MainActivity : ComponentActivity() {
    private val viewModel: RFScopeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RFScopeTheme {
                val state = viewModel.uiState.collectAsStateWithLifecycle().value
                RFScopeScreen(
                    state = state,
                    onRequestPermissions = ::requestScopePermissions,
                    onRefresh = viewModel::refresh,
                    onScanWifi = viewModel::scanWifi,
                )
            }
        }
    }

    private fun requestScopePermissions() {
        val missing = requiredPermissions().filterNot { permission ->
            ContextCompat.checkSelfPermission(this, permission) ==
                PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            viewModel.refresh()
            return
        }
        ActivityCompat.requestPermissions(
            this,
            missing.toTypedArray(),
            PERMISSION_REQUEST_CODE,
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            viewModel.refresh()
        }
    }

    private fun requiredPermissions(): List<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    }

    private companion object {
        const val PERMISSION_REQUEST_CODE = 1001
    }
}

