package com.scripthost.ui.chart

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View

/**
 * Dependency-free chart view rendering a simple LINE or BAR chart.
 *
 * All scaling math is delegated to [ChartScale]; the view stays a thin
 * drawing layer: horizontal grid lines with tick values on the left,
 * optional x-axis labels below, and the series drawn in [lineColor].
 */
class SimpleChartView(context: Context) : View(context) {

    enum class ChartType { LINE, BAR }

    var chartType: ChartType = ChartType.LINE
        set(value) {
            field = value
            invalidate()
        }

    /** Series values; non-number entries are dropped by the bridge. */
    var data: List<Float> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    /** Optional x-axis labels, aligned with [data] positions. */
    var labels: List<String> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    /** Color of the line/points or bars. Defaults to blue. */
    var lineColor: Int = 0xFF3B82F6.toInt()
        set(value) {
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE0E0E0.toInt()
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF616161.toInt()
        textSize = 10f * density
    }

    private val seriesPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f * density
        style = Paint.Style.STROKE
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val values = data
        if (values.isEmpty() || width == 0 || height == 0) return

        // Plot area, leaving room for tick values on the left and labels below
        val padLeft = 44f * density
        val padTop = 8f * density
        val padRight = 12f * density
        val padBottom = (if (labels.isEmpty()) 12f else 24f) * density

        val plotLeft = paddingLeft + padLeft
        val plotTop = paddingTop + padTop
        val plotRight = width - paddingRight - padRight
        val plotBottom = height - paddingBottom - padBottom
        if (plotRight <= plotLeft || plotBottom <= plotTop) return

        val plotWidth = plotRight - plotLeft
        val plotHeight = plotBottom - plotTop

        val (min, max) = ChartScale.rangeOf(values)

        fun yFor(value: Float): Float =
            plotBottom - ChartScale.normalize(value, min, max) * plotHeight

        // Grid lines and tick values
        for (tick in ChartScale.niceTicks(min, max)) {
            val y = yFor(tick)
            canvas.drawLine(plotLeft, y, plotRight, y, gridPaint)
            val text = formatTick(tick)
            canvas.drawText(text, plotLeft - textPaint.measureText(text) - 4f * density,
                y + textPaint.textSize / 3f, textPaint)
        }

        // X-axis labels, truncated to fit their slot
        if (labels.isNotEmpty()) {
            val slot = plotWidth / labels.size
            for (i in labels.indices) {
                val text = truncate(labels[i], slot - 4f * density)
                val x = plotLeft + slot * i + slot / 2f - textPaint.measureText(text) / 2f
                canvas.drawText(text, x, height - paddingBottom - 6f * density, textPaint)
            }
        }

        seriesPaint.color = lineColor
        pointPaint.color = lineColor

        if (chartType == ChartType.BAR) {
            val slot = plotWidth / values.size
            val barWidth = slot * 0.7f
            seriesPaint.style = Paint.Style.FILL
            for (i in values.indices) {
                val cx = plotLeft + slot * i + slot / 2f
                canvas.drawRect(cx - barWidth / 2f, yFor(values[i]),
                    cx + barWidth / 2f, plotBottom, seriesPaint)
            }
            seriesPaint.style = Paint.Style.STROKE
        } else {
            val step = if (values.size > 1) plotWidth / (values.size - 1) else 0f
            val path = Path()
            for (i in values.indices) {
                val x = if (values.size > 1) plotLeft + step * i else plotLeft + plotWidth / 2f
                val y = yFor(values[i])
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                canvas.drawCircle(x, y, 3f * density, pointPaint)
            }
            canvas.drawPath(path, seriesPaint)
        }
    }

    /** Whole numbers without decimals, everything else with one fraction digit. */
    private fun formatTick(value: Float): String =
        if (value == value.toLong().toFloat()) value.toLong().toString()
        else "%.1f".format(value)

    private fun truncate(text: String, maxWidth: Float): String {
        if (maxWidth <= 0f || textPaint.measureText(text) <= maxWidth) return text
        var result = text
        while (result.isNotEmpty() && textPaint.measureText("$result…") > maxWidth) {
            result = result.dropLast(1)
        }
        return if (result.isEmpty()) "…" else "$result…"
    }
}
