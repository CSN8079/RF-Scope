package com.rfscope.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rfscope.app.data.TinySaConnectionStatus
import com.rfscope.app.data.TinySaAverageMode
import com.rfscope.app.data.TinySaToneMode
import com.rfscope.app.data.TinySaWaterfallRow
import com.rfscope.app.data.TinySaMarker
import com.rfscope.app.data.TinySaSpectrum
import com.rfscope.app.data.TinySaUiState
import com.rfscope.app.ui.theme.ScopeAmber
import com.rfscope.app.ui.theme.ScopeBlack
import com.rfscope.app.ui.theme.ScopeCyan
import com.rfscope.app.ui.theme.ScopeGreen
import com.rfscope.app.ui.theme.ScopeGreenDim
import com.rfscope.app.ui.theme.ScopeGrid
import com.rfscope.app.ui.theme.ScopeLine
import com.rfscope.app.ui.theme.ScopeMuted
import com.rfscope.app.ui.theme.ScopePanel
import com.rfscope.app.ui.theme.ScopeRed
import com.rfscope.app.ui.theme.ScopeWhite
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun TinySaPanel(
    state: TinySaUiState,
    onConnect: () -> Unit,
    onScan: () -> Unit,
    onStartLive: () -> Unit,
    onStopLive: () -> Unit,
    onToggleTone: () -> Unit,
    onSetRange: (Long, Long, Int) -> Unit,
    onSetRbw: (Int?) -> Unit,
    onSetAverage: (TinySaAverageMode) -> Unit,
    onSetToneMode: (TinySaToneMode) -> Unit,
    onSetToneReference: (Double) -> Unit,
 onSetThresholdEnabled: (Boolean) -> Unit,
 onSetThresholdDbm: (Double) -> Unit,
    onToggleMarker: (Int) -> Unit,
 onSetMarkerAutoPeak: (Int, Boolean) -> Unit,
    onSetMarkerFrequency: (Int, Long) -> Unit,
    onSetActiveMarker: (Int) -> Unit,
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
 Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
 if (showSettings) {
 TinySaSettingsDialog(
 state = state,
 onStartLive = onStartLive,
 onStopLive = onStopLive,
 onToggleTone = onToggleTone,
 onSetRange = onSetRange,
 onSetRbw = onSetRbw,
 onSetAverage = onSetAverage,
 onSetToneMode = onSetToneMode,
 onSetToneReference = onSetToneReference,
 onSetThresholdEnabled = onSetThresholdEnabled,
 onSetThresholdDbm = onSetThresholdDbm,
 onToggleMarker = onToggleMarker,
 onSetMarkerFrequency = onSetMarkerFrequency,
 onSetMarkerAutoPeak = onSetMarkerAutoPeak,
 onSetActiveMarker = onSetActiveMarker,
 onScan = onScan,
 onDismiss = { showSettings = false },
 )
 }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "TINYSA SPECTRUM",
                    color = ScopeWhite,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                )
                Text(
                    text = "(频谱数据)",
                    color = ScopeMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                )
            }
            when (state.status) {
                TinySaConnectionStatus.CONNECTED -> TextButton(onClick = onScan) {
                    Text("SCAN 430-500 MHz", color = ScopeGreen, fontSize = 9.sp)
                }

                TinySaConnectionStatus.CONNECTING -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        color = ScopeAmber,
                        strokeWidth = 1.5.dp,
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("CONNECTING", color = ScopeAmber, fontSize = 9.sp)
                }

                else -> TextButton(onClick = onConnect) {
                    Text("CONNECT  (连接)", color = ScopeGreen, fontSize = 9.sp)
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = ScopePanel),
            border = BorderStroke(1.dp, ScopeLine),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = when (state.status) {
                        TinySaConnectionStatus.DISCONNECTED -> "USB CDC READY / NOT CONNECTED  (未连接)"
                        TinySaConnectionStatus.CONNECTING -> "OPENING TINYSA SERIAL PORT  (正在打开串口)"
                        TinySaConnectionStatus.CONNECTED -> "TINYSA CONNECTED  (已连接)"
                        TinySaConnectionStatus.ERROR -> "TINYSA ERROR  (通信错误)"
                    },
                    color = when (state.status) {
                        TinySaConnectionStatus.CONNECTED -> ScopeGreen
                        TinySaConnectionStatus.ERROR -> ScopeRed
                        TinySaConnectionStatus.CONNECTING -> ScopeAmber
                        TinySaConnectionStatus.DISCONNECTED -> ScopeMuted
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                state.messageEn?.let { message ->
                    Text(
                        text = "$message  (${state.messageZh ?: ""})",
                        color = ScopeMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                    )
                }
                val spectrum = state.spectrum
                if (state.isScanning) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = ScopeAmber,
                            strokeWidth = 1.5.dp,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "SCANNING  (扫描中)",
                            color = ScopeAmber,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                        )
                    }
                }
                Row(
 modifier = Modifier.fillMaxWidth(),
 horizontalArrangement = Arrangement.End,
 ) {
 TextButton(onClick = { showSettings = true }) {
 Text("SETTINGS (\u8bbe\u7f6e)", color = ScopeCyan, fontSize = 9.sp)
 }
 }
 if (spectrum != null) {
                    SpectrumSummary(spectrum)
                    TinySaSmartDisplay(spectrum = spectrum, waterfall = state.waterfall, thresholdEnabled = state.thresholdEnabled, thresholdDbm = state.thresholdDbm, markers = state.markers, activeMarkerId = state.activeMarkerId)
 } else {
                    Text(
                        text = "NO SPECTRUM DATA YET  (尚未读取频谱)",
                        color = ScopeMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                    )
                }
                Text(
                    text = "TinySA Ultra uses scanraw; raw/32 - 174 dBm.",
                    color = ScopeMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                )
            }
        }
    }
}

@Composable
private fun SpectrumSummary(spectrum: TinySaSpectrum) {
    val snr = spectrum.peakDbm - spectrum.noiseFloorDbm
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SummaryItem("PEAK", formatMHz(spectrum.peakHz), "${formatDbm(spectrum.peakDbm)} dBm")
        SummaryItem("NOISE", "FLOOR", "${formatDbm(spectrum.noiseFloorDbm)} dBm")
        SummaryItem("SNR", "${formatDbm(snr)} dB", "${spectrum.points} pts")
    }
}

@Composable
private fun SummaryItem(label: String, value: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = label,
            color = ScopeMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
        )
        Text(
            text = value,
            color = ScopeGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = detail,
            color = ScopeMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
        )
    }
}

@Composable
private fun TinySaSpectrumChart(spectrum: TinySaSpectrum) {
    val minPower = (spectrum.noiseFloorDbm - 8.0).coerceAtMost(-80.0)
    val maxPower = (spectrum.peakDbm + 8.0).coerceAtLeast(-30.0)
    val powerSpan = (maxPower - minPower).coerceAtLeast(1.0)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        ) {
            for (index in 0..4) {
                val y = size.height * index / 4f
                drawLine(
                    color = ScopeGrid,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = if (index == 0 || index == 4) 1.5f else 1f,
                )
            }
            val path = Path()
            spectrum.frequenciesHz.forEachIndexed { index, _ ->
                val x = index / (spectrum.points - 1f) * size.width
                val normalized = ((spectrum.powersDbm[index] - minPower) / powerSpan)
                    .coerceIn(0.0, 1.0)
                val y = size.height - normalized.toFloat() * size.height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = ScopeCyan,
                style = Stroke(width = 2f, cap = StrokeCap.Round),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatMHz(spectrum.startHz),
                color = ScopeMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
            )
            Text(
                text = "FREQUENCY  (频率)",
                color = ScopeMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
            )
            Text(
                text = formatMHz(spectrum.stopHz),
                color = ScopeMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
            )
        }
    }
}

private fun formatMHz(hz: Long): String = formatMHz(hz.toDouble())

private fun formatMHz(hz: Double): String = String.format(Locale.US, "%.2f MHz", hz / 1_000_000.0)
private fun formatDbm(value: Double): String = String.format(Locale.US, "%.1f", value)


@Composable
private fun TinySaControls(
    state: TinySaUiState,
    onStartLive: () -> Unit,
    onStopLive: () -> Unit,
    onToggleTone: () -> Unit,
    onSetRange: (Long, Long, Int) -> Unit,
    onSetRbw: (Int?) -> Unit,
    onSetAverage: (TinySaAverageMode) -> Unit,
    onSetToneMode: (TinySaToneMode) -> Unit,
    onSetToneReference: (Double) -> Unit,
 onSetThresholdEnabled: (Boolean) -> Unit,
 onSetThresholdDbm: (Double) -> Unit,
    onToggleMarker: (Int) -> Unit,
 onSetMarkerAutoPeak: (Int, Boolean) -> Unit,
    onSetMarkerFrequency: (Int, Long) -> Unit,
    onSetActiveMarker: (Int) -> Unit,
    onScan: () -> Unit,
) {
    var startText by rememberSaveable { mutableStateOf(formatMhzValue(state.scanStartHz)) }
    var stopText by rememberSaveable { mutableStateOf(formatMhzValue(state.scanStopHz)) }
    var pointsText by rememberSaveable { mutableStateOf(state.scanPoints.toString()) }
    val activeMarker = state.markers.firstOrNull { it.id == state.activeMarkerId }
    var markerText by rememberSaveable { mutableStateOf(formatMhzValue(activeMarker?.frequencyHz ?: state.scanStartHz)) }
    var showCarrierBands by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.scanStartHz, state.scanStopHz, state.scanPoints) {
        startText = formatMhzValue(state.scanStartHz)
        stopText = formatMhzValue(state.scanStopHz)
        pointsText = state.scanPoints.toString()
    }
    LaunchedEffect(state.activeMarkerId, activeMarker?.frequencyHz) {
        markerText = formatMhzValue(activeMarker?.frequencyHz ?: state.scanStartHz)
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (showCarrierBands) {
            CarrierBandDialog(
                onDismiss = { showCarrierBands = false },
                onSelect = {
                    onSetRange(it.startHz, it.stopHz, state.scanPoints)
                    showCarrierBands = false
                },
            )
        }
        TextButton(onClick = { showCarrierBands = true }) {
            Text("运营商 4G/5G 频段", color = ScopeAmber, fontSize = 8.sp)
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            listOf(
                "88-108M" to (88_000_000L to 108_000_000L),
                "144-148M" to (144_000_000L to 148_000_000L),
                "400-500M" to (400_000_000L to 500_000_000L),
                "U5 430-440M" to (430_000_000L to 440_000_000L),
                "2.4-2.5G" to (2_400_000_000L to 2_500_000_000L),
                "3.4-3.6G" to (3_400_000_000L to 3_600_000_000L),
            ).forEach { (label, range) ->
                TextButton(onClick = { onSetRange(range.first, range.second, state.scanPoints) }) {
                    Text(label, color = ScopeCyan, fontSize = 8.sp)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            CompactField(
                value = startText,
                onValueChange = { startText = it },
                label = "START MHz",
                modifier = Modifier.weight(1f),
            )
            CompactField(
                value = stopText,
                onValueChange = { stopText = it },
                label = "STOP MHz",
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactField(
                value = pointsText,
                onValueChange = { pointsText = it.filter(Char::isDigit) },
                label = "POINTS",
                modifier = Modifier.width(82.dp),
            )
            TextButton(onClick = {
                val start = startText.toDoubleOrNull()?.times(1_000_000.0)?.toLong()
                val stop = stopText.toDoubleOrNull()?.times(1_000_000.0)?.toLong()
                val points = pointsText.toIntOrNull()
                if (start != null && stop != null && points != null) {
                    onSetRange(start, stop, points)
                }
            }) {
                Text("APPLY", color = ScopeGreen, fontSize = 9.sp)
            }
            TextButton(onClick = if (state.isLive) onStopLive else onStartLive) {
                Text(
                    if (state.isLive) "STOP LIVE" else "LIVE",
                    color = if (state.isLive) ScopeRed else ScopeGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            TextButton(onClick = onScan, enabled = !state.isLive) {
                Text("SINGLE", color = if (state.isLive) ScopeMuted else ScopeGreenDim, fontSize = 9.sp)
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("RBW", color = ScopeMuted, fontSize = 8.sp)
            listOf<Pair<String, Int?>>(
                "AUTO" to null,
                "3k" to 3,
                "10k" to 10,
                "30k" to 30,
                "100k" to 100,
                "300k" to 300,
            ).forEach { (label, value) ->
                TextButton(onClick = { onSetRbw(value) }) {
                    Text(
                        label,
                        color = if (state.rbwKhz == value && (value != null || state.rbwKhz == null)) ScopeGreen else ScopeMuted,
                        fontSize = 8.sp,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("VBW/AVG", color = ScopeMuted, fontSize = 8.sp)
            listOf(
                "OFF" to TinySaAverageMode.OFF,
                "4x" to TinySaAverageMode.AVER4,
                "16x" to TinySaAverageMode.AVER16,
            ).forEach { (label, mode) ->
                TextButton(onClick = { onSetAverage(mode) }) {
                    Text(label, color = if (state.averageMode == mode) ScopeGreen else ScopeMuted, fontSize = 8.sp)
                }
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onToggleTone) {
                Text(
                    if (state.isToneEnabled) "TONE ON ${state.toneFrequencyHz.toInt()}Hz" else "TONE OFF",
                    color = if (state.isToneEnabled) ScopeAmber else ScopeMuted,
                    fontSize = 8.sp,
                )
            }
            listOf(
                "PEAK" to TinySaToneMode.PEAK,
                "AVG" to TinySaToneMode.AVERAGE,
                "MARKER" to TinySaToneMode.MARKER,
            ).forEach { (label, mode) ->
                TextButton(onClick = { onSetToneMode(mode) }) {
                    Text(label, color = if (state.toneMode == mode) ScopeGreen else ScopeMuted, fontSize = 8.sp)
                }
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("MARKERS", color = ScopeMuted, fontSize = 8.sp)
            state.markers.forEach { marker ->
                TextButton(onClick = {
                    if (marker.enabled) onSetActiveMarker(marker.id) else onToggleMarker(marker.id)
                }) {
                    Text(
                        text = "M${marker.id}",
                        color = when {
                            marker.id == state.activeMarkerId -> ScopeGreen
                            marker.enabled -> ScopeCyan
                            else -> ScopeMuted
                        },
                        fontSize = 9.sp,
                    )
                }
            }
        }

        Row(
 modifier = Modifier.horizontalScroll(rememberScrollState()),
 horizontalArrangement = Arrangement.spacedBy(2.dp),
 verticalAlignment = Alignment.CenterVertically,
 ) {
 Text("MODE", color = ScopeMuted, fontSize = 8.sp)
 TextButton(onClick = { onSetMarkerAutoPeak(state.activeMarkerId, true) }) {
 Text("AUTO PEAK", color = ScopeAmber, fontSize = 8.sp)
 }
 TextButton(onClick = { onSetMarkerAutoPeak(state.activeMarkerId, false) }) {
 Text("MANUAL", color = ScopeCyan, fontSize = 8.sp)
 }
 Text(if (state.markers.firstOrNull { it.id == state.activeMarkerId }?.autoPeak == true) "AUTO" else "MANUAL", color = ScopeMuted, fontSize = 8.sp)
 }

 Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactField(
                value = markerText,
                onValueChange = { markerText = it },
                label = "MARKER ${state.activeMarkerId} MHz",
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = {
                markerText.toDoubleOrNull()?.times(1_000_000.0)?.toLong()?.let { hz ->
                    onSetMarkerFrequency(state.activeMarkerId, hz)
                }
            }) {
                Text("SET M${state.activeMarkerId}", color = ScopeGreen, fontSize = 8.sp)
            }
        }

        Row(
 modifier = Modifier.fillMaxWidth(),
 verticalAlignment = Alignment.CenterVertically,
 horizontalArrangement = Arrangement.spacedBy(6.dp),
 ) {
 TextButton(onClick = { onSetThresholdEnabled(!state.thresholdEnabled) }) {
 Text(if (state.thresholdEnabled) "LIMIT ON" else "LIMIT OFF", color = if (state.thresholdEnabled) ScopeAmber else ScopeMuted, fontSize = 8.sp)
 }
 Text("${state.thresholdDbm.toInt()} dBm", color = ScopeAmber, fontSize = 8.sp)
 Slider(value = state.thresholdDbm.toFloat(), onValueChange = { onSetThresholdDbm(it.toDouble()) }, valueRange = -120f..-10f, modifier = Modifier.weight(1f))
 }

 Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "TONE REF ${state.toneReferenceDbm.toInt()} dBm",
                color = ScopeMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
            )
            Slider(
                value = state.toneReferenceDbm.toFloat(),
                onValueChange = { onSetToneReference(it.toDouble()) },
                valueRange = -100f..-10f,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = "RANGE ${formatMHz(state.scanStartHz)} - ${formatMHz(state.scanStopHz)}  •  ${state.scanPoints} PTS  •  ${state.lastScanDurationMs ?: 0} ms  •  WF ${state.waterfall.size}",
            color = ScopeMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
        )
    }
}

@Composable
private fun CompactField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        label = { Text(label, fontSize = 8.sp, fontFamily = FontFamily.Monospace) },
        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ScopeWhite),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@Composable
private fun TinySaWaterfall(
    frames: List<TinySaWaterfallRow>,
    startHz: Long,
    stopHz: Long,
) {
    if (frames.isEmpty()) return
    val visible = frames.takeLast(240)
    val columns = visible.firstOrNull()?.powersDbm?.size ?: return
    val rowHeight = 2.dp
    val totalHeight = rowHeight * visible.size
    val newestTime = visible.last().timestampMs

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = "WATERFALL  (瀑布图)  •  ${visible.size} ROWS / 1 SCAN = 1 ROW",
            color = ScopeCyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(46.dp)
                    .height(totalHeight),
            ) {
                visible.indices.filter { it % 20 == 0 }.forEach { rowIndex ->
                    val source = visible[visible.lastIndex - rowIndex]
                    val seconds = ((newestTime - source.timestampMs) / 1000L).coerceAtLeast(0L)
                    Text(
                        text = waterfallTimeLabel(seconds),
                        color = ScopeMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 7.sp,
                        modifier = Modifier.offset(y = rowHeight * rowIndex),
                    )
                }
            }
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(totalHeight),
            ) {
                val rows = visible.size
                val cellWidth = size.width / columns
                val cellHeight = size.height / rows
                visible.forEachIndexed { rowIndex, _ ->
                    val source = visible[visible.lastIndex - rowIndex]
                    val y = rowIndex * cellHeight
                    var column = 0
                    while (column < columns) {
                        val value = source.powersDbm.getOrNull(column) ?: -120f
                        val x = column * cellWidth
                        drawRect(
                            color = waterfallColor(value),
                            topLeft = Offset(x, y),
                            size = Size(
                                width = (cellWidth * 2f).coerceAtMost(size.width - x),
                                height = cellHeight + 0.4f,
                            ),
                        )
                        column += 2
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 46.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatMHz(startHz), color = ScopeMuted, fontFamily = FontFamily.Monospace, fontSize = 7.sp)
            Text(formatMHz((startHz + stopHz) / 2L), color = ScopeMuted, fontFamily = FontFamily.Monospace, fontSize = 7.sp)
            Text(formatMHz(stopHz), color = ScopeMuted, fontFamily = FontFamily.Monospace, fontSize = 7.sp)
        }
    }
}

private fun waterfallTimeLabel(seconds: Long): String = when {
    seconds == 0L -> "0s"
    seconds < 60L -> "-${seconds}s"
    seconds % 60L == 0L -> "-${seconds / 60L}m"
    else -> "-${seconds / 60L}m${seconds % 60L}s"
}
private fun waterfallColor(dbm: Float): Color = when {
    dbm >= -40f -> Color(0xFFFF4040)
    dbm >= -60f -> Color(0xFFFFD000)
    dbm >= -80f -> Color(0xFF00C853)
    dbm >= -100f -> Color(0xFF00695C)
    else -> Color(0xFF001B16)
}

private fun formatMhzValue(hz: Long): String = String.format(Locale.US, "%.3f", hz / 1_000_000.0)

data class CarrierBandPreset(
    val name: String,
    val startHz: Long,
    val stopHz: Long,
)

private val carrierBandPresets = listOf(
    CarrierBandPreset("中国移动 5G n41 2515-2675", 2_515_000_000L, 2_675_000_000L),
    CarrierBandPreset("中国移动 5G n79 4800-4900", 4_800_000_000L, 4_900_000_000L),
    CarrierBandPreset("中国移动 4G B38 2570-2620", 2_570_000_000L, 2_620_000_000L),
    CarrierBandPreset("中国移动 4G B39 1880-1920", 1_880_000_000L, 1_920_000_000L),
    CarrierBandPreset("中国联通 5G n78 3500-3600", 3_500_000_000L, 3_600_000_000L),
    CarrierBandPreset("中国联通 4G B1 2110-2170", 2_110_000_000L, 2_170_000_000L),
    CarrierBandPreset("中国联通 4G B3 1805-1880", 1_805_000_000L, 1_880_000_000L),
    CarrierBandPreset("中国电信 5G n78 3400-3500", 3_400_000_000L, 3_500_000_000L),
    CarrierBandPreset("中国电信 5G n1 2110-2170", 2_110_000_000L, 2_170_000_000L),
    CarrierBandPreset("中国电信 4G B1 2110-2170", 2_110_000_000L, 2_170_000_000L),
    CarrierBandPreset("中国电信 4G B5 869-894", 869_000_000L, 894_000_000L),
    CarrierBandPreset("中国广电 5G n28 758-788", 758_000_000L, 788_000_000L),
)

@Composable
private fun CarrierBandDialog(
    onDismiss: () -> Unit,
    onSelect: (CarrierBandPreset) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "运营商 4G/5G 频段",
                color = ScopeWhite,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                carrierBandPresets.forEach { preset ->
                    TextButton(onClick = { onSelect(preset) }) {
                        Text(
                            text = "${preset.name}  (${formatMHz(preset.startHz)} - ${formatMHz(preset.stopHz)})",
                            color = ScopeCyan,
                            fontSize = 9.sp,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE  (关闭)", color = ScopeMuted, fontSize = 9.sp)
            }
        },
    )
}

private val SmartPlotGutter = 48.dp
private val SmartSpectrumHeight = 170.dp
private val SmartWaterfallRowHeight = 2.dp
private const val SmartWaterfallMaxRows = 240

private data class SmartDbScale(
    val minDbm: Double,
    val maxDbm: Double,
    val ticks: List<Double>,
)

@Composable
private fun TinySaSmartDisplay(
    spectrum: TinySaSpectrum,
    waterfall: List<TinySaWaterfallRow>,
    thresholdEnabled: Boolean,
    thresholdDbm: Double,
    markers: List<TinySaMarker>,
    activeMarkerId: Int,
) {
    val scale = smartDbScale(spectrum, thresholdEnabled, thresholdDbm)
    val visibleRows = waterfall.takeLast(SmartWaterfallMaxRows)
    val totalWaterfallHeight = SmartWaterfallRowHeight * visibleRows.size.coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScopeBlack)
            .padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "SMART SDR VIEW (\u9891\u8c31\u4e0e\u7011\u5e03) dBm",
                color = ScopeCyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${formatAxisHz(spectrum.startHz)} - ${formatAxisHz(spectrum.stopHz)}",
                color = ScopeMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(SmartSpectrumHeight),
        ) {
            SmartDbAxis(scale)
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(ScopeBlack),
            ) {
                drawSmartSpectrum(
                    spectrum = spectrum,
                    scale = scale,
                    thresholdEnabled = thresholdEnabled,
                    thresholdDbm = thresholdDbm,
                    markers = markers,
                    activeMarkerId = activeMarkerId,
                )
            }
        }

        SmartFrequencyAxis(
            startHz = spectrum.startHz,
            stopHz = spectrum.stopHz,
            label = "FREQUENCY (\u9891\u7387)",
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ScopeLine),
        )

        if (visibleRows.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalWaterfallHeight),
            ) {
                SmartWaterfallTimeRail(
                    frames = visibleRows,
                    totalHeight = totalWaterfallHeight,
                )
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(ScopeBlack),
                ) {
                    drawSmartWaterfall(
                        frames = visibleRows,
                        displayStartHz = spectrum.startHz,
                        displayStopHz = spectrum.stopHz,
                        scale = scale,
                    )
                }
            }
            SmartFrequencyAxis(
                startHz = spectrum.startHz,
                stopHz = spectrum.stopHz,
                label = "WATERFALL (\u7011\u5e03)",
            )
        }

        SmartColorLegend()
    }
}

@Composable
private fun SmartDbAxis(scale: SmartDbScale) {
    Column(
        modifier = Modifier
            .width(SmartPlotGutter)
            .height(SmartSpectrumHeight)
            .padding(end = 5.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        scale.ticks.forEach { tick ->
            Text(
                text = String.format(Locale.US, "%.0f", tick),
                color = ScopeMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 7.sp,
            )
        }
    }
}

@Composable
private fun SmartFrequencyAxis(
    startHz: Long,
    stopHz: Long,
    label: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(SmartPlotGutter))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(formatAxisHz(startHz), color = ScopeMuted, fontFamily = FontFamily.Monospace, fontSize = 7.sp)
            Text(label, color = ScopeMuted, fontFamily = FontFamily.Monospace, fontSize = 7.sp)
            Text(formatAxisHz(stopHz), color = ScopeMuted, fontFamily = FontFamily.Monospace, fontSize = 7.sp)
        }
    }
}

@Composable
private fun SmartWaterfallTimeRail(
    frames: List<TinySaWaterfallRow>,
    totalHeight: androidx.compose.ui.unit.Dp,
) {
    val newestTime = frames.lastOrNull()?.timestampMs ?: 0L
    Box(
        modifier = Modifier
            .width(SmartPlotGutter)
            .height(totalHeight),
    ) {
        frames.indices.filter { it % 20 == 0 }.forEach { rowIndex ->
            val source = frames[frames.lastIndex - rowIndex]
            val seconds = ((newestTime - source.timestampMs) / 1000L).coerceAtLeast(0L)
            Text(
                text = waterfallTimeLabel(seconds),
                color = ScopeMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 7.sp,
                modifier = Modifier
                    .width(SmartPlotGutter - 4.dp)
                    .offset(y = SmartWaterfallRowHeight * rowIndex - 3.dp),
            )
        }
    }
}

@Composable
private fun SmartColorLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("LEVEL", color = ScopeMuted, fontFamily = FontFamily.Monospace, fontSize = 7.sp)
        Text("WEAK", color = ScopeMuted, fontFamily = FontFamily.Monospace, fontSize = 7.sp)
        listOf(0.05f, 0.25f, 0.45f, 0.65f, 0.82f, 1.0f).forEach { level ->
            Box(
                modifier = Modifier
                    .width(15.dp)
                    .height(5.dp)
                    .background(smartSpectrumColor(level)),
            )
        }
        Text("STRONG", color = ScopeMuted, fontFamily = FontFamily.Monospace, fontSize = 7.sp)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmartSpectrum(
    spectrum: TinySaSpectrum,
    scale: SmartDbScale,
    thresholdEnabled: Boolean,
    thresholdDbm: Double,
    markers: List<TinySaMarker>,
    activeMarkerId: Int,
) {
    val width = size.width
    val height = size.height
    val startHz = spectrum.startHz.toDouble()
    val stopHz = spectrum.stopHz.toDouble()
    val spanHz = (stopHz - startHz).coerceAtLeast(1.0)
    val dbSpan = (scale.maxDbm - scale.minDbm).coerceAtLeast(1.0)

    scale.ticks.forEach { tick ->
        val y = ((scale.maxDbm - tick) / dbSpan * height).toFloat().coerceIn(0f, height)
        drawLine(ScopeGrid.copy(alpha = 0.62f), Offset(0f, y), Offset(width, y), 1f)
    }
    for (index in 0..4) {
        val x = width * index / 4f
        drawLine(ScopeGrid.copy(alpha = 0.35f), Offset(x, 0f), Offset(x, height), 1f)
    }

    if (spectrum.powersDbm.size >= 2) {
        val fill = Path()
        val line = Path()
        var lastX = 0f
        spectrum.powersDbm.forEachIndexed { index, value ->
            val frequency = spectrum.frequenciesHz.getOrNull(index) ?: (startHz + spanHz * index / (spectrum.points - 1.0))
            val x = ((frequency - startHz) / spanHz * width).toFloat().coerceIn(0f, width)
            val normalized = ((value - scale.minDbm) / dbSpan).coerceIn(0.0, 1.0)
            val y = (height - normalized.toFloat() * height).coerceIn(0f, height)
            if (index == 0) {
                line.moveTo(x, y)
                fill.moveTo(x, height)
                fill.lineTo(x, y)
            } else {
                line.lineTo(x, y)
                fill.lineTo(x, y)
            }
            lastX = x
        }
        fill.lineTo(lastX, height)
        fill.close()
        drawPath(fill, Color(0xFF00CFFF).copy(alpha = 0.12f))
        drawPath(line, Color(0xFF00CFFF).copy(alpha = 0.22f), style = Stroke(5f, cap = StrokeCap.Round))

        for (index in 1 until spectrum.powersDbm.size) {
            val previous = spectrum.powersDbm[index - 1]
            val current = spectrum.powersDbm[index]
            val x1 = frequencyToX(spectrum.frequenciesHz.getOrNull(index - 1) ?: startHz, startHz, spanHz, width)
            val x2 = frequencyToX(spectrum.frequenciesHz.getOrNull(index) ?: stopHz, startHz, spanHz, width)
            val y1 = powerToY(previous, scale, dbSpan, height)
            val y2 = powerToY(current, scale, dbSpan, height)
            val level = ((previous + current) / 2.0 - scale.minDbm) / dbSpan
            drawLine(
                color = smartSpectrumColor(level.toFloat()),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 2.3f,
                cap = StrokeCap.Round,
            )
        }
    }

    if (thresholdEnabled && thresholdDbm in scale.minDbm..scale.maxDbm) {
        val y = ((scale.maxDbm - thresholdDbm) / dbSpan * height).toFloat().coerceIn(0f, height)
        var x = 0f
        while (x < width) {
            drawLine(
                color = ScopeAmber.copy(alpha = 0.9f),
                start = Offset(x, y),
                end = Offset((x + 10f).coerceAtMost(width), y),
                strokeWidth = 1.4f,
            )
            x += 16f
        }
    }

    markers.filter { it.enabled && it.frequencyHz in spectrum.startHz..spectrum.stopHz }.forEach { marker ->
        val x = ((marker.frequencyHz.toDouble() - startHz) / spanHz * width).toFloat().coerceIn(0f, width)
        val color = if (marker.id == activeMarkerId) ScopeGreen else ScopeCyan.copy(alpha = 0.55f)
        drawLine(color.copy(alpha = 0.55f), Offset(x, 0f), Offset(x, height), 1f)
        drawCircle(color, radius = 2.5f, center = Offset(x, 4f))
    }

    val peakX = ((spectrum.peakHz - startHz) / spanHz * width).toFloat().coerceIn(0f, width)
    val peakY = powerToY(spectrum.peakDbm, scale, dbSpan, height)
    drawCircle(ScopeRed.copy(alpha = 0.22f), radius = 7f, center = Offset(peakX, peakY))
    drawCircle(ScopeRed, radius = 2.8f, center = Offset(peakX, peakY))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmartWaterfall(
    frames: List<TinySaWaterfallRow>,
    displayStartHz: Long,
    displayStopHz: Long,
    scale: SmartDbScale,
) {
    val width = size.width
    val height = size.height
    val displaySpan = (displayStopHz - displayStartHz).coerceAtLeast(1L).toDouble()
    val dbSpan = (scale.maxDbm - scale.minDbm).coerceAtLeast(1.0)
    drawRect(ScopeBlack)

    frames.forEachIndexed { displayIndex, row ->
        val source = frames[frames.lastIndex - displayIndex]
        val rowStart = if (source.startHz > 0L && source.stopHz > source.startHz) source.startHz else displayStartHz
        val rowStop = if (source.startHz > 0L && source.stopHz > source.startHz) source.stopHz else displayStopHz
        val rowSpan = (rowStop - rowStart).coerceAtLeast(1L).toDouble()
        val y = height * displayIndex / frames.size
        val rowCellHeight = height / frames.size + 0.35f
        val values = source.powersDbm
        if (values.isEmpty()) return@forEachIndexed
        if (values.size == 1) {
            drawRect(smartSpectrumColor(((values[0] - scale.minDbm) / dbSpan).toFloat()), Offset(0f, y), Size(width, rowCellHeight))
            return@forEachIndexed
        }
        values.indices.forEach { index ->
            val f0 = rowStart + rowSpan * index / (values.size - 1.0)
            val f1 = rowStart + rowSpan * (index + 1) / (values.size - 1.0)
            val left = ((f0 - displayStartHz) / displaySpan * width).toFloat().coerceIn(0f, width)
            val right = ((f1 - displayStartHz) / displaySpan * width).toFloat().coerceIn(0f, width)
            if (right > left) {
                val normalized = ((values[index] - scale.minDbm) / dbSpan).toFloat()
                drawRect(smartSpectrumColor(normalized), Offset(left, y), Size((right - left).coerceAtLeast(0.7f), rowCellHeight))
            }
        }
    }
}

private fun frequencyToX(frequency: Double, startHz: Double, spanHz: Double, width: Float): Float =
    ((frequency - startHz) / spanHz * width).toFloat().coerceIn(0f, width)

private fun powerToY(powerDbm: Double, scale: SmartDbScale, dbSpan: Double, height: Float): Float =
    (height - ((powerDbm - scale.minDbm) / dbSpan).toFloat().coerceIn(0f, 1f) * height).coerceIn(0f, height)

private fun smartDbScale(
    spectrum: TinySaSpectrum,
    thresholdEnabled: Boolean,
    thresholdDbm: Double,
): SmartDbScale {
    var low = spectrum.noiseFloorDbm - 12.0
    var high = spectrum.peakDbm + 10.0
    if (thresholdEnabled) {
        low = minOf(low, thresholdDbm - 5.0)
        high = maxOf(high, thresholdDbm + 5.0)
    }
    if (high - low < 50.0) {
        val center = (high + low) / 2.0
        low = center - 25.0
        high = center + 25.0
    }
    low = (floor(low / 10.0) * 10.0).coerceIn(-150.0, -20.0)
 high = ceil(high / 10.0) * 10.0
 if (high < low + 40.0) high = low + 40.0
 if (high > 10.0) high = 10.0
 val ticks = (0..5).map { high - (high - low) * it / 5.0 }
    return SmartDbScale(low, high, ticks)
}

private val smartSpectrumStops = listOf(
    0.00f to Color(0xFF00152E),
    0.18f to Color(0xFF004A9F),
    0.36f to Color(0xFF00B8E8),
    0.54f to Color(0xFF00E676),
    0.72f to Color(0xFFFFEB3B),
    0.86f to Color(0xFFFF8A00),
    1.00f to Color(0xFFFF2A2A),
)

private fun smartSpectrumColor(level: Float): Color {
    val bounded = level.coerceIn(0f, 1f)
    for (index in 1 until smartSpectrumStops.size) {
        val (rightLevel, rightColor) = smartSpectrumStops[index]
        if (bounded <= rightLevel) {
            val (leftLevel, leftColor) = smartSpectrumStops[index - 1]
            val fraction = ((bounded - leftLevel) / (rightLevel - leftLevel).coerceAtLeast(0.0001f))
            return blendColor(leftColor, rightColor, fraction)
        }
    }
    return smartSpectrumStops.last().second
}

private fun blendColor(from: Color, to: Color, fraction: Float): Color = Color(
    red = from.red + (to.red - from.red) * fraction,
    green = from.green + (to.green - from.green) * fraction,
    blue = from.blue + (to.blue - from.blue) * fraction,
    alpha = 1f,
)

private fun formatAxisHz(hz: Long): String = if (hz >= 1_000_000_000L) {
    String.format(Locale.US, "%.3fG", hz / 1_000_000_000.0)
} else {
    String.format(Locale.US, "%.1fM", hz / 1_000_000.0)
}

@Composable
private fun TinySaSettingsDialog(
    state: TinySaUiState,
    onStartLive: () -> Unit,
    onStopLive: () -> Unit,
    onToggleTone: () -> Unit,
    onSetRange: (Long, Long, Int) -> Unit,
    onSetRbw: (Int?) -> Unit,
    onSetAverage: (TinySaAverageMode) -> Unit,
    onSetToneMode: (TinySaToneMode) -> Unit,
    onSetToneReference: (Double) -> Unit,
    onSetThresholdEnabled: (Boolean) -> Unit,
    onSetThresholdDbm: (Double) -> Unit,
    onToggleMarker: (Int) -> Unit,
    onSetMarkerFrequency: (Int, Long) -> Unit,
    onSetMarkerAutoPeak: (Int, Boolean) -> Unit,
    onSetActiveMarker: (Int) -> Unit,
    onScan: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "TINYSA MENU (\u9891\u8c31\u4eea\u83dc\u5355)",
                color = ScopeWhite,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TinySaControls(
                    state = state,
                    onStartLive = onStartLive,
                    onStopLive = onStopLive,
                    onToggleTone = onToggleTone,
                    onSetRange = onSetRange,
                    onSetRbw = onSetRbw,
                    onSetAverage = onSetAverage,
                    onSetToneMode = onSetToneMode,
                    onSetToneReference = onSetToneReference,
                    onSetThresholdEnabled = onSetThresholdEnabled,
                    onSetThresholdDbm = onSetThresholdDbm,
                    onToggleMarker = onToggleMarker,
                    onSetMarkerFrequency = onSetMarkerFrequency,
                    onSetMarkerAutoPeak = onSetMarkerAutoPeak,
                    onSetActiveMarker = onSetActiveMarker,
                    onScan = onScan,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE  (�ر�)", color = ScopeGreen, fontSize = 9.sp)
            }
        },
    )
}
