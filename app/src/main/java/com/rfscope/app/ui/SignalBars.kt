package com.rfscope.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rfscope.app.ui.theme.ScopeGreen
import com.rfscope.app.ui.theme.ScopeLine

enum class SignalMetric {
    RSRP,
    RSRQ,
    SINR,
}

@Composable
fun SignalBars(
    bars: Int,
    modifier: Modifier = Modifier,
) {
    val count = bars.coerceIn(0, 3)
    val activeColor = when (count) {
        3 -> ScopeGreen
        2 -> Color(0xFFFFB74D)
        1 -> Color(0xFFFF4D4D)
        else -> ScopeLine
    }
    Row(
        modifier = modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((7 + index * 4).dp)
                    .background(
                        color = if (index < count) activeColor else ScopeLine,
                        shape = RoundedCornerShape(1.dp),
                    ),
            )
        }
    }
}

fun signalBarsForMetric(metric: SignalMetric, value: Int?): Int {
    if (value == null) return 0
    return when (metric) {
        SignalMetric.RSRP -> when {
            value >= -72 -> 3
            value >= -114 -> 2
            else -> 1
        }

        SignalMetric.RSRQ -> when {
            value >= -10 -> 3
            value >= -20 -> 2
            else -> 1
        }

        SignalMetric.SINR -> when {
            value >= 20 -> 3
            value >= 0 -> 2
            else -> 1
        }
    }
}

fun signalBarsLabel(metric: SignalMetric, value: Int?): String {
    val bars = signalBarsForMetric(metric, value)
    return when (bars) {
        3 -> "3/3 (满格)"
        2 -> "2/3 (两格)"
        1 -> "1/3 (一格)"
        else -> "0/3 (无数据)"
    }
}
