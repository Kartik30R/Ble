//package com.example.ecgpreview
//
//import android.content.Context
//import android.graphics.Color
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.compose.foundation.layout.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.viewinterop.AndroidView
//import com.github.mikephil.charting.charts.LineChart
//import com.github.mikephil.charting.components.MarkerView
//import com.github.mikephil.charting.components.XAxis
//import com.github.mikephil.charting.data.*
//import com.github.mikephil.charting.highlight.Highlight
//import com.github.mikephil.charting.utils.MPPointF
//import kotlinx.coroutines.delay
//import kotlin.math.sin
//import kotlin.random.Random
//
//@Composable
//fun RealTimeEcgChart() {
//
//    var chart: LineChart? by remember { mutableStateOf(null) }
//    var xIndex by remember { mutableStateOf(0f) }
//    var value by remember { mutableStateOf(0f) }
//
//    // Fake ECG-like generator
//    LaunchedEffect(Unit) {
//        var t = 0f
//        while (true) {
//            val base = sin(t) * 8f
//            val spike = if (Random.nextFloat() > 0.97f) 18f else 0f
//            value = (base + spike).coerceIn(-20f, 20f)
//            t += 0.2f
//            delay(16)
//        }
//    }
//
//    AndroidView(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(320.dp),
//        factory = { context ->
//
//            LineChart(context).apply {
//
//                layoutParams = ViewGroup.LayoutParams(
//                    ViewGroup.LayoutParams.MATCH_PARENT,
//                    ViewGroup.LayoutParams.MATCH_PARENT
//                )
//
//                description.isEnabled = false
//                setTouchEnabled(true)
//                setPinchZoom(false)
//                setScaleEnabled(false)
//                legend.isEnabled = false
//                isHighlightPerTapEnabled = true
//
//                xAxis.position = XAxis.XAxisPosition.BOTTOM
//                xAxis.setDrawLabels(false)
//                xAxis.setDrawGridLines(false)
//
//                axisRight.isEnabled = false
//                axisLeft.axisMinimum = -20f
//                axisLeft.axisMaximum = 20f
//                axisLeft.setDrawGridLines(true)
//
//                marker = ECGMarker(context)
//
//                data = LineData()
//            }.also { chart = it }
//        },
//        update = { lineChart ->
//
//            val data = lineChart.data
//            if (data != null) {
//
//                var set = data.getDataSetByIndex(0)
//
//                if (set == null) {
//                    set = LineDataSet(null, "ECG").apply {
//                        color = Color.GREEN
//                        lineWidth = 2f
//                        setDrawCircles(false)
//                        setDrawValues(false)
//                        setDrawFilled(false)
//                        setDrawHighlightIndicators(true)
//                        highLightColor = Color.WHITE
//                    }
//                    data.addDataSet(set)
//                }
//
//                data.addEntry(Entry(xIndex, value), 0)
//
//                data.notifyDataChanged()
//                lineChart.notifyDataSetChanged()
//
//                lineChart.setVisibleXRangeMaximum(200f)
//                lineChart.moveViewToX(xIndex)
//
//                // Auto-remove highlight if out of view
//                val lowestX = lineChart.lowestVisibleX
//                val highlight = lineChart.highlighted
//                if (highlight != null && highlight.isNotEmpty()) {
//                    if (highlight[0].x < lowestX) {
//                        lineChart.highlightValue(null)
//                    }
//                }
//
//                xIndex++
//            }
//        }
//    )
//}
//
//class ECGMarker(context: Context) :
//    MarkerView(context, android.R.layout.simple_list_item_1) {
//
//    private val tv: TextView = findViewById(android.R.id.text1)
//
//    override fun refreshContent(e: Entry?, highlight: Highlight?) {
//        tv.text = String.format("%.2f", e?.y ?: 0f)
//        super.refreshContent(e, highlight)
//    }
//
//    override fun getOffset(): MPPointF {
//        return MPPointF(-(width / 2f), -height.toFloat())
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun PreviewEcgChart() {
//    RealTimeEcgChart()
//}