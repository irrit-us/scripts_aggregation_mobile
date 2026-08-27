package com.scripthost.bridge

import android.content.Context
import android.graphics.Paint
import android.os.Looper
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.scripthost.TestApplication
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Unit tests for [UIBridge] page-stack behavior (pushPage/popPage/pageDepth).
 * The bridge is constructed without register(), so no V8 runtime is involved.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class UIBridgeTest {

    private lateinit var host: FrameLayout
    private lateinit var bridge: UIBridge

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        host = FrameLayout(context)
        bridge = UIBridge(context, host)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun initialState_hostContainsOnlyRootPage() {
        assertThat(host.childCount).isEqualTo(1)
        assertThat(host.getChildAt(0)).isInstanceOf(LinearLayout::class.java)
        assertThat(bridge.pageDepth()).isEqualTo(1)
    }

    @Test
    fun pushPage_swapsHostChildAndIncreasesDepth() {
        val rootPage = host.getChildAt(0)

        assertThat(bridge.pushPage()).isEqualTo(2)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(host.childCount).isEqualTo(1)
        assertThat(host.getChildAt(0)).isNotSameInstanceAs(rootPage)
        assertThat(bridge.pageDepth()).isEqualTo(2)

        val secondPage = host.getChildAt(0)
        assertThat(bridge.pushPage()).isEqualTo(3)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(host.childCount).isEqualTo(1)
        assertThat(host.getChildAt(0)).isNotSameInstanceAs(secondPage)
        assertThat(bridge.pageDepth()).isEqualTo(3)
    }

    @Test
    fun popPage_restoresPreviousPage() {
        val rootPage = host.getChildAt(0)
        bridge.pushPage()
        val secondPage = host.getChildAt(0)
        bridge.pushPage()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(bridge.popPage()).isTrue()
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(host.childCount).isEqualTo(1)
        assertThat(host.getChildAt(0)).isSameInstanceAs(secondPage)
        assertThat(bridge.pageDepth()).isEqualTo(2)

        assertThat(bridge.popPage()).isTrue()
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(host.childCount).isEqualTo(1)
        assertThat(host.getChildAt(0)).isSameInstanceAs(rootPage)
        assertThat(bridge.pageDepth()).isEqualTo(1)
    }

    @Test
    fun popPage_atRoot_returnsFalseAndKeepsChild() {
        val rootPage = host.getChildAt(0)

        assertThat(bridge.popPage()).isFalse()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(bridge.pageDepth()).isEqualTo(1)
        assertThat(host.childCount).isEqualTo(1)
        assertThat(host.getChildAt(0)).isSameInstanceAs(rootPage)
    }

    @Test
    fun applyStrikeThrough_addsAndRemovesFlag() {
        val checkBox = android.widget.CheckBox(ApplicationProvider.getApplicationContext())

        UIBridge.applyStrikeThrough(checkBox, true)
        assertThat(checkBox.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG)
            .isEqualTo(Paint.STRIKE_THRU_TEXT_FLAG)

        UIBridge.applyStrikeThrough(checkBox, false)
        assertThat(checkBox.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG).isEqualTo(0)
    }

    @Test
    fun applyStrikeThrough_preservesOtherFlags() {
        val textView = android.widget.TextView(ApplicationProvider.getApplicationContext())
        textView.paintFlags = Paint.UNDERLINE_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG

        UIBridge.applyStrikeThrough(textView, true)
        assertThat(textView.paintFlags and Paint.UNDERLINE_TEXT_FLAG)
            .isEqualTo(Paint.UNDERLINE_TEXT_FLAG)
        assertThat(textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG)
            .isEqualTo(Paint.STRIKE_THRU_TEXT_FLAG)

        UIBridge.applyStrikeThrough(textView, false)
        assertThat(textView.paintFlags and Paint.UNDERLINE_TEXT_FLAG)
            .isEqualTo(Paint.UNDERLINE_TEXT_FLAG)
        assertThat(textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG).isEqualTo(0)
    }
}
