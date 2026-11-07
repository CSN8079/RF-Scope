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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rfscope.app.data.SignalSample
import com.rfscope.app.ui.theme.ScopeAmber
import com.rfscope.app.ui.theme.ScopeCyan
import com.rfscope.app.ui.theme.ScopeGreen
import com.rfscope.app.ui.theme.ScopeLine
import com.rfscope.app.ui.theme.ScopeMuted
import com.rfscope.app.ui.theme.ScopePanel
import com.rfscope.app.ui.theme.ScopeWhite
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SignalTrendCard(history: List<SignalSample>) {
    val rsrpValues = history.mapNotNull { it.rsrp?.toFloat() }
    val rsrqValues = history.mapNotNull { it.rsrq?.toFloat() }
    val sinrValues = history.mapNotNull { it.sinr?.toFloat() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = ScopePanel),
        border = BorderStroke(1.dp, ScopeLine),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        text = "SIGNAL TREND",
                        color = ScopeWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.3.sp,
                    )
                    Text(
                        text = "(信号趋势)  3 LINES IN PARALLEL",
                        color = ScopeMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                    )
                }
                Text(
                    text = "${history.size.coerceAtMost(60)} / 60 SAMPLES",
                    color = ScopeMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                )
            }

            if (history.isEmpty()) {
                Text(
                    text = "WAITING FOR SIGNAL SAMPLES  (等待信号采样)",
                    color = ScopeMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                )
            } else {
                SparkRow(
                    label = "RSRP",
                    zh = "参考信号接收功率",
                    values = rsrpValues,
                    range = -140f..-40f,
                    current = history.lastOrNull { it.rsrp != null }?.rsrp,
                    unit = "dBm",
                    color = ScopeGreen,
                )
                SparkRow(
                    label = "RSRQ",
                    zh = "参考信号接收质量",
                    values = rsrqValues,
                    range = -20f..0f,
                    current = history.lastOrNull { it.rsrq != null }?.rsrq,
                    unit = "dB",
                    color = ScopeAmber,
                )
                SparkRow(
                    label = "SINR",
                    zh = "信噪比",
                    values = sinrValues,
                    range = -20f..30f,
                    current = history.lastOrNull { it.sinr != null }?.sinr,
                    unit = "dB",
                    color = ScopeCyan,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 42.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatTrendTime(history.firstOrNull()?.timestampMs),
                        color = ScopeMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                    )
                    Text(
                        text = "TIME  (时间)",
                        color = ScopeMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                    )
                    Text(
                        text = formatTrendTime(history.lastOrNull()?.timestampMs),
                        color = ScopeMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SparkRow(
    label: String,
    zh: String,
    values: List<Float>,
    range: ClosedFloatingPointRange<Float>,
    current: Int?,
    unit: String,
    color: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = label,
                color = color,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "($zh)",
                color = ScopeMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = current?.let { "$it $unit" } ?: "—",
                color = color,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .width(36.dp)
                    .height(54.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(axisLabel(range.endInclusive), color = ScopeMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text(axisLabel((range.start + range.endInclusive) / 2f), color = ScopeMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text(axisLabel(range.start), color = ScopeMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.width(6.dp))
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
            ) {
                for (index in 0..4) {
                    val y = size.height * index / 4f
                    drawLine(
                        color = ScopeLine,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                    )
                }
                for (index in 0..4) {
                    val x = size.width * index / 4f
                    drawLine(
                        color = ScopeLine.copy(alpha = 0.65f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f,
                    )
                }
                if (values.size >= 2) {
                    val path = Path()
                    values.forEachIndexed { index, value ->
                        val x = index / (values.size - 1f) * size.width
                        val normalized = ((value - range.start) / (range.endInclusive - range.start))
                            .coerceIn(0f, 1f)
                        val y = size.height - normalized * size.height
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 2f, cap = StrokeCap.Round),
                    )
                } else if (values.size == 1) {
                    drawCircle(
                        color = color,
                        radius = 3f,
                        center = Offset(size.width / 2f, size.height / 2f),
                    )
                }
            }
        }
    }
}

private fun axisLabel(value: Float): String = value.toInt().toString()

private fun formatTrendTime(epochMs: Long?): String {
    if (epochMs == null) return "--:--"
    return Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
}
