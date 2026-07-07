package luzzr.xi.feature.overlay

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Test

class BubbleTouchHandlerTest {

    @Before
    fun setUp() {
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockk(relaxed = true)

        mockkConstructor(Handler::class)
        every { anyConstructed<Handler>().postDelayed(any(), any<Long>()) } returns true
        every { anyConstructed<Handler>().removeCallbacks(any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `isDragging is true during move`() {
        val mockParams = WindowManager.LayoutParams().apply {
            x = 0
            y = 0
        }
        val mockWm = mockk<WindowManager>(relaxed = true)
        val mockView = mockk<View>(relaxed = true)
        every { mockView.parent } returns null  // skip expandTouchArea

        val handler = BubbleTouchHandler(
            params = mockParams,
            screenWidth = 1080,
            displayHeight = 1920,
            density = 2.0f,
            windowManager = mockWm,
            view = mockView,
            onClick = {},
            onLongPress = {}
        )

        val downEvent = mockk<MotionEvent> {
            every { action } returns MotionEvent.ACTION_DOWN
            every { rawX } returns 100f
            every { rawY } returns 100f
        }
        handler.handleTouchEvent(downEvent)

        val moveEvent = mockk<MotionEvent> {
            every { action } returns MotionEvent.ACTION_MOVE
            every { rawX } returns 200f
            every { rawY } returns 100f
        }
        handler.handleTouchEvent(moveEvent)

        assertEquals(100, mockParams.x)
    }
}
