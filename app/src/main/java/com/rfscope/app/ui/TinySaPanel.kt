package com.rfscope.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rfscope.app.data.TinySaAverageMode
import com.rfscope.app.data.TinySaConnectionStatus
import com.rfscope.app.data.TinySaSpectrum
import com.rfscope.app.data.TinySaToneMode
import com.rfscope.app.data.TinySaUiState
import com.rfscope.app.ui.theme.ScopeAmber
import com.rfscope.app.ui.theme.ScopeCyan
import com.rfscope.app.ui.theme.ScopeGreen
import com.rfscope.app.ui.theme.ScopeGreenDim
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
) {
    var showMenu by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "TINYSA ULTRA",
                    color = ScopeWhite,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "\u5b9e\u65f6\u9891\u8c31 / \u5c04\u9891\u76d1\u6d4b",
                    color = ScopeMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                )
            }
            when (state.status) {
                TinySaConnectionStatus.CONNECTED -> TextButton(
                    onClick = onScan,
                    enabled = !state.isLive,
                ) {
                    Text("SCAN", color = if (state.isLive) ScopeMuted else ScopeGreen, fontSize = 9.sp)
                }
                TinySaConnectionStatus.CONNECTING -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        color = ScopeAmber,
                        strokeWidth = 1.5.dp,
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("CONNECTING", color = ScopeAmber, fontSize = 8.sp)
                }
                else -> TextButton(onClick = onConnect) {
                    Text("CONNECT  (\u8fde\u63a5)", color = ScopeGreen, fontSize = 9.sp)
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
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when (state.status) {
                            TinySaConnectionStatus.CONNECTED -> "USB CDC CONNECTED"
                            TinySaConnectionStatus.CONNECTING -> "OPENING SERIAL PORT"
                            TinySaConnectionStatus.ERROR -> "TINYSA ERROR"
                            TinySaConnectionStatus.DISCONNECTED -> "USB CDC READY"
                        },
                        color = when (state.status) {
                            TinySaConnectionStatus.CONNECTED -> ScopeGreen
                            TinySaConnectionStatus.ERROR -> ScopeRed
                            TinySaConnectionStatus.CONNECTING -> ScopeAmber
                            TinySaConnectionStatus.DISCONNECTED -> ScopeMuted
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (state.isLive) "LIVE \u626b\u63cf\u4e2d" else "IDLE",
                        color = if (state.isLive) ScopeAmber else ScopeMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                    )
                }

                TinySaMainControls(
                    state = state,
                    onStartLive = onStartLive,
                    onStopLive = onStopLive,
                    onScan = onScan,
                    onToggleTone = onToggleTone,
                    onSetToneMode = onSetToneMode,
                    onSetToneReference = onSetToneReference,
                    onOpenMenu = { showMenu = true },
                )

                state.messageEn?.let { message ->
                    Text(
                        text = "$message  (${state.messageZh ?: ""})",
                        color = ScopeMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                    )
                }

                val spectrum = state.spectrum
                if (spectrum != null) {
                    SpectrumMiniSummary(spectrum)
                    TinySaSdrDisplay(state)
                } else {
                    Text(
                        text = "NO SPECTRUM DATA YET  (\u5c1a\u672a\u8bfb\u53d6\u9891\u8c31)",
                        color = ScopeMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                    )
                }
            }
        }
    }

    if (showMenu) {
        TinySaMenuDialogV2(
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
            onSetSpectrumScaleAuto = onSetSpectrumScaleAuto,
            onSetSpectrumScaleMin = onSetSpectrumScaleMin,
            onSetSpectrumScaleMax = onSetSpectrumScaleMax,
            onToggleMarker = onToggleMarker,
            onSetMarkerFrequency = onSetMarkerFrequency,
            onSetMarkerAutoPeak = onSetMarkerAutoPeak,
            onSetActiveMarker = onSetActiveMarker,
            onScan = onScan,
            onDismiss = { showMenu = false },
        )
    }
}

@Composable
private fun SpectrumMiniSummary(spectrum: TinySaSpectrum) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        MiniMetric("PEAK", formatMHz(spectrum.peakHz), formatDbm(spectrum.peakDbm) + " dBm")
        MiniMetric("NOISE", "FLOOR", formatDbm(spectrum.noiseFloorDbm) + " dBm")
        MiniMetric("SNR", formatDbm(spectrum.peakDbm - spectrum.noiseFloorDbm) + " dB", "${spectrum.points} pts")
    }
}

@Composable
private fun MiniMetric(label: String, value: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(label, color = ScopeMuted, fontFamily = FontFamily.Monospace, fontSize = 7.sp)
        Text(value, color = ScopeGreen, fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(detail, color = ScopeMuted, fontFamily = FontFamily.Monospace, fontSize = 7.sp)
    }
}

private fun formatMHz(hz: Double): String = String.format(Locale.US, "%.3f MHz", hz / 1_000_000.0)
private fun formatDbm(value: Double): String = String.format(Locale.US, "%.1f", value)
