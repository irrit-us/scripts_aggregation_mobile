package com.scripthost.ui.chart

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.scripthost.TestApplication
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [SimpleChartView] covering property round-trips and
 * crash-safety of [SimpleChartView.onDraw] for edge-case data sets.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class SimpleChartViewTest {

    private lateinit var view: SimpleChartView

    @Before
    fun setUp() {
        view = SimpleChartView(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun properties_roundTripThroughGetters() {
        view.data = listOf(1f, 2.5f, -3f)
        view.labels = listOf("a", "b", "c")
        view.lineColor = Color.RED
        view.chartType = SimpleChartView.ChartType.BAR

        assertThat(view.data).containsExactly(1f, 2.5f, -3f).inOrder()
        assertThat(view.labels).containsExactly("a", "b", "c").inOrder()
        assertThat(view.lineColor).isEqualTo(Color.RED)
        assertThat(view.chartType).isEqualTo(SimpleChartView.ChartType.BAR)
    }

    @Test
    fun chartType_defaultsToLine() {
        assertThat(view.chartType).isEqualTo(SimpleChartView.ChartType.LINE)
    }

    @Test
    fun draw_emptyData_doesNotCrash() {
        view.data = emptyList()
        drawView()
    }

    @Test
    fun draw_singlePoint_doesNotCrash() {
        view.data = listOf(5f)
        view.labels = listOf("only")
        drawView()
    }

    @Test
    fun draw_negativeValues_doesNotCrash() {
        view.data = listOf(-10f, 4f, -2f, 8f)
        view.labels = listOf("a", "b", "c", "d")
        drawView()
    }

    @Test
    fun draw_barChart_doesNotCrash() {
        view.chartType = SimpleChartView.ChartType.BAR
        view.data = listOf(3f, 7f, 1f)
        view.labels = listOf("a", "b", "c")
        drawView()
    }

    @Test
    fun draw_moreLabelsThanDataPoints_doesNotCrash() {
        view.data = listOf(1f, 2f)
        view.labels = listOf("a", "b", "c", "d", "e", "f")
        drawView()
    }

    private fun drawView() {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 400, 300)
        view.draw(Canvas(Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)))
    }
}
