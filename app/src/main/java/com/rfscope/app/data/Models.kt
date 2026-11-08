package com.rfscope.app.data

data class OperatorInfo(
    val code: String,
    val mcc: String,
    val mnc: String,
    val nameEn: String,
    val nameZh: String,
)

data class BandMatch(
    val name: String,
    val operatorCode: String?,
    val startMhz: Double,
    val endMhz: Double,
    val noteEn: String,
    val noteZh: String,
)

enum class CellReadStatus {
    WAITING,
    READY,
    PERMISSION_REQUIRED,
    LOCATION_DISABLED,
    NO_NR_CELL,
    ERROR,
}

data class CellularSnapshot(
    val operator: OperatorInfo? = null,
    val networkType: String = "UNKNOWN",
    val band: BandMatch? = null,
    val bands: List<Int> = emptyList(),
    val frequencyMhz: Double? = null,
    val nrarfcn: Int? = null,
    val pci: Int? = null,
    val tac: Int? = null,
    val nci: Long? = null,
    val rsrp: Int? = null,
    val rsrq: Int? = null,
    val sinr: Int? = null,
    val csiRsrp: Int? = null,
    val csiRsrq: Int? = null,
    val csiSinr: Int? = null,
    val signalLevel: Int? = null,
    val status: CellReadStatus = CellReadStatus.WAITING,
    val detailEn: String? = null,
    val detailZh: String? = null,
) {
    val interferenceRisk: Boolean
        get() = sinr != null && sinr < 0

    val mcc: String? get() = operator?.mcc
    val mnc: String? get() = operator?.mnc
}

enum class WifiScanStatus {
    IDLE,
    READY,
    PERMISSION_REQUIRED,
    LOCATION_DISABLED,
    THROTTLED,
    ERROR,
}

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val frequencyMhz: Int,
    val channel: Int?,
    val widthMhz: Int?,
    val levelDbm: Int,
    val band: String,
    val standard: String?,
    val hintEn: String,
    val hintZh: String,
)

data class WifiScanOutcome(
    val status: WifiScanStatus = WifiScanStatus.IDLE,
    val networks: List<WifiNetwork> = emptyList(),
    val detailEn: String? = null,
    val detailZh: String? = null,
)

/** One 2-second sample retained for the three live sparklines. */
data class SignalSample(
    val timestampMs: Long,
    val rsrp: Int?,
    val rsrq: Int?,
    val sinr: Int?,
)

data class UsbDeviceSummary(
    val deviceName: String,
    val vendorId: Int,
    val productId: Int,
    val manufacturer: String?,
    val product: String?,
    val hasSerialInterface: Boolean,
    val permissionGranted: Boolean,
    val likelyTinySa: Boolean,
)

data class UsbScanSummary(
    val hostSupported: Boolean = false,
    val devices: List<UsbDeviceSummary> = emptyList(),
)

enum class TinySaConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
}

data class TinySaSpectrum(
    val startHz: Long,
    val stopHz: Long,
    val points: Int,
    val frequenciesHz: DoubleArray,
    val powersDbm: DoubleArray,
    val peakIndex: Int,
    val peakHz: Double,
    val peakDbm: Double,
    val noiseFloorDbm: Double,
    val capturedAtMs: Long,
)

enum class TinySaAverageMode {
    OFF,
    AVER4,
    AVER16,
    MAX_HOLD,
    MIN_HOLD,
}

enum class TinySaToneMode {
    PEAK,
    AVERAGE,
    MARKER,
}

data class TinySaWaterfallRow(
    val timestampMs: Long,
    val powersDbm: FloatArray,
 val startHz: Long = 0L,
 val stopHz: Long = 0L,
)

data class TinySaMarker(
    val id: Int,
    val enabled: Boolean,
    val frequencyHz: Long,
    val autoPeak: Boolean = false,
)

data class TinySaUiState(
    val status: TinySaConnectionStatus = TinySaConnectionStatus.DISCONNECTED,
    val spectrum: TinySaSpectrum? = null,
    val waterfall: List<TinySaWaterfallRow> = emptyList(),
    val isScanning: Boolean = false,
    val isLive: Boolean = false,
    val isPaused: Boolean = false,
    val maxPeakDbm: Double? = null,
    val maxPeakHz: Long? = null,
    val maxPeakAtMs: Long? = null,
    val scanStartHz: Long = 400_000_000L,
    val scanStopHz: Long = 500_000_000L,
    val scanPoints: Int = 400,
    val rbwKhz: Int? = null,
    val averageMode: TinySaAverageMode = TinySaAverageMode.OFF,
    val toneMode: TinySaToneMode = TinySaToneMode.PEAK,
    val markers: List<TinySaMarker> = listOf(
        TinySaMarker(1, true, 450_000_000L),
        TinySaMarker(2, false, 425_000_000L),
        TinySaMarker(3, false, 475_000_000L),
        TinySaMarker(4, false, 500_000_000L),
    ),
    val activeMarkerId: Int = 1,
    val lastScanDurationMs: Long? = null,
    val isToneEnabled: Boolean = false,
    val toneFrequencyHz: Double = 440.0,
    val toneReferenceDbm: Double = -20.0,
    val thresholdEnabled: Boolean = false,
    val thresholdDbm: Double = -30.0,
 val spectrumScaleAuto: Boolean = true,
 val spectrumScaleMinDbm: Double = -120.0,
 val spectrumScaleMaxDbm: Double = -20.0,
    val dfMaxPeakDbm: Double? = null,
    val dfMaxPeakHz: Double? = null,
    val dfCaptureCount: Int = 0,
    val dfTargetNote: String = "",
    val messageEn: String? = null,
    val messageZh: String? = null,
)
data class RfScopeUiState(
    val cellular: CellularSnapshot = CellularSnapshot(),
    val wifi: WifiScanOutcome = WifiScanOutcome(),
    val usb: UsbScanSummary = UsbScanSummary(),
    val tinySa: TinySaUiState = TinySaUiState(),
    val signalHistory: List<SignalSample> = emptyList(),
    val wifiScanInProgress: Boolean = false,
    val isRefreshing: Boolean = false,
    val lastUpdatedEpochMs: Long? = null,
)
