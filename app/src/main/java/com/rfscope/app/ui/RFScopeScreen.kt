package com.rfscope.app.ui

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
import androidx.compose.material3.Card
import androidx.compose.material3.ButtonDefaults
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
import com.rfscope.app.data.WifiNetwork
import com.rfscope.app.data.WifiScanOutcome
import com.rfscope.app.data.WifiScanStatus
import com.rfscope.app.ui.theme.RFScopeTheme
import com.rfscope.app.ui.theme.ScopeBlack
import com.rfscope.app.ui.theme.ScopeGreen
import com.rfscope.app.ui.theme.ScopeGreenDim
import com.rfscope.app.ui.theme.ScopeLine
import com.rfscope.app.ui.theme.ScopeMuted
import com.rfscope.app.ui.theme.ScopePanel
import com.rfscope.app.ui.theme.ScopeRed
import com.rfscope.app.ui.theme.ScopeWhite
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RFScopeScreen(
    state: RfScopeUiState,
    onRequestPermissions: () -> Unit,
    onRefresh: () -> Unit,
    onScanWifi: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ScopeBlack)
            .systemBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Header(state.lastUpdatedEpochMs) }
        item { CellularPanel(state.cellular, onRequestPermissions, onRefresh) }
        if (state.cellular.interferenceRisk) {
            item { InterferenceWarning(state.cellular.sinr) }
        }
        item { WifiPanel(state.wifi, state.wifiScanInProgress, onScanWifi, onRequestPermissions) }
        item { HardwarePanel() }
        item { FooterNote() }
    }
}

@Composable
private fun Header(lastUpdatedEpochMs: Long?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "RF-SCOPE",
                color = ScopeGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Text(
                text = "PORTABLE RF / NETWORK MONITOR  (便携式无线电与网络监测)",
                color = ScopeMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(ScopeGreen))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "LIVE",
                    color = ScopeGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                )
            }
            Text(
                text = lastUpdatedEpochMs?.let(::formatTime) ?: "--:--:--",
                color = ScopeMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun CellularPanel(
    snapshot: CellularSnapshot,
    onRequestPermissions: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "SERVING CELL",
            subtitle = "服务小区",
            action = {
                TextButton(onClick = onRefresh) {
                    Text("REFRESH  (刷新)", color = ScopeGreenDim, fontSize = 10.sp)
                }
            },
        )

        if (snapshot.status == CellReadStatus.PERMISSION_REQUIRED) {
            AccessBanner(
                title = "ACCESS REQUIRED  (需要权限)",
                bodyEn = snapshot.detailEn ?: "Location permission is required.",
                bodyZh = snapshot.detailZh ?: "",
                actionLabel = "GRANT  (授权)",
                onAction = onRequestPermissions,
                tone = BannerTone.GREEN,
            )
        } else if (snapshot.status == CellReadStatus.LOCATION_DISABLED) {
            AccessBanner(
                title = "LOCATION OFF  (定位已关闭)",
                bodyEn = snapshot.detailEn ?: "Enable system Location.",
                bodyZh = snapshot.detailZh ?: "",
                tone = BannerTone.AMBER,
            )
        } else if (snapshot.status == CellReadStatus.NO_NR_CELL) {
            AccessBanner(
                title = "NR CELL NOT EXPOSED  (未暴露 NR 小区)",
                bodyEn = snapshot.detailEn ?: "Public telephony API returned no CellInfoNr.",
                bodyZh = snapshot.detailZh ?: "",
                tone = BannerTone.AMBER,
            )
        } else if (snapshot.status == CellReadStatus.ERROR) {
            AccessBanner(
                title = "TELEMETRY ERROR  (读取失败)",
                bodyEn = snapshot.detailEn ?: "Telephony API rejected the request.",
                bodyZh = snapshot.detailZh ?: "",
                tone = BannerTone.RED,
            )
        }

        ScopeCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MetricLine(
                    en = "OPERATOR",
                    zh = "运营商",
                    value = snapshot.operator?.code ?: "—",
                )
                if (snapshot.operator != null) {
                    Text(
                        text = "${snapshot.operator.nameEn}  (${snapshot.operator.nameZh})",
                        color = ScopeMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                    )
                }
                MetricLine("NETWORK", "网络类型", snapshot.networkType)
                MetricLine("NR-BAND", "5G频段", snapshot.band?.name ?: "—")
                MetricLine(
                    en = "NR-FREQ",
                    zh = "中心频率",
                    value = snapshot.frequencyMhz?.let { formatMhz(it) } ?: "—",
                    unit = snapshot.frequencyMhz?.let { "MHz" },
                )
                MetricLine("NR-ARFCN", "NR 绝对射频信道号", snapshot.nrarfcn?.toString() ?: "—")
                MetricLine("PCI", "物理小区标识", snapshot.pci?.toString() ?: "—")
                MetricLine("TAC", "跟踪区码", snapshot.tac?.toString() ?: "—")

                ScopeDivider()

                MetricLine(
                    en = "RSRP",
                    zh = "参考信号接收功率",
                    value = snapshot.rsrp?.let { "$it" } ?: "—",
                    unit = snapshot.rsrp?.let { "dBm" },
                    valueColor = if (snapshot.rsrp != null) ScopeGreen else ScopeMuted,
                )
                MetricLine(
                    en = "RSRQ",
                    zh = "参考信号接收质量",
                    value = snapshot.rsrq?.let { "$it" } ?: "—",
                    unit = snapshot.rsrq?.let { "dB" },
                    valueColor = if (snapshot.rsrq != null) ScopeGreen else ScopeMuted,
                )
                MetricLine(
                    en = "SINR",
                    zh = "信噪比",
                    value = snapshot.sinr?.let { "$it" } ?: "—",
                    unit = snapshot.sinr?.let { "dB" },
                    valueColor = when {
                        snapshot.sinr == null -> ScopeMuted
                        snapshot.sinr < 0 -> ScopeRed
                        else -> ScopeGreen
                    },
                )

                snapshot.band?.let { band ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "BAND CONTEXT  (频段说明): ${band.noteEn} (${band.noteZh})",
                        color = ScopeMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                    )
                }
            }
        }
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
    onRequestPermissions: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "WI-FI SCAN",
            subtitle = "Wi-Fi 扫描",
            action = {
                TextButton(onClick = onScanWifi, enabled = !scanning) {
                    if (scanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            color = ScopeGreen,
                            strokeWidth = 1.5.dp,
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = if (scanning) "SCANNING" else "SCAN  (扫描)",
                        color = if (scanning) ScopeMuted else ScopeGreenDim,
                        fontSize = 10.sp,
                    )
                }
            },
        )

        when (outcome.status) {
            WifiScanStatus.PERMISSION_REQUIRED -> AccessBanner(
                title = "WI-FI ACCESS REQUIRED  (需要 Wi-Fi 权限)",
                bodyEn = outcome.detailEn ?: "Nearby Wi-Fi permission is required.",
                bodyZh = outcome.detailZh ?: "",
                actionLabel = "GRANT  (授权)",
                onAction = onRequestPermissions,
                tone = BannerTone.GREEN,
            )

            WifiScanStatus.LOCATION_DISABLED -> AccessBanner(
                title = "LOCATION OFF  (定位已关闭)",
                bodyEn = outcome.detailEn ?: "Enable system Location.",
                bodyZh = outcome.detailZh ?: "",
                tone = BannerTone.AMBER,
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
                    ScopeCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            outcome.networks.take(8).forEach { network ->
                                WifiRow(network)
                            }
                            if (outcome.networks.size > 8) {
                                Text(
                                    text = "+ ${outcome.networks.size - 8} MORE  (更多网络)",
                                    color = ScopeMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
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

@Composable
private fun WifiRow(network: WifiNetwork) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                text = "${network.bssid}  •  ${network.band}  •  ${network.levelDbm} dBm",
                color = ScopeMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${network.frequencyMhz} MHz",
                color = ScopeGreen,
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
private fun HardwarePanel() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = "HARDWARE LINK", subtitle = "硬件接口")
        ScopeCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MetricLine("TINYSA ULTRA", "频谱仪", "NOT CONNECTED", valueColor = ScopeMuted)
                MetricLine("HT8 ANTENNA", "定向天线", "NOT CONNECTED", valueColor = ScopeMuted)
                Text(
                    text = "BLE / USB serial transport and SNR, -3dB width, harmonic, Max Hold, Averaging are reserved in hardware/RfMeasurement.kt  (硬件与测量算法接口已预留)",
                    color = ScopeMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                )
            }
        }
    }
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
private fun MetricLine(
    en: String,
    zh: String,
    value: String,
    unit: String? = null,
    valueColor: Color = ScopeGreen,
) {
    Text(
        text = metricAnnotated(en, zh, value, unit, valueColor),
        modifier = Modifier.fillMaxWidth(),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        lineHeight = 25.sp,
    )
}

private fun metricAnnotated(
    en: String,
    zh: String,
    value: String,
    unit: String?,
    valueColor: Color,
): AnnotatedString = buildAnnotatedString {
    withStyle(
        SpanStyle(
            color = ScopeWhite,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp,
        ),
    ) {
        append(en)
    }
    withStyle(
        SpanStyle(
            color = ScopeMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
        ),
    ) {
        append("  ($zh)  ")
    }
    withStyle(
        SpanStyle(
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        ),
    ) {
        append(value)
    }
    if (unit != null) {
        withStyle(
            SpanStyle(
                color = ScopeMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            ),
        ) {
            append(" $unit")
        }
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
) {
    val accent = when (tone) {
        BannerTone.GREEN -> ScopeGreen
        BannerTone.AMBER -> Color(0xFFFFB000)
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
            if (actionLabel != null && onAction != null) {
                TextButton(
                    onClick = onAction,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = accent),
                ) {
                    Text(actionLabel, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
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
    val wifi = WifiNetwork(
        ssid = "LAB-5G",
        bssid = "AA:BB:CC:DD:EE:FF",
        frequencyMhz = 5180,
        channel = 36,
        widthMhz = 80,
        levelDbm = -48,
        band = "5 GHz",
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
                    frequencyMhz = 2524.95,
                    nrarfcn = 504990,
                    pci = 321,
                    tac = 12345,
                    rsrp = -70,
                    rsrq = -10,
                    sinr = -4,
                    status = CellReadStatus.READY,
                ),
                wifi = WifiScanOutcome(
                    status = WifiScanStatus.READY,
                    networks = listOf(wifi),
                ),
            ),
            onRequestPermissions = {},
            onRefresh = {},
            onScanWifi = {},
        )
    }
}


