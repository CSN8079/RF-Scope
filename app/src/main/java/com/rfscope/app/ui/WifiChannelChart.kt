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
fun WifiChannelChart(networks: List<WifiNetwork>, band: String) {
    val items = networks.filter { it.band == band && it.channel != null }
    if (items.isEmpty()) return

    val minChannel = items.minOf { it.channel!! }.toFloat()
    val maxChannel = items.maxOf { it.channel!! }.toFloat()
    val left = if (minChannel == maxChannel) minChannel - 1f else minChannel
    val right = if (minChannel == maxChannel) maxChannel + 1f else maxChannel
    val channelSpan = right - left
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

                    items.forEach { network ->
                        val channel = network.channel?.toFloat() ?: return@forEach
                        val centerX = ((channel - left) / channelSpan).coerceIn(0f, 1f) * size.width
                        val channelWidth = when (band) {
                            "2.4 GHz" -> ((network.widthMhz ?: 20) / 5f)
                            else -> ((network.widthMhz ?: 20) / 20f)
                        }.coerceAtLeast(0.5f)
                        val halfWidth = (channelWidth / channelSpan / 2f * size.width)
                            .coerceAtLeast(3f)
                        val normalized = ((network.levelDbm - yMin) / (yMax - yMin)).coerceIn(0f, 1f)
                        val top = size.height - normalized * size.height
                        val color = when {
                            network.levelDbm >= -60 -> ScopeGreen
                            network.levelDbm >= -75 -> ScopeAmber
                            else -> ScopeRed
                        }
                        drawRoundRect(
                            color = color.copy(alpha = 0.22f),
                            topLeft = Offset(centerX - halfWidth, top),
                            size = Size(halfWidth * 2f, size.height - top),
                            cornerRadius = CornerRadius(3f, 3f),
                        )
                        drawLine(
                            color = color,
                            start = Offset(centerX - halfWidth, top),
                            end = Offset(centerX + halfWidth, top),
                            strokeWidth = 2f,
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
