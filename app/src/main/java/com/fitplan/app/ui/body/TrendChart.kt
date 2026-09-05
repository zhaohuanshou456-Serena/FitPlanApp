package com.fitplan.app.ui.body

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import com.fitplan.app.ui.common.smart

/** 简单折线图：横轴按记录顺序等距；点击某节点可显示其数值 */
@Composable
fun TrendChart(
    points: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier,
    height: Int = 200
) {
    val color = MaterialTheme.colorScheme.primary
    val accentColor = MaterialTheme.colorScheme.tertiary
    val emptyColor = MaterialTheme.colorScheme.onSurfaceVariant

    val selState = remember { mutableStateOf<Int?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .pointerInput(points) {
                val n = points.size
                detectTapGestures { offset ->
                    if (n > 1) {
                        val idx = ((offset.x / size.width) * (n - 1)).roundToInt().coerceIn(0, n - 1)
                        selState.value = idx
                    }
                }
            }
    ) {
        if (points.size < 2) {
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

        val n = points.size
        fun xOf(i: Int) = if (n <= 1) size.width / 2 else (i.toFloat() / (n - 1)) * size.width
        fun yOf(v: Double) = size.height - (((v - lo) / (hi - lo)) * size.height).toFloat()

        val path = Path()
        points.forEachIndexed { i, (_, v) ->
            val x = xOf(i)
            val y = yOf(v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 4f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )

        points.forEachIndexed { i, (_, v) ->
            drawCircle(color = color, radius = 6f, center = Offset(xOf(i), yOf(v)))
        }

        val lastIdx = n - 1
        drawCircle(
            color = accentColor,
            radius = 8f,
            center = Offset(xOf(lastIdx), yOf(points.last().second))
        )

        // 点击选中的节点：放大 + 显示数值
        selState.value?.let { idx ->
            val x = xOf(idx)
            val y = yOf(points[idx].second)
            drawCircle(color = accentColor, radius = 11f, center = Offset(x, y))
            drawCircle(color = Color.White, radius = 4f, center = Offset(x, y))
            val label = points[idx].second.smart()
            val paint = Paint().apply {
                this.isAntiAlias = true
                color = android.graphics.Color.WHITE
                textSize = 14.sp.value
            }
            drawContext.canvas.nativeCanvas.drawText(label, x + 8f, y - 8f, paint)
        }
    }
}
