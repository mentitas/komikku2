package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.content.Context
import android.os.Parcelable
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.viewpager.widget.DirectionalViewPager
import eu.kanade.tachiyomi.ui.reader.viewer.GestureDetectorWithLongTap
import kotlin.math.abs

/**
 * Pager implementation that listens for tap and long tap and allows temporarily disabling touch
 * events in order to work with child views that need to disable touch events on this parent. The
 * pager can also be declared to be vertical by creating it with [isHorizontal] to false.
 *
 * Also, it doesn't have sliding animations
 *
 */
open class Pager(
    context: Context,
    isHorizontal: Boolean = true,
) : DirectionalViewPager(context, isHorizontal) {

    /**
     * Tap listener function to execute when a tap is detected.
     */
    var tapListener: ((MotionEvent) -> Unit)? = null

    /**
     * Long tap listener function to execute when a long tap is detected.
     */
    var longTapListener: ((MotionEvent) -> Boolean)? = null


    /*
    * Swipe detection sensibility
    */
    private val swipeThreshold = 100
    private val swipeVelocityThreshold = 100

    // SY -->
    var isRestoring = false

    override fun onRestoreInstanceState(state: Parcelable?) {
        isRestoring = true
        val currentItem = currentItem
        super.onRestoreInstanceState(state)
        setCurrentItem(currentItem, false)
        isRestoring = false
    }
    // SY <--

    /**
     * Gesture listener that implements tap and long tap events.
     */
    private val gestureListener = object : GestureDetectorWithLongTap.Listener() {
        override fun onSingleTapConfirmed(ev: MotionEvent): Boolean {
            tapListener?.invoke(ev)
            return true
        }

        override fun onLongTapConfirmed(ev: MotionEvent) {
            val listener = longTapListener
            if (listener != null && listener.invoke(ev)) {
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false

            val diffY = e2.y - e1.y
            val diffX = e2.x - e1.x

            // Check if the swipe is horizontal or vertical based on pager orientation
            return if (abs(diffX) > abs(diffY)) {
                if (abs(diffX) > swipeThreshold && abs(velocityX) > swipeVelocityThreshold) {
                    if (diffX > 0) onSwipeRight() else onSwipeLeft()
                    true
                } else false
            } else {
                if (abs(diffY) > swipeThreshold && abs(velocityY) > swipeVelocityThreshold) {
                    if (diffY > 0) onSwipeBottom() else onSwipeTop()
                    true
                } else false
            }
        }

    }

    /**
     * Gesture detector which handles motion events.
     */
    private val gestureDetector = GestureDetectorWithLongTap(context, gestureListener)

    /**
     * Whether the gesture detector is currently enabled.
     */
    private var isGestureDetectorEnabled = true

    // --- Swipe Logic ---

    private fun onSwipeLeft() {
        setCurrentItem(currentItem + 1, false)
    }

    private fun onSwipeRight() {
        setCurrentItem(currentItem - 1, false)
    }

    private fun onSwipeBottom() {
        setCurrentItem(currentItem + 1, false)
    }

    private fun onSwipeTop() {
        setCurrentItem(currentItem - 1, false)
    }
    /**
     * Dispatches a touch event.
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val handled = super.dispatchTouchEvent(ev)
        // if (isGestureDetectorEnabled) {
        //     gestureDetector.onTouchEvent(ev)
        // }
        return handled
    }

    /**
     * Whether the given [ev] should be intercepted. Only used to prevent crashes when child
     * views manipulate [requestDisallowInterceptTouchEvent].
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // return try {
        //     super.onInterceptTouchEvent(ev)
        // } catch (e: IllegalArgumentException) {
        //     false
        // }

        if (isGestureDetectorEnabled) {
            gestureDetector.onTouchEvent(ev)
        }
        return false
    }

    /**
     * Handles a touch event. Only used to prevent crashes when child views manipulate
     * [requestDisallowInterceptTouchEvent].
     */
    override fun onTouchEvent(ev: MotionEvent): Boolean {

        if (isGestureDetectorEnabled) {
            gestureDetector.onTouchEvent(ev)
        }
        return true
        //return try {
        //    super.onTouchEvent(ev)
        //} catch (e: NullPointerException) {
        //    false
        //} catch (e: IndexOutOfBoundsException) {
        //    false
        //} catch (e: IllegalArgumentException) {
        //    false
        //}
    }

    /**
     * Executes the given key event when this pager has focus. Just do nothing because the reader
     * already dispatches key events to the viewer and has more control than this method.
     */
    override fun executeKeyEvent(event: KeyEvent): Boolean {
        // Disable viewpager's default key event handling
        return false
    }

    /**
     * Enables or disables the gesture detector.
     */
    fun setGestureDetectorEnabled(enabled: Boolean) {
        isGestureDetectorEnabled = enabled
    }
}
