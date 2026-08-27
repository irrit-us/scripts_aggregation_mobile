package com.scripthost.ui

import android.content.Context
import android.view.MotionEvent
import android.view.VelocityTracker
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.abs

/**
 * Shared chrome for full-screen sub-screens (settings, script editor, script
 * sub-pages): a top-left X button and a rightward swipe that both close the
 * current screen.
 */
object SubScreenChrome {

    private const val MIN_FLING_DISTANCE_DP = 64
    private const val MIN_FLING_VELOCITY_PX_S = 800f
    private const val HORIZONTAL_DOMINANCE = 1.5f

    /**
     * A top-left X (✕) button that invokes [onClose].
     */
    fun closeButton(context: Context, onClose: () -> Unit): TextView {
        return TextView(context).apply {
            text = "✕"
            textSize = 22f
            setPadding(24, 16, 24, 16)
            setOnClickListener { onClose() }
        }
    }

    /**
     * A container that invokes [onClose] on a clear, quick rightward fling.
     *
     * The gesture is observed in `dispatchTouchEvent`, which sees every touch
     * before child dispatch — so the fling is detected even when children
     * (ScrollViews, sliders, buttons) consume the events, which a root-view
     * `OnTouchListener` would never see. Events are never consumed or
     * intercepted, so vertical scrolling, taps, and child horizontal gestures
     * keep working. The velocity threshold separates a fling from a slow
     * drag (e.g. scrubbing a Slider), and horizontal dominance keeps vertical
     * flings from closing the screen.
     */
    fun swipeRightCloseContainer(context: Context, onClose: () -> Unit): FrameLayout {
        val minDistancePx = MIN_FLING_DISTANCE_DP * context.resources.displayMetrics.density

        return object : FrameLayout(context) {
            private var downX = 0f
            private var velocityTracker: VelocityTracker? = null

            override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = ev.x
                        velocityTracker?.recycle()
                        velocityTracker = VelocityTracker.obtain().apply { addMovement(ev) }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        velocityTracker?.addMovement(ev)
                    }
                    MotionEvent.ACTION_UP -> {
                        velocityTracker?.let { tracker ->
                            tracker.addMovement(ev)
                            tracker.computeCurrentVelocity(1000)
                            val velocityX = tracker.xVelocity
                            val velocityY = tracker.yVelocity
                            if (ev.x - downX > minDistancePx &&
                                velocityX > MIN_FLING_VELOCITY_PX_S &&
                                velocityX > abs(velocityY) * HORIZONTAL_DOMINANCE
                            ) {
                                onClose()
                            }
                        }
                        velocityTracker?.recycle()
                        velocityTracker = null
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        velocityTracker?.recycle()
                        velocityTracker = null
                    }
                }
                return super.dispatchTouchEvent(ev)
            }
        }
    }
}
