package com.rfscope.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rfscope.app.data.TinySaConnectionStatus
import com.rfscope.app.data.TinySaSpectrum
import com.rfscope.app.data.TinySaUiState
import com.rfscope.app.ui.theme.ScopeAmber
import com.rfscope.app.ui.theme.ScopeCyan
import com.rfscope.app.ui.theme.ScopeGreen
import com.rfscope.app.ui.theme.ScopeGrid
import com.rfscope.app.ui.theme.ScopeLine
import com.rfscope.app.ui.theme.ScopeMuted
import com.rfscope.app.ui.theme.ScopePanel
import com.rfscope.app.ui.theme.ScopeRed
import com.rfscope.app.ui.theme.ScopeWhite
import java.util.Locale

@Composable
fun TinySaPanel(
    state: TinySaUiState,
    onConnect: () -> Unit,
    onScan: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                if (spectrum != null) {
                    SpectrumSummary(spectrum)
                    TinySaSpectrumChart(spectrum)
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

