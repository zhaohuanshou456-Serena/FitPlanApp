package com.fitplan.app.ui.body

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/** 一个简单的折线图，x=时间戳，y=数值 */
@Composable
fun TrendChart(
    points: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier,
    height: Int = 200
) {
    val color = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier.fillMaxWidth().height(height.dp)) {
        if (points.size < 2) {
            // 数据不足提示
            drawLine(
                color = emptyColor.copy(alpha = 0.5f),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 2f
            )
            return@Canvas
        }
        val minY = points.minOf { it.second }
        val maxY = points.maxOf { it.second }
        val spanY = max(maxY - minY, 0.0001)
        val padY = spanY * 0.15
        val lo = minY - padY
        val hi = maxY + padY

        val first = points.first().first.toFloat()
        val last = points.last().first.toFloat()
        val xRange = max(last - first, 1f)

        fun xOf(t: Long) = ((t - first) / xRange) * size.width
        fun yOf(v: Double) = size.height - (((v - lo) / (hi - lo)) * size.height).toFloat()

        // 连线
        val path = Path()
        points.forEachIndexed { i, (t, v) ->
            val x = xOf(t)
            val y = yOf(v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 4f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )

        // 数据点
        points.forEach { (t, v) ->
            drawCircle(
                color = color,
                radius = 6f,
                center = Offset(xOf(t), yOf(v))
            )
        }
        // 最新值标签点
        val lastP = points.last()
        drawCircle(
            color = MaterialTheme.colorScheme.tertiary,
            radius = 8f,
            center = Offset(xOf(lastP.first), yOf(lastP.second))
        )
    }
}
