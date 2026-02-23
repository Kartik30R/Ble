package com.blesense.app.features.bluetooth.presentation.widget.graph

import android.content.Context
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.blesense.app.features.bluetooth.domain.model.AccPoint
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

@Composable
fun AccelerometerGraphCard(
    title: String,
    cur: Float?,
    hist: List<AccPoint>,
    lineCol: Color,
    curLabel: String,
    naLabel: String,
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)        ),
     ) {

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .fillMaxWidth()
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "$curLabel: ${cur?.let { "%.2f".format(it) } ?: naLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(Modifier.height(16.dp))

            OptimizedAccelerometerChart(
                hist = hist,
                lineColor = lineCol
            )
        }
    }
}

@Composable
private fun OptimizedAccelerometerChart(
    hist: List<AccPoint>,
    lineColor: Color
) {

    var lastCount by remember { mutableStateOf(0) }
    val axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),

        factory = { context ->

            LineChart(context).apply {

                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                setBackgroundColor(AndroidColor.TRANSPARENT)
                setDrawGridBackground(false)
                description.isEnabled = false
                setTouchEnabled(true)
                setPinchZoom(false)
                setScaleEnabled(false)
                legend.isEnabled = false
                isHighlightPerTapEnabled = true

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawLabels(false)
                    setDrawGridLines(false)
                    textColor = axisTextColor
                }

                axisRight.isEnabled = false

                axisLeft.apply {
                    axisMinimum = -20f
                    axisMaximum = 20f
                    textColor = axisTextColor
                    textSize = 10f
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                }

                marker = SimpleMarker(context, lineColor.toArgb())
                data = LineData()
            }
        },

        update = { lineChart ->

            lineChart.axisLeft.textColor = axisTextColor

            val data = lineChart.data ?: return@AndroidView
            var set = data.getDataSetByIndex(0)

            if (set == null) {
                set = LineDataSet(mutableListOf(), "").apply {
                    color = lineColor.toArgb()
                    lineWidth = 2.5f
                    setDrawCircles(false)
                    setDrawValues(false)
                    setDrawFilled(false)
                    fillAlpha = 25
                    fillColor = lineColor.toArgb()
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    setDrawHorizontalHighlightIndicator(false)
                    setDrawVerticalHighlightIndicator(false)
                }
                data.addDataSet(set)
            }

            if (hist.size > lastCount) {

                val newPoints = hist.drop(lastCount)

                newPoints.forEach {
                    val xIndex = set.entryCount.toFloat()
                    data.addEntry(
                        Entry(xIndex, it.value),
                        0
                    )
                }

                lastCount = hist.size

                data.notifyDataChanged()
                lineChart.notifyDataSetChanged()

                lineChart.setVisibleXRangeMaximum(50f)
                lineChart.moveViewToX(set.entryCount.toFloat())
            }

            val lowest = lineChart.lowestVisibleX
            val highlight = lineChart.highlighted
            if (!highlight.isNullOrEmpty()) {
                if (highlight[0].x < lowest) {
                    lineChart.highlightValue(null)
                }
            }

            lineChart.invalidate()
        }
    )
}

private class SimpleMarker(
    context: Context,
    private val textColor: Int
) : MarkerView(context, android.R.layout.simple_list_item_1) {

    private val tv: TextView = findViewById(android.R.id.text1)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        tv.setTextColor(textColor)
        tv.text = String.format("%.2f", e?.y ?: 0f)
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}