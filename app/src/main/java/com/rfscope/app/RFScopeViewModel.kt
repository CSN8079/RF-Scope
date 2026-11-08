package com.rfscope.app

import android.app.Application
import android.app.PendingIntent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rfscope.app.data.CellInfoReader
import com.rfscope.app.data.CellularSnapshot
import com.rfscope.app.data.RfScopeUiState
import com.rfscope.app.data.SignalSample
import com.rfscope.app.data.TinySaConnectionStatus
import com.rfscope.app.data.WifiScanner
import com.rfscope.app.hardware.TinySaUsbTransport
import com.rfscope.app.hardware.UsbDeviceReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RFScopeViewModel(application: Application) : AndroidViewModel(application) {
    private val cellReader = CellInfoReader(application)
    private val wifiScanner = WifiScanner(application)
    private val usbReader = UsbDeviceReader(application)
    private val tinySaTransport = TinySaUsbTransport(application)

    private val _uiState = MutableStateFlow(RfScopeUiState())
    val uiState: StateFlow<RfScopeUiState> = _uiState

    private val signalHistory = ArrayDeque<SignalSample>()
    private var pollJob: Job? = null
    private var refreshJob: Job? = null

    init {
        startPolling()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                readCellularOnce()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        _uiState.update { it.copy(isRefreshing = true) }
        refreshJob = viewModelScope.launch {
            try {
                readCellularOnce()
                delay(MIN_REFRESH_FEEDBACK_MS)
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun onPermissionsChanged() {
        refresh()
        scanWifi()
    }

    fun requestUsbPermission(pendingIntent: PendingIntent): Boolean =
        usbReader.requestPermissionForLikelyTinySa(pendingIntent)

    fun connectTinySa() {
        if (_uiState.value.tinySa.isScanning ||
            _uiState.value.tinySa.status == TinySaConnectionStatus.CONNECTING
        ) return

        _uiState.update {
            it.copy(
                tinySa = it.tinySa.copy(
                    status = TinySaConnectionStatus.CONNECTING,
                    messageEn = "Opening TinySA CDC serial port...",
                    messageZh = "正在打开 TinySA CDC 串口...",
                ),
            )
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { tinySaTransport.connect() }
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        tinySa = it.tinySa.copy(
                            status = TinySaConnectionStatus.CONNECTED,
                            messageEn = "TinySA connected",
                            messageZh = "TinySA 已连接",
                        ),
                    )
                }
                scanTinySa()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        tinySa = it.tinySa.copy(
                            status = TinySaConnectionStatus.ERROR,
                            messageEn = error.message ?: "TinySA connection failed",
                            messageZh = "TinySA 连接失败",
                        ),
                    )
                }
            }
        }
    }

    fun scanTinySa() {
        if (_uiState.value.tinySa.isScanning) return
        if (_uiState.value.tinySa.status != TinySaConnectionStatus.CONNECTED) {
            connectTinySa()
            return
        }

        _uiState.update {
            it.copy(tinySa = it.tinySa.copy(isScanning = true))
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                tinySaTransport.scan(
                    startHz = TINYSA_DEFAULT_START_HZ,
                    stopHz = TINYSA_DEFAULT_STOP_HZ,
                    points = TINYSA_DEFAULT_POINTS,
                )
            }
            result.onSuccess { spectrum ->
                _uiState.update {
                    it.copy(
                        tinySa = it.tinySa.copy(
                            status = TinySaConnectionStatus.CONNECTED,
                            spectrum = spectrum,
                            isScanning = false,
                            messageEn = "Spectrum received",
                            messageZh = "频谱数据已读取",
                        ),
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        tinySa = it.tinySa.copy(
                            status = TinySaConnectionStatus.ERROR,
                            isScanning = false,
                            messageEn = error.message ?: "TinySA scan failed",
                            messageZh = "TinySA 扫描失败",
                        ),
                    )
                }
            }
        }
    }

    fun scanWifi() {
        if (_uiState.value.wifiScanInProgress) return
        _uiState.update { it.copy(wifiScanInProgress = true) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) { wifiScanner.scan() }
            _uiState.update {
                it.copy(
                    wifi = outcome,
                    wifiScanInProgress = false,
                )
            }
        }
    }

    private suspend fun readCellularOnce() {
        val snapshot = withContext(Dispatchers.IO) { cellReader.read() }
        val usb = withContext(Dispatchers.IO) { usbReader.scan() }
        val now = System.currentTimeMillis()
        appendHistory(snapshot, now)
        _uiState.update {
            it.copy(
                cellular = snapshot,
                signalHistory = signalHistory.toList(),
                usb = usb,
                lastUpdatedEpochMs = now,
            )
        }
    }

    private fun appendHistory(snapshot: CellularSnapshot, timestampMs: Long) {
        if (snapshot.status != com.rfscope.app.data.CellReadStatus.READY) return
        if (snapshot.rsrp == null && snapshot.rsrq == null && snapshot.sinr == null) return

        signalHistory.addLast(
            SignalSample(
                timestampMs = timestampMs,
                rsrp = snapshot.rsrp,
                rsrq = snapshot.rsrq,
                sinr = snapshot.sinr,
            ),
        )
        while (signalHistory.size > MAX_HISTORY_SAMPLES) {
            signalHistory.removeFirst()
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        refreshJob?.cancel()
        tinySaTransport.disconnect()
        super.onCleared()
    }

    private companion object {
        const val POLL_INTERVAL_MS = 2_000L
        const val MIN_REFRESH_FEEDBACK_MS = 350L
        const val MAX_HISTORY_SAMPLES = 60
        const val TINYSA_DEFAULT_START_HZ = 400_000_000L
        const val TINYSA_DEFAULT_STOP_HZ = 500_000_000L
        const val TINYSA_DEFAULT_POINTS = 200
    }
}
