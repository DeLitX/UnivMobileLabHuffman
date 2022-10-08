package com.delitx.huffmancoding.ui.bar_chart.renderer.bar

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.delitx.huffmancoding.ui.bar_chart.BarChartData

interface BarDrawer {
    fun drawBar(
        drawScope: DrawScope,
        canvas: Canvas,
        barArea: Rect,
        bar: BarChartData.Bar
    )
}
