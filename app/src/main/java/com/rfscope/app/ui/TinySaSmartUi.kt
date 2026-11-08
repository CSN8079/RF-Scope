package com.rfscope.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rfscope.app.data.TinySaAverageMode
import com.rfscope.app.data.TinySaConnectionStatus
import com.rfscope.app.data.TinySaMarker
import com.rfscope.app.data.TinySaSpectrum
import com.rfscope.app.data.TinySaToneMode
import com.rfscope.app.data.TinySaUiState
import com.rfscope.app.data.TinySaWaterfallRow
import com.rfscope.app.ui.theme.ScopeAmber
import com.rfscope.app.ui.theme.ScopeBlack
import com.rfscope.app.ui.theme.ScopeCyan
import com.rfscope.app.ui.theme.ScopeGreen
import com.rfscope.app.ui.theme.ScopeGrid
import com.rfscope.app.ui.theme.ScopeLine
import com.rfscope.app.ui.theme.ScopeMuted
import com.rfscope.app.ui.theme.ScopePanel
import com.rfscope.app.ui.theme.ScopeRed
import com.rfscope.app.ui.theme.ScopeWhite
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private val SdrPlotGutter = 48.dp
private val SdrSpectrumHeight = 168.dp
private val SdrWaterfallHeight = 240.dp
private const val SdrWaterfallVisibleRows = 120
private val SdrWaterfallScale = SdrDbScale(-120.0, -20.0, listOf(-20.0, -40.0, -60.0, -80.0, -100.0, -120.0))

private data class SdrDbScale(
    val minDbm: Double,
    val maxDbm: Double,
    val ticks: List<Double>,
)

@Composable
fun TinySaMainControls(
    state: TinySaUiState,
    onStartLive: () -> Unit,
    onStopLive: () -> Unit,
    onScan: () -> Unit,
    onToggleTone: () -> Unit,
    onSetToneMode: (TinySaToneMode) -> Unit,
    onSetToneReference: (Double) -> Unit,
    onOpenMenu: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniActionButton(
                label = if (state.isLive) "STOP" else "LIVE",
                active = state.isLive,
                activeColor = ScopeRed,
                modifier = Modifier.weight(1f),
                onClick = if (state.isLive) onStopLive else onStartLive,
            )
            MiniActionButton(
                label = "SINGLE",
                enabled = !state.isLive,
                modifier = Modifier.weight(1f),
                onClick = onScan,
            )
            MiniActionButton(
                label = "TONE",
                active = state.isToneEnabled,
                activeColor = ScopeAmber,
                modifier = Modifier.weight(1f),
                onClick = onToggleTone,
            )
            MiniActionButton(
                label = "MENU",
                active = true,
                activeColor = ScopeCyan,
                modifier = Modifier.weight(1f),
                onClick = onOpenMenu,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(
                "PEAK" to TinySaToneMode.PEAK,
                "AVG" to TinySaToneMode.AVERAGE,
                "MARK" to TinySaToneMode.MARKER,
            ).forEach { (label, mode) ->
                MiniActionButton(
                    label = label,
                    active = state.toneMode == mode,
                    activeColor = ScopeGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { onSetToneMode(mode) },
                )
            }
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
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp),
            )
        }
    }
}
@Composable
private fun MiniActionButton(
    label: String,
    active: Boolean = false,
    activeColor: Color = ScopeGreen,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(28.dp),
        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
    ) {
        Text(
            text = label,
            color = when {
                !enabled -> ScopeMuted.copy(alpha = 0.45f)
                active -> activeColor
                else -> ScopeMuted
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
fun TinySaSdrDisplay(state: TinySaUiState) {
    val spectrum = state.spectrum ?: return
    val scale = if (state.spectrumScaleAuto) {
        sdrAutoScale(spectrum, state.thresholdEnabled, state.thresholdDbm)
    } else {
        sdrManualScale(state.spectrumScaleMinDbm, state.spectrumScaleMaxDbm)
    }
    val visibleRows = state.waterfall.takeLast(SdrWaterfallVisibleRows)
    val waterfallRenderer = remember { SdrWaterfallRenderer() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScopeBlack),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "SMART SDR  (\u9891\u8c31\u4e0e\u7011\u5e03)",
                color = ScopeCyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (state.spectrumScaleAuto) {
                    "AUTO ${scale.minDbm.toInt()} \u81f3 ${scale.maxDbm.toInt()} dBm"
                } else {
                    "MANUAL ${scale.minDbm.toInt()} \u81f3 ${scale.maxDbm.toInt()} dBm"
                },
                color = if (state.spectrumScaleAuto) ScopeMuted else ScopeAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(SdrSpectrumHeight),
        ) {
            SdrDbAxis(scale)
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(ScopeBlack),
            ) {
                drawSdrSpectrum(
                    spectrum = spectrum,
                    scale = scale,
                    thresholdEnabled = state.thresholdEnabled,
                    thresholdDbm = state.thresholdDbm,
                    markers = state.markers,
                    activeMarkerId = state.activeMarkerId,
                )
            }
        }

        SdrFrequencyAxis(
            startHz = spectrum.startHz,
            stopHz = spectrum.stopHz,
            label = "FREQUENCY  (\u9891\u7387)",
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
                    .height(SdrWaterfallHeight),
            ) {
                SdrTimeRail(
                    frames = visibleRows,
                    totalHeight = SdrWaterfallHeight,
                )
                SdrWaterfallPlot(
                    frames = visibleRows,
                    displayStartHz = spectrum.startHz,
                    displayStopHz = spectrum.stopHz,
                    renderer = waterfallRenderer,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
            SdrFrequencyAxis(
                startHz = spectrum.startHz,
                stopHz = spectrum.stopHz,
                label = "WATERFALL  (\u7011\u5e03)",
            )
        }

        SdrLegend()
    }
}

@Composable
private fun SdrDbAxis(scale: SdrDbScale) {
    Column(
        modifier = Modifier
            .width(SdrPlotGutter)
            .height(SdrSpectrumHeight)
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
private fun SdrFrequencyAxis(startHz: Long, stopHz: Long, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(SdrPlotGutter))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(sdrFormatHz(startHz), color = ScopeMuted, fontFamily = FontFamily.Monospace, fontSize = 7.sp)
            Text(label, color = ScopeMuted, fontFamily = FontFamily.Monospace, fontSize = 7.sp)
            Text(sdrFormatHz(stopHz), color = ScopeMuted, fontFamily = FontFamily.Monospace, fontSize = 7.sp)
        }
    }
}

@Composable
private fun SdrTimeRail(frames: List<TinySaWaterfallRow>, totalHeight: androidx.compose.ui.unit.Dp) {
    val newestTime = frames.lastOrNull()?.timestampMs ?: 0L
    Box(
        modifier = Modifier
            .width(SdrPlotGutter)
            .height(totalHeight),
    ) {
        frames.indices.filter { it % 20 == 0 }.forEach { rowIndex ->
            val source = frames[frames.lastIndex - rowIndex]
            val seconds = ((newestTime - source.timestampMs) / 1000L).coerceAtLeast(0L)
            Text(
                text = sdrTimeLabel(seconds),
                color = ScopeMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 7.sp,
                modifier = Modifier
                    .width(SdrPlotGutter - 4.dp)
                    .offset(y = (SdrWaterfallHeight / SdrWaterfallVisibleRows) * rowIndex - 3.dp),
            )
        }
    }
}

@Composable
private fun SdrWaterfallPlot(
    frames: List<TinySaWaterfallRow>,
    displayStartHz: Long,
    displayStopHz: Long,
    renderer: SdrWaterfallRenderer,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.background(ScopeBlack)) {
        val widthPx = size.width.roundToInt().coerceAtLeast(1)
        val heightPx = size.height.roundToInt().coerceAtLeast(1)
        renderer.render(frames, displayStartHz, displayStopHz, widthPx, heightPx)?.let { image ->
            drawImage(image)
        }
    }
}

@Composable
private fun SdrLegend() {
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
                    .background(sdrColor(level)),
            )
        }
        Text("STRONG", color = ScopeMuted, fontFamily = FontFamily.Monospace, fontSize = 7.sp)
    }
}

private fun DrawScope.drawSdrSpectrum(
    spectrum: TinySaSpectrum,
    scale: SdrDbScale,
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
            val frequency = spectrum.frequenciesHz.getOrNull(index) ?: spectrum.startHz.toDouble()
            val x = sdrFrequencyToX(frequency, startHz, spanHz, width)
            val y = sdrPowerToY(value, scale, dbSpan, height)
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
        drawPath(fill, Color(0xFF00CFFF).copy(alpha = 0.10f))
        drawPath(line, Color(0xFF00CFFF).copy(alpha = 0.20f), style = Stroke(5f, cap = StrokeCap.Round))

        for (index in 1 until spectrum.powersDbm.size) {
            val previous = spectrum.powersDbm[index - 1]
            val current = spectrum.powersDbm[index]
            val x1 = sdrFrequencyToX(spectrum.frequenciesHz.getOrNull(index - 1) ?: startHz, startHz, spanHz, width)
            val x2 = sdrFrequencyToX(spectrum.frequenciesHz.getOrNull(index) ?: stopHz, startHz, spanHz, width)
            val y1 = sdrPowerToY(previous, scale, dbSpan, height)
            val y2 = sdrPowerToY(current, scale, dbSpan, height)
            val level = ((previous + current) / 2.0 - scale.minDbm) / dbSpan
            drawLine(
                color = sdrColor(level.toFloat()),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 2.2f,
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
                strokeWidth = 1.3f,
            )
            x += 16f
        }
    }

    markers.filter { it.enabled && it.frequencyHz in spectrum.startHz..spectrum.stopHz }.forEach { marker ->
        val x = sdrFrequencyToX(marker.frequencyHz.toDouble(), startHz, spanHz, width)
        val color = if (marker.id == activeMarkerId) ScopeGreen else ScopeCyan.copy(alpha = 0.55f)
        drawLine(color.copy(alpha = 0.55f), Offset(x, 0f), Offset(x, height), 1f)
        drawCircle(color, radius = 2.5f, center = Offset(x, 4f))
    }

    val peakX = sdrFrequencyToX(spectrum.peakHz, startHz, spanHz, width)
    val peakY = sdrPowerToY(spectrum.peakDbm, scale, dbSpan, height)
    drawCircle(ScopeRed.copy(alpha = 0.22f), radius = 7f, center = Offset(peakX, peakY))
    drawCircle(ScopeRed, radius = 2.8f, center = Offset(peakX, peakY))
}

private class SdrWaterfallRenderer {
    private var front: Bitmap? = null
    private var back: Bitmap? = null
    private var paint = AndroidPaint()
    private var lastTimestamp = 0L
    private var lastWidth = 0
    private var lastHeight = 0
    private var lastCount = 0
    private var lastStartHz = 0L
    private var lastStopHz = 0L

    fun render(
        frames: List<TinySaWaterfallRow>,
        startHz: Long,
        stopHz: Long,
        width: Int,
        height: Int,
    ): androidx.compose.ui.graphics.ImageBitmap? {
        if (frames.isEmpty() || width <= 0 || height <= 0) return null
        val newest = frames.last().timestampMs
        val sameRange = startHz == lastStartHz && stopHz == lastStopHz
        val canShift = front != null &&
            sameRange &&
            width == lastWidth &&
            height == lastHeight &&
            newest != lastTimestamp &&
            frames.size >= lastCount &&
            frames.size - lastCount <= 1

        if (canShift) {
            shift(frames, startHz, stopHz, width, height)
        } else if (newest != lastTimestamp || !sameRange || width != lastWidth || height != lastHeight) {
            rebuild(frames, startHz, stopHz, width, height)
        }

        lastTimestamp = newest
        lastWidth = width
        lastHeight = height
        lastCount = frames.size
        lastStartHz = startHz
        lastStopHz = stopHz
        return front?.asImageBitmap()
    }

    private fun rebuild(
        frames: List<TinySaWaterfallRow>,
        startHz: Long,
        stopHz: Long,
        width: Int,
        height: Int,
    ) {
        val target = ensureBitmap(front, width, height)
        val canvas = AndroidCanvas(target)
        canvas.drawColor(android.graphics.Color.BLACK)
        val rowHeight = height.toFloat() / frames.size
        frames.indices.forEach { rowIndex ->
            val source = frames[frames.lastIndex - rowIndex]
            drawRow(canvas, source, startHz, stopHz, rowIndex * rowHeight, rowHeight, width)
        }
        front = target
    }

    private fun shift(
        frames: List<TinySaWaterfallRow>,
        startHz: Long,
        stopHz: Long,
        width: Int,
        height: Int,
    ) {
        val destination = ensureBitmap(back, width, height)
        val canvas = AndroidCanvas(destination)
        canvas.drawColor(android.graphics.Color.BLACK)
        front?.let { canvas.drawBitmap(it, 0f, height.toFloat() / frames.size, null) }
        drawRow(canvas, frames.last(), startHz, stopHz, 0f, height.toFloat() / frames.size, width)
        back = front
        front = destination
    }

    private fun drawRow(
        canvas: AndroidCanvas,
        row: TinySaWaterfallRow,
        displayStartHz: Long,
        displayStopHz: Long,
        top: Float,
        height: Float,
        width: Int,
    ) {
        val values = row.powersDbm
        if (values.isEmpty()) return
        val rowStart = if (row.startHz > 0L && row.stopHz > row.startHz) row.startHz else displayStartHz
        val rowStop = if (row.startHz > 0L && row.stopHz > row.startHz) row.stopHz else displayStopHz
        val rowSpan = (rowStop - rowStart).coerceAtLeast(1L).toDouble()
        val displaySpan = (displayStopHz - displayStartHz).coerceAtLeast(1L).toDouble()
        val dbSpan = (SdrWaterfallScale.maxDbm - SdrWaterfallScale.minDbm).coerceAtLeast(1.0)
        if (values.size == 1) {
            paint.color = sdrColor(((values[0] - SdrWaterfallScale.minDbm) / dbSpan).toFloat()).toArgb()
            canvas.drawRect(0f, top, width.toFloat(), top + height, paint)
            return
        }
        values.indices.forEach { index ->
            val f0 = rowStart + rowSpan * index / (values.size - 1.0)
            val f1 = rowStart + rowSpan * (index + 1) / (values.size - 1.0)
            val left = ((f0 - displayStartHz) / displaySpan * width).toFloat().coerceIn(0f, width.toFloat())
            val right = ((f1 - displayStartHz) / displaySpan * width).toFloat().coerceIn(0f, width.toFloat())
            if (right > left) {
                val normalized = ((values[index] - SdrWaterfallScale.minDbm) / dbSpan).toFloat()
                paint.color = sdrColor(normalized).toArgb()
                canvas.drawRect(left, top, right.coerceAtLeast(left + 0.65f), top + height + 0.25f, paint)
            }
        }
    }

    private fun ensureBitmap(source: Bitmap?, width: Int, height: Int): Bitmap {
        return if (source != null && source.width == width && source.height == height) {
            source
        } else {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
    }
}

private fun sdrAutoScale(
    spectrum: TinySaSpectrum,
    thresholdEnabled: Boolean,
    thresholdDbm: Double,
): SdrDbScale {
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
    return sdrScale(low, high)
}

private fun sdrManualScale(minDbm: Double, maxDbm: Double): SdrDbScale {
    var low = minDbm.coerceIn(-150.0, 10.0)
    var high = maxDbm.coerceIn(-140.0, 10.0)
    if (high < low + 10.0) {
        if (low <= 0.0) high = (low + 10.0).coerceAtMost(10.0) else high = 10.0
    }
    if (high <= low) low = high - 10.0
    return sdrScale(low, high)
}

private fun sdrScale(low: Double, high: Double): SdrDbScale {
    val ticks = (0..5).map { high - (high - low) * it / 5.0 }
    return SdrDbScale(low, high, ticks)
}

private val sdrColorStops = listOf(
    0.00f to Color(0xFF00152E),
    0.18f to Color(0xFF004A9F),
    0.36f to Color(0xFF00B8E8),
    0.54f to Color(0xFF00E676),
    0.72f to Color(0xFFFFEB3B),
    0.86f to Color(0xFFFF8A00),
    1.00f to Color(0xFFFF2A2A),
)

private fun sdrColor(level: Float): Color {
    val bounded = level.coerceIn(0f, 1f)
    for (index in 1 until sdrColorStops.size) {
        val (rightLevel, rightColor) = sdrColorStops[index]
        if (bounded <= rightLevel) {
            val (leftLevel, leftColor) = sdrColorStops[index - 1]
            val fraction = (bounded - leftLevel) / (rightLevel - leftLevel).coerceAtLeast(0.0001f)
            return Color(
                red = leftColor.red + (rightColor.red - leftColor.red) * fraction,
                green = leftColor.green + (rightColor.green - leftColor.green) * fraction,
                blue = leftColor.blue + (rightColor.blue - leftColor.blue) * fraction,
                alpha = 1f,
            )
        }
    }
    return sdrColorStops.last().second
}

private fun sdrFrequencyToX(frequency: Double, startHz: Double, spanHz: Double, width: Float): Float =
    ((frequency - startHz) / spanHz * width).toFloat().coerceIn(0f, width)

private fun sdrPowerToY(powerDbm: Double, scale: SdrDbScale, dbSpan: Double, height: Float): Float =
    (height - ((powerDbm - scale.minDbm) / dbSpan).toFloat().coerceIn(0f, 1f) * height).coerceIn(0f, height)

private fun sdrFormatHz(hz: Long): String = if (hz >= 1_000_000_000L) {
    String.format(Locale.US, "%.3fG", hz / 1_000_000_000.0)
} else {
    String.format(Locale.US, "%.1fM", hz / 1_000_000.0)
}

private fun sdrTimeLabel(seconds: Long): String = when {
    seconds == 0L -> "0s"
    seconds < 60L -> "-${seconds}s"
    seconds % 60L == 0L -> "-${seconds / 60L}m"
    else -> "-${seconds / 60L}m${seconds % 60L}s"
}

private fun sdrFormatMhzValue(hz: Long): String = String.format(Locale.US, "%.3f", hz / 1_000_000.0)

private data class SdrCarrierPreset(val name: String, val nameZh: String, val startHz: Long, val stopHz: Long)

private val sdrCarrierPresets = listOf(
    SdrCarrierPreset("CMCC n41", "\u4e2d\u56fd\u79fb\u52a8 n41", 2_515_000_000L, 2_675_000_000L),
    SdrCarrierPreset("CMCC n79", "\u4e2d\u56fd\u79fb\u52a8 n79", 4_800_000_000L, 4_900_000_000L),
    SdrCarrierPreset("CU n78", "\u4e2d\u56fd\u8054\u901a n78", 3_500_000_000L, 3_600_000_000L),
    SdrCarrierPreset("CT n78", "\u4e2d\u56fd\u7535\u4fe1 n78", 3_400_000_000L, 3_500_000_000L),
    SdrCarrierPreset("CT n1", "\u4e2d\u56fd\u7535\u4fe1 n1", 2_110_000_000L, 2_170_000_000L),
    SdrCarrierPreset("CBN n28", "\u4e2d\u56fd\u5e7f\u7535 n28", 758_000_000L, 788_000_000L),
)

@Composable
fun TinySaMenuDialogV2(
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
    onSetSpectrumScaleAuto: (Boolean) -> Unit,
    onSetSpectrumScaleMin: (Double) -> Unit,
    onSetSpectrumScaleMax: (Double) -> Unit,
    onToggleMarker: (Int) -> Unit,
    onSetMarkerFrequency: (Int, Long) -> Unit,
    onSetMarkerAutoPeak: (Int, Boolean) -> Unit,
    onSetActiveMarker: (Int) -> Unit,
    onScan: () -> Unit,
    onDismiss: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var showCarrier by rememberSaveable { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            color = ScopePanel,
            border = BorderStroke(1.dp, ScopeLine),
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "TINYSA MENU  (\u9891\u8c31\u4eea\u83dc\u5355)",
                        color = ScopeWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
                    ) {
                        Text("BACK  (\u8fd4\u56de)", color = ScopeGreen, fontSize = 8.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    listOf("FREQ", "LEVEL", "MARKER", "MEASURE", "CONFIG").forEachIndexed { index, label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .background(
                                    color = if (tab == index) ScopeGreen.copy(alpha = 0.14f) else Color.Transparent,
                                    shape = RoundedCornerShape(2.dp),
                                )
                                .clickable { tab = index },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                color = if (tab == index) ScopeGreen else ScopeMuted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 7.sp,
                                fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ScopeLine),
                )
                Text(
                    text = "SETTINGS / ${listOf("FREQ", "LEVEL", "MARKER", "MEASURE", "CONFIG")[tab]}",
                    color = ScopeCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                )

                when (tab) {
                    0 -> FreqMenuContent(
                        state = state,
                        onSetRange = onSetRange,
                        onShowCarrier = { showCarrier = true },
                    )
                    1 -> LevelMenuContent(
                        state = state,
                        onSetRbw = onSetRbw,
                        onSetAverage = onSetAverage,
                        onSetThresholdEnabled = onSetThresholdEnabled,
                        onSetThresholdDbm = onSetThresholdDbm,
                        onSetSpectrumScaleAuto = onSetSpectrumScaleAuto,
                        onSetSpectrumScaleMin = onSetSpectrumScaleMin,
                        onSetSpectrumScaleMax = onSetSpectrumScaleMax,
                    )
                    2 -> MarkerMenuContent(
                        state = state,
                        onToggleMarker = onToggleMarker,
                        onSetMarkerFrequency = onSetMarkerFrequency,
                        onSetMarkerAutoPeak = onSetMarkerAutoPeak,
                        onSetActiveMarker = onSetActiveMarker,
                    )
                    3 -> MeasureMenuContent(
                        state = state,
                        onStartLive = onStartLive,
                        onStopLive = onStopLive,
                        onToggleTone = onToggleTone,
                        onSetToneMode = onSetToneMode,
                        onSetToneReference = onSetToneReference,
                        onScan = onScan,
                    )
                    else -> ConfigMenuContent(state)
                }
            }
        }
    }

    if (showCarrier) {
        AlertDialog(
            onDismissRequest = { showCarrier = false },
            title = { Text("CARRIER BAND  (\u8f7d\u6ce2\u9891\u6bb5)", color = ScopeWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
            text = {
                Column {
                    sdrCarrierPresets.forEach { preset ->
                        TextButton(
                            onClick = {
                                onSetRange(preset.startHz, preset.stopHz, state.scanPoints)
                                showCarrier = false
                            },
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        ) {
                            Text(
                                "${preset.name}  ${preset.nameZh}  ${sdrFormatHz(preset.startHz)} - ${sdrFormatHz(preset.stopHz)}",
                                color = ScopeCyan,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCarrier = false }) {
                    Text("CLOSE", color = ScopeGreen, fontSize = 8.sp)
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FreqMenuContent(
    state: TinySaUiState,
    onSetRange: (Long, Long, Int) -> Unit,
    onShowCarrier: () -> Unit,
) {
    var startText by rememberSaveable { mutableStateOf(sdrFormatMhzValue(state.scanStartHz)) }
    var stopText by rememberSaveable { mutableStateOf(sdrFormatMhzValue(state.scanStopHz)) }
    var pointsText by rememberSaveable { mutableStateOf(state.scanPoints.toString()) }
    LaunchedEffect(state.scanStartHz, state.scanStopHz, state.scanPoints) {
        startText = sdrFormatMhzValue(state.scanStartHz)
        stopText = sdrFormatMhzValue(state.scanStopHz)
        pointsText = state.scanPoints.toString()
    }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            MenuField(startText, { startText = it }, "START MHz", Modifier.weight(1f))
            MenuField(stopText, { stopText = it }, "STOP MHz", Modifier.weight(1f))
            MenuField(pointsText, { pointsText = it.filter(Char::isDigit) }, "PTS", Modifier.width(62.dp))
            MiniActionButton(
                label = "SET",
                active = true,
                onClick = {
                    val start = startText.toDoubleOrNull()?.times(1_000_000.0)?.toLong()
                    val stop = stopText.toDoubleOrNull()?.times(1_000_000.0)?.toLong()
                    val points = pointsText.toIntOrNull()
                    if (start != null && stop != null && points != null) {
                        onSetRange(start, stop, points)
                    }
                },
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            listOf(
                "88-108M" to (88_000_000L to 108_000_000L),
                "144-148M" to (144_000_000L to 148_000_000L),
                "400-500M" to (400_000_000L to 500_000_000L),
                "U5 430-440M" to (430_000_000L to 440_000_000L),
                "2.4-2.5G" to (2_400_000_000L to 2_500_000_000L),
                "3.4-3.6G" to (3_400_000_000L to 3_600_000_000L),
            ).forEach { (label, range) ->
                MiniActionButton(
                    label = label,
                    modifier = Modifier.width(82.dp),
                    onClick = { onSetRange(range.first, range.second, state.scanPoints) },
                )
            }
            MiniActionButton(
                label = "CARRIER (\u8f7d\u6ce2)",
                active = true,
                activeColor = ScopeAmber,
                modifier = Modifier.width(82.dp),
                onClick = onShowCarrier,
            )
        }
        Text(
            "RANGE ${sdrFormatHz(state.scanStartHz)} - ${sdrFormatHz(state.scanStopHz)}  |  ${state.scanPoints} PTS",
            color = ScopeMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LevelMenuContent(
    state: TinySaUiState,
    onSetRbw: (Int?) -> Unit,
    onSetAverage: (TinySaAverageMode) -> Unit,
    onSetThresholdEnabled: (Boolean) -> Unit,
    onSetThresholdDbm: (Double) -> Unit,
    onSetSpectrumScaleAuto: (Boolean) -> Unit,
    onSetSpectrumScaleMin: (Double) -> Unit,
    onSetSpectrumScaleMax: (Double) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("SCALE", color = ScopeMuted, fontSize = 8.sp)
            MiniActionButton("AUTO", active = state.spectrumScaleAuto, onClick = { onSetSpectrumScaleAuto(true) })
            MiniActionButton("MANUAL", active = !state.spectrumScaleAuto, activeColor = ScopeAmber, onClick = { onSetSpectrumScaleAuto(false) })
            Text("${state.spectrumScaleMinDbm.toInt()} \u81f3 ${state.spectrumScaleMaxDbm.toInt()} dBm", color = ScopeAmber, fontSize = 8.sp)
        }
        if (!state.spectrumScaleAuto) {
            MenuSliderRow("TOP", state.spectrumScaleMaxDbm.toFloat(), -140f..10f) {
                onSetSpectrumScaleMax(it.toDouble())
            }
            MenuSliderRow("BOTTOM", state.spectrumScaleMinDbm.toFloat(), -150f..5f) {
                onSetSpectrumScaleMin(it.toDouble())
            }
        }
        Text("RBW", color = ScopeMuted, fontSize = 8.sp)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            listOf<Pair<String, Int?>>("AUTO" to null, "3k" to 3, "10k" to 10, "30k" to 30, "100k" to 100, "300k" to 300, "600k" to 600, "850k" to 850, "1M" to 1000).forEach { (label, value) ->
                MiniActionButton(
                    label = label,
                    active = state.rbwKhz == value,
                    modifier = Modifier.width(47.dp),
                    onClick = { onSetRbw(value) },
                )
            }
        }
        Text("VBW / AVG", color = ScopeMuted, fontSize = 8.sp)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MiniActionButton("OFF", active = state.averageMode == TinySaAverageMode.OFF, modifier = Modifier.width(52.dp), onClick = { onSetAverage(TinySaAverageMode.OFF) })
            MiniActionButton("4x", active = state.averageMode == TinySaAverageMode.AVER4, modifier = Modifier.width(52.dp), onClick = { onSetAverage(TinySaAverageMode.AVER4) })
            MiniActionButton("16x", active = state.averageMode == TinySaAverageMode.AVER16, modifier = Modifier.width(52.dp), onClick = { onSetAverage(TinySaAverageMode.AVER16) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            MiniActionButton(
                label = if (state.thresholdEnabled) "LIMIT ON" else "LIMIT OFF",
                active = state.thresholdEnabled,
                activeColor = ScopeAmber,
                onClick = { onSetThresholdEnabled(!state.thresholdEnabled) },
            )
            Text("${state.thresholdDbm.toInt()} dBm", color = ScopeAmber, fontSize = 8.sp)
            Slider(
                value = state.thresholdDbm.toFloat(),
                onValueChange = { onSetThresholdDbm(it.toDouble()) },
                valueRange = -120f..-10f,
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp),
            )
        }
    }
}

@Composable
private fun MarkerMenuContent(
    state: TinySaUiState,
    onToggleMarker: (Int) -> Unit,
    onSetMarkerFrequency: (Int, Long) -> Unit,
    onSetMarkerAutoPeak: (Int, Boolean) -> Unit,
    onSetActiveMarker: (Int) -> Unit,
) {
    val active = state.markers.firstOrNull { it.id == state.activeMarkerId }
    var markerText by rememberSaveable { mutableStateOf(sdrFormatMhzValue(active?.frequencyHz ?: state.scanStartHz)) }
    LaunchedEffect(state.activeMarkerId, active?.frequencyHz) {
        markerText = sdrFormatMhzValue(active?.frequencyHz ?: state.scanStartHz)
    }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("MARKER", color = ScopeMuted, fontSize = 8.sp)
            state.markers.forEach { marker ->
                MiniActionButton(
                    label = "M${marker.id}",
                    active = marker.id == state.activeMarkerId,
                    activeColor = if (marker.enabled) ScopeGreen else ScopeCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { if (marker.enabled) onSetActiveMarker(marker.id) else onToggleMarker(marker.id) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("MODE", color = ScopeMuted, fontSize = 8.sp)
            MiniActionButton(
                label = "AUTO PEAK",
                active = active?.autoPeak == true,
                activeColor = ScopeAmber,
                modifier = Modifier.weight(1f),
                onClick = { onSetMarkerAutoPeak(state.activeMarkerId, true) },
            )
            MiniActionButton(
                label = "MANUAL",
                active = active?.autoPeak == false,
                modifier = Modifier.weight(1f),
                onClick = { onSetMarkerAutoPeak(state.activeMarkerId, false) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            MenuField(markerText, { markerText = it }, "M${state.activeMarkerId} MHz", Modifier.weight(1f))
            MiniActionButton("SET M${state.activeMarkerId}", active = true, onClick = {
                markerText.toDoubleOrNull()?.times(1_000_000.0)?.toLong()?.let { onSetMarkerFrequency(state.activeMarkerId, it) }
            })
        }
        Text("M1-M4: tap to activate, long menu is not required", color = ScopeMuted, fontSize = 7.sp)
    }
}

@Composable
private fun MeasureMenuContent(
    state: TinySaUiState,
    onStartLive: () -> Unit,
    onStopLive: () -> Unit,
    onToggleTone: () -> Unit,
    onSetToneMode: (TinySaToneMode) -> Unit,
    onSetToneReference: (Double) -> Unit,
    onScan: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            MiniActionButton(if (state.isLive) "STOP LIVE" else "LIVE", active = state.isLive, activeColor = ScopeRed, onClick = if (state.isLive) onStopLive else onStartLive)
            MiniActionButton("SINGLE", enabled = !state.isLive, onClick = onScan)
            MiniActionButton("TONE", active = state.isToneEnabled, activeColor = ScopeAmber, onClick = onToggleTone)
            MiniActionButton("PEAK", active = state.toneMode == TinySaToneMode.PEAK, onClick = { onSetToneMode(TinySaToneMode.PEAK) })
            MiniActionButton("AVG", active = state.toneMode == TinySaToneMode.AVERAGE, onClick = { onSetToneMode(TinySaToneMode.AVERAGE) })
            MiniActionButton("MARKER", active = state.toneMode == TinySaToneMode.MARKER, onClick = { onSetToneMode(TinySaToneMode.MARKER) })
        }
        MenuSliderRow("TONE REF", state.toneReferenceDbm.toFloat(), -100f..-10f) {
            onSetToneReference(it.toDouble())
        }
        Text("LIVE is optimized for fluid updates; TONE follows the selected measurement source.", color = ScopeMuted, fontSize = 7.sp)
    }
}

@Composable
private fun ConfigMenuContent(state: TinySaUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            "STATUS ${state.status.name}",
            color = if (state.status == TinySaConnectionStatus.CONNECTED) ScopeGreen else ScopeAmber,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
        )
        Text(
            "RANGE ${sdrFormatHz(state.scanStartHz)} - ${sdrFormatHz(state.scanStopHz)}  |  ${state.scanPoints} PTS",
            color = ScopeMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
        )
        Text(
            "LAST ${state.lastScanDurationMs ?: 0} ms  |  WATERFALL ${state.waterfall.size}/240  |  RBW ${state.rbwKhz?.toString() ?: "AUTO"}  |  AVG ${state.averageMode.name}",
            color = ScopeMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
        )
        state.messageEn?.let {
            Text("$it ${state.messageZh ?: ""}", color = ScopeMuted, fontFamily = FontFamily.Monospace, fontSize = 8.sp)
        }
    }
}

@Composable
private fun MenuSliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = ScopeMuted, fontFamily = FontFamily.Monospace, fontSize = 8.sp)
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier
                .weight(1f)
                .height(28.dp),
        )
        Text(value.toInt().toString(), color = ScopeAmber, fontFamily = FontFamily.Monospace, fontSize = 8.sp)
    }
}

@Composable
private fun MenuField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(60.dp),
        singleLine = true,
        label = { Text(label, fontSize = 7.sp, fontFamily = FontFamily.Monospace) },
        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ScopeWhite),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}
