package com.rfscope.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rfscope.app.data.WifiNetwork
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
fun WifiChannelChart(
    networks: List<WifiNetwork>,
    band: String,
    selectedBssid: String?,
) {
    val items = networks.filter { it.band == band && it.channel != null }
    if (items.isEmpty()) return

    val (left, right) = when (band) {
        "2.4 GHz" -> 1f to 14f
        "5 GHz" -> 36f to 165f
        "6 GHz" -> 1f to 233f
        else -> items.minOf { it.channel!! }.toFloat() to items.maxOf { it.channel!! }.toFloat()
    }
    val channelSpan = (right - left).coerceAtLeast(1f)
    val yMin = -100f
    val yMax = -20f

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "$band CHANNEL MAP",
                    color = ScopeWhite,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${items.size} AP  (${items.size} 个热点)",
                    color = ScopeMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                )
            }
            Text(
                text = "CHANNEL OCCUPANCY  (信道占用)",
                color = ScopeMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .width(33.dp)
                        .height(132.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("-30", color = ScopeMuted, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                    Text("-50", color = ScopeMuted, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                    Text("-70", color = ScopeMuted, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                    Text("-90", color = ScopeMuted, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                }
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(132.dp),
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

                    items.sortedBy { it.bssid == selectedBssid }.forEach { network ->
                        val channel = network.channel?.toFloat() ?: return@forEach
                        val centerX = ((channel - left) / channelSpan).coerceIn(0f, 1f) * size.width
                        val channelWidth = when (band) {
                            "2.4 GHz" -> ((network.widthMhz ?: 20) / 5f)
                            else -> ((network.widthMhz ?: 20) / 20f)
                        }.coerceAtLeast(0.5f)
                        val halfWidthPx = (channelWidth / channelSpan / 2f * size.width)
                            .coerceAtLeast(4f)
                        val normalized = ((network.levelDbm - yMin) / (yMax - yMin)).coerceIn(0f, 1f)
                        val peakY = size.height - normalized * size.height
                        val bottomY = size.height
                        val color = when {
                            network.levelDbm >= -60 -> ScopeGreen
                            network.levelDbm >= -75 -> ScopeAmber
                            else -> ScopeRed
                        }
                        val isSelected = network.bssid == selectedBssid
                        val dimmed = selectedBssid != null && !isSelected
                        val strokeColor = if (dimmed) {
                            color.copy(alpha = 0.18f)
                        } else if (isSelected) {
                            ScopeGreen
                        } else {
                            color
                        }
                        val fillAlpha = when {
                            isSelected -> 0.30f
                            dimmed -> 0.03f
                            else -> 0.16f
                        }
                        val strokeWidth = if (isSelected) 3.5f else 1.8f
                        val path = Path()
                        val steps = 24
                        for (step in 0..steps) {
                            val ratio = step / steps.toFloat()
                            val offset = ratio * 2f - 1f
                            val x = (centerX + offset * halfWidthPx).coerceIn(0f, size.width)
                            val y = peakY + (bottomY - peakY) * offset * offset
                            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path = path, color = strokeColor.copy(alpha = fillAlpha))
                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 39.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val tickChannels = (0..4).map { index ->
                    (left + channelSpan * index / 4f).toInt()
                }
                tickChannels.forEach { channel ->
                    Text(
                        text = channel.toString(),
                        color = ScopeMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = "dBm",
                    color = ScopeMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                )
            }
        }
    }
}


