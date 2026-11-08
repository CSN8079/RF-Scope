package com.rfscope.app

import android.app.Application
import android.app.PendingIntent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rfscope.app.data.CellInfoReader
import com.rfscope.app.data.CellularSnapshot
import com.rfscope.app.data.RfScopeUiState
import com.rfscope.app.data.SignalSample
import com.rfscope.app.data.SignalMetrics
import com.rfscope.app.data.SignalStrengthMonitor
import com.rfscope.app.data.TinySaConnectionStatus
import com.rfscope.app.data.TinySaAverageMode
import com.rfscope.app.data.TinySaToneMode
import com.rfscope.app.data.TinySaSpectrum
import com.rfscope.app.data.TinySaUiState
import com.rfscope.app.data.TinySaWaterfallRow
import com.rfscope.app.data.WifiScanner
import com.rfscope.app.hardware.TinySaUsbTransport
import com.rfscope.app.hardware.SignalTonePlayer
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
import kotlin.math.abs

class RFScopeViewModel(application: Application) : AndroidViewModel(application) {
    private val cellReader = CellInfoReader(application)
    private val wifiScanner = WifiScanner(application)
    private val usbReader = UsbDeviceReader(application)
    private val tinySaTransport = TinySaUsbTransport(application)
    private val signalMonitor = SignalStrengthMonitor(application)
    private val tonePlayer = SignalTonePlayer()

    private val _uiState = MutableStateFlow(RfScopeUiState())
    val uiState: StateFlow<RfScopeUiState> = _uiState

    private val signalHistory = ArrayDeque<SignalSample>()
    private var pollJob: Job? = null
    private var refreshJob: Job? = null
    private var tinySaLiveJob: Job? = null
    private var lastSignalSampleMs = 0L

    init {
        startPolling()
        startSignalMonitor()
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
        startSignalMonitor()
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

    fun setTinySaScanRange(startHz: Long, stopHz: Long, points: Int) {
        if (stopHz <= startHz) return
        val safePoints = points.coerceIn(50, 1000)
        _uiState.update {
            it.copy(
                tinySa = it.tinySa.copy(
                    scanStartHz = startHz,
                    scanStopHz = stopHz,
                    scanPoints = safePoints,
                    spectrum = null,
                    waterfall = emptyList(),
                    lastScanDurationMs = null,
                    messageEn = "Scan range updated",
                    messageZh = "扫描范围已更新",
                ),
            )
        }
    }

    fun startTinySaLive() {
        if (_uiState.value.tinySa.isPaused) {
            resumeTinySa()
            return
        }
        if (_uiState.value.tinySa.isLive) return
        if (_uiState.value.tinySa.status != TinySaConnectionStatus.CONNECTED) {
            connectTinySa()
            return
        }
        tinySaLiveJob?.cancel()
        _uiState.update {
            it.copy(
                tinySa = it.tinySa.copy(
                    isLive = true,
                    isPaused = false,
                    isScanning = true,
                    messageEn = "Live spectrum scanning",
                    messageZh = "实时频谱扫描中",
                ),
            )
        }
        tinySaLiveJob = viewModelScope.launch {
            while (isActive) {
                val current = _uiState.value.tinySa
                val startedAt = System.currentTimeMillis()
                val result = withContext(Dispatchers.IO) {
                    tinySaTransport.scan(
                        startHz = current.scanStartHz,
                        stopHz = current.scanStopHz,
                        points = current.scanPoints,
                     rbwKhz = current.rbwKhz,
                     averageMode = current.averageMode,
                    )
                }
                result.onSuccess { spectrum ->
                    if (!isActive || _uiState.value.tinySa.isPaused) return@onSuccess
                    val duration = System.currentTimeMillis() - startedAt
                    val toneState = _uiState.value.tinySa
                    val toneLevel = toneLevelFor(spectrum, toneState)
                    val toneFrequency = SignalTonePlayer.frequencyForLevel(toneLevel, toneState.toneReferenceDbm)
                    if (toneState.isToneEnabled) {
                        tonePlayer.updateLevel(toneLevel, toneState.toneReferenceDbm)
                    }
                    _uiState.update { state ->
                        val frames = (state.tinySa.waterfall + TinySaWaterfallRow(spectrum.capturedAtMs, FloatArray(spectrum.powersDbm.size) { spectrum.powersDbm[it].toFloat() }, startHz = spectrum.startHz, stopHz = spectrum.stopHz))
                            .takeLast(MAX_WATERFALL_FRAMES)
                        state.copy(
                            tinySa = state.tinySa.copy(
                                status = TinySaConnectionStatus.CONNECTED,
                                spectrum = spectrum,
                                waterfall = frames,
                                isScanning = true,
                                isLive = true,
                                isPaused = false,
                                lastScanDurationMs = duration,
                                toneFrequencyHz = toneFrequency,
                                messageEn = "Live spectrum updated",
                                messageZh = "实时频谱已更新",
                            ),
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            tinySa = it.tinySa.copy(
                                status = TinySaConnectionStatus.CONNECTED,
                                isScanning = false,
                                isLive = false,
                                messageEn = "Connected, live scan failed: ${error.message ?: "unknown error"}",
                                messageZh = "已连接，但实时扫描失败",
                            ),
                        )
                    }
                }
                if (_uiState.value.tinySa.status == TinySaConnectionStatus.ERROR) break
                delay(LIVE_SCAN_GAP_MS)
            }
        }
    }

    fun toggleTinySaPause() {
        val current = _uiState.value.tinySa
        if (current.isPaused) {
            resumeTinySa()
        } else {
            pauseTinySa()
        }
    }

    private fun pauseTinySa() {
        val current = _uiState.value.tinySa
        if (!current.isLive || current.isPaused) return
        tinySaLiveJob?.cancel()
        tinySaLiveJob = null
        _uiState.update {
            it.copy(tinySa = it.tinySa.copy(
                isPaused = true,
                isScanning = false,
                messageEn = "Spectrum paused",
                messageZh = "频谱已暂停",
            ))
        }
        viewModelScope.launch {
            delay(120)
            withContext(Dispatchers.IO) { tinySaTransport.abort() }
        }
    }

    private fun resumeTinySa() {
        if (!_uiState.value.tinySa.isPaused) return
        _uiState.update {
            it.copy(tinySa = it.tinySa.copy(
                isPaused = false,
                isLive = false,
                isScanning = false,
                messageEn = "Resuming live spectrum...",
                messageZh = "正在恢复实时频谱...",
            ))
        }
        startTinySaLive()
    }

    fun resetMaxPeak() {
        _uiState.update {
            it.copy(tinySa = it.tinySa.copy(
                maxPeakDbm = null,
                maxPeakHz = null,
                maxPeakAtMs = null,
            ))
        }
    }
    fun stopTinySaLive() {
        tinySaLiveJob?.cancel()
        tinySaLiveJob = null
        _uiState.update {
            it.copy(
                tinySa = it.tinySa.copy(
                    isLive = false,
                    isPaused = false,
                    isScanning = false,
                    messageEn = "Live scan stopped",
                    messageZh = "实时扫描已停止",
                ),
            )
        }
        viewModelScope.launch {
            delay(120)
            withContext(Dispatchers.IO) { tinySaTransport.abort() }
        }
    }

    fun toggleTinySaTone() {
        val enabled = !_uiState.value.tinySa.isToneEnabled
        if (enabled) {
            tonePlayer.start()
            val current = _uiState.value.tinySa
            current.spectrum?.let { spectrum ->
                val level = toneLevelFor(spectrum, current)
                tonePlayer.updateLevel(level, current.toneReferenceDbm)
            }
        } else {
            tonePlayer.stop()
        }
        _uiState.update {
            it.copy(tinySa = it.tinySa.copy(isToneEnabled = enabled))
        }
    }

    fun scanTinySa() {
        if (_uiState.value.tinySa.isScanning || _uiState.value.tinySa.isLive) return
        if (_uiState.value.tinySa.status != TinySaConnectionStatus.CONNECTED) {
            connectTinySa()
            return
        }

        val current = _uiState.value.tinySa
        _uiState.update { it.copy(tinySa = it.tinySa.copy(isScanning = true)) }
        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            val result = withContext(Dispatchers.IO) {
                tinySaTransport.scan(
                    startHz = current.scanStartHz,
                    stopHz = current.scanStopHz,
                    points = current.scanPoints,
                 rbwKhz = current.rbwKhz,
                 averageMode = current.averageMode,
                )
            }
            result.onSuccess { spectrum ->
                val duration = System.currentTimeMillis() - startedAt
                val toneState = _uiState.value.tinySa
                val toneLevel = toneLevelFor(spectrum, toneState)
                val toneFrequency = SignalTonePlayer.frequencyForLevel(toneLevel, toneState.toneReferenceDbm)
                if (toneState.isToneEnabled) {
                    tonePlayer.updateLevel(toneLevel, toneState.toneReferenceDbm)
                }
                _uiState.update { state ->
                        val frames = (state.tinySa.waterfall + TinySaWaterfallRow(spectrum.capturedAtMs, FloatArray(spectrum.powersDbm.size) { spectrum.powersDbm[it].toFloat() }, startHz = spectrum.startHz, stopHz = spectrum.stopHz))
                        .takeLast(MAX_WATERFALL_FRAMES)
                    state.copy(
                        tinySa = state.tinySa.withMaxPeak(spectrum).copy(
                            status = TinySaConnectionStatus.CONNECTED,
                            spectrum = spectrum,
                            waterfall = frames,
                            isScanning = false,
                            lastScanDurationMs = duration,
                            toneFrequencyHz = toneFrequency,
                            messageEn = "Spectrum received",
                            messageZh = "频谱数据已读取",
                        ),
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        tinySa = it.tinySa.copy(
                            status = TinySaConnectionStatus.CONNECTED,
                            isScanning = false,
                            messageEn = "Connected, scan failed: ${error.message ?: "unknown error"}",
                            messageZh = "已连接，但扫描失败",
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

    private fun startSignalMonitor() {
        signalMonitor.start { metrics ->
            val now = System.currentTimeMillis()
            _uiState.update { state ->
                val current = state.cellular
                val updated = current.copy(
                    rsrp = metrics.rsrp ?: current.rsrp,
                    rsrq = metrics.rsrq ?: current.rsrq,
                    sinr = metrics.sinr ?: current.sinr,
                )
                appendHistory(updated, now)
                state.copy(
                    cellular = updated,
                    signalHistory = signalHistory.toList(),
                    lastUpdatedEpochMs = now,
                )
            }
        }
    }

    private fun appendHistory(snapshot: CellularSnapshot, timestampMs: Long) {
        val hasMeasuredData = snapshot.status == com.rfscope.app.data.CellReadStatus.READY &&
            (snapshot.rsrp != null || snapshot.rsrq != null || snapshot.sinr != null)
        signalHistory.addLast(
            SignalSample(
                timestampMs = timestampMs,
                rsrp = if (hasMeasuredData) snapshot.rsrp else null,
                rsrq = if (hasMeasuredData) snapshot.rsrq else null,
                sinr = if (hasMeasuredData) snapshot.sinr else null,
            ),
        )
        while (signalHistory.size > MAX_HISTORY_SAMPLES) {
            signalHistory.removeFirst()
        }
    }

    fun setTinySaRbw(khz: Int?) {
        _uiState.update { it.copy(tinySa = it.tinySa.copy(rbwKhz = khz)) }
    }

    fun setTinySaAverage(mode: TinySaAverageMode) {
        _uiState.update { it.copy(tinySa = it.tinySa.copy(averageMode = mode)) }
    }

    fun setTinySaToneMode(mode: TinySaToneMode) {
        _uiState.update { it.copy(tinySa = it.tinySa.copy(toneMode = mode)) }
        refreshToneLevel()
    }

    fun setTinySaToneReference(dbm: Double) {
        _uiState.update { it.copy(tinySa = it.tinySa.copy(toneReferenceDbm = dbm.coerceIn(-100.0, -10.0))) }
        refreshToneLevel()
    }

    fun setThresholdEnabled(enabled: Boolean) {
        _uiState.update { it.copy(tinySa = it.tinySa.copy(thresholdEnabled = enabled)) }
    }

    fun setThresholdDbm(dbm: Double) {
        _uiState.update { it.copy(tinySa = it.tinySa.copy(thresholdDbm = dbm.coerceIn(-120.0, 0.0))) }
    }

    fun setSpectrumScaleAuto(enabled: Boolean) {
 _uiState.update { it.copy(tinySa = it.tinySa.copy(spectrumScaleAuto = enabled)) }
 }

 fun setSpectrumScaleMin(dbm: Double) {
 _uiState.update { state -> val max = state.tinySa.spectrumScaleMaxDbm
 state.copy(tinySa = state.tinySa.copy(spectrumScaleAuto = false, spectrumScaleMinDbm = dbm.coerceIn(-150.0, max - 10.0)))
 }
 }

 fun setSpectrumScaleMax(dbm: Double) {
 _uiState.update { state -> val min = state.tinySa.spectrumScaleMinDbm
 state.copy(tinySa = state.tinySa.copy(spectrumScaleAuto = false, spectrumScaleMaxDbm = dbm.coerceIn(min + 10.0, 10.0)))
 }
 }

 fun toggleTinySaMarker(id: Int) {
        _uiState.update { state ->
            val marker = state.tinySa.markers.firstOrNull { it.id == id } ?: return@update state
            val enabled = !marker.enabled
            state.copy(
                tinySa = state.tinySa.copy(
                    markers = state.tinySa.markers.map {
                        if (it.id == id) it.copy(enabled = enabled) else it
                    },
                    activeMarkerId = if (enabled) id else state.tinySa.activeMarkerId,
                ),
            )
        }
        refreshToneLevel()
    }

    fun setTinySaMarkerFrequency(id: Int, frequencyHz: Long) {
        _uiState.update { state ->
            state.copy(
                tinySa = state.tinySa.copy(
                    markers = state.tinySa.markers.map {
                        if (it.id == id) it.copy(enabled = true, frequencyHz = frequencyHz) else it
                    },
                    activeMarkerId = id,
                ),
            )
        }
        refreshToneLevel()
    }

    fun setTinySaMarkerAutoPeak(id: Int, autoPeak: Boolean) {
        _uiState.update { state ->
            state.copy(
                tinySa = state.tinySa.copy(
                    markers = state.tinySa.markers.map {
                        if (it.id == id) it.copy(enabled = true, autoPeak = autoPeak) else it
                    },
                    activeMarkerId = id,
                ),
            )
        }
        refreshToneLevel()
    }

    fun setActiveTinySaMarker(id: Int) {
        _uiState.update { it.copy(tinySa = it.tinySa.copy(activeMarkerId = id)) }
    }

    fun captureDfMax() {
        val spectrum = _uiState.value.tinySa.spectrum ?: return
        _uiState.update {
            it.copy(tinySa = it.tinySa.copy(
                dfMaxPeakDbm = spectrum.peakDbm,
                dfMaxPeakHz = spectrum.peakHz,
                dfCaptureCount = it.tinySa.dfCaptureCount + 1,
            ))
        }
    }

    fun resetDfMax() {
        _uiState.update {
            it.copy(tinySa = it.tinySa.copy(
                dfMaxPeakDbm = null,
                dfMaxPeakHz = null,
                dfCaptureCount = 0,
            ))
        }
    }

    fun setDfTargetNote(note: String) {
        _uiState.update { it.copy(tinySa = it.tinySa.copy(dfTargetNote = note.take(80))) }
    }

private fun TinySaUiState.withMaxPeak(spectrum: TinySaSpectrum): TinySaUiState {
    val previous = maxPeakDbm
    return if (previous == null || spectrum.peakDbm > previous) {
        copy(
            maxPeakDbm = spectrum.peakDbm,
            maxPeakHz = spectrum.peakHz.toLong(),
            maxPeakAtMs = spectrum.capturedAtMs,
        )
    } else {
        this
    }
}
    private fun toneLevelFor(spectrum: TinySaSpectrum, state: TinySaUiState): Double {
        return when (state.toneMode) {
            TinySaToneMode.PEAK -> spectrum.peakDbm
            TinySaToneMode.AVERAGE -> spectrum.powersDbm.average()
            TinySaToneMode.MARKER -> {
                val marker = state.markers.firstOrNull { it.id == state.activeMarkerId && it.enabled }
                    ?: return spectrum.peakDbm
                val index = spectrum.frequenciesHz.indices.minByOrNull {
                    abs(spectrum.frequenciesHz[it] - marker.frequencyHz)
                } ?: return spectrum.peakDbm
                spectrum.powersDbm[index]
            }
        }
    }

    private fun refreshToneLevel() {
        val state = _uiState.value.tinySa
        val spectrum = state.spectrum ?: return
        val level = toneLevelFor(spectrum, state)
        if (state.isToneEnabled) tonePlayer.updateLevel(level, state.toneReferenceDbm)
        _uiState.update {
            it.copy(tinySa = it.tinySa.copy(
                toneFrequencyHz = SignalTonePlayer.frequencyForLevel(level, state.toneReferenceDbm),
            ))
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        refreshJob?.cancel()
        tinySaTransport.disconnect()
        signalMonitor.stop()
        tinySaLiveJob?.cancel()
        tonePlayer.release()
        super.onCleared()
    }

    private companion object {
        const val POLL_INTERVAL_MS = 1_000L
        const val MIN_REFRESH_FEEDBACK_MS = 350L
        const val MAX_HISTORY_SAMPLES = 60
        const val TINYSA_DEFAULT_START_HZ = 400_000_000L
        const val TINYSA_DEFAULT_STOP_HZ = 500_000_000L
        const val TINYSA_DEFAULT_POINTS = 200
        const val MAX_WATERFALL_FRAMES = 240
        const val LIVE_SCAN_GAP_MS = 180L
    }
}




