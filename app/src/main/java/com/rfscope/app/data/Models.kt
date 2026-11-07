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
    val frequencyMhz: Double? = null,
    val nrarfcn: Int? = null,
    val pci: Int? = null,
    val tac: Int? = null,
    val rsrp: Int? = null,
    val rsrq: Int? = null,
    val sinr: Int? = null,
    val status: CellReadStatus = CellReadStatus.WAITING,
    val detailEn: String? = null,
    val detailZh: String? = null,
) {
    val interferenceRisk: Boolean
        get() = sinr != null && sinr < 0
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
    val hintEn: String,
    val hintZh: String,
)

data class WifiScanOutcome(
    val status: WifiScanStatus = WifiScanStatus.IDLE,
    val networks: List<WifiNetwork> = emptyList(),
    val detailEn: String? = null,
    val detailZh: String? = null,
)

data class RfScopeUiState(
    val cellular: CellularSnapshot = CellularSnapshot(),
    val wifi: WifiScanOutcome = WifiScanOutcome(),
    val wifiScanInProgress: Boolean = false,
    val lastUpdatedEpochMs: Long? = null,
)

