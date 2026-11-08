package com.rfscope.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rfscope.app.data.UsbDeviceSummary
import com.rfscope.app.data.UsbScanSummary
import com.rfscope.app.hardware.AntennaType
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rfscope.app.data.BandMatch
import com.rfscope.app.data.CellReadStatus
import com.rfscope.app.data.CellularSnapshot
import com.rfscope.app.data.OperatorInfo
import com.rfscope.app.data.RfScopeUiState
import com.rfscope.app.data.SignalSample
import com.rfscope.app.data.WifiNetwork
import com.rfscope.app.data.WifiScanOutcome
import com.rfscope.app.data.WifiScanStatus
import com.rfscope.app.data.TinySaConnectionStatus
import com.rfscope.app.data.TinySaAverageMode
import com.rfscope.app.data.TinySaToneMode
import com.rfscope.app.data.TinySaUiState
import com.rfscope.app.ui.theme.RFScopeTheme
import com.rfscope.app.ui.theme.ScopeAmber
import com.rfscope.app.ui.theme.ScopeBlack
import com.rfscope.app.ui.theme.ScopeCyan
import com.rfscope.app.ui.theme.ScopeGreen
import com.rfscope.app.ui.theme.ScopeGreenDim
import com.rfscope.app.ui.theme.ScopeGrid
import com.rfscope.app.ui.theme.ScopeLine
import com.rfscope.app.ui.theme.ScopeMuted
import com.rfscope.app.ui.theme.ScopePanel
import com.rfscope.app.ui.theme.ScopePanelAlt
import com.rfscope.app.ui.theme.ScopeRed
import com.rfscope.app.ui.theme.ScopeWhite
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun RFScopeScreen(
    state: RfScopeUiState,
    onRequestCellularPermissions: () -> Unit,
    onRequestWifiPermissions: () -> Unit,
    onRequestUsbPermission: () -> Unit,
    onConnectTinySa: () -> Unit,
    onScanTinySa: () -> Unit,
    onStartTinySaLive: () -> Unit,
    onStopTinySaLive: () -> Unit,
    onToggleTinySaPause: () -> Unit,
    onToggleTinySaTone: () -> Unit,
    onSetTinySaRange: (Long, Long, Int) -> Unit,
    onSetTinySaRbw: (Int?) -> Unit,
    onSetTinySaAverage: (TinySaAverageMode) -> Unit,
    onSetTinySaToneMode: (TinySaToneMode) -> Unit,
    onSetTinySaToneReference: (Double) -> Unit,
    onSetThresholdEnabled: (Boolean) -> Unit,
    onSetThresholdDbm: (Double) -> Unit,
 onSetSpectrumScaleAuto: (Boolean) -> Unit,
 onSetSpectrumScaleMin: (Double) -> Unit,
 onSetSpectrumScaleMax: (Double) -> Unit,
    onToggleTinySaMarker: (Int) -> Unit,
    onSetTinySaMarkerFrequency: (Int, Long) -> Unit,
    onSetTinySaMarkerAutoPeak: (Int, Boolean) -> Unit,
    onSetActiveTinySaMarker: (Int) -> Unit,
    onResetMaxPeak: () -> Unit,
    onRefresh: () -> Unit,
    onScanWifi: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pageName by rememberSaveable { mutableStateOf(RfScopePage.LIVE.name) }
    var antennaName by rememberSaveable { mutableStateOf(AntennaType.HT8_DIRECTIONAL.name) }
    val page = RfScopePage.valueOf(pageName)
    val selectedAntenna = AntennaType.valueOf(antennaName)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScopeBlack)
            .systemBarsPadding(),
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Header(state.lastUpdatedEpochMs, state.isRefreshing)
        }
        Box(modifier = Modifier.weight(1f)) {
            when (page) {
                RfScopePage.LIVE -> LivePage(
                    state = state,
                    onRequestCellularPermissions = onRequestCellularPermissions,
                    onRefresh = onRefresh,
                    onOpenAppSettings = onOpenAppSettings,
                    onOpenLocationSettings = onOpenLocationSettings,
                )

                RfScopePage.WIFI -> WifiPage(
                    state = state,
                    onScanWifi = onScanWifi,
                    onRequestWifiPermissions = onRequestWifiPermissions,
                    onOpenAppSettings = onOpenAppSettings,
                    onOpenLocationSettings = onOpenLocationSettings,
                )

                RfScopePage.HARDWARE -> HardwarePage(
                    usb = state.usb,
                    tinySa = state.tinySa,
                    selectedAntenna = selectedAntenna,
                    onAntennaSelected = { antennaName = it.name },
                    onRequestUsbPermission = onRequestUsbPermission,
                    onConnectTinySa = onConnectTinySa,
                    onScanTinySa = onScanTinySa,
                    onStartTinySaLive = onStartTinySaLive,
                    onStopTinySaLive = onStopTinySaLive,
                    onToggleTinySaPause = onToggleTinySaPause,
                    onToggleTinySaTone = onToggleTinySaTone,
                    onSetTinySaRange = onSetTinySaRange,
                    onSetTinySaRbw = onSetTinySaRbw,
                    onSetTinySaAverage = onSetTinySaAverage,
                    onSetTinySaToneMode = onSetTinySaToneMode,
                    onSetTinySaToneReference = onSetTinySaToneReference,
                    onSetThresholdEnabled = onSetThresholdEnabled,
                    onSetThresholdDbm = onSetThresholdDbm,
 onSetSpectrumScaleAuto = onSetSpectrumScaleAuto,
 onSetSpectrumScaleMin = onSetSpectrumScaleMin,
 onSetSpectrumScaleMax = onSetSpectrumScaleMax,
                    onToggleTinySaMarker = onToggleTinySaMarker,
                    onSetTinySaMarkerFrequency = onSetTinySaMarkerFrequency,
                    onSetTinySaMarkerAutoPeak = onSetTinySaMarkerAutoPeak,
                    onSetActiveTinySaMarker = onSetActiveTinySaMarker,
                    onResetMaxPeak = onResetMaxPeak,
                    onRefresh = onRefresh,
                )
            }
        }
        RfScopeTabBar(
            selected = page,
            onSelected = { pageName = it.name },
        )
    }
}

@Composable
private fun LivePage(
    state: RfScopeUiState,
    onRequestCellularPermissions: () -> Unit,
    onRefresh: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            CellularStatusBanner(
                snapshot = state.cellular,
                onRequestCellularPermissions = onRequestCellularPermissions,
                onOpenAppSettings = onOpenAppSettings,
                onOpenLocationSettings = onOpenLocationSettings,
            )
        }
        item {
            LiveMetricsPanel(
                snapshot = state.cellular,
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
            )
        }
        item { SignalTrendCard(state.signalHistory) }
        if (state.cellular.interferenceRisk) {
            item { InterferenceWarning(state.cellular.sinr) }
        }
        item { CellDetailsPanel(state.cellular) }
        item { FooterNote() }
    }
}

@Composable
private fun WifiPage(
    state: RfScopeUiState,
    onScanWifi: () -> Unit,
    onRequestWifiPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            WifiPanel(
                outcome = state.wifi,
                scanning = state.wifiScanInProgress,
                onScanWifi = onScanWifi,
                onRequestWifiPermissions = onRequestWifiPermissions,
                onOpenAppSettings = onOpenAppSettings,
                onOpenLocationSettings = onOpenLocationSettings,
            )
        }
        item { FooterNote() }
    }
}

@Composable
private fun HardwarePage(
    usb: UsbScanSummary,
    tinySa: TinySaUiState,
    selectedAntenna: AntennaType,
    onAntennaSelected: (AntennaType) -> Unit,
    onRequestUsbPermission: () -> Unit,
    onConnectTinySa: () -> Unit,
    onScanTinySa: () -> Unit,
    onStartTinySaLive: () -> Unit,
    onStopTinySaLive: () -> Unit,
    onToggleTinySaPause: () -> Unit,
    onToggleTinySaTone: () -> Unit,
    onSetTinySaRange: (Long, Long, Int) -> Unit,
    onSetTinySaRbw: (Int?) -> Unit,
    onSetTinySaAverage: (TinySaAverageMode) -> Unit,
    onSetTinySaToneMode: (TinySaToneMode) -> Unit,
    onSetTinySaToneReference: (Double) -> Unit,
    onSetThresholdEnabled: (Boolean) -> Unit,
    onSetThresholdDbm: (Double) -> Unit,
    onSetSpectrumScaleAuto: (Boolean) -> Unit,
    onSetSpectrumScaleMin: (Double) -> Unit,
    onSetSpectrumScaleMax: (Double) -> Unit,
    onToggleTinySaMarker: (Int) -> Unit,
    onSetTinySaMarkerFrequency: (Int, Long) -> Unit,
    onSetTinySaMarkerAutoPeak: (Int, Boolean) -> Unit,
    onSetActiveTinySaMarker: (Int) -> Unit,
    onResetMaxPeak: () -> Unit,
    onRefresh: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            TinySaPanel(
                state = tinySa,
                onConnect = onConnectTinySa,
                onScan = onScanTinySa,
                onStartLive = onStartTinySaLive,
                onStopLive = onStopTinySaLive,
                onTogglePause = onToggleTinySaPause,
                onToggleTone = onToggleTinySaTone,
                onSetRange = onSetTinySaRange,
                onSetRbw = onSetTinySaRbw,
                onSetAverage = onSetTinySaAverage,
                onSetToneMode = onSetTinySaToneMode,
                onSetToneReference = onSetTinySaToneReference,
                onSetThresholdEnabled = onSetThresholdEnabled,
                onSetThresholdDbm = onSetThresholdDbm,
                onSetSpectrumScaleAuto = onSetSpectrumScaleAuto,
                onSetSpectrumScaleMin = onSetSpectrumScaleMin,
                onSetSpectrumScaleMax = onSetSpectrumScaleMax,
                onToggleMarker = onToggleTinySaMarker,
                onSetMarkerFrequency = onSetTinySaMarkerFrequency,
                onSetMarkerAutoPeak = onSetTinySaMarkerAutoPeak,
                onSetActiveMarker = onSetActiveTinySaMarker,
                onResetMaxPeak = onResetMaxPeak,
            )
        }
        item { AntennaSelector(selectedAntenna, onAntennaSelected) }
        if (tinySa.status != TinySaConnectionStatus.CONNECTED) {
            item { UsbDevicePanel(usb, onRefresh, onRequestUsbPermission) }
            item { HardwarePanel(selectedAntenna) }
        }
        item { FooterNote() }
    }
}
@Composable
private fun RfScopeTabBar(
    selected: RfScopePage,
    onSelected: (RfScopePage) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScopePanel)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RfScopePage.entries.forEach { page ->
            PageTab(
                page = page,
                selected = page == selected,
                onClick = { onSelected(page) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PageTab(
    page: RfScopePage,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .background(if (selected) ScopePanelAlt else Color.Transparent)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = page.labelEn,
            color = if (selected) ScopeGreen else ScopeMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            letterSpacing = 0.7.sp,
        )
        Text(
            text = "(${page.labelZh})",
            color = if (selected) ScopeGreenDim else ScopeMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
        )
        Box(
            Modifier
                .width(24.dp)
                .height(2.dp)
                .background(if (selected) ScopeGreen else Color.Transparent),
        )
    }
}
@Composable
private fun Header(lastUpdatedEpochMs: Long?, isRefreshing: Boolean) {
    val clock by produceState(initialValue = formatTime(System.currentTimeMillis())) {
        while (true) {
            value = formatTime(System.currentTimeMillis())
            delay(1_000)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "RF-SCOPE",
                color = ScopeWhite,
                fontFamily = FontFamily.Monospace,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Text(
                text = "TINYSA ULTRA SUPPORT  /  RF + NETWORK MONITOR",
                color = ScopeCyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isRefreshing) ScopeAmber else ScopeGreen),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isRefreshing) "SYNC" else "LIVE",
                    color = if (isRefreshing) ScopeAmber else ScopeGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                )
            }
            Text(
                text = clock,
                color = ScopeGreenDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
            Text(
                text = "LAST ${lastUpdatedEpochMs?.let(::formatTime) ?: "--:--:--"}",
                color = ScopeMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
            )
        }
    }
}

@Composable
private fun CellularStatusBanner(
    snapshot: CellularSnapshot,
    onRequestCellularPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
) {
    when (snapshot.status) {
        CellReadStatus.PERMISSION_REQUIRED -> AccessBanner(
            title = "CELL ACCESS REQUIRED  (需要蜂窝权限)",
            bodyEn = snapshot.detailEn ?: "Precise location and phone-state permissions are required.",
            bodyZh = snapshot.detailZh ?: "",
            tone = BannerTone.GREEN,
            actionLabel = "GRANT ACCESS  (授权)",
            onAction = onRequestCellularPermissions,
            secondaryActionLabel = "SETTINGS  (设置)",
            onSecondaryAction = onOpenAppSettings,
        )

        CellReadStatus.LOCATION_DISABLED -> AccessBanner(
            title = "LOCATION OFF  (定位已关闭)",
            bodyEn = snapshot.detailEn ?: "Enable system Location for cell identity.",
            bodyZh = snapshot.detailZh ?: "",
            tone = BannerTone.AMBER,
            actionLabel = "OPEN LOCATION  (打开定位)",
            onAction = onOpenLocationSettings,
            secondaryActionLabel = "SETTINGS  (设置)",
            onSecondaryAction = onOpenAppSettings,
        )

        CellReadStatus.NO_NR_CELL -> AccessBanner(
            title = "NR CELL NOT EXPOSED  (未暴露 NR 小区)",
            bodyEn = snapshot.detailEn ?: "Public telephony API returned no CellInfoNr.",
            bodyZh = snapshot.detailZh ?: "",
            tone = BannerTone.AMBER,
        )

        CellReadStatus.ERROR -> AccessBanner(
            title = "TELEMETRY ERROR  (读取失败)",
            bodyEn = snapshot.detailEn ?: "Telephony API rejected the request.",
            bodyZh = snapshot.detailZh ?: "",
            tone = BannerTone.RED,
        )

        CellReadStatus.WAITING, CellReadStatus.READY -> Unit
    }
}

@Composable
private fun LiveMetricsPanel(
    snapshot: CellularSnapshot,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(
            title = "LIVE METRICS",
            subtitle = "实时指标",
            action = {
                TextButton(onClick = onRefresh, enabled = !isRefreshing) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            color = ScopeAmber,
                            strokeWidth = 1.5.dp,
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isRefreshing) "REFRESHING" else "REFRESH  (刷新)",
                        color = if (isRefreshing) ScopeAmber else ScopeGreenDim,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            },
        )

        ScopeCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroMetric(
                        label = "NR-BAND",
                        zh = "5G频段",
                        value = snapshot.band?.name ?: "—",
                        unit = null,
                        color = ScopeGreen,
                        modifier = Modifier.weight(1f),
                    )
                    HeroMetric(
                        label = "FREQ",
                        zh = "中心频率",
                        value = snapshot.frequencyMhz?.let(::formatMhz) ?: "—",
                        unit = "MHz",
                        color = ScopeCyan,
                        modifier = Modifier.weight(1.25f),
                    )
                    HeroMetric(
                        label = "PCI",
                        zh = "物理小区",
                        value = snapshot.pci?.toString() ?: "—",
                        unit = null,
                        color = ScopeWhite,
                        modifier = Modifier.weight(0.8f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroMetric(
                        label = "RSRP",
                        zh = "接收功率",
                        value = snapshot.rsrp?.toString() ?: "—",
                        unit = "dBm",
                        color = ScopeGreen,
                        bars = signalBarsForMetric(SignalMetric.RSRP, snapshot.rsrp),
                        modifier = Modifier.weight(1f),
                    )
                    HeroMetric(
                        label = "RSRQ",
                        zh = "接收质量",
                        value = snapshot.rsrq?.toString() ?: "—",
                        unit = "dB",
                        color = ScopeAmber,
                        bars = signalBarsForMetric(SignalMetric.RSRQ, snapshot.rsrq),
                        modifier = Modifier.weight(1f),
                    )
                    HeroMetric(
                        label = "SINR",
                        zh = "信噪比",
                        value = snapshot.sinr?.toString() ?: "—",
                        unit = "dB",
                        color = if (snapshot.interferenceRisk) ScopeRed else ScopeCyan,
                        bars = signalBarsForMetric(SignalMetric.SINR, snapshot.sinr),
                        modifier = Modifier.weight(1f),
                    )
                }

                ScopeDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = snapshot.operator?.let {
                            "${it.code} / ${it.nameEn}  (${it.nameZh})"
                        } ?: "OPERATOR  (运营商): —",
                        color = ScopeMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                    )
                    Text(
                        text = "NET ${snapshot.networkType}",
                        color = ScopeGreenDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroMetric(
    label: String,
    zh: String,
    value: String,
    unit: String?,
    color: Color,
    bars: Int? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(ScopePanelAlt, RoundedCornerShape(3.dp))
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            color = ScopeWhite,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        )
        Text(
            text = "($zh)",
            color = ScopeMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = color,
                fontFamily = FontFamily.Monospace,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            unit?.let {
                Spacer(Modifier.width(3.dp))
                Text(
                    text = it,
                    color = ScopeMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                )
            }
        }
        if (bars != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SignalBars(bars)
                Text(
                    text = "$bars/3",
                    color = ScopeMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                )
            }
        }
    }
}

@Composable
private fun CellDetailsPanel(snapshot: CellularSnapshot) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = "CELL DETAILS", subtitle = "小区详情")
        ScopeCard {
            Column {
                DetailLine(
                    en = "OPERATOR",
                    zh = "运营商",
                    value = snapshot.operator?.let {
                        "${it.code} / ${it.nameEn} (${it.nameZh})"
                    } ?: "—",
                )
                DetailLine("NETWORK", "网络类型", snapshot.networkType)
                DetailLine(
                    en = "PLMN",
                    zh = "MCC / MNC",
                    value = if (snapshot.mcc != null && snapshot.mnc != null) {
                        "${snapshot.mcc} / ${snapshot.mnc}"
                    } else {
                        "—"
                    },
                )
                DetailLine("NR-ARFCN", "NR 信道号", snapshot.nrarfcn?.toString() ?: "—")
                DetailLine("NCI", "NR 小区标识", formatNci(snapshot.nci))
                DetailLine("TAC", "跟踪区码", snapshot.tac?.toString() ?: "—")
                DetailLine(
                    en = "SSB BANDS",
                    zh = "SSB 频段",
                    value = snapshot.bands.takeIf { it.isNotEmpty() }
                        ?.joinToString(", ") { "n$it" }
                        ?: snapshot.band?.name
                        ?: "—",
                )
                DetailLine(
                    en = "SIGNAL LEVEL",
                    zh = "信号等级",
                    value = signalLevelLabel(snapshot.signalLevel),
                )
                DetailLine("CSI-RSRP", "CSI 参考信号功率", valueWithUnit(snapshot.csiRsrp, "dBm"))
                DetailLine("CSI-RSRQ", "CSI 参考信号质量", valueWithUnit(snapshot.csiRsrq, "dB"))
                DetailLine("CSI-SINR", "CSI 信噪比", valueWithUnit(snapshot.csiSinr, "dB"))
            }
        }
    }
}

@Composable
private fun DetailLine(
    en: String,
    zh: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = ScopeWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                ) { append(en) }
                withStyle(
                    SpanStyle(
                        color = ScopeMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                    ),
                ) { append("  ($zh)") }
            },
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = ScopeGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun InterferenceWarning(sinr: Int?) {
    ScopeCard(borderColor = ScopeRed) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(ScopeRed))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "CO-CHANNEL INTERFERENCE RISK  (同频干扰风险)",
                    color = ScopeRed,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "SINR ${sinr ?: "--"} dB is below 0 dB. Treat this as a heuristic warning, not a confirmed interferer.",
                color = ScopeWhite,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
            Text(
                text = "SINR 低于 0 dB，仅作为风险提示，不能单独证明存在同频干扰源。",
                color = ScopeMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun WifiPanel(
    outcome: WifiScanOutcome,
    scanning: Boolean,
    onScanWifi: () -> Unit,
    onRequestWifiPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
) {
    var selectedBssid by rememberSaveable { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(
            title = "WI-FI SCAN",
            subtitle = "Wi-Fi 扫描",
            action = {
                TextButton(onClick = onScanWifi, enabled = !scanning) {
                    if (scanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            color = ScopeAmber,
                            strokeWidth = 1.5.dp,
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = if (scanning) "SCANNING" else "SCAN  (扫描)",
                        color = if (scanning) ScopeAmber else ScopeGreenDim,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            },
        )

        when (outcome.status) {
            WifiScanStatus.PERMISSION_REQUIRED -> AccessBanner(
                title = "WI-FI ACCESS REQUIRED  (需要 Wi-Fi 权限)",
                bodyEn = outcome.detailEn ?: "Nearby Wi-Fi permission is required.",
                bodyZh = outcome.detailZh ?: "",
                tone = BannerTone.GREEN,
                actionLabel = "GRANT WI-FI  (授权)",
                onAction = onRequestWifiPermissions,
                secondaryActionLabel = "SETTINGS  (设置)",
                onSecondaryAction = onOpenAppSettings,
            )

            WifiScanStatus.LOCATION_DISABLED -> AccessBanner(
                title = "LOCATION OFF  (定位已关闭)",
                bodyEn = outcome.detailEn ?: "Enable system Location.",
                bodyZh = outcome.detailZh ?: "",
                tone = BannerTone.AMBER,
                actionLabel = "OPEN LOCATION  (打开定位)",
                onAction = onOpenLocationSettings,
                secondaryActionLabel = "SETTINGS  (设置)",
                onSecondaryAction = onOpenAppSettings,
            )

            WifiScanStatus.THROTTLED -> AccessBanner(
                title = "SCAN THROTTLED  (扫描被限流)",
                bodyEn = outcome.detailEn ?: "Android is returning cached scan results.",
                bodyZh = outcome.detailZh ?: "",
                tone = BannerTone.AMBER,
            )

            WifiScanStatus.ERROR -> AccessBanner(
                title = "WI-FI SCAN ERROR  (Wi-Fi 扫描失败)",
                bodyEn = outcome.detailEn ?: "Wi-Fi scan failed.",
                bodyZh = outcome.detailZh ?: "",
                tone = BannerTone.RED,
            )

            WifiScanStatus.IDLE -> Text(
                text = "TAP SCAN TO DISCOVER NEARBY NETWORKS  (点击扫描附近网络)",
                color = ScopeMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )

            WifiScanStatus.READY -> {
                if (outcome.networks.isEmpty()) {
                    Text(
                        text = "NO WI-FI NETWORKS FOUND  (未发现 Wi-Fi 网络)",
                        color = ScopeMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                    )
                } else {
                    Text(
                        text = wifiSummary(outcome.networks),
                        color = ScopeMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                    )
                    val visibleNetworks = outcome.networks.take(12)
                    WifiChannelChart(outcome.networks, "2.4 GHz", selectedBssid)
                    WifiChannelChart(outcome.networks, "5 GHz", selectedBssid)
                    WifiChannelChart(outcome.networks, "6 GHz", selectedBssid)
                    ScopeCard {
                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            visibleNetworks.forEachIndexed { index, network ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                ) {
                                    WifiRow(
                                        network = network,
                                        selected = network.bssid == selectedBssid,
                                        onClick = {
                                            selectedBssid = if (selectedBssid == network.bssid) null else network.bssid
                                        },
                                    )
                                }
                                if (index < visibleNetworks.lastIndex) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(ScopeGrid.copy(alpha = 0.82f)),
                                    )
                                }
                            }
                            if (outcome.networks.size > 12) {
                                Text(
                                    text = "+ ${outcome.networks.size - 12} MORE  (更多网络)",
                                    color = ScopeMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                    }
                    outcome.networks.firstOrNull()?.let { network ->
                        Text(
                            text = "${network.hintEn}  (${network.hintZh})",
                            color = ScopeMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiRow(
    network: WifiNetwork,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(if (selected) ScopeGreen.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = network.ssid,
                color = ScopeWhite,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${network.bssid}  •  ${network.band}  •  ${network.standard ?: "—"}",
                color = if (selected) ScopeGreenDim else ScopeMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${network.levelDbm} dBm",
                color = ScopeGreenDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${network.frequencyMhz} MHz",
                color = ScopeCyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
            Text(
                text = "CH ${network.channel ?: "—"} / ${network.widthMhz ?: "—"} MHz",
                color = ScopeMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
            )
        }
    }
}

@Composable
private fun HardwarePanel(selectedAntenna: AntennaType) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = "HARDWARE LINK", subtitle = "硬件接口")
        ScopeCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DetailLine("TINYSA ULTRA", "频谱仪", "USB / NOT CONNECTED")
                DetailLine("ANTENNA PROFILE", "天线配置", "${selectedAntenna.labelEn} (${selectedAntenna.labelZh})")
                ScopeDivider()
                Text(
                    text = "PHONE → TINYSA ULTRA → ANTENNA",
                    color = ScopeCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "手机只通过 USB 连接 TinySA；HT8、八木、拉杆 GP 和小环天线都连接在频谱仪侧。",
                    color = ScopeMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                )
                Text(
                    text = "USB HOST (OTG): CDC SERIAL READY  (CDC 串口驱动已接入)",
                    color = ScopeMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                )
            }
        }
    }
}

@Composable
private fun AntennaSelector(
    selected: AntennaType,
    onSelected: (AntennaType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = "ANTENNA TYPE", subtitle = "天线类型")
        ScopeCard {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AntennaType.entries.forEach { antenna ->
                    AntennaOption(
                        antenna = antenna,
                        selected = antenna == selected,
                        onClick = { onSelected(antenna) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AntennaOption(
    antenna: AntennaType,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .background(if (selected) ScopePanelAlt else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .padding(top = 4.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(if (selected) ScopeGreen else ScopeLine),
        )
        Spacer(Modifier.width(9.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "${antenna.labelEn}  (${antenna.labelZh})",
                color = if (selected) ScopeGreen else ScopeWhite,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
            Text(
                text = "${antenna.detailEn}  (${antenna.detailZh})",
                color = ScopeMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
            )
        }
    }
}

@Composable
private fun UsbDevicePanel(
    usb: UsbScanSummary,
    onRefresh: () -> Unit,
    onRequestUsbPermission: () -> Unit,
) {
    val likelyDevice = usb.devices.firstOrNull { it.likelyTinySa }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(
            title = "USB HOST",
            subtitle = "USB 主机",
            action = {
                TextButton(onClick = onRefresh) {
                    Text("RESCAN  (重新检测)", color = ScopeGreenDim, fontSize = 10.sp)
                }
            },
        )
        ScopeCard {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                if (!usb.hostSupported) {
                    Text(
                        text = "USB HOST API UNAVAILABLE  (设备不支持 USB 主机或系统未开放)",
                        color = ScopeAmber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                    )
                } else if (usb.devices.isEmpty()) {
                    Text(
                        text = "USB HOST: AVAILABLE  (USB 主机可用)",
                        color = ScopeGreenDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                    )
                    Text(
                        text = "NO USB DEVICE DETECTED  (未检测到 USB 设备)",
                        color = ScopeAmber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                    )
                    Text(
                        text = "检查 OTG 转接头、数据线、TinySA 电源和 USB 串口模式。",
                        color = ScopeMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                    )
                } else {
                    usb.devices.forEach { device ->
                        UsbDeviceRow(device)
                    }
                    if (likelyDevice != null && !likelyDevice.permissionGranted) {
                        TextButton(onClick = onRequestUsbPermission) {
                            Text(
                                "REQUEST USB PERMISSION  (请求 USB 权限)",
                                color = ScopeGreen,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    } else if (likelyDevice?.permissionGranted == true) {
                        Text(
                            "USB PERMISSION: GRANTED  (USB 权限已授权)",
                            color = ScopeGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                        )
                    }
                }
                Text(
                    text = "USB DETECTION + TINYSA SERIAL  (USB 检测与 TinySA 串口已接入)",
                    color = ScopeMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                )
            }
        }
    }
}

@Composable
private fun UsbDeviceRow(device: UsbDeviceSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = if (device.likelyTinySa) "LIKELY TINYSA / 可能是 TinySA" else "USB DEVICE / USB 设备",
            color = if (device.likelyTinySa) ScopeGreen else ScopeWhite,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${device.product ?: "Unknown product"}  (${device.manufacturer ?: "unknown"})",
            color = ScopeWhite,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
        )
        Text(
            text = "VID: ${hexId(device.vendorId)}  PID: ${hexId(device.productId)}",
            color = ScopeCyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
        )
        Text(
            text = "CDC SERIAL: ${if (device.hasSerialInterface) "FOUND" else "NOT FOUND"}  •  PERMISSION: ${if (device.permissionGranted) "GRANTED" else "NOT GRANTED"}",
            color = ScopeMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
        )
        ScopeDivider()
    }
}

private enum class RfScopePage(
    val labelEn: String,
    val labelZh: String,
) {
    LIVE("LIVE", "实时"),
    WIFI("WI-FI", "Wi-Fi"),
    HARDWARE("HARDWARE", "硬件"),
}
@Composable
private fun FooterNote() {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        ScopeDivider()
        Text(
            text = "PUBLIC TELEPHONY API IS BEST-EFFORT  (公开 Telephony API 只能尽力读取)",
            color = ScopeMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
        )
        Text(
            text = "NSA/SA visibility depends on modem, OEM ROM, SIM and Android version.",
            color = ScopeMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = title,
                color = ScopeWhite,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            )
            Text(
                text = "($subtitle)",
                color = ScopeMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
            )
        }
        action?.invoke()
    }
}

@Composable
private fun AccessBanner(
    title: String,
    bodyEn: String,
    bodyZh: String,
    tone: BannerTone,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    val accent = when (tone) {
        BannerTone.GREEN -> ScopeGreen
        BannerTone.AMBER -> ScopeAmber
        BannerTone.RED -> ScopeRed
    }
    ScopeCard(borderColor = accent) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                color = accent,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = bodyEn,
                color = ScopeWhite,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
            if (bodyZh.isNotBlank()) {
                Text(
                    text = bodyZh,
                    color = ScopeMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                )
            }
            Row {
                if (actionLabel != null && onAction != null) {
                    TextButton(
                        onClick = onAction,
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = accent),
                    ) {
                        Text(actionLabel, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    Spacer(Modifier.width(12.dp))
                    TextButton(
                        onClick = onSecondaryAction,
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = ScopeMuted),
                    ) {
                        Text(
                            secondaryActionLabel,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeCard(
    borderColor: Color = ScopeLine,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = ScopePanel),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            content()
        }
    }
}

@Composable
private fun ScopeDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ScopeLine),
    )
}

private enum class BannerTone {
    GREEN,
    AMBER,
    RED,
}

private fun formatMhz(value: Double): String = String.format(Locale.US, "%.2f", value)

private fun hexId(value: Int): String = String.format(Locale.US, "0x%04X", value)

private fun formatNci(nci: Long?): String =
    nci?.let { "0x" + it.toString(16).uppercase(Locale.US) } ?: "—"

private fun valueWithUnit(value: Int?, unit: String): String =
    value?.let { "$it $unit" } ?: "—"

private fun signalLevelLabel(level: Int?): String = when (level) {
    0 -> "NONE  (无)"
    1 -> "POOR  (差)"
    2 -> "MODERATE  (中)"
    3 -> "GOOD  (良好)"
    4 -> "GREAT  (优秀)"
    else -> "—"
}

private fun wifiSummary(networks: List<WifiNetwork>): String {
    val count24 = networks.count { it.band == "2.4 GHz" }
    val count5 = networks.count { it.band == "5 GHz" }
    val count6 = networks.count { it.band == "6 GHz" }
    return "BANDS  (频段): 2.4G $count24  •  5G $count5  •  6G $count6"
}

private fun formatTime(epochMs: Long): String = Instant.ofEpochMilli(epochMs)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("HH:mm:ss"))

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun RFScopePreview() {
    val operator = OperatorInfo("CMCC", "460", "00", "China Mobile", "中国移动")
    val band = BandMatch(
        name = "n41",
        operatorCode = "CMCC",
        startMhz = 2515.0,
        endMhz = 2675.0,
        noteEn = "China Mobile 2.6 GHz 5G",
        noteZh = "中国移动 2.6 GHz 5G",
    )
    val now = 1_760_000_000_000L
    val history = (0 until 20).map { index ->
        SignalSample(
            timestampMs = now - index * 2_000L,
            rsrp = -68 - (index % 5),
            rsrq = -9 - (index % 3),
            sinr = 18 - (index % 6),
        )
    }.reversed()
    val wifi = WifiNetwork(
        ssid = "LAB-5G",
        bssid = "AA:BB:CC:DD:EE:FF",
        frequencyMhz = 5180,
        channel = 36,
        widthMhz = 80,
        levelDbm = -48,
        band = "5 GHz",
        standard = "Wi-Fi 5",
        hintEn = "Usually cleaner, but wall penetration is weaker.",
        hintZh = "信道通常较干净，但穿透力差。",
    )
    RFScopeTheme {
        RFScopeScreen(
            state = RfScopeUiState(
                cellular = CellularSnapshot(
                    operator = operator,
                    networkType = "5G NR",
                    band = band,
                    bands = listOf(41),
                    frequencyMhz = 2524.95,
                    nrarfcn = 504990,
                    pci = 321,
                    tac = 12345,
                    nci = 0x1A2B3C4D5L,
                    rsrp = -70,
                    rsrq = -10,
                    sinr = 12,
                    csiRsrp = -72,
                    csiRsrq = -11,
                    csiSinr = 10,
                    signalLevel = 3,
                    status = CellReadStatus.READY,
                ),
                wifi = WifiScanOutcome(
                    status = WifiScanStatus.READY,
                    networks = listOf(wifi),
                ),
                signalHistory = history,
            ),
            onRequestCellularPermissions = {},
            onRequestWifiPermissions = {},
            onRequestUsbPermission = {},
            onConnectTinySa = {},
            onScanTinySa = {},
            onStartTinySaLive = {},
            onStopTinySaLive = {},
            onToggleTinySaPause = {},
            onToggleTinySaTone = {},
            onSetTinySaRange = { _, _, _ -> },
            onSetTinySaRbw = {},
            onSetTinySaAverage = {},
            onSetTinySaToneMode = {},
            onSetTinySaToneReference = {},
            onSetThresholdEnabled = {},
            onSetThresholdDbm = {},
 onSetSpectrumScaleAuto = {},
 onSetSpectrumScaleMin = {},
 onSetSpectrumScaleMax = {},
            onToggleTinySaMarker = {},
            onSetTinySaMarkerFrequency = { _, _ -> },
            onSetTinySaMarkerAutoPeak = { _, _ -> },
            onSetActiveTinySaMarker = {},
            onResetMaxPeak = {},
            onRefresh = {},
            onScanWifi = {},
            onOpenAppSettings = {},
            onOpenLocationSettings = {},
        )
    }
}











