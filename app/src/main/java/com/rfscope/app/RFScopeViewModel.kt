package com.rfscope.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rfscope.app.data.CellInfoReader
import com.rfscope.app.data.RfScopeUiState
import com.rfscope.app.data.WifiScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RFScopeViewModel(application: Application) : AndroidViewModel(application) {
    private val cellReader = CellInfoReader(application)
    private val wifiScanner = WifiScanner(application)

    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(RfScopeUiState())
    val uiState: kotlinx.coroutines.flow.StateFlow<RfScopeUiState> = _uiState

    private var pollJob: Job? = null

    init {
        startPolling()
    }

    /**
     * Foreground polling is intentional for the skeleton. Before release, gate
     * this loop with repeatOnLifecycle(STARTED) or a foreground service.
     */
    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                readCellularOnce()
                delay(2_000)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { readCellularOnce() }
    }

    fun scanWifi() {
        if (_uiState.value.wifiScanInProgress) return
        _uiState.value = _uiState.value.copy(wifiScanInProgress = true)
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) { wifiScanner.scan() }
            _uiState.value = _uiState.value.copy(
                wifi = outcome,
                wifiScanInProgress = false,
            )
        }
    }

    private suspend fun readCellularOnce() {
        val snapshot = withContext(Dispatchers.IO) { cellReader.read() }
        _uiState.value = _uiState.value.copy(
            cellular = snapshot,
            lastUpdatedEpochMs = System.currentTimeMillis(),
        )
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}
